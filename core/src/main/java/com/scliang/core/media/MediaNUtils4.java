package com.scliang.core.media;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2020/7/13.
 */
public final class MediaNUtils4 implements IMediaNUtils {
    private MediaNUtils4() {
    }

    private static class SingletonHolder {
        private static final MediaNUtils4 INSTANCE = new MediaNUtils4();
    }

    public static MediaNUtils4 getInstance() {
        return MediaNUtils4.SingletonHolder.INSTANCE;
    }

    private boolean mInit;

    @Override
    public boolean isInit() { return mInit; }

    /**
     * 加载libjmedia4.so
     * @return true:加载成功
     */
    @Override
    public boolean init() {
        try {
            System.loadLibrary("jmedia4");
            mInit = true;
        } catch (Throwable e) {
            mInit = false;
        }
        return mInit;
    }

    // info
    @Override
    public String desc() {
        return "MediaNUtils4";
    }

    // file
    @Override
    public native int createWavFileAndWritePCMData(String file, int sampleRate,
                                                   byte[] data, int size);
    @Override
    public native int createMp3FileAndWritePCMData(String file, int sampleRate,
                                                   byte[] data, int size);

    // webrtc
    @Override
    public native boolean webrtcCreateApm();
    @Override
    public native void webrtcFreeApm();
    @Override
    public native boolean webrtcSetupApm16K(int mode);
    @Override
    public native boolean webrtcSetupApm441K(int mode);
    @Override
    public native int webrtcProcessStream(short[] data);
    @Override
    public native void webrtcFileProcess(String inFile, String outFile, int mode);
}
