package com.scliang.core.bridge;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.scliang.core.base.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * SCore Bridge
 * Created by ShangChuanliang
 * on 17/9/30.
 */
public final class BasicBridge implements IBridge {
    private WebView mWebView;
    private com.tencent.smtt.sdk.WebView mX5WebView;
    private Handler mHandler;
    private Map<String, IBridgeCallback> mBridgeCallbacks;
    private boolean mUseJsPrompt;

    BasicBridge(Handler handler, boolean useJsPrompt) {
        mHandler = handler;
        mBridgeCallbacks = new HashMap<>();
        mUseJsPrompt = useJsPrompt;
    }

    @Override
    public void setTarget(WebView webView) {
        mWebView = webView;
        mX5WebView = null;
    }

    @Override
    public void setTarget(com.tencent.smtt.sdk.WebView webView) {
        mWebView = null;
        mX5WebView = webView;
    }

    @Override
    public void registerHandler(String handlerName, IBridgeCallback callback) {
        synchronized (BasicBridge.class) {
            mBridgeCallbacks.put(handlerName, callback);
        }
    }

    @Override
    public void unregisterHandler(String handlerName) {
        synchronized (BasicBridge.class) {
            mBridgeCallbacks.remove(handlerName);
        }
    }

    @JavascriptInterface
    public String fromJS(String data) {
        if (mUseJsPrompt) {
            return "";
        }

        operateFromJSData(data);
        // handlerFlag - 暂时无意义
        return UUID.randomUUID().toString();
    }

    @Override
    public void toJS(String name, String data) {
        toJS(name, data, null);
    }

    @Override
    public void toJS(String name, String data, final IToJSResultCallback callback) {
        if (mWebView != null) {
            JSONObject object = new JSONObject();
            try {
                object.put(HANDLE_DATA_NAME, name);
                try {
                    JSONObject dataObj = new JSONObject(data);
                    object.put(HANDLE_DATA_VALUE, dataObj);
                } catch (JSONException e) {
                    object.put(HANDLE_DATA_VALUE, data);
                }
            } catch (JSONException ignored) {}

            final String script = String.format(Locale.CHINESE,
                    "javascript:toJS('%s')", object.toString());

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                mWebView.loadUrl(script);
            } else {
                mWebView.evaluateJavascript(script, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Logger.d("BasicBridge", "evaluateJavascript result: " + value);
                        if (callback != null) callback.onJSResultCallback(value);
                    }
                });
            }
        }
        if (mX5WebView != null) {
            JSONObject object = new JSONObject();
            try {
                object.put(HANDLE_DATA_NAME, name);
                try {
                    JSONObject dataObj = new JSONObject(data);
                    object.put(HANDLE_DATA_VALUE, dataObj);
                } catch (JSONException e) {
                    object.put(HANDLE_DATA_VALUE, data);
                }
            } catch (JSONException ignored) {}

            final String script = String.format(Locale.CHINESE,
                    "javascript:toJS('%s')", object.toString());

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                mX5WebView.loadUrl(script);
            } else {
                mX5WebView.evaluateJavascript(script, new com.tencent.smtt.sdk.ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Logger.d("BasicBridge", "evaluateJavascript result: " + value);
                        if (callback != null) callback.onJSResultCallback(value);
                    }
                });
            }
        }
    }

    @Override
    public void operateJSData(String data) {
        operateFromJSData(data);
    }

    // 处理JS传过来的数据
    private void operateFromJSData(String data) {
        // debug
        Logger.d("BasicBridge", "OperateFromJSData: " + data);
        // 操作放到Handler中进行
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(data);
        } catch (JSONException ignored) {
            jsonObject = new JSONObject();
        }
        String name = jsonObject.optString(HANDLE_DATA_NAME);
        if (!TextUtils.isEmpty(name)) {
            synchronized (BasicBridge.class) {
                IBridgeCallback callback = mBridgeCallbacks.get(name);
                if (callback != null) {
                    // 已经注册过的HandlerName
                    HandlerObject object = new HandlerObject();
                    object.name = name;
                    object.data = jsonObject.optString(HANDLE_DATA_VALUE);
                    object.callback = callback;
                    sendNamedHandler(object);
                } else {
                    // 没有注册此HandlerName时发送通用的Handler
                    sendUnHandler(data);
                }
            }
        }
        // 非JSON格式或没有HandlerName时发送通用的Handler
        else {
            sendUnHandler(data);
        }
    }

    // 发送通用的Handler
    private void sendUnHandler(String data) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(HANDLE_WHAT_FROM_JS);
            msg.obj = data;
            mHandler.sendMessage(msg);
        }
    }

    // 发送HandlerName的Handler
    private void sendNamedHandler(HandlerObject data) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(HANDLE_WHAT_HANDLE_JS);
            msg.obj = data;
            mHandler.sendMessage(msg);
        }
    }
}
