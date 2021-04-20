package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/5.
 */
public class UIRecorderRulerView extends BaseViewGroup {
    private final int sMaxTimeInSecond = 3;
    private final int sScaleCount = sMaxTimeInSecond * 150;
    private static int sMinScaleWidth;
    private static int sMinY;
    private static int sMaxY;
    private int mWidth;
    private int mHeight;
    private int mScaleWidth;
    private int mOffset;
    private Paint mNumPaint;
    private Paint mScalePaint;

    public UIRecorderRulerView(Context context) {
        super(context);
    }

    public UIRecorderRulerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIRecorderRulerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        sMinScaleWidth = dp2px(18.5f);
        sMinY = dp2px(10);
        sMaxY = dp2px(15);

        mNumPaint = new Paint();
        mNumPaint.setAntiAlias(true);
        mNumPaint.setColor(0xff666666);
        mNumPaint.setTextSize(dp2px(12));
        mNumPaint.setTextAlign(Paint.Align.CENTER);

        mScalePaint = new Paint();
        mScalePaint.setAntiAlias(true);
        mScalePaint.setColor(0xff999999);
        mScalePaint.setStrokeWidth(dp2px(0.5f));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        mScaleWidth = sMinScaleWidth * sScaleCount;
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int x;
        int sy = mHeight / 2;
        int ey;
        int ny = sy - dp2px(5);
        int numCount = 0;
        for (int i = 0; i <= sScaleCount; i++) {
            // draw scale
            x = mOffset + mWidth / 2 + sMinScaleWidth * i;
            ey = sy + (i % 5 == 0 ? sMaxY : sMinY);
            canvas.drawLine(x, sy, x, ey, mScalePaint);
            // draw num
            if (i % 5 == 0) {
                int s = numCount;
                int m = s / 60;
                s = s % 60;
                String str = (m < 10 ? ("0" + m) : m) + ":" + (s < 10 ? ("0" + s) : s);
                canvas.drawText(str, x, ny, mNumPaint);
                numCount += 2;
            }
        }
    }

    public void updateTime(long time) {
        float s = time / (sMaxTimeInSecond * 6000f);
        mOffset = -(int) (s * mScaleWidth);
        invalidate();
    }
}
