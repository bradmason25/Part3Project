package com.affectiva.part3project;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
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
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by brad on 17/03/2017.
 */

public class DataExportService extends AsyncTask<String, Void, Void> {
    String serverDomain, prefix, movFilename, emotFilename;
    String filename;
    int timerPeriod;

    @Override
    protected Void doInBackground(String... vars) {
        serverDomain = "svm-bm6g14-partIIIproject.ecs.soton.ac.uk";
        prefix = vars[0]+"/";
        movFilename = prefix+"data.csv";
        emotFilename = prefix+"emotion.csv";
        timerPeriod = Integer.parseInt(vars[1]);

        String[] movementData=null, emotionData=null;
        boolean dataRemoved = false;
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

    private void sendToServer(String data, String server) throws IOException{
        int server_port = 12345;
        DatagramSocket s = new DatagramSocket();
        InetAddress local = InetAddress.getByName(server);
        int msg_length = data.length();
        byte[] message = data.getBytes();
        DatagramPacket p = new DatagramPacket(message, msg_length, local, server_port);
        s.send(p);
    }

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

    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void deleteFile(String filename) {
        File file = new File(filename);
        file.delete();
    }

    private String[] collateData(String[] movData, String[] emotData) {
        int movLength = movData.length;
        int emotLength = emotData.length;
        String[] colData=null, emot, mov;
        long movTime, emotTime;

        if (movLength <= emotLength) {
            colData = new String[movLength];
            for (int i=0;i<movLength;i++) {
                mov = movData[i].split(",");
                movTime = Long.parseLong(mov[mov.length-1]);
                for (int j=0;j<emotLength;j++) {
                    emot = emotData[j].split(",");
                    emotTime = Long.parseLong(emot[emot.length-1]);
                    if (emotTime<movTime && emotTime>(movTime-timerPeriod)) {
                        colData[i] = emot[0]+","+emot[1]+","+emot[2]+","+emot[3]+","+emot[4]+","+emot[5]+","+emot[6]+","+mov[0]+","+mov[1];
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
                        colData[i] = emot[0]+","+emot[1]+","+emot[2]+","+emot[3]+","+emot[4]+","+emot[5]+","+emot[6]+","+mov[0]+","+mov[1];
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
