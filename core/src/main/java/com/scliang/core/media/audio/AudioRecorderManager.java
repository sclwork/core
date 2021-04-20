package com.scliang.core.media.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.scliang.core.base.BaseActivity;
import com.scliang.core.base.BaseApplication;
import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.Logger;
import com.scliang.core.base.OnCheckPermissionImpl;
import com.scliang.core.base.Permission;
import com.scliang.core.base.rsa.Base64Utils;
import com.scliang.core.base.rsa.RSAProvider;
import com.scliang.core.media.IMediaNUtils;
import com.scliang.core.media.MediaNUtils3;
import com.scliang.core.media.MediaNUtils4;
import com.scliang.core.media.object.JRecorder;
import com.scliang.core.media.MediaNUtils;
import com.scliang.core.media.object.PCMBuffer;
import com.scliang.core.media.voice.OnVoiceRecognizerListener;
import com.scliang.core.media.voice.Voice;
import com.scliang.core.media.voice.VoiceRecognizer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/18.
 */
public final class AudioRecorderManager
    implements AudioRecorderStateListener, JRecorder.StateChangedListener {
  private AudioRecorderManager() {
  }

  private static class SingletonHolder {
    private static final AudioRecorderManager INSTANCE = new AudioRecorderManager();
  }

  public static AudioRecorderManager getInstance() {
    return SingletonHolder.INSTANCE;
  }


  private final static List<Handler> sHandlers =
    Collections.synchronizedList(new LinkedList<>());
  private OnVoiceRecognizerListener mAgainOnVoiceRecognizerListener;
  private final static List<SoftReference<OnAudioRecorderStopListener>> sOnAudioRecorderStopListeners =
    Collections.synchronizedList(new LinkedList<>());

  // 录音状态
  public final static int STATE_IDLE = 0;
  public final static int STATE_RECORDING = 1;
  public final static int STATE_RECORD_PAUSED = 2;
  public final static int STATE_RECORD_STOPED = 7;

  // 录音状态改变标示
  public static final int WHAT_RECORD_STATE_CHANGED = 3;
  // 处理状态改变标示
  public static final int WHAT_PROCESS_STATE_CHANGED = 4;

  // 音频分贝改变标示
  public static final int WHAT_RECORD_dB_CHANGED = 5;
  public static final int WHAT_RECORD_dB_SO_HIGH = 51;
  public static final int WHAT_RECORD_ERROR_INFO = 52;

  // 设备耳机等外设插入拔出状态变更
  public static final int WHAT_HEADSET_PLUG_CHANGED = 6;

  // Has Permissions
  public final static int WHAT_HAS_PERMISSIONS = 6666;

  // 音频获取源
  private final static int sAudioSourceMic = MediaRecorder.AudioSource.MIC;
  private final static int sAudioSourceVcm = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
  // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持 22050，16000，11025
  private final static int sDefaultSampleRateInHz = 44100;
  // 科大讯飞SDK暂时只支持16000
  private final static int sKDXFSampleRateInHz = 16000;
  private static int sSampleRateInHz = sDefaultSampleRateInHz;
  // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_IN_MONO为单声道
  // 这个参数关联了AudioRecordHandler.calculatePCM方法里计算PCM时长的实现
  // 如果修改了这个参数，需要及时更新到AudioRecordHandler.calculatePCM方法里
  private final static int sChannelConfig = AudioFormat.CHANNEL_IN_MONO;
  // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
  // 这个参数关联了AudioRecordHandler.calculatePCM方法里计算PCM时长的实现
  // 如果修改了这个参数，需要及时更新到AudioRecordHandler.calculatePCM方法里
  private final static int sAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
  // 通知周期，录音每满160帧，进行一次通知
  private static final int FRAME_COUNT = 160;
  // 输出MP3的码率
  public final static int BIT_RATE = 128;
  // Audio Buffer Size
  private static int sPCMBufferSize = 0;
  // Audio Buffer
  private static short[] sPCMBuffer;
  // 用于存取待转换的PCM数据
  private static String sPCMBufferFileName;

  // Application
  private SoftReference<BaseApplication> mApp;
  // Notice Handler
  private NoticeHandler mNoticeHandler;
  // Audio Record Handler
  private AudioRecordHandler mAudioRecordHandler;
  // PCM Buffer Write Handler
  private WritePCMHandler mWritePCMHandler;
  // PCM Calculate Handler
  private CalculatePCMHandler mCalculatePCMHandler;

  // Current State
  private int mState = STATE_IDLE;

  // Check CPU ABI Support
  private boolean isCpuAbiSupport;
  private static IMediaNUtils sMediaNUtils;

  // 音量控制 - VolumeAdjust
  private RecordVolumeAgcConfig mWebRTCAgcConfig;

  // 避免重复初始化
  private boolean isInited;

  // 判断是否可以对录音器进行操作
  private boolean mOpable;

  // 接收耳机麦克风插入情况 - 检查耳机插入情况
  private HeadsetPlugReceiver mHeadsetPlugReceiver;
  // 记录着耳机麦克风是否插入
  // 0:默认内置麦克风
  // 1:有线耳机麦克风
  // 2:蓝牙耳机麦克风
  private int mHeadsetPlugType = 0;

  // 更新采用率: 44100是目前的标准，但是某些设备仍然支持 22050，16000，11025; 科大讯飞SDK暂时只支持16000
  private void updateSampleRateInHz(int sampleRateInHz) {
    sSampleRateInHz = sampleRateInHz;
    // 音频采集Buffer最小Size
    int minBufferSize = AudioRecord.getMinBufferSize(
        sSampleRateInHz,
        sChannelConfig,
        sAudioFormat
    );
    // Audio Buffer Size
    int tmpSize = sSampleRateInHz / 10;
    sPCMBufferSize = Math.max(tmpSize, minBufferSize);
    // Audio Buffer
    sPCMBuffer = new short[sPCMBufferSize];
  }

  public void register(Handler handler) {
    sHandlers.add(handler);
  }

  public void unregister(Handler handler) {
    sHandlers.remove(handler);
  }

  /**
   * 获得当前状态
   *
   * @return STATE_IDLE | STATE_RECORDING | STATE_RECORD_PAUSED
   */
  public int getState() {
    return mState;
  }

  /**
   * 初始化音频录音管理器
   */
  public void init(BaseApplication application, String pCMBufferFileName,
                   int pcmUnitTimeIntervalInMillisecond,
                   RecordVolumeAgcConfig webRTCAgcConfig) {
    mApp = new SoftReference<>(application);
    final Context context = application.getApplicationContext();

    // 避免重复初始化
    if (!isInited) {
      sPCMBufferFileName = pCMBufferFileName;
      mWebRTCAgcConfig = webRTCAgcConfig;

      // 接收耳机麦克风插入情况
      if (mHeadsetPlugReceiver == null) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        mHeadsetPlugReceiver = new HeadsetPlugReceiver(new Runnable() {
          @Override
          public void run() {
            // 检查耳机麦克风是否插入
            checkHeadsetPlugState();
            // 通知HeadsetPlugConfigs已改变
            notifyRecordConfigs();
          }
        });
        application.registerReceiver(mHeadsetPlugReceiver, filter);
      }

      // WebRTC支持如下CPU架构['arm64-v8a']
      // 方法内调用了载入libjmedia2.so的代码，不需要重复载入
      // 检查结果记录在isCpuAbiSupport变量上，其他代码块可以使用isCpuAbiSupport这个变量
      checkCpuAbiSupport();

      // Notice Handler
      mNoticeHandler = new NoticeHandler(Looper.getMainLooper(), this);

      // 检查耳机麦克风是否插入
      checkHeadsetPlugState();

      // 启动PCMBuffer写入Thread
      final HandlerThread writeThread = new HandlerThread("PCMBufferWriteThread-" + System.currentTimeMillis());
      writeThread.start();
      mWritePCMHandler = new WritePCMHandler(writeThread.getLooper(),
        pCMBufferFileName, mNoticeHandler, isCpuAbiSupport);

      // 启动PCM计算Handler
      final HandlerThread calculateThread = new HandlerThread("CalculatePCMThread-" + System.currentTimeMillis());
      calculateThread.start();
      mCalculatePCMHandler = new CalculatePCMHandler(calculateThread.getLooper(),
          pcmUnitTimeIntervalInMillisecond, mWritePCMHandler, mNoticeHandler);

      // 启动录制音频的Thread
      final HandlerThread recordThread = new HandlerThread("AudioRecordThread-" + System.currentTimeMillis());
      recordThread.start();
      mAudioRecordHandler = new AudioRecordHandler(context, recordThread.getLooper(),
          mNoticeHandler, mWritePCMHandler, mCalculatePCMHandler, this,
          isCpuAbiSupport);

      // 通知HeadsetPlugConfigs已改变
      notifyRecordConfigs();

      // 标记已经初始化完成
      isInited = true;
    }
  }

  /**
   * 销毁音频录音管理器
   */
  public void destroy() {

    // 避免重复销毁
    if (isInited) {

      // 解注册耳机麦克风插入状态监听
      unregisterHeadsetPlugReceiver();

      // 停止语音操作工具
      Voice.getInstance().stop();

      // 释放录音设备
      stopAudioRecord();

      // 终止音频处理Thread
      exitAudioRecord();

      // 终止PCMBuffer写入Handler
      exitPCMBufferWriteHandler();

      // 终止PCM计算Handler
      exitCalculatePCMHandler();

//      // 销毁降噪
//      destroyAudioApm();
    }

    // 标记已经撤销初始化
    isInited = false;
  }

  @Override
  public void onLossAudioFocus() {
//    // 释放录音设备
//    stopAudioRecord();
    // notice
    if (mNoticeHandler != null) {
      mNoticeHandler.sendEmptyMessage(700);
    }
  }

  @Override
  public void onAudioRecorderStateChanged(int what, int arg1, int arg2, Object obj) {
    switch (what) {

      // 开启录音
      case 100:
      // 继续录音
      case 200: {
        mState = STATE_RECORDING;
      }
      break;

      // 暂停录音
      case 300: {
        mState = STATE_RECORD_PAUSED;
      }
      break;

      // 停止录音
      case 999: {
        mState = STATE_RECORD_STOPED;
      }
      break;

      // 录音mp3文件处理完成
      case 888: {
        mState = STATE_IDLE;
        // 通知所有停止监听器
        for (SoftReference<OnAudioRecorderStopListener> sl : sOnAudioRecorderStopListeners) {
          OnAudioRecorderStopListener listener = sl == null ? null : sl.get();
          if (listener != null) listener.onAudioRecorderStoped();
        }
        // 通知完成后，清空监听器列表
        sOnAudioRecorderStopListeners.clear();
        // 如果降噪开关打开
        if (mWebRTCAgcConfig != null && mWebRTCAgcConfig.isVoiceNSOpen()) {
          // 销毁降噪
          destroyAudioApm();
        }
      }
      break;
    }
  }

  // 包装语音识别监听器
  private OnVoiceRecognizerListener wrapOnVoiceRecognizerListener(final OnVoiceRecognizerListener listener) {
    return new OnVoiceRecognizerListener() {
      @Override
      public void onRecogStart(VoiceRecognizer recognizer) {
        if (listener != null) listener.onRecogStart(recognizer);
      }

      @Override
      public void onRecognition(VoiceRecognizer recognizer, String word) {
        if (listener != null) listener.onRecognition(recognizer, word);
      }

      @Override
      public void onRecogStop(VoiceRecognizer recognizer) {
        if (listener != null) listener.onRecogStop(recognizer);
      }

      @Override
      public void onRecogError(VoiceRecognizer recognizer, int code, String message) {
        if (listener != null) listener.onRecogError(recognizer, code, message);
      }

      @Override
      public void onRecogLast(VoiceRecognizer recognizer) {
        if (listener != null) listener.onRecogLast(recognizer);
      }

      @Override
      public void onRecogLog(VoiceRecognizer recognizer, String log) {
        if (mWritePCMHandler != null) mWritePCMHandler.addVoiceRecogLog(log);
        if (listener != null) listener.onRecogLog(recognizer, log);
      }
    };
  }

  // 启动录音
  public void startAudioRecord(String fileName) {
    startAudioRecord(fileName, null);
  }

  // 启动录音
  public void startAudioRecord(final String fileName,
                               OnVoiceRecognizerListener voiceRecognizerListener) {
    if (mOpable && mState == STATE_IDLE) {

      // 更新录音采用率，科大讯飞SDK暂时只支持16000
      // 更新录音采用率，不使用科大讯飞SDK时，改为默认的采用率44100
      if (voiceRecognizerListener != null) {
        updateSampleRateInHz(sKDXFSampleRateInHz);
      } else {
        updateSampleRateInHz(sDefaultSampleRateInHz);
      }

      // 如果降噪开关打开
      if (mWebRTCAgcConfig != null && mWebRTCAgcConfig.isVoiceNSOpen()) {
        // 销毁降噪
        destroyAudioApm();
        // 初始化降噪
        initAudioApm(sSampleRateInHz, mWebRTCAgcConfig.getVoiceNSMode());
      }

      // 重置PCM计算Handler
      resetCalculatePCMHandler();

      // 判断是否需要启动语音操作工具
      if (voiceRecognizerListener != null) {
        // 启动语音操作工具
        Voice.getInstance().setOnVoiceRecognizerListener(
          wrapOnVoiceRecognizerListener(voiceRecognizerListener));
        Voice.getInstance().start();
      }

      // 启动录音
      if (mAudioRecordHandler != null) {
        mAudioRecordHandler.start(fileName);
      }
    }
  }

  // 继续录音
  public void resumeAudioRecord() {
    if (mOpable && mState == STATE_RECORD_PAUSED) {
      if (mAudioRecordHandler != null) {
        mAudioRecordHandler.resume();
      }
    }
  }

  // 暂停录音
  public void pauseAudioRecord() {
    if (mOpable && mState == STATE_RECORDING) {
      if (mAudioRecordHandler != null) {
        mAudioRecordHandler.pause();
      }
    }
  }

  // 停止录音
  public void stopAudioRecord() {
    stopAudioRecord(true, null);
  }

  // 停止录音
  public void stopAudioRecord(OnAudioRecorderStopListener stopListener) {
    stopAudioRecord(true, stopListener);
  }

  // 停止录音
  public void stopAudioRecord(boolean stopVoice, OnAudioRecorderStopListener stopListener) {
    if (stopListener != null) {
      sOnAudioRecorderStopListeners.add(new SoftReference<>(stopListener));
    }
    if (mOpable && (mState == STATE_RECORDING || mState == STATE_RECORD_PAUSED)) {
      if (mAudioRecordHandler != null) {
        mAudioRecordHandler.stop();
      }
      if (stopVoice) {
        // 停止语音操作工具
        Voice.getInstance().setOnVoiceRecognizerListener(null);
        Voice.getInstance().stop();
      } else {
        Voice.getInstance().stopListening();
      }
    }
  }

  // 终止录音线程
  private void exitAudioRecord() {
    if (mOpable && mAudioRecordHandler != null) {
      mAudioRecordHandler.exit();
    }
  }

  // 判断是否正在录音
  public boolean isRecording() {
    return mState == STATE_RECORDING || mState == STATE_RECORD_PAUSED;
  }

  // 创建Wav文件，并写入数据
  public int createWavFileAndWritePCMData(String file, int sampleRate,
                                          byte[] data, int size) {
    if (isCpuAbiSupport) {
      return sMediaNUtils.createWavFileAndWritePCMData(
        file, sampleRate, data, size);
    } else {
      return 0;
    }
  }

  // 创建Mp3文件，并写入数据
  public int createMp3FileAndWritePCMData(String file, int sampleRate,
                                          byte[] data, int size) {
    if (isCpuAbiSupport) {
      return sMediaNUtils.createMp3FileAndWritePCMData(
              file, sampleRate, data, size);
    } else {
      return 0;
    }
  }

  // 降噪处理给定的Wav/Mp3文件
  public void processWavOrMp3FileWebRTC(String inFile, String outFile, int mode) {
    if (isCpuAbiSupport) {
      sMediaNUtils.webrtcFileProcess(inFile, outFile, mode);
    }
  }

  // 初始化WebRTC降噪功能
  public void initWebRTCAgc(int sample, int mode) {
    // 销毁降噪
    destroyAudioApm();
    // 初始化降噪
    initAudioApm(sample, mode);
  }

  // 销毁WebRTC降噪功能
  public void destroyWebRTCAgc() {
    // 销毁降噪
    destroyAudioApm();
  }

  // WebRTC降噪处理
  public void processWebRTCAgc(byte[] buffer, int size) {
    if (isCpuAbiSupport) {
      short[] sBuf = new short[size >> 1];
      for (int i = 0; i < sBuf.length; i++) {
        sBuf[i] = (short) ((buffer[i * 2] & 0xff) +
                          ((buffer[i * 2 + 1] & 0xff) << 8));
      }

      AudioRecordHandler.processWebRTCAgc(sMediaNUtils, sBuf, sBuf.length);

      int count = sBuf.length;
      for (int i = 0; i < count; i++) {
        buffer[i * 2] = (byte) (sBuf[i]); // sBuf[i] >> 0
        buffer[i * 2 + 1] = (byte) (sBuf[i] >> 8);
      }
    }
  }

  // WebRTC降噪处理
  public void processWebRTCAgc(short[] buffer, int size) {
    if (isCpuAbiSupport) {
      AudioRecordHandler.processWebRTCAgc(sMediaNUtils, buffer, size);
    }
  }

  // 更新WebRTCAgcConfig - 音量增益配置
  public void updateWebRTCAgcConfig(RecordVolumeAgcConfig config) {
    mWebRTCAgcConfig = config;
    // 通知RecordConfigs已改变
    notifyRecordConfigs();
  }

  // VolumeAdjustOpen开关
  public void switchPCMAdjustVolumeOpen(boolean open) {
    if (mWebRTCAgcConfig != null) {
      mWebRTCAgcConfig.mPCMVolumeAdjustOpen = open;
      // 通知RecordConfigs已改变
      notifyRecordConfigs();
    }
  }

  // VoiceNS开关
  public void switchVoiceNSOpen(boolean open) {
    if (mWebRTCAgcConfig != null) {
      mWebRTCAgcConfig.mVoiceNSOpen = open;
      // 通知RecordConfigs已改变
      notifyRecordConfigs();
    }
  }

  // 更新PCMAdjustVolumeFactor
  public void updatePCMAdjustVolumeFactor(float normalFactor, float headsetFactor) {
    if (mWebRTCAgcConfig != null) {
      mWebRTCAgcConfig.mPCMAdjustVolumeNormalFactor = Math.max(Math.min(normalFactor, 10f), 1f);
      mWebRTCAgcConfig.mPCMAdjustVolumeHeadsetFactor = Math.max(Math.min(headsetFactor, 10f), 1f);
      // 通知RecordConfigs已改变
      notifyRecordConfigs();
    }
  }

  // PCM音量增益
  public void pcmAdjustVolume(byte[] buffer, int size, float factor) {
    short[] sBuf = new short[size >> 1];
    for (int i = 0; i < sBuf.length; i++) {
      sBuf[i] = (short) ((buffer[i * 2] & 0xff) +
                        ((buffer[i * 2 + 1] & 0xff) << 8));
    }

    MediaNUtils.pcmAdjustVolume(sBuf, sBuf.length, factor);

    int count = sBuf.length;
    for (int i = 0; i < count; i++) {
      buffer[i * 2] = (byte) (sBuf[i]); // sBuf[i] >> 0
      buffer[i * 2 + 1] = (byte) (sBuf[i] >> 8);
    }
  }

  // PCM音量增益
  public void pcmAdjustVolume(short[] buffer, int size, float factor) {
    MediaNUtils.pcmAdjustVolume(buffer, size, factor);
  }

  // 通知RecordConfigs已改变
  private void notifyRecordConfigs() {
    if (mWritePCMHandler != null) {
      mWritePCMHandler.setVolumeAdjustConfig(
        mHeadsetPlugType,
        // 0:默认内置麦克风
        // 1:有线耳机麦克风
        // 2:蓝牙耳机麦克风
        mHeadsetPlugType == 1 ?
          mWebRTCAgcConfig == null ? 1f : mWebRTCAgcConfig.getPCMAdjustVolumeHeadsetFactor():
          mWebRTCAgcConfig == null ? 1f : mWebRTCAgcConfig.getPCMAdjustVolumeNormalFactor(),
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isPCMVolumeAdjustOpen(),
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isVoiceNSOpen(),
              mWebRTCAgcConfig == null ? 0 : mWebRTCAgcConfig.getVoiceNSMode());
    }
    if (mAudioRecordHandler != null) {
      mAudioRecordHandler.setVolumeAdjustConfig(
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isCheckBluetoothSco(),
        mHeadsetPlugType,
        // 0:默认内置麦克风
        // 1:有线耳机麦克风
        // 2:蓝牙耳机麦克风
        mHeadsetPlugType == 1 ?
          mWebRTCAgcConfig == null ? 1f : mWebRTCAgcConfig.getPCMAdjustVolumeHeadsetFactor():
          mWebRTCAgcConfig == null ? 1f : mWebRTCAgcConfig.getPCMAdjustVolumeNormalFactor(),
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isPCMVolumeAdjustOpen(),
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isVoiceNSOpen());
    }
    if (mCalculatePCMHandler != null) {
      mCalculatePCMHandler.setPCMMaxVolumedB(
        mWebRTCAgcConfig == null ? 0 : mWebRTCAgcConfig.getPCMMaxVolumedB());
    }
  }

  // 重新转写PCM文件
  public void voiceRecognizePCMFile(final OnVoiceRecognizerListener listener) {
    if (listener == null) {
      return;
    }

    // 判断是否正常录音
    if (isRecording()) {
      return;
    }

    // 判断是否正在进行重新转写
    if (mAgainOnVoiceRecognizerListener != null) {
      return;
    }

    // 判断是否存在PCM文件
    final File file = new File(sPCMBufferFileName);
    if (!file.exists()) {
      return;
    }

    // 生成新的转写监听器
    mAgainOnVoiceRecognizerListener = new OnVoiceRecognizerListener() {
      @Override
      public void onRecogStart(VoiceRecognizer recognizer) {
        listener.onRecogStart(recognizer);
      }

      @Override
      public void onRecognition(VoiceRecognizer recognizer, String word) {
        listener.onRecognition(recognizer, word);
      }

      @Override
      public void onRecogLog(VoiceRecognizer recognizer, String result) {
        listener.onRecogLog(recognizer, result);
      }

      @Override
      public void onRecogStop(VoiceRecognizer recognizer) {
        listener.onRecogStop(recognizer);
      }

      @Override
      public void onRecogError(VoiceRecognizer recognizer, int code, String message) {
        listener.onRecogError(recognizer, code, message);

        // PCM文件读取完成
        if (recognizer == VoiceRecognizer.NONE && code == -1) {
          // 转写停止
          mAgainOnVoiceRecognizerListener = null;
        }
      }

      @Override
      public void onRecogLast(VoiceRecognizer recognizer) {
        listener.onRecogLast(recognizer);
      }
    };

    // 启动语音转写
    Voice.getInstance().setOnVoiceRecognizerListener(mAgainOnVoiceRecognizerListener);
    Voice.getInstance().start();

    // 启动新线程读取PCM文件
    // 启动PCMBuffer读取Thread
    try {
      final HandlerThread readThread = new HandlerThread("PCMBufferReadThread-" + System.currentTimeMillis());
      readThread.start();
      final ReadPCMHandler readPCMHandler = new ReadPCMHandler(readThread.getLooper());
      readPCMHandler.start(new FileInputStream(file), mAgainOnVoiceRecognizerListener);
    } catch (FileNotFoundException ignored) { }
  }

  // 向录音单位时间内平均分贝日志文件中追加内容
  public void appendContentIntoPCMAdBFile(String publicKey,
                                          String mp3FileName,
                                          String content,
                                          AppendDataCallback completedCallback) {
    if (TextUtils.isEmpty(content)) {
      return;
    }

    if (mWritePCMHandler == null) {
      return;
    }

    mWritePCMHandler.appendContentIntoPCMAdBFile(
      publicKey, mp3FileName, content, completedCallback);
  }

  // 向录音语音转写日志文件中追加内容
  public void appendContentIntoVoiceRecogLog(String publicKey,
                                             String mp3FileName,
                                             String content,
                                             AppendDataCallback completedCallback) {
    if (TextUtils.isEmpty(content)) {
      return;
    }

    if (mWritePCMHandler == null) {
      return;
    }

    mWritePCMHandler.appendContentIntoVoiceRecogLog(
      publicKey, mp3FileName, content, completedCallback);
  }

  /**
   * 检查权限
   */
  public boolean checkPermissions() {
    return checkPermissions((BaseActivity) null);
  }

  /**
   * 检查权限
   */
  public boolean checkPermissions(BaseFragment fragment) {
    if (fragment != null) {
      return checkPermissions((BaseActivity) fragment.getActivity());
    } else {
      return false;
    }
  }

  /**
   * 检查权限
   */
  public boolean checkPermissions(BaseActivity activity) {
    // 检查录音权限
    if (checkAudioRecordPermission(activity)) {
      // 检查存储卡读取权限
      if (checkStoragePermission(activity)) {
        mOpable = true;
        onUpdateAfterPermissions();
        return true;
      }
    }
    // 没有相应权限
    return false;
  }

  /**
   * 获取当前正常进行的录音文件的时长
   */
  public long getPCMFileDuration() {
    if (mAudioRecordHandler == null) {
      return 0;
    } else {
      return mAudioRecordHandler.getPCMFileDuration();
    }
  }

  // 得到权限后的一些操作
  private void onUpdateAfterPermissions() {
    if (mNoticeHandler != null) {
      mNoticeHandler.sendEmptyMessage(WHAT_HAS_PERMISSIONS);
    }
  }

  // 检查录音权限
  private boolean checkAudioRecordPermission(BaseActivity activity) {
    return Permission.hasRecorderPermission(new OnCheckPermissionImpl(activity));
  }

  // 检查SD权限
  private boolean checkStoragePermission(BaseActivity activity) {
    return Permission.hasStoragePermission(new OnCheckPermissionImpl(activity));
  }

  // WebRTC支持如下CPU架构['arm64-v8a']
  // 方法内调用了载入libjmedia2.so的代码，不需要重复载入
  // 检查结果记录在isCpuAbiSupport变量上，其他代码块可以使用isCpuAbiSupport这个变量
  private void checkCpuAbiSupport() {
    final List<String> abis = new ArrayList<>();
    abis.add("arm64-v8a");
    final String abi_ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
        Build.SUPPORTED_ABIS[0] : Build.CPU_ABI;
    final String abi = abi_ == null ? "" : abi_.toLowerCase();
    final boolean cpuSupport = abis.contains(abi);
    // 需要判断是否成功载入libjmedia3.so/libjmedia4.so
    if (MediaNUtils3.getInstance().init()) {
      sMediaNUtils = MediaNUtils3.getInstance();
    } else if (MediaNUtils4.getInstance().init()) {
      sMediaNUtils = MediaNUtils4.getInstance();
    } else {
      sMediaNUtils = null;
    }
    isCpuAbiSupport = cpuSupport && sMediaNUtils != null;
  }

  // 初始化降噪
  private void initAudioApm(int sample, int mode) {
    if (isCpuAbiSupport) {
      if (sMediaNUtils.webrtcCreateApm()) {
        Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS create success.");
        if (sample == 16000) {
          if (sMediaNUtils.webrtcSetupApm16K(mode)) {
            Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS setup 16K success.");
          } else {
            Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS setup 16K fail.");
          }
        } else if (sample == 44100) {
          if (sMediaNUtils.webrtcSetupApm441K(mode)) {
            Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS setup 441K success.");
          } else {
            Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS setup 441K fail.");
          }
        }
      } else {
        Logger.e("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS create fail.");
      }
    }
  }

  // 销毁降噪
  private void destroyAudioApm() {
    if (isCpuAbiSupport) {
      sMediaNUtils.webrtcFreeApm();
    }
  }

  // 终止PCMBuffer写入Handler
  private void exitPCMBufferWriteHandler() {
    if (mWritePCMHandler != null) {
      mWritePCMHandler.exit();
    }
  }

  // 重置PCM计算Handler
  private void resetCalculatePCMHandler() {
    if (mCalculatePCMHandler != null) {
      mCalculatePCMHandler.reset();
    }
  }

  // 获得当前PCM平均分贝
  public double getCurrentAveragePCMdB() {
    if (mCalculatePCMHandler == null) {
      return 0;
    }

    return mCalculatePCMHandler.getAveragePCMdB();
  }

  // 终止PCM计算Handler
  private void exitCalculatePCMHandler() {
    if (mCalculatePCMHandler != null) {
      mCalculatePCMHandler.exit();
    }
  }

  public void setCalculateSecPCMdBShowMins(int minCount, int mindB) {
    if (mCalculatePCMHandler != null) {
      mCalculatePCMHandler.setSecPCMdBShowMins(minCount, mindB);
    }
  }

  // 分解SecPCMdBShowCount
  public int[] resolveSecPCMdBShowCount(int count) {
    if (mCalculatePCMHandler != null) {
      return mCalculatePCMHandler.resolveSecPCMdBShowCount(count);
    } else {
      return new int[] {0, 0};
    }
  }

  // 检查耳机麦克风是否插入
  private void checkHeadsetPlugState() {
    Context context = mApp == null ? null : mApp.get();
    checkHeadsetPlugState(context);
  }

  // 检查耳机麦克风是否插入
  public void checkHeadsetPlugState(Context context) {
    // 0:默认内置麦克风
    // 1:有线耳机麦克风
    // 2:蓝牙耳机麦克风
    int headsetPlugType = 0;
    if (context != null) {
      AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      if (manager != null) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
          AudioDeviceInfo[] devices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS);
          for (AudioDeviceInfo device : devices) {
            Logger.d("HeadsetPlugReceiver", "device: " + device.getType());
            if (device.getType() ==
              // A device type describing a headset, which is the combination of a headphones and microphone.
              // 描述耳机的一种设备类型，它是耳机和麦克风的组合。
              AudioDeviceInfo.TYPE_WIRED_HEADSET) {
              headsetPlugType = 1;
            } else if (device.getType() ==
              // A device type describing a Bluetooth device typically used for telephony.
              // 描述通常用于电话的蓝牙设备的设备类型。
              AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
              headsetPlugType = 2;
            }
          }
        } else {
          headsetPlugType = manager.isWiredHeadsetOn() ? 1 : 0;
        }
      }
    }

    mHeadsetPlugType = headsetPlugType;
    if (mNoticeHandler != null) {
      Message msg = mNoticeHandler.obtainMessage();
      msg.what = WHAT_HEADSET_PLUG_CHANGED;
      msg.arg1 = mHeadsetPlugType;
      mNoticeHandler.sendMessage(msg);
    }
  }

  /*
   * 获得耳机麦克风类型
   *   0:默认内置麦克风
   *   1:有线耳机麦克风
   *   2:蓝牙耳机麦克风
   */
  public int getHeadsetPlugType() {
    return mHeadsetPlugType;
  }

  public int getRecordSource() {
    if (mAudioRecordHandler == null) {
      return sAudioSourceMic;
    }

    return mAudioRecordHandler.getRecordSource();
  }

  // 解注册耳机麦克风插入状态监听
  private void unregisterHeadsetPlugReceiver() {
    if (mHeadsetPlugReceiver != null &&
      mApp != null && mApp.get() != null) {
      mApp.get().unregisterReceiver(mHeadsetPlugReceiver);
    }
    mHeadsetPlugReceiver = null;
  }

  // 检查设备是否支持BluetoothSco
  public static boolean isSupportBluetoothSco(Context context) {
    if (context == null) {
      return false;
    }

    AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    return manager != null && manager.isBluetoothScoAvailableOffCall();
  }



  // 检查BluetoothSco
  public static boolean checkBluetoothSco(Context context) {
    if (context == null) {
      return false;
    }

    AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    if (manager == null) {
      return false;
    }

    if (!manager.isBluetoothScoAvailableOffCall()) {
      return false;
    }

    if (manager.isBluetoothScoOn()) {
      return true;
    }

    manager.startBluetoothSco();
    long timeout = 10;
    while (!manager.isBluetoothScoOn() && timeout-- > 0) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (timeout == 50) {
        manager.startBluetoothSco();
      }
    }

    return manager.isBluetoothScoOn();
  }

  // 尝试停止BluetoothSco
  public static void tryStopBluetoothSco(Context context) {
    if (context == null) {
      return;
    }

    AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    if (manager == null) {
      return;
    }

    if (!manager.isBluetoothScoAvailableOffCall()) {
      return;
    }

    manager.stopBluetoothSco();
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class HeadsetPlugReceiver extends BroadcastReceiver {
    private Runnable mOnChangedCallback;

    public HeadsetPlugReceiver(Runnable onChangedCallback) {
      mOnChangedCallback = onChangedCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      Logger.d("HeadsetPlugReceiver", intent.getAction());
      if (mOnChangedCallback != null) mOnChangedCallback.run();
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  public static class RecordVolumeAgcConfig {
    private int mMinLevel;
    private int mMaxLevel;
    private int mCompressionGaindB;
    private boolean mLimiterEnable;
    private int mTargetLevelDbfs;
    private boolean mCheckBluetoothSco;
    private float mPCMAdjustVolumeNormalFactor;
    private float mPCMAdjustVolumeHeadsetFactor;
    private float mPCMMaxVolumedB;
    private boolean mPCMVolumeAdjustOpen;
    private boolean mVoiceNSOpen;
    private int mVoiceNSMode;
    private String mFFmpegParams;

    public RecordVolumeAgcConfig(int minLevel, int maxLevel,
                                 int compressionGaindB,
                                 boolean limiterEnable,
                                 int targetLevelDbfs,
                                 boolean checkBluetoothSco,
                                 float pCMAdjustVolumeNormalFactor,
                                 float pCMAdjustVolumeHeadsetFactor,
                                 float pCMMaxVolumedB,
                                 boolean pCMAdjustVolumeOpen,
                                 boolean voiceNSOpen,
                                 int voiceNSMode,
                                 String ffmpegParams) {
      mMinLevel = minLevel;
      mMaxLevel = maxLevel;
      mCompressionGaindB = compressionGaindB;
      mLimiterEnable = limiterEnable;
      mTargetLevelDbfs = targetLevelDbfs;
      mCheckBluetoothSco = checkBluetoothSco;
      mPCMAdjustVolumeNormalFactor = pCMAdjustVolumeNormalFactor;
      mPCMAdjustVolumeHeadsetFactor = pCMAdjustVolumeHeadsetFactor;
      mPCMMaxVolumedB = pCMMaxVolumedB;
      mPCMVolumeAdjustOpen = pCMAdjustVolumeOpen;
      mVoiceNSOpen = voiceNSOpen;
      mVoiceNSMode = voiceNSMode;
      mFFmpegParams = ffmpegParams;
    }

    public int getMinLevel() {
      return mMinLevel;
    }

    public int getMaxLevel() {
      return mMaxLevel;
    }

    public int getCompressionGaindB() {
      return mCompressionGaindB;
    }

    public int getTargetLevelDbfs() {
      return mTargetLevelDbfs;
    }

    public boolean isLimiterEnable() {
      return mLimiterEnable;
    }

    public boolean isCheckBluetoothSco() {
      return mCheckBluetoothSco;
    }

    public float getPCMAdjustVolumeNormalFactor() {
      return mPCMAdjustVolumeNormalFactor;
    }

    public float getPCMAdjustVolumeHeadsetFactor() {
      return mPCMAdjustVolumeHeadsetFactor;
    }

    public float getPCMMaxVolumedB() {
      return mPCMMaxVolumedB;
    }

    public boolean isPCMVolumeAdjustOpen() {
      return mPCMVolumeAdjustOpen;
    }

    public boolean isVoiceNSOpen() {
      return mVoiceNSOpen;
    }

    // Allow for modes: 0, 1, 2, 3.
    public int getVoiceNSMode() {
      return mVoiceNSMode;
    }

    public String getFFmpegParams() {
      return mFFmpegParams;
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class NoticeHandler extends Handler {
    private String mFileName;
    private SoftReference<AudioRecorderStateListener> mListener;

    NoticeHandler(Looper looper, AudioRecorderStateListener listener) {
      super(looper);
      mListener = new SoftReference<>(listener);
    }

    // 设置音频录音的文件全路径
    public void setFileName(String fileName) {
      mFileName = fileName;
    }

    @Override
    public void handleMessage(@NotNull Message msg) {
      AudioRecorderStateListener listener = mListener.get();
      if (listener != null) {
        listener.onAudioRecorderStateChanged(
            msg.what,
            msg.arg1,
            msg.arg2,
            msg.obj
        );
      }
      try {
        int what = msg.what;
        String jsonParams = null;
        switch (msg.what) {

          // 获得应有的权限
          case WHAT_HAS_PERMISSIONS: {
            what = WHAT_HAS_PERMISSIONS;
          }
          break;

          // 开启录音
          case 100: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Started");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // 继续录音
          case 200: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Resumed");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // 暂停录音
          case 300: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Paused");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // 来电后停止录音
          case 700: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "LossAudioFocus");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // 停止录音
          case 999: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Stoped");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          /////////////////////////////////////////////////
          // 录音读取过程
          case 666: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Read");
            json.put("arg1", msg.arg1);
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // 读取录音失败
          case 900: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Error");
            json.put("arg1", msg.arg1);
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          /////////////////////////////////////////////////
          // 处理录音 - 开始
          case 801: {
            what = WHAT_PROCESS_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "ProcessStarted");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // 处理录音 - 处理过程
          case 810: {
            what = WHAT_PROCESS_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Processing");
            json.put("percent", msg.arg1 * 100 / (float) msg.arg2);
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // 处理录音 - 完成
          case 888: {
            what = WHAT_PROCESS_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "ProcessCompleted");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          /////////////////////////////////////////////////
          // 音频单位时间内平均分贝
          case WHAT_RECORD_dB_CHANGED: {
            what = WHAT_RECORD_dB_CHANGED;
          }
          break;

          /////////////////////////////////////////////////
          // 设备耳机等外设插入拔出状态变更
          case WHAT_HEADSET_PLUG_CHANGED: {
            what = WHAT_HEADSET_PLUG_CHANGED;
          }
          break;
        }

        // 发送消息
        sendNoticeMessage(what, msg.arg1, msg.arg2, jsonParams);
      } catch (JSONException | UnsupportedEncodingException ignored) {
      }
    }

    private void setupMsgDefaultValues(Message msg, JSONObject json)
        throws JSONException, UnsupportedEncodingException {
      if (!TextUtils.isEmpty(mFileName)) {
        json.put("data", URLEncoder.encode(mFileName, "utf-8"));
      }
      if (msg.obj != null) {
        json.put("obj", msg.obj);
      }
    }

    private void sendNoticeMessage(int what, int arg1, int arg2, Object obj) {
      for (int i = 0; i < sHandlers.size(); i++) {
        Handler handler = sHandlers.get(i);
        if (handler != null) {
          Message notice = handler.obtainMessage(what);
          notice.arg1 = arg1;
          notice.arg2 = arg2;
          notice.obj = obj;
          handler.sendMessage(notice);
        }
      }
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class AudioRecordHandler extends Handler
      implements AudioRecord.OnRecordPositionUpdateListener, JRecorder.StateChangedListener {
    private SoftReference<Context> mContext;
    // Notice Handler
    private NoticeHandler mNoticeHandler;
    // Audio Record
    private JRecorder mRecorder;
    // JRecorder State Changed Listener
    private JRecorder.StateChangedListener mStateChangedListener;
    // PCM Buffer Write Handler
    private WritePCMHandler mWritePCMHandler;
    // PCM Calculate Handler
    private CalculatePCMHandler mCalculatePCMHandler;
    // Check CPU ABI Support
    private boolean isCpuAbiSupport;
    // 判断是否要尝试BluetoothSco
    private boolean mCheckBluetoothSco;
    // 判断是否打开语音降噪
    private boolean mVoiceNSOpen;
    // 记录着耳机麦克风是否插入
    // 0:默认内置麦克风
    // 1:有线耳机麦克风
    // 2:蓝牙耳机麦克风
    private int mHeadsetPlugType = 0;
    private int mRecordSource = sAudioSourceMic;
    private float mVolumeAdjustFactor = 1f;
    private boolean mVolumeAdjustOpen = false;
    // 标记是否已经被停止
    private boolean isRequestStop;
    // 记录下录音PCM文件写入的长度
    private long mPCMFileLength = 0;
    // 记录下录音PCM文件时长，单位：秒
    private long mPCMFileDuration = 0;

    AudioRecordHandler(Context context, Looper looper, NoticeHandler noticeHandler,
                       WritePCMHandler writePCMHandler,
                       CalculatePCMHandler calculatePCMHandler,
                       JRecorder.StateChangedListener stateChangedListener,
                       boolean support) {
      super(looper);
      mContext = new SoftReference<>(context);
      mNoticeHandler = noticeHandler;
      mWritePCMHandler = writePCMHandler;
      mCalculatePCMHandler = calculatePCMHandler;
      mStateChangedListener = stateChangedListener;
      isCpuAbiSupport = support;
    }

    void start(String fileName) {
      mPCMFileLength = 0;
      mPCMFileDuration = 0;
      isRequestStop = false;
      Message msg = obtainMessage(100);
      msg.obj = fileName;
      sendMessage(msg);
    }

    void resume() {
      isRequestStop = false;
      sendEmptyMessage(200);
    }

    void pause() {
      isRequestStop = false;
      sendEmptyMessage(300);
    }

    void stop() {
      mPCMFileLength = 0;
      mPCMFileDuration = 0;
      isRequestStop = true;
      removeCallbacksAndMessages(null);
      sendEmptyMessage(999);
    }

    void exit() {
      mPCMFileLength = 0;
      mPCMFileDuration = 0;
      if (!isRequestStop) removeCallbacksAndMessages(null);
      isRequestStop = true;
      sendEmptyMessage(0);
    }

    void setVolumeAdjustConfig(boolean checkBluetoothSco,
                               int headsetPlugType, float factor,
                               boolean volumeOpen,
                               boolean voiceNSOpen) {
      mCheckBluetoothSco = checkBluetoothSco;
      mHeadsetPlugType = headsetPlugType;
      mVolumeAdjustFactor = factor;
      mVolumeAdjustOpen = volumeOpen;
      mVoiceNSOpen = voiceNSOpen;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
      // nothing
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
      // nothing
    }

    private void writePCMBufferToFile(short[] rawData, int readSize) {
        if (mWritePCMHandler != null) {
          mWritePCMHandler.addPCMBufferWrite(rawData, readSize);
        }
    }

    int getRecordSource() {
      return mRecordSource;
    }

    private boolean start(int what, String name) {
      final Context context = mContext == null ? null : mContext.get();
      if (context != null && mRecorder == null) {
        // 0:默认内置麦克风
        // 1:有线耳机麦克风
        // 2:蓝牙耳机麦克风
        if (mCheckBluetoothSco) {
          mRecordSource = mHeadsetPlugType == 2 ? (
            // 检查BluetoothSco
            checkBluetoothSco(context) ? sAudioSourceVcm : sAudioSourceMic
          ) : sAudioSourceMic;
        } else {
          mRecordSource = sAudioSourceMic;
        }
        // 通知日志记录器选择的音频源
        if (mWritePCMHandler != null) {
          mWritePCMHandler.setRecordSource(mRecordSource);
        }
        // 初始化JRecorder
        mRecorder = new JRecorder(
          context,
          mRecordSource,
          sSampleRateInHz,
          sChannelConfig,
          sAudioFormat,
          sPCMBufferSize);
        mRecorder.setStateChangedListener(this);
        mRecorder.setRecordPositionUpdateListener(this, this);
        mRecorder.setPositionNotificationPeriod(FRAME_COUNT);
        mRecorder.startRecording();
        if (mNoticeHandler != null) {
          mNoticeHandler.sendEmptyMessage(what);
        }
        // 记录到_adB.txt
        if (mWritePCMHandler != null) {
          mWritePCMHandler.addErrorInfo(name + ":true");
        }
        return true;
      } else {
        // 记录到_adB.txt
        if (mWritePCMHandler != null) {
          mWritePCMHandler.addErrorInfo(name + ":false");
        }
        return false;
      }
    }

    private void read() {
      if (!isRequestStop && mRecorder != null) {
        int readSize = mRecorder.read(sPCMBuffer, 0, sPCMBufferSize);
        if (!isRequestStop) {
          if (readSize >= 0) {
            if (readSize > 0) {
              // 音量增益打开并且增益倍数大于1
              if (mVolumeAdjustOpen && mVolumeAdjustFactor > 1f) {
                MediaNUtils.pcmAdjustVolume(sPCMBuffer, readSize, mVolumeAdjustFactor);
              }
//              // 判断是否需要打开音频降噪
//              if (isCpuAbiSupport && mVoiceNSOpen) {
//                // WebRTC降噪
//                processWebRTCAgc(sMediaNUtils, sPCMBuffer, readSize);
//              }
              // 将PCM数据写入文件
              writePCMBufferToFile(sPCMBuffer, readSize);
              // 计算PCM
              calculatePCM(sPCMBuffer, readSize);
            }
            // 循环读取
            sendEmptyMessage(666);
          } else {
            if (mNoticeHandler != null) {
              Message msg = mNoticeHandler.obtainMessage(900);
              msg.arg1 = readSize;
              mNoticeHandler.sendMessage(msg);
            }
            // 读取录音失败
            sendEmptyMessage(999);
          }
        }
      }
    }

    private void pauseRecorder() {
      if (mRecorder != null) {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
      }

      // 尝试停止BluetoothSco
      if (mCheckBluetoothSco) {
        tryStopBluetoothSco(mContext == null ? null : mContext.get());
      }

      // 通知录音暂停
      if (mNoticeHandler != null) {
        mNoticeHandler.sendEmptyMessage(300);
      }
    }

    private void stopRecorder() {
      if (mRecorder != null) {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
      }

      // 尝试停止BluetoothSco
      if (mCheckBluetoothSco) {
        tryStopBluetoothSco(mContext == null ? null : mContext.get());
      }

      // 通知录音暂停或停止
      if (mNoticeHandler != null) {
        mNoticeHandler.sendEmptyMessage(999);
      }

      // 停止录音
      // 通知写入、mp3编码器停止工作
      if (mWritePCMHandler != null) {
        mWritePCMHandler.stopRecord();
      }
    }

    static int calculatePCMUnitDelayMs(int sampleRateInHz, int readSize) {
      float bLength = readSize << 1; // bytes
      return (int) ((bLength / ((float)sampleRateInHz) / 2.0f) * 1000);
    }

    static short[] s16KNSBuf = new short[160];

    static void processWebRTCAgc(IMediaNUtils mediaNUtils, short[] buffer, int size) {
      if (mediaNUtils != null) {
        try {
          int res = mediaNUtils.webrtcProcessStream(buffer);
          Logger.d("WebRTC", "process(" + mediaNUtils.desc() + "): " + res);
        } catch (Throwable e) {
          Logger.d("WebRTC", "process(" + mediaNUtils.desc() + ") except: " + e.getMessage());
        }
      }
    }

    private void calculatePCM(short[] rawData, int readSize) {
      // 计算PCM分贝值
      if (!isRequestStop && mCalculatePCMHandler != null) {
        mCalculatePCMHandler.addPCMBufferWrite(rawData, readSize);
      }
      // 记录下PCM总长度
      mPCMFileLength += readSize << 1; // bytes
      // 计算出PCM时长，单位：秒
      // 数据量Byte = 采样频率Hz ×（采样位数/8）× 声道数 × 时间s
      // ！！！需要对应！！！ - 当前配置如下
      //    AudioRecorderManager.sAudioFormat   = AudioFormat.ENCODING_PCM_16BIT
      //    AudioRecorderManager.sChannelConfig = AudioFormat.CHANNEL_IN_MONO
      mPCMFileDuration = mPCMFileLength / sSampleRateInHz / 2;
    }

    private long getPCMFileDuration() {
      return mPCMFileDuration;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {

        // 终止线程
        case 0: {
          // 记录到_adB.txt
          if (mWritePCMHandler != null) {
            mWritePCMHandler.addErrorInfo("exit");
          }
          // quit
          getLooper().quitSafely();
        }
        break;

        // 开启录音
        case 100: {
          if (mWritePCMHandler != null) {
            final String fileName = msg.obj instanceof String ? (String) msg.obj : "";
            // 记录到_adB.txt
            mWritePCMHandler.addErrorInfo("reset");
            // reset
            mWritePCMHandler.reset(fileName, () -> {
              // 记录到_adB.txt
              mWritePCMHandler.addErrorInfo("start:" + fileName);
              // start
              if (start(100, "start")) { read(); }
            });
          }
        }
        break;

        // 继续录音
        case 200: {
          // 记录到_adB.txt
          if (mWritePCMHandler != null) {
            mWritePCMHandler.addErrorInfo("resume");
          }
          // start - resume
          if (start(200, "resume")) { read(); }
        }
        break;

        // 读取录音
        case 666: {
          read();
        }
        break;

        // 暂停录音
        case 300: {
          pauseRecorder();
          // 记录到_adB.txt
          if (mWritePCMHandler != null) {
            mWritePCMHandler.addErrorInfo("pause");
          }
        }
        break;

        // 停止录音
        case 999: {
          stopRecorder();
          // 记录到_adB.txt
          if (mWritePCMHandler != null) {
            mWritePCMHandler.addErrorInfo("stop");
          }
        }
        break;
      }
    }

    @Override
    public void onLossAudioFocus() {
      if (mStateChangedListener != null) {
        mStateChangedListener.onLossAudioFocus();
      }
      // 记录到_adB.txt
      if (mWritePCMHandler != null) {
        mWritePCMHandler.addErrorInfo("loss_audio_focus");
      }
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class WritePCMHandler extends Handler {
    private Runnable mResetCallback;
    // Check CPU ABI Support
    private boolean isCpuAbiSupport;
    private String mPCMBufferFileName;
    private RandomAccessFile mPCMBufferFile;
    private RandomAccessFile mPCMAdBFile;
    private Queue<PCMBuffer> mBufferPool = new ConcurrentLinkedQueue<>();
    private long mPCMBufferOffset = 0;
    private byte[] mp3Buffer = null;
    private File mAudioMp3File;
    private RandomAccessFile mAudioMp3FileWriter;
    private char[] mAdBPoints = new char[60];
    // 记录着耳机麦克风是否插入
    // 0:默认内置麦克风
    // 1:有线耳机麦克风
    // 2:蓝牙耳机麦克风
    private int mHeadsetPlugType = 0;
    private int mRecordSource = sAudioSourceMic;
    private float mVolumeAdjustFactor = 1f;
    private boolean mVolumeAdjustOpen = false;
    private boolean mVoiceNSOpen = false;
    private int mVoiceNSMode = 0;
    private RandomAccessFile mVoiceRecogLogFile;
    // 通知
    private Handler mNoticeHandler;

    WritePCMHandler(Looper looper, String pCMBufferFileName,
                    Handler noticeHandler, boolean support) {
      super(looper);
      mPCMBufferFileName = pCMBufferFileName;
      mNoticeHandler = noticeHandler;
      isCpuAbiSupport = support;
    }

    void reset(String fileName, Runnable callback) {
      mResetCallback = callback;
      Message msg = obtainMessage(10);
      msg.obj = fileName;
      sendMessage(msg);
    }

    PCMBuffer obtainBuffer() {
      PCMBuffer buffer = mBufferPool.poll();
      if (buffer == null) {
        buffer = new PCMBuffer(sPCMBufferSize);
        Logger.d("WritePCMHandler", "Crate PCMBuffer");
      }
      return buffer;
    }

    void addPCMBufferWrite(short[] rawData, int readSize) {
      PCMBuffer buffer = obtainBuffer();
      buffer.copyFrom(rawData, mPCMBufferOffset, readSize);
      mPCMBufferOffset += readSize;
      Message msg = obtainMessage(100);
      msg.obj = buffer;
      sendMessage(msg);
    }

    void stopRecord() {
      sendEmptyMessage(999);
    }

    void addUnitAdB(double dB) {
      Message msg = obtainMessage(200);
      msg.obj = dB;
      sendMessage(msg);
    }

    void addSoHighAdB(double dB) {
      Message msg = obtainMessage(200);
      msg.arg1 = WHAT_RECORD_dB_SO_HIGH;
      msg.obj = dB;
      sendMessage(msg);
    }

    void addErrorInfo(String error) {
      Message msg = obtainMessage(200);
      msg.arg1 = WHAT_RECORD_ERROR_INFO;
      msg.obj = error;
      sendMessage(msg);
    }

    void appendContentIntoPCMAdBFile(String publicKey,
                                     String mp3FileName,
                                     String content,
                                     AppendDataCallback completedCallback) {
      AppendData data = new AppendData();
      data.publicKey = publicKey;
      data.mp3FileName = mp3FileName;
      data.content = content;
      data.callback = completedCallback;
      Message msg = obtainMessage(300);
      msg.obj = data;
      sendMessage(msg);
    }

    void addVoiceRecogLog(String log) {
      Message msg = obtainMessage(400);
      msg.obj = log;
      sendMessage(msg);
    }

    void appendContentIntoVoiceRecogLog(String publicKey,
                                        String mp3FileName,
                                        String content,
                                        AppendDataCallback completedCallback) {
      AppendData data = new AppendData();
      data.publicKey = publicKey;
      data.mp3FileName = mp3FileName;
      data.content = content;
      data.callback = completedCallback;
      Message msg = obtainMessage(401);
      msg.obj = data;
      sendMessage(msg);
    }

    void exit() {
      removeCallbacksAndMessages(null);
      sendEmptyMessage(0);
    }

    void setVolumeAdjustConfig(int headsetPlugType, float factor,
                               boolean volumeOpen, boolean voiceNSOpen,
                               int voiceNSMode) {
      mHeadsetPlugType = headsetPlugType;
      mVolumeAdjustFactor = factor;
      mVolumeAdjustOpen = volumeOpen;
      mVoiceNSOpen = voiceNSOpen;
      mVoiceNSMode = voiceNSMode;
    }

    void setRecordSource(int source) {
      mRecordSource = source;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {

        // 终止线程
        case 0: {
          getLooper().quitSafely();
        }
        break;

        // 重置操作 - 创建音频文件 - 准备开始录音
        case 10: {
          resetPCMAdBFile((String) msg.obj);
          resetPCMBufferFile();
          resetAudioFiles((String) msg.obj);
          resetBuffers();
          if (mResetCallback != null) {
            mResetCallback.run();
          }
        }
        break;

        // 处理写入操作 - 转码Mp3 - 语音听写
        case 100: {
          PCMBuffer pcmBuffer = (PCMBuffer) msg.obj;
          short[] buffer = pcmBuffer.getData();
          int size = pcmBuffer.getSize();
          // 转码Mp3
          processMp3OneBuffer(buffer, size);
          // 使用Voice语音操作工具进行语音听写操作
          Voice.getInstance().write(buffer, size);
          // 写入PCM文件
          pcmBuffer.writeTo(mPCMBufferFile);
          // 处理完成后，放回BufferPool
          mBufferPool.add(pcmBuffer);
        }
        break;

        // 处理录音停止
        case 999: {
          // 启动处理PCMMp3操作
          // notice - mp3处理开始
          if (mNoticeHandler != null) {
            mNoticeHandler.sendEmptyMessage(801);
          }
//          // 检查是否需要对开头几秒的音频进行再降噪处理
//          checkNSAgain();
          // 开始处理MP3文件
          flushMp3Data();
          closeMp3Data();
          // 检查是否要对MP3文件进行降噪
          checkNSMp3();
          // notice - mp3处理完成
          if (mNoticeHandler != null) {
            mNoticeHandler.sendEmptyMessage(888);
          }
        }
        break;

        // 处理写入单位时间内平均分贝操作
        case 200: {
          if (mPCMAdBFile != null) {
            try {
              boolean isError = msg.arg1 == WHAT_RECORD_ERROR_INFO;
              boolean soHigh  = msg.arg1 == WHAT_RECORD_dB_SO_HIGH;
              String error    = isError ? (String) msg.obj : "";
              double average  = isError ? -99.99 : (double) msg.obj;
              int count = Math.max((int) (mAdBPoints.length + average), 0);
              Arrays.fill(mAdBPoints, 0, count, soHigh ? '-' : '.');
              Arrays.fill(mAdBPoints, count, mAdBPoints.length, ' ');
              String line = String.format(Locale.CHINESE,
                "%d|%d|%d|%d|%.2f|%.2f|%s\n",
                // 增益开关
                mVolumeAdjustOpen ? 1 : 0,
                // 降噪开关
                mVoiceNSOpen ? 1 : 0,
                // 0:默认内置麦克风
                // 1:有线耳机麦克风
                // 2:蓝牙耳机麦克风
                mHeadsetPlugType,
                // 1:MIC
                // 7:VOICE_COMMUNICATION
                mRecordSource,
                mVolumeAdjustFactor,
                average,
                isError ? error : String.valueOf(mAdBPoints));
              mPCMAdBFile.write(line.getBytes());
              // debug
              Logger.d("AudioRecorderManager", "dB: " + line);
            } catch (Exception ignored) {}
          }
        }
        break;

        // 追加内容到PCMAdBFile文件中
        case 300: {
          if (msg.obj instanceof AppendData) {
              try {
                AppendData data = (AppendData) msg.obj;
                if (!TextUtils.isEmpty(data.mp3FileName)) {
                  File file = new File(data.mp3FileName + "_adB.txt");
                  if (!file.exists()) file.createNewFile();
                  if (file.exists()) {
                    RandomAccessFile pcmAdBFile = new RandomAccessFile(file, "rw");
                    String content = data.content;
                    if (!TextUtils.isEmpty(data.publicKey)) {
                      try {
                        byte[] encode = RSAProvider.encryptPublicKey(
                          data.content.getBytes(), data.publicKey);
                        content = Base64Utils.encode(encode).trim();
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                    }
                    pcmAdBFile.seek(file.length());
                    pcmAdBFile.write(String.format(Locale.CHINESE,
                      "\n--------------------------------------\n" +
                        "%s" +
                        "\n--------------------------------------\n",
                      content).getBytes());
                    pcmAdBFile.close();
                    // completed callback
                    if (data.callback != null) {
                      data.callback.onAppendDataCompleted(file.getAbsolutePath());
                    }
                  }
                }
            } catch (Throwable ignored) {}
          }
        }
        break;

        // 写入语音识别日志
        case 400: {
          try {
            String log = (String) msg.obj;
            Logger.d("AudioRecorderManager", "addVoiceRecogLog: " + log);
            if (mVoiceRecogLogFile != null) {
              mVoiceRecogLogFile.write(String.format(Locale.CHINESE,
                "%s\n", log).getBytes());
            }
          } catch (Throwable ignored) {}
        }
        break;

        // 追加内容到VoiceRecogLog文件中
        case 401: {
          if (msg.obj instanceof AppendData) {
            try {
              AppendData data = (AppendData) msg.obj;
              if (!TextUtils.isEmpty(data.mp3FileName) && !TextUtils.isEmpty(data.publicKey)) {
                File file = new File(data.mp3FileName + "_recog.txt");
                if (!file.exists()) file.createNewFile();
                if (file.exists()) {
                  RandomAccessFile recogLogFile = new RandomAccessFile(file, "rw");
                  StringBuilder conSb = new StringBuilder();
                  String line;
                  while ((line = recogLogFile.readLine()) != null) {
                    conSb.append(new String(line.getBytes(
                      StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)).append('\n');
                  }
                  recogLogFile.close();
                  if (!TextUtils.isEmpty(data.content)) {
                    conSb.append("\n--------------------------------------\n");
                    conSb.append(data.content);
                    conSb.append("\n--------------------------------------\n");
                  }
                  byte[] encode = RSAProvider.encryptPublicKey(
                    conSb.toString().getBytes(), data.publicKey);
                  String content = Base64Utils.encode(encode).trim();
                  file.delete();
                  file.createNewFile();
                  if (file.exists()) {
                    recogLogFile = new RandomAccessFile(file, "rw");
                    recogLogFile.write(String.format(Locale.CHINESE,
                      "\n--------------------------------------\n" +
                        "%s" +
                        "\n--------------------------------------\n",
                      content).getBytes());
                    recogLogFile.close();
                  }
                  // completed callback
                  if (data.callback != null) {
                    data.callback.onAppendDataCompleted(file.getAbsolutePath());
                  }
                }
              }
            } catch (Throwable ignored) {}
          }
        }
        break;
      }
    }

    // 重置PCMBuffer文件
    private void resetPCMBufferFile() {
      if (mPCMBufferFile != null) {
        try {
          mPCMBufferFile.close();
          mPCMBufferFile = null;
        } catch (IOException ignored) {
        }
      }
      File file = new File(mPCMBufferFileName);
      if (file.exists()) {
        file.delete();
      }
      try {
        if (file.createNewFile()) {
          mPCMBufferFile = new RandomAccessFile(file, "rw");
        }
      } catch (IOException ignored) {
      }
    }

    // 重置PCMAdBFile文件
    private void resetPCMAdBFile(String fileName) {
      if (mPCMAdBFile != null) {
        try {
          mPCMAdBFile.close();
          mPCMAdBFile = null;
        } catch (IOException ignored) {
        }
      }
      File file = new File(fileName + "_adB.txt");
      if (file.exists()) {
        file.delete();
      }
      try {
        if (file.createNewFile()) {
          mPCMAdBFile = new RandomAccessFile(file, "rw");
        }
      } catch (IOException ignored) {
      }
    }

    void resetAudioFiles(String fileName) {
      try {
        int channels = 2;
        // 初始化转码工具
        MediaNUtils.lamemp3Init(sSampleRateInHz, channels, sSampleRateInHz, BIT_RATE);
        // 创建Mp3Buffer
        mp3Buffer = new byte[(int) (7200 + (sPCMBufferSize * channels * 1.25))];

        if (mAudioMp3FileWriter != null) {
          try {
            mAudioMp3FileWriter.close();
            mAudioMp3FileWriter = null;
          } catch (IOException ignored) { }
        }

        if (mVoiceRecogLogFile != null) {
          try {
            mVoiceRecogLogFile.close();
            mVoiceRecogLogFile = null;
          } catch (IOException ignored) { }
        }

        if (mAudioMp3File != null && mAudioMp3File.exists()) {
          mAudioMp3File.delete();
          mAudioMp3File = null;
        }

        if (!TextUtils.isEmpty(fileName)) {
          mAudioMp3File = new File(fileName);
          if (mAudioMp3File.exists()) {
            mAudioMp3File.delete();
          }
          if (mAudioMp3File.createNewFile()) {
            mAudioMp3FileWriter = new RandomAccessFile(mAudioMp3File, "rw");
          }

          File logFile = new File(fileName + "_recog.txt");
          if (logFile.exists()) logFile.delete();
          if (logFile.createNewFile()) {
            mVoiceRecogLogFile = new RandomAccessFile(logFile, "rw");
          }
        }
      } catch (Throwable ignored) {
        mAudioMp3File = null;
        mAudioMp3FileWriter = null;
        mVoiceRecogLogFile = null;
      }
    }

    private void resetBuffers() {
      // clear buffer pool
      mBufferPool.clear();
      // init buffer pool
      for (int i = 0; i < 30; i++) {
        mBufferPool.add(new PCMBuffer(sPCMBufferSize));
      }

      // reset mPCMBufferOffset
      mPCMBufferOffset = 0;
    }

    // 处理一个PCM Buffer 转正Mp3
    private void processMp3OneBuffer(short[] buffer, int readSize) {
      if (buffer == null) {
        return;
      }

      // 写进Mp3文件
      try {
        // 转成Mp3格式
        int encodedSize = MediaNUtils.lamemp3Encode(
            buffer, buffer, readSize, mp3Buffer);
        if (mAudioMp3FileWriter != null) {
          mAudioMp3FileWriter.write(mp3Buffer, 0, encodedSize);
        }
        // debug info
        if (encodedSize <= 0) {
          throw new Exception("lamemp3_encode encodedSize <= 0");
        }
      } catch (Throwable e) {
        try {
          String msg = e.getMessage();
          msg = msg.replaceAll("\n", "###");
          msg = msg.replaceAll("\r", "###");
          addErrorInfo(msg);
          Voice.getInstance().appendRecognLog("[processMp3Error]:" + msg);
        } catch (Throwable ignored) {}
      }
    }

    // flush mp3 data
    private void flushMp3Data() {
      final int flushResult = MediaNUtils.lamemp3Flush(mp3Buffer);
      if (flushResult > 0) {
        try {
          if (mAudioMp3FileWriter != null) {
            mAudioMp3FileWriter.write(mp3Buffer, 0, flushResult);
          }
        } catch (final IOException ignored) {
        }
      }
    }

    // close mp3 data
    private void closeMp3Data() {
      MediaNUtils.lamemp3Close();
    }

//    // 检查是否需要对开头几秒的音频进行再降噪处理
//    private void checkNSAgain() {
//      if (isCpuAbiSupport && mVoiceNSOpen &&
//        mPCMBufferFile != null && mAudioMp3FileWriter != null) {
//        try {
//          mPCMBufferFile.seek(0);
//          mAudioMp3FileWriter.seek(0);
//          short[] sBuf = new short[sPCMBufferSize];
//          byte[] bBuf = new byte[sPCMBufferSize * 2];
//          // 1分钟600个 - 1秒钟10个
//          for (int i = 0; i < 5; i++) {
//            mPCMBufferFile.read(bBuf);
//            for (int j = 0; j < sPCMBufferSize; j++) {
//              sBuf[j] = (short) (((bBuf[j * 2]) & 0xff) +
//                                (((bBuf[j * 2 + 1]) & 0xff) << 8));
//            }
//            // WebRTC降噪
//            AudioRecordHandler.processWebRTCAgc(sMediaNUtils, sBuf, sPCMBufferSize);
//            // 写进Mp3文件
//            // 转成Mp3格式
//            int encodedSize = MediaNUtils.lamemp3Encode(
//              sBuf, sBuf, sPCMBufferSize, mp3Buffer);
//            mAudioMp3FileWriter.write(mp3Buffer, 0, encodedSize);
//          }
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//    }

    // 检查是否要对MP3文件进行降噪
    private void checkNSMp3() {
      if (isCpuAbiSupport && mVoiceNSOpen) {
        String inMp3 = mAudioMp3File == null ? "" : mAudioMp3File.getAbsolutePath();
        String outMp3 = String.format(Locale.CHINESE, "%s.mp3",
          inMp3.replace(".mp3", "_ns"));
        sMediaNUtils.webrtcFileProcess(inMp3, outMp3, mVoiceNSMode);
      }
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class CalculatePCMHandler extends Handler {
    // Buffer pool
    private Queue<SPCMBuffer> mBufferPool = new ConcurrentLinkedQueue<>();
    // PCMdB
    private float mPCMMaxVolumedB;
    private int mPCMdBCount;
    private double mPCMdBSum;
    // 每单位时间内 PCMdB
    private int mUnitPCMdBCount;
    private double mUnitPCMdBSum;
    private long mUnitTime;
    // 单位时间间隔
    private int mUnitTimeInterval;
    // 每秒 PCMdB
    private int mSecPCMdBCount;
    private double mSecPCMdBSum;
    private int mSecPCMdBLeftShowCount;
    private int mSecPCMdBRightShowCount;
    private int[] m3SecAdB = new int[]{0, 0, 0};
    private static int sSecPCMdBShowMinCount = 20;
    private static int sSecPCMdBShowMindB = -6000;
    // AdB写入
    private WritePCMHandler mWriteHandler;
    // 通知
    private Handler mNoticeHandler;

    CalculatePCMHandler(Looper looper, int unitTimeIntervalInMillisecond,
                        WritePCMHandler writeHandler,
                        Handler noticeHandler) {
      super(looper);
      mUnitTimeInterval = unitTimeIntervalInMillisecond;
      mWriteHandler = writeHandler;
      mNoticeHandler = noticeHandler;
    }

    void setPCMMaxVolumedB(float maxdB) {
      mPCMMaxVolumedB = maxdB;
    }

    void setSecPCMdBShowMins(int minCount, int mindB) {
      sSecPCMdBShowMinCount = minCount;
      sSecPCMdBShowMindB = mindB * 100;
    }

    SPCMBuffer obtainBuffer() {
      SPCMBuffer buffer = mBufferPool.poll();
      if (buffer == null) {
        buffer = new SPCMBuffer(sPCMBufferSize);
        Logger.d("CalculatePCMHandler", "Crate SPCMBuffer");
      }
      return buffer;
    }

    void reset() {
      removeCallbacksAndMessages(null);
      sendEmptyMessage(888);
    }

    void addPCMBufferWrite(short[] buffer, int size) {
      SPCMBuffer spcmBuffer = obtainBuffer();
      spcmBuffer.copyFrom(buffer, size);
      Message msg = obtainMessage(100);
      msg.obj = spcmBuffer;
      sendMessage(msg);
    }

    void exit() {
      removeCallbacksAndMessages(null);
      sendEmptyMessage(0);
    }

    double getAveragePCMdB() {
      if (mPCMdBCount == 0) {
        return 0;
      }

      return mPCMdBSum / mPCMdBCount;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {

        // 终止线程
        case 0: {
          getLooper().quitSafely();
        }
        break;

        // 处理计算操作
        case 100: {
          SPCMBuffer buffer = (SPCMBuffer) msg.obj;
          short[] data = buffer.getBuffer();
          if (data != null) {
            double db = MediaNUtils.pcmDB(data, data.length);
            Logger.d("CalculatePCMHandler", "db: " + db);
            if (db < 0 && db > -300) {
              mPCMdBSum += db;
              mUnitPCMdBSum += db;
              mPCMdBCount++;
              mUnitPCMdBCount++;
              // 检查音量分贝值是否过高
              checkMaxPCMdB(db);
              // 检查是否满足触发计算单位时间内平均dB
              checkUnitPCMdB();
            } else {
              Voice.getInstance().appendRecognLog("[dBerror]:" + db);
            }
          }
          // 处理完成后，放回BufferPool
          mBufferPool.add(buffer);
        }
        break;

        // 重置操作
        case 888: {
          mPCMdBSum = 0;
          mPCMdBCount = 0;
          mUnitPCMdBSum = 0;
          mUnitPCMdBCount = 0;
          mUnitTime = 0;
          mSecPCMdBSum = 0;
          mSecPCMdBCount = 0;
          mSecPCMdBLeftShowCount = 0;
          mSecPCMdBRightShowCount = 0;
          Arrays.fill(m3SecAdB, 0);
          // clear buffer pool
          mBufferPool.clear();
          // init buffer pool
          for (int i = 0; i < 20; i++) {
            mBufferPool.add(new SPCMBuffer(sPCMBufferSize));
          }
        }
        break;
      }
    }

    private void checkMaxPCMdB(double db) {
      if (db >= mPCMMaxVolumedB) {
        // 通知Handler
        if (mNoticeHandler != null) {
          Message msg = mNoticeHandler.obtainMessage(WHAT_RECORD_dB_SO_HIGH);
          msg.arg1 = (int) (db * 100);
          mNoticeHandler.sendMessage(msg);
        }
        if (mWriteHandler != null) {
          mWriteHandler.addSoHighAdB(db);
        }
      }
    }

    private void checkUnitPCMdB() {
      // 满足单位时间
      if (System.currentTimeMillis() - mUnitTime >= mUnitTimeInterval) {
        // 计算单位时间的平均分贝值
        unitPCMdB();
        // 重新设定计算时间起点等
        mUnitTime = System.currentTimeMillis();
        mUnitPCMdBSum = 0;
        mUnitPCMdBCount = 0;
      }
    }

    // 每单位时间内触发一次
    private void unitPCMdB() {
      double average = mUnitPCMdBSum / mUnitPCMdBCount;
//      Logger.d("CalculatePCMHandler", "unitPCMdB: " + average);
      // 计算出每秒AdB
      secPCMdB(average);
      // 将平均分贝数值写入文件
      if (mWriteHandler != null) {
        mWriteHandler.addUnitAdB(average);
      }
      // 通知Handler
      if (mNoticeHandler != null) {
        Message msg = mNoticeHandler.obtainMessage(WHAT_RECORD_dB_CHANGED);
        msg.arg1 = (int) (average * 100);
        msg.arg2 = ((mSecPCMdBLeftShowCount << 16) & 0xffff0000) +
                    (mSecPCMdBRightShowCount & 0xffff);
        mNoticeHandler.sendMessage(msg);
      }
    }

    // 分解SecPCMdBShowCount
    int[] resolveSecPCMdBShowCount(int count) {
      return new int[] {(count >> 16) & 0xffff, count & 0xffff};
    }

    // 计算每秒平均dB
    private void secPCMdB(double average) {
      mSecPCMdBSum += average;
      mSecPCMdBCount++;
      if (mSecPCMdBCount >= 10) {
        double secAverage = mSecPCMdBSum / mSecPCMdBCount;
        m3SecAdB[0] = m3SecAdB[1];
        m3SecAdB[1] = m3SecAdB[2];
        m3SecAdB[2] = (int) (secAverage * 100);
        mSecPCMdBSum = 0;
        mSecPCMdBCount = 0;
        // 计算每秒dB提示
        secPCMdBShow(m3SecAdB[2]);
      }
      if (mSecPCMdBLeftShowCount > 0) {
        mSecPCMdBLeftShowCount--;
      }
      if (mSecPCMdBLeftShowCount < 0) {
        mSecPCMdBLeftShowCount = 0;
      }
      if (mSecPCMdBRightShowCount > 0) {
        mSecPCMdBRightShowCount--;
      }
      if (mSecPCMdBRightShowCount < 0) {
        mSecPCMdBRightShowCount = 0;
      }
    }

    // 计算每秒dB提示
    private void secPCMdBShow(int average) {
      if (m3SecAdB[0] < sSecPCMdBShowMindB &&
          m3SecAdB[1] < sSecPCMdBShowMindB &&
          m3SecAdB[2] < sSecPCMdBShowMindB &&
          mSecPCMdBLeftShowCount <= 0) {
        mSecPCMdBLeftShowCount = sSecPCMdBShowMinCount + 1;
      }
      if (/*m3SecAdB[0] < sSecPCMdBShowMindB &&
          m3SecAdB[1] < sSecPCMdBShowMindB &&*/
          m3SecAdB[2] < sSecPCMdBShowMindB &&
          mSecPCMdBRightShowCount <= 0) {
        mSecPCMdBRightShowCount = sSecPCMdBShowMinCount + 1;
      } else {
        if (average < sSecPCMdBShowMindB &&
          mSecPCMdBRightShowCount >= 1 && mSecPCMdBRightShowCount <= 5) {
          mSecPCMdBRightShowCount += 10;
        }
      }
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class ReadPCMHandler extends Handler {
    private FileInputStream mFile;
    private byte[] mBuffer = new byte[sPCMBufferSize * 2];
    //    private short[] mSBuffer = new short[sPCMBufferSize];
    private SoftReference<OnVoiceRecognizerListener> mListener;

    ReadPCMHandler(Looper looper) {
      super(looper);
    }

    void start(FileInputStream file, OnVoiceRecognizerListener listener) {
      mFile = file;
      mListener = new SoftReference<>(listener);
      if (mFile != null) {
        sendEmptyMessage(100);
      }
    }

    void exit() {
      removeCallbacksAndMessages(null);
      sendEmptyMessage(0);
      if (mFile != null) {
        try {
          mFile.close();
        } catch (IOException ignored) { }
      }
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {

        // 终止线程
        case 0: {
          getLooper().quitSafely();
        }
        break;

        // 处理读取操作
        case 100: {
          if (mFile != null) {
            try {
              int count = mFile.read(mBuffer);
              if (count < 0) {
                // PCM文件读取完成，回调转写完成
                sendEmptyMessageDelayed(200, 3000);
              } else {
                Voice.getInstance().write(mBuffer, count);
                // loop
                sendEmptyMessageDelayed(100, 50);
              }
            } catch (IOException e) {
              exit();
            }
          } else {
            exit();
          }
        }
        break;

        // 处理PCM文件读取完成，回调、exit
        case 200: {
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              final OnVoiceRecognizerListener listener = mListener == null ? null : mListener.get();
              if (listener != null) listener.onRecogError(VoiceRecognizer.NONE, -1, "pcm read completed.");
            }
          });
          exit();
        }
        break;
      }
    }
  }

  private static class SPCMBuffer {
    private short[] mBuffer;

    SPCMBuffer(int size) {
      mBuffer = new short[size];
    }

    void copyFrom(short[] buffer, int size) {
      if (buffer != null && mBuffer != null) {
        System.arraycopy(buffer, 0, mBuffer, 0,
            Math.min(Math.min(buffer.length, mBuffer.length), size));
      }
    }

    short[] getBuffer() {
      return mBuffer;
    }
  }

  public interface AppendDataCallback {
    void onAppendDataCompleted(String file);
  }

  private static class AppendData {
    String publicKey;
    String mp3FileName;
    String content;
    AppendDataCallback callback;
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  public interface OnAudioRecorderStopListener {
    void onAudioRecorderStoped();
  }
}
