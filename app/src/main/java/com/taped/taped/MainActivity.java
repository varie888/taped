package com.taped.taped;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.taped.communication.CallCommunicator;
import com.taped.communication.ICallCallback;
import com.taped.communication.IListenCallback;
import com.taped.utils.MarshMallowPermission;
import com.taped.utils.Utils;
import com.taped.utils.VideoParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements
        RadioGroup.OnCheckedChangeListener{

    private MainActivityListenCallback mListenCB;
    private MainActivityCallCallback mCallCB;

    private RadioGroup mRadioGroup;

    private VideoParameters mVidParams = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRadioGroup =  (RadioGroup) findViewById(R.id.radio);
        mRadioGroup.setOnCheckedChangeListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final AutoCompleteTextView remoteiptv = (AutoCompleteTextView)findViewById(R.id.remoteip);

        TextView iptv = (TextView)findViewById(R.id.my_ip_id);

        final String myip = Utils.getIPAddress(getApplicationContext());
        iptv.setText("My IP: " + myip);

        mListenCB = new MainActivityListenCallback();
        CallCommunicator.listen(mListenCB);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.stream_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Calling...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                String remoteip = remoteiptv.getText().toString();

                //ChatActivity.setServerIP(remoteip);

                mCallCB = new MainActivityCallCallback();
                // calling remote party
                CallCommunicator.call(mCallCB, myip, remoteip);
            }
        });

        selectQuality();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        selectQuality();
    }

    public class MainActivityCallCallback implements ICallCallback {
        public void callback(boolean approved)
        {
            if (approved) {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("Width", mVidParams.Width);
                intent.putExtra("Height", mVidParams.Height);
                intent.putExtra("Framerate", mVidParams.Framerate);
                intent.putExtra("Bitrate", mVidParams.Bitrate);
                startActivity(intent);
                finish();
            }
        }
    }

    public class MainActivityListenCallback implements IListenCallback {
        public boolean callback(String callstr)
        {
            String ip = callstr.split(" ")[0];

            ChatActivity.setServerIP(ip);

            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
            finish();

            return true;
        }
    }

    private void selectQuality() {
        int id = mRadioGroup.getCheckedRadioButtonId();
        RadioButton button = (RadioButton) findViewById(id);
        if (button == null) return;

        String text = button.getText().toString();
        Pattern pattern = Pattern.compile("(\\d+)x(\\d+)\\D+(\\d+)\\D+(\\d+)");
        Matcher matcher = pattern.matcher(text);

        matcher.find();
        int width = Integer.parseInt(matcher.group(1));
        int height = Integer.parseInt(matcher.group(2));
        int framerate = Integer.parseInt(matcher.group(3));
        int bitrate = Integer.parseInt(matcher.group(4))*1000;

        mVidParams = new VideoParameters();
        mVidParams.Width = width;
        mVidParams.Height = height;
        mVidParams.Framerate = framerate;
        mVidParams.Bitrate = bitrate;

        //mSession.setVideoQuality(new VideoQuality(width, height, framerate, bitrate));
        Toast.makeText(this, ((RadioButton)findViewById(id)).getText(), Toast.LENGTH_SHORT).show();
    }
}
