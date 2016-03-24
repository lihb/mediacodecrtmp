package com.example.mediacodecrtmp;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by Administrator on 2016/3/4.
 */
public class VRActivity extends Activity {

    private static final String TAG = "VRActivity";
    private VRPlayer mVRPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_md_render);

        String playAddress = getIntent().getStringExtra("rtmpAddress");
        if (playAddress == null || playAddress.isEmpty()) {
            playAddress = "rtmp://183.60.140.6/ent/91590716_91590716_10057";
        }
        GLSurfaceView glSurfaceView = (GLSurfaceView) findViewById(R.id.surface_view);
        mVRPlayer = new VRPlayer(glSurfaceView, VRActivity.this);
        mVRPlayer.startPlay(playAddress);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mVRPlayer.playerTouchEvent(event) || super.onTouchEvent(event);
    }


    @Override
    protected void onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mVRPlayer.resume();
    }

    @Override
    protected void onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mVRPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVRPlayer.stop();
    }
}
