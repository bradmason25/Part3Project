package com.affectiva.part3project;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by brad on 23/02/2017.
 *
 */

public class SettingsActivity extends Activity{

    //GUI Variables
    LinearLayout mainLayout, layoutSplit, descriptionSplit, interfaceSplit;
    TextView allowImagingTextMain, allowImagingTextSub;
    CheckBox allowImagingCheck;
    //Other Variables
    boolean allowImaging;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Initialise GUI Layouts
        mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        layoutSplit = (LinearLayout) findViewById(R.id.layout_split);
        descriptionSplit = (LinearLayout) findViewById(R.id.description_split);
        interfaceSplit = (LinearLayout) findViewById(R.id.interface_split);
        //Initialise GUI TextViews
        allowImagingTextMain = (TextView) findViewById(R.id.allow_imaging_text_main);
        allowImagingTextSub = (TextView) findViewById(R.id.allow_imaging_text_sub);
        //Initialise GUI inputs
        allowImagingCheck = (CheckBox) findViewById(R.id.allow_imaging_check);
        allowImagingCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                allowImaging = b;
            }
        });
        //Initialise other variables
        allowImaging = allowImagingCheck.isChecked();

    }
}
