package com.scliang.core.media.video;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.danikula.videocache.HttpProxyCacheServer;
import com.scliang.core.R;
import com.scliang.core.base.BaseActivity;
import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.DataCompressUtils;
import com.scliang.core.base.Logger;
import com.scliang.core.media.object.JPlayer;
import com.scliang.core.ui.UIVideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/11.
 */
public abstract class BaseVideoPlayerFragment extends BaseFragment
    implements TextureView.SurfaceTextureListener {
  protected FrameLayout mGlobalMaskContainer;
  protected FrameLayout mContentContainer;
  protected UIVideoView mVideoView;

  // 当前播放的视频文件全路径
  protected String mCurrentFileName;
  protected JPlayer mPlayer;
  protected boolean isPortrait = false;
  private boolean isSurfaceTextureAvailable = false;

  // 在竖屏情况下是否显示Toolbar中的返回按钮
  protected abstract boolean onShowBackIconWithPortrait();
  // 在竖屏情况下给定的ToolbarTitle
  protected abstract String onGenerateToolbarTitleWithPortrait();
  // 当Resume时是否需要继续播放视频
  protected boolean onPageResumeCheckResumePlay() { return true; }
  // 当Pause时是否需要暂停播放视频
  protected boolean onPagePauseCheckPausePlay() { return true; }

  /**
   * 提供给子类用于填充视频视图以下的UI布局
   */
  protected View onCreateContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
    return null;
  }

  @Override
  protected final View onCreateViewHere(@NonNull LayoutInflater inflater,
                                        @Nullable ViewGroup container,
                                        @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_base_video_player, container, false);
    mGlobalMaskContainer = view.findViewById(R.id.video_global_mask_container);
    mContentContainer = view.findViewById(R.id.content_container);
    mVideoView = view.findViewById(R.id.video_view);

    final View opView = onCreateContentView(inflater, container, savedInstanceState);
    if (opView != null) {
      mContentContainer.removeAllViews();
      mContentContainer.addView(opView, new FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    return view;
  }

  @CallSuper
  @Override
  protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreatedHere(view, savedInstanceState);
    // 设置TextureView等回调监听
    if (mVideoView != null) {
      mVideoView.setSurfaceTextureListener(this);
    }
  }

  @CallSuper
  @Override
  public void onResume(Bundle args) {
    // 判断是否需要继续播放视频
    if (onPageResumeCheckResumePlay()) {
      checkPlayerOnResume();
    }
  }

  @CallSuper
  @Override
  public void onPause(Bundle args) {
    // 如果视频正在播放，此时需要将其暂停
    if (onPagePauseCheckPausePlay()) {
      checkPlayerOnPause();
    }
  }

  @CallSuper
  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    isSurfaceTextureAvailable = true;
  }

  @CallSuper
  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
  }

  @CallSuper
  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    isSurfaceTextureAvailable = false;
    stopPlay();
    return true;
  }

  @CallSuper
  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {
  }

  @CallSuper
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mVideoView != null) {
      mVideoView.setHeightChangable(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
    }
    updateUIWithOrientation(newConfig);
  }

  @Override
  public boolean onBackPressed() {
    if (isPortrait) {
      return super.onBackPressed();
    } else {
      final Activity activity = getActivity();
      if (activity != null) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }
      return true;
    }
  }

  @CallSuper
  protected void onVideoPlayStateChanged(String stateJson) {
    try {
      JSONObject obj = new JSONObject(stateJson);
      String type = obj.optString("type", "");

      // 视频准备好以后调整VideoSize
      if ("Prepared".equals(type)) {
        if (mVideoView != null && mPlayer != null) {
          mVideoView.updateVideoSize(
              mPlayer.getVideoWidth(),
              mPlayer.getVideoHeight());
        }
      }

      // 播放开始
      if ("Started".equals(type)) {
        acquireScreen();
      }
      // 继续播放
      else if ("Resumed".equals(type)) {
        acquireScreen();

      }
      // 暂停播放
      else if ("Paused".equals(type)) {
        releaseScreen();

      }
      // 视频播放完
      else if ("Completed".equals(type)) {
        releaseScreen();
      }
      // 视频播放错误
      else if ("Errored".equals(type)) {
        releaseScreen();
      }

    } catch (JSONException ignored) { }
  }




  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  public void setGlobalMaskView(View view) {
    setGlobalMaskView(view, null);
  }

  public void setGlobalMaskView(View view, FrameLayout.LayoutParams lp) {
    if (mGlobalMaskContainer != null) {
      mGlobalMaskContainer.removeAllViews();
      if (view != null) {
        mGlobalMaskContainer.addView(view, lp != null ? lp :
          new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
      }
    }
  }

  public void setVideoMaskView(View view) {
    setVideoMaskView(view, null);
  }

  public void setVideoMaskView(View view, FrameLayout.LayoutParams lp) {
    if (mVideoView != null) {
      mVideoView.setVideoMaskView(view, lp);
    }
  }

  /**
   * 播放给定的视频文件
   *
   * @param fileName 视频文件全路径
   */
  public void play(String fileName) {
    play(fileName, true);
  }

  /**
   * 播放给定的视频文件
   *
   * @param fileName 视频文件全路径
   * @param useCache 指定是否使用缓存
   */
  public void play(final String fileName, final boolean useCache) {
    String fName = fileName;
    // android9及其以上使用https
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        !TextUtils.isEmpty(fName) && fName.startsWith("http://")) {
      fName = fName.replaceFirst("http://", "https://");
    }

    // 使用缓存机制
    if (useCache) {
      // 设置缓存代理
      fName = getProxyUrl(fName);
      mCachePlaying = fName.startsWith("file");
    } else {
      mCachePlaying = false;
    }

    if (isSurfaceTextureAvailable &&
        !TextUtils.isEmpty(fName) &&
        !fName.equals(mCurrentFileName)) {
      renewPlayer(fName);
      mCurrentFileName = fName;
    }
  }

  /**
   * 继续播放当前视频文件
   */
  public void resumePlay() {
    resumePlayer();
  }

  /**
   * 暂停播放当视音频文件
   */
  public void pausePlay() {
    pausePlayer();
  }

  /**
   * 停止播放当视音频文件
   */
  public void stopPlay() {
    releasePlayer();
  }

  /**
   * 拖动进度条
   * @param position
   * @throws IllegalStateException
   */
  public void seek(int position) throws IllegalStateException {
    seekPlayer(position);
  }



  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  // Video Cache
  private static HttpProxyCacheServer sVideoCache;
  private boolean mCachePlaying;

  private static HttpProxyCacheServer getVideoCache(BaseVideoPlayerFragment fragment) {
    if (sVideoCache != null) {
      return sVideoCache;
    }

    if (fragment == null) {
      return null;
    }

    sVideoCache = fragment.newCacheProxy();
    return sVideoCache;
  }

  private String getProxyUrl(String url) {
    HttpProxyCacheServer proxy = getVideoCache(this);
    if (proxy == null) {
      return url;
    }

    return proxy.getProxyUrl(url);
  }

  protected HttpProxyCacheServer newCacheProxy() {
    Activity activity = getActivity();
    if (activity == null) {
      return null;
    }

    return new HttpProxyCacheServer.Builder(activity)
        .maxCacheSize(1024 * 1024 * 1024) // 1 Gb for cache
        .build();
  }

  public boolean isCachePlaying() {
    return mCachePlaying;
  }




  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  protected void updateUIWithOrientation(Configuration newConfig) {
    isPortrait = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE;
//    updateVideoContainerLayoutParams();
    if (mContentContainer != null) {
      mContentContainer.setVisibility(isPortrait ? View.VISIBLE : View.GONE);
    }
    if (isPortrait) {
      setToolbarType(BaseActivity.ToolbarType.TOP);
      setToolbarBackground(new ColorDrawable(
          getResources().getColor(R.color.color_base_bar)));
      setToolbarCenterTitle(onGenerateToolbarTitleWithPortrait());
      if (onShowBackIconWithPortrait()) {
        showToolbarNavigationIcon();
      } else {
        hideToolbarNavigationIcon();
      }
      // 取消全屏显示
      final Activity activity = getActivity();
      if (activity != null) {
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      }
    } else {
      setToolbarType(BaseActivity.ToolbarType.FLOAT);
      setToolbarBackground(new ColorDrawable(
          getResources().getColor(R.color.color_base_bar_hint)));
      setToolbarCenterTitle("");
      showToolbarNavigationIcon();
      // 全屏显示
      final Activity activity = getActivity();
      if (activity != null) {
        Window window = activity.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
      }
    }
  }

  private void checkPlayerOnResume() {
    if (mPlayer != null && mPlayer.getState() == JPlayer.STATE_PAUSED) {
      resumePlay();
    }
  }

  private void checkPlayerOnPause() {
    if (mPlayer != null && mPlayer.getState() == JPlayer.STATE_PLAYING) {
      pausePlay();
    }
  }


  private void renewPlayer(final String fileName) {
    releasePlayer();
    final Context context = getActivity();
    if (context != null) {
      try {
        mPlayer = new JPlayer(context, new JPlayer.OnPlayerStateChangedListener() {
          @Override
          public void onPlayerStateChanged(String jsonParams) {
            noticeVideoPlayStateChanged(jsonParams);
          }
        }, new Runnable() {
          @Override
          public void run() {
            onPlayerNeedReleaseCallback();
          }
        });
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if (mVideoView != null) {
          mPlayer.setSurface(new Surface(mVideoView.getSurfaceTexture()));
        }
        mPlayer.setScreenOnWhilePlaying(true);
        mPlayer.setDataSource(fileName);
        mPlayer.prepareAsync();
      } catch (IOException ignored) {
      }
    }
  }

  private void resumePlayer() {
    if (mPlayer != null) {
      mPlayer.resume();
    }
  }

  private void pausePlayer() {
    if (mPlayer != null) {
      mPlayer.pause();
    }
  }

  private void releasePlayer() {
    if (mPlayer != null) {
      mPlayer.stop();
      mPlayer.release();
      mPlayer = null;
      mCurrentFileName = null;
    }
  }

  private void seekPlayer(int position) throws IllegalStateException {
    if (mPlayer != null) {
      mPlayer.seek(position);
    }
  }

  private void noticeVideoPlayStateChanged(String jsonParams) {
    Logger.d("BaseVideoPlayerFragment", "onVideoPlayerStateChanged :" + jsonParams);
    onVideoPlayStateChanged(jsonParams);
  }

  // 播放器播放完成后，请求是否当地JPlayer
  protected void onPlayerNeedReleaseCallback() {
    releasePlayer();
  }




  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  /**
   * 常亮屏幕
   */
  protected void acquireScreen() {
    View view = getView();
    if (view != null) {
      view.post(new Runnable() {
        @Override
        public void run() {
          Window window = getActivity() == null ? null : getActivity().getWindow();
          if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
          Window window = getActivity() == null ? null : getActivity().getWindow();
          if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
        }
      });
    }
  }




  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  ///////////////////////////////////////////////////
  public interface OnVideoCoverImageListener {
    void onVideoCoverImageCompleted(Bitmap cover);
  }

  protected void getVideoCoverImage(final String url,
                                    final OnVideoCoverImageListener complete) {
    getVideoCoverImage(url, 500 * 1000, complete);
  }

  protected void getVideoCoverImage(final String url,
                                    final long timeUs,
                                    final OnVideoCoverImageListener complete) {
    if (TextUtils.isEmpty(url) || complete == null) {
      closeSimpleLoadDialog();
      return;
    }

    showSimpleLoadDialog_Fullscreen();

    DataCompressUtils.post(new Runnable() {
      @Override
      public void run() {
        try {
          MediaMetadataRetriever media = new MediaMetadataRetriever();
          if (url.startsWith("http")) {
            media.setDataSource(url, new HashMap<String, String>());
          } else {
            media.setDataSource(url);
          }
          Bitmap bitmap = timeUs < 0 ? media.getFrameAtTime() : media.getFrameAtTime(timeUs);
          complete.onVideoCoverImageCompleted(bitmap);
        } catch (Exception e) {
          closeSimpleLoadDialog();
        }
      }
    });
  }
}
