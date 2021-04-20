package com.scliang.core.bridge;

/**
 * SCore Bridge
 * Created by ShangChuanliang
 * on 17/9/30.
 */
public interface IBridgeCallback {

    /**
     * 对应HandlerName的操作回调 - 运行在UIMain线程中
     * @param handlerName HandlerName
     * @param data Data
     */
    void onCallback(String handlerName, String data);
}
