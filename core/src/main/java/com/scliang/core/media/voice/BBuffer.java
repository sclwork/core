package com.scliang.core.media.voice;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/5/9.
 */
final class BBuffer extends Buffer {
    byte[] mBuffer = null;

    BBuffer(byte[] buffer, int size) {
        if (buffer != null) {
            mBuffer = new byte[size];
            System.arraycopy(buffer, 0, mBuffer, 0, size);
        }
    }

    byte[] getBuffer() {
        return mBuffer;
    }
}
