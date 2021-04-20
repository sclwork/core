package com.scliang.core.demo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scliang.core.base.BaseFragment;
import com.scliang.core.ui.UIHighlightTextLineView;

/**
 * JCore
 * Created by ShangChuanliang
 * on 2017/9/28.
 */
public class DemoHighlightTextLineFragment extends BaseFragment {

    @Override
    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_demo_highlight_text, container, false);
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
      super.onViewCreatedHere(view, savedInstanceState);
      setToolbarCenterTitle("文本高亮");

      UIHighlightTextLineView highlight = view.findViewById(R.id.highlight);
      if (highlight != null) {
        int padding = dp2px(15);
//        highlight.setHighlightRect(dp2px(120), dp2px(34));
        highlight.setBackgroundColor(0xffffffff);
        highlight.setPadding(padding, 0, padding, padding);
        highlight.setTextLineSpacingDp(15, 1.5f);
        highlight.setTextLetterSpacing(0.2f);
        highlight.setText("Rust是一门系统编程语言 [1]  ，专注于安全 [2]  ，尤其是并发安全，支持函数式和命令式以及泛型等编程范式的多范式语言。Rust在语法上和C++类似 [3]  ，但是设计者想要在保证性能的同时提供更好的内存安全。 Rust最初是由Mozilla研究院的Graydon Hoare设计创造，然后在Dave Herman, Brendan Eich以及很多其他人的贡献下逐步完善的。 [4]  Rust的设计者们通过在研发Servo网站浏览器布局引擎过程中积累的经验优化了Rust语言和Rust编译器。");
      }
    }
}
