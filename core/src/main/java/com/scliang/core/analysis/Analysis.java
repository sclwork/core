package com.scliang.core.analysis;

import android.app.Application;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

import java.util.HashMap;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/12.
 */
public final class Analysis {
    private Analysis() {
    }

    private static class SingletonHolder {
        private static final Analysis INSTANCE = new Analysis();
    }

    public static Analysis getInstance() {
        return SingletonHolder.INSTANCE;
    }




    /**
     * 初始化统计分析功能模块
     * @param application BaseApplication
     * @param debuggable 是否可调试
     */
    public void init(Application application, boolean debuggable) {
        final Context context = application.getApplicationContext();
        // 初始化UMeng
        UMConfigure.init(context, UMConfigure.DEVICE_TYPE_PHONE, "");
        MobclickAgent.setDebugMode(debuggable);
        MobclickAgent.openActivityDurationTrack(false);
        MobclickAgent.setScenarioType(context, MobclickAgent.EScenarioType.E_UM_NORMAL);
        // 初始化Bugly
        CrashReport.initCrashReport(context);
    }

    /**
     * onResume(UMeng统计)
     */
    public void onResume(Context context) {
        // UMeng统计
        MobclickAgent.onResume(context);
    }

    /**
     * onPause(UMeng统计)
     */
    public void onPause(Context context) {
        // UMeng统计
        MobclickAgent.onPause(context);
    }

    /**
     * onPageStart(UMeng统计)
     * @param pageName 页面名称
     */
    public void onPageStart(String pageName) {
        // UMeng统计
        MobclickAgent.onPageStart(pageName);
    }

    /**
     * onPageEnd(UMeng统计)
     * @param pageName 页面名称
     */
    public void onPageEnd(String pageName) {
        // UMeng统计
        MobclickAgent.onPageEnd(pageName);
    }

    /**
     * onEvent 自定义事件统计(UMeng统计)
     * @param context Context
     * @param eventId EventId
     */
    public void onEvent(Context context, String eventId) {
        // UMeng统计
        MobclickAgent.onEvent(context, eventId);
    }

    /**
     * onEvent 自定义事件统计(UMeng统计)
     * @param context Context
     * @param eventId EventId
     * @param params Event Params
     */
    public void onEvent(Context context, String eventId, HashMap<String, String> params) {
        // UMeng统计
        MobclickAgent.onEvent(context, eventId, params);
    }
}
