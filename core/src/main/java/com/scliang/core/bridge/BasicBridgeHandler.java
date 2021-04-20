package com.scliang.core.bridge;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.scliang.core.base.Logger;

/**
 * SCore Bridge
 * Created by ShangChuanliang
 * on 17/9/30.
 */
public final class BasicBridgeHandler extends Handler {

    BasicBridgeHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        // 通用的Handler
        if (msg.what == IBridge.HANDLE_WHAT_FROM_JS) {
            Object d = msg.obj;
            if (d == null) {
                Logger.d("BasicBridgeHandler", "[Data] is null !");
            }
            if (d != null && d instanceof String) {
                String data = (String) d;
                Logger.d("BasicBridgeHandler", "[Data] " + data);
            }
        }

        // 有HandlerName的Handler
        if (msg.what == IBridge.HANDLE_WHAT_HANDLE_JS) {
            Object d = msg.obj;
            if (d == null) {
                Logger.d("BasicBridgeHandler", "[Data] is null !");
            }
            if (d != null && d instanceof HandlerObject) {
                HandlerObject data = (HandlerObject) d;
                Logger.d("BasicBridgeHandler", "[Data] " + data);
                if (data.callback != null) {
                    data.callback.onCallback(data.name, data.data);
                }
            }
        }
    }
}
