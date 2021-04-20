package com.scliang.core.media.audio;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import com.scliang.core.base.BaseApplication;
import com.scliang.core.base.Logger;
import com.scliang.core.media.IAudioPlayService;
import com.scliang.core.media.IAudioPlayStateCallback;
import com.scliang.core.media.object.JPlayer;
import com.scliang.core.media.object.JPlayerManager;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/9.
 */
public final class AudioPlayerManager {
    private AudioPlayerManager() {
    }

    private static class SingletonHolder {
        private static final AudioPlayerManager INSTANCE = new AudioPlayerManager();
    }

    public static AudioPlayerManager getInstance() {
        return SingletonHolder.INSTANCE;
    }


    // 播放进程 - 默认当前进程
    private AudioPlayProcess mAudioPlayProcess = AudioPlayProcess.CURRENT;


    // 连接远程播放服务后离开要处理的操作标示
    public static final int RUN_WHAT_PLAY   = 1;
    public static final int RUN_WHAT_RESUME = 2;
    public static final int RUN_WHAT_PAUSE  = 3;
    public static final int RUN_WHAT_STOP   = 4;
    public static final int RUN_WHAT_SEEK   = 5;

    // 播放状态改变标示
    public static final int WHAT_PLAY_STATE_CHANGED = 5;



    private final static List<Handler> sHandlers = Collections.synchronizedList(new LinkedList<Handler>());


    // 当前进程播放服务
    private JPlayerManager mJPlayer;

    // 远程播放服务
    private Class<? extends BaseAudioPlayService> _SERVICE_ = null;
    private IAudioPlayService mPlayService;
    private IAudioPlayStateCallback mPlayStateCallback;
    private ServiceConnection mServiceConnection;



    /**
     * 设置用于播放音频的服务
     * 如果没有设置，则默认使用UniversalAudioPlayService
     * @param service 可空，如果为空，则在当前进程中播放
     */
    public void setAudioPlayService(Class<? extends BaseAudioPlayService> service) {
        if (_SERVICE_ != null) {
            stop();
        }
        _SERVICE_ = service;
        // 检查当前进程播放服务
        if (mJPlayer != null) {
            mJPlayer.releasePlayer();
            mJPlayer = null;
        }
        // 更新播放进程类型
        if (_SERVICE_ == null) {
            mAudioPlayProcess = AudioPlayProcess.CURRENT;
        } else {
            mAudioPlayProcess = AudioPlayProcess.MEDIA;
        }
    }



    /*
     * 判断是否已经绑定到远程播放服务
     */
    private boolean isBindPlayService() {
        return mPlayService != null;
    }


    /*
     * 获得远程播放服务
     */
    private IAudioPlayService getPlayService() {
        return mPlayService;
    }


    public void register(Handler handler) {
        sHandlers.add(handler);
    }

    public void unregister(Handler handler) {
        sHandlers.remove(handler);
    }


