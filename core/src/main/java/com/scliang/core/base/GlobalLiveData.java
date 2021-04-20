package com.scliang.core.base;

import android.os.Bundle;

import androidx.lifecycle.LiveData;

/**
 * Score
 * Created by ShangChuanliang
 * on 2020/07/15.
 */
public class GlobalLiveData extends LiveData<GlobalLiveData> {
  private String mAction;
  private Bundle mArgs;

  public void updateData(String action, Bundle args) {
    mAction = action;
    mArgs = args;
    postValue(this);
  }

  public String getAction() {
    return mAction;
  }

  public Bundle getArgs() {
    return mArgs;
  }
}
