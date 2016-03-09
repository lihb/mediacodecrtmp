package com.asha.vrlib;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import com.example.mediacodecrtmp.DataManager;

import javax.microedition.khronos.opengles.GL10;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.asha.vrlib.common.GLUtil.glCheck;


/**
 * copied from surfaceTexture
 * Created by nitro888 on 15. 4. 5..
 * https://github.com/Nitro888/NitroAction360
 */
public class MD360Surface {
    public static final int SURFACE_TEXTURE_EMPTY = 0;
    private static final String TAG = "MD360Surface";

    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private int mGlSurfaceTexture;
    private int mWidth;
    private int mHeight;
    private int mVideoWidth;
    private int mVideoHeight;
    private IOnSurfaceReadyListener mOnSurfaceReadyListener;

    private MediaCodec decoder;
    private MediaFormat mediaFormat;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo info;

    public MD360Surface(IOnSurfaceReadyListener onSurfaceReadyListener) {
        this.mOnSurfaceReadyListener = onSurfaceReadyListener;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void resize(int width, int height) {
        boolean changed = false;
        if (mWidth == width && mHeight == height) changed = true;
        mWidth = width;
        mHeight = height;

        // resize the texture
        if (changed && mSurfaceTexture != null)
            mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);

    }

    public void createSurface() {
        if (mSurface != null) return;

        releaseSurface();
        mGlSurfaceTexture = createTexture();
        if (mGlSurfaceTexture != SURFACE_TEXTURE_EMPTY) {
            //attach the texture to a surface.
            //It's a clue class for rendering an android view to gl level
            mSurfaceTexture = new SurfaceTexture(mGlSurfaceTexture);
            mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
            mSurface = new Surface(mSurfaceTexture);
            if (mOnSurfaceReadyListener != null)
                mOnSurfaceReadyListener.onSurfaceReady(mSurface);
        }
/*
        try {
            decoder = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] header_sps = {0x00, 0x00, 0x00, 0x01, 0x67, 0x64, 0x00, 0x1f, (byte) 0xac, (byte) 0xd9, 0x40, (byte) 0xfc, (byte) 0x10, 0x79, 0x67, (byte) 0x9a, (byte) 0x80, (byte) 0x86, (byte) 0x83, 0x20, 0x00, 0x00,
                0x03, (byte) 0x00, 0x20, 0x00, 0x00, (byte) 0x07, (byte) 0x91, (byte) 0xe3, 0x06, 0x32, (byte) 0xc0};
        byte[] header_pps = {0x00, 0x00, 0x00, 0x01, 0x68, (byte) 0xef, (byte) 0xbc, (byte) 0xb0};
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 1000, 500);
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
        decoder.configure(mediaFormat, mSurface, null, 0);
        if (decoder == null) {
            Log.e(TAG, "decoder == null");
            return;
        }
        decoder.start();*/

    }

