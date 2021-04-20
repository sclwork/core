package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.scliang.core.base.Logger;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/19.
 */
public class UIAudioOSCView extends BaseViewGroup
        implements TextureView.SurfaceTextureListener {
    private TextureView mOSCView;
    private Rect mOSCRect = new Rect();
    private DrawHandler mOSCDrawHandler;
    private OnOSCDrawHandlerChangeListener mOnOSCDrawHandlerChangeListener;

    public UIAudioOSCView(Context context) {
        super(context);
    }

    public UIAudioOSCView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIAudioOSCView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        setBackgroundColor(0x0);

        // 初始化绘画操作Handler
        createOSCDrawHandler();

        // OSC View
        final Context context = getContext();
        mOSCView = new TextureView(context);
        mOSCView.setSurfaceTextureListener(this);
        addView(mOSCView);
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
        int maxHeight = dp2px(320);
        if (height > maxHeight) {
            height = maxHeight;
        }

        int oscPadding = 0; // dp2px(10);
        mOSCView.measure(View.MeasureSpec.makeMeasureSpec(width - oscPadding * 2, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height - oscPadding * 2, View.MeasureSpec.EXACTLY));
        mOSCRect.set(oscPadding, oscPadding,
                oscPadding + mOSCView.getMeasuredWidth(),
                oscPadding + mOSCView.getMeasuredHeight());

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mOSCView.layout(mOSCRect.left, mOSCRect.top, mOSCRect.right, mOSCRect.bottom);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // 检查是否需要创建OSCDrawHandler
        if (mOSCDrawHandler == null) {
            // 初始化绘画操作Handler
            createOSCDrawHandler();
        }
        // 启动绘画操作线程
        mOSCDrawHandler.updateSize(width, height);
        mOSCDrawHandler.startDraw(surface);
        // 通知绘画操作Handler已更改
        if (mOnOSCDrawHandlerChangeListener != null) {
            mOnOSCDrawHandlerChangeListener.onOSCDrawHandlerChanged(mOSCDrawHandler);
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mOSCDrawHandler.updateSize(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // 停止绘画操作线程
        if (mOSCDrawHandler != null) {
            mOSCDrawHandler.exit();
            mOSCDrawHandler = null;
        }
        return true;
    }

    public void setOnOSCDrawHandlerChangeListener(OnOSCDrawHandlerChangeListener listener) {
        mOnOSCDrawHandlerChangeListener = listener;
    }

    /**
     * 获得OSCHandler
     */
    public Handler getOSCHandler() {
        return mOSCDrawHandler;
    }

    /**
     * 获得OSCItems
     */
    public OSCItem[] getOSCItems() {
        return mOSCDrawHandler == null ? null : mOSCDrawHandler.mOSCs;
    }




    /////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////
    public interface OnOSCDrawHandlerChangeListener {
        void onOSCDrawHandlerChanged(Handler handler);
    }



    /////////////////////////////////////////////
    /////////////////////////////////////////////
    private void createOSCDrawHandler() {
        HandlerThread thread = new HandlerThread("DrawOSCThread-" + System.currentTimeMillis());
        thread.start();
        mOSCDrawHandler = new DrawHandler(thread.getLooper(), dp2px(1));
    }

    private static class DrawHandler extends Handler {
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
        private static int sOSCStart = OSC_START_MIDDLE;

        // 示波曲线水平位置
        private static final int OSC_POS_MIDDLE = 0;
        private static final int OSC_POS_BOTTOM = 1;
        private static int sOSCPos = OSC_POS_MIDDLE;

        // 波形相关参数
        private static float OSC_S = 1;
        private static final int OSC_BOTTOM_LINE_WIDTH   = 5; // px
        private static final int OSC_BOTTOM_LINE_PADDING = 1; // px
        private static final int OSC_BOTTOM_SUM_COUNT   = OSC_BOTTOM_LINE_WIDTH;
        private float mOSCBottomSum = 0;
        private int mOSCBottomSumIndex = 0;

        // 分贝长度
        private static final int DB_WIDTH  = 160; // px
        private static final int DB_HEIGHT = 20; // px
        private int mDBWidth = 0;

        private Surface mSurface;
        private Rect mRect = new Rect();
        private boolean isExited;
        private OSCItem[] mOSCs;
        private int mNewOSC;
        private int mDB;

        private Paint mPaintOSC;
        private Paint mPaintOSCMLine;
        private Paint mPaintDB;

        private DrawHandler(Looper looper, int oSCMLineWidth) {
            super(looper);
            // OSC Paint
            mPaintOSC = new Paint();
            mPaintOSC.setAntiAlias(true);
            mPaintOSC.setStrokeWidth(1);
            mPaintOSC.setStyle(Paint.Style.FILL);
            mPaintOSC.setColor(COLOR_OSC);
            // OSC Line Paint
            mPaintOSCMLine = new Paint();
            mPaintOSCMLine.setAntiAlias(true);
            mPaintOSCMLine.setStrokeWidth(oSCMLineWidth);
            mPaintOSCMLine.setStyle(Paint.Style.FILL);
            mPaintOSCMLine.setColor(COLOR_OSC_M_LINE);
            // dB Paint
            mPaintDB = new Paint();
            mPaintDB.setAntiAlias(true);
            mPaintDB.setStyle(Paint.Style.FILL);
            mPaintDB.setColor(COLOR_DB);
        }

        private void updateSize(int width, int height) {
//            synchronized (DrawHandler.class) {
                mRect.set(0, 0, width, height);
                OSC_S = 70f / (height / 2f);
                // resize osc
                if (sOSCStart == OSC_START_MIDDLE) {
                    resizeMiddleOSCs(width);
                } else {
                    resizeRightOSCs(width);
                }
//            }
        }

        private void startDraw(SurfaceTexture surfaceTexture) {
//            synchronized (DrawHandler.class) {
                mSurface = new Surface(surfaceTexture);
                isExited = false;
                sendEmptyMessage(100);
//            }
            Logger.d("UIAudioOSCView", "start draw");
        }

        private void exit() {
//            synchronized (DrawHandler.class) {
                isExited = true;
                removeMessages(100);
                getLooper().quitSafely();
//            }
            Logger.d("UIAudioOSCView", "exit draw");
        }

        private void clearOSCs() {
            if (mOSCs == null) return;
            if (sOSCPos == OSC_POS_MIDDLE) {
                int sHeight = mRect.height() / 2;
                for (OSCItem item : mOSCs) {
                    if (item != null) {
                        item.sy = sHeight;
                        item.ty = item.sy;
                    }
                }
            } else {
                int height = mRect.height();
                for (OSCItem item : mOSCs) {
                    if (item != null) {
                        item.sy = height;
                        item.ty = item.sy;
                    }
                }
            }
            mDB = 0;
        }

        private void resizeMiddleOSCs(int width) {
            OSCItem[] old = mOSCs;
            if (sOSCPos == OSC_POS_MIDDLE) {
//                mOSCs = new OSCItem[width];
                int count = (width / (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)) + 1;
                mOSCs = new OSCItem[count];
            } else {
                int count = (width / (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)) + 1;
                mOSCs = new OSCItem[count];
            }
            if (old != null) {
                int om = old.length / 2;
                int cm = mOSCs.length / 2;
                int mm = Math.min(om, cm);
                int oi;
                int ci;
                for (int i = 0; i < mm; i++) {
                    oi = om - i;
                    ci = cm - i;
                    if (ci >= 0 && oi >= 0) {
                        mOSCs[ci] = old[oi];
                    }
                    oi = om + i;
                    ci = cm + i;
                    if (ci < mOSCs.length && oi < old.length) {
                        mOSCs[ci] = old[oi];
                    }
                }
            }
        }

        private void resizeRightOSCs(int width) {
            OSCItem[] old = mOSCs;
            if (sOSCPos == OSC_POS_MIDDLE) {
//                mOSCs = new OSCItem[width];
                int count = (width / (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)) + 1;
                mOSCs = new OSCItem[count];
            } else {
                int count = (width / (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)) + 1;
                mOSCs = new OSCItem[count];
            }
            if (old != null) {
                int or = old.length - 1;
                int cr = mOSCs.length - 1;
                int mr = Math.min(or, cr);
                int oi;
                int ci;
                for (int i = 0; i < mr; i++) {
                    oi = or - i;
                    ci = cr - i;
                    if (ci >= 0 && oi >= 0) {
                        mOSCs[ci] = old[oi];
                    }
                }
            }
        }

        // -32767 ~ 32767
        private void insertMiddleOSC(int osc) {
            if (mOSCs == null) return;
            if (sOSCPos == OSC_POS_MIDDLE) {
                int sHeight = mRect.height() / 2;
                int cm = mOSCs.length / 2;
                System.arraycopy(mOSCs, 1, mOSCs, 0, cm);
                float s = osc / OSC_S;
                int absY = (int) s;
                OSCItem item = new OSCItem();
//                if (osc >= 0) {
//                    item.sy = sHeight - absY;
//                    item.ty = sHeight;
//                } else {
//                    item.sy = sHeight;
//                    item.ty = sHeight + absY;
//                }
                item.sy = sHeight - absY;
                item.ty = sHeight + absY;
                mOSCs[cm] = item;

//                for (int i = mOSCs.length - 1; i >= 0; i--) {
//                    item = mOSCs[i];
//                    if (item != null) {
//                        item.sx = i;
//                        item.tx = item.sx;
//                    }
//                }
                for (int i = mOSCs.length - 1; i >= 0; i--) {
                    item = mOSCs[i];
                    if (item != null) {
                        item.sx = mRect.right
                                - (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)
                                * (mOSCs.length - 1 - i)
                                - OSC_BOTTOM_LINE_WIDTH;
                        item.tx = item.sx + OSC_BOTTOM_LINE_WIDTH;
                    }
                }
            } else {
                int height = mRect.height();
                int cm = mOSCs.length / 2;
                System.arraycopy(mOSCs, 1, mOSCs, 0, cm);
                float s = osc / OSC_S * 2;
                int absY = (int) s;
                OSCItem item = new OSCItem();
                item.sy = height - absY;
                item.ty = height;
                mOSCs[cm] = item;

                for (int i = mOSCs.length - 1; i >= 0; i--) {
                    item = mOSCs[i];
                    if (item != null) {
                        item.sx = mRect.right
                                - (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)
                                * (mOSCs.length - 1 - i)
                                - OSC_BOTTOM_LINE_WIDTH;
                        item.tx = item.sx + OSC_BOTTOM_LINE_WIDTH;
                    }
                }
            }
        }

        // -32767 ~ 32767
        private void insertRightOSC(int osc) {
            if (sOSCPos == OSC_POS_MIDDLE) {
                int sHeight = mRect.height() / 2;
                int right = mOSCs.length - 1;
                System.arraycopy(mOSCs, 1, mOSCs, 0, right);
                float s = osc / OSC_S;
                int absY = (int) (sHeight * s);
                OSCItem item = new OSCItem();
//                if (osc >= 0) {
//                    item.sy = sHeight - absY;
//                    item.ty = sHeight;
//                } else {
//                    item.sy = sHeight;
//                    item.ty = sHeight + absY;
//                }
                item.sy = sHeight - absY;
                item.ty = sHeight + absY;
                mOSCs[right] = item;

//                for (int i = mOSCs.length - 1; i >= 0; i--) {
//                    item = mOSCs[i];
//                    if (item != null) {
//                        item.sx = i;
//                        item.tx = item.sx;
//                    }
//                }
                for (int i = mOSCs.length - 1; i >= 0; i--) {
                    item = mOSCs[i];
                    if (item != null) {
                        item.sx = mRect.right
                                - (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)
                                * (mOSCs.length - 1 - i)
                                - OSC_BOTTOM_LINE_WIDTH;
                        item.tx = item.sx + OSC_BOTTOM_LINE_WIDTH;
                    }
                }
            } else {
                int height = mRect.height();
                int right = mOSCs.length - 1;
                System.arraycopy(mOSCs, 1, mOSCs, 0, right);
                float s = osc / OSC_S * 2;
                int absY = (int) (height * s);
                OSCItem item = new OSCItem();
                item.sy = height - absY;
                item.ty = height;
                mOSCs[right] = item;

                for (int i = mOSCs.length - 1; i >= 0; i--) {
                    item = mOSCs[i];
                    if (item != null) {
                        item.sx = mRect.right
                                - (OSC_BOTTOM_LINE_WIDTH + OSC_BOTTOM_LINE_PADDING)
                                * (mOSCs.length - 1 - i)
                                - OSC_BOTTOM_LINE_WIDTH;
                        item.tx = item.sx + OSC_BOTTOM_LINE_WIDTH;
                    }
                }
            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                // 重置录音示波器数据
                case 0: {
//                    synchronized (DrawHandler.class) {
                        mOSCBottomSum = 0;
                        mOSCBottomSumIndex = 0;
                        clearOSCs();
//                    }
                } break;

                // 接收到录音示波器数据
                case 222: {
//                    synchronized (DrawHandler.class) {
                        // -32767 ~ 32767
//                        mNewOSC = Math.max(Math.abs(msg.arg1), 10);
//                        mDB = 34 + msg.arg2;
//                        if (mDB < 0) {
//                            mDB = 0;
//                        }
//                        mDBWidth = (int) (DB_WIDTH * (mDB / 70f));
//                        insertNewOSC();
                        mNewOSC = Math.max(Math.abs(40 + msg.arg1), 1);
//                    }
                } break;

                // 绘画
                case 100: {
//                    synchronized (DrawHandler.class) {
                        drawCore();
                        insertNewOSC();
//                    }

                    // loop draw
                    if (!isExited) {
                        sendEmptyMessageDelayed(100, 32);
                    }
                } break;
            }
        }

        private void insertNewOSC() {
            if (sOSCPos == OSC_POS_MIDDLE) {
                if (sOSCStart == OSC_START_MIDDLE) {
                    insertMiddleOSC(mNewOSC);
                } else {
                    insertRightOSC(mNewOSC);
                }
            } else {
                mOSCBottomSum += mNewOSC;
                mOSCBottomSumIndex++;
                if (mOSCBottomSumIndex == OSC_BOTTOM_SUM_COUNT) {
                    int value = (int) (mOSCBottomSum / OSC_BOTTOM_SUM_COUNT);
                    if (sOSCStart == OSC_START_MIDDLE) {
                        insertMiddleOSC(value);
                    } else {
                        insertRightOSC(value);
                    }
                    mOSCBottomSum = 0;
                    mOSCBottomSumIndex = 0;
                } else {
                    drawCore();
                }
            }
        }

        private void drawCore() {
            try {
                // lock canvas
                final Canvas canvas = mSurface.lockCanvas(mRect);
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
//                            canvas.drawLine(item.sx, item.sy, item.tx, item.ty, mPaintOSC);
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
                    // unlock canvas
                    if (!isExited && mSurface.isValid()) {
                        mSurface.unlockCanvasAndPost(canvas);
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
}
