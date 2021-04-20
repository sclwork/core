package com.scliang.core.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.scliang.core.R;
import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.Data;
import com.scliang.core.base.DataCallback;
import com.scliang.core.base.result.BaseResult;

import java.lang.ref.SoftReference;

import retrofit2.Call;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/27.
 */
public abstract class BaseSimpleFragment
        <REFRESH extends BaseResult, LOADMORE extends BaseResult>
        extends BaseFragment
        implements UIRecyclerView.OnRefreshListener,
        UIRecyclerView.OnLoadMoreListener {
    private Handler mUiHandler;
    private FrameLayout mTopContainer;
    private FrameLayout mBottomContainer;
    private UIRecyclerView mRecyclerView;

    protected LayoutInflater mInflater;

    @Override
    protected final View onCreateViewHere(@NonNull LayoutInflater inflater,
                                          @Nullable ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        mInflater = inflater;
        return mInflater.inflate(R.layout.fragment_base_simple, container, false);
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        mUiHandler = new Handler(Looper.getMainLooper());
        mTopContainer = findViewById(R.id.top_container);
        mBottomContainer = findViewById(R.id.bottom_container);
        mRecyclerView = findViewById(R.id.recycler_view);
        if (mRecyclerView != null) {
            mRecyclerView.setRefreshView(generateRefreshViewId());
            mRecyclerView.setLoadMoreView(generateLoadMoreViewId());
            mRecyclerView.setOnRefreshListener(this);
            mRecyclerView.setOnLoadMoreListener(this);
            mRecyclerView.setAdapter(new DataAdapter(this));
        }
    }

    @Override
    public final void onRefresh() {
        onPreRefresh();
        final Call<REFRESH> call = onCreateRefreshRequestCall();
        if (call != null) {
            Data.getInstance().enqueueCall(this, call, new DataCallback<REFRESH>() {
                @Override
                public void onWaiting(Call<REFRESH> call) {
                    if (mRecyclerView != null) {
//                        mRecyclerView.hideNoDataView();
                        boolean showLoadingView = mRecyclerView.getAdapter().getItemCount() <= 0 &&
                          mRecyclerView.isNoDataViewHide();
                        if (showLoadingView) {
                            mRecyclerView.showLoadingView();
                        } else {
                            mRecyclerView.hideLoadingView();
                        }
                    }
                    onRefreshRequestWaiting(call);
                }

                @Override
                public void onRequest(Call<REFRESH> call) {
                    if (mRecyclerView != null) {
//                        mRecyclerView.hideNoDataView();
                        boolean showLoadingView = mRecyclerView.getAdapter().getItemCount() <= 0 &&
                          mRecyclerView.isNoDataViewHide();
                        if (showLoadingView) {
                            mRecyclerView.showLoadingView();
                        } else {
                            mRecyclerView.hideLoadingView();
                        }
                    }
                    onRefreshRequestStart(call);
                }

                @Override
                public void onResponse(Call<REFRESH> call, @Nullable REFRESH refresh) {
                    // 请求得到响应后，将其从记录列表列表中移除
                    Data.getInstance().removeCall(BaseSimpleFragment.this, call);
                    // 恢复Refresh状态
                    completeRefresh();
                    onRefreshRequestResponse(call, refresh);
                    reloadUI();
                    // 更新提示UI
                    updateTipUIAfterRequestCompleted();
                }

                @Override
                public void onFailure(Call<REFRESH> call, Throwable throwable) {
                    // 请求得到响应后，将其从记录列表列表中移除
                    Data.getInstance().removeCall(BaseSimpleFragment.this, call);
                    // 恢复Refresh状态
                    completeRefresh();
                    onRefreshRequestFailure(call, throwable);
                    reloadUI();
                    // 更新提示UI
                    updateTipUIAfterRequestCompleted();
                }

                @Override
                public void onNoNetwork(Call<REFRESH> call) {
                    // 恢复Refresh状态
                    completeRefresh();
                    onRefreshRequestNoNetWork(call);
                    reloadUI();
                    // 更新提示UI
                    updateTipUIAfterRequestCompleted();
                }
            });
        } else {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    completeRefresh();
                }
            });
        }
    }

    @Override
    public final void onLoadMore() {
        final Call<LOADMORE> call = onCreateLoadMoreRequestCall();
        if (call != null) {
            Data.getInstance().enqueueCall(this, call, new DataCallback<LOADMORE>() {
                @Override
                public void onWaiting(Call<LOADMORE> call) {
                    onLoadMoreRequestWaiting(call);
                }

                @Override
                public void onRequest(Call<LOADMORE> call) {
                    onLoadMoreRequestStart(call);
                }

                @Override
                public void onResponse(Call<LOADMORE> call, @Nullable LOADMORE loadmore) {
                    // 请求得到响应后，将其从记录列表列表中移除
                    Data.getInstance().removeCall(BaseSimpleFragment.this, call);
                    // 恢复LoadMore状态
                    completeLoadMore();
                    onLoadMoreRequestResponse(call, loadmore);
                    reloadUI();
                }

                @Override
                public void onFailure(Call<LOADMORE> call, Throwable throwable) {
                    // 请求得到响应后，将其从记录列表列表中移除
                    Data.getInstance().removeCall(BaseSimpleFragment.this, call);
                    // 恢复LoadMore状态
                    completeLoadMore();
                    onLoadMoreRequestFailure(call, throwable);
                    reloadUI();
                }

                @Override
                public void onNoNetwork(Call<LOADMORE> call) {
                    // 恢复LoadMore状态
                    completeLoadMore();
                    onLoadMoreRequestNoNetWork(call);
                    reloadUI();
                }
            });
        } else {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    completeLoadMore();
                }
            });
        }
    }

    private void updateTipUIAfterRequestCompleted() {
        if (mRecyclerView != null) {
            // 隐藏LoadingView
            mRecyclerView.hideLoadingView();
            // 判断是否需要显示无数据提示
            boolean showNoDataView = mRecyclerView.getAdapter().getItemCount() <= 0;
            if (showNoDataView) {
                mRecyclerView.showNoDataView();
                onShowNoDataView(mRecyclerView.getNoDataView());
            } else {
                mRecyclerView.hideNoDataView();
            }
        }
    }

    private void completeRefresh() {
        if (mRecyclerView != null) {
            mRecyclerView.completeRefresh();
        }
    }

    private void completeLoadMore() {
        if (mRecyclerView != null) {
            mRecyclerView.completeLoadMore();
        }
    }

    public final void setRecyclerViewBackgroundColor(@ColorInt int color) {
        if (mRecyclerView != null) {
            mRecyclerView.setBackgroundColor(color);
        }
    }

    public final void setTopContextView(@LayoutRes int id) {
        if (mTopContainer != null) {
            try {
                mTopContainer.removeAllViews();
                mTopContainer.addView(mInflater.inflate(id, mTopContainer, false),
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT));
            } catch (Exception e) {
                mTopContainer.removeAllViews();
            }
        }
    }

  public final void setBottomContextView(@LayoutRes int id) {
    if (mBottomContainer != null) {
      try {
        mBottomContainer.removeAllViews();
        mBottomContainer.addView(mInflater.inflate(id, mBottomContainer, false),
            new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
      } catch (Exception e) {
        mBottomContainer.removeAllViews();
      }
    }
  }

    public void setLoadingView(@LayoutRes int id) {
        if (mRecyclerView != null) {
            mRecyclerView.setLoadingView(id);
        }
    }

    public void setNoDataView(@LayoutRes int id) {
        if (mRecyclerView != null) {
            mRecyclerView.setNoDataView(id);
        }
    }

    public void setLoadAlled() {
        if (mRecyclerView != null) {
            mRecyclerView.setLoadAlled();
        }
    }

    public void setNoDateTipText(CharSequence text, @IdRes int noDataTipId) {
        if (mRecyclerView != null) {
            TextView tip = mRecyclerView.findViewById(noDataTipId);
            if (tip != null) {
                tip.setText(text);
            }
        }
    }

    public final void refresh() {
        refresh(true);
    }

    public final void refresh(boolean animation) {
        if (animation) {
            if (mRecyclerView != null) {
                mRecyclerView.refresh();
            }
        } else {
            onRefresh();
        }
    }

    public final void reloadUI() {
        if (mRecyclerView != null) {
            RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            adapter.notifyDataSetChanged();
        }
    }

    public final void setRefreshable(boolean able) {
        if (mRecyclerView != null) {
            mRecyclerView.setRefreshable(able);
        }
    }

    public final void setLoadMoreable(boolean able) {
        if (mRecyclerView != null) {
            mRecyclerView.setLoadMoreable(able);
        }
    }

    public void checkSmoothScrollBy(View child, int dx, int dy) {
        if (mRecyclerView != null) {
            mRecyclerView.checkSmoothScrollBy(child, dx, dy);
        }
    }

    public void smoothScrollToPosition(int position) {
        if (mRecyclerView != null) {
            mRecyclerView.smoothScrollToPosition(position);
        }
    }

    protected void onPreRefresh() {}

    protected Call<REFRESH> onCreateRefreshRequestCall() {
        return null;
    }

    protected Call<LOADMORE> onCreateLoadMoreRequestCall() {
        return null;
    }

    protected void onRefreshRequestWaiting(Call<REFRESH> call) {
    }

    protected void onRefreshRequestStart(Call<REFRESH> call) {
    }

    protected void onLoadMoreRequestWaiting(Call<LOADMORE> call) {
    }

    protected void onLoadMoreRequestStart(Call<LOADMORE> call) {
    }

    protected void onRefreshRequestResponse(Call<REFRESH> call, REFRESH result) {
    }

    protected void onLoadMoreRequestResponse(Call<LOADMORE> call, LOADMORE result) {
    }

    protected void onRefreshRequestFailure(Call<REFRESH> call, Throwable t) {
    }

    protected void onRefreshRequestNoNetWork(Call<REFRESH> call) {
    }

    protected void onLoadMoreRequestFailure(Call<LOADMORE> call, Throwable t) {
    }

    protected void onLoadMoreRequestNoNetWork(Call<LOADMORE> call) {
    }

    protected int generateRefreshViewId() {
        return R.layout.view_recycler_refresh;
    }

    protected int generateLoadMoreViewId() {
        return R.layout.view_recycler_loadmore;
    }

    protected void onShowNoDataView(View noDataView) {
    }

    protected int getItemCount() {
        return 0;
    }

    protected int getItemViewType(int position) {
        return -1;
    }

    protected SimpleVHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    protected void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    public static abstract class SimpleVHolder extends RecyclerView.ViewHolder {
        private SoftReference<BaseSimpleFragment> mFragment;
        private SoftReference<RecyclerView.Adapter> mAdapter;

        public void setFragment(BaseSimpleFragment fragment) {
            mFragment = new SoftReference<>(fragment);
        }

        void setAdapter(RecyclerView.Adapter adapter) {
            mAdapter = new SoftReference<>(adapter);
        }

        public SimpleVHolder(View itemView) {
            super(itemView);
        }

        public final BaseSimpleFragment getFragment() {
            return mFragment == null ? null : mFragment.get();
        }

        public final RecyclerView.Adapter getAdapter() {
            return mAdapter == null ? null : mAdapter.get();
        }

        public final void reloadUI() {
            RecyclerView.Adapter adapter = getAdapter();
            if (adapter != null) adapter.notifyDataSetChanged();
        }

        public final int dp2px(float dp) {
            final float scale = itemView.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private static class DataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private SoftReference<BaseSimpleFragment> mFragment;

        public DataAdapter(BaseSimpleFragment fragment) {
            mFragment = new SoftReference<>(fragment);
        }

        @Override
        public SimpleVHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            BaseSimpleFragment fragment = mFragment.get();
            if (fragment != null) {
                SimpleVHolder holder = fragment.onCreateViewHolder(parent, viewType);
                if (holder != null) {
                    holder.setFragment(fragment);
                    holder.setAdapter(this);
                }
                return holder;
            } else {
                return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            BaseSimpleFragment fragment = mFragment.get();
            if (fragment != null) {
                fragment.onBindViewHolder(holder, position);
            }
        }

        @Override
        public int getItemViewType(int position) {
            BaseSimpleFragment fragment = mFragment.get();
            if (fragment != null) {
                return fragment.getItemViewType(position);
            } else {
                return -1;
            }
        }

        @Override
        public int getItemCount() {
            BaseSimpleFragment fragment = mFragment.get();
            if (fragment != null) {
                return fragment.getItemCount();
            } else {
                return 0;
            }
        }
    }
}
