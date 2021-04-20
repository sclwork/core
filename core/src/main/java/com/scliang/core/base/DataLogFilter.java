package com.scliang.core.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.scliang.core.base.rsa.Base64Utils;
import com.scliang.core.base.rsa.RSAProvider;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/8/27.
 */
public final class DataLogFilter implements DataLogFileInterceptor.Logger {
    private static boolean mDebuggable;
    private static String mLogPath;
    private static SoftReference<OnRequestAliPathListener> mOnRequestAliPathListener;

    public interface OnRequestAliPathListener {
        String onRequestPubKey();
        String onRequestAliPath(String fileName);
    }

    DataLogFilter(boolean debuggable, String logPath, OnRequestAliPathListener listener) {
        mDebuggable = debuggable;
        mLogPath = logPath;
        mOnRequestAliPathListener = new SoftReference<>(listener);
    }

    public static final class LogItem {
        public String name;
        public String path;

        long getNameLong() {
            if (TextUtils.isEmpty(name)) {
                return 0;
            }

            if (name.length() <= 5) {
                return 0;
            }

            final String str = name.substring(0, name.length() - 5);
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    public interface OnLogItemsListener {
        void onLogItemsCompleted(List<LogItem> logs);
    }

    public static void getLogFileItems(final OnLogItemsListener listener) {
        if (listener == null) {
            return;
        }

        LogHandleUtil.post(() -> {
            List<LogItem> items = new ArrayList<>();
            if (!TextUtils.isEmpty(mLogPath)) {
                final File path = new File(mLogPath);
                if (path.exists() && path.isDirectory()) {
                    File[] files = path.listFiles((dir, name) -> name.endsWith(".html"));
                    if (files != null) {
                        for (File file : files) {
                            LogItem item = new LogItem();
                            item.name = file.getName();
                            item.path = file.getAbsolutePath();
                            items.add(item);
                        }
                    }
                }
            }
            Collections.sort(items, (o1, o2) -> (int) (o2.getNameLong() - o1.getNameLong()));
            listener.onLogItemsCompleted(items);
        });
    }

    @Override
    public void log(String message) {
        if (!TextUtils.isEmpty(message)) {
            LogHandleUtil.post(message);
        }
    }

    static class LogHandleUtil {
        private HandlerThread mThread;
        private Handler mHandler;
        private SimpleDateFormat mSimpleDateFormat;
        private String mLogName;
        private RandomAccessFile mLogFile;
        private String mWaitUploadLogName;
        private SharedPreferences mSP;

        private LogHandleUtil() {
            mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.CHINESE);
            BaseApplication app = BaseApplication.getApp();
            if (app != null) mSP = app.getSharedPreferences(
              "LogHandleUtil", Context.MODE_PRIVATE);
            if (mSP != null) mLogName = mSP.getString("LogName", "");
        }

        private static class SingletonHolder {
            private static final LogHandleUtil INSTANCE = new LogHandleUtil();
        }

        private static LogHandleUtil getInstance() {
            return LogHandleUtil.SingletonHolder.INSTANCE;
        }

        private void renewThread() {
            if (mThread != null) {
                mThread.quitSafely();
                mThread = null;
            }

            mThread = new HandlerThread("LogHandleUtil-" + System.currentTimeMillis());
            mThread.start();
            mHandler = new Handler(mThread.getLooper()) {
                @Override
                public void handleMessage(@NotNull Message msg) {
                    if (msg.what == 100) {
                        final String log = (String) msg.obj;
                        writeLogToFile(log);
                    }
                }
            };
        }

        private void checkoutThread() {
            if (mThread != null && mThread.isAlive() && mHandler != null) {
                return;
            }

            renewThread();
        }

        static void post(Runnable runnable) {
            getInstance().checkoutThread();
            if (getInstance().mHandler != null) {
                getInstance().mHandler.post(runnable);
            }
        }

        static void post(String log) {
            getInstance().checkoutThread();
            if (getInstance().mHandler != null) {
                getInstance().mHandler.sendMessage(
                        getInstance().mHandler
                                .obtainMessage(100, log));
            }
        }

        private void createLogFile(File file) {
            try {
                mLogFile = new RandomAccessFile(file, "rw");
            } catch (IOException ignored) { }
        }

        private void checkLogFile() {
            final String now = String.format(Locale.CHINESE,
                    mDebuggable ? "%s/%s.html" : "%s/lg%s.txt",
              mLogPath, mSimpleDateFormat.format(new Date()));
            // 当前日志文件名称
            if (now.equals(mLogName)) {
                final File file = new File(mLogName);
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                        if (mLogFile != null) {
                            try {
                                mLogFile.close();
                            } catch (IOException ignored) { }
                            mLogFile = null;
                        }
                    } catch (IOException ignored) { }
                }
                if (file.exists() && mLogFile == null) {
                    createLogFile(file);
                }
            }
            // 需要创建新名字的日志文件
            else {
                if (mLogFile != null) {
                    try {
                        mLogFile.close();
                    } catch (IOException ignored) { }
                    mLogFile = null;
                }
                mWaitUploadLogName = mLogName;
                mLogName = now;
                if (mSP != null) mSP.edit().putString("LogName", mLogName).apply();
                final File file = new File(mLogName);
                try {
                    file.createNewFile();
                } catch (IOException ignored) { }
                if (file.exists()) {
                    createLogFile(file);
                }
                // 上传上一次日志文件
                if (!mDebuggable &&
                    !TextUtils.isEmpty(mWaitUploadLogName) &&
                    mWaitUploadLogName.endsWith("txt")) {
                    post(uploadLogRunnable);
                }
            }
        }

