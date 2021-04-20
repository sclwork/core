package com.scliang.core.base;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.multidex.MultiDexApplication;

import android.text.TextUtils;
import android.view.LayoutInflater;

import com.scliang.core.R;
import com.scliang.core.media.audio.AudioRecorderManager;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Map;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/9/28.
 */
public abstract class BaseApplication
        extends MultiDexApplication
        implements Application.ActivityLifecycleCallbacks {
    private static BaseApplication sMe;
    private static ArrayList<BaseActivity> sActivities;
    private static SoftReference<BaseActivity> sPermissionActivity;

    // 注册本地广播
    private GlobalViewModel mGlobalViewModel;
    // 用于监听网络改变
    private NetworkStatusReceiver mNetworkStatusReceiver;
    // 用于监听耳机状态 - HeadsetPlugState
    private HeadsetPlugReceiver mHeadsetPlugReceiver;

    /**
     * 获得BaseApplication实例
     * @return BaseApplication实例
     */
    public static BaseApplication getApp() {
        return sMe;
    }

    public BaseApplication() {
        super();
        sMe = this;
        sActivities = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册Activity生命周期回调
        registerActivityLifecycleCallbacks(this);
        // 实例化并注册本地广播接收器
        mGlobalViewModel = ViewModelProvider.AndroidViewModelFactory
          .getInstance(this).create(GlobalViewModel.class);
        mGlobalViewModel.observeForever(new GlobalLiveDataObserver(this));
        // 实例化并注册网络状态监听器
        mNetworkStatusReceiver = new NetworkStatusReceiver(this);
        // 实例化并注册耳机状态监听器
        mHeadsetPlugReceiver = new HeadsetPlugReceiver(this);
    }

    @Override
    public void onTerminate() {
        // 解注册本地广播接收器
        if (mGlobalViewModel != null) {
            mGlobalViewModel.removeObserver();
        }
        // 解注册网络状态监听器
        if (mNetworkStatusReceiver != null) {
            mNetworkStatusReceiver.unregister();
        }
        // 解注册耳机状态监听器
        if (mHeadsetPlugReceiver != null) {
            mHeadsetPlugReceiver.unregister();
        }
        super.onTerminate();
    }

    @Override
    public final void onActivityCreated(Activity activity, Bundle bundle) {
        if (activity instanceof BaseActivity) {
            sActivities.add((BaseActivity) activity);
        }
        // 通知子类
        onActCreated(activity, bundle);
    }

    protected void onActCreated(Activity activity, Bundle bundle) {
        // nothing
    }

    @Override
    public final void onActivityDestroyed(Activity activity) {
        if (activity instanceof BaseActivity) {
            sActivities.remove(activity);
        }
        // 通知子类
        onActDestroyed(activity);
    }

    protected void onActDestroyed(Activity activity) {
        // nothing
    }

    @Override
    public final void onActivityResumed(Activity activity) {
    }

    @Override
    public final void onActivityPaused(Activity activity) {
    }

    @Override
    public final void onActivityStarted(Activity activity) {
    }

    @Override
    public final void onActivityStopped(Activity activity) {
    }

    @Override
    public final void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    /**
     * 请求权限
     * @param permission 申请的权限名称
     */
    public static void requestPermission(final String permission) {
        updatePermissionActivity();
        final BaseActivity activity = sPermissionActivity == null ? null : sPermissionActivity.get();
        if (activity != null) {
            activity.requestPermission(permission);
        }
    }

    /**
     * 权限请求结果回调
     * @param permission 权限名称
     * @param granted 是否请求成功
     */
    public void onRequestPermissionsResult(String permission, boolean granted) {
        // nothing
        Logger.d(getClass().getName(),
                "onRequestPermissionsResult : " +
                        permission + (granted ? "true" : "false"));
    }

    /**
     * 定制StatusBar的颜色
     */
    public int giveStatusBarColor() {
        return 0x00000000;
    }

    /**
     * finish所有的Activities
     */
    public void finishAllActivities() {
        for (BaseActivity activity : sActivities) {
            if (activity != null) {
                activity.finish();
                activity.overridePendingTransition(0, 0);
            }
        }
        sActivities.clear();
    }

    /**
     * 关闭所有现有的Activities，并打开给定的Fragment
     */
    public void clearStartFragment(Class<? extends BaseFragment> fragment, Bundle args) {
        clearStartFragment(fragment, null, args);
    }

    /**
     * 关闭所有现有的Activities，并打开给定的Fragment
     */
    public void clearStartFragment(final Class<? extends BaseFragment> fragment,
                                   final Class<? extends BaseActivity> activity,
                                   final Bundle args) {
        final Context context = this;
        final ArrayList<BaseActivity> activities = new ArrayList<>(sActivities);
        sActivities.clear();

        for (BaseActivity act : activities) {
            if (act != null) {
                act.finish();
                act.overridePendingTransition(0, 0);
            }
        }

        Class<?> _container = activity == null ? UniversalActivity.class : activity;
        Intent intent = new Intent(context, _container);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("fragment", fragment.getName());
        if (args != null) { intent.putExtra("args", args); }
        startActivity(intent);
    }

    /**
     * 关闭所有现有的Activities，并打开给定的Activity
     */
    public void clearStartActivity(final Class<? extends BaseActivity> activity,
                                   final Bundle args) {
        final Context context = this;
        final ArrayList<BaseActivity> activities = new ArrayList<>(sActivities);
        sActivities.clear();

        for (BaseActivity act : activities) {
            if (act != null) {
                act.finish();
                act.overridePendingTransition(0, 0);
            }
        }

        Intent intent = new Intent(context, activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (args != null) { intent.putExtras(args); }
        startActivity(intent);
    }

    /**
     * 设置Application级别的ContentMaskView
     */
    protected @LayoutRes
    int onGenerateContentMaskView() {
        return 0;
    }

    /**
     * 设置Application级别的ContentMaskReloadActionId
     */
    protected @IdRes
    int onGenerateContentMaskReloadActionId() {
        return 0;
    }

    /**
     * 设置Application级别的DataGlobalParams，用于Data数据请求中的bParams中
     */
    public Map<String, Object> onGenerateDataGlobalParams() {
        return null;
    }

    // 此App触发的所有请求，如果请求中响应符合ShotOff、Banned、Reject的要求，就会调用到这个方法
    final void questionerError(int code, String msg) {
        onQuestionerError(code, msg);
    }

    /**
     * 此App触发的所有请求，如果请求中响应符合ShotOff、Banned、Reject的要求，就会调用到这个方法
     */
    public void onQuestionerError(int code, String msg) {
        // nothing
    }

//    // 此App触发的所有请求，如果请求中响应符合Banned的要求，就会调用到这个方法
//    final void questionerBanned(String msg) {
//        onQuestionerBanned(msg);
//    }

//    /**
//     * 此App触发的所有请求，如果请求中响应符合Banned的要求，就会调用到这个方法
//     */
//    public void onQuestionerBanned(String msg) {
//        // nothing
//    }

    /**
     * 发布一个本地广播
     * @param action Action
     * @param args Args
     */
    public void sendLocalBroadcast(String action, Bundle args) {
        if (!TextUtils.isEmpty(action) && mGlobalViewModel != null) {
            mGlobalViewModel.updateData(action, args);
        }
    }

//    // 获得当前进程的名称
//    protected String getProcessName() {
//        String processName = "";
//        int pid = android.os.Process.myPid();
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
//            if (processInfo.pid == pid) {
//                processName = processInfo.processName;
//                break;
//            }
//        }
//        return processName;
//    }

    // 获取LastShowingActivity
    public static BaseActivity getLastShowingActivity() {
        if (sActivities.size() > 0) return sActivities.get(sActivities.size() - 1);
        else return null;
    }

    // 显示一个Toast
    protected void toast(String text, final Runnable dismissRunnable) {
        updatePermissionActivity();
        if (sPermissionActivity != null && sPermissionActivity.get() != null) {
            sPermissionActivity.get().toast(text, dismissRunnable);
        } else {
            if (dismissRunnable != null) {
                dismissRunnable.run();
            }
        }
    }

    // 更新sPermissionActivity
    private static void updatePermissionActivity() {
        if (sActivities.size() > 0) {
            sPermissionActivity = new SoftReference<>(sActivities.get(sActivities.size() - 1));
        }
    }

    // 权限更新后
    final void requestPermissionsResult(@NonNull String[] permissions,
                                        @NonNull int[] grantResults) {
        // 通知子类
        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            onRequestPermissionsResult(permissions[i],
                    grantResults[i] == PackageManager.PERMISSION_GRANTED);
        }
        // 通知其下所有BaseActivity
        for (int i = sActivities.size() - 1; i >= 0; i--) {
            BaseActivity activity = sActivities.get(i);
            if (activity != null) {
                activity.requestPermissionsResult(permissions, grantResults);
            }
        }
    }

    // Log write to path
    protected String onGenerateLogWriteToPath() {
        return "";
    }

    // Log write to path
    final String generateLogWriteToPath() {
        return onGenerateLogWriteToPath();
    }

    // ContentMaskView
    final @LayoutRes
    int generateContentMaskView() {
        final int id = onGenerateContentMaskView();
        try {
            LayoutInflater.from(this).inflate(id, null, false);
            return id;
        } catch (Exception e) {
            return R.layout.view_application_default_content_mask;
        }
    }

    // Local BroadcastReceiver
    private static class GlobalLiveDataObserver implements Observer<GlobalLiveData> {
        private SoftReference<BaseApplication> mApp;

        GlobalLiveDataObserver(BaseApplication app) {
            mApp = new SoftReference<>(app);
        }

        @Override
        public void onChanged(GlobalLiveData data) {
            if (mApp.get() != null && data != null) {
                mApp.get().receiveLocalBroadcastReceiver(
                  data.getAction(), data.getArgs());
            }
        }
    }

    // Network Status
    private static class NetworkStatusReceiver extends BroadcastReceiver {
        private SoftReference<BaseApplication> mApp;

        /**
         * 实例化广播接收器,并将其注册到BaseApplication上
         * @param app app
         */
        public NetworkStatusReceiver(BaseApplication app) {
            mApp = new SoftReference<>(app);
            // 注册该广播接收器
            register();
        }

        /**
         * 解注册该广播接收器
         */
        public void unregister() {
            BaseApplication app = mApp.get();
            if (app != null) {
                app.unregisterReceiver(this);
            }
        }

        private void register() {
            BaseApplication app = mApp.get();
            if (app != null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                app.registerReceiver(this, filter);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm =(ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                BaseApplication app = mApp.get();
                if (app != null) {
                    app.onNetworkConnectedCore(info.getType());
                }
            } else {
                BaseApplication app = mApp.get();
                if (app != null) {
                    app.onNetworkDisconnectedCore();
                }
            }
        }
    }

    // HeadsetPlug
    private static class HeadsetPlugReceiver extends BroadcastReceiver {
        private SoftReference<BaseApplication> mApp;

        /**
         * 实例化广播接收器,并将其注册到BaseApplication上
         * @param app app
         */
        public HeadsetPlugReceiver(BaseApplication app) {
            mApp = new SoftReference<>(app);
            // 注册该广播接收器
            register();
        }

        /**
         * 解注册该广播接收器
         */
        public void unregister() {
            BaseApplication app = mApp.get();
            if (app != null) {
                app.unregisterReceiver(this);
            }
        }

        private void register() {
            BaseApplication app = mApp.get();
            if (app != null) {
                IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
                app.registerReceiver(this, filter);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d("HeadsetPlugReceiver", intent.getAction());
            BaseApplication app = mApp.get();
            if (app != null) {
                AudioRecorderManager.getInstance().checkHeadsetPlugState(app);
                Bundle args = new Bundle();
                args.putInt("HeadsetPlugType",
                  AudioRecorderManager.getInstance().getHeadsetPlugType());
                app.sendLocalBroadcast("HeadsetPlugChanged", args);
            }
        }
    }

    // 处理本地广播接收
    final void receiveLocalBroadcastReceiver(String action, Bundle args) {
        for (int i = sActivities.size() - 1; i >= 0; i--) {
            BaseActivity activity = sActivities.get(i);
            if (activity != null) {
                activity.receiveLocalBroadcastReceiver(action, args);
            }
        }
    }

    // 处理网络连接,广播分发
    private void onNetworkConnectedCore(int type) {
        onNetworkConnected(type);
        for (int i = sActivities.size() - 1; i >= 0; i--) {
            BaseActivity activity = sActivities.get(i);
            if (activity != null) {
                activity.onNetworkConnectedCore(type);
            }
        }
    }

    // 网络链接接通
    protected void onNetworkConnected(int type) {}

    // 处理网络断开,广播分发
    private void onNetworkDisconnectedCore() {
        onNetworkDisconnected();
        for (int i = sActivities.size() - 1; i >= 0; i--) {
            BaseActivity activity = sActivities.get(i);
            if (activity != null) {
                activity.onNetworkDisconnectedCore();
            }
        }
    }

    // 网络链接断开
    protected void onNetworkDisconnected() {}

//    // 监听手机来电
//    private void checkListenPhoneState() {
//        if (Permission.hasReadPhoneStatePermission()) {
//            TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
//            if (tm != null) {
//                tm.listen(new AppPhoneListener(), PhoneStateListener.LISTEN_CALL_STATE);
//            }
//        }
//    }

//    // 监听来电状态
//    private final static class AppPhoneListener extends PhoneStateListener {
//        @Override
//        public void onCallStateChanged(int state, String incomingNumber) {
//            // 来电回调
//            if (state == TelephonyManager.CALL_STATE_RINGING) {
//                for (int i = sActivities.size() - 1; i >= 0; i--) {
//                    BaseActivity activity = sActivities.get(i);
//                    if (activity != null) {
//                        activity.onCallRingingCore();
//                    }
//                }
//            }
//        }
//    }
}
