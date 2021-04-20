package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.scliang.core.R;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/9/12.
 */
public class UIPointsBar2 extends BaseViewGroup {
    private int mWidth;
    private int mHeight;
    private float mPointRadius;
    private int mPointWidth;
    private float mPointDrawX;
    private float mPointDrawY;
    private Paint mPointNSPaint;
    private Paint mPointSDPaint;
    private int mCurrent;
    private int mCount;
    private int mNormalWidth;
    private int mSelectedWidth;
    private int mSpace;
    private Path mPath;
    private RectF mRect;

    public UIPointsBar2(Context context) {
        super(context);
    }

    public UIPointsBar2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIPointsBar2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        int pointColor = getResources().getColor(R.color.colorAccent);
        // init
        mNormalWidth = dp2px(8.0f);
        mSelectedWidth = dp2px(20.0f);
        mPointRadius = dp2px(2.0f);
        mSpace = (int) (mPointRadius * 2);
        mPointWidth = dp2px(1);
        mPointNSPaint = new Paint();
        mPointNSPaint.setAntiAlias(true);
        mPointNSPaint.setStyle(Paint.Style.FILL);
        mPointNSPaint.setStrokeWidth(mPointWidth);
        mPointNSPaint.setColor(pointColor);
        mPointSDPaint = new Paint();
        mPointSDPaint.setAntiAlias(true);
        mPointSDPaint.setStyle(Paint.Style.FILL);
        mPointSDPaint.setColor(pointColor);
        mPath = new Path();
        mRect = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = mCount > 1 ? (mSelectedWidth + mNormalWidth * (mCount - 1) + mSpace * (mCount - 1)) : 0;
        mHeight = (int) (mPointRadius * 2);
        mPointDrawY = mHeight / 2;

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int save = canvas.save();
        mPointDrawX = 0;
        for (int p = 0; p < mCount; p++) {
            mPath.reset();
            if (p == mCurrent) {
                mPath.addCircle(mPointDrawX + mPointRadius, mPointDrawY, mPointRadius,
                    Path.Direction.CW);
                mRect.set(mPointDrawX + mPointRadius, 0,
                    mPointDrawX + mSelectedWidth - mPointRadius, mHeight);
                mPath.addRect(mRect, Path.Direction.CW);
                mPath.addCircle(mPointDrawX + mSelectedWidth - mPointRadius, mPointDrawY, mPointRadius,
                    Path.Direction.CW);
                mPointDrawX = mPointDrawX + mSelectedWidth + mSpace;
            } else {
                mPath.addCircle(mPointDrawX + mPointRadius, mPointDrawY, mPointRadius,
                    Path.Direction.CW);
                mRect.set(mPointDrawX + mPointRadius, 0,
                    mPointDrawX + mNormalWidth - mPointRadius, mHeight);
                mPath.addRect(mRect, Path.Direction.CW);
                mPath.addCircle(mPointDrawX + mNormalWidth - mPointRadius, mPointDrawY, mPointRadius,
                    Path.Direction.CW);
                mPointDrawX = mPointDrawX + mNormalWidth + mSpace;
            }
            mPath.close();
            canvas.drawPath(mPath, p == mCurrent ? mPointSDPaint : mPointNSPaint);
        }
        canvas.restoreToCount(save);
    }

    public void setCount(int count) {
        mCount = count;
        requestLayout();
    }

    public void setCurrent(int current) {
        if (current < 0) {
            mCurrent = 0;
        } else if (current >= mCount) {
            mCurrent = mCount - 1;
        } else {
            mCurrent = current;
        }

        if (mCurrent >= mCount) {
            mCurrent = mCount - 1;
        }
        if (mCurrent < 0) {
            mCurrent = 0;
        }

        postInvalidate();
    }

    public void setPointColor(int normalColor, int selectColor) {
        mPointNSPaint.setColor(normalColor);
        mPointSDPaint.setColor(selectColor);
        postInvalidate();
    }
}
