package com.scliang.core.ui;

import android.content.Context;
import android.graphics.Rect;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.scliang.core.R;
import com.scliang.core.base.Logger;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/13.
 */
public class UIRecyclerView extends BaseViewGroup implements Runnable {
    private boolean mRefreshable;
    private boolean mLoadMoreable;

    private boolean mInitLoadMoreable;
    private boolean mSRecyclerRequestLayoutCompletedAble = true;

    private int mTouchSlop;
    private boolean isBeingDragged = false;
    private float mLastMotionY;
    private float mTouchDownY = 0;

    private int mDragOffset;
    private int mRefreshDragLength = -1;
    private int mLoadMoreDragLength = -1;
    private int mPriScrollY = 0;

    private @LayoutRes
    int mRefreshViewId;
    private @LayoutRes
    int mLoadMoreViewId;
    private @LayoutRes
    int mLoadingViewId = R.layout.view_uirecycler_loading;
    private @LayoutRes
    int mNoDataViewId = R.layout.view_uirecycler_no_data;
    private boolean mNoDataViewFinal = false;
    private @LayoutRes
    int mLoadAlledViewId;

    private BaseRecyclerDragView mRefreshView;
    private Rect mRefreshViewRect = new Rect();
    private BaseRecyclerDragView mLoadMoreView;
    private Rect mLoadMoreViewRect = new Rect();

    private View mLoadingView;
    private Rect mLoadingViewRect = new Rect();
    private View mNoDataView;
//    private Rect mNoDataViewRect = new Rect();

    private SRecyclerView mRecyclerView;
    private Rect mRecyclerViewRect = new Rect();
    private RAdapter mRAdapter;

    private boolean isNoticeDragged = false;
    private boolean isRefreshing = false;
    private boolean isLoadMoreing = false;
    private boolean isRestoring = false;
    private boolean isLoading = false;

    private boolean isAutoLoadMore = false;
    private boolean isSlidingToLast = false;

    private Scroller mDragScroller;
    private Runnable mRestoreDragCompleteCallback;
    private OnRefreshListener mOnRefreshListener;
    private OnLoadMoreListener mOnLoadMoreListener;

    public UIRecyclerView(Context context) {
        super(context);
    }

