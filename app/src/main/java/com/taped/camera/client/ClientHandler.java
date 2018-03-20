package com.taped.camera.client;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.taped.taped.ChatActivity;

import java.lang.ref.WeakReference;

/**
 * Created by varie on 16/03/2018.
 */
public class ClientHandler extends Handler {
    /*private final WeakReference<ChatActivity> mActivity;

    public ClientHandler(ChatActivity activity) {
        mActivity = new WeakReference<ChatActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        ChatActivity activity = mActivity.get();
        if (activity != null) {
            try {
                //activity.setLastFrame((Bitmap) msg.obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.handleMessage(msg);
        }
    }*/
}
