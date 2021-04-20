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
import android.widget.HorizontalScrollView;

import com.scliang.core.base.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/24.
 */
public class UIAudioCutView extends BaseViewGroup {
    private OSCContainer mOSCContainer;
    private Rect mOSCContainerRect = new Rect();

    public UIAudioCutView(Context context) {
        super(context);
    }

    public UIAudioCutView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIAudioCutView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        setBackgroundColor(0x0);

        // OSC Container
        final Context context = getContext();
        mOSCContainer = new OSCContainer(context);
        addView(mOSCContainer);
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

        int oscPadding = dp2px(10);
        mOSCContainer.measure(View.MeasureSpec.makeMeasureSpec(width - oscPadding * 2, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height - oscPadding * 2, View.MeasureSpec.EXACTLY));
        mOSCContainerRect.set(oscPadding, oscPadding,
                oscPadding + mOSCContainer.getMeasuredWidth(),
                oscPadding + mOSCContainer.getMeasuredHeight());

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mOSCContainer.layout(mOSCContainerRect.left, mOSCContainerRect.top,
                mOSCContainerRect.right, mOSCContainerRect.bottom);
    }

    /**
     * 获得CutHandler
     */
    public Handler getCutHandler() {
        return mOSCContainer.mCutDrawHandler;
    }




    /////////////////////////////////////////////
    /////////////////////////////////////////////
    private static class OSCContainer extends HorizontalScrollView
            implements TextureView.SurfaceTextureListener {
        private TextureView mOSCView;
        private Rect mOSCRect = new Rect();
        private DrawHandler mCutDrawHandler;
        private int mOSCWidth = 0;

        public OSCContainer(Context context) {
            super(context);
            init();
        }

        public OSCContainer(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public OSCContainer(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            setBackgroundColor(0x0);
            setVerticalScrollBarEnabled(false);
            setHorizontalScrollBarEnabled(false);

            // 初始化绘画操作Handler
            HandlerThread thread = new HandlerThread("DrawCutThread-" + System.currentTimeMillis());
            thread.start();
            mCutDrawHandler = new DrawHandler(thread.getLooper(), new OnDrawStateChangedListener() {
                @Override
                public void onOSCdBsChanged(List<String> oscdBs) {
                    updateOSCWidth(oscdBs.size());
                }
            });

            // OSC View
            final Context context = getContext();
            mOSCView = new TextureView(context);
            mOSCView.setSurfaceTextureListener(this);
            addView(mOSCView);
        }

        private int dp2px(float dp) {
            final float scale = getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }

        private void updateOSCWidth(int width) {
            mOSCWidth = width;
            post(new Runnable() {
                @Override
                public void run() {
                    smoothScrollTo(0, 0);
                    requestLayout();
                }
            });
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int wMode = MeasureSpec.getMode(widthMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);
//        int hMode = MeasureSpec.getMode(heightMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            int minHeight = dp2px(80);
            if (height < minHeight) {
                height = minHeight;
            }
            int maxHeight = dp2px(120);
            if (height > maxHeight) {
                height = maxHeight;
            }

            int oscWidth = Math.max(mOSCWidth, width);
            int oscPadding = 0;
            mOSCView.measure(MeasureSpec.makeMeasureSpec(oscWidth - oscPadding * 2, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height - oscPadding * 2, MeasureSpec.EXACTLY));
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
            // 启动绘画操作线程
            mCutDrawHandler.updateSize(width, height);
            mCutDrawHandler.startDraw(surface);
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            mCutDrawHandler.updateSize(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // 停止绘画操作线程
            if (mCutDrawHandler != null) {
                mCutDrawHandler.exit();
                mCutDrawHandler = null;
            }
            return true;
        }
    }




    /////////////////////////////////////////////
    /////////////////////////////////////////////
    private static class DrawHandler extends Handler {
        // OSC background color
        private static final int COLOR_OSC_BACKGROUND = 0xff333333;
        // OSC color
        private static final int COLOR_OSC            = 0xff11ff11;
        // OSC middle line color
        private static final int COLOR_OSC_M_LINE     = 0xff11ff11;

        private static final float OSC_S = 32767f / 12.0f;

        private OnDrawStateChangedListener mOnDrawStateChangedListener;
        private Surface mSurface;
        private Rect mRect = new Rect();
        private boolean isExited;
        private String mOSCdBFileName;
        private RandomAccessFile mOSCdBFile;
        private List<String> mOSCdBs;
        private OSCItem[] mOSCs;
        private Paint mPaintOSC;
        private Paint mPaintOSCMLine;

        private DrawHandler(Looper looper, OnDrawStateChangedListener listener) {
            super(looper);
            mOnDrawStateChangedListener = listener;
            // OSC Paint
            mPaintOSC = new Paint();
            mPaintOSC.setAntiAlias(true);
            mPaintOSC.setStrokeWidth(1);
            mPaintOSC.setStyle(Paint.Style.FILL);
            mPaintOSC.setColor(COLOR_OSC);
            // OSC Line Paint
            mPaintOSCMLine = new Paint();
            mPaintOSCMLine.setAntiAlias(true);
            mPaintOSCMLine.setStrokeWidth(1);
            mPaintOSCMLine.setStyle(Paint.Style.FILL);
            mPaintOSCMLine.setColor(COLOR_OSC_M_LINE);
        }

        private void updateSize(int width, int height) {
            synchronized (DrawHandler.class) {
                mRect.set(0, 0, width, height);
                updateOSCs();
            }
        }

        private void startDraw(SurfaceTexture surfaceTexture) {
            synchronized (DrawHandler.class) {
                mSurface = new Surface(surfaceTexture);
                isExited = false;
                sendEmptyMessage(100);
            }
            Logger.d("UIAudioCutView", "start draw");
        }

        private void exit() {
            synchronized (DrawHandler.class) {
                isExited = true;
                removeMessages(100);
                getLooper().quitSafely();
            }
            Logger.d("UIAudioCutView", "exit draw");
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                // 收到波形分贝数据
                case 888: {
                    synchronized (DrawHandler.class) {
                        mOSCdBFileName = (String) msg.obj;
                        openOSCdBFile();
                        readOSCs();
                        updateOSCs();
                    }
                    if (mOnDrawStateChangedListener != null) {
                        mOnDrawStateChangedListener.onOSCdBsChanged(mOSCdBs);
                    }
                    Logger.d("UIAudioCutView", "OSC-dB: " + mOSCdBs);
                } break;

                // 绘画
                case 100: {
                    synchronized (DrawHandler.class) {
                        // lock canvas
                        final Canvas canvas = mSurface.lockCanvas(mRect);
                        if (canvas != null) {
                            // draw background
                            canvas.drawColor(COLOR_OSC_BACKGROUND);
                            // draw h middle line
                            canvas.drawLine(mRect.left, mRect.centerY(),
                                    mRect.right, mRect.centerY(),
                                    mPaintOSCMLine);
                            // draw osc
                            for (OSCItem item : mOSCs) {
                                if (item != null) {
                                    canvas.drawLine(item.sx, item.sy, item.tx, item.ty, mPaintOSC);
                                }
                            }
                            // unlock canvas
                            if (!isExited && mSurface.isValid()) {
                                mSurface.unlockCanvasAndPost(canvas);
                            }
                        }
                    }

                    // loop draw
                    if (!isExited) {
                        sendEmptyMessageDelayed(100, 16);
                    }
                } break;
            }
        }

        // 打开OSCdB文件
        private void openOSCdBFile() {
            if (mOSCdBFile != null) {
                try {
                    mOSCdBFile.close();
                    mOSCdBFile = null;
                } catch (IOException ignored) {
                }
            }
            File file = new File(mOSCdBFileName);
            if (file.exists()) {
                try {
                    mOSCdBFile = new RandomAccessFile(file, "rw");
                } catch (IOException ignored) {
                }
            }
        }

        private void readOSCs() {
            if (mOSCdBFile != null) {
                mOSCdBs = new ArrayList<>();
                String line;
                try {
                    while ((line = mOSCdBFile.readLine()) != null) {
                        mOSCdBs.add(line);
                    }
                } catch (IOException ignored) {}
            }
        }

        private void updateOSCs() {
            mOSCs = new OSCItem[mOSCdBs.size()];
            int sHeight = mRect.height() / 2;
            int osc;
            float s;
            int absY;
            for (int i = 0; i < mOSCs.length; i++) {
                String[] osc_db = mOSCdBs.get(i).split("#");
                osc = Integer.valueOf(osc_db[0]);
                s = Math.abs(osc) / OSC_S;
                absY = (int) (sHeight * s);
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
                item.sx = i;
                item.tx = item.sx;
                mOSCs[i] = item;
            }
        }
    }




    /////////////////////////////////////////////
    /////////////////////////////////////////////
    private interface OnDrawStateChangedListener {
        void onOSCdBsChanged(List<String> oscdBs);
    }




    /////////////////////////////////////////////
    /////////////////////////////////////////////
    private static class OSCItem {
        private int sx;
        private int tx;
        private int sy;
        private int ty;
    }
}
