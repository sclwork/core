package com.scliang.core.base;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.scliang.core.R;
import com.scliang.core.ui.UIRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/8/27.
 */
public abstract class BaseDataLogFileListFragment extends BaseFragment {
    private Handler mUIHandler;
    private UIRecyclerView mUIRecyclerView;
    private LogItemAdapter mLogItemAdapter;

    private interface OnLogFileClickListener {
        void onLogClicked(DataLogFilter.LogItem log);
    }

    protected void onLogFileClicked(DataLogFilter.LogItem log) {}

    @Override
    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_log_file_list, container, false);
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        showToolbarNavigationIcon();
        setToolbarCenterTitle(R.string.title_data_log_file_list);
        setToolbarBottomLineVisibility(true);

        mUIHandler = new Handler(Looper.getMainLooper());
        mUIRecyclerView = findViewById(R.id.list);
        mLogItemAdapter = new LogItemAdapter(new OnLogFileClickListener() {
            @Override
            public void onLogClicked(DataLogFilter.LogItem log) {
                onLogFileClicked(log);
            }
        });
        if (mUIRecyclerView != null) {
            mUIRecyclerView.setRefreshable(true);
            mUIRecyclerView.setRefreshView(R.layout.view_recycler_refresh);
            mUIRecyclerView.setOnRefreshListener(new UIRecyclerView.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refresh();
                }
            });
            mUIRecyclerView.setAdapter(mLogItemAdapter);
        }

        refresh();
    }

    private void refresh() {
        // 获得日志文件列表
        DataLogFilter.getLogFileItems(new DataLogFilter.OnLogItemsListener() {
            @Override
            public void onLogItemsCompleted(final List<DataLogFilter.LogItem> logs) {
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mUIRecyclerView != null) {
                            mUIRecyclerView.completeRefresh();
                        }
                        updateUI(logs);
                    }
                });
            }
        });
    }

    private void updateUI(final List<DataLogFilter.LogItem> logs) {
        if (mLogItemAdapter != null) {
            mLogItemAdapter.setItems(logs);
        }
    }

    private static final class LogItemViewHolder extends RecyclerView.ViewHolder {
        private OnLogFileClickListener mOnLogFileClickListener;
        private TextView mName;

        LogItemViewHolder(View itemView, OnLogFileClickListener listener) {
            super(itemView);
            mOnLogFileClickListener = listener;
            mName = itemView.findViewById(R.id.name);
            if (mName != null) {
                mName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DataLogFilter.LogItem log = (DataLogFilter.LogItem) view.getTag();
                        if (log != null && mOnLogFileClickListener != null) {
                            mOnLogFileClickListener.onLogClicked(log);
                        }
                    }
                });
            }
        }

        void update(DataLogFilter.LogItem log) {
            if (mName != null && log != null) {
                mName.setText(log.name);
                mName.setTag(log);
            }
        }
    }

    private static final class LogItemAdapter extends RecyclerView.Adapter {
        private OnLogFileClickListener mOnLogFileClickListener;
        private List<DataLogFilter.LogItem> mItems = new ArrayList<>();

        LogItemAdapter(OnLogFileClickListener listener) {
            mOnLogFileClickListener = listener;
        }

        void setItems(List<DataLogFilter.LogItem> items) {
            mItems.clear();
            if (items != null) {
                mItems.addAll(items);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_log_file_item, parent, false);
            return new LogItemViewHolder(view, mOnLogFileClickListener);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((LogItemViewHolder)holder).update(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }
}
