package com.affectiva.part3project;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
The DataCollectionService is used called by the timers within 'MainAcitivity'.
It is implemented by allowing it to run indefinitely, then when the timer is next called the service is destroyed.
Therefore, the call to flush the collected data is called from within the 'onDestroy' method.

The purpose of this class is to collect the environmental data used by the study.
This particular implementation is a demonstration that collected both the number of steps taken whilst the service is running,
and the acceleration data. It sums the acceleration data and then find the average upon the death of the service.

This is likely the main class that should be modified for the purpose of a different experimental study.
Below are comments for a guide of what you might wish to modify, and how the code has been designed to make this process
as simple and helpful as possible.
 */

public class DataCollectionService extends Service implements SensorEventListener{
    //Environmental Data Variables
    private List<Double> movement;
    private int steps;
    private int counterSteps = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        showMessage("Created Service");
        //Environmental Data Variables Initialisation
        movement = new ArrayList<>();
        steps = 0;

        //To register a new listener you simply pass the method registerListener a type
        //You must register a sensor in order for it to detect events
        //This is demonstrated below
        registerListener(Sensor.TYPE_LINEAR_ACCELERATION);
        registerListener(Sensor.TYPE_STEP_COUNTER);

        //This calls for the creation of a separate thread to capture an image and process it
        //using the Affectiva API to determine the emotion of the user
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor s = sensorEvent.sensor;
        float[] values = sensorEvent.values;

        //Here you can define what action to take when each sensor detects an event
        //For example the first case here executes if there has been an event in the linear acceleration sensor
        //You just add a case for each type of sensor you implement

        switch (s.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                movement.add(Math.sqrt(Math.pow(values[0],2)+Math.pow(values[1],2)+Math.pow(values[2],2)));

            case Sensor.TYPE_STEP_COUNTER:
                if(counterSteps<1)
                    counterSteps = (int) sensorEvent.values[0];
                steps = (int) sensorEvent.values[0]-counterSteps;
        }
    }

    @Override
    public void onDestroy() {
        //Upon the destruction of this service it flushes all collected data to a csv
        //In order to add to this you need to add the necessary data item to the array

        showMessage("Flushing Sensor Data");
        //This is the array you would add any additional data you wish to add to the csv
        String[] newLine = {String.valueOf(getAverageMovement()),String.valueOf(steps),String.valueOf(System.currentTimeMillis())};
        writeDate(newLine, getExternalFilesDir(null).toString()+"/data.csv");

        super.onDestroy();
    }

    private void registerListener(int sensorType) {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);

        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //showMessage("Registered Listener "+sensor.getStringType());
    }

    //This method uses the acceleration data collected throughout this services lifetime and finds the average
    private double getAverageMovement() {
        float sum = 0f;
        for (Double d: movement) {
            sum +=d;
        }
        return (sum/movement.size());
    }



    //This is a method used to write an array of strings to a file.
    //It is designed to work with csv files, the elements of the array are written on the same line with a comma seperator.
    //The line is ended with a new line to allow additional additions at a later date
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

    private void showMessage(String message) {
        Log.i("DataCollection",message);
    }

    //The methods below are required by the interface but are not used by this implementation and are thus left blank.
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {return null;}
}
