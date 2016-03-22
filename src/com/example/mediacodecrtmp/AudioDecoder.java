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

    private boolean isThreadStop = false;

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
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 524288);
//        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
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
                AudioFormat.ENCODING_PCM_16BIT, minSize*2, AudioTrack.MODE_STREAM);

        // start playing, we will feed the AudioTrack later
        audioTrack.play();
        byte[] temp = null;
        isThreadStop = true;
        while (isThreadStop) {
//           Log.i(TAG, "in AudioDecoder  while...");
           if (DataManager.getInstance().inputAudioBytesQueue.size() > 0) {
                byte[] audioData = DataManager.getInstance().inputAudioBytesQueue.poll();// 提取出音频数据

                int inputBufferIndex = mMediaDecode.dequeueInputBuffer(0);
                if (inputBufferIndex >= 0) {
                    int len = -1;
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    if(audioData[0] == 0x08 && audioData[12] == 0x01){
                        len = (audioData[2] & 0x000000FF) << 8 | audioData[3] & 0x000000FF;

                        // 去除 af 01两个字节后的数据喂给解码器
                        temp = new byte[len-2];
                        System.arraycopy(audioData, 13, temp, 0 ,len-2);

                        inputBuffer.put(temp, 0, temp.length);
                        mMediaDecode.queueInputBuffer(inputBufferIndex, 0, temp.length, 0, 0);
                    }

                }



               int outIndex = mMediaDecode.dequeueOutputBuffer(info, 1000);
               switch (outIndex) {
                   case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                       Log.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                       outputBuffers = mMediaDecode.getOutputBuffers();
                       break;
                   case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                       Log.i(TAG, "New format " + mMediaDecode.getOutputFormat());
                       break;
                   case MediaCodec.INFO_TRY_AGAIN_LATER:
                       Log.i(TAG, "dequeueOutputBuffer timed out!");
                       break;
                   default:
                       ByteBuffer outputBuffer = outputBuffers[outIndex];

//                       Log.v(TAG, "We can't use this buffer but render it due to the API limit, " + outputBuffer);
                       outputBuffer.position(0);
                       final byte[] outData = new byte[info.size];
                       outputBuffer.get(outData);
                       audioTrack.write(outData, 0, outData.length);
//                       Log.i(TAG, outData.length + " bytes decoded");
//                       Log.i(TAG, "releaseOutputBuffer");
                       mMediaDecode.releaseOutputBuffer(outIndex, false);
                       break;

               }

               // All decoded frames have been rendered, we can stop playing now
               if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                   Log.i(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                   mMediaDecode.stop();
                   mMediaDecode.release();
                   if(audioTrack != null) {
                       audioTrack.flush();
                       audioTrack.release();
                       audioTrack = null;
                   }
               }
            }

        }

        Log.i(TAG, "AudioDecoder Thread is stopped....");

    }

    public void stopAudioThread() {
        isThreadStop = false;
    }
}
