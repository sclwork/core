package com.scliang.core.base;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/9/19.
 */
public final class Size<T> {
    private T width;
    private T height;

    public Size(T width, T height) {
        this.width = width;
        this.height = height;
    }

    public T getWidth() {
        return width;
    }

    public T getHeight() {
        return height;
    }
}
