package com.scliang.core.media.object;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/7.
 */
public class PCMBuffer {
    private Cache mCache;
    private long mGlobalOffset;
    private int mSize;

    public static class Cache {
        public short[] data;

        Cache(int capacity) {
            data = new short[capacity];
        }

        final boolean isRecycled() {
            return data == null;
        }

        public final void recycle() {
            data = null;
        }
    }

    public PCMBuffer(int capacity) {
        this(new Cache(capacity), 0, 0);
    }

    private PCMBuffer(Cache cache, long offset, int size) {
        mCache = cache;
        mGlobalOffset = offset;
        mSize = size;
    }

    public void copyFrom(short[] rawData, long offset, int size) {
        if (rawData == null || mCache == null || mCache.isRecycled()) {
            return;
        }

        mGlobalOffset = offset;
        mSize = size;

        System.arraycopy(rawData, 0, mCache.data, 0,
            Math.min(Math.min(rawData.length, mCache.data.length), size));
    }

    private boolean isRecycled() {
        return mCache == null || mCache.isRecycled();
    }

    public final void recycle() {
        if (mCache != null) mCache.recycle();
        mCache = null;
    }

    public short[] getData() {
        if (mCache == null) {
            return null;
        }

        if (mCache.isRecycled()) {
            return null;
        }

        return mCache.data;
    }

    public int getSize() {
        return mSize;
    }

    public void writeTo(RandomAccessFile target) {
        if (isRecycled()) {
            return;
        }

        if (target != null) {
            try {
                target.seek(mGlobalOffset * 2);
                int count = mCache.data.length;
                byte[] dest = new byte[count << 1];
                for (int i = 0; i < count; i++) {
                    dest[i * 2] = (byte) (mCache.data[i]); // mCache.data[i] >> 0
                    dest[i * 2 + 1] = (byte) (mCache.data[i] >> 8);
                }
                target.write(dest);
            } catch (IOException ignored) { }
        }
    }
}