        private void writeLogToFile(String log) {
            if (!Permission.hasStoragePermission(new OnCheckPermissionImpl())) {
                return;
            }

            OnRequestAliPathListener listener =
              mOnRequestAliPathListener == null ? null : mOnRequestAliPathListener.get();
            String pubKey = listener == null ? null : listener.onRequestPubKey();
            if (!mDebuggable && TextUtils.isEmpty(pubKey)) {
                return;
            }

            checkLogFile();
            if (mLogFile != null) {
                try {
                    mLogFile.seek(mLogFile.length());
                    String content = decodeUnicode(log);
                    if (mDebuggable) {
                        mLogFile.writeUTF(content);
                    } else {
                        try {
                            byte[] encode = RSAProvider.encryptPublicKey(
                              content.getBytes(), pubKey);
                            content = Base64Utils.encode(encode).trim();
                        } catch (Exception ignored) { }
                        mLogFile.write(String.format(Locale.CHINESE,
                          "\n--------------------------------------\n" +
                            "%s" +
                            "\n--------------------------------------\n",
                          content).getBytes());
                    }
                } catch (IOException ignored) {
                    if (mLogFile != null) {
                        try { mLogFile.close(); } catch (IOException ignored1) { }
                    }
                    mLogFile = null;
                }
            }
        }

        private Runnable uploadLogRunnable = new Runnable() {
            @Override
            public void run() {
                final String fileName = mWaitUploadLogName;
                if (TextUtils.isEmpty(fileName)) {
                    return;
                }

                File file = new File(fileName);
                if (!file.exists()) {
                    return;
                }

                OnRequestAliPathListener listener =
                  mOnRequestAliPathListener == null ? null : mOnRequestAliPathListener.get();
                if (listener == null) {
                    return;
                }

                String path = listener.onRequestAliPath(fileName);
                if (TextUtils.isEmpty(path)) {
                    return;
                }

                Data.getInstance().uploadFileToAliOSS(path, fileName, null);
            }
        };
    }

    private static String decodeUnicode(String theString) {
        char aChar;
        int len = theString.length();
        StringBuilder outBuffer = new StringBuilder(len);
        for (int x = 0; x < len; ) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
//                                throw new IllegalArgumentException(
//                                        "Malformed   \\uxxxx   encoding.");
                                value = 0;
                                break;
                        }

                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }
}
