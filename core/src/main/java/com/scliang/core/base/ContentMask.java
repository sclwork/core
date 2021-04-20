package com.scliang.core.base;

import android.content.Context;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/9/28.
 */
public final class ContentMask extends FrameLayout {
    public ContentMask(@NonNull Context context) {
        super(context);
        init();
    }

    public ContentMask(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContentMask(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 拦截点击事件
        super.setClickable(true);
    }

    @Override
    public void setClickable(boolean clickable) {
//        super.setClickable(clickable);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }
}
