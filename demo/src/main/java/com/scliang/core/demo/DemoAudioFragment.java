package com.scliang.core.demo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scliang.core.media.audio.AudioPlayerManager;
import com.scliang.core.media.audio.AudioRecorderManager;
import com.scliang.core.media.audio.BaseAudioRecorderFragment;
import com.scliang.core.media.voice.OnVoiceRecognizerListener;
import com.scliang.core.media.voice.VoiceRecognizer;

import java.io.File;

/**
 * JCore
 * Created by ShangChuanliang
 * on 2017/10/5.
 */
public class DemoAudioFragment extends BaseAudioRecorderFragment {
//    private UIAudioCutView mCutView;
//    private UIAudioOSCSinView mOSCView;

    @Override
    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_audio, container, false);
//        view.findViewById(R.id.action_container).setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        setToolbarCenterTitle("录音-播放");

        final String audioFile = getActivity().getFilesDir().getAbsolutePath() + "/demo_audio.mp3";
        final String audioNSFile = getActivity().getFilesDir().getAbsolutePath() + "/demo_audio_ns.mp3";

        if (view != null) {
//            mCutView = view.findViewById(R.id.cut);
//            mOSCView = view.findViewById(R.id.osc);

//            mCutView.setVisibility(View.INVISIBLE);
//            mOSCView.setVisibility(View.VISIBLE);

            view.findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    if (mCutView != null) {
//                        setCutHandler(mCutView.getCutHandler());
//                    }
//                    if (mOSCView != null) {
////                        setOSCHandler(mOSCView.getOSCHandler());
////                    }
                    if (AudioRecorderManager.getInstance().checkPermissions(DemoAudioFragment.this)) {
                        startAudioRecord(audioFile, new OnVoiceRecognizerListener() {
                            @Override
                            public void onRecogStart(VoiceRecognizer recognizer) {

                            }

                            @Override
                            public void onRecognition(VoiceRecognizer recognizer, String word) {

                            }

                            @Override
                            public void onRecogStop(VoiceRecognizer recognizer) {

                            }

                            @Override
                            public void onRecogError(VoiceRecognizer recognizer, int code, String message) {

                            }

                            @Override
                            public void onRecogLast(VoiceRecognizer recognizer) {

                            }

                            @Override
                            public void onRecogLog(VoiceRecognizer recognizer, String log) {

                            }
                        });
                    }
//                    mCutView.setVisibility(View.INVISIBLE);
//                    mOSCView.setVisibility(View.VISIBLE);
                }
            });

            view.findViewById(R.id.resume).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    resumeAudioRecord();
                }
            });

            view.findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pauseAudioRecord();
                }
            });

            view.findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    mCutView.setVisibility(View.VISIBLE);
//                    mOSCView.setVisibility(View.INVISIBLE);
                    stopAudioRecord();
                }
            });

            view.findViewById(R.id.cut_action).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    setAudioCutRegion(6000, 15000);
                }
            });

            view.findViewById(R.id.process).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    startProcessToMp3();
                }
            });

            view.findViewById(R.id.start_play).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final File file = new File(audioFile);
                    if (file.exists()) {
                        play("file://" + audioFile);
                    }
                }
            });
            view.findViewById(R.id.start_play).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final File file = new File(audioNSFile);
                    if (file.exists()) {
                        play("file://" + audioNSFile);
                    }
                    return true;
                }
            });

            view.findViewById(R.id.resume_play).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    resumePlay();
                }
            });

            view.findViewById(R.id.pause_play).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pausePlay();
                }
            });

            view.findViewById(R.id.stop_play).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopPlay();
                }
            });

            view.findViewById(R.id.stop_play_service).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AudioPlayerManager.getInstance().stop();
                }
            });
        }
    }

    @Override
    protected void onUpdateAfterPermissions() {
        final View view = getView();
        if (view != null) {
            view.findViewById(R.id.action_container).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected String generatePCMBufferFileName() {
        return getActivity().getFilesDir().getAbsolutePath() + "/demo_pcm.pcm";
    }

    @Override
    protected int generateRecordUnitPCMdBTime() {
        return 80;
    }

    @Override
    protected AudioRecorderManager.RecordVolumeAgcConfig generateRecordVolumeAgcConfig() {
        return new AudioRecorderManager.RecordVolumeAgcConfig(
          0,
          255,
          20,
          true,
          3,
          false,
          3.0f,
          6.0f,
          -13f,
          true,
          true,
          2,
          ""
        );
    }

//    @Override
//    protected String generateOSCdBFileName() {
//        return "/sdcard/demo_pcm_osc.pcm";
//    }
}
