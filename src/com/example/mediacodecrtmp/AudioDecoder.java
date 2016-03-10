package com.example.mediacodecrtmp;

import android.media.*;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2016/3/9.
 */
public class AudioDecoder extends Thread{

    private static final String TAG = "AudioDecoder";
    private MediaCodec mMediaDecode;

    private MediaFormat format;

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;

    private MediaCodec.BufferInfo info;


    private AudioTrack audioTrack;

    @Override
    public void run() {
        Log.i(TAG, "in AudioDecoder thread");
        try {
            mMediaDecode = MediaCodec.createDecoderByType("audio/mp4a-latm");
        } catch (IOException e) {
            e.printStackTrace();
        }
        format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 441000);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 16);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mMediaDecode.configure(format, null, null, 0);
        mMediaDecode.start();

        inputBuffers = mMediaDecode.getInputBuffers();
        outputBuffers = mMediaDecode.getOutputBuffers();
        info = new MediaCodec.BufferInfo();

        // configure AudioTrack
        int channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
        int minSize = AudioTrack.getMinBufferSize(441000, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 441000, channelConfiguration,
                AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);

        // start playing, we will feed the AudioTrack later
        audioTrack.play();
        byte[] temp = null;

        while (!Thread.interrupted()) {
            if (DataManager.getInstance().inputAudioBytesQueue.size() > 0) {
                byte[] audioData = DataManager.getInstance().inputAudioBytesQueue.poll();// 提取出音频数据

                int inputBufferIndex = mMediaDecode.dequeueInputBuffer(0);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    if (audioData[0] == 0x08 && audioData[12] == 0x00) {
                        byte[] fisrtData = new byte[2];
                        fisrtData[0] = audioData[13];
                        fisrtData[1] = audioData[14];
                        inputBuffer.put(fisrtData);
                    } else if(audioData[0] == 0x08 && audioData[12] == 0x01){
                        int len = (audioData[2] & 0x000000FF) << 8 | audioData[3] & 0x000000FF;
                        temp = new byte[len];
                        System.arraycopy(audioData, 11, temp, 0 ,len);

                        inputBuffer.put(temp);
                    }

                    mMediaDecode.queueInputBuffer(inputBufferIndex, 0, audioData.length, System.nanoTime() / 1000, 0);
                }


                int outputBufferIndex = mMediaDecode.dequeueOutputBuffer(info, 1000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    outputBuffer.position(0);
                    byte[] outData = new byte[info.size];
                    outputBuffer.get(outData);
                    audioTrack.write(outData, 0, outData.length);
                    Log.i(TAG, outData.length + " bytes decoded");
                }
            }

        }

    }
}
