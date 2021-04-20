package com.scliang.core.ui;

import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

public interface OnBannerCreateItemViewListener<T> {
  View onBannerCreateItemView(@NonNull ViewGroup container, int position, T item);
}
