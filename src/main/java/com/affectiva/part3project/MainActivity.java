package com.affectiva.part3project;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is a very bare sample app to demonstrate the usage of the CameraDetector object from Affectiva.
 * It displays statistics on frames per second, percentage of time a face was detected, and the user's smile score.
 *
 * The app shows off the maneuverability of the SDK by allowing the user to start and stop the SDK and also hide the camera SurfaceView.
 *
 * For use with SDK 2.02
 */


//https://github.com/PhilJay/MPAndroidChart
//This is the URL for help with the chart api that has already been added to the repositories


public class MainActivity extends Activity implements Detector.ImageListener, CameraDetector.CameraEventListener {
    final String LOG_TAG = "Part3Project";

    //GUI Variables
    TextView resultTextView, emojiTextView, debug;
    ToggleButton toggleButton;
    Button settingsButton;
    Intent settingsIntent;
    SurfaceView cameraPreview;
    RelativeLayout mainLayout;

    //Camera Variables
    boolean isCameraBack = false;
    boolean isSDKStarted = true;
    CameraDetector detector;
    int previewWidth = 0;
    int previewHeight = 0;
    boolean SDKon;
    Face face;
    Intent serviceIntent;

    //Timer Variables
    int timerPeriod = 10000;
    Timer timer = new Timer();

    CSV csv;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialise Variables
        SDKon = true;
        resultTextView = (TextView) findViewById(R.id.result_textview);
        emojiTextView = (TextView) findViewById(R.id.emoji_textview);

        toggleButton = (ToggleButton) findViewById(R.id.front_back_toggle_button);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isCameraBack = isChecked;
                switchCamera(isCameraBack? CameraDetector.CameraType.CAMERA_BACK : CameraDetector.CameraType.CAMERA_FRONT);
            }
        });



        String permit = "false";
        settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        settingsIntent.putExtra("permit", permit);

        settingsButton = (Button) findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(settingsIntent, 0xe110);
            }
        });

        //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera frames
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        cameraPreview = new SurfaceView(this) {
            @Override
            public void onMeasure(int widthSpec, int heightSpec) {
                int measureWidth = MeasureSpec.getSize(widthSpec);
                int measureHeight = MeasureSpec.getSize(heightSpec);
                int width;
                int height;
                if (previewHeight == 0 || previewWidth == 0) {
                    width = measureWidth;
                    height = measureHeight;
                } else {
                    float viewAspectRatio = (float)measureWidth/measureHeight;
                    float cameraPreviewAspectRatio = (float) previewWidth/previewHeight;

                    if (cameraPreviewAspectRatio > viewAspectRatio) {
                        width = measureWidth;
                        height =(int) (measureWidth / cameraPreviewAspectRatio);
                    } else {
                        width = (int) (measureHeight * cameraPreviewAspectRatio);
                        height = measureHeight;
                    }
                }
                setMeasuredDimension(width,height);
            }
        };
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);
        cameraPreview.setLayoutParams(params);
        mainLayout.addView(cameraPreview,0);

        detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraPreview);
        detector.setDetectAllEmotions(true);
        detector.setDetectAllEmojis(true);
        detector.setImageListener(this);
        detector.setOnCameraEventListener(this);

        csv = new CSV(getExternalFilesDir(null).toString()+"/data.csv");

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("timerPeriod", timerPeriod/1000);
            editor.commit();
        } catch(Exception e) {
            Log.e("Settings","Failure submitting new settings");
            //e.printStackTrace();
        }

        startTimers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            //This is called when the settings activity is closed
            //It corrects the parameters from settings
            timerPeriod = preferences.getInt("timerPeriod", 0)*1000;
            //This resets the timers with the new period from settings
            timer.cancel();
            timer.purge();
            timer = new Timer();
            startTimers();
        } catch(Exception e) {e.printStackTrace();}

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSDKStarted) {
            startDetector();
        }
        SDKon = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDetector();
        SDKon = false;
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

    void switchCamera(CameraDetector.CameraType type) {
        detector.setCameraType(type);
    }

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v) {
        if (list == null)
            return;
        if (list.size() == 0) {
            resultTextView.setText("No Face Detected");
            emojiTextView.setText("");
        } else {
            face = list.get(0);
            if (face.emojis.getDominantEmoji().name().contains("UNKNOWN")) {
                resultTextView.setText("NEUTRAL");
            }
            else {
                resultTextView.setText(face.emojis.getDominantEmoji().name());
            }
            emojiTextView.setText(face.emojis.getDominantEmoji().getUnicode());

        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onCameraSizeSelected(int width, int height, Frame.ROTATE rotate) {
        if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
            previewWidth = height;
            previewHeight = width;
        } else {
            previewHeight = height;
            previewWidth = width;
        }
        cameraPreview.requestLayout();
    }

    //This is where the scheduling is organised, once called it will create the events whereby data is captured and recorded along wiht
    //the events where data is exported to a server. These timers will begin once this method is called
    public void startTimers() {
        serviceIntent = new Intent(MainActivity.this, PhotoTakingService.class);
        final Handler handler = new Handler();

        //This TimerTask also collects data from gui view which shouldn't be used in final data
        TimerTask dataCollectionTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("Data Collection", "Called");

                        if(!SDKon)
                            startService(serviceIntent);
                        //debug.setText(newLine[4]);
                        //incrLog();
                    }
                });
            }
        };
        timer.schedule(dataCollectionTask, timerPeriod, timerPeriod);

        //It's advisable to run the export task with a delay that doesn't coincide with above period so that the tasks don't overlap
        //timer.schedule(dataExportTask, 330000, 300000);

    }
}
