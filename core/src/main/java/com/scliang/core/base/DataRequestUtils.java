package com.scliang.core.base;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/11/21.
 */
final class DataRequestUtils {
    static void post(DataRequestRunnable runnable) {
        if (runnable == null) {
            return;
        }

        Call call = runnable.mCall;

        if (call == null) {
            return;
        }

        Request request = call.request();
        if (request == null) {
            return;
        }

        runnable.posted();

        RequestBody body = request.body();
        if (body == null || "GET".equalsIgnoreCase(request.method())) {
            _GetUtils.post(runnable);
        } else {
            if (body instanceof MultipartBody) {
                _MultipartUtils.post(runnable);
            } else {
                _FormUtils.post(runnable);
            }
        }
    }

    static final class DataRequestRunnable<T> implements Runnable {
        private Questioner mQuestioner;
        private Call<T> mCall;
        private DataCallback<T> mCallback;
        private final Handler fUIHandler = new Handler(Looper.getMainLooper());

        DataRequestRunnable(final Questioner questioner,
                            final Call<T> call,
                            final DataCallback<T> callback) {
            mQuestioner = questioner;
            mCall = call;
            mCallback = callback;
        }

        final void posted() {
            fUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mQuestioner.responseCallbackable() && mCallback != null) {
                        mCallback.onWaiting(mCall);
                    }
                }
            });
        }

        @Override
        public void run() {
            if (Data.hasConnectedNetwork()) {
                if (mCall != null) {
                    fUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mQuestioner.responseCallbackable() && mCallback != null) {
                                mCallback.onRequest(mCall);
                            }
                        }
                    });

                    try {
                        final Response<T> response = mCall.execute();
                        // 标记是否Error
                        boolean isError = false;
                        // 执行完成，移除Call
                        Data.getInstance().removeCall(mQuestioner, mCall);

                        // 接口是否被踢出、禁止、拒绝
                        try {
                            T result = response.body();
                            if (result != null) {
                                Method method = result.getClass().getMethod("isError");
                                isError = (boolean) method.invoke(result);
                                if (isError) {
                                    method = result.getClass().getMethod("getCode");
                                    final int code = (int) method.invoke(result);
                                    method = result.getClass().getMethod("getMsg");
                                    final String msg = (String) method.invoke(result);
                                    fUIHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mQuestioner.responseCallbackable()) {
                                                mQuestioner.onQuestionerError(code, msg);
                                            }
                                        }
                                    });
                                }
                            }
                        } catch (NoSuchMethodException |
                                IllegalAccessException |
                                InvocationTargetException ignored) { }

                        // 标记Error后不执行以下操作
                        if (isError) {
                            return;
                        }

                        // 接口是否执行成功
                        if (mQuestioner.questionerResponsable()) {
                            try {
                                T result = response.body();
                                if (result != null) {
                                    Method method = result.getClass().getMethod("isSuccess");
                                    boolean isSuccess = (boolean) method.invoke(result);
                                    if (isSuccess) {
                                        fUIHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (mQuestioner.responseCallbackable()) {
                                                    mQuestioner.onQuestionerResponseSuccess();
                                                }
                                            }
                                        });
                                    }
                                }
                            } catch (NoSuchMethodException |
                                    IllegalAccessException |
                                    InvocationTargetException ignored) { }
                        }

                        // 判断是否为缓存Response
                        Headers headers = response.headers();
                        boolean isCache = headers != null &&
                                ("MEMCACHE".equals(headers.get("MEMCACHE")));
                        final T result = response.body();
                        try {
                            if (result != null) {
                                Method method = result.getClass()
                                        .getMethod("setCache", boolean.class);
                                method.invoke(result, isCache);
                            }
                        } catch (NoSuchMethodException |
                                IllegalAccessException |
                                InvocationTargetException ignored) { }

                        // 响应回调
                        fUIHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mQuestioner.responseCallbackable() && mCallback != null) {
                                    mCallback.onResponse(mCall, result);
                                }
                            }
                        });
                    } catch (final IOException e) {
                        Data.getInstance().removeCall(mQuestioner, mCall);
                        fUIHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mQuestioner.responseCallbackable() && mCallback != null) {
                                    mCallback.onFailure(mCall, e);
                                }
                            }
                        });
                    }
                }
            } else {
                fUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mQuestioner.responseCallbackable() && mCallback != null) {
                            mCallback.onNoNetwork(mCall);
                        }
                    }
                });
            }
        }
    }




    private static class _GetUtils {
        private HandlerThread mThread;
        private Handler mHandler;

        private _GetUtils() {
        }

        private static class SingletonHolder {
            private static final _GetUtils INSTANCE = new _GetUtils();
        }

        private static _GetUtils getInstance() {
            return _GetUtils.SingletonHolder.INSTANCE;
        }

        private void renewThread() {
            if (mThread != null) {
                mThread.quitSafely();
                mThread = null;
            }

            mThread = new HandlerThread("_GetUtils-" + System.currentTimeMillis());
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
        }

        private void checkoutThread() {
            if (mThread != null && mThread.isAlive() && mHandler != null) {
                return;
            }

            renewThread();
        }

        private static void post(Runnable runnable) {
            getInstance().checkoutThread();
            if (getInstance().mHandler != null) {
                getInstance().mHandler.post(runnable);
            }
        }
    }

    private static class _FormUtils {
        private HandlerThread mThread;
        private Handler mHandler;

        private _FormUtils() {
        }

        private static class SingletonHolder {
            private static final _FormUtils INSTANCE = new _FormUtils();
        }

        private static _FormUtils getInstance() {
            return _FormUtils.SingletonHolder.INSTANCE;
        }

        private void renewThread() {
            if (mThread != null) {
                mThread.quitSafely();
                mThread = null;
            }

            mThread = new HandlerThread("_FormUtils-" + System.currentTimeMillis());
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
        }

        private void checkoutThread() {
            if (mThread != null && mThread.isAlive() && mHandler != null) {
                return;
            }

            renewThread();
        }

        private static void post(Runnable runnable) {
            getInstance().checkoutThread();
            if (getInstance().mHandler != null) {
                getInstance().mHandler.post(runnable);
            }
        }
    }

    private static class _MultipartUtils {
        private HandlerThread mThread;
        private Handler mHandler;

        private _MultipartUtils() {
        }

        private static class SingletonHolder {
            private static final _MultipartUtils INSTANCE = new _MultipartUtils();
        }

        private static _MultipartUtils getInstance() {
            return _MultipartUtils.SingletonHolder.INSTANCE;
        }

        private void renewThread() {
            if (mThread != null) {
                mThread.quitSafely();
                mThread = null;
            }

            mThread = new HandlerThread("_MultipartUtils-" + System.currentTimeMillis());
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
        }

        private void checkoutThread() {
            if (mThread != null && mThread.isAlive() && mHandler != null) {
                return;
            }

            renewThread();
        }

        private static void post(Runnable runnable) {
            getInstance().checkoutThread();
            if (getInstance().mHandler != null) {
                getInstance().mHandler.post(runnable);
            }
        }
    }
}
