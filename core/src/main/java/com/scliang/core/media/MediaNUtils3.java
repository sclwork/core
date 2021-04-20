package com.scliang.core.media;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2020/7/11.
 */
public final class MediaNUtils3 implements IMediaNUtils {
    private MediaNUtils3() {
    }

    private static class SingletonHolder {
        private static final MediaNUtils3 INSTANCE = new MediaNUtils3();
    }

    public static MediaNUtils3 getInstance() {
        return MediaNUtils3.SingletonHolder.INSTANCE;
    }

    private boolean mInit;

    @Override
    public boolean isInit() { return mInit; }

    /**
     * 加载libjmedia3.so
     * @return true:加载成功
     */
    @Override
    public boolean init() {
        try {
            System.loadLibrary("jmedia3");
            mInit = true;
        } catch (Throwable e) {
            mInit = false;
        }
        return mInit;
    }

    // info
    @Override
    public String desc() {
        return "MediaNUtils3";
    }

    // file
    @Override
    public int createWavFileAndWritePCMData(String file, int sampleRate,
                                            byte[] data, int size) {
        throw new RuntimeException("Is not Support in MediaNUtils3.");
    }
    @Override
    public int createMp3FileAndWritePCMData(String file, int sampleRate,
                                            byte[] data, int size) {
        throw new RuntimeException("Is not Support in MediaNUtils3.");
    }

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
    public void webrtcFileProcess(String inFile, String outFile, int mode) {
        throw new RuntimeException("Is not Support in MediaNUtils3.");
    }
}
