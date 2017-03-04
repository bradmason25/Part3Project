package com.affectiva.part3project;

import android.app.Activity;
import android.content.SharedPreferences;
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

/**
 * Created by brad on 23/02/2017.
 *
 */

public class SettingsActivity extends Activity{

    //GUI Variables
    LinearLayout mainLayout, layoutSplit, descriptionSplit, interfaceSplit;
    TextView periodTextMain, periodTextSub;
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
        //Initialise GUI inputs
        periodNum = (EditText) findViewById(R.id.period_num);
        //Initialise other variables
        submitButton = (Button) findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitVariables();
                finish();
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
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("timerPeriod", Integer.valueOf(periodNum.getText().toString()));
            editor.commit();
        } catch(Exception e) {
            Log.e("Settings","Failure submitting new settings");
            //e.printStackTrace();
        }
    }
}
