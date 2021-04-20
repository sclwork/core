package com.scliang.core.demo;

import android.os.Environment;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.scliang.core.base.BaseApplication;
import com.scliang.core.base.OnCheckPermissionImpl;
import com.scliang.core.ble.BLE;
import com.scliang.core.base.Data;
import com.scliang.core.base.Permission;
import com.scliang.core.media.voice.Voice;
import com.scliang.core.media.voice.VoiceConfig;

import java.io.File;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/3/20.
 */
public class DemoApplication extends BaseApplication {

    @Override
    public int giveStatusBarColor() {
        return 0xffffffff;
    }

    @Override
    protected String onGenerateLogWriteToPath() {
        String path = "";
        if (Permission.hasStoragePermission(new OnCheckPermissionImpl())) {
            path = Environment.getExternalStorageDirectory().toString() + "/SCore";
            File root = new File(path);
            if (!root.exists()) {
                root.mkdirs();
            }
        }
        return path;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
        Data.getInstance().init(this, "http://scliang.com", "", 5000, true);
//        Voice.getInstance().init(this, VoiceConfig.Baidu(), true);
        BLE.getInstance().init(this);
    }
}

