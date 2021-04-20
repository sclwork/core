package com.scliang.core.media.voice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.scliang.core.base.BaseApplication;
import com.scliang.core.base.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/1/20.
 *
 * 语音管理
 * 包括科大讯飞语音听写等功能
 *
 */
public final class Voice {
    // 语音识别器[科大讯飞/*|百度语音*/] - 默认科大讯飞
    private VoiceRecognizer mVoiceRecognizer = VoiceRecognizer.KDXF;
    private Queue<Buffer> mBufferQueue = new LinkedList<>();
    private OnVoiceRecognizerListener mOnVoiceRecognizerListener;
    private final byte[] mKDXFSpeechSync = new byte[] {0};
    private SpeechRecognizer mKDXFSpeechRecognizer;
    private KDXFVoiceWriteHandler mKDXFVoiceWriteHandler;
//    private EventManager mBaiduEventManager;
//    private BaiduInputStream mBaiduInputStream = new BaiduInputStream(mBufferQueue);
    private boolean mInited = false;
    private Boolean mKDXFInitSuccess = null;
    private boolean mKDXFInitCompleted;
//    private boolean mBaiduRunning;
    private boolean mVoiceStarting = false;
//    private final byte[] mBaiduSync = new byte[] {0};
    private OnSetVoiceParamsListener mOnSetVoiceParamsListener;
    private String mKDXFAppId;
//    private boolean mVoiceWritable;
    static final byte[] sVoiceBufferQueueSync = new byte[] {0};
    private boolean mStopListeningCalled = false;
    private static boolean useKDXFDWAParameter = true;
    private LinkedHashMap<String, String> mKDXFRecognResults = new LinkedHashMap<>();

    private Voice() {
    }

    private static class SingletonHolder {
        private static final Voice INSTANCE = new Voice();
    }

    public static Voice getInstance() {
        return Voice.SingletonHolder.INSTANCE;
    }

//    public static InputStream getBaiduInputStream() {
//        return getInstance().mBaiduInputStream;
//    }

    private SoftReference<Context> mContext;

    public static void setUseKDXFDWAParameter(boolean use) {
        useKDXFDWAParameter = use;
    }

    public static boolean isUseKDXFDWAParameter() {
        return useKDXFDWAParameter;
    }

    boolean isVoiceStarting() {
        return mVoiceStarting;
    }

    /**
     * 设置语音听写器参数
     */
    public void setOnSetVoiceParamsListener(OnSetVoiceParamsListener listener) {
        mOnSetVoiceParamsListener = listener;
    }

    /**
     * 使用ApplicationContext初始化Voice工具
     */
    public void init(BaseApplication application,
                     @NonNull VoiceConfig voiceConfig,
                     boolean debuggable) {
        // 避免重复初始化
        if (mInited) {
            Logger.e("Voice", "Init Success.");
            return;
        }

        final Context context = application.getApplicationContext();
        mContext = new SoftReference<>(context);
        mVoiceRecognizer = voiceConfig.getVoiceRecognizer();

        // 判断是否满足科大讯飞
        String xfAppId = voiceConfig.getKDXFAppId();
        if (mVoiceRecognizer == VoiceRecognizer.KDXF) {
            if (TextUtils.isEmpty(xfAppId)) {
                mInited = false;
                Logger.e("Voice", "Init Fail.");
                return;
            }

            // 记录下科大讯飞AppId
            mKDXFAppId = xfAppId;
        }

//        // 判断是否满足百度语音
//        if (mVoiceRecognizer == VoiceRecognizer.BAIDU) {
//            // nothing
//        }

        // 标记已经初始化完成
        mInited = true;
        Logger.e("Voice", "Init Success.");
    }

    /**
     * 启动语音操作工具
     */
    public void start() {
        start(true);
    }

