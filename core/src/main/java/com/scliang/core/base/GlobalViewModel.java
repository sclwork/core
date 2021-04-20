package com.scliang.core.base;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

/**
 * Score
 * Created by ShangChuanliang
 * on 2020/07/15.
 */
public class GlobalViewModel extends AndroidViewModel {
  private Observer<GlobalLiveData> mObserver;
  private GlobalLiveData mData = new GlobalLiveData();

  public GlobalViewModel(@NonNull Application application) {
    super(application);
  }

  public void observeForever(@NonNull Observer<GlobalLiveData> observer) {
    mObserver = observer;
    mData.observeForever(mObserver);
  }

  public void removeObserver() {
    mData.removeObserver(mObserver);
  }

  public void updateData(String action, Bundle args) {
    mData.updateData(action, args);
  }
}
