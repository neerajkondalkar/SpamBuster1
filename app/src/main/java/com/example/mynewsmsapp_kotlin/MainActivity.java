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

    private static final int READ_SMS_PERMISSION_REQUEST = 1;

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
           getPermissionToReadSms();   //will define later
        } else {
           refreshSmsInbox();        //will define later
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_SMS_PERMISSION_REQUEST) {
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
            if(shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)){
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_REQUEST);
        }
    }

    public  void refreshSmsInbox(){
        ContentResolver content_resolver = getContentResolver();
        Cursor sms_inbox_cursor = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        int index_body = sms_inbox_cursor.getColumnIndex("body");
        int index_address = sms_inbox_cursor.getColumnIndex("address");
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
            sms_manager.sendTextMessage("9999966666", null, input.getText().toString(), null, null);
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