    /**
     * 启动语音操作工具
     */
    public void start(boolean clearBufferQueue) {
        // 判断是否有可用网络
        if (!hasConnectedNetwork()) {
            // Debug
            Logger.d("Voice", "Has no ConnectedNetwork.");
            return;
        }

        // 判断是否满足科大讯飞
        if (mVoiceRecognizer == VoiceRecognizer.KDXF) {
            final Context context = mContext == null ? null : mContext.get();
            if (context == null || TextUtils.isEmpty(mKDXFAppId)) {
                // 语音识别出错标记为不可写入
                synchronized (sVoiceBufferQueueSync) {
//                    mVoiceWritable = false;
                    // 需要清空BufferQueue
                    mBufferQueue.clear();
                }
                // 停止语音操作工具
                stop(false);
                // 通知语音识别出错
                if (mOnVoiceRecognizerListener != null) {
                    mOnVoiceRecognizerListener.onRecogError(VoiceRecognizer.KDXF, -77, "Start Fail.");
                }
                // Debug
                Logger.d("Voice", "SpeechRecognizer Start Fail.");
                return;
            }

            // 请勿在“=”与appid之间添加任何空字符或者转义符
            SpeechUtility.createUtility(context, com.iflytek.cloud.SpeechConstant.APPID + "=" + mKDXFAppId);
        }

        // 启动语音操作工具
        start(clearBufferQueue, new Runnable() {
            @Override
            public void run() {
//                // 可以接受语音数据写入
//                synchronized (sVoiceBufferQueueSync) {
//                    mVoiceWritable = true;
//                }
            }
        });

        // 标记主动停止VoiceListening: false
        mStopListeningCalled = false;
    }

    /**
     * 启动语音操作工具
     */
    @SuppressLint("HandlerLeak")
    public void start(boolean clearBufferQueue, final Runnable completed) {
        // 判断是否有可用网络
        if (!hasConnectedNetwork()) {
            // Debug
            Logger.d("Voice", "Has no ConnectedNetwork.");
            return;
        }

        // 如果没有设置OnVoiceRecognizerListener监听器就不能启动语音操作工具
        if (mOnVoiceRecognizerListener == null) {
            return;
        }

        // 检查并停止正在运行的语音操作工具
        stop(false);

        // 判断Voice是否初始化完成
        if (!mInited) {
            Logger.e("Voice", "Need Init.");
            return;
        }

        // 启动一个新的语音操作工具
        final Context context = mContext == null ? null : mContext.get();
        if (context == null) {
            return;
        }

        // reset mKDXFRecognResults
        mKDXFRecognResults.clear();

        // 判断是否需要清空BufferQueue
        if (clearBufferQueue) {
            synchronized (sVoiceBufferQueueSync) {
                mBufferQueue.clear();
            }
        }

        // 判断是否满足科大讯飞
        if (mVoiceRecognizer == VoiceRecognizer.KDXF) {
            mKDXFInitSuccess = null;
            mKDXFInitCompleted = false;
            final Handler uiHandler = new Handler(Looper.getMainLooper());
            // 设置KDXFSpeechRecognizer
            final Runnable setupKDXFSpeechRecognizerRunnable = new Runnable() {
                @Override
                public void run() {
                    // 设置KDXFSpeechRecognizer
                    setupKDXFSpeechRecognizer(completed);
                }
            };
            synchronized (mKDXFSpeechSync) {
                // 创建KDXFSpeechRecognizer
                mKDXFSpeechRecognizer = SpeechRecognizer.createRecognizer(context, new InitListener() {
                    @Override
                    public void onInit(final int code) {
                        // 标记KDXFSpeechRecognizer初始化成功
                        mKDXFInitSuccess = code == ErrorCode.SUCCESS;
//                        if (code == ErrorCode.SUCCESS) {
//                            // 标记KDXFSpeechRecognizer初始化成功
//                            mKDXFInitSuccess = true;
//                        } else {
////                            // 语音识别出错标记为不可写入
////                            synchronized (sVoiceBufferQueueSync) {
////                                mVoiceWritable = false;
////                                // 需要清空BufferQueue
////                                mBufferQueue.clear();
////                            }
//                            // 标记KDXFSpeechRecognizer初始化失败
//                            mKDXFInitSuccess = false;
//                        }
                        // 判断设置KDXFSpeechRecognizer
                        uiHandler.post(setupKDXFSpeechRecognizerRunnable);
                    }
                });
            }
//            // 判断设置KDXFSpeechRecognizer
//            uiHandler.post(setupKDXFSpeechRecognizerRunnable);
        }

//        // 判断是否满足百度语音
//        if (mVoiceRecognizer == VoiceRecognizer.BAIDU) {
//            // 检查是否拥有相应权限
//            if (!Permission.hasReadPhoneStatePermission(new OnCheckPermissionImpl())) {
//                // 通知识别启动失败，没有相应的权限
//                if (mOnVoiceRecognizerListener != null) {
//                    mOnVoiceRecognizerListener.onRecogError(VoiceRecognizer.BAIDU,
//                        -99, "NoReadPhoneStatePermission");
//                }
//                // 终止
//                return;
//            }
//
//            // 通知开始识别
//            if (mOnVoiceRecognizerListener != null) {
//                mOnVoiceRecognizerListener.onRecogStart(VoiceRecognizer.BAIDU);
//            }
//
//            // 开始识别
//            mBaiduInputStream.start();
//            mBaiduEventManager = EventManagerFactory.create(context, "asr");
//            mBaiduEventManager.registerListener(mBaiduEventListener);
//            Map<String, Object> params = new HashMap<>();
//            (new BaiduAutoCheck(context, new Handler() {
//                public void handleMessage(Message msg) {
//                    if (msg.what == 100) {
//                        final BaiduAutoCheck autoCheck = (BaiduAutoCheck) msg.obj;
//                        synchronized (autoCheck) {
//                            String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
////                            txtLog.append(message + "\n");
//                            //; // 可以用下面一行替代，在logcat中查看代码
//                             Logger.d("AutoCheckMessage", message);
//                        }
//                    }
//                }
//            }, false)).checkAsr(params);
//            params = makeBaiduSpeechRecognizerParams();
//            String paramsJson = new JSONObject(params).toString();
//            mBaiduEventManager.send(com.baidu.speech.asr.SpeechConstant.ASR_START,
//                paramsJson, null, 0, 0);
//            synchronized (mBaiduSync) {
//                mBaiduRunning = true;
//            }
//            // 初始化完成
//            if (completed != null) completed.run();
//        }

        // flag voice start
        mVoiceStarting = true;
    }

