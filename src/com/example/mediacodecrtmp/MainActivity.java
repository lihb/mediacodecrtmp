package com.example.mediacodecrtmp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by Administrator on 2016/3/23.
 */
public class MainActivity extends Activity{

    public static final String TAG = "MainActivity";


    private Button btnStart = null;

    private EditText mRtmpEditTxt = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        btnStart = (Button) findViewById(R.id.button1);
        mRtmpEditTxt = (EditText) findViewById(R.id.rtmpEditTxt);

        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String rtmpVideoPath = mRtmpEditTxt.getText().toString().trim();
                Log.i(TAG, "rtmp地址是:" + rtmpVideoPath);

                Intent intent = new Intent(MainActivity.this, VRActivity.class);
                intent.putExtra("playAddress", rtmpVideoPath);
                startActivity(intent);
            }
        });

    }
}
