package com.scliang.core.base;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken;
import com.alibaba.sdk.android.oss.model.ListObjectsRequest;
import com.alibaba.sdk.android.oss.model.ListObjectsResult;
import com.alibaba.sdk.android.oss.model.OSSObjectSummary;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/10/12.
 */
public class AliOSS {
    protected AliOSS() {
    }

    private static class SingletonHolder {
        private static final AliOSS INSTANCE = new AliOSS();
    }

    public static AliOSS getInstance() {
        return AliOSS.SingletonHolder.INSTANCE;
    }

    protected SoftReference<Context> mContext;

    /**
     * 使用ApplicationContext初始化AliOSS工具
     */
    public void init(BaseApplication application,
                     final String aliBU, final String aliBN,
                     final OnRequestFederationTokenListener tokenListener) {
        final Context context = application.getApplicationContext();
        mContext = new SoftReference<>(context);
        mOnRequestFederationTokenListener = tokenListener;
        setAliOSSConfigs(aliBU, aliBN);
    }

    protected String mAliBU;
    protected String mAliBN;
    protected OnRequestFederationTokenListener mOnRequestFederationTokenListener;
    protected AliOPHandler mAliOPHandler;

    public interface OnRequestFederationTokenListener {
        OSSFederationToken onRequestOSSFederationToken();
    }

    protected static class AliOPParams {
        SoftReference<Context> mContext;
        DataUploadListener mDataUploadListener;
        AliOSSListObjectListener mAliOSSListObjectListener;
        OnRequestFederationTokenListener mOnRequestFederationTokenListener;
        String mAliBU;
        String mAliBN;
        String mAliPath;
        String mFileName;
        // UGC视频提交，问题ID
        String mQId;
        // UGC视频提交，是否使用路由器
        boolean useRouter;
    }

    protected static class AliOPHandler extends Handler {
        private SoftReference<AliOSS> mAliOSS;

        AliOPHandler(Looper looper, AliOSS oss) {
            super(looper);
            mAliOSS = new SoftReference<>(oss);
        }

        public void upload(AliOPParams params) {
            File file = new File(params.mFileName);
            if (file.exists() && file.isFile()) {
                sendMessage(obtainMessage(100, params));
            }
        }

        public void list(AliOPParams params) {
            sendMessage(obtainMessage(200, params));
        }

        @Override
        public void handleMessage(@NotNull Message msg) {
            AliOSS oss = mAliOSS == null ? null : mAliOSS.get();
            if (oss == null) {
                return;
            }

            // upload
            if (msg.what == 100) {
                AliOPParams params = (AliOPParams) msg.obj;
                oss.uploadOneObject(params);
            }

            // list
            else if (msg.what == 200) {
                AliOPParams params = (AliOPParams) msg.obj;
                oss.listObjects(params);
            }
        }
    }

    protected void checkAliOPHandler() {
        if (mAliOPHandler == null) {
            HandlerThread thread = new HandlerThread("Data.AliOPThread-" + System.currentTimeMillis());
            thread.start();
            mAliOPHandler = new AliOPHandler(thread.getLooper(), this);
        }
    }

    /**
     * 设置阿里云参数
     */
    public void setAliOSSConfigs(final String aliBU, final String aliBN) {
        mAliBU = aliBU;
        mAliBN = aliBN;
    }

    /**
     * 上传文件到阿里云
     */
    public void uploadFileToAliOSS(final String aliPath,
                                   final String fileName,
                                   final DataUploadListener uploadListener) {
        uploadFileToAliOSS(mAliBU, mAliBN, aliPath, fileName, uploadListener);
    }

    /**
     * 上传文件到阿里云
     */
    public void uploadFileToAliOSS(final String aliBU, final String aliBN,
                                   final String aliPath,
                                   final String fileName,
                                   final DataUploadListener uploadListener) {
        if (!TextUtils.isEmpty(aliBU) && !TextUtils.isEmpty(aliBN) && !TextUtils.isEmpty(fileName)) {
            checkAliOPHandler();
            if (mAliOPHandler != null) {
                AliOPParams params = new AliOPParams();
                params.mContext = new SoftReference<>(mContext.get());
                params.mDataUploadListener = uploadListener;
                params.mOnRequestFederationTokenListener = mOnRequestFederationTokenListener;
                params.mAliBU = aliBU;
                params.mAliBN = aliBN;
                params.mAliPath = aliPath;
                params.mFileName = fileName;
                mAliOPHandler.upload(params);
            }
        }
    }

    public void listAliOSSObjects(final String aliPath,
                                  final AliOSSListObjectListener listObjectListener) {
        listAliOSSObjects(mAliBU, mAliBN, aliPath, listObjectListener);
    }

    public void listAliOSSObjects(final String aliBU, final String aliBN, final String aliPath,
                                  final AliOSSListObjectListener listObjectListener) {
        if (!TextUtils.isEmpty(aliBU) && !TextUtils.isEmpty(aliBN)) {
            checkAliOPHandler();
            if (mAliOPHandler != null) {
                AliOPParams params = new AliOPParams();
                params.mContext = new SoftReference<>(mContext.get());
                params.mAliOSSListObjectListener = listObjectListener;
                params.mOnRequestFederationTokenListener = mOnRequestFederationTokenListener;
                params.mAliBU = aliBU;
                params.mAliBN = aliBN;
                params.mAliPath = aliPath;
                mAliOPHandler.list(params);
            }
        }
    }