    private void checkRestart() {
        synchronized (sVoiceBufferQueueSync) {
            if (mBufferQueue.isEmpty()) {
                return;
            }
        }

        // 重启音频数据写入
        if (mKDXFVoiceWriteHandler == null) {
            renewKDXFVoiceHandler(mKDXFSpeechRecognizer);
//          if (mKDXFVoiceWriteHandler == null) {
//              renewKDXFVoiceHandler(mKDXFSpeechRecognizer);
//          } else {
//              mKDXFVoiceWriteHandler.resume();
//          }

            // debug
            Logger.e("Voice", "Need Check Restart KDXFVoiceWriteHandler.");
        }

        // 重启录音
        if (!isRecognizerRunning()) {
            restart();

            // debug
            Logger.e("Voice", "Need Check Restart Recognizer.");
        }
    }

    /**
     * 重启语音操作工具
     */
    private void restart() {
        // 判断是否需要重启
        // 判断是否满足科大讯飞
        if (mVoiceRecognizer == VoiceRecognizer.KDXF) {
            if (mKDXFSpeechRecognizer != null && mKDXFSpeechRecognizer.isListening()) {
                return;
            }
        }

//        // 判断是否满足百度语音
//        if (mVoiceRecognizer == VoiceRecognizer.BAIDU) {
//            if (mBaiduRunning) {
//                return;
//            }
//        }

        start(false);
    }

    /**
     * 停止语音输入
     */
    public void stopListening() {
        boolean queueEmpty;
        synchronized (sVoiceBufferQueueSync) {
            queueEmpty = mBufferQueue.isEmpty();
        }
        if (queueEmpty && mKDXFSpeechRecognizer != null && mKDXFSpeechRecognizer.isListening()) {
            mKDXFSpeechRecognizer.stopListening();
        }
        // 标记主动停止VoiceListening: true
        mStopListeningCalled = true;
    }

    /**
     * 停止语音操作工具
     */
    public void stop() {
        // 停止科大讯飞相关操作工具
        if (mKDXFVoiceWriteHandler != null) {
            mKDXFVoiceWriteHandler.exit();
            mKDXFVoiceWriteHandler = null;
        }
        stop(true);

        // flag voice stop
        mVoiceStarting = false;
    }