    private void releaseSurface() {
        if (mSurface != null) {
            mSurface.release();
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        mSurface = null;
        mSurfaceTexture = null;
    }

    private int createTexture() {
        mGlSurfaceTexture = SURFACE_TEXTURE_EMPTY;
        int[] textures = new int[1];

        // Generate the texture to where android view will be rendered
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        glCheck("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        glCheck("Texture bind");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return textures[0];
    }

    public void onDrawFrame(/*MediaCodec decoder*/) {
        if (mGlSurfaceTexture == SURFACE_TEXTURE_EMPTY)
            return;


        if (DataManager.getInstance().inputBytesQueue.size() > 0) {
            byte[] buf = DataManager.getInstance().inputBytesQueue.poll();
            Log.i(TAG, "inIndex >= 0, inputBytesQueue.poll()");
            int startIndex = 0;
            byte[] temp = null;
            if (buf != null) {
                if (buf[0] == 0x46 && buf[1] == 0x4c && buf[2] == 0x56 && buf[13] == 0x12 && decoder == null) {
                    // onMetadata
                    int i = 0;
                    for (i = 13; i < buf.length; i++) {
                        if (buf[i] == 0x05 && buf[i+1] == 0x77 && buf[i + 2] == 0x69 && buf[i + 3] == 0x64 && buf[i + 4] == 0x74 && buf[i + 5] == 0x68 && buf[i + 6] == 0x00) {
                            // 提取视频宽度
                            temp = new byte[8];
                            System.arraycopy(buf, i + 7, temp, 0, 8);
                            ByteBuffer byteBuffer = ByteBuffer.wrap(temp);
                            mVideoWidth = (int) byteBuffer.getDouble();
                            Log.i(TAG, "mVideoWidth = " + mVideoWidth);
                        } else if (buf[i] == 0x06 && buf[i + 1] == 0x68 && buf[i + 2] == 0x65 && buf[i + 3] == 0x69 && buf[i + 4] == 0x67 && buf[i + 5] == 0x68 && buf[i + 6] == 0x74 && buf[i + 7] == 0x00) {
                            // 提取视频高度
                            temp = new byte[8];
                            System.arraycopy(buf, i + 8, temp, 0, 8);
                            ByteBuffer byteBuffer = ByteBuffer.wrap(temp);
                            mVideoHeight = (int) byteBuffer.getDouble();
                            Log.i(TAG, "mVideoHeight = " + mVideoHeight);
                        }

                    }
                    try {
                        decoder = MediaCodec.createDecoderByType("video/avc");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mediaFormat = MediaFormat.createVideoFormat("video/avc", mVideoWidth, mVideoHeight);
                } else if (buf[0] == 0x09 && (buf[11] == 0x17 || buf[11] == 0x27)) {
                    if (buf[12] == 0x00) {
                        // 提取sps pps，并初始化decoder
                        startIndex = 22;
                        int len = (buf[startIndex] & 0x000000FF) << 8 | buf[startIndex + 1] & 0x000000FF;
                        byte[] header_sps = new byte[len + 4];
                        header_sps[0] = 0;
                        header_sps[1] = 0;
                        header_sps[2] = 0;
                        header_sps[3] = 1;
                        System.arraycopy(buf, startIndex + 2, header_sps, 4, len); // sps

                        startIndex += (len + 2 + 1);
                        len = (buf[startIndex] & 0x000000FF) << 8 | (buf[startIndex + 1] & 0x000000FF);
                        byte[] header_pps = new byte[len + 4];
                        header_pps[0] = 0;
                        header_pps[1] = 0;
                        header_pps[2] = 0;
                        header_pps[3] = 1;
                        System.arraycopy(buf, startIndex + 2, header_pps, 4, len); // pps

                        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
                        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
                        decoder.configure(mediaFormat, mSurface, null, 0);
                        decoder.start();

                        inputBuffers = decoder.getInputBuffers();
                        outputBuffers = decoder.getOutputBuffers();
                        info = new MediaCodec.BufferInfo();

                    } else if (buf[12] == 0x01) {
                        // 提取nalu帧数据
                        startIndex = 16;
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


                        long startMs = System.currentTimeMillis();
                        int sampleSize = temp.length;

                        int inIndex = decoder.dequeueInputBuffer(0);
                        Log.i(TAG, "inIndex = " + inIndex);
                        if (inIndex >= 0) {
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
                                    Log.i("MyActivity", "releaseOutputBuffer");
                                    decoder.releaseOutputBuffer(outIndex, true);
                                    break;

                            }

                            // All decoded frames have been rendered, we can stop playing now
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                Log.i("MyActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                                decoder.stop();
                                decoder.release();
                            }
                        }

                    }

                }
            }
        }


        synchronized (this) {
            mSurfaceTexture.updateTexImage();
        }
    }

    public interface IOnSurfaceReadyListener {
        void onSurfaceReady(Surface surface);
    }
}
