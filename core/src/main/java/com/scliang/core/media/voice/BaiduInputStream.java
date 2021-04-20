//package com.scliang.core.media.voice;
//
//import android.support.annotation.NonNull;
//
//import com.scliang.core.base.Logger;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Queue;
//
///**
// * SCore
// * Created by ShangChuanliang
// * on 2018/5/9.
// */
//public class BaiduInputStream extends InputStream {
//    private Queue<Buffer> mBufferQueue;
//    private byte[] mCacheBuffer;
//
//    BaiduInputStream(Queue<Buffer> bufferQueue) {
//        mBufferQueue = bufferQueue;
//    }
//
//    public void start() {
//    }
//
//    public void stop() {
//    }
//
//    @Override
//    public int read() throws IOException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public int read(@NonNull byte[] dest) throws IOException {
//        return read(dest, 0, dest.length);
//    }
//
//    @Override
//    public int read(@NonNull byte[] dest, int off, int len) throws IOException {
//        // 判断BufferQueue是否有效
//        if (mBufferQueue == null) {
//            return -1;
//        }
//
//        // 判断destBuffer是否有效
//        int destCount = len - off;
//        if (destCount <= 0) {
//            return 0;
//        }
//
//        byte[] src = null;
//
//        // 判断是存在缓存的数据
//        // 读取缓存的数据
//        if (mCacheBuffer != null) {
//            src = mCacheBuffer;
//        }
//        // 读取BufferQueue中的数据
//        else {
//            try {
//                synchronized (Voice.sVoiceBufferQueueSync) {
//                    Buffer buffer = mBufferQueue.poll();
//                    if (buffer != null) {
//                        if (buffer instanceof BBuffer) {
//                            BBuffer bBuffer = (BBuffer) buffer;
//                            src = bBuffer.mBuffer;
//                        } else if (buffer instanceof SBuffer) {
//                            SBuffer sBuffer = (SBuffer) buffer;
//                            short[] sbs = sBuffer.mBuffer;
//                            int count = sbs.length;
//                            src = new byte[count << 1];
//                            for (int i = 0; i < count; i++) {
//                                src[i * 2] = (byte) (sbs[i] >> 0);
//                                src[i * 2 + 1] = (byte) (sbs[i] >> 8);
//                            }
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                Logger.e("BaiduInputStream", e.toString());
//            }
//        }
//
//        // 判断src是否有效
//        if (src == null || src.length <= 0) {
//            return 0;
//        }
//
//        int srcCount = src.length;
//
//        // 可以拷贝的个数
//        int count = Math.min(srcCount, destCount);
//
//        // 清空缓存
//        mCacheBuffer = null;
//
//        // 判断是否需要缓存
//        if (srcCount > count) {
//            int cacheCount = srcCount - count;
//            mCacheBuffer = new byte[cacheCount];
//            System.arraycopy(src, count, mCacheBuffer, 0, cacheCount);
//        }
//
//        // 拷贝
//        System.arraycopy(src, 0, dest, off, count);
//        return count;
//    }
//}
