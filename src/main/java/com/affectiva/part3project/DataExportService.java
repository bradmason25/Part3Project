package com.affectiva.part3project;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by brad on 17/03/2017.
 */

/*
The DataExportService is not a Service in truth, in it's original design it was but the name was never changed after the design did
It is now an asynchronous method that runs at the same time as the services.
It's purpose is to take the csv files of data collected by the two services and collate them into one file.

This file is then sent to the server defined in the code.
The method was made to be as robust as possible due to the risk of multi-threading and the potential for the files to be unpredictable in their state as the class is called
As a result the class has proven in both testing and implementation to be quite robust.

For the majority of cases this too shouldn't be altered, unless you know what you are altering specifically.
 */

public class DataExportService extends AsyncTask<String, Void, Void> {
    String serverDomain, prefix, movFilename, emotFilename;
    int timerPeriod;

    //To ensure that this service is ran on a seperate thread it follows a different implementation to the services used in this application
    //As a result the method that follow is called when the new thread is started
    @Override
    protected Void doInBackground(String... vars) {
        //Variable initialisation
        serverDomain = "svm-bm6g14-partIIIproject.ecs.soton.ac.uk";
        prefix = vars[0]+"/";
        movFilename = prefix+"data.csv";
        emotFilename = prefix+"emotion.csv";
        timerPeriod = Integer.parseInt(vars[1]);

        String[] movementData=null, emotionData=null;
        boolean dataRemoved = false;

        /*
        The following method will attempt to collect the data from both environmental and emotion data files
        It will delete the files and then attempt to collate the data to a new file

        Should it fail at any point it will attempt a recovery of the original files

        Despite the sceptic view of this method is has proved to be resilient in testing and implementation
         */
        try {
            movementData = getDataFromFile(movFilename).split("\n");
            emotionData = getDataFromFile(emotFilename).split("\n");
            showMessage("Collected Data");
            deleteFile(movFilename); deleteFile(emotFilename);
            showMessage("Removed old files");
            dataRemoved = true;

            String[] collatedData = collateData(movementData, emotionData);
            showMessage("Collated Data - "+collatedData.length+ " items");
            for(int i=0;i<collatedData.length;i++) {
                sendToServer(collatedData[i]+"\n", serverDomain);
            }
            showMessage("Sent "+String.valueOf(collatedData.length)+" items");

        } catch (Exception e) {
            showMessage("Transmission Failed");
            e.printStackTrace();
            try {
                if (dataRemoved) {
                    for (int i = 0; i < movementData.length; i++) {
                        writeDate(movementData[i].split(","), movFilename);
                    }
                    for (int i = 0; i < emotionData.length; i++) {
                        writeDate(emotionData[i].split(","), emotFilename);
                    }
                }
                showMessage("Restored data");
            } catch (Exception ee) {
                showMessage("Data restoration fail");
            }
        }

        return null;
    }

    //This method is used to transmit a string to a domain or IP using a UDP packet
    private void sendToServer(String data, String server) throws IOException{
        int server_port = 12345;
        DatagramSocket s = new DatagramSocket();
        InetAddress local = InetAddress.getByName(server);
        int msg_length = data.length();
        byte[] message = data.getBytes();
        DatagramPacket p = new DatagramPacket(message, msg_length, local, server_port);
        s.send(p);
    }

    //This is a simple but repeatedly used method to fetch the contents of a file
    private String getDataFromFile(String filename) throws IOException{
        File csv = new File(filename);

        int length = (int) csv.length();
        byte[] bytes = new byte[length];
        FileInputStream in = new FileInputStream(csv);

        try {
            in.read(bytes);
        } finally {
            in.close();
        }
        csv.delete();
        return new String(bytes);
    }

    //This is a simple but repeatedly used method to write the contents of a file
    private boolean writeDate(String[] line, String filename) {
        try {
            File newFile= new File (filename);
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
            if (line.length>0)
                bw.write(line[line.length-1]+"\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void deleteFile(String filename) {
        File file = new File(filename);
        file.delete();
    }

    //This is the method used to collate the two files into one
    //This method is robust to the number of data items collected from the environment but not the emotion
    //For the majority of uses the emotion data should not be modified
    private String[] collateData(String[] movData, String[] emotData) {
        int movLength = movData.length;
        int emotLength = emotData.length;
        String[] colData=null, emot, mov;
        long movTime, emotTime;

        //These two very similar segments are intended to be this way
        //If there is an imbalance in the number of data items the code will check to find the most suitable two
        //items to collate, it decides based on the epoch time attached to the data
        //The two cases are based on which set of data has the most items in order to ensure that no data is missed
        if (movLength <= emotLength) {
            colData = new String[movLength];
            for (int i=0;i<movLength;i++) {
                mov = movData[i].split(",");
                movTime = Long.parseLong(mov[mov.length-1]);
                for (int j=0;j<emotLength;j++) {
                    emot = emotData[j].split(",");
                    emotTime = Long.parseLong(emot[emot.length-1]);
                    if (emotTime<movTime && emotTime>(movTime-timerPeriod)) {
                        //If you were to change the emotion data collected for any reason, this is the line that should be altered
                        //Along with the similar line with the comment in the else clause
                        colData[i] = emot[0]+","+emot[1]+","+emot[2]+","+emot[3]+","+emot[4]+","+emot[5]+","+emot[6];
                        for (int x=0;x<mov.length-1;x++) {
                            colData[i] = colData[i]+","+mov[x];
                        }
                    }
                }
            }
        }else {
            colData = new String[emotLength];
            for (int i=0;i<emotLength;i++) {
                emot = emotData[i].split(",");
                emotTime = Long.parseLong(emot[emot.length-1]);
                for (int j=0;j<movLength;j++) {
                    mov = movData[j].split(",");
                    movTime = Long.parseLong(mov[mov.length-1]);
                    if (emotTime<movTime && emotTime>(movTime+timerPeriod)) {
                        //If you were to change the emotion data collected for any reason, this is the line that should be altered
                        //Along with the similar line with the comment in the initial if clause
                        colData[i] = emot[0]+","+emot[1]+","+emot[2]+","+emot[3]+","+emot[4]+","+emot[5]+","+emot[6];
                        for (int x=0;x<mov.length-1;x++) {
                            colData[i] = colData[i]+","+mov[x];
                        }
                    }
                }
            }
        }

        return colData;
    }

    private void showMessage(String message) {
        Log.i("Comms",message);
    }


}
