//package com.scliang.core.media;
//
///**
// * SCore
// * Created by ShangChuanliang
// * on 2017/10/6.
// */
//public final class MediaNUtils2 implements IMediaNUtils {
//    private MediaNUtils2() {
//    }
//
//    private static class SingletonHolder {
//        private static final MediaNUtils2 INSTANCE = new MediaNUtils2();
//    }
//
//    public static MediaNUtils2 getInstance() {
//        return MediaNUtils2.SingletonHolder.INSTANCE;
//    }
//
//    private boolean mInit;
//    public boolean isInit() { return mInit; }
//
//    @Override
//    public boolean webrtc_createApm() {
//        return false;
//    }
//
//    /**
//     * 加载libjmedia2.so
//     * @return true:加载成功
//     */
//    public boolean init() {
//        try {
//            System.loadLibrary("jmedia2");
//            mInit = true;
//        } catch (Throwable e) {
//            mInit = false;
//        }
//        return mInit;
//    }
//
//
//
//
//    // webrtc
//    public native boolean webrtc_createApm(
//            boolean aecExtendFilter, boolean speechIntelligibilityEnhance,
//            boolean delayAgnostic, boolean beamforming,
//            boolean nextGenerationAec, boolean experimentalNs,
//            boolean experimentalAgc);
//    public native void webrtc_freeApm();
//    public native int webrtc_highPassFilterEnable(boolean enable);
//    public native int webrtc_aecEnable(boolean enable);
//    public native int webrtc_aecClockDriftCompensationEnable(boolean enable);
//    public native int webrtc_aecSetSuppressionLevel(int level);
//    public native int webrtc_aecmEnable(boolean enable);
//    public native int webrtc_aecmSetSuppressionLevel(int level);
//    public native int webrtc_nsSetLevel(int level);
//    public native int webrtc_nsSetHighLevel();
//    public native int webrtc_nsEnable(boolean enable);
//    public native int webrtc_agcSetAnalogLevelLimits(int minimum, int maximum);
//    public native int webrtc_agcSetMode(int mode);
//    public native int webrtc_agcSetModeFixedDigital();
//    public native int webrtc_agcSetTargetLevelDbfs(int level);
//    public native int webrtc_agcSetCompressionGainDb(int gain);
//    public native int webrtc_agcEnableLimiter(boolean enable);
//    public native int webrtc_agcEnable(boolean enable);
//    public native int webrtc_agcSetStreamAnalogLevel(int level);
//    public native int webrtc_agcStreamAnalogLevel();
//    public native int webrtc_vadEnable(boolean enable);
//    public native int webrtc_vadSetLikelihood(int likelihood);
//    public native boolean webrtc_vadStreamHasVoice();
//    public native int webrtc_processStream(short[] input);
//    public native int webrtc_processReverseStream(short[] input);
//    public native int webrtc_setStreamDelayMs(int delay);
//
//
//
//
//    @Override
//    public boolean webrtc_setupApm16K() {
//        return false;
//    }
//
//    @Override
//    public boolean webrtc_setupApm441K() {
//        return false;
//    }
//
//    @Override
//    public int webrtc_processStream16K(short[] data) {
//        return 0;
//    }
//
//    @Override
//    public int webrtc_processStream441K(short[] data) {
//        return 0;
//    }
//
//
//
//
//    // speex
//    public native int speex_open(int compression);
//    public native int speex_encode(short[] lin, int offset, byte[] encoded, int size);
//    public native int speex_decode(byte[] encoded, short[] lin, int size);
//    public native int speex_getFrameSize();
//    public native void speex_close();
//}
