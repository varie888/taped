package com.taped.taped;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.taped.camera.preview.RemotePreview;
import com.taped.camera.preview.SelfPreview;
import com.taped.camera.source.ServerThread;
import com.taped.utils.MarshMallowPermission;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.VideoQuality;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ChatActivity extends AppCompatActivity implements View.OnClickListener,
        RtspClient.Callback,
        Session.Callback,
        SurfaceHolder.Callback{
    private static final String TAG = "ChatActivity";

    //private SelfPreview mSelfPreview;
    //private RemotePreview mRemotePreview;

    MarshMallowPermission marshMallowPermission;

    private SurfaceView mSurfaceView;
    private SurfaceView mSurfacePreView;
    private Session mSession;
    private RtspClient mClient;

    private RtspServer mServer;
    private Session mServerSession;

    private static String mServerIP = null;

    public static void setServerIP(String serverIP)
    {
        mServerIP = serverIP;
    }


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private final Handler mHideHandler = new Handler();
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        marshMallowPermission = new MarshMallowPermission(this);

        if (!marshMallowPermission.checkPermissionForCamera()){
            marshMallowPermission.requestPermissionForCamera();
        }

        if (!marshMallowPermission.checkPermissionForExternalStorage()){
            marshMallowPermission.requestPermissionForExternalStorage();
        }

        //mSelfPreview = new SelfPreview(this);
        //mRemotePreview = new RemotePreview(this);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.surface);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        startStreamServer();

        startStreamer();
    }

    private void startStreamer()
    {
        mSurfacePreView = (SurfaceView) findViewById(R.id.preview_surface);

        // Configures the SessionBuilder
        mSession = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                //.setAudioQuality(new AudioQuality(8000,16000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceView(mSurfacePreView)
                .setPreviewOrientation(90)
                .setCallback(this)
                .build();

        // Configures the RTSP client
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);

        mSurfacePreView.getHolder().addCallback(this);

        selectQuality();
    }

    private void startStreamServer()
    {
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);

        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(1234));
        editor.commit();

        //mServer = new RtspServer();
        //mServer.setPort(1234);
        //mServer.setAuthorization("user", "user1234");

        // Configures the SessionBuilder
        mServerSession = SessionBuilder.getInstance()
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264).build();

        RtspServer.mSession = mServerSession;

        //mServer.start();
        // Starts the RTSP server
        this.startService(new Intent(this,RtspServer.class));

        mSurfaceView.getHolder().addCallback(this);

        mServerSession.startPreview();