    /**
     * 停止语音操作工具
     */
    public void stop(boolean flagListening) {
        // 停止科大讯飞相关操作工具
//        if (mKDXFVoiceWriteHandler != null) {
//            mKDXFVoiceWriteHandler.exit();
//            mKDXFVoiceWriteHandler = null;
//        }
        // 停止科大讯飞相关操作工具
        synchronized (mKDXFSpeechSync) {
            if (mKDXFSpeechRecognizer != null) {
                if (mKDXFSpeechRecognizer.isListening()) {
                    mKDXFSpeechRecognizer.stopListening();
                }
                mKDXFSpeechRecognizer.cancel();
                mKDXFSpeechRecognizer.destroy();
                mKDXFSpeechRecognizer = null;
                Logger.d("Voice", "SpeechRecognizer Destroy Success.");
            }
        }

//        // 停止百度语音相关操作工具
//        if (mBaiduEventManager != null) {
//            mBaiduInputStream.stop();
//            mBaiduEventManager.send(com.baidu.speech.asr.SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
//            mBaiduEventManager.send(com.baidu.speech.asr.SpeechConstant.ASR_STOP, "{}", null, 0, 0);
//            mBaiduEventManager.unregisterListener(mBaiduEventListener);
//            mBaiduEventManager = null;
//            synchronized (mBaiduSync) {
//                mBaiduRunning = false;
//            }
//        }

        if (flagListening) {
            // 标记主动停止VoiceListening: true
            mStopListeningCalled = true;
        }

        // 通知停止识别
        // 判断是否满足科大讯飞
        if (mVoiceRecognizer == VoiceRecognizer.KDXF) {
            if (mOnVoiceRecognizerListener != null) {
                mOnVoiceRecognizerListener.onRecogStop(VoiceRecognizer.KDXF);
            }
        }
//        // 判断是否满足百度语音
//        if (mVoiceRecognizer == VoiceRecognizer.BAIDU) {
//            if (mOnVoiceRecognizerListener != null) {
//                mOnVoiceRecognizerListener.onRecogStop(VoiceRecognizer.BAIDU);
//            }
//        }
    }

    /**
     * 判断语音操作工具是否正在运行
     */
    private boolean isRecognizerRunning() {
        // 判断是否满足科大讯飞
        if (mVoiceRecognizer == VoiceRecognizer.KDXF) {
            return mKDXFSpeechRecognizer != null && mKDXFSpeechRecognizer.isListening();
        }

//        // 判断是否满足百度语音
//        if (mVoiceRecognizer == VoiceRecognizer.BAIDU) {
//            synchronized (mBaiduSync) {
//                return mBaiduRunning;
//            }
//        }

        // 默认
        return false;
    }

    /**
     * 向语音操作工具中写入音频数据，用于进行语音听写
     */
    public void write(byte[] buffer, int size) {
        // 判断Voice是否初始化完成
        if (!mInited) {
            Logger.e("Voice", "Need Init.");
            return;
        }

//        // 判断是否可以写入
//        synchronized (sVoiceBufferQueueSync) {
//            if (!mVoiceWritable) {
//                return;
//            }
//        }

        // 判断是否可以语音听写
        if (mOnVoiceRecognizerListener != null) {
            if (buffer == null) {
                return;
            }

            synchronized (sVoiceBufferQueueSync) {
                mBufferQueue.add(new BBuffer(buffer, size));
            }
//            // 检查VoiceHandler是否正在运行
//            if (!isRunning()) {
//                start(false, null);
//            }
        }
    }

    /**
     * 向语音操作工具中写入音频数据，用于进行语音听写
     */
    public void write(short[] buffer, int size) {
        // 判断Voice是否初始化完成
        if (!mInited) {
            Logger.e("Voice", "Need Init.");
            return;
        }

//        // 判断是否可以写入
//        synchronized (sVoiceBufferQueueSync) {
//            if (!mVoiceWritable) {
//                return;
//            }
//        }

        // 判断是否可以语音听写
        if (mOnVoiceRecognizerListener != null) {
            if (buffer == null) {
                return;
            }

            // 将数据添加到队列
            synchronized (sVoiceBufferQueueSync) {
                mBufferQueue.add(new SBuffer(buffer, size));
            }

            // 检查VoiceHandler是否正在运行
            checkRestart();
        }
    }

    /**
     * 设置语音操作工具回调监听器
     * 使用科大讯飞语音听写SDK，识别后的词语回调
     * 如果没有设置这个监听器即为null时，不调用语音听写SDK
     * 只有设置了这个监听器，才会实时录音过程中进行语音听写转文字功能
     */
    public void setOnVoiceRecognizerListener(OnVoiceRecognizerListener listener) {
        // 判断Voice是否初始化完成
        if (!mInited) {
            Logger.e("Voice", "Need Init.");
            return;
        }

        // 设置回调监听器
        mOnVoiceRecognizerListener = listener;
    }

    /**
     * 主动追加录音转写等相关日志
     */
    public void appendRecognLog(String log) {
        try {
            if (mOnVoiceRecognizerListener != null && !TextUtils.isEmpty(log)) {
                mOnVoiceRecognizerListener.onRecogLog(VoiceRecognizer.KDXF, log);
            }
        } catch (Throwable ignored) {}
    }

