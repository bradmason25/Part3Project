package com.affectiva.part3project;

/**
 * Created by brad on 20/02/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.PhotoDetector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;

/** Takes a single photo on service start. */
public class PhotoTakingService extends Service implements SensorEventListener, Detector.ImageListener {
    //Environmental Data Variables
    private List<Double> movement;
    private int steps;
    private long timeCreated;

    //Photo Variables
    boolean isFocusing = false;
    Face face;
    boolean noFace = true;

    private CSV csv;
    private PhotoDetector detector;

    @Override
    public void onCreate() {
        super.onCreate();


        //csv = new CSV(getExternalFilesDir(null).toString()+"/emotionData.csv");

        //Environmental Data stuff
        timeCreated = System.currentTimeMillis();
        movement = new ArrayList<>();
        steps = 0;

        //To register a new listener you simply pass the method registerListener a type
        //This is demonstrated below
        registerListener(Sensor.TYPE_LINEAR_ACCELERATION);
        registerListener(Sensor.TYPE_STEP_COUNTER);

        //Photo stuffs
        takePhoto(this);
    }

    private void registerListener(int sensorType) {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);

        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //showMessage("Registered Listener "+sensor.getStringType());
    }

    private double getAverageMovement() {
        float sum = 0f;
        for (Double d: movement) {
            sum +=d;
        }
        return (sum/movement.size())*Math.pow(System.currentTimeMillis()-timeCreated/1000,2);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor s = sensorEvent.sensor;
        float[] values = sensorEvent.values;
        int counterSteps = 0;

        if(s.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            movement.add(Math.sqrt(Math.pow(values[0],2)+Math.pow(values[1],2)+Math.pow(values[2],2)));
        }
        if(s.getType() == Sensor.TYPE_STEP_COUNTER) {
            if(counterSteps<1)
                counterSteps = (int) sensorEvent.values[0];
            steps = (int) sensorEvent.values[0] - counterSteps;
        }
    }

    private void processImage(Bitmap bmp) {
        showMessage("Processing Image");
        Frame.BitmapFrame frame = new Frame.BitmapFrame(bmp, Frame.COLOR_FORMAT.UNKNOWN_TYPE);

        detector = new PhotoDetector(this,1, Detector.FaceDetectorMode.LARGE_FACES);
        detector.setDetectAllEmotions(true);
        detector.setDetectAllExpressions(true);
        detector.setDetectAllAppearances(true);
        detector.setImageListener(this);

        startDetector();
        detector.process(frame);
        stopDetector();


        //stopSelf();
    }

    public void onDestroy() {
        showMessage("Closing Service");
        if(!noFace) {
            String[] newLine = {String.valueOf(face.emotions.getAnger()), String.valueOf(face.emotions.getContempt()), String.valueOf(face.emotions.getDisgust()), String.valueOf(face.emotions.getFear()),
                    String.valueOf(face.emotions.getJoy()), String.valueOf(face.emotions.getSadness()), String.valueOf(face.emotions.getSurprise()), String.valueOf(getAverageMovement()), String.valueOf(steps),
                    String.valueOf(System.currentTimeMillis())};
            //csv.addData(newLine);
            writeDate(newLine,getExternalFilesDir(null).toString()+"/emotionData.csv");
        }
        super.onDestroy();
    }

    private boolean writeDate(String[] line, String file) {
        Log.i("CSV","Writing Line");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            for (int i=0; i<line.length-1; i++) {
                bw.write(line[i]+",");
            }
            bw.write(line[line.length-1]+"\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void takePhoto(final Context context) {
        showMessage("Starting Camera");

        final SurfaceView preview = new SurfaceView(context);
        SurfaceHolder holder = preview.getHolder();
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            //The preview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {

                Camera camera = null;

                try {
                    camera = Camera.open();

                    try {
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }


                    camera.startPreview();

                    final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            camera.release();
                            showMessage("Image Captured");
                            Bitmap bmp = rotateImage(BitmapFactory.decodeByteArray(data, 0, data.length),90);

                            File photo = new File(getExternalFilesDir(null).toString()+"/image"+System.currentTimeMillis()+".jpg");
                            FileOutputStream fos = null;
                            if (photo.exists())
                                photo.delete();
                            try {
                                fos = new FileOutputStream(photo.getPath());
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            } catch (Exception e) {e.printStackTrace();}
                            finally {
                                try {
                                    if (fos != null) {
                                        fos.close();
                                    }
                                } catch (Exception e) {e.printStackTrace();}
                            }

                            processImage(bmp);
                        }

                        private Bitmap rotateImage(Bitmap img, int degree) {
                            Matrix matrix = new Matrix();
                            matrix.postRotate(degree);
                            Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
                            img.recycle();
                            return rotatedImg;
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
                    //camera.takePicture(null,null,pictureCallback);
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
    public void onImageResults(List<Face> list, Frame frame, float v) {
        if (list == null)
            return;
        if (list.size() == 0) {
            noFace = true;
            showMessage("No Face Found");
        } else {
            face = list.get(0);
            noFace = false;
            showMessage("Face Found");
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



    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}