package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/30.
 */
public class UIRingDistributeView extends BaseViewGroup {
    private static final int COUNT = 7;

    private static final int COLOR_A = 0xff78a9f9;
    private static final int COLOR_B = 0xff43adc8;
    private static final int COLOR_C = 0xff50c78e;
    private static final int COLOR_D = 0xffee837d;
    private static final int COLOR_E = 0xff6b76d0;
    private static final int COLOR_F = 0xfff29b76;
    private static final int COLOR_G = 0xffcc9933;
    private static final int COLOR_H = 0xffffffff;
    private static final int COLOR_I = 0xffaaaaaa;

    private static final float ANGLE_OFFSET = -90;
    private static final float GAP_ANGLE = 1.5f;
    private static final float ZERO_ANGLE = 0.0f;

    private float[] mValues;
    private DrawArc[] mDrawArcs;
    private Paint mPaintA;
    private Paint mPaintB;
    private Paint mPaintC;
    private Paint mPaintD;
    private Paint mPaintE;
    private Paint mPaintF;
    private Paint mPaintG;
    private Paint mPaintH;
    private Paint mPaintI;

    private int mWidth = 0;
    private int mHeight = 0;
    private int mOutsideRadius = 0;
    private int mInsideRadius = 0;
    private int mRingWidth = 0;
    private RectF mRingRect = new RectF();
    private float mSum = 0;

    public UIRingDistributeView(Context context) {
        super(context);
    }

    public UIRingDistributeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIRingDistributeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        mPaintA = new Paint();
        mPaintA.setAntiAlias(true);
        mPaintA.setStyle(Paint.Style.FILL);
        mPaintA.setColor(COLOR_A);

        mPaintB = new Paint();
        mPaintB.setAntiAlias(true);
        mPaintB.setStyle(Paint.Style.FILL);
        mPaintB.setColor(COLOR_B);

        mPaintC = new Paint();
        mPaintC.setAntiAlias(true);
        mPaintC.setStyle(Paint.Style.FILL);
        mPaintC.setColor(COLOR_C);

        mPaintD = new Paint();
        mPaintD.setAntiAlias(true);
        mPaintD.setStyle(Paint.Style.FILL);
        mPaintD.setColor(COLOR_D);

        mPaintE = new Paint();
        mPaintE.setAntiAlias(true);
        mPaintE.setStyle(Paint.Style.FILL);
        mPaintE.setColor(COLOR_E);

        mPaintF = new Paint();
        mPaintF.setAntiAlias(true);
        mPaintF.setStyle(Paint.Style.FILL);
        mPaintF.setColor(COLOR_F);

        mPaintG = new Paint();
        mPaintG.setAntiAlias(true);
        mPaintG.setStyle(Paint.Style.FILL);
        mPaintG.setColor(COLOR_G);

        mPaintH = new Paint();
        mPaintH.setAntiAlias(true);
        mPaintH.setStyle(Paint.Style.FILL);
        mPaintH.setColor(COLOR_H);

        mPaintI = new Paint();
        mPaintI.setAntiAlias(true);
        mPaintI.setStyle(Paint.Style.FILL);
        mPaintI.setColor(COLOR_I);

        mValues = new float[] {0, 0, 0, 0, 0, 0, 0};

