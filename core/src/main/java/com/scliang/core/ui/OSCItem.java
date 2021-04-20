package com.scliang.core.ui;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/9.
 */
final class OSCItem {
    int sx;
    int tx;
    int sy;
    int ty;

    OSCItem() {
    }

    OSCItem(OSCItem item) {
        copyFrom(item);
    }

    void copyFrom(OSCItem item) {
        if (item != null) {
            sx = item.sx;
            tx = item.tx;
            sy = item.sy;
            ty = item.ty;
        }
    }
}
