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
import android.widget.TextView;
import android.widget.Toast;

import com.taped.communication.CallCommunicator;
import com.taped.communication.ICallCallback;
import com.taped.communication.IListenCallback;
import com.taped.utils.MarshMallowPermission;
import com.taped.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainActivityListenCallback mListenCB;
    private MainActivityCallCallback mCallCB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    public class MainActivityCallCallback implements ICallCallback {
        public void callback(boolean approved)
        {
            if (approved) {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
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

}
