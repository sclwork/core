package com.scliang.core.ui;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/7.
 */
public interface OnCategoryListener {
    void onItemMove(int starPos, int endPos);
    void onMoveToMyChannel(int starPos, int endPos);
    void onMoveToOtherChannel(int starPos, int endPos);
}
