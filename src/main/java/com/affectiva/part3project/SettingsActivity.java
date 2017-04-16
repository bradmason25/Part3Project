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

/*
This class is the handler for the settings menu. If you wish to alter what settings can be changed, you would need to edit it here.
You would also need to modify the 'onActivityResults' method from the 'MainActivity' class for the way the submitted settings are handled.

In order to add your own settings you would need to use new variables for the graphical elements as well as the actual variables you wish to alter.
The code below is a demonstration of how to implement this with the timer variable used by the timer to know when to next call the services.

If you implement a new setting, be sure to keep the final variable modifications in the 'submitVariables' method, as this is called when the user presses the submit button on the graphical interface
 */

public class SettingsActivity extends Activity{

    //GUI Variables
    LinearLayout mainLayout, layoutSplit, descriptionSplit, interfaceSplit;
    TextView periodTextMain, periodTextSub, errorMessage;
    Button submitButton, cancelButton;
    EditText periodNum;
    //Other Variables
    SharedPreferences preferences;
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
                //Reset the error message in case it was used
                errorMessage.setText("");
                //Here you prepare the Android settings persistency class
                SharedPreferences.Editor editor = preferences.edit();
                //You use this method to add a variable to the settings
                editor.putInt("timerPeriod", Integer.valueOf(periodNum.getText().toString()));
                //Once all variables have been prepare they are submitted
                editor.commit();
                //The variables will then be handled by the 'MainActivy' class when this interface closes
                finish();
            }
        } catch(Exception e) {
            Log.e("Settings","Failure submitting new settings");
            //e.printStackTrace();
        }
    }
}
