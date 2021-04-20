package com.scliang.core.demo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.DataCallback;
import com.scliang.core.base.Logger;
import com.scliang.core.demo.api.DemoApi;
import com.scliang.core.demo.result.TestResult;

import retrofit2.Call;

/**
 * SCore Demo
 * Created by ShangChuanliang
 * on 2017/10/10.
 */
public class DemoMainFragment extends BaseFragment {

    @Override
    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_demo_main, container, false);
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        setToolbarCenterTitle("Demo");

        // 测试 -- 测试
        view.findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFragment(DemoFragment.class);
            }
        });

        view.findViewById(R.id.bridge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFragment(DemoWebFragment.class);
            }
        });

        view.findViewById(R.id.audio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFragment(DemoAudioFragment.class);
            }
        });

        view.findViewById(R.id.video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFragment(DemoVideoFragment.class);
            }
        });

        view.findViewById(R.id.http).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                request(DemoApi.class, "test", new String[]{"8869"}, new DataCallback<TestResult>() {
                    @Override
                    public void onWaiting(Call<TestResult> call) {
                        Logger.d("MainActivity", "Waiting: " + call.toString());
                    }

                    @Override
                    public void onRequest(Call<TestResult> call) {
                        Logger.d("MainActivity", "Request: " + call.toString());
                    }

                    @Override
                    public void onResponse(Call<TestResult> call, @Nullable TestResult result) {
                        Logger.d("MainActivity", "Result: " + (result != null ? result.version : null));
                    }

                    @Override
                    public void onFailure(Call<TestResult> call, Throwable t) {
                        Logger.d("MainActivity", "Request Failure: " + t);
                    }

                    @Override
                    public void onNoNetwork(Call<TestResult> call) {
                        Logger.d("MainActivity", "Request NoNetwork");
                    }
                });
            }
        });

        view.findViewById(R.id.recycler).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFragment(DemoRecyclerFragment.class);
            }
        });

        view.findViewById(R.id.simple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        view.findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        view.findViewById(R.id.category).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFragment(DemoCategoryFragment.class);
            }
        });

        view.findViewById(R.id.push_dlg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DemoPushFragment demoFragment = new DemoPushFragment();
                showPushDialog(demoFragment);
            }
        });

        view.findViewById(R.id.category_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFragment(DemoCategoryListFragment.class);
            }
        });
    }
}
