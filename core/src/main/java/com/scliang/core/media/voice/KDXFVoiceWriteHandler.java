package com.scliang.core.media.voice;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.iflytek.cloud.SpeechRecognizer;

import java.util.LinkedList;
import java.util.Queue;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/5/9.
 */
public class KDXFVoiceWriteHandler extends Handler {
    private boolean isExit;
    private Queue<Buffer> mBufferQueue;
    private SpeechRecognizer mSpeechRecognizer;

    KDXFVoiceWriteHandler(Looper looper, SpeechRecognizer speechRecognizer) {
        super(looper);
        mSpeechRecognizer = speechRecognizer;
        isExit = false;
    }

//    private void addBufferWrite(BBuffer buffer) {
//        if (buffer != null) {
//            Message msg = obtainMessage(100);
//            msg.obj = buffer;
//            sendMessage(msg);
//        }
//    }

//    private void addBufferWrite(SBuffer buffer) {
//        if (buffer != null) {
//            Message msg = obtainMessage(200);
//            msg.obj = buffer;
//            sendMessage(msg);
//        }
//    }

    void start(Queue<Buffer> bufferQueue) {
        isExit = false;
        mBufferQueue = bufferQueue;
        removeCallbacksAndMessages(null);
        sendEmptyMessage(10);
    }

    void exit() {
        isExit = true;
        removeCallbacksAndMessages(null);
        sendEmptyMessage(0);
    }

//    void resume() {
//        sendEmptyMessage(10);
//    }
//
//    void pause() {
//        removeCallbacksAndMessages(null);
//    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {

            // 终止线程
            case 0: {
                getLooper().quitSafely();
            }
            break;

            // 启动读取循环
            case 10: {
                if (mBufferQueue != null) {
                    try {
                        synchronized (Voice.sVoiceBufferQueueSync) {
                            Buffer buffer = mBufferQueue.poll();
//                            Logger.d("KDXFVoiceWriteHandler", "read voice buffer queue: " +
//                                (buffer == null ? "null" : buffer.getClass().getSimpleName()) +
//                                " queue size: " + mBufferQueue.size());
                            if (buffer != null) {
                                if (buffer instanceof BBuffer) {
//                                    addBufferWrite((BBuffer) buffer);
                                    writeBBuffer((BBuffer) buffer);
                                } else if (buffer instanceof SBuffer) {
//                                    addBufferWrite((SBuffer) buffer);
                                    writeSBuffer((SBuffer) buffer);
                                }
                                if (!isExit && Voice.getInstance().isVoiceStarting()) {
                                    sendEmptyMessage(10);
                                }
                            } else {
                                if (!isExit && Voice.getInstance().isVoiceStarting()) {
                                    sendEmptyMessageDelayed(10, 100);
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (!isExit && Voice.getInstance().isVoiceStarting()) {
                            sendEmptyMessageDelayed(10, 100);
                        }
                    }
                }
            }
            break;

//            // 处理写入操作 - byte
//            case 100: {
//                final BBuffer buffer = (BBuffer) msg.obj;
//                if (buffer != null && buffer.mBuffer != null) {
//                    if (mSpeechRecognizer != null && mSpeechRecognizer.isListening()) {
//                        final byte[] dest = buffer.mBuffer;
//                        mSpeechRecognizer.writeAudio(dest, 0, dest.length);
//                    } else {
//                        if (mBufferQueue instanceof LinkedList) {
//                            ((LinkedList<Buffer>)mBufferQueue).addFirst(buffer);
//                        }
//                    }
//                }
//            } break;

//            // 处理写入操作 - short
//            case 200: {
//                final SBuffer buffer = (SBuffer) msg.obj;
//                if (buffer != null && buffer.mBuffer != null) {
//                    if (mSpeechRecognizer != null && mSpeechRecognizer.isListening()) {
//                        final short[] mSrc = buffer.mBuffer;
//                        final int count = mSrc.length;
//                        final byte[] dest = new byte[count << 1];
//                        for (int i = 0; i < count; i++) {
//                            dest[i * 2] = (byte) (mSrc[i] >> 0);
//                            dest[i * 2 + 1] = (byte) (mSrc[i] >> 8);
//                        }
//                        mSpeechRecognizer.writeAudio(dest, 0, dest.length);
//                    } else {
//                        if (mBufferQueue instanceof LinkedList) {
//                            ((LinkedList<Buffer>)mBufferQueue).addFirst(buffer);
//                        }
//                    }
//                }
//            } break;
        }
    }

    private void writeBBuffer(BBuffer buffer) {
        if (buffer.mBuffer != null) {
            if (mSpeechRecognizer != null && mSpeechRecognizer.isListening()) {
                final byte[] dest = buffer.mBuffer;
                mSpeechRecognizer.writeAudio(dest, 0, dest.length);
            } else {
                if (mBufferQueue instanceof LinkedList) {
                    ((LinkedList<Buffer>) mBufferQueue).addFirst(buffer);
//                    Logger.d("KDXFVoiceHandler", "BBuffer addFirst.");
                }
            }
        }
    }

    private void writeSBuffer(SBuffer buffer) {
        if (buffer.mBuffer != null) {
            if (mSpeechRecognizer != null && mSpeechRecognizer.isListening()) {
                final short[] mSrc = buffer.mBuffer;
                final int count = mSrc.length;
                final byte[] dest = new byte[count << 1];
                for (int i = 0; i < count; i++) {
                    dest[i * 2] = (byte) (mSrc[i]); // mSrc[i] >> 0
                    dest[i * 2 + 1] = (byte) (mSrc[i] >> 8);
                }
                mSpeechRecognizer.writeAudio(dest, 0, dest.length);
            } else {
                if (mBufferQueue instanceof LinkedList) {
                    ((LinkedList<Buffer>) mBufferQueue).addFirst(buffer);
//                    Logger.d("KDXFVoiceHandler", "SBuffer addFirst.");
                }
            }
        }
    }
}
