package com.example.administrator.uwbbluetooth;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class StartActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGHT = 3000; // 延迟六秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                Intent mainIntent = new Intent();
                mainIntent.setClass(StartActivity.this,MainActivity.class);
                StartActivity.this.startActivity(mainIntent);
                StartActivity.this.finish();
            }

        }, SPLASH_DISPLAY_LENGHT);

    }


}
