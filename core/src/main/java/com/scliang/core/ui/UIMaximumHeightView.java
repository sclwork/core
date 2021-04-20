package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/22.
 */
public class UIMaximumHeightView extends BaseViewGroup {
    private int mMaxHeight;
    private int mCurrentHeight;
    private View mOnlyChild;
    private Rect mOnlyChildRect = new Rect();

    public UIMaximumHeightView(Context context) {
        super(context);
    }

    public UIMaximumHeightView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIMaximumHeightView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
    }

    /**
     * 设置视图的最大高度
     * @param maxHeight 最大高度,大于0,否则无效
     */
    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
        requestLayout();
    }

    /**
     * 设置视图的高度
     * @param height 视图高度,不会高于MaxHeight
     */
    public void setCurrentHeight(int height) {
        mCurrentHeight = height;
        requestLayout();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            mOnlyChild = getChildAt(0);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mOnlyChild != null) {
            if (mMaxHeight <= 0) {
                mOnlyChild.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
            } else {
                if (mCurrentHeight > 0) {
                    mOnlyChild.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(mCurrentHeight, MeasureSpec.EXACTLY));
                } else {
                    mOnlyChild.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
                }
                int childHeight = mOnlyChild.getMeasuredHeight();
                if (childHeight > mMaxHeight) {
                    mOnlyChild.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.EXACTLY));
                }
            }
            mOnlyChildRect.set(0, 0, mOnlyChild.getMeasuredWidth(), mOnlyChild.getMeasuredHeight());
            width = mOnlyChildRect.width();
            height = mOnlyChildRect.height();
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mOnlyChild != null) {
            mOnlyChild.layout(mOnlyChildRect.left, mOnlyChildRect.top,
                    mOnlyChildRect.right, mOnlyChildRect.bottom);
        }
    }
}
