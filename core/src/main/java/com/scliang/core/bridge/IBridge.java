package com.scliang.core.bridge;

import android.webkit.WebView;

/**
 * SCore Bridge
 * Created by ShangChuanliang
 * on 17/9/30.
 */
public interface IBridge {
    /**
     * Handler What: H5调用native fromJS
     * <br/>
     * 数据[String data]存放在Message的obj中
     */
    int HANDLE_WHAT_FROM_JS = 211;

    /**
     * Handler What: H5调用native fromJS
     * <br/>
     * 数据[[handlerName,data] data]存放在Message的obj中
     */
    int HANDLE_WHAT_HANDLE_JS = 312;
    String HANDLE_DATA_NAME   = "name";
    String HANDLE_DATA_VALUE  = "data";

    /**
     * 设置目标WebView
     * @param webView JWebView
     */
    void setTarget(WebView webView);
    void setTarget(com.tencent.smtt.sdk.WebView webView);

    /**
     * 注册一个操作
     * @param name name
     * @param callback callback
     */
    void registerHandler(String name, IBridgeCallback callback);

    /**
     * 解注册一个操作
     * @param name name
     */
    void unregisterHandler(String name);

    /**
     * 将[String Data]发送给JS
     * @param name 操作句柄
     * @param data 待发送的数据
     */
    void toJS(String name, String data);

    /**
     * 将[String Data]发送给JS
     * @param name 操作句柄
     * @param data 待发送的数据
     * @param callback JS返回回调
     */
    void toJS(String name, String data, IToJSResultCallback callback);

    /**
     * 处理JS传过来的数据
     * @param data JS传过来的数据
     */
    void operateJSData(String data);
}
