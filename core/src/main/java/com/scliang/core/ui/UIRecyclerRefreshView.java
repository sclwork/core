package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/26.
 */
public class UIRecyclerRefreshView extends BaseRecyclerDragView {
    protected boolean isRefresh;
    protected View mOnlyChild;
    protected Rect mOnlyChildRect = new Rect();
    protected ImageView mImage;
    protected View mProgress;
    protected TextView mText;
    protected boolean isImageUp = false;
    protected boolean isImageDown = false;

    public UIRecyclerRefreshView(Context context) {
        super(context);
    }

    public UIRecyclerRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIRecyclerRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
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
            mImage = mOnlyChild.findViewWithTag("image");
            if (mImage != null) {
                mImage.setVisibility(VISIBLE);
            }
            mProgress = mOnlyChild.findViewWithTag("progress");
            if (mProgress != null) {
                mProgress.setVisibility(GONE);
            }
            mText = mOnlyChild.findViewWithTag("text");
            if (mText != null) {
                mText.setText("下拉刷新");
                mText.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    protected void onUpdateDragOffset(int offset, int length) {
//        float s = offset / (float)length;
//        int color = 0x00ff4444 | (Math.min((int)(0xff * s), 0xff) << 24);
//        setBackgroundColor(color);
        if (!isRefresh) {
            if (mImage != null) {
                mImage.setVisibility(VISIBLE);
                if (offset < length) {
                    if (!isImageDown) {
                        isImageDown = true;
                        if (isImageUp) {
                            Animation rotateAnimation = new RotateAnimation(
                                    180, 0,
                                    Animation.RELATIVE_TO_SELF, 0.5f,
                                    Animation.RELATIVE_TO_SELF, 0.5f);
                            rotateAnimation.setFillAfter(true);
                            rotateAnimation.setDuration(150);
                            rotateAnimation.setInterpolator(new LinearInterpolator());
                            mImage.startAnimation(rotateAnimation);
                        }
                    }
                    isImageUp = false;
                } else {
                    if (!isImageUp) {
                        isImageUp = true;
                        if (isImageDown) {
                            Animation rotateAnimation = new RotateAnimation(
                                    0, 180,
                                    Animation.RELATIVE_TO_SELF, 0.5f,
                                    Animation.RELATIVE_TO_SELF, 0.5f);
                            rotateAnimation.setFillAfter(true);
                            rotateAnimation.setDuration(150);
                            rotateAnimation.setInterpolator(new LinearInterpolator());
                            mImage.startAnimation(rotateAnimation);
                        }
                    }
                    isImageDown = false;
                }
            }
            if (mProgress != null) {
                mProgress.setVisibility(GONE);
            }
            if (mText != null) {
                if (offset < length) {
                    mText.setText("下拉刷新");
                } else {
                    mText.setText("松开立即刷新");
                }
            }
        } else {
            if (mImage != null) {
                mImage.setVisibility(GONE);
            }
            if (mProgress != null) {
                mProgress.setVisibility(VISIBLE);
            }
            if (mText != null) {
                mText.setText("正在刷新...");
            }
        }
    }

    @Override
    protected void onStartAction() {
        isRefresh = true;
        if (mImage != null) {
            mImage.setVisibility(GONE);
        }
        if (mProgress != null) {
            mProgress.setVisibility(VISIBLE);
        }
        if (mText != null) {
            mText.setText("正在刷新...");
        }
    }

    @Override
    protected void onCompleteAction() {
        isRefresh = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
//        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = 300; // MeasureSpec.getSize(heightMeasureSpec);

        if (mOnlyChild != null) {
            mOnlyChild.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
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
