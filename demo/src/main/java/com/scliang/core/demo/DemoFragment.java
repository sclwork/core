package com.scliang.core.demo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.Data;
import com.scliang.core.base.DataCallback;
import com.scliang.core.base.Logger;
import com.scliang.core.base.NUtils;
import com.scliang.core.demo.api.DemoApi;
import com.scliang.core.demo.result.TestResult;

import retrofit2.Call;

/**
 * JCore
 * Created by ShangChuanliang
 * on 2017/9/28.
 */
public class DemoFragment extends BaseFragment {

    @Override
    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_demo, container, false);
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        setToolbarCenterTitle(getClass().getSimpleName());

        view.findViewById(R.id.action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Data.getInstance().request(DemoFragment.this, DemoApi.class, "test", new String[] {""}, new DataCallback<TestResult>(){
                    @Override
                    public void onWaiting(Call<TestResult> call) {
                        Logger.d("DemoFragment", "Waiting: " + call);
                    }

                    @Override
                    public void onRequest(Call<TestResult> call) {
                        Logger.d("DemoFragment", "Request: " + call);
                    }

                    @Override
                    public void onResponse(Call<TestResult> call, @Nullable TestResult testResult) {
                        Logger.d("DemoFragment", "Response: " + testResult.version);
                    }

                    @Override
                    public void onFailure(Call<TestResult> call, Throwable throwable) {
                        Logger.d("DemoFragment", "Failure: " + call);
                    }

                    @Override
                    public void onNoNetwork(Call<TestResult> call) {
                        Logger.d("DemoFragment", "NoNetwork: " + call);
                    }
                });
            }
        });

        EditText editText = view.findViewById(R.id.edit);
        NUtils.setTrimInflate(editText, 100, false, false, false);
    }
}