    protected void uploadOneObject(final AliOPParams params) {
        File file = new File(params.mFileName);
        if (file.exists()) {
            final Handler uiHandler = new Handler(Looper.getMainLooper());
            final DataUploadListener updateListener = params.mDataUploadListener;
            final OnRequestFederationTokenListener tokenListener = params.mOnRequestFederationTokenListener;
            try {
                uiHandler.post(() -> {
                    if (updateListener != null) updateListener.onUploadStarted(params.mFileName);
                });
                long old = System.currentTimeMillis();
                final String fileKey = TextUtils.isEmpty(params.mAliPath) ? file.getName() : params.mAliPath + "/" + file.getName();

                OSSCredentialProvider provider = new OSSFederationCredentialProvider() {
                    @Override
                    public OSSFederationToken getFederationToken() throws ClientException {
                        if (tokenListener == null) {
                            throw new ClientException("Create FederationToken fail.");
                        }
                        return tokenListener.onRequestOSSFederationToken();
                    }
                };
                ClientConfiguration configuration = new ClientConfiguration();
                configuration.setSocketTimeout(30000);
                configuration.setConnectionTimeout(30000);
                configuration.setMaxConcurrentRequest(10);

                final Context context = params.mContext == null ? null : params.mContext.get();
                if (context == null) {
                    throw new Exception("Context is null.");
                }

                OSS oss = new OSSClient(context, params.mAliBU, provider, configuration);

//                    String eTag;
//                    if (isNetworkMobile(params)) {
//                        MultipartUploadRequest request = new MultipartUploadRequest(params.mAliN, fileKey, params.mFileName);
//                        CompleteMultipartUploadResult result = oss.multipartUpload(request);
//                        eTag = result.getETag();
//                    } else {
//                        PutObjectRequest request = new PutObjectRequest(params.mAliBN, fileKey, params.mFileName);
//                        PutObjectResult result = oss.putObject(request);
//                        eTag = result.getETag();
//                    }

                PutObjectRequest request = new PutObjectRequest(params.mAliBN, fileKey, params.mFileName);
                request.setProgressCallback((request1, currentSize, totalSize) -> {
                    Logger.d("UploadToAliOSS", "Upload onProgress: " + currentSize + "/" + totalSize);
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (updateListener != null) updateListener.onUploadProgress(
                                    params.mFileName, currentSize, totalSize);
                        }
                    });
                });

                PutObjectResult result = oss.putObject(request);
                String eTag = result.getETag();

                long time = System.currentTimeMillis() - old;
                Logger.d("UploadToAliOSS", "Upload Result Time:" + time + " eTag:" + eTag);
                final String url = oss.presignConstrainedObjectURL(params.mAliBN, fileKey, 7 * 24 * 60 * 60);
                Logger.d("UploadToAliOSS", "Upload Result Url:" + url);
                uiHandler.post(() -> {
                    if (updateListener != null) updateListener.onUploadCompleted(params.mFileName, fileKey, url);
                });
            } catch (final Exception e) {
                Logger.d("UploadToAliOSS", "Upload Exception:" + e.toString());
                uiHandler.post(() -> {
                    if (updateListener != null) updateListener.onUploadError(params.mFileName, e);
                });
            }
        }
    }

    protected void listObjects(final AliOPParams params) {
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        final AliOSSListObjectListener mListObjectListener = params.mAliOSSListObjectListener;
        final OnRequestFederationTokenListener tokenListener = params.mOnRequestFederationTokenListener;
        try {
            OSSCredentialProvider provider = new OSSFederationCredentialProvider() {
                @Override
                public OSSFederationToken getFederationToken() throws ClientException {
                    if (tokenListener == null) {
                        throw new ClientException("Create FederationToken fail.");
                    }
                    return tokenListener.onRequestOSSFederationToken();
                }
            };
            ClientConfiguration configuration = new ClientConfiguration();
            configuration.setSocketTimeout(30000);
            configuration.setConnectionTimeout(30000);
            configuration.setMaxConcurrentRequest(10);

            final Context context = params.mContext == null ? null : params.mContext.get();
            if (context == null) {
                throw new Exception("Context is null.");
            }

            OSS oss = new OSSClient(context, params.mAliBU, provider, configuration);

            ListObjectsRequest request = new ListObjectsRequest(params.mAliBN);
            request.setMaxKeys(1000);
            request.setPrefix(String.format(Locale.CHINESE, "%s/", params.mAliPath));
            ListObjectsResult result = oss.listObjects(request);

            final List<OSSObjectSummary> objects = new ArrayList<>();
            List<OSSObjectSummary> objectSummaries = result.getObjectSummaries();
            for (OSSObjectSummary summary : objectSummaries) {
                Logger.d("UploadToAliOSS", "ListObjects: " + summary.getKey());
                objects.add(summary);
            }

            // 如果还有数据，则再连续请求两次
            for (int i = 0; i < 2 && !TextUtils.isEmpty(result.getNextMarker()); i++) {
                request.setMarker(result.getNextMarker());
                result = oss.listObjects(request);
                objectSummaries = result.getObjectSummaries();
                for (OSSObjectSummary summary : objectSummaries) {
                    Logger.d("UploadToAliOSS", "ListObjects: " + summary.getKey());
                    objects.add(summary);
                }
            }

            uiHandler.post(() -> {
                if (mListObjectListener != null) mListObjectListener.onAliOSSListObjects(objects);
            });
        } catch (final Exception e) {
            Logger.d("UploadToAliOSS", "ListObjects Exception:" + e.toString());
            uiHandler.post(() -> {
                if (mListObjectListener != null) mListObjectListener.onAliOSSListObjects(null);
            });
        }
    }
}