    public UIRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public UIRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onInit() {
        super.onInit();
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        final Context context = getContext();
        // RecyclerView
        mRecyclerView = new SRecyclerView(context, this);
        mRecyclerView.setItemAnimator(new UIRecyclerItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
                updateScrollY();
                isSlidingToLast = mPriScrollY > 0;
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (isAutoLoadMore) {
                    LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && manager != null) {
                        int lastVisibleItem = manager.findLastCompletelyVisibleItemPosition();
                        int totalItemCount = manager.getItemCount();
                        if (lastVisibleItem == (totalItemCount - 1) && isSlidingToLast && loadMoreable() && !isLoading) {
                            startAutoLoadMore();
                        }
                    }
                }
            }
        });
        addView(mRecyclerView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // global padding
        int padding = 0;

        // RefreshView
        if (/*(refreshable() || isRestoring) && isBeingDragged &&*/
                mRefreshView != null/* && mRefreshView.getParent() == this*/) {
            mRefreshView.measure(MeasureSpec.makeMeasureSpec(width - padding * 2, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
            mRefreshViewRect.set(padding, -mRefreshView.getMeasuredHeight(),
                    padding + mRefreshView.getMeasuredWidth(),
                    0);
            if (mRefreshDragLength < 0) {
                mRefreshDragLength = mRefreshView.getMeasuredHeight();
            }
        }
        // RecyclerView
        mRecyclerView.measure(MeasureSpec.makeMeasureSpec(width - padding * 2, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height - padding * 2, MeasureSpec.EXACTLY));
        mRecyclerViewRect.set(padding, padding,
                padding + mRecyclerView.getMeasuredWidth(),
                padding + mRecyclerView.getMeasuredHeight());
        // LoadMoreView
        if (/*(loadMoreable() || isRestoring) && isBeingDragged &&*/
                mLoadMoreView != null/* && mLoadMoreView.getParent() == this*/) {
            mLoadMoreView.measure(MeasureSpec.makeMeasureSpec(width - padding * 2, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
            mLoadMoreViewRect.set(padding, mRecyclerViewRect.bottom,
                    padding + mLoadMoreView.getMeasuredWidth(),
                    mRecyclerViewRect.bottom + mLoadMoreView.getMeasuredHeight());
            if (mLoadMoreDragLength < 0) {
                mLoadMoreDragLength = mLoadMoreView.getMeasuredHeight();
            }
        }

        // LoadingView
        if (mLoadingView != null) {
            mLoadingView.measure(MeasureSpec.makeMeasureSpec(width - padding * 2, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height - padding * 2, MeasureSpec.AT_MOST));
            mLoadingViewRect.set(0, 0,
                    mLoadingView.getMeasuredWidth(), mLoadingView.getMeasuredHeight());
            mLoadingViewRect.offsetTo((width - padding * 2 - mLoadingViewRect.width()) / 2,
                    (height - padding * 2 - mLoadingViewRect.height()) / 2);
        }

//        // NoDataView
//        if (mNoDataView != null) {
//            mNoDataView.measure(MeasureSpec.makeMeasureSpec(width - padding * 2, MeasureSpec.AT_MOST),
//                    MeasureSpec.makeMeasureSpec(height - padding * 2, MeasureSpec.AT_MOST));
//            mNoDataViewRect.set(0, 0,
//                    mNoDataView.getMeasuredWidth(), mNoDataView.getMeasuredHeight());
//            mNoDataViewRect.offsetTo((width - padding * 2 - mNoDataViewRect.width()) / 2,
//                    (height - padding * 2 - mNoDataViewRect.height()) / 2);
//        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // RefreshView
        if ((refreshable() || isRestoring) && isBeingDragged &&
                mRefreshView != null && mRefreshView.getParent() == this) {
            mRefreshView.layout(mRefreshViewRect.left, mRefreshViewRect.top + mDragOffset,
                    mRefreshViewRect.right, mRefreshViewRect.bottom + mDragOffset);
        }
        // RecyclerView
        mRecyclerView.layout(mRecyclerViewRect.left, mRecyclerViewRect.top + mDragOffset,
                mRecyclerViewRect.right, mRecyclerViewRect.bottom + mDragOffset);
        // LoadMoreView
        if ((loadMoreable() || isRestoring) && isBeingDragged &&
                mLoadMoreView != null && mLoadMoreView.getParent() == this) {
            mLoadMoreView.layout(mLoadMoreViewRect.left, mLoadMoreViewRect.top + mDragOffset,
                    mLoadMoreViewRect.right, mLoadMoreViewRect.bottom + mDragOffset);
        }
        // LoadingView
        if (mLoadingView != null) {
            mLoadingView.layout(mLoadingViewRect.left, mLoadingViewRect.top,
                    mLoadingViewRect.right, mLoadingViewRect.bottom);
        }
//        // NoDataView
//        if (mNoDataView != null) {
//            mNoDataView.layout(mNoDataViewRect.left, mNoDataViewRect.top,
//                    mNoDataViewRect.right, mNoDataViewRect.bottom);
//        }
        // Scroller
        if (isRestoring && mDragScroller != null && mDragScroller.computeScrollOffset()) {
            if (mDragScroller.isFinished()) {
                mDragScroller = null;
                isRestoring = false;
                mDragOffset = 0;
                if (mRestoreDragCompleteCallback != null) {
                    mRestoreDragCompleteCallback.run();
                    mRestoreDragCompleteCallback = null;
                }
            } else {
                mDragOffset = mDragScroller.getCurrY();
            }
            post(this::requestLayout);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Logger.d("UIRecyclerView", "dispatchTouchEvent action: " + event.getAction());
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!mRefreshable && !mLoadMoreable) {
            return super.onInterceptTouchEvent(event);
        }

        final int action = event.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (isBeingDragged)) {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                boolean isBeingDraggedY = false;
                final float y = event.getY();
                final float yDiff = y - mLastMotionY;
                updateScrollY();
                if ((yDiff > mTouchSlop && mPriScrollY == 0) ||
                        (yDiff < -mTouchSlop && isSlideToBottom())) {
                    isBeingDraggedY = true;
                    mLastMotionY = y;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                isBeingDragged = isBeingDraggedY;
                mTouchDownY = event.getY();
            }
            break;
            case MotionEvent.ACTION_DOWN: {
                mLastMotionY = event.getY();
                mTouchDownY = event.getY();
                isBeingDragged = false;
            }
            break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                isBeingDragged = false;
            }
            break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mRefreshable && !mLoadMoreable) {
            return super.onTouchEvent(event);
        }

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                int yOffset = (int) (event.getY() - mTouchDownY);
                if ((yOffset > 0 && mRefreshView != null && mRefreshable && !isLoadMoreing) ||
                        (yOffset < 0 && mLoadMoreView != null && mLoadMoreable && !isRefreshing)) {
                    updateDragOffset(yOffset / 2);
                }
//                mTouchDownY = event.getY();
                // BeingDragged
                if (!isNoticeDragged) {
                    isNoticeDragged = true;
                    beingDragged();
                }
                return true;
            }
            case MotionEvent.ACTION_DOWN: {
                isNoticeDragged = false;
                mTouchDownY = event.getY();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                isNoticeDragged = false;
                if (mDragOffset != 0) {
                    Runnable restoreCallback = null;
                    if (refreshable()) {
                        if (canStartRefresh()) {
                            startRefresh();
                        } else {
                            restoreCallback = () -> removeRefreshView(mRefreshView);
                        }
                    }
                    if (loadMoreable()) {
                        if (canStartLoadMore()) {
                            startLoadMore();
                        } else {
                            restoreCallback = () -> removeLoadMoreView(mLoadMoreView);
                        }
                    }
                    restoreDragView(restoreCallback);
                }
            }
            break;
        }
        return super.onTouchEvent(event);
    }

    public final <T extends View> T findViewByIdHere(@IdRes int id) {
        T view = findViewById(id);
        if (view == null && mNoDataView != null) {
            view = mNoDataView.findViewById(id);
        }
        return view;
    }





    //////////////////////////////////////////////
    //////////////////////////////////////////////
    private boolean isSlideToBottom() {
        return mRecyclerView.computeVerticalScrollExtent() +
                mRecyclerView.computeVerticalScrollOffset()
                >= mRecyclerView.computeVerticalScrollRange();
    }

    private void updateScrollY() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int position = layoutManager.findFirstVisibleItemPosition();
            View firstChildView = layoutManager.findViewByPosition(position);
            int itemHeight = firstChildView == null ? 0 : firstChildView.getHeight();
            int top = firstChildView == null ? 0 : firstChildView.getTop();
            ViewGroup.LayoutParams lp = firstChildView == null ? null : firstChildView.getLayoutParams();
            if (lp instanceof MarginLayoutParams) {
                MarginLayoutParams mlp = (MarginLayoutParams) lp;
                top -= mlp.topMargin;
            }
            mPriScrollY = (position) * itemHeight - top;
        }
    }

    private void updateDragOffset(int offset) {
        mDragOffset = offset;
        noticeDragOffsetChanged();
        requestLayout();
    }

    private void beingDragged() {
        if (mDragOffset > 0) {
            if (refreshable()) {
                inflateRefreshView();
                if (mRefreshView != null) {
                    addView(mRefreshView);
                }
            }
        } else if (mDragOffset < 0) {
            if (loadMoreable()) {
                inflateLoadMoreView();
                if (mLoadMoreView != null) {
                    addView(mLoadMoreView);
                }
            }
        }
    }

    private boolean canStartRefresh() {
        return !isLoading && mDragOffset >= mRefreshDragLength;
    }

    private boolean canStartLoadMore() {
        return !isLoading && mDragOffset <= -mLoadMoreDragLength;
    }

    private void restoreDragView(Runnable callback) {
        mRestoreDragCompleteCallback = callback;
        mDragScroller = new Scroller(getContext(), new DecelerateInterpolator());
        mDragScroller.startScroll(0, mDragOffset, 0, -mDragOffset, 150);
        isRestoring = true;
        requestLayout();
    }

    private void restoreRefreshCompleteDragView(Runnable callback) {
        mRestoreDragCompleteCallback = callback;
        int dy = Math.min(mPriScrollY - mRefreshViewRect.height(), 0);
        mDragScroller = new Scroller(getContext(), new DecelerateInterpolator());
        mDragScroller.startScroll(0, 0, 0, dy, 150);
        isRestoring = true;
        requestLayout();
    }

    private void restoreLoadMoreCompleteDragView(Runnable callback) {
//        mRestoreDragCompleteCallback = callback;
//        mDragScroller = new Scroller(getContext(), new DecelerateInterpolator());
//        mDragScroller.startScroll(0, 0, 0, mLoadMoreViewRect.height(), 150);
//        isRestoring = true;
//        requestLayout();
        if (callback != null) callback.run();
    }

    private void removeRefreshView(BaseRecyclerDragView refreshView) {
        if (refreshView != null) {
            removeView(refreshView);
        }
    }

    private void removeLoadMoreView(BaseRecyclerDragView loadMoreView) {
        if (loadMoreView != null) {
            removeView(loadMoreView);
        }
    }

    private boolean refreshable() {
        return mRefreshable && !isRefreshing && !isLoadMoreing;
    }

    private boolean loadMoreable() {
        return mLoadMoreable && !isLoadMoreing && !isRefreshing;
    }

    private void startRefresh() {
        if (mRefreshable && mRefreshView != null) {
            isRefreshing = true;
            removeView(mRefreshView);
            mDragOffset -= mRefreshViewRect.height();
            mRAdapter.setRefreshView(mRefreshView);
            mRAdapter.setLoadAlled(false);
            mSRecyclerRequestLayoutCompletedAble = true;
            setLoadMoreableCore(mInitLoadMoreable);
            noticeDragStartRefresh();
            if (mOnRefreshListener != null) {
                mOnRefreshListener.onRefresh();
            }
        }
    }

    private void startLoadMore() {
        if (mLoadMoreable && mLoadMoreView != null) {
            isLoadMoreing = true;
            removeView(mLoadMoreView);
            mDragOffset += mLoadMoreViewRect.height();
            mRAdapter.setLoadMoreView(mLoadMoreView);
            mRecyclerView.scrollToPosition(mRAdapter.getItemCount() - 1);
            noticeDragStartLoadMore();
            if (mOnLoadMoreListener != null) {
                mOnLoadMoreListener.onLoadMore();
            }
        }
    }

    private void startAutoLoadMore() {
        if (mLoadMoreable && mLoadMoreView != null) {
            isLoadMoreing = true;
            removeView(mLoadMoreView);
            mRAdapter.setAutoLoadMore(mLoadMoreView);
            mRecyclerView.scrollToPosition(mRAdapter.getItemCount() - 1);
            noticeDragStartLoadMore();
            if (mOnLoadMoreListener != null) {
                mOnLoadMoreListener.onLoadMore();
            }
        }
    }

    private void inflateRefreshView() {
        try {
            View tmp = LayoutInflater.from(getContext())
                    .inflate(mRefreshViewId, this, false);
            if (tmp instanceof BaseRecyclerDragView) {
                mRefreshView = (BaseRecyclerDragView) tmp;
            }
        } catch (Exception e) {
            mRefreshView = null;
        }
    }

    private void inflateLoadMoreView() {
        try {
            View tmp = LayoutInflater.from(getContext())
                    .inflate(mLoadMoreViewId, this, false);
            if (tmp instanceof BaseRecyclerDragView) {
                mLoadMoreView = (BaseRecyclerDragView) tmp;
            }
        } catch (Exception e) {
            mLoadMoreView = null;
        }
    }

    private void noticeDragOffsetChanged() {
        if (mDragOffset > 0 && mRefreshView != null && !isRefreshing) {
            mRefreshView.updateDragOffset(mDragOffset, mRefreshDragLength);
        } else if (mDragOffset < 0 && mLoadMoreView != null && !isLoadMoreing) {
            mLoadMoreView.updateDragOffset(Math.abs(mDragOffset), mLoadMoreDragLength);
        } else if (mDragOffset == 0) {
            if (mRefreshView != null && !isRefreshing) {
                mRefreshView.updateDragOffset(0, mRefreshDragLength);
            }
            if (mLoadMoreView != null && !isLoadMoreing) {
                mLoadMoreView.updateDragOffset(0, mLoadMoreDragLength);
            }
        }
    }

    private void noticeDragStartRefresh() {
        if (mRefreshView != null) {
            mRefreshView.startAction();
        }
    }

    private void noticeDragStartLoadMore() {
        if (mLoadMoreView != null) {
            mLoadMoreView.startAction();
        }
    }

    private void noticeDragCompleteRefresh() {
        if (mRefreshView != null) {
            mRefreshView.completeAction();
        }
    }

    private void noticeDragCompleteLoadMore() {
        if (mLoadMoreView != null) {
            mLoadMoreView.completeAction();
        }
    }

    // mSRecyclerRequestLayoutCompleted
    @Override
    public void run() {
        if (!mSRecyclerRequestLayoutCompletedAble) {
            return;
        }

        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter != null) {
            final int count = adapter.getItemCount();
            setLoadMoreableCore(count > 0 && mInitLoadMoreable);
        }
    }




    //////////////////////////////////////////////
    //////////////////////////////////////////////
    public RecyclerView.LayoutManager getLayoutManager() {
        return mRecyclerView.getLayoutManager();
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        mRAdapter = new RAdapter(this, adapter);
        mRecyclerView.setAdapter(mRAdapter);
    }

    public RecyclerView.Adapter getAdapter() {
        return mRAdapter.mAdapter;
    }

    public void refresh() {
        startRefresh();
    }

    public void setRefreshable(boolean able) {
        mRefreshable = able;
    }

    public void setLoadMoreable(boolean able) {
        mInitLoadMoreable = able;
        setLoadMoreableCore(able);
    }

    public void setAutoLoadMore(boolean auto) {
        isAutoLoadMore = auto;
    }

    private void setLoadMoreableCore(boolean able) {
        mLoadMoreable = able;
    }

    public void setRefreshDragLength(int length) {
        mRefreshDragLength = length;
    }

    public void setLoadMoreDragLength(int length) {
        mLoadMoreDragLength = length;
    }

    public int getRefreshDragLength() {
        return mRefreshDragLength;
    }

    public int getLoadMoreDragLength() {
        return mLoadMoreDragLength;
    }

    public void setRefreshView(@LayoutRes int id) {
        mRefreshViewId = id;
        inflateRefreshView();
    }

    public void setLoadMoreView(@LayoutRes int id) {
        mLoadMoreViewId = id;
        inflateLoadMoreView();
    }

    public void setLoadAlledViewId(@LayoutRes int id) {
        mLoadAlledViewId = id;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    public void completeRefresh() {
        if (isRefreshing && mRefreshView != null) {
            restoreRefreshCompleteDragView(() -> {
                LinearLayoutManager layoutManager = (LinearLayoutManager)
                        mRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int position = Math.max(layoutManager.findFirstVisibleItemPosition() - 1, 0);
                    mRAdapter.setRefreshView(null);
                    layoutManager.scrollToPosition(position);
                    noticeDragCompleteRefresh();
                    isRefreshing = false;
                }
            });
        }
    }

    public void completeLoadMore() {
        if (isLoadMoreing && mLoadMoreView != null) {
            restoreLoadMoreCompleteDragView(() -> {
                mRAdapter.setLoadMoreView(null);
                noticeDragCompleteLoadMore();
                isLoadMoreing = false;
            });
        }
    }

    public void setLoadingView(@LayoutRes int id) {
        mLoadingViewId = id;
    }

    public void showLoadingView() {
        if (isLoading) {
            return;
        }

        try {
//            hideNoDataView();
            hideLoadingView();
            mLoadingView = LayoutInflater.from(getContext())
                    .inflate(mLoadingViewId, this, false);
            addView(mLoadingView);
            isLoading = true;
        } catch (Exception e) {
            mLoadingView = null;
            isLoading = false;
        }
    }

    public void hideLoadingView() {
        if (mLoadingView != null) {
            View removeView = mLoadingView;
            mLoadingView = null;
            removeView(removeView);
        }
        isLoading = false;
    }

    public void setNoDataView(@LayoutRes int id) {
        setNoDataView(id, false);
    }

    public void setNoDataView(@LayoutRes int id, boolean finalNoDataView) {
        mNoDataViewId = id;
        mNoDataViewFinal = finalNoDataView;
    }

    public void showNoDataView() {
        if (mNoDataViewFinal && mNoDataView != null) {
            if (mRAdapter != null) mRAdapter.showNoDataView(true);
        } else {
            try {
                hideNoDataView();
                hideLoadingView();
                if (mRAdapter != null) {
                    mNoDataView = LayoutInflater.from(getContext())
                            .inflate(mNoDataViewId, this, false);
                    mRAdapter.setNoDataView(mNoDataView);
                    mRAdapter.showNoDataView(true);
                }
            } catch (Exception e) {
                mNoDataView = null;
            }
        }
    }

    public void hideNoDataView() {
        if (mNoDataView != null && mRAdapter != null) {
            if (mNoDataViewFinal) {
                mRAdapter.showNoDataView(false);
            } else {
                mRAdapter.showNoDataView(false);
            }
        }
    }

    public View getNoDataView() {
        return mNoDataView;
    }

    public boolean isFinalNoDataView() {
        return mNoDataViewFinal;
    }

    public boolean isNoDataViewHide() {
        return mNoDataView == null ||
               mRAdapter == null ||
               !mRAdapter.mNoDataShowing;
    }

    /**
     * 显示全部加载完成
     */
    public void setLoadAlled() {
        if (mRAdapter != null) {
            mRAdapter.setLoadAlledViewId(mLoadAlledViewId);
            mRAdapter.setLoadAlled(true);
            setLoadMoreableCore(false);
            mSRecyclerRequestLayoutCompletedAble = false;
        }
    }

    public void closeLoadAlled() {
        if (mRAdapter != null) {
            mRAdapter.setLoadAlled(false);
            setLoadMoreableCore(mInitLoadMoreable);
            mSRecyclerRequestLayoutCompletedAble = true;
        }
    }

    public void checkSmoothScrollBy(final View child, final int dx, final int dy) {
        if (mRecyclerView != null) {
            boolean smooth = false;
            LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            if (layoutManager != null) {
                int position = layoutManager.findLastVisibleItemPosition();
                for (int offset = 0; offset < 3; offset++) {
                    View childView = layoutManager.findViewByPosition(position - offset);
                    if (child == childView) {
                        smooth = true;
                        break;
                    }
                }
            }
            if (smooth) {
                mRecyclerView.post(() -> mRecyclerView.smoothScrollBy(dx, dy));
            }
        }
    }

    public void scrollToPosition(final int position, final int offset) {
        if (mRecyclerView == null) {
            return;
        }

        mRecyclerView.post(() -> {
            LinearLayoutManager layoutManager =
              (LinearLayoutManager) mRecyclerView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(position, offset);
            }
        });
    }

    public void smoothScrollToPosition(int position) {
        if (mRecyclerView != null) {
            mRecyclerView.smoothScrollToPosition(position);
        }
    }

    public void prohibitRecyclerItemAnimator() {
        if (mRecyclerView == null) {
            return;
        }

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (!(itemAnimator instanceof SimpleItemAnimator)) {
            return;
        }

        itemAnimator.setChangeDuration(0);
        ((SimpleItemAnimator)itemAnimator).setSupportsChangeAnimations(false);
    }

    public void addOnScrollListener(RecyclerView.OnScrollListener listener) {
        if (mRecyclerView == null) {
            return;
        }

        mRecyclerView.addOnScrollListener(listener);
    }

    public void removeOnScrollListener(RecyclerView.OnScrollListener listener) {
        if (mRecyclerView == null) {
            return;
        }

        mRecyclerView.removeOnScrollListener(listener);
    }

    public int getPriScrollY() {
        return mPriScrollY;
    }




    //////////////////////////////////////////////
    //////////////////////////////////////////////
    static final class RRefreshVHolder extends RecyclerView.ViewHolder {
        RRefreshVHolder(View itemView, View child) {
            super(itemView);
            updateChildView(child);
        }

        void updateChildView(View child) {
            if (itemView instanceof FrameLayout) {
                FrameLayout container = (FrameLayout) itemView;
                container.removeAllViews();
                container.addView(child, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
            }
        }
    }
    static final class RLoadMoreVHolder extends RecyclerView.ViewHolder {
        RLoadMoreVHolder(View itemView, View child) {
            super(itemView);
            updateChildView(child);
        }

        void updateChildView(View child) {
            if (itemView instanceof FrameLayout) {
                FrameLayout container = (FrameLayout) itemView;
                container.removeAllViews();
                container.addView(child, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
            }
        }
    }
    static final class RLoadAllVHolder extends RecyclerView.ViewHolder {
        RLoadAllVHolder(View itemView) {
            super(itemView);
        }
    }
    static final class RNoDataVHolder extends RecyclerView.ViewHolder {
        RNoDataVHolder(View itemView) {
            super(itemView);
        }
    }




    //////////////////////////////////////////////
    //////////////////////////////////////////////
    private static final class SRecyclerView extends RecyclerView {
        private SoftReference<Runnable> mRequestLayoutCompleted;

        public SRecyclerView(Context context, Runnable requestLayoutCompleted) {
            super(context);
            mRequestLayoutCompleted = new SoftReference<>(requestLayoutCompleted);
        }

        @Override
        public void requestLayout() {
            super.requestLayout();
            if (mRequestLayoutCompleted != null && mRequestLayoutCompleted.get() != null) {
                mRequestLayoutCompleted.get().run();
            }
        }
    }




    //////////////////////////////////////////////
    //////////////////////////////////////////////
    private static final class RAdapter extends RecyclerView.Adapter {
//        private SoftReference<UIRecyclerView> mView;
        private RecyclerView.Adapter mAdapter;

        private static final int VIEW_TYPE_REFRESH  = 8888;
        private static final int VIEW_TYPE_LOADMORE = 9999;
        private static final int VIEW_TYPE_LOADALLL = 9990;
        private static final int VIEW_TYPE_NODATA   = 9900;

        private View mRefreshView;
        private View mLoadMoreView;
        private View mNoDataView;
        private boolean mRefreshViewRemoved = true;
        private boolean mLoadMoreViewRemoved = true;
        private boolean mLoadMoreViewNeedRemove = false;
        private boolean mLoadAlled = false;
        private boolean mNoDataShowing = false;
        private RRefreshVHolder mRRefreshVHolder;
        private RLoadMoreVHolder mRLoadMoreVHolder;
        private LayoutInflater mInflater;
        private @LayoutRes int mLoadAlledId;

        private RAdapter(UIRecyclerView view, RecyclerView.Adapter adapter) {
//            mView = new SoftReference<>(view);
            mAdapter = adapter;
            mInflater = LayoutInflater.from(view.getContext());
        }

        private void setRefreshView(View view) {
            if (view == null) {
                mRefreshViewRemoved = true;
//                notifyItemRemoved(0);
                notifyDataSetChanged();
            } else {
                mRefreshView = view;
                if (mRRefreshVHolder == null) {
                    mRRefreshVHolder = new RRefreshVHolder(
                            new FrameLayout(mRefreshView.getContext()), mRefreshView);
                } else {
                    mRRefreshVHolder.updateChildView(mRefreshView);
                }
                mRefreshViewRemoved = false;
                notifyDataSetChanged();
            }
        }

        private void setLoadMoreView(View view) {
            if (view == null) {
                mLoadMoreViewNeedRemove = true;
            } else {
                mLoadMoreView = view;
                if (mRLoadMoreVHolder == null) {
                    mRLoadMoreVHolder = new RLoadMoreVHolder(
                            new FrameLayout(mLoadMoreView.getContext()), mLoadMoreView);
                } else {
                    mRLoadMoreVHolder.updateChildView(mLoadMoreView);
                }
                mLoadMoreViewRemoved = false;
            }
            notifyDataSetChanged();
        }

        private void setAutoLoadMore(View view) {
            if (view == null) {
                return;
            }

            if (mLoadMoreViewNeedRemove) {
                mLoadMoreViewNeedRemove = false;
            }

            mLoadMoreView = view;
            if (mRLoadMoreVHolder == null) {
                mRLoadMoreVHolder = new RLoadMoreVHolder(
                        new FrameLayout(mLoadMoreView.getContext()), mLoadMoreView);
            } else {
                mRLoadMoreVHolder.updateChildView(mLoadMoreView);
            }
            mLoadMoreViewRemoved = false;
            notifyDataSetChanged();
        }

        private void setLoadAlledViewId(@LayoutRes int id) {
            mLoadAlledId = id;
        }

        private void setNoDataView(View view) {
            mNoDataView = view;
        }

        private void showNoDataView(boolean show) {
            mNoDataShowing = show;
            notifyDataSetChanged();
        }

        @Override
        public void registerAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
            super.registerAdapterDataObserver(observer);
            mAdapter.registerAdapterDataObserver(observer);
        }

        @Override
        public void unregisterAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
            super.unregisterAdapterDataObserver(observer);
            mAdapter.unregisterAdapterDataObserver(observer);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_REFRESH) {
                return mRRefreshVHolder;
            } else if (viewType == VIEW_TYPE_LOADMORE) {
                return mRLoadMoreVHolder;
            } else if (viewType == VIEW_TYPE_LOADALLL) {
                try {
                    return new RLoadAllVHolder(mInflater.inflate(
                            mLoadAlledId, parent, false));
                } catch (Throwable e) {
                    return new RLoadAllVHolder(mInflater.inflate(
                            R.layout.view_uirecycler_load_all_tip, parent, false));
                }
            } else if (viewType == VIEW_TYPE_NODATA) {
                return new RNoDataVHolder(mNoDataView);
            } else {
                return mAdapter.onCreateViewHolder(parent, viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            checkNeedRemoveLoadMoreView();
            if (mRefreshView != null && !mRefreshViewRemoved && position == 0) {
            } else if (mLoadAlled && position == getItemCount() - 1) {
            } else if (mLoadMoreView != null && !mLoadMoreViewRemoved && position == getItemCount() - 1) {
            } else {
                int pos = mRefreshView != null && !mRefreshViewRemoved ? position - 1 : position;
                mAdapter.onBindViewHolder(holder, pos);
            }
        }

        @Override
        public int getItemViewType(int position) {
            checkNeedRemoveLoadMoreView();
            if (mRefreshView != null && !mRefreshViewRemoved && position == 0) {
                return VIEW_TYPE_REFRESH;
            }
            if (mLoadAlled && position == getItemCount() - 1) {
                return VIEW_TYPE_LOADALLL;
            }
            if (mLoadMoreView != null && !mLoadMoreViewRemoved && position == getItemCount() - 1) {
                return VIEW_TYPE_LOADMORE;
            }
            if (mNoDataShowing && (
                    ((mRefreshView == null || mRefreshViewRemoved) && position == 0) ||
                     (mRefreshView != null && !mRefreshViewRemoved && position == 1))) {
                return VIEW_TYPE_NODATA;
            }
            int pos = mRefreshView != null && !mRefreshViewRemoved ? position - 1 : position;
            return mAdapter.getItemViewType(pos);
        }

        @Override
        public int getItemCount() {
            checkNeedRemoveLoadMoreView();
            int count = Math.max(mAdapter.getItemCount(), 0);
            if (mRefreshView != null && !mRefreshViewRemoved) {
                count++;
            }
            if ((mLoadMoreView != null && !mLoadMoreViewRemoved) || mLoadAlled) {
                count++;
            }
            if (mNoDataShowing && (
                    ((mRefreshView == null || mRefreshViewRemoved) && count <= 0) ||
                     (mRefreshView != null && !mRefreshViewRemoved && count == 1))) {
                count++;
            }
            return count;
        }

        void setLoadAlled(boolean alled) {
            mLoadAlled = alled;
            notifyDataSetChanged();
        }

        private void checkNeedRemoveLoadMoreView() {
            if (mLoadMoreViewNeedRemove) {
                mLoadMoreViewNeedRemove = false;
                mLoadMoreViewRemoved = true;
            }
        }
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }
}
