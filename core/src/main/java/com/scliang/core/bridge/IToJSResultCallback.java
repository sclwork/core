package com.scliang.core.bridge;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/6/14.
 */
public interface IToJSResultCallback {

    /**
     * 调用toJS方法后等待JS返回值的回调
     */
    void onJSResultCallback(String result);
}
