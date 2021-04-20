package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/9.
 */
public class UIAudioPlayOSCView extends BaseViewGroup {
    // OSC background color
    private static final int COLOR_OSC_BACKGROUND = 0xfff7f7f7;
    // OSC color
    private static final int COLOR_OSC            = 0xff999999;
    // OSC middle line color
    private static final int COLOR_OSC_M_LINE     = 0xffd56b66;
    // dB color
    private static final int COLOR_DB             = 0xccff4444;
    // dB background color
    private static final int COLOR_DB_BACKGROUND  = 0x22ff4444;

    // 示波曲线起点位置
    private static final int OSC_START_MIDDLE = 0;
    private static final int OSC_START_RIGHT  = 1;
    private static int sOSCStart = OSC_START_RIGHT;

    // 示波曲线水平位置
    private static final int OSC_POS_MIDDLE = 0;
    private static final int OSC_POS_BOTTOM = 1;
    private static int sOSCPos = OSC_POS_MIDDLE;

    // 波形相关参数
    private static final float OSC_S = 32767f / 20.0f;
    private static final int OSC_BOTTOM_LINE_WIDTH   = 5; // px
    private static final int OSC_BOTTOM_LINE_PADDING = 1; // px
    private static final int OSC_BOTTOM_SUM_COUNT   = OSC_BOTTOM_LINE_WIDTH;
    private float mOSCBottomSum = 0;
    private int mOSCBottomSumIndex = 0;

    // 分贝长度
    private static final int DB_WIDTH  = 160; // px
    private static final int DB_HEIGHT = 20; // px
    private int mDBWidth = 0;

    private Rect mRect = new Rect();
    private OSCItem[] mOSCs;
    private int mDB;

    private Paint mPaintOSC;
    private Paint mPaintOSCMLine;
    private Paint mPaintDB;
    private Paint mScalePaint;

    private static float sMinScaleWidth;
    private static int sMinY;
    private static int sMaxY;

    public UIAudioPlayOSCView(Context context) {
        super(context);
    }

    public UIAudioPlayOSCView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIAudioPlayOSCView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        setBackgroundColor(0x0);

        // OSC Paint
        mPaintOSC = new Paint();
        mPaintOSC.setAntiAlias(true);
        mPaintOSC.setStrokeWidth(1);
        mPaintOSC.setStyle(Paint.Style.FILL);
        mPaintOSC.setColor(COLOR_OSC);
        // OSC Line Paint
        mPaintOSCMLine = new Paint();
        mPaintOSCMLine.setAntiAlias(true);
        mPaintOSCMLine.setStrokeWidth(dp2px(1));
        mPaintOSCMLine.setStyle(Paint.Style.FILL);
        mPaintOSCMLine.setColor(COLOR_OSC_M_LINE);
        // dB Paint
        mPaintDB = new Paint();
        mPaintDB.setAntiAlias(true);
        mPaintDB.setStyle(Paint.Style.FILL);
        mPaintDB.setColor(COLOR_DB);

        mScalePaint = new Paint();
        mScalePaint.setAntiAlias(true);
        mScalePaint.setColor(0xff999999);
        mScalePaint.setStrokeWidth(dp2px(0.5f));

        sMinScaleWidth = dp2px(18.5f);
        sMinY = dp2px(10);
        sMaxY = dp2px(15);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
//        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);

        int minHeight = dp2px(80);
        if (height < minHeight) {
            height = minHeight;
        }
        int maxHeight = dp2px(120);
        if (height > maxHeight) {
            height = maxHeight;
        }

        int count = (width / (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)) + 1;
        if (mOSCs == null || mOSCs.length != count) {
//            mOSCs = new OSCItem[width];
            mOSCs = new OSCItem[count];
            int w = width - getPaddingLeft() - getPaddingRight();
            int c = (int) (w / sMinScaleWidth);
            if (w % sMinScaleWidth != 0) {
                c++;
                sMinScaleWidth = (float) w / c;
            }
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            // draw background
            canvas.drawColor(COLOR_OSC_BACKGROUND);
//                // draw h middle line
//                if (sOSCPos == OSC_POS_MIDDLE) {
//                    canvas.drawLine(mRect.left, mRect.centerY(),
//                            mRect.right, mRect.centerY(),
//                            mPaintOSCMLine);
//                }
            // draw v middle line
            if (sOSCStart == OSC_START_MIDDLE) {
                canvas.drawLine(mRect.centerX(), mRect.top,
                        mRect.centerX(), mRect.bottom,
                        mPaintOSCMLine);
            }
            // draw osc
            if (sOSCPos == OSC_POS_MIDDLE) {
                for (OSCItem item : mOSCs) {
                    if (item != null) {
//                        canvas.drawLine(item.sx, item.sy, item.tx, item.ty, mPaintOSC);
                        canvas.drawRect(item.sx, item.sy, item.tx, item.ty, mPaintOSC);
                    }
                }
            } else {
                for (OSCItem item : mOSCs) {
                    if (item != null) {
                        canvas.drawRect(item.sx, item.sy, item.tx, item.ty, mPaintOSC);
                    }
                }
            }
//                // draw dB
//                mPaintDB.setColor(COLOR_DB_BACKGROUND);
//                canvas.drawRect(30, 30, 30 + DB_WIDTH, 30 + DB_HEIGHT, mPaintDB);
//                mPaintDB.setColor(COLOR_DB);
//                canvas.drawRect(30, 30, 30 + mDBWidth, 30 + DB_HEIGHT, mPaintDB);

            float x;
            int ey;
            for (int i = 0; i <= (getWidth() - getPaddingLeft() - getPaddingRight()) / sMinScaleWidth; i++) {
                // draw scale
                x = getPaddingLeft() + sMinScaleWidth * i;
                ey = (i % 5 == 0 ? sMaxY : sMinY);
                canvas.drawLine(x, 0, x, ey, mScalePaint);
            }
        }
    }

    public void updateOSCItems(OSCItem[] items) {
        if (items != null && mOSCs != null) {
            int count = 0;
            int destIndex = 0;
            while (count < mOSCs.length) {
                for (int i = 0; i < items.length && destIndex < mOSCs.length; i++) {
                    OSCItem item = items[i];
                    int sx = destIndex * (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING);
                    if (item != null && Math.abs(item.ty - item.sy) > 0) {
                        if (mOSCs[destIndex] == null) {
                            mOSCs[destIndex] = new OSCItem(item);
                            mOSCs[destIndex].sx = sx;
                            mOSCs[destIndex].tx = sx + OSC_BOTTOM_LINE_WIDTH;
                            if (sx < getPaddingLeft() ||
                                    sx > mOSCs.length * (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING) - getPaddingRight()) {
                                mOSCs[destIndex].sy = 0;
                                mOSCs[destIndex].ty = 0;
                            }
                        } else {
                            mOSCs[destIndex].copyFrom(item);
                            mOSCs[destIndex].sx = sx;
                            mOSCs[destIndex].tx = sx + OSC_BOTTOM_LINE_WIDTH;
                            if (sx < getPaddingLeft() ||
                                    sx > mOSCs.length * (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING) - getPaddingRight()) {
                                mOSCs[destIndex].sy = 0;
                                mOSCs[destIndex].ty = 0;
                            }
                        }
                        destIndex++;
                    }
                }
                count++;
            }
            invalidate();
        }
    }
}
