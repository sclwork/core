package com.scliang.core.media.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.scliang.core.base.BaseActivity;
import com.scliang.core.base.BaseApplication;
import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.Logger;
import com.scliang.core.base.OnCheckPermissionImpl;
import com.scliang.core.base.Permission;
import com.scliang.core.base.rsa.Base64Utils;
import com.scliang.core.base.rsa.RSAProvider;
import com.scliang.core.media.IMediaNUtils;
import com.scliang.core.media.MediaNUtils3;
import com.scliang.core.media.MediaNUtils4;
import com.scliang.core.media.object.JRecorder;
import com.scliang.core.media.MediaNUtils;
import com.scliang.core.media.object.PCMBuffer;
import com.scliang.core.media.voice.OnVoiceRecognizerListener;
import com.scliang.core.media.voice.Voice;
import com.scliang.core.media.voice.VoiceRecognizer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/18.
 */
public final class AudioRecorderManager
    implements AudioRecorderStateListener, JRecorder.StateChangedListener {
  private AudioRecorderManager() {
  }

  private static class SingletonHolder {
    private static final AudioRecorderManager INSTANCE = new AudioRecorderManager();
  }

  public static AudioRecorderManager getInstance() {
    return SingletonHolder.INSTANCE;
  }


  private final static List<Handler> sHandlers =
    Collections.synchronizedList(new LinkedList<>());
  private OnVoiceRecognizerListener mAgainOnVoiceRecognizerListener;
  private final static List<SoftReference<OnAudioRecorderStopListener>> sOnAudioRecorderStopListeners =
    Collections.synchronizedList(new LinkedList<>());

  // ????????????
  public final static int STATE_IDLE = 0;
  public final static int STATE_RECORDING = 1;
  public final static int STATE_RECORD_PAUSED = 2;
  public final static int STATE_RECORD_STOPED = 7;

  // ????????????????????????
  public static final int WHAT_RECORD_STATE_CHANGED = 3;
  // ????????????????????????
  public static final int WHAT_PROCESS_STATE_CHANGED = 4;

  // ????????????????????????
  public static final int WHAT_RECORD_dB_CHANGED = 5;
  public static final int WHAT_RECORD_dB_SO_HIGH = 51;
  public static final int WHAT_RECORD_ERROR_INFO = 52;

  // ?????????????????????????????????????????????
  public static final int WHAT_HEADSET_PLUG_CHANGED = 6;

  // Has Permissions
  public final static int WHAT_HAS_PERMISSIONS = 6666;

  // ???????????????
  private final static int sAudioSourceMic = MediaRecorder.AudioSource.MIC;
  private final static int sAudioSourceVcm = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
  // ????????????????????????44100??????????????????????????????????????????????????? 22050???16000???11025
  private final static int sDefaultSampleRateInHz = 44100;
  // ????????????SDK???????????????16000
  private final static int sKDXFSampleRateInHz = 16000;
  private static int sSampleRateInHz = sDefaultSampleRateInHz;
  // ??????????????????????????????CHANNEL_IN_STEREO???????????????CHANNEL_IN_MONO????????????
  // ?????????????????????AudioRecordHandler.calculatePCM???????????????PCM???????????????
  // ???????????????????????????????????????????????????AudioRecordHandler.calculatePCM?????????
  private final static int sChannelConfig = AudioFormat.CHANNEL_IN_MONO;
  // ??????????????????:PCM 16???????????????????????????????????????PCM 8???????????????????????????????????????????????????
  // ?????????????????????AudioRecordHandler.calculatePCM???????????????PCM???????????????
  // ???????????????????????????????????????????????????AudioRecordHandler.calculatePCM?????????
  private final static int sAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
  // ???????????????????????????160????????????????????????
  private static final int FRAME_COUNT = 160;
  // ??????MP3?????????
  public final static int BIT_RATE = 128;
  // Audio Buffer Size
  private static int sPCMBufferSize = 0;
  // Audio Buffer
  private static short[] sPCMBuffer;
  // ????????????????????????PCM??????
  private static String sPCMBufferFileName;

  // Application
  private SoftReference<BaseApplication> mApp;
  // Notice Handler
  private NoticeHandler mNoticeHandler;
  // Audio Record Handler
  private AudioRecordHandler mAudioRecordHandler;
  // PCM Buffer Write Handler
  private WritePCMHandler mWritePCMHandler;
  // PCM Calculate Handler
  private CalculatePCMHandler mCalculatePCMHandler;

  // Current State
  private int mState = STATE_IDLE;

  // Check CPU ABI Support
  private boolean isCpuAbiSupport;
  private static IMediaNUtils sMediaNUtils;

  // ???????????? - VolumeAdjust
  private RecordVolumeAgcConfig mWebRTCAgcConfig;

  // ?????????????????????
  private boolean isInited;

  // ??????????????????????????????????????????
  private boolean mOpable;

  // ????????????????????????????????? - ????????????????????????
  private HeadsetPlugReceiver mHeadsetPlugReceiver;
  // ????????????????????????????????????
  // 0:?????????????????????
  // 1:?????????????????????
  // 2:?????????????????????
  private int mHeadsetPlugType = 0;

  // ???????????????: 44100??????????????????????????????????????????????????? 22050???16000???11025; ????????????SDK???????????????16000
  private void updateSampleRateInHz(int sampleRateInHz) {
    sSampleRateInHz = sampleRateInHz;
    // ????????????Buffer??????Size
    int minBufferSize = AudioRecord.getMinBufferSize(
        sSampleRateInHz,
        sChannelConfig,
        sAudioFormat
    );
    // Audio Buffer Size
    int tmpSize = sSampleRateInHz / 10;
    sPCMBufferSize = Math.max(tmpSize, minBufferSize);
    // Audio Buffer
    sPCMBuffer = new short[sPCMBufferSize];
  }

  public void register(Handler handler) {
    sHandlers.add(handler);
  }

  public void unregister(Handler handler) {
    sHandlers.remove(handler);
  }

  /**
   * ??????????????????
   *
   * @return STATE_IDLE | STATE_RECORDING | STATE_RECORD_PAUSED
   */
  public int getState() {
    return mState;
  }

  /**
   * ??????????????????????????????
   */
  public void init(BaseApplication application, String pCMBufferFileName,
                   int pcmUnitTimeIntervalInMillisecond,
                   RecordVolumeAgcConfig webRTCAgcConfig) {
    mApp = new SoftReference<>(application);
    final Context context = application.getApplicationContext();

    // ?????????????????????
    if (!isInited) {
      sPCMBufferFileName = pCMBufferFileName;
      mWebRTCAgcConfig = webRTCAgcConfig;

      // ?????????????????????????????????
      if (mHeadsetPlugReceiver == null) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        mHeadsetPlugReceiver = new HeadsetPlugReceiver(new Runnable() {
          @Override
          public void run() {
            // ?????????????????????????????????
            checkHeadsetPlugState();
            // ??????HeadsetPlugConfigs?????????
            notifyRecordConfigs();
          }
        });
        application.registerReceiver(mHeadsetPlugReceiver, filter);
      }

      // WebRTC????????????CPU??????['arm64-v8a']
      // ????????????????????????libjmedia2.so?????????????????????????????????
      // ?????????????????????isCpuAbiSupport???????????????????????????????????????isCpuAbiSupport????????????
      checkCpuAbiSupport();

      // Notice Handler
      mNoticeHandler = new NoticeHandler(Looper.getMainLooper(), this);

      // ?????????????????????????????????
      checkHeadsetPlugState();

      // ??????PCMBuffer??????Thread
      final HandlerThread writeThread = new HandlerThread("PCMBufferWriteThread-" + System.currentTimeMillis());
      writeThread.start();
      mWritePCMHandler = new WritePCMHandler(writeThread.getLooper(),
        pCMBufferFileName, mNoticeHandler, isCpuAbiSupport);

      // ??????PCM??????Handler
      final HandlerThread calculateThread = new HandlerThread("CalculatePCMThread-" + System.currentTimeMillis());
      calculateThread.start();
      mCalculatePCMHandler = new CalculatePCMHandler(calculateThread.getLooper(),
          pcmUnitTimeIntervalInMillisecond, mWritePCMHandler, mNoticeHandler);

      // ?????????????????????Thread
      final HandlerThread recordThread = new HandlerThread("AudioRecordThread-" + System.currentTimeMillis());
      recordThread.start();
      mAudioRecordHandler = new AudioRecordHandler(context, recordThread.getLooper(),
          mNoticeHandler, mWritePCMHandler, mCalculatePCMHandler, this,
          isCpuAbiSupport);

      // ??????HeadsetPlugConfigs?????????
      notifyRecordConfigs();

      // ???????????????????????????
      isInited = true;
    }
  }

  /**
   * ???????????????????????????
   */
  public void destroy() {

    // ??????????????????
    if (isInited) {

      // ??????????????????????????????????????????
      unregisterHeadsetPlugReceiver();

      // ????????????????????????
      Voice.getInstance().stop();

      // ??????????????????
      stopAudioRecord();

      // ??????????????????Thread
      exitAudioRecord();

      // ??????PCMBuffer??????Handler
      exitPCMBufferWriteHandler();

      // ??????PCM??????Handler
      exitCalculatePCMHandler();

//      // ????????????
//      destroyAudioApm();
    }

    // ???????????????????????????
    isInited = false;
  }

  @Override
  public void onLossAudioFocus() {
//    // ??????????????????
//    stopAudioRecord();
    // notice
    if (mNoticeHandler != null) {
      mNoticeHandler.sendEmptyMessage(700);
    }
  }

  @Override
  public void onAudioRecorderStateChanged(int what, int arg1, int arg2, Object obj) {
    switch (what) {

      // ????????????
      case 100:
      // ????????????
      case 200: {
        mState = STATE_RECORDING;
      }
      break;

      // ????????????
      case 300: {
        mState = STATE_RECORD_PAUSED;
      }
      break;

      // ????????????
      case 999: {
        mState = STATE_RECORD_STOPED;
      }
      break;

      // ??????mp3??????????????????
      case 888: {
        mState = STATE_IDLE;
        // ???????????????????????????
        for (SoftReference<OnAudioRecorderStopListener> sl : sOnAudioRecorderStopListeners) {
          OnAudioRecorderStopListener listener = sl == null ? null : sl.get();
          if (listener != null) listener.onAudioRecorderStoped();
        }
        // ???????????????????????????????????????
        sOnAudioRecorderStopListeners.clear();
        // ????????????????????????
        if (mWebRTCAgcConfig != null && mWebRTCAgcConfig.isVoiceNSOpen()) {
          // ????????????
          destroyAudioApm();
        }
      }
      break;
    }
  }

  // ???????????????????????????
  private OnVoiceRecognizerListener wrapOnVoiceRecognizerListener(final OnVoiceRecognizerListener listener) {
    return new OnVoiceRecognizerListener() {
      @Override
      public void onRecogStart(VoiceRecognizer recognizer) {
        if (listener != null) listener.onRecogStart(recognizer);
      }

      @Override
      public void onRecognition(VoiceRecognizer recognizer, String word) {
        if (listener != null) listener.onRecognition(recognizer, word);
      }

      @Override
      public void onRecogStop(VoiceRecognizer recognizer) {
        if (listener != null) listener.onRecogStop(recognizer);
      }

      @Override
      public void onRecogError(VoiceRecognizer recognizer, int code, String message) {
        if (listener != null) listener.onRecogError(recognizer, code, message);
      }

      @Override
      public void onRecogLast(VoiceRecognizer recognizer) {
        if (listener != null) listener.onRecogLast(recognizer);
      }

      @Override
      public void onRecogLog(VoiceRecognizer recognizer, String log) {
        if (mWritePCMHandler != null) mWritePCMHandler.addVoiceRecogLog(log);
        if (listener != null) listener.onRecogLog(recognizer, log);
      }
    };
  }

  // ????????????
  public void startAudioRecord(String fileName) {
    startAudioRecord(fileName, null);
  }

  // ????????????
  public void startAudioRecord(final String fileName,
                               OnVoiceRecognizerListener voiceRecognizerListener) {
    if (mOpable && mState == STATE_IDLE) {

      // ????????????????????????????????????SDK???????????????16000
      // ?????????????????????????????????????????????SDK??????????????????????????????44100
      if (voiceRecognizerListener != null) {
        updateSampleRateInHz(sKDXFSampleRateInHz);
      } else {
        updateSampleRateInHz(sDefaultSampleRateInHz);
      }

      // ????????????????????????
      if (mWebRTCAgcConfig != null && mWebRTCAgcConfig.isVoiceNSOpen()) {
        // ????????????
        destroyAudioApm();
        // ???????????????
        initAudioApm(sSampleRateInHz, mWebRTCAgcConfig.getVoiceNSMode());
      }

      // ??????PCM??????Handler
      resetCalculatePCMHandler();

      // ??????????????????????????????????????????
      if (voiceRecognizerListener != null) {
        // ????????????????????????
        Voice.getInstance().setOnVoiceRecognizerListener(
          wrapOnVoiceRecognizerListener(voiceRecognizerListener));
        Voice.getInstance().start();
      }

      // ????????????
      if (mAudioRecordHandler != null) {
        mAudioRecordHandler.start(fileName);
      }
    }
  }

  // ????????????
  public void resumeAudioRecord() {
    if (mOpable && mState == STATE_RECORD_PAUSED) {
      if (mAudioRecordHandler != null) {
        mAudioRecordHandler.resume();
      }
    }
  }

  // ????????????
  public void pauseAudioRecord() {
    if (mOpable && mState == STATE_RECORDING) {
      if (mAudioRecordHandler != null) {
        mAudioRecordHandler.pause();
      }
    }
  }

  // ????????????
  public void stopAudioRecord() {
    stopAudioRecord(true, null);
  }

  // ????????????
  public void stopAudioRecord(OnAudioRecorderStopListener stopListener) {
    stopAudioRecord(true, stopListener);
  }

  // ????????????
  public void stopAudioRecord(boolean stopVoice, OnAudioRecorderStopListener stopListener) {
    if (stopListener != null) {
      sOnAudioRecorderStopListeners.add(new SoftReference<>(stopListener));
    }
    if (mOpable && (mState == STATE_RECORDING || mState == STATE_RECORD_PAUSED)) {
      if (mAudioRecordHandler != null) {
        mAudioRecordHandler.stop();
      }
      if (stopVoice) {
        // ????????????????????????
        Voice.getInstance().setOnVoiceRecognizerListener(null);
        Voice.getInstance().stop();
      } else {
        Voice.getInstance().stopListening();
      }
    }
  }

  // ??????????????????
  private void exitAudioRecord() {
    if (mOpable && mAudioRecordHandler != null) {
      mAudioRecordHandler.exit();
    }
  }

  // ????????????????????????
  public boolean isRecording() {
    return mState == STATE_RECORDING || mState == STATE_RECORD_PAUSED;
  }

  // ??????Wav????????????????????????
  public int createWavFileAndWritePCMData(String file, int sampleRate,
                                          byte[] data, int size) {
    if (isCpuAbiSupport) {
      return sMediaNUtils.createWavFileAndWritePCMData(
        file, sampleRate, data, size);
    } else {
      return 0;
    }
  }

  // ??????Mp3????????????????????????
  public int createMp3FileAndWritePCMData(String file, int sampleRate,
                                          byte[] data, int size) {
    if (isCpuAbiSupport) {
      return sMediaNUtils.createMp3FileAndWritePCMData(
              file, sampleRate, data, size);
    } else {
      return 0;
    }
  }

  // ?????????????????????Wav/Mp3??????
  public void processWavOrMp3FileWebRTC(String inFile, String outFile, int mode) {
    if (isCpuAbiSupport) {
      sMediaNUtils.webrtcFileProcess(inFile, outFile, mode);
    }
  }

  // ?????????WebRTC????????????
  public void initWebRTCAgc(int sample, int mode) {
    // ????????????
    destroyAudioApm();
    // ???????????????
    initAudioApm(sample, mode);
  }

  // ??????WebRTC????????????
  public void destroyWebRTCAgc() {
    // ????????????
    destroyAudioApm();
  }

  // WebRTC????????????
  public void processWebRTCAgc(byte[] buffer, int size) {
    if (isCpuAbiSupport) {
      short[] sBuf = new short[size >> 1];
      for (int i = 0; i < sBuf.length; i++) {
        sBuf[i] = (short) ((buffer[i * 2] & 0xff) +
                          ((buffer[i * 2 + 1] & 0xff) << 8));
      }

      AudioRecordHandler.processWebRTCAgc(sMediaNUtils, sBuf, sBuf.length);

      int count = sBuf.length;
      for (int i = 0; i < count; i++) {
        buffer[i * 2] = (byte) (sBuf[i]); // sBuf[i] >> 0
        buffer[i * 2 + 1] = (byte) (sBuf[i] >> 8);
      }
    }
  }

  // WebRTC????????????
  public void processWebRTCAgc(short[] buffer, int size) {
    if (isCpuAbiSupport) {
      AudioRecordHandler.processWebRTCAgc(sMediaNUtils, buffer, size);
    }
  }

  // ??????WebRTCAgcConfig - ??????????????????
  public void updateWebRTCAgcConfig(RecordVolumeAgcConfig config) {
    mWebRTCAgcConfig = config;
    // ??????RecordConfigs?????????
    notifyRecordConfigs();
  }

  // VolumeAdjustOpen??????
  public void switchPCMAdjustVolumeOpen(boolean open) {
    if (mWebRTCAgcConfig != null) {
      mWebRTCAgcConfig.mPCMVolumeAdjustOpen = open;
      // ??????RecordConfigs?????????
      notifyRecordConfigs();
    }
  }

  // VoiceNS??????
  public void switchVoiceNSOpen(boolean open) {
    if (mWebRTCAgcConfig != null) {
      mWebRTCAgcConfig.mVoiceNSOpen = open;
      // ??????RecordConfigs?????????
      notifyRecordConfigs();
    }
  }

  // ??????PCMAdjustVolumeFactor
  public void updatePCMAdjustVolumeFactor(float normalFactor, float headsetFactor) {
    if (mWebRTCAgcConfig != null) {
      mWebRTCAgcConfig.mPCMAdjustVolumeNormalFactor = Math.max(Math.min(normalFactor, 10f), 1f);
      mWebRTCAgcConfig.mPCMAdjustVolumeHeadsetFactor = Math.max(Math.min(headsetFactor, 10f), 1f);
      // ??????RecordConfigs?????????
      notifyRecordConfigs();
    }
  }

  // PCM????????????
  public void pcmAdjustVolume(byte[] buffer, int size, float factor) {
    short[] sBuf = new short[size >> 1];
    for (int i = 0; i < sBuf.length; i++) {
      sBuf[i] = (short) ((buffer[i * 2] & 0xff) +
                        ((buffer[i * 2 + 1] & 0xff) << 8));
    }

    MediaNUtils.pcmAdjustVolume(sBuf, sBuf.length, factor);

    int count = sBuf.length;
    for (int i = 0; i < count; i++) {
      buffer[i * 2] = (byte) (sBuf[i]); // sBuf[i] >> 0
      buffer[i * 2 + 1] = (byte) (sBuf[i] >> 8);
    }
  }

  // PCM????????????
  public void pcmAdjustVolume(short[] buffer, int size, float factor) {
    MediaNUtils.pcmAdjustVolume(buffer, size, factor);
  }

  // ??????RecordConfigs?????????
  private void notifyRecordConfigs() {
    if (mWritePCMHandler != null) {
      mWritePCMHandler.setVolumeAdjustConfig(
        mHeadsetPlugType,
        // 0:?????????????????????
        // 1:?????????????????????
        // 2:?????????????????????
        mHeadsetPlugType == 1 ?
          mWebRTCAgcConfig == null ? 1f : mWebRTCAgcConfig.getPCMAdjustVolumeHeadsetFactor():
          mWebRTCAgcConfig == null ? 1f : mWebRTCAgcConfig.getPCMAdjustVolumeNormalFactor(),
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isPCMVolumeAdjustOpen(),
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isVoiceNSOpen(),
              mWebRTCAgcConfig == null ? 0 : mWebRTCAgcConfig.getVoiceNSMode());
    }
    if (mAudioRecordHandler != null) {
      mAudioRecordHandler.setVolumeAdjustConfig(
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isCheckBluetoothSco(),
        mHeadsetPlugType,
        // 0:?????????????????????
        // 1:?????????????????????
        // 2:?????????????????????
        mHeadsetPlugType == 1 ?
          mWebRTCAgcConfig == null ? 1f : mWebRTCAgcConfig.getPCMAdjustVolumeHeadsetFactor():
          mWebRTCAgcConfig == null ? 1f : mWebRTCAgcConfig.getPCMAdjustVolumeNormalFactor(),
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isPCMVolumeAdjustOpen(),
        mWebRTCAgcConfig != null && mWebRTCAgcConfig.isVoiceNSOpen());
    }
    if (mCalculatePCMHandler != null) {
      mCalculatePCMHandler.setPCMMaxVolumedB(
        mWebRTCAgcConfig == null ? 0 : mWebRTCAgcConfig.getPCMMaxVolumedB());
    }
  }

  // ????????????PCM??????
  public void voiceRecognizePCMFile(final OnVoiceRecognizerListener listener) {
    if (listener == null) {
      return;
    }

    // ????????????????????????
    if (isRecording()) {
      return;
    }

    // ????????????????????????????????????
    if (mAgainOnVoiceRecognizerListener != null) {
      return;
    }

    // ??????????????????PCM??????
    final File file = new File(sPCMBufferFileName);
    if (!file.exists()) {
      return;
    }

    // ???????????????????????????
    mAgainOnVoiceRecognizerListener = new OnVoiceRecognizerListener() {
      @Override
      public void onRecogStart(VoiceRecognizer recognizer) {
        listener.onRecogStart(recognizer);
      }

      @Override
      public void onRecognition(VoiceRecognizer recognizer, String word) {
        listener.onRecognition(recognizer, word);
      }

      @Override
      public void onRecogLog(VoiceRecognizer recognizer, String result) {
        listener.onRecogLog(recognizer, result);
      }

      @Override
      public void onRecogStop(VoiceRecognizer recognizer) {
        listener.onRecogStop(recognizer);
      }

      @Override
      public void onRecogError(VoiceRecognizer recognizer, int code, String message) {
        listener.onRecogError(recognizer, code, message);

        // PCM??????????????????
        if (recognizer == VoiceRecognizer.NONE && code == -1) {
          // ????????????
          mAgainOnVoiceRecognizerListener = null;
        }
      }

      @Override
      public void onRecogLast(VoiceRecognizer recognizer) {
        listener.onRecogLast(recognizer);
      }
    };

    // ??????????????????
    Voice.getInstance().setOnVoiceRecognizerListener(mAgainOnVoiceRecognizerListener);
    Voice.getInstance().start();

    // ?????????????????????PCM??????
    // ??????PCMBuffer??????Thread
    try {
      final HandlerThread readThread = new HandlerThread("PCMBufferReadThread-" + System.currentTimeMillis());
      readThread.start();
      final ReadPCMHandler readPCMHandler = new ReadPCMHandler(readThread.getLooper());
      readPCMHandler.start(new FileInputStream(file), mAgainOnVoiceRecognizerListener);
    } catch (FileNotFoundException ignored) { }
  }

  // ???????????????????????????????????????????????????????????????
  public void appendContentIntoPCMAdBFile(String publicKey,
                                          String mp3FileName,
                                          String content,
                                          AppendDataCallback completedCallback) {
    if (TextUtils.isEmpty(content)) {
      return;
    }

    if (mWritePCMHandler == null) {
      return;
    }

    mWritePCMHandler.appendContentIntoPCMAdBFile(
      publicKey, mp3FileName, content, completedCallback);
  }

  // ????????????????????????????????????????????????
  public void appendContentIntoVoiceRecogLog(String publicKey,
                                             String mp3FileName,
                                             String content,
                                             AppendDataCallback completedCallback) {
    if (TextUtils.isEmpty(content)) {
      return;
    }

    if (mWritePCMHandler == null) {
      return;
    }

    mWritePCMHandler.appendContentIntoVoiceRecogLog(
      publicKey, mp3FileName, content, completedCallback);
  }

  /**
   * ????????????
   */
  public boolean checkPermissions() {
    return checkPermissions((BaseActivity) null);
  }

  /**
   * ????????????
   */
  public boolean checkPermissions(BaseFragment fragment) {
    if (fragment != null) {
      return checkPermissions((BaseActivity) fragment.getActivity());
    } else {
      return false;
    }
  }

  /**
   * ????????????
   */
  public boolean checkPermissions(BaseActivity activity) {
    // ??????????????????
    if (checkAudioRecordPermission(activity)) {
      // ???????????????????????????
      if (checkStoragePermission(activity)) {
        mOpable = true;
        onUpdateAfterPermissions();
        return true;
      }
    }
    // ??????????????????
    return false;
  }

  /**
   * ????????????????????????????????????????????????
   */
  public long getPCMFileDuration() {
    if (mAudioRecordHandler == null) {
      return 0;
    } else {
      return mAudioRecordHandler.getPCMFileDuration();
    }
  }

  // ??????????????????????????????
  private void onUpdateAfterPermissions() {
    if (mNoticeHandler != null) {
      mNoticeHandler.sendEmptyMessage(WHAT_HAS_PERMISSIONS);
    }
  }

  // ??????????????????
  private boolean checkAudioRecordPermission(BaseActivity activity) {
    return Permission.hasRecorderPermission(new OnCheckPermissionImpl(activity));
  }

  // ??????SD??????
  private boolean checkStoragePermission(BaseActivity activity) {
    return Permission.hasStoragePermission(new OnCheckPermissionImpl(activity));
  }

  // WebRTC????????????CPU??????['arm64-v8a']
  // ????????????????????????libjmedia2.so?????????????????????????????????
  // ?????????????????????isCpuAbiSupport???????????????????????????????????????isCpuAbiSupport????????????
  private void checkCpuAbiSupport() {
    final List<String> abis = new ArrayList<>();
    abis.add("arm64-v8a");
    final String abi_ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
        Build.SUPPORTED_ABIS[0] : Build.CPU_ABI;
    final String abi = abi_ == null ? "" : abi_.toLowerCase();
    final boolean cpuSupport = abis.contains(abi);
    // ??????????????????????????????libjmedia3.so/libjmedia4.so
    if (MediaNUtils3.getInstance().init()) {
      sMediaNUtils = MediaNUtils3.getInstance();
    } else if (MediaNUtils4.getInstance().init()) {
      sMediaNUtils = MediaNUtils4.getInstance();
    } else {
      sMediaNUtils = null;
    }
    isCpuAbiSupport = cpuSupport && sMediaNUtils != null;
  }

  // ???????????????
  private void initAudioApm(int sample, int mode) {
    if (isCpuAbiSupport) {
      if (sMediaNUtils.webrtcCreateApm()) {
        Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS create success.");
        if (sample == 16000) {
          if (sMediaNUtils.webrtcSetupApm16K(mode)) {
            Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS setup 16K success.");
          } else {
            Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS setup 16K fail.");
          }
        } else if (sample == 44100) {
          if (sMediaNUtils.webrtcSetupApm441K(mode)) {
            Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS setup 441K success.");
          } else {
            Logger.d("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS setup 441K fail.");
          }
        }
      } else {
        Logger.e("WebRTC", sMediaNUtils.desc() + "-WebRTC-NS create fail.");
      }
    }
  }

  // ????????????
  private void destroyAudioApm() {
    if (isCpuAbiSupport) {
      sMediaNUtils.webrtcFreeApm();
    }
  }

  // ??????PCMBuffer??????Handler
  private void exitPCMBufferWriteHandler() {
    if (mWritePCMHandler != null) {
      mWritePCMHandler.exit();
    }
  }

  // ??????PCM??????Handler
  private void resetCalculatePCMHandler() {
    if (mCalculatePCMHandler != null) {
      mCalculatePCMHandler.reset();
    }
  }

  // ????????????PCM????????????
  public double getCurrentAveragePCMdB() {
    if (mCalculatePCMHandler == null) {
      return 0;
    }

    return mCalculatePCMHandler.getAveragePCMdB();
  }

  // ??????PCM??????Handler
  private void exitCalculatePCMHandler() {
    if (mCalculatePCMHandler != null) {
      mCalculatePCMHandler.exit();
    }
  }

  public void setCalculateSecPCMdBShowMins(int minCount, int mindB) {
    if (mCalculatePCMHandler != null) {
      mCalculatePCMHandler.setSecPCMdBShowMins(minCount, mindB);
    }
  }

  // ??????SecPCMdBShowCount
  public int[] resolveSecPCMdBShowCount(int count) {
    if (mCalculatePCMHandler != null) {
      return mCalculatePCMHandler.resolveSecPCMdBShowCount(count);
    } else {
      return new int[] {0, 0};
    }
  }

  // ?????????????????????????????????
  private void checkHeadsetPlugState() {
    Context context = mApp == null ? null : mApp.get();
    checkHeadsetPlugState(context);
  }

  // ?????????????????????????????????
  public void checkHeadsetPlugState(Context context) {
    // 0:?????????????????????
    // 1:?????????????????????
    // 2:?????????????????????
    int headsetPlugType = 0;
    if (context != null) {
      AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      if (manager != null) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
          AudioDeviceInfo[] devices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS);
          for (AudioDeviceInfo device : devices) {
            Logger.d("HeadsetPlugReceiver", "device: " + device.getType());
            if (device.getType() ==
              // A device type describing a headset, which is the combination of a headphones and microphone.
              // ????????????????????????????????????????????????????????????????????????
              AudioDeviceInfo.TYPE_WIRED_HEADSET) {
              headsetPlugType = 1;
            } else if (device.getType() ==
              // A device type describing a Bluetooth device typically used for telephony.
              // ?????????????????????????????????????????????????????????
              AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
              headsetPlugType = 2;
            }
          }
        } else {
          headsetPlugType = manager.isWiredHeadsetOn() ? 1 : 0;
        }
      }
    }

    mHeadsetPlugType = headsetPlugType;
    if (mNoticeHandler != null) {
      Message msg = mNoticeHandler.obtainMessage();
      msg.what = WHAT_HEADSET_PLUG_CHANGED;
      msg.arg1 = mHeadsetPlugType;
      mNoticeHandler.sendMessage(msg);
    }
  }

  /*
   * ???????????????????????????
   *   0:?????????????????????
   *   1:?????????????????????
   *   2:?????????????????????
   */
  public int getHeadsetPlugType() {
    return mHeadsetPlugType;
  }

  public int getRecordSource() {
    if (mAudioRecordHandler == null) {
      return sAudioSourceMic;
    }

    return mAudioRecordHandler.getRecordSource();
  }

  // ??????????????????????????????????????????
  private void unregisterHeadsetPlugReceiver() {
    if (mHeadsetPlugReceiver != null &&
      mApp != null && mApp.get() != null) {
      mApp.get().unregisterReceiver(mHeadsetPlugReceiver);
    }
    mHeadsetPlugReceiver = null;
  }

  // ????????????????????????BluetoothSco
  public static boolean isSupportBluetoothSco(Context context) {
    if (context == null) {
      return false;
    }

    AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    return manager != null && manager.isBluetoothScoAvailableOffCall();
  }



  // ??????BluetoothSco
  public static boolean checkBluetoothSco(Context context) {
    if (context == null) {
      return false;
    }

    AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    if (manager == null) {
      return false;
    }

    if (!manager.isBluetoothScoAvailableOffCall()) {
      return false;
    }

    if (manager.isBluetoothScoOn()) {
      return true;
    }

    manager.startBluetoothSco();
    long timeout = 10;
    while (!manager.isBluetoothScoOn() && timeout-- > 0) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (timeout == 50) {
        manager.startBluetoothSco();
      }
    }

    return manager.isBluetoothScoOn();
  }

  // ????????????BluetoothSco
  public static void tryStopBluetoothSco(Context context) {
    if (context == null) {
      return;
    }

    AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    if (manager == null) {
      return;
    }

    if (!manager.isBluetoothScoAvailableOffCall()) {
      return;
    }

    manager.stopBluetoothSco();
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class HeadsetPlugReceiver extends BroadcastReceiver {
    private Runnable mOnChangedCallback;

    public HeadsetPlugReceiver(Runnable onChangedCallback) {
      mOnChangedCallback = onChangedCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      Logger.d("HeadsetPlugReceiver", intent.getAction());
      if (mOnChangedCallback != null) mOnChangedCallback.run();
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  public static class RecordVolumeAgcConfig {
    private int mMinLevel;
    private int mMaxLevel;
    private int mCompressionGaindB;
    private boolean mLimiterEnable;
    private int mTargetLevelDbfs;
    private boolean mCheckBluetoothSco;
    private float mPCMAdjustVolumeNormalFactor;
    private float mPCMAdjustVolumeHeadsetFactor;
    private float mPCMMaxVolumedB;
    private boolean mPCMVolumeAdjustOpen;
    private boolean mVoiceNSOpen;
    private int mVoiceNSMode;
    private String mFFmpegParams;

    public RecordVolumeAgcConfig(int minLevel, int maxLevel,
                                 int compressionGaindB,
                                 boolean limiterEnable,
                                 int targetLevelDbfs,
                                 boolean checkBluetoothSco,
                                 float pCMAdjustVolumeNormalFactor,
                                 float pCMAdjustVolumeHeadsetFactor,
                                 float pCMMaxVolumedB,
                                 boolean pCMAdjustVolumeOpen,
                                 boolean voiceNSOpen,
                                 int voiceNSMode,
                                 String ffmpegParams) {
      mMinLevel = minLevel;
      mMaxLevel = maxLevel;
      mCompressionGaindB = compressionGaindB;
      mLimiterEnable = limiterEnable;
      mTargetLevelDbfs = targetLevelDbfs;
      mCheckBluetoothSco = checkBluetoothSco;
      mPCMAdjustVolumeNormalFactor = pCMAdjustVolumeNormalFactor;
      mPCMAdjustVolumeHeadsetFactor = pCMAdjustVolumeHeadsetFactor;
      mPCMMaxVolumedB = pCMMaxVolumedB;
      mPCMVolumeAdjustOpen = pCMAdjustVolumeOpen;
      mVoiceNSOpen = voiceNSOpen;
      mVoiceNSMode = voiceNSMode;
      mFFmpegParams = ffmpegParams;
    }

    public int getMinLevel() {
      return mMinLevel;
    }

    public int getMaxLevel() {
      return mMaxLevel;
    }

    public int getCompressionGaindB() {
      return mCompressionGaindB;
    }

    public int getTargetLevelDbfs() {
      return mTargetLevelDbfs;
    }

    public boolean isLimiterEnable() {
      return mLimiterEnable;
    }

    public boolean isCheckBluetoothSco() {
      return mCheckBluetoothSco;
    }

    public float getPCMAdjustVolumeNormalFactor() {
      return mPCMAdjustVolumeNormalFactor;
    }

    public float getPCMAdjustVolumeHeadsetFactor() {
      return mPCMAdjustVolumeHeadsetFactor;
    }

    public float getPCMMaxVolumedB() {
      return mPCMMaxVolumedB;
    }

    public boolean isPCMVolumeAdjustOpen() {
      return mPCMVolumeAdjustOpen;
    }

    public boolean isVoiceNSOpen() {
      return mVoiceNSOpen;
    }

    // Allow for modes: 0, 1, 2, 3.
    public int getVoiceNSMode() {
      return mVoiceNSMode;
    }

    public String getFFmpegParams() {
      return mFFmpegParams;
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class NoticeHandler extends Handler {
    private String mFileName;
    private SoftReference<AudioRecorderStateListener> mListener;

    NoticeHandler(Looper looper, AudioRecorderStateListener listener) {
      super(looper);
      mListener = new SoftReference<>(listener);
    }

    // ????????????????????????????????????
    public void setFileName(String fileName) {
      mFileName = fileName;
    }

    @Override
    public void handleMessage(@NotNull Message msg) {
      AudioRecorderStateListener listener = mListener.get();
      if (listener != null) {
        listener.onAudioRecorderStateChanged(
            msg.what,
            msg.arg1,
            msg.arg2,
            msg.obj
        );
      }
      try {
        int what = msg.what;
        String jsonParams = null;
        switch (msg.what) {

          // ?????????????????????
          case WHAT_HAS_PERMISSIONS: {
            what = WHAT_HAS_PERMISSIONS;
          }
          break;

          // ????????????
          case 100: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Started");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // ????????????
          case 200: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Resumed");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // ????????????
          case 300: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Paused");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // ?????????????????????
          case 700: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "LossAudioFocus");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // ????????????
          case 999: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Stoped");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          /////////////////////////////////////////////////
          // ??????????????????
          case 666: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Read");
            json.put("arg1", msg.arg1);
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // ??????????????????
          case 900: {
            what = WHAT_RECORD_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Error");
            json.put("arg1", msg.arg1);
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          /////////////////////////////////////////////////
          // ???????????? - ??????
          case 801: {
            what = WHAT_PROCESS_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "ProcessStarted");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // ???????????? - ????????????
          case 810: {
            what = WHAT_PROCESS_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "Processing");
            json.put("percent", msg.arg1 * 100 / (float) msg.arg2);
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          // ???????????? - ??????
          case 888: {
            what = WHAT_PROCESS_STATE_CHANGED;
            JSONObject json = new JSONObject();
            json.put("type", "ProcessCompleted");
            // other default values
            setupMsgDefaultValues(msg, json);
            jsonParams = json.toString();
          }
          break;

          /////////////////////////////////////////////////
          // ?????????????????????????????????
          case WHAT_RECORD_dB_CHANGED: {
            what = WHAT_RECORD_dB_CHANGED;
          }
          break;

          /////////////////////////////////////////////////
          // ?????????????????????????????????????????????
          case WHAT_HEADSET_PLUG_CHANGED: {
            what = WHAT_HEADSET_PLUG_CHANGED;
          }
          break;
        }

        // ????????????
        sendNoticeMessage(what, msg.arg1, msg.arg2, jsonParams);
      } catch (JSONException | UnsupportedEncodingException ignored) {
      }
    }

    private void setupMsgDefaultValues(Message msg, JSONObject json)
        throws JSONException, UnsupportedEncodingException {
      if (!TextUtils.isEmpty(mFileName)) {
        json.put("data", URLEncoder.encode(mFileName, "utf-8"));
      }
      if (msg.obj != null) {
        json.put("obj", msg.obj);
      }
    }

    private void sendNoticeMessage(int what, int arg1, int arg2, Object obj) {
      for (int i = 0; i < sHandlers.size(); i++) {
        Handler handler = sHandlers.get(i);
        if (handler != null) {
          Message notice = handler.obtainMessage(what);
          notice.arg1 = arg1;
          notice.arg2 = arg2;
          notice.obj = obj;
          handler.sendMessage(notice);
        }
      }
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class AudioRecordHandler extends Handler
      implements AudioRecord.OnRecordPositionUpdateListener, JRecorder.StateChangedListener {
    private SoftReference<Context> mContext;
    // Notice Handler
    private NoticeHandler mNoticeHandler;
    // Audio Record
    private JRecorder mRecorder;
    // JRecorder State Changed Listener
    private JRecorder.StateChangedListener mStateChangedListener;
    // PCM Buffer Write Handler
    private WritePCMHandler mWritePCMHandler;
    // PCM Calculate Handler
    private CalculatePCMHandler mCalculatePCMHandler;
    // Check CPU ABI Support
    private boolean isCpuAbiSupport;
    // ?????????????????????BluetoothSco
    private boolean mCheckBluetoothSco;
    // ??????????????????????????????
    private boolean mVoiceNSOpen;
    // ????????????????????????????????????
    // 0:?????????????????????
    // 1:?????????????????????
    // 2:?????????????????????
    private int mHeadsetPlugType = 0;
    private int mRecordSource = sAudioSourceMic;
    private float mVolumeAdjustFactor = 1f;
    private boolean mVolumeAdjustOpen = false;
    // ???????????????????????????
    private boolean isRequestStop;
    // ???????????????PCM?????????????????????
    private long mPCMFileLength = 0;
    // ???????????????PCM???????????????????????????
    private long mPCMFileDuration = 0;

    AudioRecordHandler(Context context, Looper looper, NoticeHandler noticeHandler,
                       WritePCMHandler writePCMHandler,
                       CalculatePCMHandler calculatePCMHandler,
                       JRecorder.StateChangedListener stateChangedListener,
                       boolean support) {
      super(looper);
      mContext = new SoftReference<>(context);
      mNoticeHandler = noticeHandler;
      mWritePCMHandler = writePCMHandler;
      mCalculatePCMHandler = calculatePCMHandler;
      mStateChangedListener = stateChangedListener;
      isCpuAbiSupport = support;
    }

    void start(String fileName) {
      mPCMFileLength = 0;
      mPCMFileDuration = 0;
      isRequestStop = false;
      Message msg = obtainMessage(100);
      msg.obj = fileName;
      sendMessage(msg);
    }

    void resume() {
      isRequestStop = false;
      sendEmptyMessage(200);
    }

    void pause() {
      isRequestStop = false;
      sendEmptyMessage(300);
    }

    void stop() {
      mPCMFileLength = 0;
      mPCMFileDuration = 0;
      isRequestStop = true;
      removeCallbacksAndMessages(null);
      sendEmptyMessage(999);
    }

    void exit() {
      mPCMFileLength = 0;
      mPCMFileDuration = 0;
      if (!isRequestStop) removeCallbacksAndMessages(null);
      isRequestStop = true;
      sendEmptyMessage(0);
    }

    void setVolumeAdjustConfig(boolean checkBluetoothSco,
                               int headsetPlugType, float factor,
                               boolean volumeOpen,
                               boolean voiceNSOpen) {
      mCheckBluetoothSco = checkBluetoothSco;
      mHeadsetPlugType = headsetPlugType;
      mVolumeAdjustFactor = factor;
      mVolumeAdjustOpen = volumeOpen;
      mVoiceNSOpen = voiceNSOpen;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
      // nothing
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
      // nothing
    }

    private void writePCMBufferToFile(short[] rawData, int readSize) {
        if (mWritePCMHandler != null) {
          mWritePCMHandler.addPCMBufferWrite(rawData, readSize);
        }
    }

    int getRecordSource() {
      return mRecordSource;
    }

    private boolean start(int what, String name) {
      final Context context = mContext == null ? null : mContext.get();
      if (context != null && mRecorder == null) {
        // 0:?????????????????????
        // 1:?????????????????????
        // 2:?????????????????????
        if (mCheckBluetoothSco) {
          mRecordSource = mHeadsetPlugType == 2 ? (
            // ??????BluetoothSco
            checkBluetoothSco(context) ? sAudioSourceVcm : sAudioSourceMic
          ) : sAudioSourceMic;
        } else {
          mRecordSource = sAudioSourceMic;
        }
        // ???????????????????????????????????????
        if (mWritePCMHandler != null) {
          mWritePCMHandler.setRecordSource(mRecordSource);
        }
        // ?????????JRecorder
        mRecorder = new JRecorder(
          context,
          mRecordSource,
          sSampleRateInHz,
          sChannelConfig,
          sAudioFormat,
          sPCMBufferSize);
        mRecorder.setStateChangedListener(this);
        mRecorder.setRecordPositionUpdateListener(this, this);
        mRecorder.setPositionNotificationPeriod(FRAME_COUNT);
        mRecorder.startRecording();
        if (mNoticeHandler != null) {
          mNoticeHandler.sendEmptyMessage(what);
        }
        // ?????????_adB.txt
        if (mWritePCMHandler != null) {
          mWritePCMHandler.addErrorInfo(name + ":true");
        }
        return true;
      } else {
        // ?????????_adB.txt
        if (mWritePCMHandler != null) {
          mWritePCMHandler.addErrorInfo(name + ":false");
        }
        return false;
      }
    }

    private void read() {
      if (!isRequestStop && mRecorder != null) {
        int readSize = mRecorder.read(sPCMBuffer, 0, sPCMBufferSize);
        if (!isRequestStop) {
          if (readSize >= 0) {
            if (readSize > 0) {
              // ??????????????????????????????????????????1
              if (mVolumeAdjustOpen && mVolumeAdjustFactor > 1f) {
                MediaNUtils.pcmAdjustVolume(sPCMBuffer, readSize, mVolumeAdjustFactor);
              }
//              // ????????????????????????????????????
//              if (isCpuAbiSupport && mVoiceNSOpen) {
//                // WebRTC??????
//                processWebRTCAgc(sMediaNUtils, sPCMBuffer, readSize);
//              }
              // ???PCM??????????????????
              writePCMBufferToFile(sPCMBuffer, readSize);
              // ??????PCM
              calculatePCM(sPCMBuffer, readSize);
            }
            // ????????????
            sendEmptyMessage(666);
          } else {
            if (mNoticeHandler != null) {
              Message msg = mNoticeHandler.obtainMessage(900);
              msg.arg1 = readSize;
              mNoticeHandler.sendMessage(msg);
            }
            // ??????????????????
            sendEmptyMessage(999);
          }
        }
      }
    }

    private void pauseRecorder() {
      if (mRecorder != null) {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
      }

      // ????????????BluetoothSco
      if (mCheckBluetoothSco) {
        tryStopBluetoothSco(mContext == null ? null : mContext.get());
      }

      // ??????????????????
      if (mNoticeHandler != null) {
        mNoticeHandler.sendEmptyMessage(300);
      }
    }

    private void stopRecorder() {
      if (mRecorder != null) {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
      }

      // ????????????BluetoothSco
      if (mCheckBluetoothSco) {
        tryStopBluetoothSco(mContext == null ? null : mContext.get());
      }

      // ???????????????????????????
      if (mNoticeHandler != null) {
        mNoticeHandler.sendEmptyMessage(999);
      }

      // ????????????
      // ???????????????mp3?????????????????????
      if (mWritePCMHandler != null) {
        mWritePCMHandler.stopRecord();
      }
    }

    static int calculatePCMUnitDelayMs(int sampleRateInHz, int readSize) {
      float bLength = readSize << 1; // bytes
      return (int) ((bLength / ((float)sampleRateInHz) / 2.0f) * 1000);
    }

    static short[] s16KNSBuf = new short[160];

    static void processWebRTCAgc(IMediaNUtils mediaNUtils, short[] buffer, int size) {
      if (mediaNUtils != null) {
        try {
          int res = mediaNUtils.webrtcProcessStream(buffer);
          Logger.d("WebRTC", "process(" + mediaNUtils.desc() + "): " + res);
        } catch (Throwable e) {
          Logger.d("WebRTC", "process(" + mediaNUtils.desc() + ") except: " + e.getMessage());
        }
      }
    }

    private void calculatePCM(short[] rawData, int readSize) {
      // ??????PCM?????????
      if (!isRequestStop && mCalculatePCMHandler != null) {
        mCalculatePCMHandler.addPCMBufferWrite(rawData, readSize);
      }
      // ?????????PCM?????????
      mPCMFileLength += readSize << 1; // bytes
      // ?????????PCM?????????????????????
      // ?????????Byte = ????????????Hz ?????????????????/8????? ????????? ?? ??????s
      // ?????????????????????????????? - ??????????????????
      //    AudioRecorderManager.sAudioFormat   = AudioFormat.ENCODING_PCM_16BIT
      //    AudioRecorderManager.sChannelConfig = AudioFormat.CHANNEL_IN_MONO
      mPCMFileDuration = mPCMFileLength / sSampleRateInHz / 2;
    }

    private long getPCMFileDuration() {
      return mPCMFileDuration;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {

        // ????????????
        case 0: {
          // ?????????_adB.txt
          if (mWritePCMHandler != null) {
            mWritePCMHandler.addErrorInfo("exit");
          }
          // quit
          getLooper().quitSafely();
        }
        break;

        // ????????????
        case 100: {
          if (mWritePCMHandler != null) {
            final String fileName = msg.obj instanceof String ? (String) msg.obj : "";
            // ?????????_adB.txt
            mWritePCMHandler.addErrorInfo("reset");
            // reset
            mWritePCMHandler.reset(fileName, () -> {
              // ?????????_adB.txt
              mWritePCMHandler.addErrorInfo("start:" + fileName);
              // start
              if (start(100, "start")) { read(); }
            });
          }
        }
        break;

        // ????????????
        case 200: {
          // ?????????_adB.txt
          if (mWritePCMHandler != null) {
            mWritePCMHandler.addErrorInfo("resume");
          }
          // start - resume
          if (start(200, "resume")) { read(); }
        }
        break;

        // ????????????
        case 666: {
          read();
        }
        break;

        // ????????????
        case 300: {
          pauseRecorder();
          // ?????????_adB.txt
          if (mWritePCMHandler != null) {
            mWritePCMHandler.addErrorInfo("pause");
          }
        }
        break;

        // ????????????
        case 999: {
          stopRecorder();
          // ?????????_adB.txt
          if (mWritePCMHandler != null) {
            mWritePCMHandler.addErrorInfo("stop");
          }
        }
        break;
      }
    }

    @Override
    public void onLossAudioFocus() {
      if (mStateChangedListener != null) {
        mStateChangedListener.onLossAudioFocus();
      }
      // ?????????_adB.txt
      if (mWritePCMHandler != null) {
        mWritePCMHandler.addErrorInfo("loss_audio_focus");
      }
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class WritePCMHandler extends Handler {
    private Runnable mResetCallback;
    // Check CPU ABI Support
    private boolean isCpuAbiSupport;
    private String mPCMBufferFileName;
    private RandomAccessFile mPCMBufferFile;
    private RandomAccessFile mPCMAdBFile;
    private Queue<PCMBuffer> mBufferPool = new ConcurrentLinkedQueue<>();
    private long mPCMBufferOffset = 0;
    private byte[] mp3Buffer = null;
    private File mAudioMp3File;
    private RandomAccessFile mAudioMp3FileWriter;
    private char[] mAdBPoints = new char[60];
    // ????????????????????????????????????
    // 0:?????????????????????
    // 1:?????????????????????
    // 2:?????????????????????
    private int mHeadsetPlugType = 0;
    private int mRecordSource = sAudioSourceMic;
    private float mVolumeAdjustFactor = 1f;
    private boolean mVolumeAdjustOpen = false;
    private boolean mVoiceNSOpen = false;
    private int mVoiceNSMode = 0;
    private RandomAccessFile mVoiceRecogLogFile;
    // ??????
    private Handler mNoticeHandler;

    WritePCMHandler(Looper looper, String pCMBufferFileName,
                    Handler noticeHandler, boolean support) {
      super(looper);
      mPCMBufferFileName = pCMBufferFileName;
      mNoticeHandler = noticeHandler;
      isCpuAbiSupport = support;
    }

    void reset(String fileName, Runnable callback) {
      mResetCallback = callback;
      Message msg = obtainMessage(10);
      msg.obj = fileName;
      sendMessage(msg);
    }

    PCMBuffer obtainBuffer() {
      PCMBuffer buffer = mBufferPool.poll();
      if (buffer == null) {
        buffer = new PCMBuffer(sPCMBufferSize);
        Logger.d("WritePCMHandler", "Crate PCMBuffer");
      }
      return buffer;
    }

    void addPCMBufferWrite(short[] rawData, int readSize) {
      PCMBuffer buffer = obtainBuffer();
      buffer.copyFrom(rawData, mPCMBufferOffset, readSize);
      mPCMBufferOffset += readSize;
      Message msg = obtainMessage(100);
      msg.obj = buffer;
      sendMessage(msg);
    }

    void stopRecord() {
      sendEmptyMessage(999);
    }

    void addUnitAdB(double dB) {
      Message msg = obtainMessage(200);
      msg.obj = dB;
      sendMessage(msg);
    }

    void addSoHighAdB(double dB) {
      Message msg = obtainMessage(200);
      msg.arg1 = WHAT_RECORD_dB_SO_HIGH;
      msg.obj = dB;
      sendMessage(msg);
    }

    void addErrorInfo(String error) {
      Message msg = obtainMessage(200);
      msg.arg1 = WHAT_RECORD_ERROR_INFO;
      msg.obj = error;
      sendMessage(msg);
    }

    void appendContentIntoPCMAdBFile(String publicKey,
                                     String mp3FileName,
                                     String content,
                                     AppendDataCallback completedCallback) {
      AppendData data = new AppendData();
      data.publicKey = publicKey;
      data.mp3FileName = mp3FileName;
      data.content = content;
      data.callback = completedCallback;
      Message msg = obtainMessage(300);
      msg.obj = data;
      sendMessage(msg);
    }

    void addVoiceRecogLog(String log) {
      Message msg = obtainMessage(400);
      msg.obj = log;
      sendMessage(msg);
    }

    void appendContentIntoVoiceRecogLog(String publicKey,
                                        String mp3FileName,
                                        String content,
                                        AppendDataCallback completedCallback) {
      AppendData data = new AppendData();
      data.publicKey = publicKey;
      data.mp3FileName = mp3FileName;
      data.content = content;
      data.callback = completedCallback;
      Message msg = obtainMessage(401);
      msg.obj = data;
      sendMessage(msg);
    }

    void exit() {
      removeCallbacksAndMessages(null);
      sendEmptyMessage(0);
    }

    void setVolumeAdjustConfig(int headsetPlugType, float factor,
                               boolean volumeOpen, boolean voiceNSOpen,
                               int voiceNSMode) {
      mHeadsetPlugType = headsetPlugType;
      mVolumeAdjustFactor = factor;
      mVolumeAdjustOpen = volumeOpen;
      mVoiceNSOpen = voiceNSOpen;
      mVoiceNSMode = voiceNSMode;
    }

    void setRecordSource(int source) {
      mRecordSource = source;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {

        // ????????????
        case 0: {
          getLooper().quitSafely();
        }
        break;

        // ???????????? - ?????????????????? - ??????????????????
        case 10: {
          resetPCMAdBFile((String) msg.obj);
          resetPCMBufferFile();
          resetAudioFiles((String) msg.obj);
          resetBuffers();
          if (mResetCallback != null) {
            mResetCallback.run();
          }
        }
        break;

        // ?????????????????? - ??????Mp3 - ????????????
        case 100: {
          PCMBuffer pcmBuffer = (PCMBuffer) msg.obj;
          short[] buffer = pcmBuffer.getData();
          int size = pcmBuffer.getSize();
          // ??????Mp3
          processMp3OneBuffer(buffer, size);
          // ??????Voice??????????????????????????????????????????
          Voice.getInstance().write(buffer, size);
          // ??????PCM??????
          pcmBuffer.writeTo(mPCMBufferFile);
          // ????????????????????????BufferPool
          mBufferPool.add(pcmBuffer);
        }
        break;

        // ??????????????????
        case 999: {
          // ????????????PCMMp3??????
          // notice - mp3????????????
          if (mNoticeHandler != null) {
            mNoticeHandler.sendEmptyMessage(801);
          }
//          // ???????????????????????????????????????????????????????????????
//          checkNSAgain();
          // ????????????MP3??????
          flushMp3Data();
          closeMp3Data();
          // ??????????????????MP3??????????????????
          checkNSMp3();
          // notice - mp3????????????
          if (mNoticeHandler != null) {
            mNoticeHandler.sendEmptyMessage(888);
          }
        }
        break;

        // ?????????????????????????????????????????????
        case 200: {
          if (mPCMAdBFile != null) {
            try {
              boolean isError = msg.arg1 == WHAT_RECORD_ERROR_INFO;
              boolean soHigh  = msg.arg1 == WHAT_RECORD_dB_SO_HIGH;
              String error    = isError ? (String) msg.obj : "";
              double average  = isError ? -99.99 : (double) msg.obj;
              int count = Math.max((int) (mAdBPoints.length + average), 0);
              Arrays.fill(mAdBPoints, 0, count, soHigh ? '-' : '.');
              Arrays.fill(mAdBPoints, count, mAdBPoints.length, ' ');
              String line = String.format(Locale.CHINESE,
                "%d|%d|%d|%d|%.2f|%.2f|%s\n",
                // ????????????
                mVolumeAdjustOpen ? 1 : 0,
                // ????????????
                mVoiceNSOpen ? 1 : 0,
                // 0:?????????????????????
                // 1:?????????????????????
                // 2:?????????????????????
                mHeadsetPlugType,
                // 1:MIC
                // 7:VOICE_COMMUNICATION
                mRecordSource,
                mVolumeAdjustFactor,
                average,
                isError ? error : String.valueOf(mAdBPoints));
              mPCMAdBFile.write(line.getBytes());
              // debug
              Logger.d("AudioRecorderManager", "dB: " + line);
            } catch (Exception ignored) {}
          }
        }
        break;

        // ???????????????PCMAdBFile?????????
        case 300: {
          if (msg.obj instanceof AppendData) {
              try {
                AppendData data = (AppendData) msg.obj;
                if (!TextUtils.isEmpty(data.mp3FileName)) {
                  File file = new File(data.mp3FileName + "_adB.txt");
                  if (!file.exists()) file.createNewFile();
                  if (file.exists()) {
                    RandomAccessFile pcmAdBFile = new RandomAccessFile(file, "rw");
                    String content = data.content;
                    if (!TextUtils.isEmpty(data.publicKey)) {
                      try {
                        byte[] encode = RSAProvider.encryptPublicKey(
                          data.content.getBytes(), data.publicKey);
                        content = Base64Utils.encode(encode).trim();
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                    }
                    pcmAdBFile.seek(file.length());
                    pcmAdBFile.write(String.format(Locale.CHINESE,
                      "\n--------------------------------------\n" +
                        "%s" +
                        "\n--------------------------------------\n",
                      content).getBytes());
                    pcmAdBFile.close();
                    // completed callback
                    if (data.callback != null) {
                      data.callback.onAppendDataCompleted(file.getAbsolutePath());
                    }
                  }
                }
            } catch (Throwable ignored) {}
          }
        }
        break;

        // ????????????????????????
        case 400: {
          try {
            String log = (String) msg.obj;
            Logger.d("AudioRecorderManager", "addVoiceRecogLog: " + log);
            if (mVoiceRecogLogFile != null) {
              mVoiceRecogLogFile.write(String.format(Locale.CHINESE,
                "%s\n", log).getBytes());
            }
          } catch (Throwable ignored) {}
        }
        break;

        // ???????????????VoiceRecogLog?????????
        case 401: {
          if (msg.obj instanceof AppendData) {
            try {
              AppendData data = (AppendData) msg.obj;
              if (!TextUtils.isEmpty(data.mp3FileName) && !TextUtils.isEmpty(data.publicKey)) {
                File file = new File(data.mp3FileName + "_recog.txt");
                if (!file.exists()) file.createNewFile();
                if (file.exists()) {
                  RandomAccessFile recogLogFile = new RandomAccessFile(file, "rw");
                  StringBuilder conSb = new StringBuilder();
                  String line;
                  while ((line = recogLogFile.readLine()) != null) {
                    conSb.append(new String(line.getBytes(
                      StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)).append('\n');
                  }
                  recogLogFile.close();
                  if (!TextUtils.isEmpty(data.content)) {
                    conSb.append("\n--------------------------------------\n");
                    conSb.append(data.content);
                    conSb.append("\n--------------------------------------\n");
                  }
                  byte[] encode = RSAProvider.encryptPublicKey(
                    conSb.toString().getBytes(), data.publicKey);
                  String content = Base64Utils.encode(encode).trim();
                  file.delete();
                  file.createNewFile();
                  if (file.exists()) {
                    recogLogFile = new RandomAccessFile(file, "rw");
                    recogLogFile.write(String.format(Locale.CHINESE,
                      "\n--------------------------------------\n" +
                        "%s" +
                        "\n--------------------------------------\n",
                      content).getBytes());
                    recogLogFile.close();
                  }
                  // completed callback
                  if (data.callback != null) {
                    data.callback.onAppendDataCompleted(file.getAbsolutePath());
                  }
                }
              }
            } catch (Throwable ignored) {}
          }
        }
        break;
      }
    }

    // ??????PCMBuffer??????
    private void resetPCMBufferFile() {
      if (mPCMBufferFile != null) {
        try {
          mPCMBufferFile.close();
          mPCMBufferFile = null;
        } catch (IOException ignored) {
        }
      }
      File file = new File(mPCMBufferFileName);
      if (file.exists()) {
        file.delete();
      }
      try {
        if (file.createNewFile()) {
          mPCMBufferFile = new RandomAccessFile(file, "rw");
        }
      } catch (IOException ignored) {
      }
    }

    // ??????PCMAdBFile??????
    private void resetPCMAdBFile(String fileName) {
      if (mPCMAdBFile != null) {
        try {
          mPCMAdBFile.close();
          mPCMAdBFile = null;
        } catch (IOException ignored) {
        }
      }
      File file = new File(fileName + "_adB.txt");
      if (file.exists()) {
        file.delete();
      }
      try {
        if (file.createNewFile()) {
          mPCMAdBFile = new RandomAccessFile(file, "rw");
        }
      } catch (IOException ignored) {
      }
    }

    void resetAudioFiles(String fileName) {
      try {
        int channels = 2;
        // ?????????????????????
        MediaNUtils.lamemp3Init(sSampleRateInHz, channels, sSampleRateInHz, BIT_RATE);
        // ??????Mp3Buffer
        mp3Buffer = new byte[(int) (7200 + (sPCMBufferSize * channels * 1.25))];

        if (mAudioMp3FileWriter != null) {
          try {
            mAudioMp3FileWriter.close();
            mAudioMp3FileWriter = null;
          } catch (IOException ignored) { }
        }

        if (mVoiceRecogLogFile != null) {
          try {
            mVoiceRecogLogFile.close();
            mVoiceRecogLogFile = null;
          } catch (IOException ignored) { }
        }

        if (mAudioMp3File != null && mAudioMp3File.exists()) {
          mAudioMp3File.delete();
          mAudioMp3File = null;
        }

        if (!TextUtils.isEmpty(fileName)) {
          mAudioMp3File = new File(fileName);
          if (mAudioMp3File.exists()) {
            mAudioMp3File.delete();
          }
          if (mAudioMp3File.createNewFile()) {
            mAudioMp3FileWriter = new RandomAccessFile(mAudioMp3File, "rw");
          }

          File logFile = new File(fileName + "_recog.txt");
          if (logFile.exists()) logFile.delete();
          if (logFile.createNewFile()) {
            mVoiceRecogLogFile = new RandomAccessFile(logFile, "rw");
          }
        }
      } catch (Throwable ignored) {
        mAudioMp3File = null;
        mAudioMp3FileWriter = null;
        mVoiceRecogLogFile = null;
      }
    }

    private void resetBuffers() {
      // clear buffer pool
      mBufferPool.clear();
      // init buffer pool
      for (int i = 0; i < 30; i++) {
        mBufferPool.add(new PCMBuffer(sPCMBufferSize));
      }

      // reset mPCMBufferOffset
      mPCMBufferOffset = 0;
    }

    // ????????????PCM Buffer ??????Mp3
    private void processMp3OneBuffer(short[] buffer, int readSize) {
      if (buffer == null) {
        return;
      }

      // ??????Mp3??????
      try {
        // ??????Mp3??????
        int encodedSize = MediaNUtils.lamemp3Encode(
            buffer, buffer, readSize, mp3Buffer);
        if (mAudioMp3FileWriter != null) {
          mAudioMp3FileWriter.write(mp3Buffer, 0, encodedSize);
        }
        // debug info
        if (encodedSize <= 0) {
          throw new Exception("lamemp3_encode encodedSize <= 0");
        }
      } catch (Throwable e) {
        try {
          String msg = e.getMessage();
          msg = msg.replaceAll("\n", "###");
          msg = msg.replaceAll("\r", "###");
          addErrorInfo(msg);
          Voice.getInstance().appendRecognLog("[processMp3Error]:" + msg);
        } catch (Throwable ignored) {}
      }
    }

    // flush mp3 data
    private void flushMp3Data() {
      final int flushResult = MediaNUtils.lamemp3Flush(mp3Buffer);
      if (flushResult > 0) {
        try {
          if (mAudioMp3FileWriter != null) {
            mAudioMp3FileWriter.write(mp3Buffer, 0, flushResult);
          }
        } catch (final IOException ignored) {
        }
      }
    }

    // close mp3 data
    private void closeMp3Data() {
      MediaNUtils.lamemp3Close();
    }

//    // ???????????????????????????????????????????????????????????????
//    private void checkNSAgain() {
//      if (isCpuAbiSupport && mVoiceNSOpen &&
//        mPCMBufferFile != null && mAudioMp3FileWriter != null) {
//        try {
//          mPCMBufferFile.seek(0);
//          mAudioMp3FileWriter.seek(0);
//          short[] sBuf = new short[sPCMBufferSize];
//          byte[] bBuf = new byte[sPCMBufferSize * 2];
//          // 1??????600??? - 1??????10???
//          for (int i = 0; i < 5; i++) {
//            mPCMBufferFile.read(bBuf);
//            for (int j = 0; j < sPCMBufferSize; j++) {
//              sBuf[j] = (short) (((bBuf[j * 2]) & 0xff) +
//                                (((bBuf[j * 2 + 1]) & 0xff) << 8));
//            }
//            // WebRTC??????
//            AudioRecordHandler.processWebRTCAgc(sMediaNUtils, sBuf, sPCMBufferSize);
//            // ??????Mp3??????
//            // ??????Mp3??????
//            int encodedSize = MediaNUtils.lamemp3Encode(
//              sBuf, sBuf, sPCMBufferSize, mp3Buffer);
//            mAudioMp3FileWriter.write(mp3Buffer, 0, encodedSize);
//          }
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//    }

    // ??????????????????MP3??????????????????
    private void checkNSMp3() {
      if (isCpuAbiSupport && mVoiceNSOpen) {
        String inMp3 = mAudioMp3File == null ? "" : mAudioMp3File.getAbsolutePath();
        String outMp3 = String.format(Locale.CHINESE, "%s.mp3",
          inMp3.replace(".mp3", "_ns"));
        sMediaNUtils.webrtcFileProcess(inMp3, outMp3, mVoiceNSMode);
      }
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class CalculatePCMHandler extends Handler {
    // Buffer pool
    private Queue<SPCMBuffer> mBufferPool = new ConcurrentLinkedQueue<>();
    // PCMdB
    private float mPCMMaxVolumedB;
    private int mPCMdBCount;
    private double mPCMdBSum;
    // ?????????????????? PCMdB
    private int mUnitPCMdBCount;
    private double mUnitPCMdBSum;
    private long mUnitTime;
    // ??????????????????
    private int mUnitTimeInterval;
    // ?????? PCMdB
    private int mSecPCMdBCount;
    private double mSecPCMdBSum;
    private int mSecPCMdBLeftShowCount;
    private int mSecPCMdBRightShowCount;
    private int[] m3SecAdB = new int[]{0, 0, 0};
    private static int sSecPCMdBShowMinCount = 20;
    private static int sSecPCMdBShowMindB = -6000;
    // AdB??????
    private WritePCMHandler mWriteHandler;
    // ??????
    private Handler mNoticeHandler;

    CalculatePCMHandler(Looper looper, int unitTimeIntervalInMillisecond,
                        WritePCMHandler writeHandler,
                        Handler noticeHandler) {
      super(looper);
      mUnitTimeInterval = unitTimeIntervalInMillisecond;
      mWriteHandler = writeHandler;
      mNoticeHandler = noticeHandler;
    }

    void setPCMMaxVolumedB(float maxdB) {
      mPCMMaxVolumedB = maxdB;
    }

    void setSecPCMdBShowMins(int minCount, int mindB) {
      sSecPCMdBShowMinCount = minCount;
      sSecPCMdBShowMindB = mindB * 100;
    }

    SPCMBuffer obtainBuffer() {
      SPCMBuffer buffer = mBufferPool.poll();
      if (buffer == null) {
        buffer = new SPCMBuffer(sPCMBufferSize);
        Logger.d("CalculatePCMHandler", "Crate SPCMBuffer");
      }
      return buffer;
    }

    void reset() {
      removeCallbacksAndMessages(null);
      sendEmptyMessage(888);
    }

    void addPCMBufferWrite(short[] buffer, int size) {
      SPCMBuffer spcmBuffer = obtainBuffer();
      spcmBuffer.copyFrom(buffer, size);
      Message msg = obtainMessage(100);
      msg.obj = spcmBuffer;
      sendMessage(msg);
    }

    void exit() {
      removeCallbacksAndMessages(null);
      sendEmptyMessage(0);
    }

    double getAveragePCMdB() {
      if (mPCMdBCount == 0) {
        return 0;
      }

      return mPCMdBSum / mPCMdBCount;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {

        // ????????????
        case 0: {
          getLooper().quitSafely();
        }
        break;

        // ??????????????????
        case 100: {
          SPCMBuffer buffer = (SPCMBuffer) msg.obj;
          short[] data = buffer.getBuffer();
          if (data != null) {
            double db = MediaNUtils.pcmDB(data, data.length);
            Logger.d("CalculatePCMHandler", "db: " + db);
            if (db < 0 && db > -300) {
              mPCMdBSum += db;
              mUnitPCMdBSum += db;
              mPCMdBCount++;
              mUnitPCMdBCount++;
              // ?????????????????????????????????
              checkMaxPCMdB(db);
              // ???????????????????????????????????????????????????dB
              checkUnitPCMdB();
            } else {
              Voice.getInstance().appendRecognLog("[dBerror]:" + db);
            }
          }
          // ????????????????????????BufferPool
          mBufferPool.add(buffer);
        }
        break;

        // ????????????
        case 888: {
          mPCMdBSum = 0;
          mPCMdBCount = 0;
          mUnitPCMdBSum = 0;
          mUnitPCMdBCount = 0;
          mUnitTime = 0;
          mSecPCMdBSum = 0;
          mSecPCMdBCount = 0;
          mSecPCMdBLeftShowCount = 0;
          mSecPCMdBRightShowCount = 0;
          Arrays.fill(m3SecAdB, 0);
          // clear buffer pool
          mBufferPool.clear();
          // init buffer pool
          for (int i = 0; i < 20; i++) {
            mBufferPool.add(new SPCMBuffer(sPCMBufferSize));
          }
        }
        break;
      }
    }

    private void checkMaxPCMdB(double db) {
      if (db >= mPCMMaxVolumedB) {
        // ??????Handler
        if (mNoticeHandler != null) {
          Message msg = mNoticeHandler.obtainMessage(WHAT_RECORD_dB_SO_HIGH);
          msg.arg1 = (int) (db * 100);
          mNoticeHandler.sendMessage(msg);
        }
        if (mWriteHandler != null) {
          mWriteHandler.addSoHighAdB(db);
        }
      }
    }

    private void checkUnitPCMdB() {
      // ??????????????????
      if (System.currentTimeMillis() - mUnitTime >= mUnitTimeInterval) {
        // ????????????????????????????????????
        unitPCMdB();
        // ?????????????????????????????????
        mUnitTime = System.currentTimeMillis();
        mUnitPCMdBSum = 0;
        mUnitPCMdBCount = 0;
      }
    }

    // ??????????????????????????????
    private void unitPCMdB() {
      double average = mUnitPCMdBSum / mUnitPCMdBCount;
//      Logger.d("CalculatePCMHandler", "unitPCMdB: " + average);
      // ???????????????AdB
      secPCMdB(average);
      // ?????????????????????????????????
      if (mWriteHandler != null) {
        mWriteHandler.addUnitAdB(average);
      }
      // ??????Handler
      if (mNoticeHandler != null) {
        Message msg = mNoticeHandler.obtainMessage(WHAT_RECORD_dB_CHANGED);
        msg.arg1 = (int) (average * 100);
        msg.arg2 = ((mSecPCMdBLeftShowCount << 16) & 0xffff0000) +
                    (mSecPCMdBRightShowCount & 0xffff);
        mNoticeHandler.sendMessage(msg);
      }
    }

    // ??????SecPCMdBShowCount
    int[] resolveSecPCMdBShowCount(int count) {
      return new int[] {(count >> 16) & 0xffff, count & 0xffff};
    }

    // ??????????????????dB
    private void secPCMdB(double average) {
      mSecPCMdBSum += average;
      mSecPCMdBCount++;
      if (mSecPCMdBCount >= 10) {
        double secAverage = mSecPCMdBSum / mSecPCMdBCount;
        m3SecAdB[0] = m3SecAdB[1];
        m3SecAdB[1] = m3SecAdB[2];
        m3SecAdB[2] = (int) (secAverage * 100);
        mSecPCMdBSum = 0;
        mSecPCMdBCount = 0;
        // ????????????dB??????
        secPCMdBShow(m3SecAdB[2]);
      }
      if (mSecPCMdBLeftShowCount > 0) {
        mSecPCMdBLeftShowCount--;
      }
      if (mSecPCMdBLeftShowCount < 0) {
        mSecPCMdBLeftShowCount = 0;
      }
      if (mSecPCMdBRightShowCount > 0) {
        mSecPCMdBRightShowCount--;
      }
      if (mSecPCMdBRightShowCount < 0) {
        mSecPCMdBRightShowCount = 0;
      }
    }

    // ????????????dB??????
    private void secPCMdBShow(int average) {
      if (m3SecAdB[0] < sSecPCMdBShowMindB &&
          m3SecAdB[1] < sSecPCMdBShowMindB &&
          m3SecAdB[2] < sSecPCMdBShowMindB &&
          mSecPCMdBLeftShowCount <= 0) {
        mSecPCMdBLeftShowCount = sSecPCMdBShowMinCount + 1;
      }
      if (/*m3SecAdB[0] < sSecPCMdBShowMindB &&
          m3SecAdB[1] < sSecPCMdBShowMindB &&*/
          m3SecAdB[2] < sSecPCMdBShowMindB &&
          mSecPCMdBRightShowCount <= 0) {
        mSecPCMdBRightShowCount = sSecPCMdBShowMinCount + 1;
      } else {
        if (average < sSecPCMdBShowMindB &&
          mSecPCMdBRightShowCount >= 1 && mSecPCMdBRightShowCount <= 5) {
          mSecPCMdBRightShowCount += 10;
        }
      }
    }
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  private static class ReadPCMHandler extends Handler {
    private FileInputStream mFile;
    private byte[] mBuffer = new byte[sPCMBufferSize * 2];
    //    private short[] mSBuffer = new short[sPCMBufferSize];
    private SoftReference<OnVoiceRecognizerListener> mListener;

    ReadPCMHandler(Looper looper) {
      super(looper);
    }

    void start(FileInputStream file, OnVoiceRecognizerListener listener) {
      mFile = file;
      mListener = new SoftReference<>(listener);
      if (mFile != null) {
        sendEmptyMessage(100);
      }
    }

    void exit() {
      removeCallbacksAndMessages(null);
      sendEmptyMessage(0);
      if (mFile != null) {
        try {
          mFile.close();
        } catch (IOException ignored) { }
      }
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {

        // ????????????
        case 0: {
          getLooper().quitSafely();
        }
        break;

        // ??????????????????
        case 100: {
          if (mFile != null) {
            try {
              int count = mFile.read(mBuffer);
              if (count < 0) {
                // PCM???????????????????????????????????????
                sendEmptyMessageDelayed(200, 3000);
              } else {
                Voice.getInstance().write(mBuffer, count);
                // loop
                sendEmptyMessageDelayed(100, 50);
              }
            } catch (IOException e) {
              exit();
            }
          } else {
            exit();
          }
        }
        break;

        // ??????PCM??????????????????????????????exit
        case 200: {
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              final OnVoiceRecognizerListener listener = mListener == null ? null : mListener.get();
              if (listener != null) listener.onRecogError(VoiceRecognizer.NONE, -1, "pcm read completed.");
            }
          });
          exit();
        }
        break;
      }
    }
  }

  private static class SPCMBuffer {
    private short[] mBuffer;

    SPCMBuffer(int size) {
      mBuffer = new short[size];
    }

    void copyFrom(short[] buffer, int size) {
      if (buffer != null && mBuffer != null) {
        System.arraycopy(buffer, 0, mBuffer, 0,
            Math.min(Math.min(buffer.length, mBuffer.length), size));
      }
    }

    short[] getBuffer() {
      return mBuffer;
    }
  }

  public interface AppendDataCallback {
    void onAppendDataCompleted(String file);
  }

  private static class AppendData {
    String publicKey;
    String mp3FileName;
    String content;
    AppendDataCallback callback;
  }


  /////////////////////////////////////////////////////
  /////////////////////////////////////////////////////
  public interface OnAudioRecorderStopListener {
    void onAudioRecorderStoped();
  }
}
