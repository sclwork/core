package com.scliang.core.demo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scliang.core.base.BaseFragment;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/5/9.
 */
public class DemoVoiceFragment extends BaseFragment {

    @Override
    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_demo_voice, container, false);
    }
}