    // 设置KDXFSpeechRecognizer
    private void setupKDXFSpeechRecognizer(Runnable completed) {
        if (mKDXFInitCompleted || mKDXFInitSuccess == null) {
            return;
        }

        // 判断是否初始化成功
        if (!mKDXFInitSuccess || mKDXFSpeechRecognizer == null) {
//            // 语音识别出错标记为不可写入
//            synchronized (sVoiceBufferQueueSync) {
//                mVoiceWritable = false;
//                // 需要清空BufferQueue
//                mBufferQueue.clear();
//            }
            // 停止语音操作工具
            stop(false);
            // 通知语音识别出错
            if (mOnVoiceRecognizerListener != null) {
                mOnVoiceRecognizerListener.onRecogError(VoiceRecognizer.KDXF, -88, "Init Fail.");
            }
            // Debug
            Logger.d("Voice", "SpeechRecognizer Init Fail.");
        }

        // 初始化成功
        else {
            // 通知开始识别
            if (mOnVoiceRecognizerListener != null) {
                mOnVoiceRecognizerListener.onRecogStart(VoiceRecognizer.KDXF);
            }

            synchronized (mKDXFSpeechSync) {
                if (mKDXFSpeechRecognizer != null) {
                    // 设置科大讯飞语音操作工具参数
                    setKDXFSpeechRecognizerParams(mKDXFSpeechRecognizer);
                    // 启动科大讯飞语音操作工具
                    mKDXFSpeechRecognizer.startListening(mKDXFRecognizerListener);
                    // 创建一个VoiceHandler
                    renewKDXFVoiceHandler(mKDXFSpeechRecognizer);
                    // Debug
                    Logger.d("Voice", "SpeechRecognizer Init Success.");
                    // 初始化成功 - 完成
                    mKDXFInitCompleted = true;
                    if (completed != null) completed.run();
                }
            }
        }
    }

    // 创建一个KDXFVoiceHandler
    private void renewKDXFVoiceHandler(SpeechRecognizer speechRecognizer) {
        // 判断Voice是否初始化完成
        if (!mInited) {
            Logger.e("Voice", "Need Init.");
            return;
        }

        // 判断是否满足科大讯飞
        if (mVoiceRecognizer != VoiceRecognizer.KDXF) {
            return;
        }

        // 停止已有的语音操作Handler
        if (mKDXFVoiceWriteHandler != null) {
            mKDXFVoiceWriteHandler.exit();
            mKDXFVoiceWriteHandler = null;
        }

        // 启动语音操作Handler
        final HandlerThread thread = new HandlerThread("VoiceOPThread-" + System.currentTimeMillis());
        thread.start();
        mKDXFVoiceWriteHandler = new KDXFVoiceWriteHandler(thread.getLooper(), speechRecognizer);
        mKDXFVoiceWriteHandler.start(mBufferQueue);
    }

    // 设置科大讯飞语音操作工具参数
    private void setKDXFSpeechRecognizerParams(SpeechRecognizer speechRecognizer) {
        // 判断Voice是否初始化完成
        if (!mInited) {
            Logger.e("Voice", "Need Init.");
            return;
        }

        // 判断是否满足科大讯飞
        if (mVoiceRecognizer != VoiceRecognizer.KDXF) {
            return;
        }

        if (speechRecognizer == null) {
            return;
        }

        // 清空参数
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.PARAMS, null);
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.AUDIO_SOURCE, "-1");
        // 设置听写引擎
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.ENGINE_TYPE, com.iflytek.cloud.SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.ACCENT, "mandarin");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.VAD_BOS, "5000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.VAD_EOS, "5000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.ASR_PTT, "1");
        // 判断是否使用动态修正参数
        if (useKDXFDWAParameter) {
            /*
            未开启动态修正：实时返回识别结果，每次返回的结果都是对之前结果的追加；
            开启动态修正：实时返回识别结果，每次返回的结果有可能是对之前结果的的追加，也有可能是要替换之前某次返回的结果（即修正）；
            开启动态修正，相较于未开启，返回结果的颗粒度更小，视觉冲击效果更佳；
            使用动态修正功能需到控制台-流式听写-高级功能处点击开通，并设置相应参数方可使用，参数设置方法：mIat.setParameter("dwa", "wpgs"); ；
            动态修正功能仅 中文 支持；
            未开启与开启返回的结果格式不同，详见下方；
            若开通了动态修正功能并设置了dwa=wpgs（仅中文支持），会有如下字段返回：

            参数	类型	    描述
            pgs	string	开启wpgs会有此字段
            取值为 "apd"时表示该片结果是追加到前面的最终结果；取值为"rpl" 时表示替换前面的部分结果，替换范围为rg字段
            rg	array	替换范围，开启wpgs会有此字段
            假设值为[2,5]，则代表要替换的是第2次到第5次返回的结果
             */
            speechRecognizer.setParameter("dwa", "wpgs");
        } else {
            // 设置听写结果是否结果动态修正，为“1”则在听写过程中动态递增地返回结果，否则只在听写结束之后返回最终结果
            // 注：该参数暂时只对在线听写有效
            speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.ASR_DWA, "1");
        }
        // 设置采用率
        speechRecognizer.setParameter(com.iflytek.cloud.SpeechConstant.SAMPLE_RATE, "16000");

        // 获取外部自定义参数
        if (mOnSetVoiceParamsListener != null) {
            Map<String, String> ps = mOnSetVoiceParamsListener
                .onSetVoiceParams(VoiceConfig.KDXF(mKDXFAppId));
            if (ps != null) {
                for (String key : ps.keySet()) {
                    String value = ps.get(key);
                    speechRecognizer.setParameter(key, value);
                }
            }
        }
    }

