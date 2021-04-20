package com.scliang.core.base;

import androidx.annotation.Nullable;

import retrofit2.Call;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/11.
 */
public interface DataCallback<T> {
    void onWaiting(Call<T> call);

    void onRequest(Call<T> call);
    void onResponse(Call<T> call, @Nullable T t);
    void onFailure(Call<T> call, Throwable throwable);

    void onNoNetwork(Call<T> call);

}
