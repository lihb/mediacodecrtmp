package com.example.mediacodecrtmp;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by lihb on 16/1/10.
 */
public class DataManager {

    public LinkedBlockingQueue<byte[]> inputBytesQueue = new LinkedBlockingQueue<byte[]>(40);

    // 音频数据
    public LinkedBlockingQueue<byte[]> inputAudioBytesQueue = new LinkedBlockingQueue<byte[]>(40);

    // 视频参数
    public int videoWidth;
    public int videoHeight;
    public int framerate;
    public byte[] header_sps;
    public byte[] header_pps;


    // 音频参数
    public int channel_count;
    public int sample_rate;
    public int bit_rate;
    public int aac_profile;
    public int csd_0;

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