//    // 创建百度语音操作工具参数
//    private Map<String, Object> makeBaiduSpeechRecognizerParams() {
//        Map<String, Object> params = new HashMap<>();
//        params.put("pid", "1537");
//        params.put("infile_list", "#com.scliang.core.media.voice.Voice.getBaiduInputStream()");
//
//        // 获取外部自定义参数
//        if (mOnSetVoiceParamsListener != null) {
//            Map<String, String> ps = mOnSetVoiceParamsListener
//                .onSetVoiceParams(VoiceConfig.Baidu());
//            if (ps != null) {
//                for (String key : ps.keySet()) {
//                    String value = ps.get(key);
//                    if (value != null) {
//                        params.put(key, value);
//                    }
//                }
//            }
//        }
//
//        return params;
//    }

    // 设置科大讯飞语音操作工具监听器
    private RecognizerListener mKDXFRecognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int volume, // 当前音量值，范围[0-30]
                                    byte[] bytes) {
        }

        @Override
        public void onBeginOfSpeech() {
            Logger.d("Voice", "SpeechRecognizer onBeginOfSpeech");

            // callback recognizer log
            if (mOnVoiceRecognizerListener != null) {
                mOnVoiceRecognizerListener.onRecogLog(VoiceRecognizer.KDXF, "[beginofspeech]");
            }
        }

        @Override
        public void onEndOfSpeech() {
            Logger.d("Voice", "SpeechRecognizer onEndOfSpeech");

            // callback recognizer log
            if (mOnVoiceRecognizerListener != null) {
                mOnVoiceRecognizerListener.onRecogLog(VoiceRecognizer.KDXF, "[endofspeech]");
            }

            // 暂停音频数据写入
            if (mKDXFVoiceWriteHandler != null) {
//                mKDXFVoiceWriteHandler.pause();
              mKDXFVoiceWriteHandler.exit();
              mKDXFVoiceWriteHandler = null;
            }
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean isLast) {
            if (recognizerResult != null) {
                Logger.d("Voice", "SpeechRecognizer onResult: " + recognizerResult.getResultString());
                try {
                    if (useKDXFDWAParameter) {
                        String text = KDXFJsonParser.parseIatResult(recognizerResult.getResultString());
                        String sn = "";
                        String pgs = "";
                        String rg = "";
                        // 读取json结果中的sn字段
                        try {
                            JSONObject resultJson = new JSONObject(recognizerResult.getResultString());
                            sn = resultJson.optString("sn");
                            pgs = resultJson.optString("pgs");
                            rg = resultJson.optString("rg");
                        } catch (JSONException ignored) { }
                        //如果pgs是rpl就在已有的结果中删除掉要覆盖的sn部分
                        if (pgs.equals("rpl")) {
                            String[] strings = rg.replace("[", "")
                                .replace("]", "").split(",");
                            if (strings.length >= 2) {
                                try {
                                    int begin = Integer.parseInt(strings[0]);
                                    int end = Integer.parseInt(strings[1]);
                                    for (int i = begin; i <= end; i++) {
                                        mKDXFRecognResults.remove(String.valueOf(i));
                                    }
                                } catch (Exception ignored) { }
                            }
                        }
                        // append result
                        mKDXFRecognResults.put(sn, text);
                        StringBuilder resultBuffer = new StringBuilder();
                        for (String key : mKDXFRecognResults.keySet()) {
                            resultBuffer.append(mKDXFRecognResults.get(key));
                        }
                        if (mOnVoiceRecognizerListener != null) {
                            mOnVoiceRecognizerListener.onRecognition(VoiceRecognizer.KDXF, resultBuffer.toString());
                        }
                    } else {
                        JSONObject obj = new JSONObject(recognizerResult.getResultString());
                        JSONArray arr = obj.optJSONArray("ws");
                        if (arr != null && arr.length() > 0) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject wsObj = arr.optJSONObject(i);
                                if (wsObj != null) {
                                    JSONArray cwArr = wsObj.optJSONArray("cw");
                                    if (cwArr != null && cwArr.length() > 0) {
//                                        for (int j = 0; j < cwArr.length(); j++) {
                                            JSONObject cwObj = cwArr.optJSONObject(0);
                                            if (cwObj != null) {
                                                String w = cwObj.optString("w");
                                                if (!TextUtils.isEmpty(w) && mOnVoiceRecognizerListener != null) {
                                                    mOnVoiceRecognizerListener.onRecognition(VoiceRecognizer.KDXF, w);
                                                }
                                            }
//                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) { }
            }

            // callback recognizer log
            if (mOnVoiceRecognizerListener != null) {
                if (recognizerResult == null) {
                    mOnVoiceRecognizerListener.onRecogLog(
                      VoiceRecognizer.KDXF, String.format(Locale.CHINESE,
                        "[isLast:%s]RecognizerResult is null.",
                        String.valueOf(isLast)));
                } else {
                    mOnVoiceRecognizerListener.onRecogLog(
                      VoiceRecognizer.KDXF, String.format(Locale.CHINESE,
                        "[isLast:%s]%s",
                        String.valueOf(isLast), recognizerResult.getResultString()));
                }
            }

            // debug
            Logger.d("Voice", "SpeechRecognizer onResult isLast: " + isLast);

            // 最后的识别时，停止语音操作工具
            if (isLast) {
                if (mKDXFVoiceWriteHandler != null) {
//                    mKDXFVoiceWriteHandler.pause();
                  mKDXFVoiceWriteHandler.exit();
                  mKDXFVoiceWriteHandler = null;
                }
                stop(false);
                boolean queueEmpty;
                synchronized (sVoiceBufferQueueSync) {
                    queueEmpty = mBufferQueue.isEmpty();
                }
                if (mStopListeningCalled && queueEmpty) {
                    // 停止语音听写
                    if (mKDXFSpeechRecognizer != null && mKDXFSpeechRecognizer.isListening()) {
                        mKDXFSpeechRecognizer.stopListening();
                    }
                    // 通知识别结束
                    if (mOnVoiceRecognizerListener != null) {
                        mOnVoiceRecognizerListener.onRecogLast(VoiceRecognizer.KDXF);
                    }
                }
                // 重启听写机
                restart();
            } else {
                // 检查是否需要重启
                checkRestart();
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            if (speechError != null) {
                Logger.d("Voice", "SpeechRecognizer onError: (" +
                    speechError.getErrorCode() + ") " +
                    speechError.getErrorDescription());
            }

            // callback recognizer log
            if (mOnVoiceRecognizerListener != null && speechError != null) {
                mOnVoiceRecognizerListener.onRecogLog(
                  VoiceRecognizer.KDXF, String.format(Locale.CHINESE,
                    "[error:%d]%s",
                    speechError.getErrorCode(), speechError.getErrorDescription()));
            }

            // 判断是否识别结束
            // 10118/20008 您好像没有说话哦  --- 忽略这个错误
            final int code = speechError == null ? -11 : speechError.getErrorCode();
            if (code == 10118 || code == 20008) {
                boolean queueEmpty;
                synchronized (sVoiceBufferQueueSync) {
                    queueEmpty = mBufferQueue.isEmpty();
                }
                if (mStopListeningCalled && queueEmpty) {
                    // 停止语音听写
                    if (mKDXFSpeechRecognizer != null && mKDXFSpeechRecognizer.isListening()) {
                        mKDXFSpeechRecognizer.stopListening();
                    }
                    // 通知识别结束
                    if (mOnVoiceRecognizerListener != null) {
                        mOnVoiceRecognizerListener.onRecogLast(VoiceRecognizer.KDXF);
                    }
                }
            }

            // 出现错误后，停止语音操作工具
            stop(false);

            // 10118/20008 您好像没有说话哦  --- 重启听写机
            if (code == 10118 || code == 20008) {
                restart();
                return;
            }

//            // 语音识别出错标记为不可写入
//            synchronized (sVoiceBufferQueueSync) {
//                mVoiceWritable = false;
//                // 需要清空BufferQueue
//                mBufferQueue.clear();
//            }

            // 通知语音识别出错
            if (mOnVoiceRecognizerListener != null) {
                mOnVoiceRecognizerListener.onRecogError(VoiceRecognizer.KDXF,
                    speechError == null ? 0  : speechError.getErrorCode(),
                    speechError == null ? "" : speechError.getErrorDescription());
            }

            // 检查是否需要重启
            checkRestart();
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            Logger.d("Voice", "SpeechRecognizer onEvent: " + eventType + ", " + arg1 + ", " + arg2);

            // callback recognizer log
            if (mOnVoiceRecognizerListener != null) {
                StringBuilder objSb = new StringBuilder();
                objSb.append('[');
                if (obj != null && obj.keySet() != null) {
                    for (String key : obj.keySet()) {
                        objSb.append(key).append(':').append(obj.getString(key));
                        objSb.append(',');
                    }
                }
                objSb.append(']');
                mOnVoiceRecognizerListener.onRecogLog(
                  VoiceRecognizer.KDXF, String.format(Locale.CHINESE,
                    "[error:%d]%d,%d,%s",
                    eventType, arg1, arg2, objSb.toString()));
            }
        }
    };

//    // 设置百度语音操作工具监听器
//    private EventListener mBaiduEventListener = new EventListener() {
//        @Override
//        public void onEvent(String name, String params, byte[] data, int offset, int length) {
//            Logger.d("Voice", "name:" + name + "; params:" + params);
//            // 本次语音识别结束
//            if (com.baidu.speech.asr.SpeechConstant.CALLBACK_EVENT_ASR_EXIT.equals(name)) {
//                synchronized (mBaiduSync) {
//                    mBaiduRunning = false;
//                }
//
//                // 检查是否需要重启
//                checkRestart();
//            }
//            // 本次语音识别结果
//            if (com.baidu.speech.asr.SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL.equals(name) ||
//                    com.baidu.speech.asr.SpeechConstant.CALLBACK_EVENT_ASR_FINISH.equals(name)) {
//                // 解析结果
//                BaiduRecogResult recogResult = BaiduRecogResult.parseJson(params);
//                String bestResult = recogResult.getBestResult();
//                if (!TextUtils.isEmpty(bestResult)) {
//                    // 通知识别结果
//                    if (mOnVoiceRecognizerListener != null) {
//                        mOnVoiceRecognizerListener.onRecognition(VoiceRecognizer.BAIDU, bestResult);
//                    }
//                }
//                // callback recognizer result
//                if (mOnVoiceRecognizerListener != null) {
//                  mOnVoiceRecognizerListener.onRecogResult(VoiceRecognizer.BAIDU, bestResult);
//                }
//
//                // 检查是否需要重启
//                checkRestart();
//            }
//            // 本次语音识别出错
//            if (com.baidu.speech.asr.SpeechConstant.CALLBACK_EVENT_ASR_ERROR.equals(name)) {
////                // 语音识别出错标记为不可写入
////                synchronized (sVoiceBufferQueueSync) {
////                    mVoiceWritable = false;
////                    // 需要清空BufferQueue
////                    mBufferQueue.clear();
////                }
//                // 停止语音操作工具
//                stop(false);
//                // 通知语音识别出错
//                if (mOnVoiceRecognizerListener != null) {
//                    mOnVoiceRecognizerListener.onRecogError(VoiceRecognizer.BAIDU, 0, params);
//                }
//
//                // 检查是否需要重启
//                checkRestart();
//            }
//            // 本次语音识别结束
//            if (com.baidu.speech.asr.SpeechConstant.CALLBACK_EVENT_ASR_FINISH.equals(name)) {
//                // 通知识别结束
//                if (mOnVoiceRecognizerListener != null) {
//                    mOnVoiceRecognizerListener.onRecogLast(VoiceRecognizer.BAIDU);
//                }
//
//                // 检查是否需要重启
//                checkRestart();
//            }
//        }
//    };




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    // 判断是否有可用网络
    private boolean hasConnectedNetwork() {
        ConnectivityManager cm =(ConnectivityManager) BaseApplication.getApp()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } else {
            return false;
        }
    }
}
