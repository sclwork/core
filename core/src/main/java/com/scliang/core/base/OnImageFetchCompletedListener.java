package com.scliang.core.base;

import android.net.Uri;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/9/12.
 */
public interface OnImageFetchCompletedListener {

    void onImageFetchCompleted(Uri uri, int width, int height);

}
