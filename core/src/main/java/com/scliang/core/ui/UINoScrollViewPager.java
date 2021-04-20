package com.scliang.core.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/1/11.
 */
public class UINoScrollViewPager extends ViewPager {
    private boolean noScroll = true;

    public UINoScrollViewPager(Context context) {
        super(context);
    }

    public UINoScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setNoScroll(boolean noScroll) {
        this.noScroll = noScroll;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        /* return false;//super.onTouchEvent(event); */
        return !noScroll && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return !noScroll && super.onInterceptTouchEvent(event);
    }
}
