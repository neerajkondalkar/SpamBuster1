package com.example.mynewsmsapp_kotlin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "[MY_DEBUG] " + MainActivity.class.getSimpleName(); //for debugging
    private static final String KEY_LIST_CONTENTS = "ListContent";
    ArrayList<String> sms_messages_list = new ArrayList<>();
    ListView messages;
    ArrayAdapter array_adapter;
    EditText input;
    SmsManager sms_manager = SmsManager.getDefault();

    // store current instance in inst, will be used in SmsBroadCast receiver to  call
    // MainActivity.updateInbox() with the current instance using function instance() defined at the bottom of MainActivity class
    private static MainActivity inst;

    public static boolean active = false;

    //will be used as requestCode parameter in requestPermissions(new String[]{Manifest.permission.READ_SMS}, REQUESTCODEFORPERMISSIONS_READSMS_ENDOFPERMISSIONS);
    private static final int REQUESTCODEFORPERMISSIONS_READSMS_READCONTACTS_ENDOFPERMISSIONS = 27015; //only for READSMS permission

    @Override
    public void onStart() {
        String TAG_onstart;
        TAG_onstart = TAG + " onStart(): ";
        Log.d(TAG_onstart, " called ");
        super.onStart();
        active = true; //indicate that activity is live so as to refreshInbox //check in the overriden SmsBroadcastReceiver.onReceive() method
        inst = this;
    }

    @Override
    public void onStop() {
        final String TAG_onStop = TAG + " onStop(): ";
        Log.d(TAG_onStop, " called ");
        super.onStop();
        //active = false; //indicate that activity is not live so as to refreshInbox //check in the overriden SmsBroadcastReceiver.onReceive() method
                            //                  creates problem because even if app is minimized it says not active resulting in no new message display
                            // puting active = false in onDestroy;
    }

    @Override
    protected void onDestroy() {
        final String TAG_onStop = " onDestroy(): ";
        Log.d(TAG, TAG_onStop + " called ");
        super.onDestroy();
        active = false; //indicate that activity is killed ,   check SmsBroadcastReceiver.onReceive() method
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String TAG_onCreate = TAG + " onCreate() ";
        Log.d(TAG_onCreate, " called ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messages = (ListView) findViewById(R.id.messages);
        input = (EditText) findViewById(R.id.sms_text_input);
        array_adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sms_messages_list);
        messages.setAdapter(array_adapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            //if permission to READ_SMS is not granted
            EditText input;
            getNecessaryPermissions();
        } else {
            //if permission is already granted previously
           refreshSmsInbox();
//            getSmsFromInbox();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        final String TAG_onSaveInstanceState = " onSaveInstanceState(): ";
        Log.d(TAG, TAG_onSaveInstanceState + "called");
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(KEY_LIST_CONTENTS, sms_messages_list);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        final String TAG_onRestoreInstanceState = " onRestoreInstanceState(): ";
        Log.d(TAG, TAG_onRestoreInstanceState + "called");
        super.onRestoreInstanceState(savedInstanceState);
        sms_messages_list = savedInstanceState.getStringArrayList(KEY_LIST_CONTENTS);
    }

    // this is a callback from requestPermissions(new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_REQUEST);
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       final  String TAG_onRequestPermissionResult = "onReqestPermissionResult(): ";
       Log.d(TAG, TAG_onRequestPermissionResult + " called ");
        if (requestCode == REQUESTCODEFORPERMISSIONS_READSMS_READCONTACTS_ENDOFPERMISSIONS) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read SMS and Read contacts permission granted", Toast.LENGTH_SHORT).show();
                Log.d(TAG,  TAG_onRequestPermissionResult + permissions[0] + " " + permissions[1] + " permissions granted");
                refreshSmsInbox();
            } else if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(this, "Read SMS and Read contacts permission denied", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onRequestPermissionResult + permissions[0] + " " + permissions[1] + " permissions denied");
            }
            else if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(this, "Read SMS granted and Read contacts permission denied", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onRequestPermissionResult + permissions[0] + " permission granted " + permissions[1] + " permissions denied");
            }
            else if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Read SMS denied and Read contacts permission granted", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onRequestPermissionResult + permissions[0] + " permission denied " + permissions[1] + " permissions granted");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        }
    }

    //requesting permissions to read sms, read contacts
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getNecessaryPermissions(){
        final String TAG_getNecessaryPermissions = " getNecessartPermissions(): ";
        Log.d(TAG, TAG_getNecessaryPermissions + " called");
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED){
            //if permission is not granted then show an education UI to give reason to user
            if(shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)){
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            //and then let the system request permission from user for your app.
            //results in callback to onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
            requestPermissions(new String[]{Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS}, REQUESTCODEFORPERMISSIONS_READSMS_READCONTACTS_ENDOFPERMISSIONS);
        }
    }

    //reads all sms from database and display all the sms using the array_adapter
    public  void refreshSmsInbox() {
//        ArrayList<String> sms_list = new ArrayList<String>();

        final String TAG_refreshSmsInbox = " refreshSmsInbox(): ";
        Log.d(TAG, TAG_refreshSmsInbox + " called ");
        ContentResolver content_resolver = getContentResolver();
        Cursor sms_inbox_cursor = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");
        //[DEBUG] start
//        System.out.print(TAG + TAG_refreshSmsInbox + " [DEBUG] "+ " refreshSmsInbox() :  all columns in sms/inbox : \n [DEBUG]");
        Log.d(TAG, TAG_refreshSmsInbox + " all columns in sms/inbox : ");
        int column_index=0;
        for(String str_col : sms_inbox_cursor.getColumnNames()) {
            //System.out.print(" " + str_col);
            Log.d(TAG, TAG_refreshSmsInbox + " [ " + column_index + " ] " + str_col);
            column_index++;
        }
        System.out.println();
        //[DEBUG] end

        int index_body = sms_inbox_cursor.getColumnIndex("body");
        int index_date = sms_inbox_cursor.getColumnIndex("date");
        Log.d(TAG, TAG_refreshSmsInbox+  "index body = " + index_body + '\n');
        int index_address = sms_inbox_cursor.getColumnIndex("address");
        Log.d(TAG, TAG_refreshSmsInbox+  "index_address = " + index_address + '\n');
        if (index_body < 0 || !sms_inbox_cursor.moveToFirst()){
            return;
        }
        array_adapter.clear();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
        String date_str="";
        long milli_seconds=0;
        Calendar calendar = Calendar.getInstance();
        String printable_date;
        do{
            date_str = sms_inbox_cursor.getString(index_date);
            milli_seconds = Long.parseLong(date_str);
            Log.d(TAG, TAG_refreshSmsInbox+ "milli_seconds = " + Long.toString(milli_seconds));
            calendar.setTimeInMillis(milli_seconds);
            Log.d(TAG, TAG_refreshSmsInbox + "formatter.format(calender.getTime()) returns " + formatter.format((calendar.getTime())));
            printable_date = formatter.format(calendar.getTime());
            String contact_name = getContactName(this, sms_inbox_cursor.getString(index_address));
            Log.d(TAG, TAG_refreshSmsInbox + "getContactName() returns = " + contact_name);

            String str = "SMS From: "  + contact_name + "\n Recieved at: " + printable_date + "\n" + sms_inbox_cursor.getString(index_body);

            /*
            if(sms_inbox_cursor.getString(index_address).equals("9999988888")) {

                array_adapter.add(str);
            }
            */
            array_adapter.add(str); //add the message to adapter list view
//            sms_list.add(str);
        }while (sms_inbox_cursor.moveToNext());
    }

    public  static  String getContactName(Context context, String phone_number){
        final String TAG_getContactName = " getContactName(): ";
        Log.d(TAG, TAG_getContactName + " called ");
        Log.d(TAG, TAG_getContactName + " phone_number = " + phone_number);
        ContentResolver content_resolver = context.getContentResolver();
        Log.d(TAG, TAG_getContactName + "content_resolver = " + content_resolver);
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone_number));
        Log.d(TAG, TAG_getContactName + "uri = " + uri);
        Cursor cursor = content_resolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        Log.d(TAG, TAG_getContactName + "cursor = " + cursor);
        if (cursor == null){
            return phone_number;
        }
        String name = phone_number;
        if(cursor.moveToNext()){
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            Log.d(TAG, TAG_getContactName + "name = " + name);
        }
        if (cursor!=null && !cursor.isClosed()){
            cursor.close();
        }
        return name;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onClickSendButton(View view) {
        final String TAG_onClickSendButton = " onClickSendButton(): ";
        Log.d(TAG, TAG_onClickSendButton  +" called ");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            getNecessaryPermissions();
        } else {
            sms_manager.sendTextMessage("+919320969783", null, input.getText().toString(), null, null);
            Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();
        }
    }

    //to update the array adapter view so that the index 0 of list view will show the latest sms received
    public void updateInbox(final String sms_message){
        final String TAG_updateInbox = " updateInbox(): ";
        Log.d(TAG, TAG_updateInbox  +" called ");

        //always place new sms at top i.e index 0
        array_adapter.insert(sms_message, 0);

        //notify the individual views in adapter view about the change
        array_adapter.notifyDataSetChanged();
    }

    //just to preserve the current instance so that it is not lost when we return from SmsBroadcastReciever class
    public static MainActivity instance() {
        return inst;
    }
}

