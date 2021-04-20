package com.scliang.core.demo;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.scliang.core.media.video.BaseVideoPlayerFragment;

import org.json.JSONObject;

/**
 * SCore Demo
 * Created by ShangChuanliang
 * on 2017/10/11.
 */
public class DemoVideoFragment extends BaseVideoPlayerFragment {
  private static final String sVideoUrl = "";
  private View mVideoOpAction;
  private SeekBar mSeekBar;

  @Override
  protected boolean onShowBackIconWithPortrait() {
    return false;
  }

  @Override
  protected String onGenerateToolbarTitleWithPortrait() {
    return "视频播放";
  }

//  @Override
//  protected String onGenerateVideoPathForThumbnail() {
//    return sVideoUrl;
//  }

  @Override
  protected View onCreateContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
    mVideoOpAction = inflater.inflate(R.layout.view_video_op_actions, container, false);
    mSeekBar = mVideoOpAction.findViewById(R.id.seek_bar);
    return inflater.inflate(R.layout.view_video_operator, container, false);
  }

  @Override
  protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreatedHere(view, savedInstanceState);
    setToolbarCenterTitle("视频播放");
    setVideoMaskView(mVideoOpAction);

    // 播放
    final View play = findViewById(R.id.play);
    if (play != null) play.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // play
        play(sVideoUrl);
      }
    });

    // 全屏播放
    final View fullScreen = findViewById(R.id.full_screen);
    if (fullScreen != null) fullScreen.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final Activity activity = getActivity();
        if (activity != null) {
          activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
      }
    });
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    super.onSurfaceTextureAvailable(surface, width, height);
  }

  @Override
  protected void onVideoPlayStateChanged(String stateJson) {
    super.onVideoPlayStateChanged(stateJson);
    try {
      JSONObject state = new JSONObject(stateJson);
      String type = state.getString("type");
      if ("Playing".equals(type)) {
        int position = Integer.parseInt(state.getString("position"));
        int duration = Integer.parseInt(state.getString("duration"));
        mSeekBar.setMax(duration);
        mSeekBar.setProgress(position);
      }
    } catch (Exception ignored) {
    }
  }
}
