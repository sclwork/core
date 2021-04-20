package com.scliang.core.media.audio;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;

import com.scliang.core.base.BaseApplication;
import com.scliang.core.media.voice.OnVoiceRecognizerListener;

import java.lang.ref.SoftReference;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/2.
 */
public abstract class BaseAudioRecorderFragment
        extends BaseAudioPlayerFragment implements AudioRecorderStateListener {

    // UI Record Handler
    protected UIRecordHandler mUIRecordHandler;

    protected abstract String generatePCMBufferFileName();

//    protected abstract String generateOSCdBFileName();

    protected abstract int generateRecordUnitPCMdBTime();
    protected abstract AudioRecorderManager.RecordVolumeAgcConfig generateRecordVolumeAgcConfig();

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        BaseApplication application = null;
        Activity activity = getActivity();
        if (activity != null) {
            Application app = activity.getApplication();
            if (app instanceof BaseApplication) {
                application = (BaseApplication) app;
            }
        }

        // UI Handler
        mUIRecordHandler = new UIRecordHandler(Looper.getMainLooper(), this);

        // 只能一个录音实例，所以这里需要先销毁音频录音管理器
        AudioRecorderManager.getInstance().destroy();
        // Register Audio Record Handler
        AudioRecorderManager.getInstance().register(mUIRecordHandler);
        // 初始化音频录音管理器
        if (application != null) {
            AudioRecorderManager.getInstance().init(application, generatePCMBufferFileName(),
              generateRecordUnitPCMdBTime(), generateRecordVolumeAgcConfig());
        }
    }

    @Override
    public void onDestroyView() {

        // Unregister Audio Record Handler
        AudioRecorderManager.getInstance().unregister(mUIRecordHandler);
        // 销毁音频录音管理器
        AudioRecorderManager.getInstance().destroy();

        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(String permission, boolean granted) {
        super.onRequestPermissionsResult(permission, granted);
        if (granted) {
            AudioRecorderManager.getInstance().checkPermissions();
        }
    }

    @Override
    public void onAudioRecorderStateChanged(int what, int arg1, int arg2, Object obj) {
//        Logger.d("BaseAudioRecorderFragment", "onAudioRecorderStateChanged what: " + what + "; obj:" + obj);
        // 得到权限后的一些操作
        if (what == AudioRecorderManager.WHAT_HAS_PERMISSIONS) {
            onUpdateAfterPermissions();
        }
    }

    // 得到权限后的一些操作
    protected void onUpdateAfterPermissions() {
    }

//    // 设置用于接收示波数据的Handler
//    protected void setOSCHandler(Handler handler) {
//        AudioRecorderManager.getInstance().setOSCHandler(handler);
//    }

//    // 设置用于传输音频数据到剪切View的Handler
//    protected void setCutHandler(Handler handler) {
//        AudioRecorderManager.getInstance().setCutHandler(handler);
//    }

    // 启动录音
    protected void startAudioRecord(String fileName) {
        AudioRecorderManager.getInstance().startAudioRecord(fileName);
    }

    // 启动录音
    protected void startAudioRecord(String fileName, OnVoiceRecognizerListener voiceRecognizerListener) {
        AudioRecorderManager.getInstance().startAudioRecord(fileName, voiceRecognizerListener);
    }

    // 继续录音
    protected void resumeAudioRecord() {
        AudioRecorderManager.getInstance().resumeAudioRecord();
    }

    // 暂停录音
    protected void pauseAudioRecord() {
        AudioRecorderManager.getInstance().pauseAudioRecord();
    }

    // 停止录音
    protected void stopAudioRecord() {
        AudioRecorderManager.getInstance().stopAudioRecord();
    }

    // 停止录音
    protected void stopAudioRecord(AudioRecorderManager.OnAudioRecorderStopListener stopListener) {
        AudioRecorderManager.getInstance().stopAudioRecord(stopListener);
    }

    // 停止录音
    protected void stopAudioRecord(boolean stopVoice) {
        AudioRecorderManager.getInstance().stopAudioRecord(stopVoice, null);
    }

    // 停止录音
    protected void stopAudioRecord(boolean stopVoice,
                                   AudioRecorderManager.OnAudioRecorderStopListener stopListener) {
        AudioRecorderManager.getInstance().stopAudioRecord(stopVoice, stopListener);
    }

//    // 设置剪切区域
//    protected void setAudioCutRegion(long start, long end) {
//        AudioRecorderManager.getInstance().setCutRegion(start, end);
//    }

//    // 开始处理PCM转Mp3
//    protected void startProcessToMp3() {
//        AudioRecorderManager.getInstance().startProcessToMp3();
//    }

    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    private static class UIRecordHandler extends Handler {
        private SoftReference<AudioRecorderStateListener> mListener;

        UIRecordHandler(Looper looper, AudioRecorderStateListener listener) {
            super(looper);
            mListener = new SoftReference<>(listener);
        }

        @Override
        public void handleMessage(Message msg) {
            AudioRecorderStateListener listener = mListener.get();
            if (listener != null) {
                listener.onAudioRecorderStateChanged(
                        msg.what,
                        msg.arg1,
                        msg.arg2,
                        msg.obj
                );
            }
        }
    }
}