/*
        try {
            Thread.sleep(3000);
        }
        catch (Exception e) {

        }*/

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        toggleStream();
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    /////// STERAMING /////

    @Override
    public void onClick(View v) {
        //toggleStream();
        /*switch (v.getId()) {
            case R.id.start:
                mLayoutServerSettings.setVisibility(View.GONE);
                toggleStream();
                break;
            case R.id.flash:
                if (mButtonFlash.getTag().equals("on")) {
                    mButtonFlash.setTag("off");
                    mButtonFlash.setImageResource(R.drawable.ic_flash_on_holo_light);
                } else {
                    mButtonFlash.setImageResource(R.drawable.ic_flash_off_holo_light);
                    mButtonFlash.setTag("on");
                }
                mSession.toggleFlash();
                break;
            case R.id.camera:
                mSession.switchCamera();
                break;
            case R.id.settings:
                if (mLayoutVideoSettings.getVisibility() == View.GONE &&
                        mLayoutServerSettings.getVisibility() == View.GONE) {
                    mLayoutServerSettings.setVisibility(View.VISIBLE);
                } else {
                    mLayoutServerSettings.setVisibility(View.GONE);
                    mLayoutVideoSettings.setVisibility(View.GONE);
                }
                break;
            case R.id.video:
                mRadioGroup.clearCheck();
                mLayoutServerSettings.setVisibility(View.GONE);
                mLayoutVideoSettings.setVisibility(View.VISIBLE);
                break;
            case R.id.save:
                mLayoutServerSettings.setVisibility(View.GONE);
                break;
        }*/
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mClient.release();
        mSession.release();
        mServerSession.release();
        mSurfacePreView.getHolder().removeCallback(this);
    }

    private void selectQuality() {
        String text = "320x240, 30 fps, 250 Kbps";
        Pattern pattern = Pattern.compile("(\\d+)x(\\d+)\\D+(\\d+)\\D+(\\d+)");
        Matcher matcher = pattern.matcher(text);

        matcher.find();
        int width = Integer.parseInt(matcher.group(1));
        int height = Integer.parseInt(matcher.group(2));
        int framerate = Integer.parseInt(matcher.group(3));
        int bitrate = Integer.parseInt(matcher.group(4))*1000;

        mSession.setVideoQuality(new VideoQuality(width, height, framerate, bitrate));
        //Toast.makeText(this, ((RadioButton)findViewById(id)).getText(), Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Selected resolution: "+width+"x"+height);
    }

    private void enableUI() {
        /*mButtonStart.setEnabled(true);
        mButtonCamera.setEnabled(true);*/
    }

    // Connects/disconnects to the RTSP server and starts/stops the stream
    public void toggleStream() {
        //mProgressBar.setVisibility(View.VISIBLE);
        if (!mClient.isStreaming()) {
            //String ip,port,path;

/*            // We save the content user inputs in Shared Preferences
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this);
            SharedPreferences.Editor editor = mPrefs.edit();
            String uri = mServerIP + ":1234";
            editor.putString("uri", uri);
            editor.putString("password", "");
            editor.putString("username", "");
            editor.commit();

            // We parse the URI written in the Editext
            Pattern uri = Pattern.compile("rtsp://(.+):(\\d*)/(.+)");
            Matcher m = uri.matcher(mEditTextURI.getText()); m.find();
            ip = m.group(1);
            port = m.group(2);
            path = m.group(3);*/

            if (mServerIP == null)
                return;

            //mClient.setCredentials("user", "user1234");
            mClient.setServerAddress(mServerIP, 1234);
            //mClient.setStreamPath("/");
            mClient.startStream();

        } else {
            // Stops the stream and disconnects from the RTSP server
            mClient.stopStream();
        }
    }

    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        // Displays a popup to report the eror to the user
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        builder.setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBitrateUpdate(long bitrate) {
        //mTextBitrate.setText(""+bitrate/1000+" kbps");
    }

    @Override
    public void onPreviewStarted() {
        /*if (mSession.getCamera() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mButtonFlash.setEnabled(false);
            mButtonFlash.setTag("off");
            mButtonFlash.setImageResource(R.drawable.ic_flash_on_holo_light);
        }
        else {
            mButtonFlash.setEnabled(true);
        }*/
    }

    @Override
    public void onSessionConfigured() {

    }

    @Override
    public void onSessionStarted() {
        enableUI();
        /*mButtonStart.setImageResource(R.drawable.ic_switch_video_active);
        mProgressBar.setVisibility(View.GONE);*/
    }

    @Override
    public void onSessionStopped() {
        enableUI();
        /*mButtonStart.setImageResource(R.drawable.ic_switch_video);
        mProgressBar.setVisibility(View.GONE);*/
    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {
        //mProgressBar.setVisibility(View.GONE);
        switch (reason) {
            case Session.ERROR_CAMERA_ALREADY_IN_USE:
                break;
            case Session.ERROR_CAMERA_HAS_NO_FLASH:
                /*mButtonFlash.setImageResource(R.drawable.ic_flash_on_holo_light);
                mButtonFlash.setTag("off");*/
                break;
            case Session.ERROR_INVALID_SURFACE:
                break;
            case Session.ERROR_STORAGE_NOT_READY:
                break;
            case Session.ERROR_CONFIGURATION_NOT_SUPPORTED:
                VideoQuality quality = mSession.getVideoTrack().getVideoQuality();
                logError("The following settings are not supported on this phone: "+
                        quality.toString()+" "+
                        "("+e.getMessage()+")");
                e.printStackTrace();
                return;
            case Session.ERROR_OTHER:
                break;
        }

        if (e != null) {
            logError(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onRtspUpdate(int message, Exception e) {
        switch (message) {
            case RtspClient.ERROR_CONNECTION_FAILED:
            case RtspClient.ERROR_WRONG_CREDENTIALS:
                //mProgressBar.setVisibility(View.GONE);
                enableUI();
                logError(e.getMessage());
                e.printStackTrace();
                break;
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSession.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mClient.stopStream();
        mServer.stop();
    }
}
