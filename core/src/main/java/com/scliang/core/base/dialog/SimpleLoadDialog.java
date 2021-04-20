package com.scliang.core.base.dialog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.scliang.core.R;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/30.
 */
public class SimpleLoadDialog extends BaseDialog {

  @Override
  protected View onCreateContextView(LayoutInflater inflater, ViewGroup container) {
    return inflater.inflate(R.layout.view_dialog_simple_load, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // 预先隐藏LoadingAnimation
    final View loading = view.findViewById(R.id.loading);
    if (loading != null) loading.setVisibility(View.VISIBLE);
    final View loadingAnimation = view.findViewById(R.id.loading_animation);
    if (loadingAnimation != null) loadingAnimation.setVisibility(View.GONE);

    // tip
    final TextView tip = view.findViewById(R.id.tip);
    if (tip != null) tip.setVisibility(View.GONE);

    // 检查是否有可用动画JSON
    Bundle args = getArguments();
    final String assetsJsonFileName = args == null ? "" :
        args.getString("AssetsJson", "");
    if (!TextUtils.isEmpty(assetsJsonFileName)) {
      useLoadingAnimation(assetsJsonFileName);
    }
  }

  public boolean updateTip(final String tipText) {
    return updateTip(tipText, 0xff222222, 15);
  }

  public boolean updateTip(final String tipText, final int tipColor, final int tipSize) {
    final View view = getView();
    if (view == null) {
      return false;
    }

    final TextView tip = view.findViewById(R.id.tip);
    if (tip != null) {
      tip.post(new Runnable() {
        @Override
        public void run() {
          if (TextUtils.isEmpty(tipText)) {
            tip.setVisibility(View.GONE);
          } else {
            tip.setText(tipText);
            tip.setTextColor(tipColor);
            tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, tipSize);
            tip.setVisibility(View.VISIBLE);
          }
        }
      });
    }

    return true;
  }

  public void useLoadingAnimation(final String assetsJsonFileName) {
    final View view = getView();
    if (view == null) {
      return;
    }

    if (TextUtils.isEmpty(assetsJsonFileName)) {
      final View loading = view.findViewById(R.id.loading);
      if (loading != null) loading.setVisibility(View.VISIBLE);
      final View loadingAnimation = view.findViewById(R.id.loading_animation);
      if (loadingAnimation != null) loadingAnimation.setVisibility(View.GONE);
      return;
    }

    final View loading = view.findViewById(R.id.loading);
    if (loading != null) loading.setVisibility(View.GONE);
    final LottieAnimationView loadingAnimation = view.findViewById(R.id.loading_animation);
    if (loadingAnimation != null) {
      loadingAnimation.setVisibility(View.VISIBLE);
      loadingAnimation.setAnimation(assetsJsonFileName);
    }
  }
}
