package com.example.mediacodecrtmp;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by lihb on 16/2/29.
 */
public class RtmpNative {

    private static LinkedBlockingQueue<byte[]> inputQueue;
    private static LinkedBlockingQueue<byte[]> inputAudioQueue;

    public RtmpNative(LinkedBlockingQueue<byte[]> inputQueue, LinkedBlockingQueue<byte[]> inputAudioQueue) {
        this.inputQueue = inputQueue;
        this.inputAudioQueue = inputAudioQueue;
    }

    public static boolean offerAvcData(byte[] inputbuf) {
        Log.i("test offerAvcData()", inputQueue.toString());
        return inputQueue.offer(inputbuf);
    }

    public static boolean offerAudioData(byte[] inputbuf) {
//        Log.i("test offerAudioData()", inputAudioQueue.toString());
        return inputAudioQueue.offer(inputbuf);
    }

    public native int naTest(String rtmpAddress);

    public native void naStopThread();

    static {
        System.loadLibrary("rtmp-0");
        System.loadLibrary("rtmp_jni");
    }
}
