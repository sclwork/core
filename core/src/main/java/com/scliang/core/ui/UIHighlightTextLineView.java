package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.core.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.scliang.core.R;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2019/2/21.
 */
public class UIHighlightTextLineView extends BaseViewGroup
    implements NestedScrollView.OnScrollChangeListener {
  public UIHighlightTextLineView(Context context) {
    super(context);
  }

  public UIHighlightTextLineView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public UIHighlightTextLineView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private NestedScrollView mScrollTouchView;
  private ScrollView mScrollView;
  private Rect mScrollRect;
  private TextView mTextView;
  private TextView mTextTouchView;
  private int mTextColor;
  private Rect mTextPadding;
  private Rect mHighlightRect;
  private Rect mNormalTopRect;
  private Rect mNormalBottomRect;
  private int mHighlightTop;
  private int mHighlightHeight;
  private int mHighlightColor;
  private Paint mHighlightPaint;
  private Bitmap mTextBitmap;
  private int mTextBitmapOffsetTop;
  private ColorFilter mColorFilter;

  private boolean mHighlightOpen;

  @Override
  protected void onInit() {
    super.onInit();
    loadLayout();
    setHighlightOpen(true);

    mTextColor = 0xff222222;
    mHighlightColor = 0xffff4400;
    mScrollRect = new Rect();
    mTextPadding = new Rect();
    mHighlightRect = new Rect();
    mNormalTopRect = new Rect();
    mNormalBottomRect = new Rect();
    mHighlightTop = 0;
    mHighlightHeight = dp2px(34);
    mHighlightPaint = new Paint();
    mHighlightPaint.setColor(mHighlightColor);
    mHighlightPaint.setAntiAlias(true);
    mHighlightPaint.setStyle(Paint.Style.FILL);

    createColorFilter();

    if (mScrollView == null) {
      mScrollView = new ScrollView(getContext());
    }
    addView(mScrollView);

    if (mTextView == null) {
      mTextView = new TextView(getContext());
      mScrollView.addView(mTextView, new ScrollView.LayoutParams(
          ScrollView.LayoutParams.MATCH_PARENT,
          ScrollView.LayoutParams.WRAP_CONTENT
      ));
    }
    mTextView.setTextColor(mTextColor);

    mScrollTouchView = new NestedScrollView(getContext());
    mScrollTouchView.setOnScrollChangeListener(this);
    mScrollTouchView.setVerticalScrollBarEnabled(false);
    mScrollTouchView.setHorizontalScrollBarEnabled(false);
    mScrollTouchView.setScrollBarStyle(SCROLLBARS_INSIDE_INSET);
    mScrollTouchView.setVerticalFadingEdgeEnabled(false);
    mScrollTouchView.setHorizontalFadingEdgeEnabled(false);
    mScrollTouchView.setOverScrollMode(OVER_SCROLL_NEVER);
    addView(mScrollTouchView);

    mTextTouchView = new TextView(getContext());
    mTextTouchView.setTextColor(Color.TRANSPARENT);
    mScrollTouchView.addView(mTextTouchView, new NestedScrollView.LayoutParams(
        NestedScrollView.LayoutParams.MATCH_PARENT,
        NestedScrollView.LayoutParams.WRAP_CONTENT
    ));

    setPadding(0, 0, 0, 0);
    setTextSizeDp(19);
  }

  @Override
  protected void onInitAttrs(AttributeSet attrs) {
    super.onInitAttrs(attrs);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    mScrollView.measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    mScrollRect.set(0, 0,
        mScrollView.getMeasuredWidth(), mScrollView.getMeasuredHeight());

    mScrollTouchView.measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

    setMeasuredDimension(width, height);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    mScrollView.layout(mScrollRect.left, mScrollRect.top,
        mScrollRect.right, mScrollRect.bottom);
    mScrollTouchView.layout(mScrollRect.left, mScrollRect.top,
        mScrollRect.right, mScrollRect.bottom);
    // update highlight rect
    mHighlightRect.set(mScrollRect.left, mScrollRect.top + mHighlightTop,
        mScrollRect.right, mScrollRect.top + mHighlightTop + mHighlightHeight);
    // update normal rect
    mNormalTopRect.set(mScrollRect.left, mScrollRect.top,
        mScrollRect.right, mHighlightRect.top);
    mNormalBottomRect.set(mScrollRect.left, mHighlightRect.bottom,
        mScrollRect.right, mScrollRect.bottom);
    // update text padding
    mTextView.setPadding(mTextPadding.left, mTextPadding.top + mHighlightTop,
        mTextPadding.right,
        mTextPadding.bottom + mScrollRect.height() - mHighlightHeight / 2 - mHighlightTop);
    mTextTouchView.setPadding(mTextPadding.left, mTextPadding.top + mHighlightTop,
        mTextPadding.right,
        mTextPadding.bottom + mScrollRect.height() - mHighlightHeight / 2 - mHighlightTop);
    // create text bitmap
    createTextBitmap();
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (mHighlightOpen) {
      drawNormalTopRect(canvas);
      drawHighlightRect(canvas);
      drawNormalBottomRect(canvas);
    }
    // super
    super.dispatchDraw(canvas);
  }

  @Override
  public void onScrollChange(NestedScrollView v,
                             int scrollX, int scrollY,
                             int oldScrollX, int oldScrollY) {
    mTextBitmapOffsetTop = -scrollY;
    mScrollView.scrollTo(scrollX, scrollY);
  }

  private void loadLayout() {
    final View view = LayoutInflater.from(getContext())
        .inflate(R.layout.view_highlight_text_line, this, false);
    if (view != null) {
      mScrollView = view.findViewById(R.id.scroll);
      mTextView = view.findViewById(R.id.text);
    }
  }

  private void createColorFilter() {
    mColorFilter = new LightingColorFilter(0xffffffff, mHighlightColor);
  }

  private void createTextBitmap() {
    if (mTextBitmap != null && !mTextBitmap.isRecycled()) {
      mTextBitmap.recycle();
    }
    if (mHighlightOpen) {
      mTextView.setTextColor(mTextColor);
      mTextBitmap = Bitmap.createBitmap(
          mTextView.getWidth(), mTextView.getHeight(),
          Bitmap.Config.ARGB_8888);
      final Canvas c = new Canvas(mTextBitmap);
      mTextView.draw(c);
      mTextView.setTextColor(Color.TRANSPARENT);
    }
  }

  private void drawNormalTopRect(Canvas canvas) {
    if (mTextBitmap != null && !mTextBitmap.isRecycled() && !mNormalTopRect.isEmpty()) {
      int sc = canvas.saveLayer(mNormalTopRect.left, mNormalTopRect.top,
          mNormalTopRect.right, mNormalTopRect.bottom, null, Canvas.ALL_SAVE_FLAG);
      canvas.drawBitmap(mTextBitmap, 0, mTextBitmapOffsetTop, null);
      canvas.restoreToCount(sc);
    }
  }

  private void drawHighlightRect(Canvas canvas) {
    if (mTextBitmap != null && !mTextBitmap.isRecycled() && !mHighlightRect.isEmpty()) {
      int sc = canvas.saveLayer(mHighlightRect.left, mHighlightRect.top,
          mHighlightRect.right, mHighlightRect.bottom, null, Canvas.ALL_SAVE_FLAG);
      mHighlightPaint.setColorFilter(mColorFilter);
      canvas.drawBitmap(mTextBitmap, 0, mTextBitmapOffsetTop, mHighlightPaint);
      mHighlightPaint.setColorFilter(null);
      canvas.restoreToCount(sc);
    }
  }

  private void drawNormalBottomRect(Canvas canvas) {
    if (mTextBitmap != null && !mTextBitmap.isRecycled() && !mNormalBottomRect.isEmpty()) {
      int sc = canvas.saveLayer(mNormalBottomRect.left, mNormalBottomRect.top,
          mNormalBottomRect.right, mNormalBottomRect.bottom, null, Canvas.ALL_SAVE_FLAG);
      canvas.drawBitmap(mTextBitmap, 0, mTextBitmapOffsetTop, null);
      canvas.restoreToCount(sc);
    }
  }

  public void setHighlightOpen(boolean open) {
    mHighlightOpen = open;
    setLayerType(mHighlightOpen ? LAYER_TYPE_SOFTWARE : LAYER_TYPE_HARDWARE, null);
    requestLayout();
  }

  public void setHightlightColor(@ColorInt int color) {
    mHighlightColor = color;
    createColorFilter();
    postInvalidate();
  }

  public void setHighlightRect(int top, int height) {
    mHighlightTop = top;
    mHighlightHeight = height;
    requestLayout();
  }

  public void setText(CharSequence text) {
    mTextBitmapOffsetTop = 0;
    mScrollView.scrollTo(0, 0);
    mScrollTouchView.scrollTo(0, 0);
    mTextView.setText(text);
    mTextTouchView.setText(text);
    requestLayout();
  }

  public void setTextColor(@ColorInt int color) {
    mTextColor = color;
    createColorFilter();
    createTextBitmap();
  }

  public void setTextSizeDp(float dp) {
    mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, dp);
    mTextTouchView.setTextSize(TypedValue.COMPLEX_UNIT_SP, dp);
  }

  public void setTextLetterSpacing(float letterSpacing) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mTextView.setLetterSpacing(letterSpacing);
      mTextTouchView.setLetterSpacing(letterSpacing);
    }
  }

  public void setTextLineSpacingDp(float add, float mult) {
    mTextView.setLineSpacing(add, mult);
    mTextTouchView.setLineSpacing(add, mult);
  }

  /**
   * text padding
   * ignore bottom
   * @param left
   * @param top
   * @param right
   * @param bottom ignore
   */
  @Override
  public void setPadding(int left, int top, int right, int bottom) {
    mTextPadding.set(left, top, right, 0);
    mTextView.setPadding(mTextPadding.left, mTextPadding.top + mHighlightTop,
        mTextPadding.right,
        mTextPadding.bottom + mScrollRect.height() - mHighlightHeight / 2 - mHighlightTop);
    mTextTouchView.setPadding(mTextPadding.left, mTextPadding.top + mHighlightTop,
        mTextPadding.right,
        mTextPadding.bottom + mScrollRect.height() - mHighlightHeight / 2 - mHighlightTop);
  }
}
