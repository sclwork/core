package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;
import com.scliang.core.R;
import com.scliang.core.base.Data;
import com.scliang.core.base.OnImageFetchCompletedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/9/12.
 */
public class UIBannerLayout<T extends IBanner> extends BaseViewGroup
        implements OnBannerWHRatioChangeListener, OnBannerItemClickListener,
        ViewPager.OnPageChangeListener, View.OnTouchListener,
    OnBannerCreateItemViewListener<T> {
    private int mWidth;
    private int mHeight;
    private float mRatio;
    private float mGivenRatio;
    private ViewPager mViewPager;
    private BannerAdapter mBannerAdapter;
    private static Rect mViewPagerRect = new Rect();
    private OnBannerItemClickListener mOnBannerItemClickListener;
    private OnBannerCreateItemViewListener<T> mOnBannerCreateItemViewListener;
    private UIPointsBar mPointBar;
    private Rect mPointRect = new Rect();
    private Timer mAutoTimer;
    private long mAutoPeriod = 3000;

    public UIBannerLayout(Context context) {
        super(context);
    }

    public UIBannerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UIBannerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        final Context context = getContext();
        // init
        mViewPager = new ViewPager(context);
        mBannerAdapter = new BannerAdapter<>(context,
            this, this, this);
        mPointBar = new UIPointsBar(context);
        // set
        mViewPager.setOnTouchListener(this);
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setAdapter(mBannerAdapter);
        // add
        addView(mViewPager);
        addView(mPointBar);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);

        if (mGivenRatio <= 0) {
            mHeight = mRatio == 0 ? ((int) (mWidth * 0.1f)) : ((int) (mWidth / mRatio));
        } else {
            mHeight = (int) (mWidth / mGivenRatio);
        }

        if (mViewPager != null) {
            mViewPager.measure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
            mViewPagerRect.set(0, 0,
                    mViewPager.getMeasuredWidth(), mViewPager.getMeasuredHeight());
        }

        if (mPointBar != null) {
            mPointBar.measure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.AT_MOST));
            int w = mPointBar.getMeasuredWidth();
            int h = mPointBar.getMeasuredHeight();
            int l = (mWidth - w) / 2;
            int t = mHeight - h - dp2px(5);
            mPointRect.set(l, t, l + w, t + h);
        }

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mViewPager != null) {
            mViewPager.layout(mViewPagerRect.left, mViewPagerRect.top,
                    mViewPagerRect.right, mViewPagerRect.bottom);
        }
        if (mPointBar != null) {
            mPointBar.layout(mPointRect.left, mPointRect.top,
                    mPointRect.right, mPointRect.bottom);
        }
    }

    @Override
    public void onBannerWHRatioChanged(float ratio) {
        mRatio = ratio;
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
    }

    @Override
    public void onBannerItemClicked(IBanner banner) {
        if (mOnBannerItemClickListener != null) {
            mOnBannerItemClickListener.onBannerItemClicked(banner);
        }
    }

    @Override
    public View onBannerCreateItemView(@NonNull ViewGroup container, int position, T item) {
        if (mOnBannerCreateItemViewListener == null) {
            return null;
        } else {
            return mOnBannerCreateItemViewListener
                .onBannerCreateItemView(container, position, item);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (mPointBar != null) {
            PagerAdapter adapter = mViewPager.getAdapter();
            int count = adapter == null ? 0 : adapter.getCount();
            if (position + 1 >= count) {
                position = 1;
            }
            mPointBar.setCurrent(position - 1);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE) {
            stopAutoTimer();
        } else if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_OUTSIDE ||
                action == MotionEvent.ACTION_CANCEL) {
            startAutoTimer();
        }
        return false;
    }

    public void setGivenRatio(float ratio) {
        mGivenRatio = ratio;
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
    }

    public void setPointColor(int normalColor, int selectColor) {
        if (mPointBar != null) {
            mPointBar.setPointColor(normalColor, selectColor);
        }
    }

    public void setOnBannerItemClickListener(OnBannerItemClickListener listener) {
        mOnBannerItemClickListener = listener;
    }

    public void setOnBannerCreateItemViewListener(
        OnBannerCreateItemViewListener<T> listener) {
        mOnBannerCreateItemViewListener = listener;
    }

    public void updateItems(T... items) {
        stopAutoTimer();
        if (mBannerAdapter != null) {
            List<T> is = new ArrayList<>();
            if (items != null && items.length > 0) {
                Collections.addAll(is, items);
            }
            mBannerAdapter.updateItems(is, mViewPager);
        }
        if (mPointBar != null) {
            mPointBar.setCount(items == null ? 0 : items.length);
        }
        startAutoTimer();
    }

    public void setAutoPeriod(long period) {
        stopAutoTimer();
        mAutoPeriod = period;
        startAutoTimer();
    }

    private void startAutoTimer() {
        if (mAutoTimer != null) {
            mAutoTimer.cancel();
            mAutoTimer = null;
        }

        PagerAdapter adapter = mViewPager.getAdapter();
        int count = adapter == null ? 0 : adapter.getCount();
        if (count <= 3) {
            return;
        }

        mAutoTimer = new Timer();
        mAutoTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mViewPager != null) {
                    PagerAdapter adapter = mViewPager.getAdapter();
                    int count = adapter == null ? 0 : adapter.getCount();
                    int current = mViewPager.getCurrentItem();

                    current++;
                    if (current >= count) {
                        current = 1;
                    }

                    final int fCurrent = current;
                    mViewPager.post(new Runnable() {
                        @Override
                        public void run() {
                            mViewPager.setCurrentItem(fCurrent, true);
                        }
                    });
                }
            }
        }, mAutoPeriod, mAutoPeriod);
    }

    private void stopAutoTimer() {
        if (mAutoTimer != null) {
            mAutoTimer.cancel();
            mAutoTimer = null;
        }
    }

    ///////////////////////////////////////////////
    ///////////////////////////////////////////////
    private static class BannerAdapter<T extends IBanner> extends LoopVPAdapter<T>
            implements OnImageFetchCompletedListener {
        private float mRatio;
        private OnBannerWHRatioChangeListener mOnBannerWHRatioChangeListener;
        private OnBannerItemClickListener mOnBannerItemClickListener;
        private OnBannerCreateItemViewListener<T> mBannerCreateItemViewListener;

        BannerAdapter(Context context, OnBannerWHRatioChangeListener listener,
                      OnBannerItemClickListener clickListener,
                      OnBannerCreateItemViewListener<T> createListener) {
            super(context);
            mOnBannerWHRatioChangeListener = listener;
            mOnBannerItemClickListener = clickListener;
            mBannerCreateItemViewListener = createListener;
        }

        @Override
        public void updateItems(List<T> items, ViewPager viewPager) {
            mRatio = 0;
            super.updateItems(items, viewPager);
            if (items.size() <= 0) {
                if (mOnBannerWHRatioChangeListener != null) {
                    mOnBannerWHRatioChangeListener.onBannerWHRatioChanged(mRatio);
                }
            }
        }

        @Override
        protected View getItemView(@NonNull ViewGroup container, int position, final T item) {
            View view = mBannerCreateItemViewListener == null ? null :
                mBannerCreateItemViewListener.onBannerCreateItemView(container, position, item);
            if (view == null) {
                view = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.view_banner_item, container, false);
            }
            SimpleDraweeView draweeView = view.findViewById(R.id.banner_item_pic);
            if (item != null && draweeView != null) {
                String imageUrl = item.getImageUrl();
                if (!TextUtils.isEmpty(imageUrl)) {
                    Data.getInstance().fetchSimpleDraweeView(draweeView, imageUrl, this);
                }
            }

            View action = view.findViewById(R.id.banner_item_action);
            if (item != null && action != null) {
                action.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mOnBannerItemClickListener != null) {
                            mOnBannerItemClickListener.onBannerItemClicked(item);
                        }
                    }
                });
            }

            container.addView(view);
            return view;
        }

        @Override
        public void onImageFetchCompleted(Uri uri, int width, int height) {
            float r = width / ((float)height);
            if (r < mRatio || mRatio == 0) {
                mRatio = r;
                if (mOnBannerWHRatioChangeListener != null) {
                    mOnBannerWHRatioChangeListener.onBannerWHRatioChanged(mRatio);
                }
            }
        }
    }
}
