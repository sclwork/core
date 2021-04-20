package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

public class UIVideoView extends BaseViewGroup implements TextureView.SurfaceTextureListener {
  private final static float sAspectRatio = 16f / 9f;
  private int mWidth;
  private int mHeight;
  private float mAspectRatio = sAspectRatio;
  private TextureView mVideoView;
  private Rect mVideoRect = new Rect();
  private int mVideoWidth = 0;
  private int mVideoHeight = 0;
  private TextureView.SurfaceTextureListener mSurfaceTextureCallback;
  private FrameLayout mVideoMaskContainer;
  private Rect mMaskRect = new Rect();
//  private OnGenerateThumbnailListener mOnGenerateThumbnailListener;
//  private Surface mThumbnailSurface;
  private boolean mHeightChangable = true;
  private boolean mCenterCrop = false;

  public UIVideoView(Context context) {
    super(context);
  }

  public UIVideoView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public UIVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onInit() {
    super.onInit();
    // add video view child
    mVideoView = new TextureView(getContext());
    mVideoView.setSurfaceTextureListener(this);
    addView(mVideoView);
    // add mask view child
    mVideoMaskContainer = new FrameLayout(getContext());
    addView(mVideoMaskContainer);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    mWidth = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    // aspect ratio
    float aspectRatio;
    if (mVideoHeight > 0) {
      aspectRatio = (float) mVideoWidth / (float) mVideoHeight;
    } else {
      aspectRatio = mAspectRatio;
    }

    // child size
    int childWidth = mWidth;
    int childHeight = height;

    if (mCenterCrop) {
      childHeight = (int) (mWidth / aspectRatio);
      if (childHeight < height) {
        mHeight = height;
        childHeight = height;
        childWidth = (int) (childHeight * aspectRatio);
      } else {
        mHeight = height;
        childWidth = mWidth;
      }
    } else {
      childHeight = (int) (mWidth / aspectRatio);
      if (mVideoHeight > 0) {
        if (mHeightChangable) {
          childWidth = mWidth;
          mHeight = childHeight;
        } else {
          if (childHeight > height) {
            childWidth = (int) (height * aspectRatio);
            childHeight = height;
          } else {
            childWidth = mWidth;
          }
          mHeight = height;
        }
      } else {
        childWidth = mWidth;
        mHeight = childHeight;
      }
    }

    mVideoView.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
    int left = (mWidth - childWidth) / 2;
    int top = (mHeight - childHeight) / 2;
    mVideoRect.set(left, top, left + childWidth, top + childHeight);

    mVideoMaskContainer.measure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
    mMaskRect.set(0, 0, mWidth, mHeight);

    setMeasuredDimension(mWidth, mHeight);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    mVideoView.layout(mVideoRect.left, mVideoRect.top, mVideoRect.right, mVideoRect.bottom);
    mVideoMaskContainer.layout(mMaskRect.left, mMaskRect.top, mMaskRect.right, mMaskRect.bottom);
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//    final Bitmap thumbnail = mOnGenerateThumbnailListener == null ?
//        null : mOnGenerateThumbnailListener.onGenerateThumbnail();
//    initVideoSize(width, height, thumbnail);
//    requestLayout();
//    showThumbnail(thumbnail);
    if (mSurfaceTextureCallback != null) {
      mSurfaceTextureCallback.onSurfaceTextureAvailable(surface, width, height);
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//    mVideoWidth = width;
//    mVideoHeight= height;
//    requestLayout();
    if (mSurfaceTextureCallback != null) {
      mSurfaceTextureCallback.onSurfaceTextureSizeChanged(surface, width, height);
    }
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    if (mSurfaceTextureCallback != null) {
      mSurfaceTextureCallback.onSurfaceTextureDestroyed(surface);
    }
    return true;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    if (mSurfaceTextureCallback != null) {
      mSurfaceTextureCallback.onSurfaceTextureUpdated(surface);
    }
  }

  public void setTransform(Matrix transform) {
    if (mVideoView != null) {
      mVideoView.setTransform(transform);
    }
  }

  public Matrix getTransform(Matrix transform) {
    if (transform == null) {
      transform = new Matrix();
    }

    if (mVideoView == null) {
      return transform;
    } else {
      transform = mVideoView.getTransform(transform);
      return transform;
    }
  }

  public void updateVideoSize(int width, int height) {
    mVideoWidth = width;
    mVideoHeight = height;
    requestLayout();
  }

//  private void initVideoSize(int width, int height, Bitmap thumbnail) {
//    if (thumbnail == null) {
//      mVideoWidth = width;
//      mVideoHeight = height;
//    } else {
//      mVideoWidth = thumbnail.getWidth();
//      mVideoHeight = thumbnail.getHeight();
//    }
//  }

//  private void showThumbnail(Bitmap thumbnail) {
//    if (thumbnail != null) {
//      if (mThumbnailSurface != null) {
//        mThumbnailSurface.release();
//        mThumbnailSurface = null;
//      }
//      mThumbnailSurface = new Surface(mVideoView.getSurfaceTexture());
//      final Canvas canvas = mThumbnailSurface.lockCanvas(mVideoRect);
//      if (canvas != null) {
//        int count = canvas.save();
//        canvas.drawBitmap(thumbnail,
//            new Rect(0, 0, thumbnail.getWidth(), thumbnail.getHeight()),
//            mVideoRect,
//            null);
//        canvas.restoreToCount(count);
//      }
//      mThumbnailSurface.unlockCanvasAndPost(canvas);
//      mThumbnailSurface.release();
//      mThumbnailSurface = null;
//    }
//  }

//  public void setOnGenerateThumbnailListener(OnGenerateThumbnailListener listener) {
//    mOnGenerateThumbnailListener = listener;
//  }

  public void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
    mSurfaceTextureCallback = listener;
  }

  public SurfaceTexture getSurfaceTexture() {
    return mVideoView.getSurfaceTexture();
  }

  public void setVideoMaskView(View view) {
    setVideoMaskView(view, null);
  }

  public void setVideoMaskView(View view, FrameLayout.LayoutParams lp) {
    mVideoMaskContainer.removeAllViews();
    if (view != null) {
      mVideoMaskContainer.addView(view, lp != null ? lp :
          new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT));
    }
  }

  public int getViewWidth() {
    return mWidth;
  }

  public int getViewHeight() {
    return mHeight;
  }

  public int getVideoViewWidth() {
    return mVideoRect.width();
  }

  public int getVideoViewHeight() {
    return mVideoRect.height();
  }

  public void setAspectRatio(float aspectRatio) {
    if (aspectRatio <= 0) {
      mAspectRatio = sAspectRatio;
    } else {
      mAspectRatio = aspectRatio;
    }
    requestLayout();
  }

  public void setHeightChangable(boolean changable) {
    mHeightChangable = changable;
    requestLayout();
  }

  public void setCenterCrop(boolean centerCrop) {
    mCenterCrop = centerCrop;
    requestLayout();
  }

  public interface OnGenerateThumbnailListener {
    Bitmap onGenerateThumbnail();
  }
}
