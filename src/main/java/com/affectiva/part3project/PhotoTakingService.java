package com.affectiva.part3project;

/**
 * Created by brad on 20/02/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.os.Handler;
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
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;

/** Takes a single photo on service start. */
public class PhotoTakingService extends Service implements Detector.ImageListener {


    //Photo Variables
    boolean isFocusing = false;
    Face face;
    boolean noFace = true;

    private PhotoDetector detector;

    //This defines whether or not to save the images captured for debugging
    //This should always be set to false for deployment but can be set to
    //true if some debugging is needed on the images captured
    boolean debugMode = false;

    @Override
    public void onCreate() {
        super.onCreate();

        showMessage("Starting Camera");
        takePhoto(this);
    }


    private void processImage(Bitmap bmp) {
        showMessage("Processing Image");
        Frame.BitmapFrame frame = new Frame.BitmapFrame(bmp, Frame.COLOR_FORMAT.UNKNOWN_TYPE);

        detector = new PhotoDetector(this,1, Detector.FaceDetectorMode.LARGE_FACES);
        detector.setDetectAllEmotions(true);
        detector.setDetectAllExpressions(true);
        detector.setDetectAllAppearances(true);
        detector.setImageListener(this);
        detector.disableAnalytics();

        startDetector();
        detector.process(frame);
        stopDetector();
    }

    private void flushResults() {
        showMessage("Flushing emotion data");
        String nothing = "0.0";
        String[] newLine;
        if(!noFace) {
            newLine = new String[] {String.valueOf(face.emotions.getAnger()), String.valueOf(face.emotions.getContempt()), String.valueOf(face.emotions.getDisgust()), String.valueOf(face.emotions.getFear()),
                    String.valueOf(face.emotions.getJoy()), String.valueOf(face.emotions.getSadness()), String.valueOf(face.emotions.getSurprise()), String.valueOf(System.currentTimeMillis())};
        } else {
            newLine = new String[] {nothing, nothing, nothing, nothing, nothing, nothing, nothing, String.valueOf(System.currentTimeMillis())};
        }

        writeDate(newLine,getExternalFilesDir(null).toString()+"/emotion.csv");
        stopSelf();
    }

    private boolean writeDate(String[] line, String file) {
        try {
            File newFile= new File (file);
            FileWriter fw;
            if (newFile.exists())
            {
                fw = new FileWriter(newFile,true);
            }
            else
            {
                newFile.createNewFile();
                fw = new FileWriter(newFile);
            }
            BufferedWriter bw = new BufferedWriter(fw);

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
        final SurfaceView preview = new SurfaceView(context);
        SurfaceHolder holder = preview.getHolder();
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            //The preview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {

                showMessage("Start Building the Camera");
                Camera camera = null;


                try {
                    camera = openFrontFacingCameraGingerbread();
                    showMessage("Opened the front camera :)");

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
                            Bitmap bmp = rotateImage(BitmapFactory.decodeByteArray(data, 0, data.length), 270);

                            if (debugMode) {
                                //The code below can be used to store the images to the local directory for debugging

                                File photo = new File(getExternalFilesDir(null).toString() + "/image" + System.currentTimeMillis() + ".jpg");
                                FileOutputStream fos = null;
                                if (photo.exists())
                                    photo.delete();
                                try {
                                    fos = new FileOutputStream(photo.getPath());
                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    try {
                                        if (fos != null) {
                                            fos.close();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
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
                } catch (Exception e) {
                    showMessage("Failed to open front camera, giving up");
                    if (camera != null)
                        camera.release();
                    //throw new RuntimeException(e);
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
            flushResults();
        } else {
            face = list.get(0);
            noFace = false;
            showMessage("Face Found");
            flushResults();
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