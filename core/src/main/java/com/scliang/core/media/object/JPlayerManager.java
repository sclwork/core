package com.scliang.core.media.object;

import android.content.Context;

import java.io.IOException;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/5/11.
 */
public final class JPlayerManager {
    JPlayer mPlayer;
    String mSeekFileName;
    int mSeekPosition;

    public JPlayerManager() {
    }

    public void renewPlayer(Context context, final String fileName,
                             JPlayer.OnPlayerStateChangedListener listener) {
        releasePlayer();
        try {
            mPlayer = new JPlayer(context, listener, new Runnable() {
                @Override
                public void run() {
                    releasePlayer();
                }
            });
            if (fileName.equals(mSeekFileName)) {
                if (mSeekPosition > 0) {
                    mPlayer.setSeekPosition(mSeekPosition);
                    mSeekPosition = 0;
                }
            } else {
                mSeekFileName = "";
                mSeekPosition = 0;
            }
            mPlayer.setDataSource(fileName);
            mPlayer.prepareAsync();
        } catch (IOException ignored) {
        }
    }

    public void resumePlayer() {
        if (mPlayer != null) {
            mPlayer.resume();
        }
    }

    public void pausePlayer() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    public void seekPlayer(String fileName, int position) {
        if (mPlayer != null) {
            mSeekFileName = "";
            mSeekPosition = 0;
            mPlayer.seek(position);
        } else {
            mSeekFileName = fileName;
            mSeekPosition = position;
        }
    }

    public boolean isPlayerPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    public boolean isPlayerPaused() {
        return mPlayer != null && mPlayer.getState() == JPlayer.STATE_PAUSED;
    }
}
