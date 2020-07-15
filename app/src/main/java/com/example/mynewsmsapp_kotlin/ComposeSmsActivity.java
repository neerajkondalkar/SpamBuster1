package com.example.mynewsmsapp_kotlin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComposeSmsActivity extends AppCompatActivity {

    private static final String TAG = "[MY_DEBUG] " + ComposeSmsActivity.class.getSimpleName();
    EditText input_contact;
    EditText input_sms;
    SmsManager sms_manager = SmsManager.getDefault();

    @Override
    protected void onStart() {
        String TAG_onStart = " onStart(): ";
        Log.d(TAG, TAG_onStart + " called ");
        super.onStart();
    }

    @Override
    protected void onStop() {
        String TAG_onStop = " onStop(): ";
        Log.d(TAG, TAG_onStop + " called ");
        super.onStop();
    }

    @Override
    protected void onResume() {
        String TAG_onResume = " onResume(): ";
        Log.d(TAG, TAG_onResume + " called ");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        String TAG_onDestroy = " onResume(): ";
        Log.d(TAG, TAG_onDestroy + " called ");
        super.onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        String TAG_onCreate = " onCreate(): ";
        Log.d(TAG, TAG_onCreate + " called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_sms);

        input_contact = (EditText) findViewById(R.id.edit_text_phone_or_name);
        input_sms = (EditText) findViewById(R.id.edit_text_input_message);

        checkNecessaryPermissions();
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

        String str_input_contact = input_contact.getText().toString();
        String str_input_sms = input_sms.getText().toString();

        Log.d(TAG, TAG_onClickSendSms + "str_input_contact = " + str_input_contact);
        Log.d(TAG, TAG_onClickSendSms + "str_input_sms  = " + str_input_sms);


        try {
            String phone_pattern = "^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$";
            Matcher matcher;
            Pattern r = Pattern.compile(phone_pattern);
            if (!str_input_contact.isEmpty()) {
                matcher = r.matcher(str_input_contact.trim());
                if (matcher.find()) {
                    if(!str_input_sms.isEmpty()) {
                        Log.d(TAG, TAG_onClickSendSms + "valid phone number and message");
                        sms_manager.sendTextMessage(str_input_contact, null, str_input_sms, null, null);
                        Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Log.d(TAG, TAG_onClickSendSms + "Error: message empty");
                        Toast.makeText(this, "Error: Message cannot be empty! ", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG, TAG_onClickSendSms + "Error: invalid phone number");
                    Toast.makeText(this, "Error: Invalid phone number", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Log.d(TAG, TAG_onClickSendSms + "Error: phone number empty");
                Toast.makeText(this, "Error: Please enter mobile number ", Toast.LENGTH_LONG).show();
            }
        }

        //null pointer exception sometimes when View is not present
        catch (Exception e){
            Log.d(TAG, TAG_onClickSendSms + "Exception : " + e);
        }

    }

    public void onClickBackToMainActivity(View view){
        String TAG_onClickBackToMainActivity = " onClickBackToMainActivity(): ";
        Log.d(TAG, TAG_onClickBackToMainActivity + " called");
        Log.d(TAG, TAG_onClickBackToMainActivity + "MainActivity.active = " + MainActivity.active);
        finish();
    }
}
