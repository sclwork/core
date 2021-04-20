package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2019/4/26.
 */
public class UIMaximumWidthView extends FrameLayout {
    private int mMaxWidth;
    private View mOnlyChild;
    private Rect mOnlyChildRect = new Rect();

    public UIMaximumWidthView(Context context) {
        super(context);
    }

    public UIMaximumWidthView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIMaximumWidthView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置视图的最大宽带
     * @param maxWidth 最大高度,大于0,否则无效
     */
    public void setMaxWidth(int maxWidth) {
        mMaxWidth = maxWidth;
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
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mOnlyChild != null) {
            if (height <= 0) {
                height = 5000;
            }
            if (mMaxWidth > 0) {
                mOnlyChild.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height * 10, MeasureSpec.AT_MOST));
                int childWidth = mOnlyChild.getMeasuredWidth();
                if (childWidth > mMaxWidth) {
                    mOnlyChild.measure(MeasureSpec.makeMeasureSpec(mMaxWidth, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(height * 10, MeasureSpec.AT_MOST));
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
