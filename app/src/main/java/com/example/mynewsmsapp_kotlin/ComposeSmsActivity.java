package com.example.mynewsmsapp_kotlin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ComposeSmsActivity extends AppCompatActivity {

    private static final String TAG = "[MY_DEBUG] " + ComposeSmsActivity.class.getSimpleName();
    EditText input_contact;
    EditText input_sms;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        String TAG_onCreate = " onCreate(): ";
        Log.d(TAG, TAG_onCreate + " called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_sms);

        input_contact = (EditText) findViewById(R.id.edit_text_phone_or_name);
        input_sms = (EditText) findViewById(R.id.sms_text_input);

        checkNecessaryPermissions();
//        Button button_send_sms = findViewById(R.id.button_send_sms);
//        Button button_back = findViewById(R.id.button_back_to_activity_main);

    }

    public void checkNecessaryPermissions() {
        String TAG_checkNecessaryPermissions = " checkNecessaryPermissions(): ";
        Log.d(TAG, TAG_checkNecessaryPermissions + " called");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, TAG_checkNecessaryPermissions + " SEND_SMS permission granted");
        } else {
            Log.d(TAG, TAG_checkNecessaryPermissions + " SEND_SMS permission denied");
        }
    }

    public void onClickSendSms(View view){
        String TAG_onClickSendSms = " onClickSendSms(): ";
        Log.d(TAG, TAG_onClickSendSms + " called");
        input_contact  = findViewById(R.id.edit_text_phone_or_name);
        input_sms = findViewById(R.id.edit_text_input_message);

        try {
            String str_input_contact = input_contact.getText().toString();
            String str_input_sms = input_sms.getText().toString();

            if(str_input_contact.equals("") || str_input_sms.equals("")) {
                Toast.makeText(this, "Empty phone/message!", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Input accepted", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onClickSendSms + "str_input_contact = " + str_input_contact);
                Log.d(TAG, TAG_onClickSendSms + "str_input_sms  = " + str_input_sms);
            }
//        if(input_contact.toString() !> ) {
//        }
        }
        //null pointer exception sometimes
        catch (Exception e){
            Log.d(TAG, TAG_onClickSendSms + "Exception : " + e);
            Toast.makeText(this, "Empty phone/message!", Toast.LENGTH_SHORT).show();
        }

    }

    public void onClickBackToMainActivity(View view){
        String TAG_onClickBackToMainActivity = " onClickBackToMainActivity(): ";
        Log.d(TAG, TAG_onClickBackToMainActivity + " called");
        Intent intent_go_back_to_main_activity = new Intent(this, MainActivity.class);
        startActivity(intent_go_back_to_main_activity);
    }
}
