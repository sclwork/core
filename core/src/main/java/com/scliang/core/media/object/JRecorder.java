package com.scliang.core.media.object;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/4/3.
 */
public class JRecorder {
    private AudioRecord mRecorder;
    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private StateChangedListener mStateChangedListener;

    public interface StateChangedListener {
        void onLossAudioFocus();
    }

    public JRecorder(Context context, int audioSource, int sampleRateInHz, int channelConfig, int audioFormat,
              int bufferSizeInBytes) throws IllegalArgumentException {
        if (mRecorder == null) {
            mRecorder = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        }
        if (mAudioManager == null && context != null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        if (mOnAudioFocusChangeListener == null) {
            mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS/* ||
                            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT*/) {
                        if (mStateChangedListener != null) {
                            mStateChangedListener.onLossAudioFocus();
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // nothing
                    }
                }
            };
        }
    }

    public void setStateChangedListener(StateChangedListener listener) {
        mStateChangedListener = listener;
    }

    public int setPositionNotificationPeriod(int periodInFrames) {
        if (mRecorder != null) {
            return mRecorder.setPositionNotificationPeriod(periodInFrames);
        } else {
            return AudioRecord.ERROR_INVALID_OPERATION;
        }
    }

    public void setRecordPositionUpdateListener(AudioRecord.OnRecordPositionUpdateListener listener, Handler handler) {
        if (mRecorder != null) {
            mRecorder.setRecordPositionUpdateListener(listener, handler);
        }
    }

    public void startRecording() throws IllegalStateException {
        if (mRecorder != null) {
            // 请求音频播放焦点
            requestAudioFocus();
            // 启动录音
            mRecorder.startRecording();
        }
    }

    public void stop() throws IllegalStateException {
        if (mRecorder != null) {
            // 停止录音
            mRecorder.stop();
            // 放弃播放焦点
            abandonAudioFocus();
        }
    }

    public void release() {
        if (mRecorder != null) {
            mRecorder.release();
        }
    }

    public int read(@NonNull short[] audioData, int offsetInShorts, int sizeInShorts) {
        if (mRecorder != null) {
            return mRecorder.read(audioData, offsetInShorts, sizeInShorts);
        } else {
            return AudioRecord.ERROR_INVALID_OPERATION;
        }
    }

    public int read(@NonNull short[] audioData, int offsetInShorts, int sizeInShorts,
                    boolean isBlocking) {
        if (mRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return mRecorder.read(audioData, offsetInShorts, sizeInShorts,
                    isBlocking ? AudioRecord.READ_BLOCKING : AudioRecord.READ_NON_BLOCKING);
            } else {
                return mRecorder.read(audioData, offsetInShorts, sizeInShorts);
            }
        } else {
            return AudioRecord.ERROR_INVALID_OPERATION;
        }
    }

    // 请求音频播放焦点
    private void requestAudioFocus() {
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                    AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    // 放弃播放焦点
    private void abandonAudioFocus() {
        if (mAudioManager != null && mOnAudioFocusChangeListener != null) {
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        }
    }
}
