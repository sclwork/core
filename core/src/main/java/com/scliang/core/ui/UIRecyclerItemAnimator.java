package com.scliang.core.ui;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.animation.Interpolator;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/27.
 */
public final class UIRecyclerItemAnimator extends BaseRecyclerItemAnimator {
    public UIRecyclerItemAnimator() {
    }

    public UIRecyclerItemAnimator(Interpolator interpolator) {
        this();
        mInterpolator = interpolator;
    }

    @Override
    protected void preAnimateAddImpl(RecyclerView.ViewHolder holder) {
        holder.itemView.setAlpha(1);
    }

    @Override
    protected void animateAddImpl(RecyclerView.ViewHolder holder) {
    }

    @Override
    protected void animateRemoveImpl(RecyclerView.ViewHolder holder) {
        if (holder instanceof UIRecyclerView.RRefreshVHolder) {
            ViewCompat.animate(holder.itemView)
                    .translationY(-holder.itemView.getHeight())
                    .alpha(0)
                    .setDuration(getRemoveDuration())
                    .setInterpolator(mInterpolator)
                    .setListener(new DefaultRemoveVpaListener(holder))
                    .setStartDelay(getRemoveDelay(holder))
                    .start();
        } else if (holder instanceof UIRecyclerView.RLoadMoreVHolder) {
//            ViewCompat.animate(holder.itemView)
//                    .translationY(holder.itemView.getHeight())
//                    .alpha(0)
//                    .setDuration(getRemoveDuration())
//                    .setInterpolator(mInterpolator)
//                    .setListener(new DefaultRemoveVpaListener(holder))
//                    .setStartDelay(getRemoveDelay(holder))
//                    .start();
        } else {
            ViewCompat.animate(holder.itemView)
                    .alpha(0)
                    .setDuration(300)
                    .setInterpolator(mInterpolator)
                    .setListener(new DefaultRemoveVpaListener(holder))
                    .setStartDelay(getRemoveDelay(holder))
                    .start();
        }
    }
}
