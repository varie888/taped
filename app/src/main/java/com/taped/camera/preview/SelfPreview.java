package com.taped.camera.preview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
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
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import com.taped.camera.source.ServerThread;
import com.taped.taped.ChatActivity;
import com.taped.taped.R;
import com.taped.utils.MarshMallowPermission;
import com.taped.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by varie on 16/03/2018.
 */

public class SelfPreview {
    private static final String TAG = "ChatActivity";

    public static final int CAMERA_PERMISSION_REQUEST_CODE = 3;

    private final WeakReference<ChatActivity> mActivity;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Size imageDimension;
    private File file;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private TextureView textureView;

    public ByteArrayOutputStream mFrameBuffer;

    MarshMallowPermission marshMallowPermission;

    public static String SERVERIP = "localhost";
    public static final int SERVERPORT = 9191;
    private Handler handler = new Handler();

    public SelfPreview(ChatActivity activity)
    {
        mActivity = new WeakReference<ChatActivity>(activity);
        marshMallowPermission = new MarshMallowPermission(activity);

        if (!marshMallowPermission.checkPermissionForCamera()){
            marshMallowPermission.requestPermissionForCamera();
        }

        if (!marshMallowPermission.checkPermissionForExternalStorage()){
            marshMallowPermission.requestPermissionForExternalStorage();
        }

        textureView = (TextureView) activity.findViewById(R.id.texture);
        assert textureView != null;

        Thread cThread = new Thread(new ServerThread(mActivity.get(),SERVERIP,SERVERPORT,handler));
        cThread.start();
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /*final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText((ChatActivity)mActivity.get(), "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };*/
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//    public void takePicture() {
//        if(null == cameraDevice) {
//            Log.e(TAG, "cameraDevice is null");
//            return;
//        }
//        CameraManager manager = (CameraManager) mActivity.get().getSystemService(Context.CAMERA_SERVICE);
//        try {
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
//            Size[] jpegSizes = null;
//            if (characteristics != null) {
//                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
//            }
//            int width = 640;
//            int height = 480;
//            if (jpegSizes != null && 0 < jpegSizes.length) {
//                width = jpegSizes[0].getWidth();
//                height = jpegSizes[0].getHeight();
//            }
//            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
//            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
//            outputSurfaces.add(reader.getSurface());
//            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
//            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(reader.getSurface());
//            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            // Orientation
//            int rotation = mActivity.get().getWindowManager().getDefaultDisplay().getRotation();
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
//            //final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
//            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
//                @Override
//                public void onImageAvailable(ImageReader reader) {
//                    Image image = null;
//                    try {
//                        image = reader.acquireLatestImage();
//                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                        byte[] bytes = new byte[buffer.capacity()];
//                        buffer.get(bytes);
//                        save(bytes);
//
//                        //convert YuvImage(NV21) to JPEG Image data
//                        int width = 640;
//                        int height = 480;
//                        YuvImage yuvimage=new YuvImage(bytes,ImageFormat.NV21,width,height,null);
//                        System.out.println("WidthandHeight"+yuvimage.getHeight()+"::"+yuvimage.getWidth());
//                        ByteArrayOutputStream baos=new ByteArrayOutputStream();
//                        yuvimage.compressToJpeg(new Rect(0,0,width,height),100,baos);
//                        mFrameBuffer = baos;
//
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } finally {
//                        if (image != null) {
//                            image.close();
//                        }
//                    }
//                }
//                private void save(byte[] bytes) throws IOException {
//                    OutputStream output = null;
//                    try {
//                        output = new FileOutputStream(file);
//                        output.write(bytes);
//                    } finally {
//                        if (null != output) {
//                            output.close();
//                        }
//                    }
//                }
//            };
//            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
//            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
//                @Override
//                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
//                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText((ChatActivity)mActivity.get(), "Saved:" + file, Toast.LENGTH_SHORT).show();
//                    createCameraPreview();
//                }
//            };
//            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(CameraCaptureSession session) {
//                    try {
//                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
//                    } catch (CameraAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
//                @Override
//                public void onConfigureFailed(CameraCaptureSession session) {
//                }
//            }, mBackgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }

    private static long mLastSnap = System.currentTimeMillis();

    protected void createCameraPreview() {
        CameraManager manager = (CameraManager) mActivity.get().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(reader.getSurface());

            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(surface);
            outputSurfaces.add(reader.getSurface());

            // Orientation
            int rotation = mActivity.get().getWindowManager().getDefaultDisplay().getRotation();
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            final ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;

                    try {
                        image = reader.acquireLatestImage();
                        if (image == null)
                            return;

                        if (System.currentTimeMillis() - mLastSnap < 200) {
                            return;
                        }

                        mLastSnap = System.currentTimeMillis();

                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        //convert YuvImage(NV21) to JPEG Image data
                        int width = 640;
                        int height = 480;
                        YuvImage yuvimage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
                        System.out.println("WidthandHeight" + yuvimage.getHeight() + "::" + yuvimage.getWidth());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        //yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
                        baos.write(bytes, 0, bytes.length);
                        mFrameBuffer = baos;

                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;

                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText((ChatActivity)mActivity.get(), "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        /*CameraManager manager = (CameraManager) mActivity.get().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            *//*List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));*//*
            captureStillBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureStillBuilder.addTarget(reader.getSurface());
            captureStillBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = mActivity.get().getWindowManager().getDefaultDisplay().getRotation();
            captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            final ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        //convert YuvImage(NV21) to JPEG Image data
                        int width = 640;
                        int height = 480;
                        YuvImage yuvimage = new YuvImage(bytes, ImageFormat.YUV_420_888, width, height, null);
                        System.out.println("WidthandHeight" + yuvimage.getHeight() + "::" + yuvimage.getWidth());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
                        mFrameBuffer = baos;

                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText((ChatActivity) mActivity.get(), "Saved:" + file, Toast.LENGTH_SHORT).show();
                }
            };
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }*/
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) mActivity.get().getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission((ChatActivity)mActivity.get(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission((ChatActivity)mActivity.get(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((ChatActivity)mActivity.get(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText((ChatActivity)mActivity.get(), "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                ((ChatActivity)mActivity.get()).finish();
            }
        }
    }

    public void onResume() {
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    public void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
    }

    public ByteArrayOutputStream getFrameBuffer(){
        return mFrameBuffer;
    }
}
