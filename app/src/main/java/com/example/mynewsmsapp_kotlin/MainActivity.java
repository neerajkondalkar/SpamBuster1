package com.example.mynewsmsapp_kotlin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> sms_messages_list = new ArrayList<>();
    ListView messages;
    ArrayAdapter array_adapter;
    EditText input;
    SmsManager sms_manager = SmsManager.getDefault();

    private static MainActivity inst;

    //will be used as requestCode parameter in requestPermissions(new String[]{Manifest.permission.READ_SMS}, REQUESTCODEFORPERMISSIONS_READSMS_ENDOFPERMISSIONS);
    private static final int REQUESTCODEFORPERMISSIONS_READSMS_ENDOFPERMISSIONS = 27015; //only for READSMS permission

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messages = (ListView) findViewById(R.id.messages);
        input = (EditText) findViewById(R.id.sms_text_input);
        array_adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sms_messages_list);
        messages.setAdapter(array_adapter);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            EditText input;
            getPermissionToReadSms();
        } else {
           refreshSmsInbox();
        }
    }

    // this is a callback from requestPermissions(new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_REQUEST);
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUESTCODEFORPERMISSIONS_READSMS_ENDOFPERMISSIONS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read SMS permission granted", Toast.LENGTH_SHORT).show();
                refreshSmsInbox();
            } else {
                Toast.makeText(this, "Read SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToReadSms(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED){
            //if permission is not granted then show an education UI to give reason to user
            if(shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)){
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            //and then let the system request permission from user for your app.
            //results in callback to onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
            requestPermissions(new String[]{Manifest.permission.READ_SMS}, REQUESTCODEFORPERMISSIONS_READSMS_ENDOFPERMISSIONS);
        }
    }

    public  void refreshSmsInbox(){
        ContentResolver content_resolver = getContentResolver(); // CONTINUE READING FROM HERE
        Cursor sms_inbox_cursor = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        System.out.println("[DEBUG] all columns in sms/inbox : \n [DEBUG] \n");
        for(String str_col : sms_inbox_cursor.getColumnNames()) {
            System.out.print(" " + str_col);
        }
        System.out.println();
        int index_body = sms_inbox_cursor.getColumnIndex("body");
        System.out.println("[DEBUG] Line 91 returns : " + index_body + "\n");
        int index_address = sms_inbox_cursor.getColumnIndex("address");
        System.out.println("[DEBUG] Line 94 returns : " + index_address + "\n");
        if (index_body < 0 || !sms_inbox_cursor.moveToFirst()){
            return;
        }
        array_adapter.clear();
        do{
            String str = "SMS From: " + sms_inbox_cursor.getString(index_address) + "\n" + sms_inbox_cursor.getString(index_body);
            array_adapter.add(str);
        }while (sms_inbox_cursor.moveToNext());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onClickSendButton(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            getPermissionToReadSms(); //will define later
        } else {
            sms_manager.sendTextMessage("+919320969783", null, input.getText().toString(), null, null);
            Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateInbox(final String sms_message){
        array_adapter.insert(sms_message, 0);
        array_adapter.notifyDataSetChanged();
    }

    public static MainActivity instance() {
        return inst;
    }
}

