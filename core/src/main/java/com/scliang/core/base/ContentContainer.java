package com.scliang.core.base;

import android.content.Context;
import android.content.res.Resources;
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
public final class ContentContainer extends FrameLayout {
    public ContentContainer(@NonNull Context context) {
        super(context);
        init();
    }

    public ContentContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContentContainer(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    public void paddingStatusBar() {
        setPadding(
            getPaddingLeft(),
            getPaddingTop() + getStatusBarHeight(),
            getPaddingRight(),
            getPaddingBottom());
    }

    private int getStatusBarHeight() {
        int result = 0;
        Resources resources = getResources();
        int resourceId = resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        );

        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }

        return result;
    }
}
