package com.example.mediacodecrtmp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import com.asha.vrlib.MD360Renderer;
import com.asha.vrlib.MD360Surface;
import com.asha.vrlib.common.GLUtil;

/**
 * Created by Administrator on 2016/3/24.
 */
public class VRPlayer {

    private static final String TAG = "VRPlayer";
    private GLSurfaceView mGLSurfaceView;
    private MD360Renderer mRenderer;
    private RtmpNative mRtmpNative;
    private AudioDecoder mAudioDecoder;

    private Context mContext;

    public VRPlayer(GLSurfaceView gLSurfaceView,Context context) {
        this.mGLSurfaceView = gLSurfaceView;
        this.mContext = context;

        mRtmpNative = new RtmpNative(DataManager.getInstance().inputBytesQueue, DataManager.getInstance().inputAudioBytesQueue);
        mAudioDecoder = new AudioDecoder();

        mRenderer = MD360Renderer.with(mContext)
                .defaultSurface(new MD360Surface.IOnSurfaceReadyListener() {
                    @Override
                    public void onSurfaceReady(Surface surface) {
//

                    }
                })
                .build();

        // init OpenGL
        initOpenGL();
    }

    private void initOpenGL() {

        if (GLUtil.supportsEs2(mContext)) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2);

            // Set the renderer to our demo renderer, defined below.
            mGLSurfaceView.setRenderer(mRenderer);
        } else {
            mGLSurfaceView.setVisibility(View.GONE);
            Toast.makeText(mContext, "OpenGLES2 not supported.", Toast.LENGTH_SHORT).show();
        }
    }

    public void startPlay(String rtmpUrl) {
        mRtmpNative.naTest(rtmpUrl);
        mAudioDecoder.start();
    }

    public void stop() {
        if (mRenderer != null) {
            mRenderer.release();
        }
        Log.i(TAG, "stop");
        mRtmpNative.naStopThread();
        mAudioDecoder.stopAudioThread();
        DataManager.getInstance().inputBytesQueue.clear();
        DataManager.getInstance().inputAudioBytesQueue.clear();
    }

    public void pause() {
        Log.i(TAG, "pause");
        mGLSurfaceView.onPause();
    }

    public void resume() {
        Log.i(TAG, "resume");
        mGLSurfaceView.onResume();
    }

    public boolean playerTouchEvent(MotionEvent event) {
        return mRenderer.handleTouchEvent(event);
    }
}
