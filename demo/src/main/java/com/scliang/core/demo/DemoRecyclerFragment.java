package com.scliang.core.demo;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.scliang.core.base.BaseFragment;
import com.scliang.core.ui.UIRecyclerView;

import java.lang.ref.SoftReference;

/**
 * JCore
 * Created by ShangChuanliang
 * on 2017/9/28.
 */
public class DemoRecyclerFragment extends BaseFragment {

    @Override
    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recycler, container, false);
    }

    @Override
    protected void onViewCreatedHere(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        setToolbarCenterTitle(getClass().getSimpleName());

        final UIRecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setRefreshable(true);
            recyclerView.setLoadMoreable(true);
            recyclerView.setRefreshView(R.layout.view_recycler_refresh);
            recyclerView.setLoadMoreView(R.layout.view_recycler_loadmore);
            recyclerView.setOnRefreshListener(new UIRecyclerView.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.completeRefresh();
                        }
                    }, 5000);
                }
            });
            recyclerView.setOnLoadMoreListener(new UIRecyclerView.OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.completeLoadMore();
                        }
                    }, 5000);
                }
            });
            recyclerView.setAdapter(new DataAdapter(getActivity()));
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private static class DataVHolder extends RecyclerView.ViewHolder {
        private TextView mText;

        public DataVHolder(View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.text);
        }

        public void bindViewHolder(int position) {
            mText.setText("" + (position + 1));
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private static class DataAdapter extends RecyclerView.Adapter<DataVHolder> {
        private SoftReference<Context> mContext;

        public DataAdapter(Context context) {
            mContext = new SoftReference<>(context);
        }

        @Override
        public DataVHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final Context context = mContext.get();
            if (context != null) {
                View itemView = LayoutInflater.from(context).inflate(
                        R.layout.view_recycler_item, parent, false);
                return new DataVHolder(itemView);
            } else {
                return null;
            }
        }

        @Override
        public void onBindViewHolder(DataVHolder holder, int position) {
            if (holder != null) {
                holder.bindViewHolder(position);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getItemCount() {
            return 40;
        }
    }
}
