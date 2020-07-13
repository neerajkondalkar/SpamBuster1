package com.example.mynewsmsapp_kotlin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ComposeSmsActivity extends AppCompatActivity {

    private static final String TAG = "[MY_DEBUG] " + ComposeSmsActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        String TAG_onCreate = " onCreate(): ";
        Log.d(TAG, TAG_onCreate + " called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_sms);

        EditText input_contact = findViewById(R.id.edit_text_phone_or_name);
        EditText input_sms = findViewById(R.id.sms_text_input);

        checkNecessaryPermissions();
//        Button button_send_sms = findViewById(R.id.button_send_sms);
//        Button button_back = findViewById(R.id.button_back_to_activity_main);

    }

    public void checkNecessaryPermissions() {
        String TAG_checkNecessaryPermissions = " checkNecessaryPermissions(): ";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, TAG_checkNecessaryPermissions + " SEND_SMS permission granted");
        } else {
            Log.d(TAG, TAG_checkNecessaryPermissions + " SEND_SMS permission denied");
        }
    }

    public void onClickSendSms(View view){
        String TAG_onClickSendSms = " onClickSendSms(): ";
        Log.d(TAG, TAG_onClickSendSms + " called");
    }

    public void onClickBackToMainActivity(View view){
        String TAG_onClickBackToMainActivity = " onClickBackToMainActivity(): ";
        Log.d(TAG, TAG_onClickBackToMainActivity + " called");
        Intent intent_go_back_to_main_activity = new Intent(this, MainActivity.class);
        startActivity(intent_go_back_to_main_activity);
    }
}
