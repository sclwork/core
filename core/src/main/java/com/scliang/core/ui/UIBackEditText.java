package com.scliang.core.ui;

import android.content.Context;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/7.
 */
public class UIBackEditText extends AppCompatEditText {
    private Runnable mCloseCallback;

    public UIBackEditText(Context context) {
        super(context);
    }

    public UIBackEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIBackEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCloseCallback(Runnable callback) {
        mCloseCallback = callback;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (mCloseCallback != null) {
            mCloseCallback.run();
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
