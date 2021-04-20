package com.scliang.core.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/19.
 */
public abstract class BaseViewGroup extends ViewGroup {
    public BaseViewGroup(Context context) {
        super(context);
        init();
        initAttrs(null);
    }

    public BaseViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        initAttrs(attrs);
    }

    public BaseViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        initAttrs(attrs);
    }

    private void init() {
        setWillNotDraw(false);
        onInit();
    }

    private void initAttrs(AttributeSet attrs) {
        onInitAttrs(attrs);
    }

    protected void onInit() {
    }

    protected void onInitAttrs(AttributeSet attrs) {
    }

    /**
     * 根据手机的分辨率dp单位转为px像素
     * @param dp dp
     * @return px
     */
    public int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // nothing
    }
}
