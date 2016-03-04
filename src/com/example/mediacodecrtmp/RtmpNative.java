package com.example.mediacodecrtmp;

import android.os.Environment;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by lihb on 16/2/29.
 */
public class RtmpNative {

    private static LinkedBlockingQueue<byte[]> inputQueue;

    public RtmpNative(LinkedBlockingQueue<byte[]> inputQueue) {
        this.inputQueue = inputQueue;
    }

    public static boolean offer(byte[] inputbuf) {
        Log.i("lihb test----- offer()", inputQueue.toString());
        return inputQueue.offer(inputbuf);
    }


    public native int naTest();

    static {
        System.loadLibrary("rtmp-0");
        System.loadLibrary("rtmp_jni");
    }
}
