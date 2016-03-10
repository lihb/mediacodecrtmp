package com.example.mediacodecrtmp;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by lihb on 16/1/10.
 */
public class DataManager {

    public LinkedBlockingQueue<byte[]> inputBytesQueue = new LinkedBlockingQueue<byte[]>(40);
    public LinkedBlockingQueue<byte[]> inputAudioBytesQueue = new LinkedBlockingQueue<byte[]>(40);


    private static DataManager mInstance = null;

    private DataManager() {

    }

    public static synchronized DataManager getInstance(){
        if (mInstance == null) {
            mInstance = new DataManager();
        }
        return mInstance;
    }




}
