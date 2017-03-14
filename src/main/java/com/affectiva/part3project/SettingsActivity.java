package com.affectiva.part3project;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by brad on 23/02/2017.
 *
 */

public class SettingsActivity extends Activity{

    //GUI Variables
    LinearLayout mainLayout, layoutSplit, descriptionSplit, interfaceSplit;
    TextView periodTextMain, periodTextSub, errorMessage;
    Button submitButton, cancelButton;
    EditText periodNum;
    //Other Variables
    SharedPreferences preferences;
    boolean allowImaging;
    String test;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        test = getIntent().getStringExtra("permit");

        //Initialise GUI Layouts
        mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        layoutSplit = (LinearLayout) findViewById(R.id.layout_split);
        descriptionSplit = (LinearLayout) findViewById(R.id.description_split);
        interfaceSplit = (LinearLayout) findViewById(R.id.interface_split);
        //Initialise GUI TextViews
        periodTextMain = (TextView) findViewById(R.id.period_text_main);
        periodTextSub = (TextView) findViewById(R.id.period_text_sub);
        errorMessage = (TextView) findViewById(R.id.error_message);
        errorMessage.setTextColor(Color.RED);
        //Initialise GUI inputs
        periodNum = (EditText) findViewById(R.id.period_num);
        //Initialise other variables
        submitButton = (Button) findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitVariables();
            }
        });
        cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            periodNum.setText(String.valueOf(preferences.getInt("timerPeriod", 0)));
        } catch(Exception e) {e.printStackTrace();}

    }

    private void submitVariables() {
        try {
            int newPeriod = Integer.valueOf(periodNum.getText().toString());
            //Only permit periods larger than 1 seconds
            if(newPeriod<=1) {
                errorMessage.setText("You set cannot set the period below 1 second");
            }
            else {
                errorMessage.setText("");
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("timerPeriod", Integer.valueOf(periodNum.getText().toString()));
                editor.commit();
                finish();
            }
        } catch(Exception e) {
            Log.e("Settings","Failure submitting new settings");
            //e.printStackTrace();
        }
    }
}
