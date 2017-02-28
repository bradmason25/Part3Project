package com.affectiva.part3project;

/**
 * Created by brad on 20/02/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.PhotoDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Takes a single photo on service start. */
public class PhotoTakingService extends Service implements SensorEventListener, Detector.ImageListener {

    //Movement Sensor Setup
    private SensorManager mSensorManager;
    private Sensor motionDetector;
    private List<Double> movement;
    private int period = 10000;
    //Photo Variables
    boolean isFocusing = false;
    Bitmap bmp;
    Face face;
    boolean noFace = true;

    private CSV csv;
    private PhotoDetector detector;

    @Override
    public void onCreate() {
        super.onCreate();

        csv = new CSV(getExternalFilesDir(null).toString()+"/exportData.csv");
        //Movement Stuffs
        movement = new ArrayList<>();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        motionDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(this, motionDetector, SensorManager.SENSOR_DELAY_NORMAL);

        //Photo stuffs
        takePhoto(this);

        if(bmp != null) {
            Log.i("Image Capture","successful bitmap capture");
            Frame.BitmapFrame frame = new Frame.BitmapFrame(bmp, Frame.COLOR_FORMAT.UNKNOWN_TYPE);

            detector = new PhotoDetector(this, 1, Detector.FaceDetectorMode.LARGE_FACES);
            detector.setDetectAllEmotions(true);
            detector.setDetectAllExpressions(true);
            detector.setDetectAllAppearances(true);
            detector.setImageListener(this);

            startDetector();
            detector.process(frame);
            stopDetector();

            String[] newLine = {String.valueOf(face.emotions.getAnger()), String.valueOf(face.emotions.getContempt()), String.valueOf(face.emotions.getDisgust()), String.valueOf(face.emotions.getFear()),
                    String.valueOf(face.emotions.getJoy()), String.valueOf(face.emotions.getSadness()), String.valueOf(face.emotions.getSurprise()), String.valueOf(getMovement()), String.valueOf(System.currentTimeMillis())};
            csv.addData(newLine);
        }
        else {
            Log.i("Image Capture","bitmap is null");
        }
        stopSelf();
    }

    public void onDestroy() {
        showMessage("Closing Service");
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    private void takePhoto(final Context context) {
        final SurfaceView preview = new SurfaceView(context);
        SurfaceHolder holder = preview.getHolder();
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            //The preview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {
                showMessage("Surface created");

                Camera camera = null;

                try {
                    camera = openFrontFacingCameraGingerbread();
                    showMessage("Opened camera");

                    try {
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    camera.startPreview();
                    showMessage("Started preview");

                    final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                            showMessage("Took picture");
                            camera.release();
                        }
                    };

                    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                isFocusing = false;

                                camera.takePicture(null, null, pictureCallback);
                            }
                        }
                    };
                    camera.autoFocus(autoFocusCallback);

                } catch (Exception e) {
                    if (camera != null)
                        camera.release();
                    throw new RuntimeException(e);
                }
            }

            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}


        });

        WindowManager wm = (WindowManager)context
                .getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                //Don't know if this is a safe default
                PixelFormat.UNKNOWN);

        //Don't set the preview visibility to GONE or INVISIBLE
        wm.addView(preview, params);
    }


    public double getMovement() {
        List<Double> temp = new ArrayList<>();
        for (Double d: movement) {
            temp.add(d);
        }
        movement.clear();
        float sum = 0f;
        for (Double d: temp) {
            sum += d;
        }
        return (sum/temp.size())*Math.pow(period/1000,2);
    }

    private Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx<cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e("Your_TAG", "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }
        return cam;
    }


    private static void showMessage(String message) {
        Log.i("Camera", message);
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        float[] values = event.values;

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            movement.add(Math.sqrt(Math.pow(values[0],2)+Math.pow(values[1],2)+Math.pow(values[2],2)));
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v) {
        if (list == null)
            return;
        if (list.size() == 0) {
            noFace = true;
        } else {
            face = list.get(0);
            noFace = false;
        }
    }
    void startDetector() {
        if (!detector.isRunning()) {
            detector.start();
        }
    }

    void stopDetector() {
        if (detector.isRunning()) {
            detector.stop();
        }
    }
}