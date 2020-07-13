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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        String TAG_onCreate = " onCreate(): ";
        Log.d(TAG, TAG_onCreate + " called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_sms);

        input_contact = (EditText) findViewById(R.id.edit_text_phone_or_name);
        input_sms = (EditText) findViewById(R.id.edit_text_input_message);

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
//        String phone_regex = "(0/91)?[7-9][0-9]{9}";

//        ---------EDITED start---------------

        String phone_pattern = "^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$";
        Matcher matcher;
        Pattern r = Pattern.compile(phone_pattern);
        if (!input_contact.getText().toString().isEmpty()) {
            matcher = r.matcher(input_contact.getText().toString().trim());
            if (matcher.find()) {
                Toast.makeText(this, "MATCH", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "NO MATCH", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Please enter mobile number ", Toast.LENGTH_LONG).show();
        }

//        ----------EDITED end--------------

        try {
            String str_input_contact = input_contact.getText().toString();
            String str_input_sms = input_sms.getText().toString();

            if(str_input_contact.equals("") || str_input_sms.equals("")) {
                Toast.makeText(this, "Empty phone/message!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onClickSendSms + "str_input_contact = " + str_input_contact);
                Log.d(TAG, TAG_onClickSendSms + "str_input_sms  = " + str_input_sms);
            }
            else {
                Toast.makeText(this, "Input accepted", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onClickSendSms + "str_input_contact = " + str_input_contact);
                Log.d(TAG, TAG_onClickSendSms + "str_input_sms  = " + str_input_sms);

//                Boolean valid_phone = android.util.Patterns.PHONE.matcher(str_input_contact).matches() && str_input_contact.matches(phone_regex);
//                Boolean valid_phone = str_input_contact.matches(regex);
//                if (valid_phone) {
////                    sms_manager.sendTextMessage(str_input_contact, null, str_input_sms, null, null);
//                    Toast.makeText(this, "Valid number", Toast.LENGTH_SHORT).show();
//                    Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();
//                }
//                else{
//                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
//                }

            }
//        if(input_contact.toString() !> ) {
//        }
        }
        //null pointer exception sometimes when View is not properly referenced.
        catch (Exception e){
            Log.d(TAG, TAG_onClickSendSms + "Exception : " + e);
        }

    }

    public void onClickBackToMainActivity(View view){
        String TAG_onClickBackToMainActivity = " onClickBackToMainActivity(): ";
        Log.d(TAG, TAG_onClickBackToMainActivity + " called");
        Intent intent_go_back_to_main_activity = new Intent(this, MainActivity.class);
        startActivity(intent_go_back_to_main_activity);
    }
}
