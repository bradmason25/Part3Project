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

/**
 * Created by brad on 17/03/2017.
 */

public class DataExportService extends AsyncTask<String, Void, Void> {
    String serverDomain;
    String filename;

    @Override
    protected Void doInBackground(String... dirPath) {
        serverDomain = "svm-bm6g14-partIIIproject.ecs.soton.ac.uk";
        //filename = getExternalFilesDir(null).toString()+"/data.csv";
        filename = dirPath[0]+"/data.csv";

        String data = "";
        try {
            copy(new File(filename), new File(dirPath[0]+"/backup.csv"));
            Log.i("Comms","Backed Up CSV");
            data = getDataFromFile(filename);
            sendToServer(data, serverDomain);
        } catch (Exception e) {
            Log.i("Comms","Transmission Failed");
            e.printStackTrace();
            if (!writeDate(data.split("\n"), filename))
                Log.i("Comms","Accidental data loss");
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

    public void copy(File src, File dst) throws IOException {
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


}