        initDrawArcs();

//        updateValues(70, 10);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        if (isInEditMode()) {
            mRingWidth = 60;
        } else {
            mRingWidth = dp2px(30);
        }
        mOutsideRadius = (int) (Math.min(mWidth, mHeight) / 2 * 0.75f);
        mInsideRadius = mOutsideRadius - mRingWidth;
        float cx = mWidth / 2.0f;
        float cy = mHeight / 2.0f;
        mRingRect.set(cx - mOutsideRadius, cy - mOutsideRadius,
                cx + mOutsideRadius, cy + mOutsideRadius);

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw clean
        canvas.drawColor(0x0, PorterDuff.Mode.OVERLAY);
        // Draw Ring
        if (mSum <= 0) {
            canvas.drawArc(mRingRect, 0, 360, true, mPaintI);
        } else {
            for (DrawArc arc : mDrawArcs) {
                canvas.drawArc(mRingRect, arc.startAngle + ANGLE_OFFSET, arc.sweepAngle, true, arc.paint);
            }
        }
        // Draw Center Mask
        canvas.drawCircle(mRingRect.centerX(), mRingRect.centerY(), mInsideRadius, mPaintH);
    }

    public void updateValues(float... values) {
        if (values != null) {
            mSum = 0;
            for (float v : values) { mSum += v; }
            System.arraycopy(values, 0, mValues, 0,
                    Math.min(values.length, mValues.length));
            updateDrawArcsWithValues();
            postInvalidate();
        }
    }

    private void initDrawArcs() {
        float initAngle = (360f - GAP_ANGLE * COUNT) / COUNT;
        mDrawArcs = new DrawArc[] {
                new DrawArc(mPaintA, 0,                             initAngle), new DrawArc(mPaintH, initAngle,                          GAP_ANGLE),
                new DrawArc(mPaintB, initAngle + GAP_ANGLE,         initAngle), new DrawArc(mPaintH, initAngle * 2 + GAP_ANGLE,     GAP_ANGLE),
                new DrawArc(mPaintC, initAngle * 2 + GAP_ANGLE * 2, initAngle), new DrawArc(mPaintH, initAngle * 3 + GAP_ANGLE * 2, GAP_ANGLE),
                new DrawArc(mPaintD, initAngle * 3 + GAP_ANGLE * 3, initAngle), new DrawArc(mPaintH, initAngle * 4 + GAP_ANGLE * 3, GAP_ANGLE),
                new DrawArc(mPaintE, initAngle * 4 + GAP_ANGLE * 4, initAngle), new DrawArc(mPaintH, initAngle * 5 + GAP_ANGLE * 4, GAP_ANGLE),
                new DrawArc(mPaintF, initAngle * 5 + GAP_ANGLE * 5, initAngle), new DrawArc(mPaintH, initAngle * 6 + GAP_ANGLE * 5, GAP_ANGLE),
                new DrawArc(mPaintG, initAngle * 6 + GAP_ANGLE * 6, initAngle), new DrawArc(mPaintH, initAngle * 7 + GAP_ANGLE * 6, GAP_ANGLE),
        };
    }

    private void updateDrawArcsWithValues() {
        float value;
        float valueSum = 0;
        int zeroCount = 0;
        for (float v : mValues) {
            value = Math.max(v, 0);
            if (value == 0) {
                zeroCount++;
            }
            valueSum += value;
        }
        if (valueSum <= 0) {
            initDrawArcs();
        } else {
            boolean only = COUNT - zeroCount == 1;
            float useAngle = only ? 360 : (360 - GAP_ANGLE * (ZERO_ANGLE > 0 ? COUNT : (COUNT - zeroCount)) - ZERO_ANGLE * zeroCount);
            float oneAngle = useAngle / valueSum;
            float startAngle = 0;
            float sweepAngle = 0;
            for (int i = 0; i < mValues.length; i++) {
                DrawArc arc = mDrawArcs[i * 2];
                DrawArc arcGap = mDrawArcs[i * 2 + 1];

                value = Math.max(mValues[i], 0);
                sweepAngle = value == 0 ? ZERO_ANGLE : (oneAngle * value);

                arc.startAngle = startAngle;
                arc.sweepAngle = sweepAngle;
                arcGap.startAngle = startAngle + sweepAngle;
                arcGap.sweepAngle = sweepAngle <= 0 || only ? 0 : GAP_ANGLE;

                startAngle = arcGap.startAngle + arcGap.sweepAngle;
            }
        }
    }




    //////////////////////////////////////////////
    //////////////////////////////////////////////
    private static class DrawArc {
        float startAngle = 0;
        float sweepAngle = 0;
        Paint paint;

        DrawArc(Paint p, float start, float sweep) {
            startAngle = start;
            sweepAngle = sweep;
            paint = p;
        }
    }
}
