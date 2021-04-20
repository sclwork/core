package com.scliang.core.media;

/**
 * Score
 * Created by ShangChuanliang
 * on 2020/07/14.
 */
public interface IMediaNUtils {
  // init
  boolean init();
  boolean isInit();

  // info
  String desc();

  // file
  // Allow for modes: 0, 1, 2, 3.
  int createWavFileAndWritePCMData(String file, int sampleRate,
                                   byte[] data, int size);
  // Allow for modes: 0, 1, 2, 3.
  int createMp3FileAndWritePCMData(String file, int sampleRate,
                                   byte[] data, int size);

  // webrtc
  boolean webrtcCreateApm();
  void webrtcFreeApm();
  // Allow for modes: 0, 1, 2, 3.
  boolean webrtcSetupApm16K(int mode);
  // Allow for modes: 0, 1, 2, 3.
  boolean webrtcSetupApm441K(int mode);
  int webrtcProcessStream(short[] data);
  // Allow for modes: 0, 1, 2, 3.
  void webrtcFileProcess(String inFile, String outFile, int mode);
}
