package com.example.mediacodecrtmp;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.asha.vrlib.MD360Renderer;
import com.asha.vrlib.MD360Surface;
import com.asha.vrlib.common.GLUtil;

/**
 * Created by Administrator on 2016/3/4.
 */
public class VRActivity extends Activity {

    private static final String TAG = "VRActivity";
    private GLSurfaceView mGLSurfaceView;
    private MD360Renderer mRenderer;
    private RtmpNative mRtmpNative;
    private AudioDecoder mAudioDecoder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_md_render);

        mRtmpNative = new RtmpNative(DataManager.getInstance().inputBytesQueue, DataManager.getInstance().inputAudioBytesQueue);
        mRtmpNative.naTest("rtmp://183.60.140.6/ent/91590716_91590716_10057");
        mAudioDecoder = new AudioDecoder();
        mAudioDecoder.start();

        mRenderer = MD360Renderer.with(this)
                .defaultSurface(new MD360Surface.IOnSurfaceReadyListener() {
                    @Override
                    public void onSurfaceReady(Surface surface) {
//                        initDecoder(surface);
                    }
                })
                .build();
        // init OpenGL
        initOpenGL(R.id.surface_view);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mRenderer.handleTouchEvent(event) || super.onTouchEvent(event);
    }

    private void initOpenGL(int glSurfaceViewResId) {
        mGLSurfaceView = (GLSurfaceView) findViewById(glSurfaceViewResId);

        if (GLUtil.supportsEs2(this)) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2);

            // Set the renderer to our demo renderer, defined below.
            mGLSurfaceView.setRenderer(mRenderer);
        } else {
            mGLSurfaceView.setVisibility(View.GONE);
            Toast.makeText(VRActivity.this, "OpenGLES2 not supported.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRenderer != null) {
            mRenderer.release();
        }
        Log.i(TAG, "onDestory");
        mRtmpNative.naStopThread();
        mAudioDecoder.stopAudioThread();
        DataManager.getInstance().inputBytesQueue.clear();
        DataManager.getInstance().inputAudioBytesQueue.clear();
    }
}
