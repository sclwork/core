package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/26.
 */
public class UIRecyclerLoadMoreView extends BaseRecyclerDragView {
    protected boolean isLoadMore;
    protected View mOnlyChild;
    protected Rect mOnlyChildRect = new Rect();
    protected View mProgress;
    protected TextView mText;

    public UIRecyclerLoadMoreView(Context context) {
        super(context);
    }

    public UIRecyclerLoadMoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIRecyclerLoadMoreView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            mOnlyChild = getChildAt(0);
            if (mOnlyChild != null) {
                mProgress = mOnlyChild.findViewWithTag("progress");
                if (mProgress != null) {
                    mProgress.setVisibility(GONE);
                }
                mText = mOnlyChild.findViewWithTag("text");
                if (mText != null) {
                    mText.setText("上拉加载更多");
                    mText.setVisibility(VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onUpdateDragOffset(int offset, int length) {
//        float s = offset / (float)length;
//        int color = 0x00ff4444 | (Math.min((int)(0xff * s), 0xff) << 24);
//        setBackgroundColor(color);
        if (!isLoadMore) {
            if (mProgress != null) {
                mProgress.setVisibility(GONE);
            }
            if (mText != null) {
                if (offset < length) {
                    mText.setText("上拉加载更多");
                } else {
                    mText.setText("松开立即加载更多");
                }
            }
        } else {
            if (mProgress != null) {
                mProgress.setVisibility(VISIBLE);
            }
            if (mText != null) {
                mText.setText("正在加载数据中...");
            }
        }
    }

    @Override
    protected void onStartAction() {
        isLoadMore = true;
        if (mProgress != null) {
            mProgress.setVisibility(VISIBLE);
        }
        if (mText != null) {
            mText.setText("正在加载数据中...");
        }
    }

    @Override
    protected void onCompleteAction() {
        isLoadMore = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = 200; // MeasureSpec.getSize(heightMeasureSpec);

        if (mOnlyChild != null) {
            mOnlyChild.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            mOnlyChildRect.set(0, 0,
                    mOnlyChild.getMeasuredWidth(), mOnlyChild.getMeasuredHeight());
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
