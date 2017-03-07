package com.affectiva.part3project;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by brad on 18/02/2017.
 */
//Generalised CSV class
public class CSV {
    private String splitBy = ",";
    private ArrayList<String[]> data = new ArrayList<>();
    private String line;
    private String file;

    CSV(String filename) {
        file = filename;
        if (new File(filename).isFile()) {
            clearData();
            readData(file);
        }
    }
    CSV(String filename, String splitBy) {
        file = filename;
        this.splitBy = splitBy;
    }

    public ArrayList<String[]> getData() {
        return data;
    }

    public boolean  addData(String[] line) {
        Log.i("CSV","Adding Line");
        data.add(line);
        return writeDate(line);
    }

    private boolean writeDate(String[] line) {
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

    public void clearData() {
        data.clear();
        /*
        try {
            new PrintWriter(file).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    public boolean readData(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            while ((line = br.readLine()) != null) {
                addData(line.split(splitBy));
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
