package com.scliang.core.media;

import com.scliang.core.media.IAudioPlayStateCallback;

interface IAudioPlayService {

    void registerPlayStateCallback(in IAudioPlayStateCallback callback);
    void unregisterPlayStateCallback(in IAudioPlayStateCallback callback);

    void play(in String fileName);
    void resumePlay();
    void pausePlay();
    void stopPlay();

    void seekPlay(in String fileName, in int position);

    boolean isPlaying();
    boolean isPlayPaused();

}
