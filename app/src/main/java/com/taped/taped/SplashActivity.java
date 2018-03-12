package com.taped.taped;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.taped.preferences.LoginSharedPreferences;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends AppCompatActivity {

    private final int STR_SPLASH_TIME = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        startSplashTimer();
    }

    private void startSplashTimer() {
        try {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {

                    Intent intent = null;
                    if (LoginSharedPreferences.getUserName(SplashActivity.this).length() == 0)
                    {
                        intent = new Intent(SplashActivity.this, LoginActivity.class);
                    }
                    else
                    {
                        intent = new Intent(SplashActivity.this, MainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }
            }, STR_SPLASH_TIME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
