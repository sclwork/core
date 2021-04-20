package com.scliang.core.media.object;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.URLEncoder;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/8.
 */
public class JPlayer {
    private MediaPlayer mPlayer;
    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;

    private OnPlayerStateChangedListener mOnPlayerStateChangedListener;
    private OnPlayPositionChangedListener mOnPlayPositionChangedListener;

    private Runnable mOnPrepareStartListener;
    private Runnable mOnStartListener;
    private Runnable mOnResumeListener;
    private Runnable mOnPauseListener;
    private Runnable mOnStopListener;
    private Runnable mOnReleaseListener;

    private String mFileName;
    private int mSeekPosition;

    public static final int STATE_IDLE    = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED  = 2;

    private int mState = STATE_IDLE;

    private PlayPositionHandler mPlayPositionHandler;

    public JPlayer(Context context, OnPlayerStateChangedListener listener, final Runnable needReleaseCallback) {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
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
                        pause();
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // nothing
                    }
                }
            };
        }

        mOnPlayerStateChangedListener = listener;

        setOnPlayPositionChangedListener(new OnPlayPositionChangedListener() {
            @Override
            public void onPlayPositionChanged(int position, int duration) {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Playing");
                        json.put("position", position);
                        json.put("duration", duration);
                        if (!TextUtils.isEmpty(mFileName)) {
                            json.put("data", URLEncoder.encode(mFileName, "utf-8"));
                        }
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        setOnPrepareStartListener(new Runnable() {
            @Override
            public void run() {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "PrepareStart");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Prepared");
                        if (!TextUtils.isEmpty(mFileName)) {
                            json.put("data", URLEncoder.encode(mFileName, "utf-8"));
                        }
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
                if (mSeekPosition > 0) {
                    seek(mSeekPosition);
                    mSeekPosition = 0;
                }
                start();
            }
        });
        setOnStartListener(new Runnable() {
            @Override
            public void run() {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Started");
                        json.put("duration", mPlayer.getDuration());
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        setOnResumeListener(new Runnable() {
            @Override
            public void run() {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Resumed");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        setOnPauseListener(new Runnable() {
            @Override
            public void run() {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Paused");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        setOnStopListener(new Runnable() {
            @Override
            public void run() {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Stoped");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "BufferingUpdated");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        json.put("percent", percent);
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        mPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "SeekCompleted");
                        json.put("position", mPlayer.getCurrentPosition());
                        json.put("duration", mPlayer.getDuration());
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Infoed");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        json.put("what", what);
                        json.put("extra", extra);
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
                return false;
            }
        });
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (needReleaseCallback != null) {
                    needReleaseCallback.run();
                }
                // 停止播放进度读取Handler
                releasePlayPositionHandler();
                // 通知播放完成
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Completed");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (needReleaseCallback != null) {
                    needReleaseCallback.run();
                }
                // 停止播放进度读取Handler
                releasePlayPositionHandler();
                // 通知播放错误
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Errored");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        json.put("what", what);
                        json.put("extra", extra);
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
                return false;
            }
        });
        setOnReleaseListener(new Runnable() {
            @Override
            public void run() {
                if (mOnPlayerStateChangedListener != null) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "Released");
                        json.put("data", TextUtils.isEmpty(mFileName) ? "null" : URLEncoder.encode(mFileName, "utf-8"));
                        String jsonParams = json.toString();
                        mOnPlayerStateChangedListener.onPlayerStateChanged(jsonParams);
                    } catch (JSONException | UnsupportedEncodingException ignored) {
                    }
                }
            }
        });
    }

    /**
     * 获得当前播放状态
     * @return STATE_IDLE | STATE_PLAYING | STATE_PAUSED
     */
    public int getState() {
        return mState;
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        mPlayer.setScreenOnWhilePlaying(screenOn);
    }

    public void setAudioStreamType(int streamType) {
        mPlayer.setAudioStreamType(streamType);
    }

    public void setSurface(Surface surface) {
        mPlayer.setSurface(surface);
    }

    public void setDataSource(String path) throws IOException,
            IllegalArgumentException, SecurityException, IllegalStateException {
        mFileName = path;
        mPlayer.setDataSource(path);
    }

    public int getVideoWidth() {
        return mPlayer.getVideoWidth();
    }

    public int getVideoHeight() {
        return mPlayer.getVideoHeight();
    }

    public int getDuration() throws IllegalStateException {
        return mPlayer.getDuration();
    }

    public int getCurrentPosition() throws IllegalStateException {
        return mPlayer.getCurrentPosition();
    }

    public void prepare() throws IOException {
        if (mOnPrepareStartListener != null) mOnPrepareStartListener.run();
        mPlayer.prepare();
    }

    public void prepareAsync() throws IllegalStateException {
        if (mOnPrepareStartListener != null) mOnPrepareStartListener.run();
        mPlayer.prepareAsync();
    }

    public void start() throws IllegalStateException {
        if (mState == STATE_IDLE) {
            // 请求音频播放焦点
            requestAudioFocus();
            // 回调状态
            if (mOnStartListener != null) mOnStartListener.run();
            // 启动播放进度读取Handler
            startPlayPositionHandler();
            // player start
            mPlayer.start();
            // 标注当前状态
            mState = STATE_PLAYING;
        }
    }

    public void resume() throws IllegalStateException {
        if (mState == STATE_PAUSED) {
            // 请求音频播放焦点
            requestAudioFocus();
            // 回调状态
            if (mOnResumeListener != null) mOnResumeListener.run();
            // 启动播放进度读取Handler
            startPlayPositionHandler();
            // player start
            mPlayer.start();
            // 标注当前状态
            mState = STATE_PLAYING;
        }
    }

    public void pause() throws IllegalStateException {
        if (mState == STATE_PLAYING) {
            // player pause
            mPlayer.pause();
            // 停止播放进度读取Handler
            releasePlayPositionHandler();
            // 放弃播放焦点
            abandonAudioFocus();
            // 回调状态
            if (mOnPauseListener != null) mOnPauseListener.run();
            // 标注当前状态
            mState = STATE_PAUSED;
        }
    }

    public void stop() throws IllegalStateException {
        if (mState == STATE_PLAYING || mState == STATE_PAUSED) {
            // player stop
            mPlayer.stop();
            // 停止播放进度读取Handler
            releasePlayPositionHandler();
            // 放弃播放焦点
            abandonAudioFocus();
            // 回调状态
            if (mOnStopListener != null) mOnStopListener.run();
            // 标注当前状态
            mState = STATE_IDLE;
        }
    }

    public void release() {
        // player release
        mPlayer.release();
        // 放弃播放焦点
        abandonAudioFocus();
        // 回调状态
        if (mOnReleaseListener != null) mOnReleaseListener.run();
        // 标注当前状态
        mState = STATE_IDLE;
    }

    public void seek(int position) throws IllegalStateException {
        // player seek
        mPlayer.seekTo(position);
    }

    public void setSeekPosition(int position) {
        mSeekPosition = position;
    }

    private void setOnPrepareStartListener(Runnable listener) {
        mOnPrepareStartListener = listener;
    }

    private void setOnStartListener(Runnable listener) {
        mOnStartListener = listener;
    }

    private void setOnResumeListener(Runnable listener) {
        mOnResumeListener = listener;
    }

    private void setOnPauseListener(Runnable listener) {
        mOnPauseListener = listener;
    }

    private void setOnStopListener(Runnable listener) {
        mOnStopListener = listener;
    }

    private void setOnReleaseListener(Runnable listener) {
        mOnReleaseListener = listener;
    }

    private void setOnPlayPositionChangedListener(OnPlayPositionChangedListener listener) {
        mOnPlayPositionChangedListener = listener;
        if (mPlayPositionHandler != null) {
            mPlayPositionHandler.setOnPlayPositionChangedListener(mOnPlayPositionChangedListener);
        }
    }

    private void startPlayPositionHandler() {
        releasePlayPositionHandler();
        HandlerThread thread = new HandlerThread("PlayPositionThread-" + System.currentTimeMillis());
        thread.start();
        mPlayPositionHandler = new PlayPositionHandler(thread.getLooper(), this);
        mPlayPositionHandler.setOnPlayPositionChangedListener(mOnPlayPositionChangedListener);
        mPlayPositionHandler.start();
    }

    private void releasePlayPositionHandler() {
        if (mPlayPositionHandler != null) {
            mPlayPositionHandler.exit();
            mPlayPositionHandler = null;
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




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    public interface OnPlayerStateChangedListener {
        void onPlayerStateChanged(String jsonParams);
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    public interface OnPlayPositionChangedListener {
        void onPlayPositionChanged(int position, int duration);
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private static class PlayPositionHandler extends Handler {
        private SoftReference<JPlayer> mPlayer;
        private boolean isExited;
        private OnPlayPositionChangedListener mOnPlayPositionChangedListener;

        private PlayPositionHandler(Looper looper, JPlayer player) {
            super(looper);
            mPlayer = new SoftReference<>(player);
        }

        private void setOnPlayPositionChangedListener(OnPlayPositionChangedListener listener) {
            mOnPlayPositionChangedListener = listener;
        }

        private void start() {
            isExited = false;
            sendEmptyMessage(100);
        }

        private void exit() {
            isExited = true;
            removeMessages(100);
            getLooper().quitSafely();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                // 循环读取播放器播放进度
                case 100: {
                    JPlayer player = mPlayer.get();
                    if (player != null && mOnPlayPositionChangedListener != null) {
                        try {
                            mOnPlayPositionChangedListener.onPlayPositionChanged(
                                player.getCurrentPosition(),
                                player.getDuration()
                            );
                        } catch (Exception e) {
                            exit();
                        }
                    }
                    // loop draw
                    if (!isExited) {
                        sendEmptyMessageDelayed(100, 16);
                    }
                } break;
            }
        }
    }
}
