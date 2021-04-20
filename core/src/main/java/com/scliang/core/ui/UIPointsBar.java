package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.scliang.core.R;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/9/12.
 */
public class UIPointsBar extends BaseViewGroup {
    private int mWidth;
    private int mHeight;
    private float mPointRadius;
    private int mPointWidth;
    private float mPointDrawY;
    private Paint mPointNSPaint;
    private Paint mPointSDPaint;
    private int mCurrent;
    private int mCount;

    public UIPointsBar(Context context) {
        super(context);
    }

    public UIPointsBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIPointsBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        int pointColor = getResources().getColor(R.color.colorAccent);
        // init
        mPointRadius = dp2px(2.5f);
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
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = mCount > 1 ? ((int) (mPointRadius * 2 * mCount + mPointRadius * 1.5f * (mCount - 1))) : 0;
        mHeight = (int) (mPointRadius * 2);
        mPointDrawY = mHeight / 2;

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int save = canvas.save();
        float psw = mPointRadius * 2 * mCount + mPointRadius * 1.5f * (mCount - 1);
        float fpx = (mWidth - psw) / 2 + mPointRadius;
        for (int p = 0; p < mCount; p++) {
            float px = fpx + p * (mPointRadius * 3.5f);
            canvas.drawCircle(px, mPointDrawY,
                    p == mCurrent ? mPointRadius : mPointRadius - mPointWidth / 2,
                    p == mCurrent ? mPointSDPaint : mPointNSPaint);
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
