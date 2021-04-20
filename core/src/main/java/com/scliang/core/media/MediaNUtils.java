package com.scliang.core.media;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/6.
 */
public final class MediaNUtils {
    static {
        System.loadLibrary("jmedia");
    }




    // lamemp3
    public static void lamemp3Init(int inSampleRate, int outChannel, int outSampleRate, int outBitrate)
                                { lamemp3Init(inSampleRate, outChannel, outSampleRate, outBitrate, 7); }
    public native static void lamemp3Init(int inSampleRate, int outChannel, int outSampleRate, int outBitrate, int quality);
    public native static int lamemp3Encode(short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf);
    public native static int lamemp3Flush(byte[] mp3buf);
    public native static void lamemp3Close();




    // pcm
    public native static void pcmAdjustVolume(short[] buffer, int size, float factor);
    public native static double pcmDB(short[] buffer, int size);
}
