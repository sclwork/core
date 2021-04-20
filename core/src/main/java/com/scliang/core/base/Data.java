package com.scliang.core.base;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.scliang.core.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import id.zelory.compressor.Compressor;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Retrofit;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/11.
 *
 * 数据管理
 * 包括网络数据、数据库缓存数据等
 *
 */
public final class Data {
    private Data() {
    }

    private static class SingletonHolder {
        private static final Data INSTANCE = new Data();
    }

    public static Data getInstance() {
        return SingletonHolder.INSTANCE;
    }

//    private DataCacheManager mDataCacheManager;

    private static int TIMEOUT_CONNECT = 15; // sec
    private static int TIMEOUT_WRITE = 15; // sec
    private static int TIMEOUT_READ = 15; // sec
    private int UPLOAD_FILE_TIMEOUT_COUNT = 0;
    private SoftReference<Context> mContext;
    private String mSignKey = "";
    private String mBaseUrl = "";

    /**
     * 使用ApplicationContext初始化Data工具
     */
    public void init(BaseApplication application, String baseUrl, String signKey,
                     long memoryCacheTimeout,
                     boolean debuggable) {
        init(application, 15,
                baseUrl, signKey,
                null, null,
                memoryCacheTimeout,
                debuggable);
    }

    /**
     * 使用ApplicationContext初始化Data工具
     */
    public void init(BaseApplication application, int timeoutInSec,
                     String baseUrl, String signKey,
                     OnDataCallNewUrlListener dataCallNewUrlListener,
                     DataLogFilter.OnRequestAliPathListener requestAliPathListener,
                     long memoryCacheTimeout,
                     boolean debuggable) {
        TIMEOUT_CONNECT = timeoutInSec;
        TIMEOUT_WRITE = timeoutInSec;
        TIMEOUT_READ = timeoutInSec;
        final Context context = application.getApplicationContext();
        mContext = new SoftReference<>(context);
        mSignKey = signKey;
        mBaseUrl = baseUrl;
        // 初始化OkHttpClient
        initOkHttpClient(context,
                /*certificates, hostnameVerifierHosts,*/
                memoryCacheTimeout,
                application.generateLogWriteToPath(),
                requestAliPathListener,
                debuggable);
        // 初始化Retrofit
        initRetrofit(context, baseUrl, dataCallNewUrlListener);
        // 初始化Fresco
        initFresco(context);
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

//    /**
//     * 设置数据缓存管理器
//     */
//    public void setDataCacheManager(DataCacheManager cacheManager) {
//        mDataCacheManager = cacheManager;
//    }

    /**
     * 获得设备属性信息
     */
    public String deviceInfo() {
        return collectDeviceInfo(BaseApplication.getApp().getApplicationContext()).toString();
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    /**
     * 创建网络请求
     * @param questioner questioner
     * @param bParams b Params
     * @param <T> Result
     * @return Call
     */
    public <S,T> Call<T> createCBSRequestCall(Questioner questioner, Class<S> SERVICE,
                                              String apiName, Map<String, String> bParams) {
        if (bParams == null) return null;

        // C Params JSON
        JSONObject cJson = collectDeviceInfo(BaseApplication.getApp().getApplicationContext());

        // B Params JSON
        JSONObject bJson = new JSONObject();
        try {
            Map globalParams = BaseApplication.getApp().onGenerateDataGlobalParams();
            if (globalParams != null) {
                Set keys = globalParams.keySet();
                for (Object key : keys) {
                    if (key instanceof String) {
                        bJson.put((String)key, globalParams.get(key));
                    }
                }
            }
            Set<String> keys = bParams.keySet();
            for (String key : keys) {
                String value = bParams.get(key);
                if (!TextUtils.isEmpty(value)) {
                    try {
                        JSONObject jsonObject = new JSONObject(value);
                        if (value.startsWith("{") && value.endsWith("}")) {
                            bJson.put(key, jsonObject);
                        } else {
                            bJson.put(key, value);
                        }
                    } catch (JSONException eo) {
                        try {
                            JSONArray jsonArray = new JSONArray(value);
                            if (value.startsWith("[") && value.endsWith("]")) {
                                bJson.put(key, jsonArray);
                            } else {
                                bJson.put(key, value);
                            }
                        } catch (JSONException ei) {
                            bJson.put(key, value);
                        }
                    }
                } else {
                    bJson.put(key, "");
                }
            }
        } catch (JSONException ignored) {}

        // S Params JSON = md5(b.value+KEY)
        String sParams = NUtils.md5(bJson.toString() + mSignKey).toLowerCase();

        final String[] params = new String[] {
                /*c params*/cJson.toString(),
                /*b params*/bJson.toString(),
                /*s params*/sParams,
        };
        return createRequestCall(questioner, SERVICE, apiName, params);
    }

    /**
     * 创建网络请求
     * @param questioner questioner
     * @param params Request Params
     * @param <T> Result
     * @return Call
     */
    public <S,T> Call<T> createRequestCall(Questioner questioner, Class<S> SERVICE,
                                 String apiName, String[] params) {
        final String requestId = questioner.giveRequestId();
        final Class[] cPs = new Class[params == null ? 0 : params.length];
        for (int i = 0; i < cPs.length; i++) {
            cPs[i] = String.class;
        }
        try {
            Method method = SERVICE.getDeclaredMethod(apiName, cPs);
            S service = mRetrofit.create(SERVICE);
            Object[] oPs = new Object[params == null ? 0 : params.length];
            if (params != null) System.arraycopy(params, 0, oPs, 0, oPs.length);
            final Call<T> rawCall = (Call<T>) method.invoke(service, oPs);
            synchronized (mRequestSync) {
                List<Call<?>> requests = mRequestMap.get(requestId);
                if (requests == null) requests = new LinkedList<>();
                requests.add(rawCall);
                mRequestMap.put(requestId, requests);
            }
            return rawCall;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 请求网络数据
     * @param questioner questioner
     * @param bParams b Params
     * @param <T> Result
     * @return Call
     */
    public <S,T> Call<T> request(Questioner questioner, Class<S> SERVICE,
                                 String apiName, Map<String, String> bParams,
                                 final DataCallback<T> callback) {
        final Call<T> rawCall = createCBSRequestCall(questioner, SERVICE, apiName, bParams);
        enqueueCall(questioner, rawCall, callback);
        return rawCall;
    }

    /**
     * 请求网络数据
     * @param questioner questioner
     * @param params Request Params
     * @param <T> Result
     * @return Call
     */
    public <S,T> Call<T> request(Questioner questioner, Class<S> SERVICE,
                                 String apiName, String[] params, final DataCallback<T> callback) {
        final Call<T> rawCall = createRequestCall(questioner, SERVICE, apiName, params);
        enqueueCall(questioner, rawCall, callback);
        return rawCall;
    }

    /**
     * 取消所有的网络请求
     */
    public void cancelAll() {
        synchronized (mRequestSync) {
            Set<String> ids = mRequestMap.keySet();
            for (String requestId : ids) {
                List<Call<?>> requests = mRequestMap.get(requestId);
                if (requests != null) {
                    for (int i = 0; i < requests.size(); i++) {
                        Call<?> call = requests.get(i);
                        if (!call.isExecuted() && !call.isCanceled()) {
                            call.cancel();
                        }
                    }
                }
            }
            mRequestMap.clear();
        }
    }

    /**
     * 取消给定Questioner的所有网络请求
     * @param questioner questioner
     */
    public void cancel(Questioner questioner) {
        final String requestId = questioner.giveRequestId();
        synchronized (mRequestSync) {
            List<Call<?>> requests = mRequestMap.get(requestId);
            if (requests != null) {
                for (int i = 0; i < requests.size(); i++) {
                    Call<?> call = requests.get(i);
                    if (!call.isExecuted() && !call.isCanceled()) {
                        call.cancel();
                    }
                }
            }
            mRequestMap.remove(requestId);
        }
    }

    /**
     * 取消给定的网络请求
     * @param call 待取消的网络请求
     */
    public void cancel(Questioner questioner, Call<?> call) {
        final String requestId = questioner.giveRequestId();
        synchronized (mRequestSync) {
            List<Call<?>> requests = mRequestMap.get(requestId);
            if (requests != null) {
                int index = requests.indexOf(call);
                Call<?> rawCall = requests.remove(index);
                if (!rawCall.isExecuted() && !rawCall.isCanceled()) {
                    rawCall.cancel();
                }
            }
        }
    }

    /**
     * 移除给定的网络请求
     * @param removeCall 待删除的网络请求
     */
    public void removeCall(Questioner questioner, Call<?> removeCall) {
        final String requestId = questioner.giveRequestId();
        synchronized (mRequestSync) {
            if (!removeCall.isExecuted() && !removeCall.isCanceled()) {
                removeCall.cancel();
            }
            List<Call<?>> requests = mRequestMap.get(requestId);
            if (requests != null) {
                requests.remove(removeCall);
            }
        }
    }

    public <T> void enqueueCall(final Questioner questioner,
                                 final Call<T> rawCall,
                                 final DataCallback<T> callback) {
        DataRequestUtils.DataRequestRunnable<T> runnable =
                new DataRequestUtils.DataRequestRunnable<>(questioner, rawCall, callback);
        DataRequestUtils.post(runnable);
    }




//    /*
//     * 上传文件到服务器
//     * @param questioner questioner
//     * @param fileName 待上传文件全路径
//     * @param callback 文件上传回调
//     */
//    private void uploadFileCore(final Questioner questioner, final String fileName,
//                                final String type,
//                                final DataCallback<UploadFileResult> callback, boolean tryAgain) {
//        if (hasConnectedNetwork()) {
//            final Context context = mContext == null ? null : mContext.get();
//            if (context != null && !TextUtils.isEmpty(fileName)) {
//                File file = new File(fileName);
//                if (file.exists() && file.isFile()) {
//                    // create RequestBody instance from file
//                    RequestBody requestFile = RequestBody.create(
//                            MediaType.parse("multipart/form-data"), file);
//                    // MultipartBody.Part is used to send also the actual file name
//                    MultipartBody.Part body = MultipartBody.Part.createFormData(
//                            "file", file.getName(), requestFile);
//                    // add another part within the multipart request
//                    // C Params JSON
//                    JSONObject cJson = collectDeviceInfo(BaseApplication.getApp().getApplicationContext());
//                    RequestBody cParams = RequestBody.create(
//                            MediaType.parse("multipart/form-data"), cJson.toString());
//                    // B Params JSON
//                    JSONObject bJson = new JSONObject();
//                    try {
//                        Map globalParams = BaseApplication.getApp().onGenerateDataGlobalParams();
//                        if (globalParams != null) {
//                            Set keys = globalParams.keySet();
//                            for (Object key : keys) {
//                                if (key instanceof String) {
//                                    bJson.put((String)key, globalParams.get(key));
//                                }
//                            }
//                        }
//                    } catch (JSONException ignored) {
//                    }
//                    RequestBody bParams = RequestBody.create(
//                            MediaType.parse("multipart/form-data"), bJson.toString());
//                    // S Params JSON = md5(b.value+KEY)
//                    RequestBody sParams = RequestBody.create(
//                            MediaType.parse("multipart/form-data"),
//                            NUtils.md5(bJson.toString() + mSignKey).toLowerCase());
//
//                    // 创建FileRetrofit
//                    Retrofit fileRetrofit = createUploadFileRetrofit();
//
//                    // Call
//                    FileApi api = fileRetrofit.create(FileApi.class);
//                    final Call<UploadFileResult> call =
//                      "ynkp".equals(type) ?
//                        api.uploadFileYnkp(cParams, bParams, sParams, body) :
//                        ("ugc".equals(type) ?
//                            api.uploadFileUgc(cParams, bParams, sParams, body) :
//                            api.uploadFile(cParams, bParams, sParams, body)
//                        );
//                    // Enqueue Call
//                    enqueueCall(questioner, call, new DataCallback<UploadFileResult>() {
//                        @Override
//                        public void onWaiting(Call<UploadFileResult> call) {
//                            if (UPLOAD_FILE_TIMEOUT_COUNT == 0) {
//                                if (callback != null) callback.onWaiting(call);
//                            }
//                        }
//
//                        @Override
//                        public void onRequest(Call<UploadFileResult> call) {
//                            if (UPLOAD_FILE_TIMEOUT_COUNT == 0) {
//                                if (callback != null) callback.onRequest(call);
//                            }
//                        }
//                        @Override
//                        public void onResponse(Call<UploadFileResult> call,
//                                               @Nullable UploadFileResult uploadFileResult) {
//                            if (callback != null) callback.onResponse(call, uploadFileResult);
//                            UPLOAD_FILE_TIMEOUT_COUNT = 0;
//                        }
//                        @Override
//                        public void onFailure(Call<UploadFileResult> call, Throwable throwable) {
//                            if (throwable instanceof SocketTimeoutException) {
//                                UPLOAD_FILE_TIMEOUT_COUNT++;
//                                if (UPLOAD_FILE_TIMEOUT_COUNT < 8) {
//                                    uploadFileCore(questioner, fileName, type, callback, true);
//                                } else {
//                                    if (callback != null) callback.onFailure(call, throwable);
//                                    UPLOAD_FILE_TIMEOUT_COUNT = 0;
//                                }
//                            } else {
//                                if (callback != null) callback.onFailure(call, throwable);
//                                UPLOAD_FILE_TIMEOUT_COUNT = 0;
//                            }
//                        }
//                        @Override
//                        public void onNoNetwork(Call<UploadFileResult> call) {
//                            if (callback != null) callback.onNoNetwork(call);
//                        }
//                    });
//                } else {
//                    if (tryAgain) {
//                        if (callback != null)
//                            callback.onFailure(null, new Throwable("file is not exists or is not file"));
//                    }
//                }
//            } else {
//                if (tryAgain) {
//                    if (callback != null)
//                        callback.onFailure(null, new Throwable("fileName is empty"));
//                }
//            }
//        } else {
////            BaseApplication.getApp().toast(
////                    BaseApplication.getApp().getString(R.string.error_no_network_tip), null);
//            if (tryAgain) {
//                if (callback != null)
//                    callback.onFailure(null, new Throwable(
//                            BaseApplication.getApp().getString(R.string.error_no_network_tip)));
//            }
//            if (callback != null) callback.onNoNetwork(null);
//        }
//    }

    private Retrofit createUploadFileRetrofit() {
        int timeout = TIMEOUT_WRITE * (UPLOAD_FILE_TIMEOUT_COUNT + 1);

        // init file OkHttpClient
        OkHttpClient.Builder fileBuilder = new OkHttpClient.Builder();
        // 设置超时
        fileBuilder.connectTimeout(timeout, TimeUnit.SECONDS);
        fileBuilder.writeTimeout(timeout, TimeUnit.SECONDS);
        fileBuilder.readTimeout(timeout, TimeUnit.SECONDS);
        OkHttpClient okHttpClient = fileBuilder.build();

        // init file Retrofit
        Retrofit.Builder fileRetrofitBuilder = new Retrofit.Builder()
                .baseUrl(mBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(DataConverterFactory.create());

        return fileRetrofitBuilder.build();
    }

    public Retrofit createRouterRetrofit(String baseUrl) {
        if (TextUtils.isEmpty(baseUrl)) {
            return null;
        }

        // init file OkHttpClient
        OkHttpClient.Builder fileBuilder = new OkHttpClient.Builder();
        // 设置超时
        fileBuilder.connectTimeout(TIMEOUT_CONNECT, TimeUnit.SECONDS);
        fileBuilder.writeTimeout(TIMEOUT_WRITE, TimeUnit.SECONDS);
        fileBuilder.readTimeout(TIMEOUT_READ, TimeUnit.SECONDS);
        OkHttpClient okHttpClient = fileBuilder.build();

        // init file Retrofit
        Retrofit.Builder fileRetrofitBuilder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(DataConverterFactory.create());

        return fileRetrofitBuilder.build();
    }

//    /**
//     * 下载文件
//     * @param questioner questioner
//     * @param fileUrl 文件远端地址
//     * @param fileName 文件本地保存路径全名
//     * @param callback callback
//     */
//    @SuppressLint("StaticFieldLeak")
//    public void downloadFile(final Questioner questioner,
//                             final String fileUrl, final String fileName,
//                             final DataCallback<ResponseBody> callback,
//                             final OnDownloadFileListener listener) {
//        if (questioner == null || TextUtils.isEmpty(fileUrl) || TextUtils.isEmpty(fileName)) {
//            return;
//        }
//
//        if (hasConnectedNetwork()) {
//            DataCompressUtils.post(new Runnable() {
//                @Override
//                public void run() {
//                    final File targetFile = new File(fileName);
//                    if (targetFile.exists()) {
//                        targetFile.delete();
//                    }
//                    try {
//                        if (targetFile.createNewFile()) {
//                            // 创建FileRetrofit
//                            Retrofit fileRetrofit = createDownloadFileRetrofit();
//                            // Call
//                            FileApi api = fileRetrofit.create(FileApi.class);
//                            final Call<ResponseBody> call = api.downloadFile(fileUrl);
//                            // Enqueue Call
//                            enqueueCall(questioner, call, new DataCallback<ResponseBody>() {
//                                @Override
//                                public void onWaiting(Call<ResponseBody> call) {
//                                    if (callback != null) callback.onWaiting(call);
//                                }
//
//                                @Override
//                                public void onRequest(Call<ResponseBody> call) {
//                                    if (callback != null) callback.onRequest(call);
//                                }
//                                @Override
//                                public void onResponse(final Call<ResponseBody> call, @Nullable final ResponseBody responseBody) {
//                                    if (responseBody == null) {
//                                        if (callback != null) callback.onResponse(call, null);
//                                    } else {
//                                        final Handler uiHandler = new Handler(Looper.getMainLooper());
//                                        uiHandler.post(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                if (listener != null) {
//                                                    listener.onDownloadFileStart(fileName);
//                                                }
//                                            }
//                                        });
//                                        DataCompressUtils.post(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                final boolean res = writeResponseBodyToDisk(responseBody, targetFile, listener);
//                                                uiHandler.post(new Runnable() {
//                                                    @Override
//                                                    public void run() {
//                                                        if (callback != null) callback.onResponse(
//                                                                call, res ? responseBody : null);
//                                                    }
//                                                });
//                                            }
//                                        });
//                                    }
//                                }
//                                @Override
//                                public void onFailure(Call<ResponseBody> call, Throwable throwable) {
//                                    if (callback != null) callback.onFailure(call, throwable);
//                                }
//                                @Override
//                                public void onNoNetwork(Call<ResponseBody> call) {
//                                    if (callback != null) callback.onNoNetwork(call);
//                                }
//                            });
//                        } else {
//                            if (callback != null) callback.onFailure(null, new Exception("File not exists!"));
//                        }
//                    } catch (IOException e) {
//                        if (callback != null) callback.onFailure(null, e);
//                    }
//                }
//            });
//        } else {
//            if (callback != null) callback.onNoNetwork(null);
//        }
//    }

    private Retrofit createDownloadFileRetrofit() {
        int timeout = TIMEOUT_WRITE * 2;

        // init file OkHttpClient
        OkHttpClient.Builder fileBuilder = new OkHttpClient.Builder();
        // 设置超时
        fileBuilder.connectTimeout(timeout, TimeUnit.SECONDS);
        fileBuilder.writeTimeout(timeout, TimeUnit.SECONDS);
        fileBuilder.readTimeout(timeout, TimeUnit.SECONDS);
        OkHttpClient okHttpClient = fileBuilder.build();

        // init file Retrofit
        Retrofit.Builder fileRetrofitBuilder = new Retrofit.Builder()
                .baseUrl(mBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(DataConverterFactory.create());

        return fileRetrofitBuilder.build();
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, File targetFile,
                                            final OnDownloadFileListener listener) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        final Handler uiHandler = new Handler(Looper.getMainLooper());
        final String fileName = targetFile.getAbsolutePath();

        try {
            byte[] buffer = new byte[8192]; // 8K
            long fileSize = body.contentLength();
            long fileSizeDownloaded = 0;

            inputStream = body.byteStream();
            outputStream = new FileOutputStream(targetFile);

            while (true) {
                int read = inputStream.read(buffer);
                if (read == -1) {
                    break;
                }

                outputStream.write(buffer, 0, read);
                fileSizeDownloaded += read;

                // debug
                Logger.d("Data", "DownloadFile: " + fileSizeDownloaded + "/" + fileSize);

                final long fFileSize = fileSize;
                final long fDownloadSize = fileSizeDownloaded;
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.onDownloadFileProgress(fileName, fFileSize, fDownloadSize);
                        }
                    }
                });
            }

            outputStream.flush();

            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onDownloadFileCompleted(fileName);
                    }
                }
            });

            return true;
        } catch (final IOException e) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onDownloadFileError(fileName, e);
                    }
                }
            });

            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException ignored) { }
        }
    }

    /**
     * 压缩图片
     * @param context context
     * @param quality quality
     * @param compressFormat compressFormat
     * @param srcFileName srcFileName
     * @param targetPath targetPath
     * @param targetFileName targetFileName
     * @param timeout 压缩超时时间(ms)，压缩操作如果超过timeout，就copyFile后直接返回
     * @return 压缩后的图片的地址
     */
    public String compressImage(Context context,
                                int quality,
                                Bitmap.CompressFormat compressFormat,
                                String srcFileName,
                                String targetPath, String targetFileName,
                                long timeout) {
        return compressImage(context,
                quality, null,
                compressFormat, srcFileName,
                targetPath, targetFileName,
                timeout);
    }

    // 判断是否继续等待
    private boolean mCompressWait;
    private static final byte[] mCompressSync = new byte[] {0};
    private Timer mCompressTimer;
    // 记录给定的timeout下是否发生过图片压缩超时
    // 如果发生过图片压缩超时，接下来的所有图片都不再做压缩，而是copyFile后直接返回
    private long mSavedCompressImageTimeout;
    private boolean mCompressImageTimeouted;
    // 临时记录压缩后的文件路径
    private SoftReference<HashMap<String, String>> mTmpCompressImageTargetPath;

    private String getTmpCompressImageTargetPath(String source) {
        if (TextUtils.isEmpty(source)) {
            return "";
        }

        if (mTmpCompressImageTargetPath == null) {
            return "";
        }

        HashMap<String, String> map = mTmpCompressImageTargetPath.get();
        if (map == null) {
            return "";
        }

        return map.get(source);
    }

    private void setTmpCompressImageTargetPath(String source, String target) {
        if (TextUtils.isEmpty(source)) {
            return;
        }

        if (mTmpCompressImageTargetPath == null) {
            mTmpCompressImageTargetPath = new SoftReference<>(new HashMap<String, String>());
        }

        HashMap<String, String> map = mTmpCompressImageTargetPath.get();
        if (map == null) {
            mTmpCompressImageTargetPath = new SoftReference<>(new HashMap<String, String>());
        }

        map = mTmpCompressImageTargetPath.get();
        if (map != null) {
            if (TextUtils.isEmpty(target)) {
                map.remove(source);
            } else {
                map.put(source, target);
            }
        }
    }

    /**
     * 压缩图片
     * @param context context
     * @param quality quality
     * @param maxSize MaxSize
     * @param compressFormat compressFormat
     * @param srcFileName srcFileName
     * @param targetPath targetPath
     * @param targetFileName targetFileName
     * @param timeout 压缩超时时间(ms)，压缩操作如果超过timeout，就copyFile后直接返回
     * @return 压缩后的图片的地址
     */
    public String compressImage(final Context context,
                                final int quality,
                                final Size<Integer> maxSize,
                                final Bitmap.CompressFormat compressFormat,
                                final String srcFileName,
                                final String targetPath, final String targetFileName,
                                long timeout) {
        // 保证timeout有效(>=0)
        timeout = timeout <= 0 ? 0 : timeout;

        // 检查是否需要重置标记
        if (timeout != mSavedCompressImageTimeout) {
            mCompressImageTimeouted = false;
            mSavedCompressImageTimeout = timeout;
        }

        final File file = new File(srcFileName);
        if (file.exists()) {
            if (context == null) {
                try {
                    return copyFile(file, targetPath, targetFileName);
                } catch (IOException e) {
                    return srcFileName;
                }
            }

            // 如果已经发生过压缩超时
            if (mCompressImageTimeouted) {
                try {
                    return copyFile(file, targetPath, targetFileName);
                } catch (IOException e) {
                    return srcFileName;
                }
            }

            // 如果没发生过压缩超时或timeout重置时需要尝试图片压缩
            else {
                synchronized (mCompressSync) {
                    mCompressWait = true;
                    if (mCompressTimer != null) {
                        mCompressTimer.cancel();
                        mCompressTimer = null;
                    }
                    DataCompressUtils.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Compressor compressor = new Compressor(context)
                                        .setQuality(quality)
                                        .setCompressFormat(compressFormat)
                                        .setDestinationDirectoryPath(targetPath);
                                if (maxSize != null) {
                                    compressor = compressor
                                            .setMaxWidth(maxSize.getWidth())
                                            .setMaxHeight(maxSize.getHeight());
                                }
                                File targetFile = compressor.compressToFile(file, targetFileName);
                                if (targetFile != null && targetFile.exists()) {
                                    setTmpCompressImageTargetPath(file.getAbsolutePath(),
                                            targetFile.getAbsolutePath());
                                    mCompressWait = false;
                                    if (mCompressTimer != null) {
                                        mCompressTimer.cancel();
                                        mCompressTimer = null;
                                    }
                                } else {
                                    setTmpCompressImageTargetPath(file.getAbsolutePath(), "");
                                    mCompressWait = false;
                                    if (mCompressTimer != null) {
                                        mCompressTimer.cancel();
                                        mCompressTimer = null;
                                    }
                                }
                            } catch (IOException e) {
                                setTmpCompressImageTargetPath(file.getAbsolutePath(), "");
                                mCompressWait = false;
                                if (mCompressTimer != null) {
                                    mCompressTimer.cancel();
                                    mCompressTimer = null;
                                }
                            }
                        }
                    });
                    try {
                        mCompressTimer = new Timer();
                        mCompressTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                mCompressImageTimeouted = true;
                                mCompressWait = false;
                                if (mCompressTimer != null) {
                                    mCompressTimer.cancel();
                                    mCompressTimer = null;
                                }
                            }
                        }, timeout);
                        while (mCompressWait) {
                            Thread.sleep(1);
                        }
                        String targetFile = getTmpCompressImageTargetPath(file.getAbsolutePath());
                        if (TextUtils.isEmpty(targetFile)) {
                            try {
                                return copyFile(file, targetPath, targetFileName);
                            } catch (IOException ee) {
                                return srcFileName;
                            }
                        } else {
                            return targetFile;
                        }
                    } catch (InterruptedException e) {
                        try {
                            return copyFile(file, targetPath, targetFileName);
                        } catch (IOException ee) {
                            return srcFileName;
                        }
                    }
                }
            }
        } else {
            return srcFileName;
        }
    }

    /**
     * 使用DownloadManager下载apk
     */
    public long downloadApk(String title, String url, ApkDownloadCompletedListener listener) {
        if (hasConnectedNetwork()) {
            try {
                mApkDownloadCompletedListener = listener;
                // 检查下载是否完成
                ApkInstallReceiver receiver = new ApkInstallReceiver();
                BaseApplication.getApp().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                // 下载设置
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                // 通过setAllowedNetworkTypes方法可以设置允许在何种网络下下载，
                // 也可以使用setAllowedOverRoaming方法，它更加灵活
//                req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                // 此方法表示在下载过程中通知栏会一直显示该下载，在下载完成后仍然会显示，
                // 直到用户点击该通知或者消除该通知。还有其他参数可供选择
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                // 设置下载文件存放的路径，同样你可以选择以下方法存放在你想要的位置。
                // setDestinationUri
                // setDestinationInExternalPublicDir
                req.setDestinationInExternalFilesDir(BaseApplication.getApp(),
                        Environment.DIRECTORY_DOWNLOADS, title);

                // 设置一些基本显示信息
                req.setTitle(title);
                req.setDescription(BaseApplication.getApp().getString(R.string.dialog_action_download_completed));
                req.setMimeType("application/vnd.android.package-archive");

                long downloadId = -1;

                // 下载
                DownloadManager dm = (DownloadManager) BaseApplication.getApp().getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    downloadId = dm.enqueue(req);
                    receiver.setDownloadId(downloadId);
                }
                return downloadId;
            } catch (Exception ignored) {
                BaseApplication.getApp().toast(
                        BaseApplication.getApp().getString(R.string.dialog_action_url_error), null);
                return -1;
            }
        } else {
            return -1;
        }
    }

    private ApkDownloadCompletedListener mApkDownloadCompletedListener;

    private String copyFile(File source, String targetPath, String targetName) throws IOException {
        if (source == null || !source.exists() || TextUtils.isEmpty(targetPath)) {
            return "";
        }

        File target = new File(targetPath, String.format(Locale.CHINESE,
                "timeout_%s", targetName));

        return copyFile(source, target);
    }

    public static String copyFile(File source, File dest) throws IOException {
        if (source == null || !source.exists() || dest == null) {
            return "";
        }

        if (!dest.exists()) {
            dest.createNewFile();
        }

        if (!dest.exists()) {
            return source.getAbsolutePath();
        }

        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            return dest.getAbsolutePath();
        } catch (Exception e) {
            return source.getAbsolutePath();
        } finally {
            if (inputChannel != null) inputChannel.close();
            if (outputChannel != null) outputChannel.close();
        }
    }

    // Apk下载完成启动安装页面
    private class ApkInstallReceiver extends BroadcastReceiver {
        private long mDownloadId;

        public void setDownloadId(long id) {
            mDownloadId = id;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                if (mApkDownloadCompletedListener != null) {
                    mApkDownloadCompletedListener.onApkDownloadCompleted(mDownloadId);
                }
                installApk(BaseApplication.getApp(), mDownloadId);
                BaseApplication.getApp().unregisterReceiver(this);
            }
        }
    }

    public void installApk(Context context, long downloadApkId) {
        DownloadManager dManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dManager == null) {
            return;
        }

        Intent install = new Intent(Intent.ACTION_VIEW);
        Uri downloadFileUri = dManager.getUriForDownloadedFile(downloadApkId);
        if (downloadFileUri != null) {
            String packageName = BaseApplication.getApp().getPackageName();
            BaseApplication.getApp().grantUriPermission(packageName,
                    downloadFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (install.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(install);
            } else {
                Toast.makeText(context, R.string.dialog_install_act_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 下载图片
     * @param url 图片网络地址
     * @param callback 图片下载回调
     */
    public void downloadBitmap(String url, final DataCallback<Bitmap> callback) {
        if (hasConnectedNetwork()) {
            final Context context = mContext == null ? null : mContext.get();
            if (context != null && !TextUtils.isEmpty(url)) {
                final Handler uiHandler = new Handler(Looper.getMainLooper());
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onRequest(null);
                        }
                    }
                });
                ImageRequest imageRequest = ImageRequestBuilder
                        .newBuilderWithSource(Uri.parse(url))
                        .setProgressiveRenderingEnabled(true)
                        .build();
                final ImagePipeline imagePipeline = Fresco.getImagePipeline();
                final DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline
                        .fetchDecodedImage(imageRequest, context);
                final BaseBitmapDataSubscriber subscriber = new BaseBitmapDataSubscriber() {
                    @Override
                    protected void onNewResultImpl(final Bitmap bitmap) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onResponse(null, bitmap);
                                }
                            }
                        });
                    }

                    @Override
                    protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onFailure(null, null);
                                }
                            }
                        });
                    }
                };
                dataSource.subscribe(subscriber, CallerThreadExecutor.getInstance());
            }
        } else {
            if (callback != null) callback.onNoNetwork(null);
        }
    }

    /**
     * 让imageView显示出url下的图片
     * @param imageView SimpleDraweeView
     * @param url 图片地址
     *        远程图片            http://, https://	HttpURLConnection
     *        本地文件            file://	            FileInputStream
     *        Content provider   content://	        ContentResolver
     *        asset目录下的资源    asset://	        AssetManager
     *        res目录下的资源      res://	            Resources.openRawResource
     *           res 示例:Uri uri = Uri.parse("res://包名(实际可以是任何字符串甚至留空)/" + R.drawable.ic_launcher);
     */
    public void fetchSimpleDraweeView(SimpleDraweeView imageView, String url) {
        fetchSimpleDraweeView(imageView, url, false);
    }

    /**
     * 让imageView显示出url下的图片
     * @param imageView SimpleDraweeView
     * @param url 图片地址
     *        远程图片            http://, https://	HttpURLConnection
     *        本地文件            file://	            FileInputStream
     *        Content provider   content://	        ContentResolver
     *        asset目录下的资源    asset://	        AssetManager
     *        res目录下的资源      res://	            Resources.openRawResource
     *           res 示例:Uri uri = Uri.parse("res://包名(实际可以是任何字符串甚至留空)/" + R.drawable.ic_launcher);
     */
    public void fetchSimpleDraweeView(SimpleDraweeView imageView, String url,
                                      final OnImageFetchCompletedListener listener) {
        fetchSimpleDraweeView(imageView, url, false, listener);
    }

    /**
     * 让imageView显示出url下的图片
     * @param imageView SimpleDraweeView
     * @param url 图片地址
     *        远程图片            http://, https://	HttpURLConnection
     *        本地文件            file://	            FileInputStream
     *        Content provider   content://	        ContentResolver
     *        asset目录下的资源    asset://	        AssetManager
     *        res目录下的资源      res://	            Resources.openRawResource
     *           res 示例:Uri uri = Uri.parse("res://包名(实际可以是任何字符串甚至留空)/" + R.drawable.ic_launcher);
     * @param width  指定图片加载宽度
     * @param height  指定图片加载高度
     */
    public void fetchSimpleDraweeView(SimpleDraweeView imageView, String url, int width, int height) {
        fetchSimpleDraweeView(imageView, url, width, height, false);
    }

    /**
     * 让imageView显示出url下的图片
     * @param imageView SimpleDraweeView
     * @param url 图片地址
     *        远程图片            http://, https://	HttpURLConnection
     *        本地文件            file://	            FileInputStream
     *        Content provider   content://	        ContentResolver
     *        asset目录下的资源    asset://	        AssetManager
     *        res目录下的资源      res://	            Resources.openRawResource
     *           res 示例:Uri uri = Uri.parse("res://包名(实际可以是任何字符串甚至留空)/" + R.drawable.ic_launcher);
     * @param clearCache 是否先清空此url的图片缓存
     */
    public void fetchSimpleDraweeView(SimpleDraweeView imageView, String url, boolean clearCache) {
        fetchSimpleDraweeView(imageView, url, clearCache, null);
    }

    /**
     * 让imageView显示出url下的图片
     * @param imageView SimpleDraweeView
     * @param url 图片地址
     *        远程图片            http://, https://	HttpURLConnection
     *        本地文件            file://	            FileInputStream
     *        Content provider   content://	        ContentResolver
     *        asset目录下的资源    asset://	        AssetManager
     *        res目录下的资源      res://	            Resources.openRawResource
     *           res 示例:Uri uri = Uri.parse("res://包名(实际可以是任何字符串甚至留空)/" + R.drawable.ic_launcher);
     * @param clearCache 是否先清空此url的图片缓存
     */
    public void fetchSimpleDraweeView(SimpleDraweeView imageView, String url, boolean clearCache,
                                      final OnImageFetchCompletedListener listener) {
        if (imageView != null) {
            int width = 320;
            int height = 240;
            WindowManager wm = (WindowManager) imageView.getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(dm);
                width = dm.widthPixels;
                height = dm.heightPixels;
            }
            fetchSimpleDraweeView(imageView, url, width, height, clearCache, listener);
        }
    }

    /**
     * 让imageView显示出url下的图片
     * @param imageView SimpleDraweeView
     * @param url 图片地址
     *        远程图片            http://, https://	HttpURLConnection
     *        本地文件            file://	            FileInputStream
     *        Content provider   content://	        ContentResolver
     *        asset目录下的资源    asset://	        AssetManager
     *        res目录下的资源      res://	            Resources.openRawResource
     *           res 示例:Uri uri = Uri.parse("res://包名(实际可以是任何字符串甚至留空)/" + R.drawable.ic_launcher);
     * @param width  指定图片加载宽度
     * @param height  指定图片加载高度
     * @param clearCache 是否先清空此url的图片缓存
     */
    public void fetchSimpleDraweeView(SimpleDraweeView imageView, String url,
                                      int width, int height, boolean clearCache) {
        fetchSimpleDraweeView(imageView, url, width, height, clearCache, null);
    }

    /**
     * 让imageView显示出url下的图片
     * @param imageView SimpleDraweeView
     * @param url 图片地址
     *        远程图片            http://, https://	HttpURLConnection
     *        本地文件            file://	            FileInputStream
     *        Content provider   content://	        ContentResolver
     *        asset目录下的资源    asset://	        AssetManager
     *        res目录下的资源      res://	            Resources.openRawResource
     *           res 示例:Uri uri = Uri.parse("res://包名(实际可以是任何字符串甚至留空)/" + R.drawable.ic_launcher);
     * @param width  指定图片加载宽度
     * @param height  指定图片加载高度
     * @param clearCache 是否先清空此url的图片缓存
     */
    public void fetchSimpleDraweeView(SimpleDraweeView imageView, String url,
                                      int width, int height, boolean clearCache,
                                      final OnImageFetchCompletedListener listener) {
        if (imageView != null && !TextUtils.isEmpty(url)) {
            if (url.startsWith("http") || url.startsWith("file") ||
                    url.startsWith("content") ||
                    url.startsWith("asset") || url.startsWith("res")) {
                final Uri uri = Uri.parse(url);
                if (clearCache) {
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    imagePipeline.evictFromMemoryCache(uri);
                    imagePipeline.evictFromDiskCache(uri);
                    imagePipeline.evictFromCache(uri);
                }
                ImageRequest imageRequest = ImageRequestBuilder
                        .newBuilderWithSource(uri)
                        .setResizeOptions(new ResizeOptions(width, height))
                        .build();
                DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(imageView.getController())
                        .setImageRequest(imageRequest)
                        .setControllerListener(new ControllerListener<ImageInfo>() {
                            @Override
                            public void onSubmit(String id, Object callerContext) {
                            }

                            @Override
                            public void onFinalImageSet(String id,
                                                        @javax.annotation.Nullable ImageInfo imageInfo,
                                                        @javax.annotation.Nullable Animatable animatable) {
                                Logger.d("Data", "fetchSimpleDraweeView onFinalImageSet");
                                if (listener != null) {
                                    listener.onImageFetchCompleted(uri,
                                            imageInfo == null ? 0 : imageInfo.getWidth(),
                                            imageInfo == null ? 0 : imageInfo.getHeight());
                                }
                            }

                            @Override
                            public void onIntermediateImageSet(String id,
                                                               @javax.annotation.Nullable ImageInfo imageInfo) {
                            }

                            @Override
                            public void onIntermediateImageFailed(String id, Throwable throwable) {
                            }

                            @Override
                            public void onFailure(String id, Throwable throwable) {
                            }

                            @Override
                            public void onRelease(String id) {
                            }
                        })
                        .build();
                imageView.setController(controller);
            }
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private OkHttpClient mOkHttpClient = null;
    private Retrofit mRetrofit = null;
    private final Map<String, List<Call<?>>> mRequestMap = new HashMap<>();
    private static final byte[] mRequestSync = new byte[]{0};

    // 初始化OkHttpClient
    private void initOkHttpClient(Context context,
                                  /*@RawRes int[] certificates, String[] hostnameVerifierHosts,*/
                                  long memoryCacheTimeout,
                                  String logPath, DataLogFilter.OnRequestAliPathListener listener,
                                  boolean debuggable) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
//        bindSSLSocketFactory(context, builder, certificates);
//        bindHostnameVerifier(builder, hostnameVerifierHosts);

        // 设置超时
        builder.connectTimeout(TIMEOUT_CONNECT, TimeUnit.SECONDS);
        builder.writeTimeout(TIMEOUT_WRITE, TimeUnit.SECONDS);
        builder.readTimeout(TIMEOUT_READ, TimeUnit.SECONDS);

        // 设置DataMemoryCacheInterceptor
        builder.addInterceptor(new DataMemoryCacheInterceptor(memoryCacheTimeout));

        // debuggable
        if (debuggable) {
            // Log
            DataLogger.LogUtil.init(true);
            builder.addInterceptor(new DataLoggingInterceptor(new DataLogger()));
            // Stetho
            Stetho.initializeWithDefaults(context);
            builder.addNetworkInterceptor(new StethoInterceptor());
        }

        // Log write to path
        try {
            if (!TextUtils.isEmpty(logPath)) {
                final File path = new File(logPath);
                if (path.exists() && !path.isDirectory()) {
                    path.delete();
                }
                if (!path.exists()) {
                    path.mkdirs();
                }
                if (path.exists() && path.isDirectory()) {
                    builder.addInterceptor(
                      new DataLogFileInterceptor(new DataLogFilter(debuggable, logPath, listener)));
                }
            }
        } catch (Exception ignored) {}

        // 生成OkHttpClient
        mOkHttpClient = builder.build();
    }

    // 初始化CallFactory
    public static class DataCallFactoryProxy implements okhttp3.Call.Factory {
        public static final String NAME_BASE_URL = "HeaderBaseUrlName";
        private okhttp3.Call.Factory mDelegate;
        private OnDataCallNewUrlListener mOnDataCallNewUrlListener;

        DataCallFactoryProxy(@NonNull okhttp3.Call.Factory delegate,
                             @Nullable OnDataCallNewUrlListener dataCallNewUrlListener) {
            mDelegate = delegate;
            mOnDataCallNewUrlListener = dataCallNewUrlListener;
        }

        @NotNull
        @Override
        public okhttp3.Call newCall(@NotNull Request request) {
            if (mOnDataCallNewUrlListener == null) {
                return mDelegate.newCall(request);
            }

            String baseUrlName = request.header(NAME_BASE_URL);
            if (TextUtils.isEmpty(baseUrlName)) {
                return mDelegate.newCall(request);
            }

            HttpUrl newHttpUrl = mOnDataCallNewUrlListener.onDataCallNewUrl(baseUrlName, request);
            if (newHttpUrl == null) {
                return mDelegate.newCall(request);
            }

            Request newRequest = request.newBuilder().url(newHttpUrl).build();
            return mDelegate.newCall(newRequest);
        }
    }

    public interface OnDataCallNewUrlListener {
        @Nullable HttpUrl onDataCallNewUrl(String baseUrlName, Request request);
    }

    // 初始化Retrofit
    private void initRetrofit(Context context, String baseUrl,
                              OnDataCallNewUrlListener dataCallNewUrlListener) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .callFactory(new DataCallFactoryProxy(mOkHttpClient, dataCallNewUrlListener))
                .addConverterFactory(DataConverterFactory.create());
        mRetrofit = builder.build();
    }

    // 初始化Fresco
    private void initFresco(Context context) {
        ImagePipelineConfig.Builder builder = OkHttpImagePipelineConfigFactory
                .newBuilder(context, mOkHttpClient);
        ImagePipelineConfig config = builder.build();
        Fresco.initialize(context, config);
    }

    // 绑定证书
    private void bindSSLSocketFactory(Context context, OkHttpClient.Builder builder,
                                      @RawRes int[] certificates) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            for (int i = 0; i < certificates.length; i++) {
                InputStream certificate = context.getResources().openRawResource(certificates[i]);
                keyStore.setCertificateEntry(String.valueOf(i), certificateFactory.generateCertificate(certificate));
                if (certificate != null) {
                    certificate.close();
                }
            }
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
            sslContext.init(null, new TrustManager[] { trustManager }, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, trustManager);
        } catch (Exception ignored) {}
    }

    // 构建HostnameVerifier
    private void bindHostnameVerifier(OkHttpClient.Builder builder, final String[] hostUrls) {
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                boolean ret = false;
                for (String host : hostUrls) {
                    if (host.equalsIgnoreCase(hostname)) {
                        ret = true;
                        break;
                    }
                }
                return ret;
            }
        };
        builder.hostnameVerifier(hostnameVerifier);
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    /**
     * 收集设备的信息
     */
    private JSONObject collectDeviceInfo(Context context) {
        JSONObject info = new JSONObject();
        DisplayMetrics dm = new DisplayMetrics();
        DisplayMetrics rdm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(dm);
            wm.getDefaultDisplay().getRealMetrics(rdm);
        }

        String packageName = "";
        int versionCode = 0;
        String versionName = "";
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            packageName = pi.packageName;
            versionCode = pi.versionCode;
            versionName = pi.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}

        @SuppressLint("HardwareIds")
        String androidID = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        @SuppressLint("HardwareIds")
        String id = androidID + Build.SERIAL;
        if (TextUtils.isEmpty(id)) {
            id = UUID.randomUUID().toString();
        }
        String imei = NUtils.md5(id);

        try {
            // OS Name
            info.put("OS_NAME", "Android");
            // App Package Name
            info.put("APP_PACKAGE_NAME", packageName);
            // App Version
            info.put("APP_VERSION_CODE", versionCode);
            info.put("APP_VERSION_NAME", versionName);
            // 系统版本号
            info.put("SDK_INT", Build.VERSION.SDK_INT);
            info.put("VERSION.RELEASE", Build.VERSION.RELEASE);
            // IMEI
            info.put("IMEI", imei);
            // 屏幕宽高
            info.put("SCREEN_WIDTH", dm.widthPixels);
            info.put("SCREEN_HEIGHT", dm.heightPixels);
            info.put("SCREEN_REAL_WIDTH", rdm.widthPixels);
            info.put("SCREEN_REAL_HEIGHT", rdm.heightPixels);
            // 设置参数
            info.put("DEVICE", Build.DEVICE);
            // 显示屏参数
            info.put("DISPLAY", Build.DISPLAY);
            // 硬件制造商
            info.put("MANUFACTURER", Build.MANUFACTURER);
            // 手机制造商
            info.put("PRODUCT", Build.PRODUCT);
            // 手机型号
            info.put("MODEL", Build.MODEL);
            // 系统定制商
            info.put("BRAND", Build.BRAND);
            // 主板
            info.put("BOARD", Build.BOARD);
        } catch (JSONException ignored) {}

        return info;
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    /**
     * 上传文件到阿里云
     */
    public void uploadFileToAliOSS(final String aliBPath,
                                   final String fileName,
                                   final DataUploadListener uploadListener) {
        if (TextUtils.isEmpty(fileName)) {
            return;
        }

        if (hasConnectedNetwork()) {
            AliOSS.getInstance().uploadFileToAliOSS(aliBPath, fileName, uploadListener);
        } else {
            if (uploadListener != null) uploadListener.onUploadError(fileName, new Exception("No Network."));
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    // 判断是否有可用网络
    static boolean hasConnectedNetwork() {
        ConnectivityManager cm =(ConnectivityManager) BaseApplication.getApp()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } else {
            return false;
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    static String getRequestKey(Request request) {
        String key = request.toString();
        RequestBody body = request.body();
        if (body instanceof FormBody) {
            Buffer buffer = new Buffer();
            try {
                body.writeTo(buffer);
                key += buffer.readString(StandardCharsets.UTF_8);
            } catch (IOException ignored) { }
        }
        return NUtils.md5(key);
    }
}
