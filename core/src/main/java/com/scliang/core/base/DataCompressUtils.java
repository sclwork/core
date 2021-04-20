package com.scliang.core.base;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/11/9.
 */
public final class DataCompressUtils {
    private HandlerThread mThread;
    private Handler mHandler;

    private DataCompressUtils() {
    }

    private static class SingletonHolder {
        private static final DataCompressUtils INSTANCE = new DataCompressUtils();
    }

    private static DataCompressUtils getInstance() {
        return DataCompressUtils.SingletonHolder.INSTANCE;
    }

    private void renewThread() {
        if (mThread != null) {
            mThread.quitSafely();
            mThread = null;
        }

        mThread = new HandlerThread("DataCompressUtils-" + System.currentTimeMillis());
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
    }

    private void checkoutThread() {
        if (mThread != null && mThread.isAlive() && mHandler != null) {
            return;
        }

        renewThread();
    }

    public static void post(Runnable runnable) {
        getInstance().checkoutThread();
        if (getInstance().mHandler != null) {
            getInstance().mHandler.post(runnable);
        }
    }
}