    /**
     * 停止
     */
    public void stop() {
        // 满足当前进程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.CURRENT) {
            if (mJPlayer != null) {
                mJPlayer.releasePlayer();
            }
        }
        // 满足远程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.MEDIA) {
            unbindPlayService();
            stopPlayService();
        }
    }


    /**
     * 播放给定的音频文件
     * @param fileName 音频文件全路径
     */
    public void play(String fileName) {
        // 满足当前进程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.CURRENT) {
            BaseApplication application = BaseApplication.getApp();
            if (application == null) {
                return;
            }
            if (mJPlayer == null) {
                mJPlayer = new JPlayerManager();
            }
            mJPlayer.renewPlayer(application, fileName, new JPlayer.OnPlayerStateChangedListener() {
                @Override
                public void onPlayerStateChanged(String jsonParams) {
                    noticeAudioPlayStateChanged(jsonParams);
                }
            });
        }
        // 满足远程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.MEDIA) {
            if (isBindPlayService()) {
                playCore(fileName);
            } else {
                startBindPlayService(RUN_WHAT_PLAY, fileName);
            }
        }
    }

    /**
     * 继续播放当前音频文件
     */
    public void resumePlay() {
        // 满足当前进程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.CURRENT) {
            if (mJPlayer != null) {
                mJPlayer.resumePlayer();
            }
        }
        // 满足远程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.MEDIA) {
            if (isBindPlayService()) {
                resumePlayCore();
            } else {
                startBindPlayService(RUN_WHAT_RESUME);
            }
        }
    }

    /**
     * 暂停播放当前音频文件
     */
    public void pausePlay() {
        // 满足当前进程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.CURRENT) {
            if (mJPlayer != null) {
                mJPlayer.pausePlayer();
            }
        }
        // 满足远程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.MEDIA) {
            if (isBindPlayService()) {
                pausePlayCore();
            } else {
                startBindPlayService(RUN_WHAT_PAUSE);
            }
        }
    }

    /**
     * 停止播放当前音频文件
     */
    public void stopPlay() {
        // 满足当前进程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.CURRENT) {
            if (mJPlayer != null) {
                mJPlayer.releasePlayer();
            }
        }
        // 满足远程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.MEDIA) {
            if (isBindPlayService()) {
                stopPlayCore();
            } else {
                startBindPlayService(RUN_WHAT_STOP);
            }
        }
    }

    /**
     * Seek播放
     */
    public void seekPlay(String fileName, int position) {
        // 满足当前进程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.CURRENT) {
            if (mJPlayer != null) {
                mJPlayer.seekPlayer(fileName, position);
            }
        }
        // 满足远程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.MEDIA) {
            if (isBindPlayService()) {
                seekPlayCore(fileName, position);
            } else {
                SeekPlayArg arg = new SeekPlayArg();
                arg.position = position;
                startBindPlayService(RUN_WHAT_SEEK, arg);
            }
        }
    }

    /**
     * 判断是否正在播放
     */
    public boolean isPlaying() {
        // 满足当前进程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.CURRENT) {
            return mJPlayer != null && mJPlayer.isPlayerPlaying();
        }
        // 满足远程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.MEDIA) {
            return isBindPlayService() && isPlayingCore();
        }
        // else
        return false;
    }

    /**
     * 判断是否正已暂停播放
     */
    public boolean isPlayPaused() {
        // 满足当前进程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.CURRENT) {
            return mJPlayer != null && mJPlayer.isPlayerPaused();
        }
        // 满足远程播放服务
        if (mAudioPlayProcess == AudioPlayProcess.MEDIA) {
            return isBindPlayService() && isPlayPausedCore();
        }
        // else
        return false;
    }


    private void bindPlayService(final Runnable bindCallback) {
        final Context context = BaseApplication.getApp();
        if (context != null) {
            Intent service = new Intent(context, _SERVICE_);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
            if (isBindPlayService()) {
                if (bindCallback != null) {
                    bindCallback.run();
                }
            } else {
                mServiceConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                        Logger.d("BaseAudioPlayFragment", "onServiceConnected ...");
                        mPlayService = IAudioPlayService.Stub.asInterface(iBinder);
                        registerPlayStateCallback();
                        if (bindCallback != null) {
                            bindCallback.run();
                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {
                        Logger.d("BaseAudioPlayFragment", "onServiceDisconnected ...");
                        mPlayService = null;
                    }
                };
                context.bindService(service, mServiceConnection, Service.BIND_AUTO_CREATE);
            }
        }
    }

    private void unbindPlayService() {
        final Context context = BaseApplication.getApp();
        if (context != null && mServiceConnection != null) {
            unregisterPlayStateCallback();
            context.unbindService(mServiceConnection);
            mServiceConnection = null;
            mPlayService = null;
        }
    }

    private void stopPlayService() {
        final Context context = BaseApplication.getApp();
        if (context != null) {
            Intent service = new Intent(context, _SERVICE_);
            context.stopService(service);
        }
    }

    private void registerPlayStateCallback() {
        if (mPlayService != null) {
            try {
                mPlayStateCallback = new IAudioPlayStateCallback.Stub(){
                    @Override
                    public void onAudioPlayStateChanged(String jsonParams) throws RemoteException {
                        noticeAudioPlayStateChanged(jsonParams);
                    }
                };
                mPlayService.registerPlayStateCallback(mPlayStateCallback);
            } catch (RemoteException ignored) {
            }
        }
    }

    private void unregisterPlayStateCallback() {
        if (mPlayService != null && mPlayStateCallback != null) {
            try {
                mPlayService.unregisterPlayStateCallback(mPlayStateCallback);
            } catch (RemoteException ignored) {
            }
        }
    }

    private void noticeAudioPlayStateChanged(String jsonParams) {
        for (int i = 0; i < sHandlers.size(); i++) {
            Handler handler = sHandlers.get(i);
            if (handler != null) {
                Message msg = handler.obtainMessage(WHAT_PLAY_STATE_CHANGED);
                msg.obj = jsonParams;
                handler.sendMessage(msg);
            }
        }
    }

    private void playCore(String fileName) {
        IAudioPlayService service = getPlayService();
        if (service != null) {
            try {
                service.play(fileName);
            } catch (RemoteException ignored) {
            }
        }
    }

    private void resumePlayCore() {
        IAudioPlayService service = getPlayService();
        if (service != null) {
            try {
                service.resumePlay();
            } catch (RemoteException ignored) {
            }
        }
    }

    private void pausePlayCore() {
        IAudioPlayService service = getPlayService();
        if (service != null) {
            try {
                service.pausePlay();
            } catch (RemoteException ignored) {
            }
        }
    }

    private void stopPlayCore() {
        IAudioPlayService service = getPlayService();
        if (service != null) {
            try {
                service.stopPlay();
            } catch (RemoteException ignored) {
            }
        }
    }

    private void seekPlayCore(String fileName, int position) {
        IAudioPlayService service = getPlayService();
        if (service != null) {
            try {
                service.seekPlay(fileName, position);
            } catch (RemoteException ignored) {
            }
        }
    }

    private boolean isPlayingCore() {
        IAudioPlayService service = getPlayService();
        if (service != null) {
            try {
                return service.isPlaying();
            } catch (RemoteException ignored) {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isPlayPausedCore() {
        IAudioPlayService service = getPlayService();
        if (service != null) {
            try {
                return service.isPlayPaused();
            } catch (RemoteException ignored) {
                return false;
            }
        } else {
            return false;
        }
    }

    private void startBindPlayService(final int runWhat) {
        startBindPlayService(runWhat, null);
    }

    private void startBindPlayService(final int runWhat, final Object runArg) {
        bindPlayService(new Runnable() {
            @Override
            public void run() {
                // 运行What
                switch (runWhat) {
                    case RUN_WHAT_PLAY: {
                        play((String) runArg);
                    } break;
                    case RUN_WHAT_RESUME: {
                        resumePlay();
                    } break;
                    case RUN_WHAT_PAUSE: {
                        pausePlay();
                    } break;
                    case RUN_WHAT_STOP: {
                        stopPlay();
                    } break;
                    case RUN_WHAT_SEEK: {
                        SeekPlayArg arg = (SeekPlayArg) runArg;
                        seekPlay(arg.fileName, arg.position);
                    } break;
                }
            }
        });
    }




    //////////////////////////////////////////////
    //////////////////////////////////////////////
    private static class SeekPlayArg {
        public String fileName;
        public int position;
    }
}
