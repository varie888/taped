package com.taped.camera.preview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.taped.camera.client.ClientHandler;
import com.taped.camera.client.ClientThread;
import com.taped.taped.ChatActivity;
import com.taped.taped.R;

import java.lang.ref.WeakReference;
import java.net.Socket;

/**
 * Created by varie on 16/03/2018.
 */
public class RemotePreview {
    private ImageView mCameraView;
    private ClientThread mClient;

    private static String mServerIP = null;

    private final WeakReference<ChatActivity> mActivity;

    public static void setServerIP(String mServerIP) {
        RemotePreview.mServerIP = mServerIP;
    }

    public Bitmap getLastFrame() {
        return mLastFrame;
    }

    public void setLastFrame(Bitmap lastFrame) {
        this.mLastFrame = lastFrame;
    }

    private Bitmap mLastFrame;

    //private int face_count;
    private Handler mHandler;

    /*private FaceDetector mFaceDetector = new FaceDetector(320,240,10);
    private FaceDetector.Face[] faces = new FaceDetector.Face[10];
    private PointF tmp_point = new PointF();
    private Paint tmp_paint = new Paint();*/


    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mLastFrame!=null){

                            Bitmap mutableBitmap = mLastFrame.copy(Bitmap.Config.RGB_565, true);
                            /*face_count = mFaceDetector.findFaces(mLastFrame, faces);
                            Log.d("Face_Detection", "Face Count: " + String.valueOf(face_count));*/
                            Canvas canvas = new Canvas(mutableBitmap);

                            /*for (int i = 0; i < face_count; i++) {
                                FaceDetector.Face face = faces[i];
                                tmp_paint.setColor(Color.RED);
                                tmp_paint.setAlpha(100);
                                face.getMidPoint(tmp_point);
                                canvas.drawCircle(tmp_point.x, tmp_point.y, face.eyesDistance(),
                                        tmp_paint);
                            }*/

                            mCameraView.setImageBitmap(mutableBitmap);
                        }

                    }
                }); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, 1000/10);
            }
        }
    };

    public RemotePreview(ChatActivity activity)
    {
        mLastFrame = null;

        mActivity = new WeakReference<ChatActivity>(activity);

        mHandler = new ClientHandler(((ChatActivity)mActivity.get()));

        mCameraView = (ImageView) ((ChatActivity)mActivity.get()).findViewById(R.id.camera_preview);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... unused) {
                // Background Code

                if (mServerIP == null)
                    return null;

                Socket s;
                try {
                    s = new Socket(mServerIP, 9191);
                    mClient = new ClientThread(s, mHandler);
                    new Thread(mClient).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.execute();
        mStatusChecker.run();
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        if (source != null){
            Bitmap retVal;

            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
            source.recycle();
            return retVal;
        }
        return null;
    }
}
