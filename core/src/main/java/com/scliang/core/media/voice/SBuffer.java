package com.scliang.core.media.voice;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/5/9.
 */
final class SBuffer extends Buffer {
    short[] mBuffer = null;

    SBuffer(short[] buffer, int size) {
        if (buffer != null) {
            mBuffer = new short[size];
            System.arraycopy(buffer, 0, mBuffer, 0, size);
        }
    }

    short[] getBuffer() {
        return mBuffer;
    }
}
