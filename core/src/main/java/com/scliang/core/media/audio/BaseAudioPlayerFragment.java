package com.scliang.core.media.audio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.Logger;

import java.lang.ref.SoftReference;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/8.
 */
public abstract class BaseAudioPlayerFragment
        extends BaseFragment implements AudioPlayerStateListener {

    // UI Play Handler
    protected UIPlayHandler mUIPlayHandler;

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        // UI Handler
        mUIPlayHandler = new UIPlayHandler(Looper.getMainLooper(), this);
        // Register AudioPlayService Handler
        AudioPlayerManager.getInstance().register(mUIPlayHandler);
    }

    @Override
    public void onDestroyView() {

        // Unregister AudioPlayService Handler
        AudioPlayerManager.getInstance().unregister(mUIPlayHandler);

        // 页面退出，停止播放
        stopPlay();

        super.onDestroyView();
    }

    @Override
    public void onAudioPlayerStateChanged(int what, int arg1, int arg2, Object obj) {
        Logger.d("BaseAudioPlayerFragment", "onAudioPlayerStateChanged what: " + what + "; obj:" + obj);
    }



    /**
     * 播放给定的音频文件
     * @param fileName 音频文件全路径
     */
    public void play(String fileName) {
        AudioPlayerManager.getInstance().play(fileName);
    }

    /**
     * 继续播放当前音频文件
     */
    public void resumePlay() {
        AudioPlayerManager.getInstance().resumePlay();
    }

    /**
     * 暂停播放当前音频文件
     */
    public void pausePlay() {
        AudioPlayerManager.getInstance().pausePlay();
    }

    /**
     * Seek播放当前音频文件
     */
    public void seekPlay(String fileName, int position) {
        AudioPlayerManager.getInstance().seekPlay(fileName, position);
    }

    /**
     * 停止播放当前音频文件
     */
    public void stopPlay() {
        if (AudioPlayerManager.getInstance().isPlaying() ||
                AudioPlayerManager.getInstance().isPlayPaused()) {
            AudioPlayerManager.getInstance().stopPlay();
        }
    }

    /**
     * 常亮屏幕
     */
    protected void acquireScreen() {
        View view = getView();
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        Window window = getActivity().getWindow();
                        if (window != null) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }
                }
            });
        }
    }

    /**
     * 取消常亮屏幕
     */
    protected void releaseScreen() {
        View view = getView();
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        Window window = getActivity().getWindow();
                        if (window != null) {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }
                }
            });
        }
    }


    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    private static class UIPlayHandler extends Handler {
        private SoftReference<AudioPlayerStateListener> mListener;

        UIPlayHandler(Looper looper, AudioPlayerStateListener listener) {
            super(looper);
            mListener = new SoftReference<>(listener);
        }

        @Override
        public void handleMessage(Message msg) {
            AudioPlayerStateListener listener = mListener.get();
            if (listener != null) {
                listener.onAudioPlayerStateChanged(
                        msg.what,
                        msg.arg1,
                        msg.arg2,
                        msg.obj
                );
            }
        }
    }
}
