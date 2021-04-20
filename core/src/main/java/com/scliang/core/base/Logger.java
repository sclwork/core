package com.scliang.core.base;

import android.content.Context;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/9/29.
 */
public class Logger {

    public interface OnLoggerListener {
        void onLogI(String buildType, String tag, String msg);
        void onLogD(String buildType, String tag, String msg);
        void onLogE(String buildType, String tag, String msg);
        void onLogV(String buildType, String tag, String msg);
        void onLogW(String buildType, String tag, String msg);
    }

    private Logger() {}
    private static SoftReference<OnLoggerListener> sOnLoggerListener;

    private static String BuildType;
    private static boolean IsRelease;
    private static boolean IsBeta;
    private static boolean Loggable;
    static {
        final Context context = BaseApplication.getApp().getApplicationContext();
        try {
            Class<?> clz = Class.forName(context.getPackageName() + ".BuildConfig");
            Field field = clz.getField("BUILD_TYPE");
            BuildType = (String) field.get(null);
            IsRelease = "release".equalsIgnoreCase(BuildType);
            IsBeta = "beta".equalsIgnoreCase(BuildType);
            Loggable = !IsRelease;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
            BuildType = "release";
            IsRelease = true;
            IsBeta = false;
            Loggable = false;
        }
    }

    public static void setOnLoggerListener(OnLoggerListener listener) {
        sOnLoggerListener = new SoftReference<>(listener);
    }

    public static String getBuildType() {
        return BuildType;
    }

    public static boolean isRelease() {
        return IsRelease;
    }

    public static boolean isBeta() {
        return IsBeta;
    }

    public static boolean isLoggable() {
        return Loggable;
    }

    public static void i(String tag, String msg) {
        callOnLoggerListener(1, BuildType, tag, msg);
        if (isLoggable()) Log.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        callOnLoggerListener(2, BuildType, tag, msg);
        if (isLoggable()) Log.d(tag, msg);
    }

    public static void e(String tag, String msg) {
        callOnLoggerListener(3, BuildType, tag, msg);
        if (isLoggable()) Log.e(tag, msg);
    }

    public static void v(String tag, String msg) {
        callOnLoggerListener(4, BuildType, tag, msg);
        if (isLoggable()) Log.v(tag, msg);
    }

    public static void w(String tag, String msg) {
        callOnLoggerListener(5, BuildType, tag, msg);
        if (isLoggable()) Log.w(tag, msg);
    }

    private static void callOnLoggerListener(int type, String buildType, String tag, String msg) {
        if (sOnLoggerListener == null) {
            return;
        }

        OnLoggerListener listener = sOnLoggerListener.get();

        if (listener == null) {
            return;
        }

        switch (type) {
            case 1: listener.onLogI(buildType, tag, msg); break;
            case 2: listener.onLogD(buildType, tag, msg); break;
            case 3: listener.onLogE(buildType, tag, msg); break;
            case 4: listener.onLogV(buildType, tag, msg); break;
            case 5: listener.onLogW(buildType, tag, msg); break;
        }
    }
}
