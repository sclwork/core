package com.scliang.core.media.audio;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.scliang.core.base.Logger;
import com.scliang.core.media.IAudioPlayService;
import com.scliang.core.media.IAudioPlayStateCallback;
import com.scliang.core.media.object.JPlayer;
import com.scliang.core.media.object.JPlayerManager;

import java.lang.ref.SoftReference;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/8.
 */
public abstract class BaseAudioPlayService extends Service {
    private static final int    NID  = 101;

    private AudioPlayServiceImpl mAudioPlayServiceImpl;
    private JPlayerManager mJPlayer;

    // 播放状态回调
    private RemoteCallbackList<IAudioPlayStateCallback> mAudioPlayStateCallbacks;
    private int mAudioPlayStateCallbackCount = -1;
    private final byte[] mAudioPlayStateCallbacksSync = new byte[]{0};

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAudioPlayServiceImpl;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioPlayStateCallbacks = new RemoteCallbackList<>();
        mJPlayer = new JPlayerManager();
        updateForegroundNotification("{}");
        tryCreateAudioPlayServiceImpl();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mAudioPlayStateCallbacks != null) {
            mAudioPlayStateCallbacks.kill();
        }
        stopPlay();
        super.onDestroy();
    }

    private void tryCreateAudioPlayServiceImpl() {
        if (mAudioPlayServiceImpl == null) {
            mAudioPlayServiceImpl = new AudioPlayServiceImpl(this);
        }
    }

    private void updateForegroundNotification(String jsonParams) {
        Notification notification = onUpdateForegroundNotification(jsonParams);
        if (notification == null) {
            NotificationCompat.Builder builder =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            new NotificationCompat.Builder(this,
                                    generateDefaultNotificationChannelId()) :
                            new NotificationCompat.Builder(this);
            builder.setVibrate(null);
            notification = builder.build();
        }
        if (notification instanceof NullNotification) {
            // nothing : 返回一个不需要更新的NullNotification
        } else {
            startForeground(NID, notification);
            Logger.d("BaseAudioPlayService", "startForeground: " + notification.toString());
        }
    }

    protected abstract String generateDefaultNotificationChannelId();

    protected Notification onUpdateForegroundNotification(String jsonParams) {
        return null;
    }

    private void noticeAudioPlayStateChanged(String jsonParams) {
        if (mAudioPlayStateCallbacks != null) {
            int count = mAudioPlayStateCallbackCount;
            for (int i = 0; i < count; i++) {
                try {
                    IAudioPlayStateCallback callback = mAudioPlayStateCallbacks.getBroadcastItem(i);
                    if (callback != null) try {
                        callback.onAudioPlayStateChanged(jsonParams);
                    } catch (RemoteException ignored) {
                    }
                } catch (NullPointerException ignored) {
                }
            }
        }
    }


    ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////

    protected void registerPlayStateCallback(IAudioPlayStateCallback callback) {
        synchronized (mAudioPlayStateCallbacksSync) {
            if (mAudioPlayStateCallbackCount >= 0) {
                mAudioPlayStateCallbacks.finishBroadcast();
            }
            mAudioPlayStateCallbacks.register(callback);
            mAudioPlayStateCallbackCount = mAudioPlayStateCallbacks.beginBroadcast();
        }
    }

    protected void unregisterPlayStateCallback(IAudioPlayStateCallback callback) {
        synchronized (mAudioPlayStateCallbacksSync) {
            if (mAudioPlayStateCallbackCount >= 0) {
                mAudioPlayStateCallbacks.finishBroadcast();
            }
            mAudioPlayStateCallbacks.unregister(callback);
            mAudioPlayStateCallbackCount = mAudioPlayStateCallbacks.beginBroadcast();
        }
    }

    /*
     * 播放给定的音频文件
     * @param fileName 音频文件全路径
     */
    protected void play(String fileName) {
        mJPlayer.renewPlayer(this, fileName, new JPlayer.OnPlayerStateChangedListener() {
            @Override
            public void onPlayerStateChanged(String jsonParams) {
                updateForegroundNotification(jsonParams);
                noticeAudioPlayStateChanged(jsonParams);
            }
        });
    }

    /*
     * 继续播放当前音频文件
     */
    protected void resumePlay() {
        mJPlayer.resumePlayer();
    }

    /*
     * 暂停播放当前音频文件
     */
    protected void pausePlay() {
        mJPlayer.pausePlayer();
    }

    /*
     * 停止播放当前音频文件
     */
    protected void stopPlay() {
        mJPlayer.releasePlayer();
    }

    protected void seekPlay(String fileName, int position) {
        mJPlayer.seekPlayer(fileName, position);
    }

    /*
     * 判断是否正在播放
     */
    protected boolean isPlaying() {
        return mJPlayer.isPlayerPlaying();
    }

    /*
     * 判断是否已暂停播放
     */
    protected boolean isPlayPaused() {
        return mJPlayer.isPlayerPaused();
    }


    ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////
    private static class AudioPlayServiceImpl extends IAudioPlayService.Stub {
        private SoftReference<BaseAudioPlayService> mAudioPlayService;

        AudioPlayServiceImpl(BaseAudioPlayService service) {
            mAudioPlayService = new SoftReference<>(service);
        }

        @Override
        public void registerPlayStateCallback(IAudioPlayStateCallback callback) throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            if (service != null) {
                service.registerPlayStateCallback(callback);
            }
        }

        @Override
        public void unregisterPlayStateCallback(IAudioPlayStateCallback callback) throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            if (service != null) {
                service.unregisterPlayStateCallback(callback);
            }
        }

        @Override
        public void play(String fileName) throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            if (service != null) {
                service.play(fileName);
            }
        }

        @Override
        public void resumePlay() throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            if (service != null) {
                service.resumePlay();
            }
        }

        @Override
        public void pausePlay() throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            if (service != null) {
                service.pausePlay();
            }
        }

        @Override
        public void stopPlay() throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            if (service != null) {
                service.stopPlay();
            }
        }

        @Override
        public void seekPlay(String fileName, int position) throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            if (service != null) {
                service.seekPlay(fileName, position);
            }
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            return service != null && service.isPlaying();
        }

        @Override
        public boolean isPlayPaused() throws RemoteException {
            BaseAudioPlayService service = mAudioPlayService.get();
            return service != null && service.isPlayPaused();
        }
    }


    ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////
    ///////////////////////////////////////////////////
    public static final class NullNotification extends Notification {}
}
