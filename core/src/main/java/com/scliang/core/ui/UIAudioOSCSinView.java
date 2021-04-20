package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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

import java.util.ArrayList;
import java.util.List;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/3/27.
 */
public class UIAudioOSCSinView extends BaseViewGroup
        implements TextureView.SurfaceTextureListener {
    private TextureView mOSCView;
    private Rect mOSCRect = new Rect();
    private DrawHandler mOSCDrawHandler;

    public UIAudioOSCSinView(Context context) {
        super(context);
    }

    public UIAudioOSCSinView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIAudioOSCSinView(Context context, AttributeSet attrs, int defStyleAttr) {
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
        int maxHeight = dp2px(240);
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
            if (!isInEditMode()) {
                createOSCDrawHandler();
            }
        }
        // 启动绘画操作线程
        if (mOSCDrawHandler != null) {
            mOSCDrawHandler.updateSize(width, height);
            mOSCDrawHandler.startDraw(surface);
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (mOSCDrawHandler != null) {
            mOSCDrawHandler.updateSize(width, height);
        }
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

    /**
     * 获得OSCHandler
     */
    public Handler getOSCHandler() {
        return mOSCDrawHandler;
    }



    /////////////////////////////////////////////
    /////////////////////////////////////////////
    private void createOSCDrawHandler() {
        HandlerThread thread = new HandlerThread("DrawOSCSinThread-" + System.currentTimeMillis());
        thread.start();
        mOSCDrawHandler = new DrawHandler(thread.getLooper(), dp2px(1));
    }



    /////////////////////////////////////////////
    private static class DrawHandler extends Handler {
        // OSC background color
        private static final int COLOR_OSC_BACKGROUND = 0xffffffff;
        // OSC color
        private static final int COLOR_OSC            = 0xff999999;
        // OSC middle line color
        private static final int COLOR_OSC_M_LINE     = 0xff999999;

        private Surface mSurface;
        private Rect mRect = new Rect();
        private boolean isExited;
        private int mNewOSC;
        private float mAmplitude;
        private List<Path> paths;

        private Paint mPaintOSC;
        private Paint mPaintOSCMLine;

        private DrawHandler(Looper looper, int oSCMLineWidth) {
            super(looper);
            // OSC Paint
            mPaintOSC = new Paint();
            mPaintOSC.setAntiAlias(true);
            mPaintOSC.setStrokeWidth(oSCMLineWidth);
            mPaintOSC.setStyle(Paint.Style.STROKE);
            mPaintOSC.setColor(COLOR_OSC);
            // OSC Line Paint
            mPaintOSCMLine = new Paint();
            mPaintOSCMLine.setAntiAlias(true);
            mPaintOSCMLine.setStrokeWidth(oSCMLineWidth);
            mPaintOSCMLine.setStyle(Paint.Style.STROKE);
            mPaintOSCMLine.setColor(COLOR_OSC_M_LINE);
            // path
            paths = new ArrayList<>(1);
            for (int i = 0; i < 1; i++) {
                paths.add(new Path());
            }
        }

        private void updateSize(int width, int height) {
            mRect.set(0, 0, width, height);
        }

        private void startDraw(SurfaceTexture surfaceTexture) {
            mSurface = new Surface(surfaceTexture);
            isExited = false;
            sendEmptyMessage(100);
            Logger.d("UIAudioOSCSinView", "start draw");
        }

        private void exit() {
            isExited = true;
            removeMessages(100);
            getLooper().quitSafely();
            Logger.d("UIAudioOSCSinView", "exit draw");
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // 重置录音示波器数据
                case 0: {
                } break;

                // 接收到录音示波器数据
                case 222: {
                    mNewOSC = Math.max(Math.abs(40 + msg.arg1), 1);
                    float maxOSC = 80;
                    float maxAmplitude = getWidth() * 0.4f;
                    float s = maxOSC / maxAmplitude;
                    float amplitude = mNewOSC / s;
                    if (amplitude > mAmplitude) {
                        mAmplitude = amplitude;
                    }
                } break;

                // 绘画
                case 100: {
                    drawCore();
                    // loop draw
                    if (!isExited) {
                        sendEmptyMessageDelayed(100, 16);
                    }
                } break;
            }
        }

        private int getWidth() {
            return mRect.width();
        }

        private int getHeight() {
            return mRect.height();
        }

        private void changeAmplitude() {
            if (mAmplitude > 0) {
                mAmplitude -= 5;
            } else {
                mAmplitude = 0;
            }
        }

        private void drawCore() {
            // lock canvas
            final Canvas canvas = mSurface.lockCanvas(mRect);
            if (canvas != null) {
                // draw background
                canvas.drawColor(COLOR_OSC_BACKGROUND);
//                // draw middle line
//                drawMLine(canvas);
                // draw osc
                drawVoiceLine(canvas);
                // changeAmplitude
                changeAmplitude();
                // unlock canvas
                if (!isExited && mSurface.isValid()) {
                    try { mSurface.unlockCanvasAndPost(canvas); } catch (Exception ignored) {}
                }
            }
        }

        private void drawMLine(Canvas canvas) {
            canvas.save();
            int y = getHeight() / 2;
            canvas.drawLine(0, y, getWidth(), y, mPaintOSCMLine);
            canvas.restore();
        }

        private void drawVoiceLine(Canvas canvas) {
            float period = 5;
            float phase = -getWidth() / period / 4;
            for (int n = 0; n < paths.size(); n++) {
                drawSine(canvas, paths.get(n), mPaintOSC, period, phase, mAmplitude, getWidth(),
                        getWidth() / 2f, getHeight() / 2f);
            }
        }

        private void drawSine(Canvas canvas, Path path, Paint paint, float period, float phase,
                              float amplitude, float drawWidth,
                              float xOffset, float yOffset) {
            float halfDrawWidth = getWidth() / 2f;
            float y;
            double scaling;
            canvas.save();
            path.reset();
            path.moveTo(xOffset - halfDrawWidth, yOffset); // 将绘制的起点移动到最左边
            for (float x = -halfDrawWidth; x <= halfDrawWidth; x++) {
                scaling = 1 - Math.pow(x / drawWidth * 1.5, 2); // 对y进行缩放
                y = (float) (sine(x, period, drawWidth, phase) * amplitude * Math.pow(scaling, 5));
                path.lineTo(xOffset + x, yOffset + y);
            }
            canvas.drawPath(path, paint);
            canvas.restore();
        }

        private double sine(float x, float period, float drawWidth, float phase) {
            return Math.sin(2 * Math.PI * period * (x + phase) / drawWidth);
        }
    }
}
