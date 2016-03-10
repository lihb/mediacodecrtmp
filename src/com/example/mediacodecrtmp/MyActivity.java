package com.example.mediacodecrtmp;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MyActivity extends Activity implements SurfaceHolder.Callback {

    private PlayerThread mPlayer = null;

    private byte[] buf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);
        RtmpNative rtmpNative = new RtmpNative(DataManager.getInstance().inputBytesQueue, DataManager.getInstance().inputAudioBytesQueue);
        rtmpNative.naTest();


    }

    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mPlayer == null) {
            mPlayer = new PlayerThread(holder.getSurface());
            mPlayer.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.interrupt();
        }
    }

    private class PlayerThread extends Thread {
        private MediaCodec decoder;
        private Surface surface;

        public PlayerThread(Surface surface) {
            this.surface = surface;
        }

        @Override
        public void run() {
            try {
                decoder = MediaCodec.createDecoderByType("video/avc");
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] header_sps = {0x00, 0x00, 0x00, 0x01, 0x67, 0x42, (byte)0xc0, 0x16, (byte)0x92, 0x54, 0x05, 0x01, (byte)0xed, 0x08, 0x00, 0x00, 0x03, 0x00, 0x08, 0x00, 0x00, 0x03,
                    0x00,  (byte)0xf3, 0x00, 0x00, 0x04,  (byte)0xe7,  (byte)0xc0, 0x00, 0x4e, 0x5e, 0x5e,  (byte)0xf7, 0x00,  (byte)0xf1, 0x62,  (byte)0xea};
            byte[] header_pps = {0x00, 0x00, 0x00, 0x01, 0x68,  (byte)0xce, 0x32, 0x48};
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 640, 480);
            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
            mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
            decoder.configure(mediaFormat, surface, null, 0);

            if (decoder == null) {
                Log.e("MyActivity", "Can't find video info!");
                return;
            }
            decoder.start();

            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()) {
                Log.i("MyActivity", "while start...");
                if (DataManager.getInstance().inputBytesQueue.size() > 0) {
                    Log.i("MyActivity", "before decoder.dequeueInputBuffer(0)");
                    int inIndex = decoder.dequeueInputBuffer(0);
                    Log.i("MyActivity", "inIndex = " + inIndex);
                    if (inIndex >= 0) {
                        buf = DataManager.getInstance().inputBytesQueue.poll();
                        Log.i("MyActivity", "inIndex >= 0, inputBytesQueue.poll()");
                        int startIndex = 0;
                        byte[] temp = null;
                        if (buf != null) {
                            if (buf[11] == 23 && buf[12] == 1) {
                                // I帧数据
                                startIndex = 100;

                            } else if (buf[11] == 39 && buf[12] == 1) {
                                // p帧数据
                                if (buf[25] == 12) {
                                    startIndex = 38;
                                }else if(buf[25] == 11){
                                    startIndex = 37;
                                }
                            }
                            int len = (buf[startIndex] & 0x000000FF) << 24 | (buf[startIndex + 1] & 0x000000FF) << 16 |
                                    (buf[startIndex + 2] & 0x000000FF) << 8 | buf[startIndex + 3] & 0x000000FF;
                            temp = new byte[len + 8];
                            temp[0] = 0;
                            temp[1] = 0;
                            temp[2] = 0;
                            temp[3] = 1;
                            temp[len + 4] = 0;
                            temp[len + 5] = 0;
                            temp[len + 6] = 0;
                            temp[len + 7] = 1;

                            System.arraycopy(buf, startIndex + 4, temp, 4, len);

                            int sampleSize = temp.length;
                            ByteBuffer buffer = inputBuffers[inIndex];
                            buffer.clear();
                            buffer.put(temp, 0, sampleSize);
                            if (sampleSize < 0) {
                                Log.i("MyActivity", "sampleSize < 0");
                                // We shouldn't stop the playback at this point, just pass the EOS
                                // flag to decoder, we will get it again from the
                                // dequeueOutputBuffer
                                Log.d("MyActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, 0, 0);
                            }
                        }
                    }else {
                        Log.i("MyActivity", "inIndex < 0");
                    }
                }


                int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.i("MyActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.i("MyActivity", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.i("MyActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];

                        Log.v("MyActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        Log.i("MyActivity", "releaseOutputBuffer");
                        decoder.releaseOutputBuffer(outIndex, true);
                        break;

                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i("MyActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
                Log.i("MyActivity", "while end..");
            }
            Log.i("MyActivity", "while finished..");
            decoder.stop();
            decoder.release();
        }

    }

}
