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
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
//        format.setInteger(MediaFormat.KEY_BIT_RATE, 128);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setByteBuffer("csd-0",ByteBuffer.wrap(new byte[]{0x12, (byte)0x10}));
        mMediaDecode.configure(format, null, null, 0);
        mMediaDecode.start();

        inputBuffers = mMediaDecode.getInputBuffers();
        outputBuffers = mMediaDecode.getOutputBuffers();
        info = new MediaCodec.BufferInfo();

        // configure AudioTrack
        int channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
        int minSize = AudioTrack.getMinBufferSize(44100, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, channelConfiguration,
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

                        // test1:AF开头所有数据喂给解码器
//                        temp = new byte[len];
//                        System.arraycopy(audioData, 11, temp, 0 ,len);

                        // test2:  -2：去除 af 01两个字节；+7：adts的7个字节
                        int packetLen = len -2 + 7; // -2：去除 af 01两个字节；+7：adts的7个字节
                        temp = new byte[packetLen];

                        int profile = 2;  //AAC LC
                                          //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
                        int freqIdx = 4;  //44.1KHz
                        int chanCfg = 2;  //CPE


                        // fill in ADTS data
//                        temp[0] = (byte)0xFF;
//                        temp[1] = (byte)0xF9;
//                        temp[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
//                        temp[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
//                        temp[4] = (byte)((packetLen&0x7FF) >> 3);
//                        temp[5] = (byte)(((packetLen&7)<<5) + 0x1F);
//                        temp[6] = (byte)0xFC;
//                        System.arraycopy(audioData, 13, temp, 7 ,len - 2);

                        // test3:  增加af 01两个字节；和adts的7个字节
//                        temp = new byte[len+7];
//                        temp[0] = (byte)0xAF;
//                        temp[1] = (byte)0x01;
//
//                        temp[2] = (byte)0xFF;
//                        temp[3] = (byte)0xF9;
//                        temp[4] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
//                        temp[5] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
//                        temp[6] = (byte)((packetLen&0x7FF) >> 3);
//                        temp[7] = (byte)(((packetLen&7)<<5) + 0x1F);
//                        temp[8] = (byte)0xFC;
//
//                        System.arraycopy(audioData, 13, temp, 9 ,len - 2);

                        // test4:去除 af 01两个字节后的数据喂给解码器
                        temp = new byte[len-2];
                        System.arraycopy(audioData, 13, temp, 0 ,len-2);

                        inputBuffer.put(temp);
                    }

                    mMediaDecode.queueInputBuffer(inputBufferIndex, 0, audioData.length, System.nanoTime() / 1000, 0);
                }


               /* int outputBufferIndex = mMediaDecode.dequeueOutputBuffer(info, 1000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    outputBuffer.position(0);
                    byte[] outData = new byte[info.size];
                    outputBuffer.get(outData);
                    audioTrack.write(outData, 0, outData.length);
                    Log.i(TAG, outData.length + " bytes decoded");
                }*/
               long startMs = System.currentTimeMillis();
               int outIndex = mMediaDecode.dequeueOutputBuffer(info, 10000);
               switch (outIndex) {
                   case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                       Log.i("MyActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                       outputBuffers = mMediaDecode.getOutputBuffers();
                       break;
                   case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                       Log.i("MyActivity", "New format " + mMediaDecode.getOutputFormat());
                       break;
                   case MediaCodec.INFO_TRY_AGAIN_LATER:
                       Log.i("MyActivity", "dequeueOutputBuffer timed out!");
                       break;
                   default:
                       ByteBuffer outputBuffer = outputBuffers[outIndex];

                       Log.v("MyActivity", "We can't use this buffer but render it due to the API limit, " + outputBuffer);

                       // We use a very simple clock to keep the video FPS, or the video
                       // playback will be too fast
                       while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                           try {
                               Thread.sleep(20);
                           } catch (InterruptedException e) {
                               e.printStackTrace();
                               break;
                           }
                       }
                       outputBuffer.position(0);
                       byte[] outData = new byte[info.size];
                       outputBuffer.get(outData);
                       audioTrack.write(outData, 0, outData.length);
                       Log.i(TAG, outData.length + " bytes decoded");
                       Log.i("MyActivity", "releaseOutputBuffer");
                       mMediaDecode.releaseOutputBuffer(outIndex, true);
                       break;

               }

               // All decoded frames have been rendered, we can stop playing now
               if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                   Log.i("MyActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                   mMediaDecode.stop();
                   mMediaDecode.release();
               }
            }

        }

    }
}
