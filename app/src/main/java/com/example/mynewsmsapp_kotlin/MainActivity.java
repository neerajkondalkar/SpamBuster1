package com.example.mynewsmsapp_kotlin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// This activity is used for:
// 1. Showing all messages to user
// 2. A button to take user to ComposeSmsActivity to compose a new sms
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "[MY_DEBUG] " + MainActivity.class.getSimpleName(); //for debugging
    private static final String KEY_LIST_CONTENTS = "ListContent"; //for SavedInstanceState and RestoreInstanceState which turned out of no use
    ArrayList<String> sms_messages_list = new ArrayList<>();
    ListView messages;
    ArrayAdapter array_adapter;
//    EditText input;


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
    protected void onResume() {
        String TAG_onResume = " onResume(): ";
        Log.d(TAG, TAG_onResume + " called ");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        final String TAG_onStop = " onDestroy(): ";
        Log.d(TAG, TAG_onStop + " called ");
        super.onDestroy();
        active = false; //indicate that activity is killed, check bottom of SmsBroadcastReceiver.onReceive() method
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        final String TAG_onSaveInstanceState = " onSaveInstanceState(): ";
        Log.d(TAG, TAG_onSaveInstanceState + "called");
        super.onSaveInstanceState(outState);
//        outState.putStringArrayList(KEY_LIST_CONTENTS, sms_messages_list);   //<------------TOO LARGE, causes error
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        final String TAG_onRestoreInstanceState = " onRestoreInstanceState(): ";
        Log.d(TAG, TAG_onRestoreInstanceState + "called");
        super.onRestoreInstanceState(savedInstanceState);
//        sms_messages_list = savedInstanceState.getStringArrayList(KEY_LIST_CONTENTS);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String TAG_onCreate = TAG + " onCreate() ";
        Log.d(TAG_onCreate, " called ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messages = (ListView) findViewById(R.id.messages);
//        input = (EditText) findViewById(R.id.sms_text_input);
        array_adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sms_messages_list);
        messages.setAdapter(array_adapter);

//        //to delete the database. so that everytime a new database is created
        try {
            this.deleteDatabase(SpamBusterdbHelper.DATABASE_NAME);
        }
        catch (Exception e){
            Log.d(TAG, TAG_onCreate + " Exception : " + e);
        }

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


        // ------------------------------------       READING from database         -----------------------------

        SpamBusterdbHelper db_helper = new SpamBusterdbHelper(this);

        Log.d(TAG, TAG_refreshSmsInbox + " reading the table before inserting anything ... ");

        // start here - only to read _ID to determine whether we want to insert into table or not, so that we dont get duplicates
        SQLiteDatabase db_read_for_id = db_helper.getReadableDatabase();

        String[] projection_id = { BaseColumns._ID};
        String selection_id = null;
        String[] selection_args = null;
        String sort_order = SpamBusterContract.TABLE_ALL._ID + " DESC";
        Cursor cursor_read_id = db_read_for_id.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                projection_id,             // The array of columns to return (pass null to get all)
                selection_id,              // The columns for the WHERE clause
                selection_args,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sort_order               // The sort order
        );

        List item_ids = new ArrayList();
        item_ids.clear();
        // topmost is largest/latest _ID
        while (cursor_read_id.moveToNext()){
            String temp_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID));
            Log.d(TAG, TAG_refreshSmsInbox + " _ID = " + temp_id_holder);
            item_ids.add(temp_id_holder);
        }

        try {
            Log.d(TAG, TAG_refreshSmsInbox + " item_ids[0] = " + item_ids.get(0).toString());
        }
        catch (Exception e) {
            Log.d(TAG, TAG_refreshSmsInbox + " fresh db so could not read _ID from our TABLE_ALL ! ");
            Log.d(TAG, TAG_refreshSmsInbox + " Exception : " + e);
        }
            //so now we have a list of all IDs that are already present in the table, i.e we know what sms are already present in the table
        // end READING from db here



        String date_str = "";
        long milli_seconds = 0;
        Calendar calendar = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
        String printable_date;

        ContentResolver content_resolver = getContentResolver();

//         ------------- READING the topmost _ID in sms/inbox ------------------

        Cursor cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, "_id DESC");
        String latest_sms_id = "";
        if (!cursor_check_sms_id.moveToFirst()) {
            int index_id = cursor_check_sms_id.getColumnIndex("_id");
            latest_sms_id = cursor_check_sms_id.getString(index_id);
            Log.d(TAG, TAG_refreshSmsInbox + " latest_sms_id = " + latest_sms_id);
        }
        boolean inbox_sync = false;


//      ------------------ CHECKING IF INBOX IS IN SYNC, so as to prevent duplicate sms -----------------

        try {
            if (item_ids.get(0).toString().equals(latest_sms_id)) {
                //this means that the inbuilt sms inbox and our ALL table is alread yin sync, so no need to insert values
                Log.d(TAG, TAG_refreshSmsInbox + " TABLE_ALL is in sync  with sms/inbox ! ");
                inbox_sync = true;
            }
        }
        catch (Exception e){
            Log.d(TAG, TAG_refreshSmsInbox + " database is brand new, so can't read anything from our TABLE_ALL for now.");
            Log.d(TAG, TAG_refreshSmsInbox + " Exception : " + e);
        }
        // Gets the data repository in write mode
        SQLiteDatabase db = db_helper.getWritableDatabase();
        db.beginTransaction();

//     -------------------------- inbox is not  IS NOT IN SYNC, therefore INSERT all the new messages in our db table TABLE_ALL -----------------------

        if (!inbox_sync) {

            Log.d(TAG, TAG_refreshSmsInbox + "TABLE_ALL  is not in sync  with sms/inbox ! Hence insert new messages in our db ");

            Cursor sms_inbox_cursor = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, "_id DESC");
            //[DEBUG] start
//        System.out.print(TAG + TAG_refreshSmsInbox + " [DEBUG] "+ " refreshSmsInbox() :  all columns in sms/inbox : \n [DEBUG]");
            Log.d(TAG, TAG_refreshSmsInbox + " all columns in sms/inbox : ");
            int column_index = 0;
            for (String str_col : sms_inbox_cursor.getColumnNames()) {
                //System.out.print(" " + str_col);
                Log.d(TAG, TAG_refreshSmsInbox + " [ " + column_index + " ] " + str_col);
                column_index++;
            }
            System.out.println();
            //[DEBUG] end


            int index_body = sms_inbox_cursor.getColumnIndex("body");
            int index_date = sms_inbox_cursor.getColumnIndex("date");
            Log.d(TAG, TAG_refreshSmsInbox + "index body = " + index_body + '\n');
            int index_address = sms_inbox_cursor.getColumnIndex("address");
            Log.d(TAG, TAG_refreshSmsInbox + "index_address = " + index_address + '\n');
            if (index_body < 0 || !sms_inbox_cursor.moveToFirst()) {
                return;
            }

            array_adapter.clear();

            date_str = sms_inbox_cursor.getString(index_date);
            milli_seconds = Long.parseLong(date_str);
            Log.d(TAG, TAG_refreshSmsInbox + "milli_seconds = " + Long.toString(milli_seconds));
            calendar.setTimeInMillis(milli_seconds);
            Log.d(TAG, TAG_refreshSmsInbox + "formatter.format(calender.getTime()) returns " + formatter.format((calendar.getTime())));
            printable_date = formatter.format(calendar.getTime());


            do {

                String address = sms_inbox_cursor.getString(index_address); //actual phone number
                String contact_name = getContactName(this, address); //contact name retirved from phonelookup
                Log.d(TAG, TAG_refreshSmsInbox + "getContactName() returns = " + contact_name);
                String sms_body = sms_inbox_cursor.getString(index_body);

                String str = "SMS From: " + contact_name + "\n Recieved at: " + printable_date + "\n" + sms_body;

                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                Log.d(TAG, TAG_refreshSmsInbox + " inserting value of address = " + address + " into COLUMN_SMS_ADDRESS");
                values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS, address); //insert value contact_name into COLUMN_SMS_ADDRESS
                Log.d(TAG, TAG_refreshSmsInbox + " inserting value of sms_body = " + sms_body + " into COLUMN_SMS_BODY");
                values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY, sms_body);  // insert value sms_body in COLUMN_SMS_BODY
                Log.d(TAG, TAG_refreshSmsInbox + " inserting value of date_str = " + date_str + " into COLUMN_SMS_EPOCH_DATE");
                values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE, date_str);  // insert value date_str in COLUMN_SMS_EPOCH_DATE

                // Insert the new row, returning the primary key value of the new row
                long newRowId = db.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, null, values);
                if (newRowId == -1) {
                    Log.d(TAG, TAG_refreshSmsInbox + " insert failed\n\n");
                } else {
                    Log.d(TAG, TAG_refreshSmsInbox + " Insert Complete! returned newRowId = " + newRowId + "\n\n");
                }
            /*
            if(sms_inbox_cursor.getString(index_address).equals("9999988888")) {

                array_adapter.add(str);
            }
            */
                array_adapter.add(str); //add the message to adapter list view
//            sms_list.add(str);
            } while (sms_inbox_cursor.moveToNext());

            Log.d(TAG, TAG_refreshSmsInbox + " Done inserting value! \n");
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();

            //end of inserting into db
        }

//   -----------------------         READING from table ------------------------

        Log.d(TAG, TAG_refreshSmsInbox + "\n\n");
        Log.d(TAG, TAG_refreshSmsInbox + " reading values from database");
//        SpamBusterdbHelper read_dbHelper = new SpamBusterdbHelper(this);
        SQLiteDatabase db_read = db_helper.getReadableDatabase();
        db_read.beginTransaction();
        // Define a projection that specifies which columns from the database
// you will actually use after this query.


        String[] projection = {
                BaseColumns._ID,
                SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY,
                SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS,
                SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE
        };

// Filter results WHERE "title" = 'My Title'
//        String selection = SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS + " = ? "
//                + SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY + " = ? ";
//        String[] selectionArgs = { "*, *" };
        String selection = null;
        String[] selectionArgs = null;

// How you want the results sorted in the resulting Cursor
        String sortOrder =
                SpamBusterContract.TABLE_ALL._ID;

        Cursor cursor = db_read.query(
                SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );

//        List itemIds = new ArrayList<>();
        while(cursor.moveToNext()) {
            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID));
//            itemIds.add(itemId);
            String sms_body = cursor.getString(cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY));
            String sms_address = cursor.getString(cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS));
            String epoch_date = cursor.getString(cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE));
            Log.d(TAG, TAG_refreshSmsInbox + " itemId = " + itemId);
            Log.d(TAG, TAG_refreshSmsInbox + " sms_body = " + sms_body);      // EDIT HERE
            Log.d(TAG, TAG_refreshSmsInbox + " sms_address = " + sms_address);      // EDIT HERE
            milli_seconds = Long.parseLong(epoch_date);
            calendar.setTimeInMillis(milli_seconds);
            printable_date = formatter.format(calendar.getTime());
            Log.d(TAG, TAG_refreshSmsInbox + " epoch_date = " + epoch_date + " which is : " + printable_date);      // EDIT HERE
        }
        cursor.close();
        db_read.setTransactionSuccessful();
        db_read.endTransaction();
        db_read.close();
        db_helper.close();


        //end of READING from table

        //end of all DATABASE operations
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
        final int index_displayname = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
        Log.d(TAG, TAG_getContactName + "index_displayname = " + index_displayname);
        Log.d(TAG, TAG_getContactName + "cursor = " + cursor);
        if (cursor == null){
            return phone_number;
        }
        String name = phone_number;
        if(cursor.moveToNext()){
            name = cursor.getString(index_displayname);
            Log.d(TAG, TAG_getContactName + "name = " + name);
        }
        if (cursor!=null && !cursor.isClosed()){
            cursor.close();
        }
        return name;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onClickComposeSms(View view) {
        final String TAG_onClickSendButton = " onClickSendButton(): ";
        Log.d(TAG, TAG_onClickSendButton  +" called ");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            getNecessaryPermissions();
        }
        else {
            Intent intent_to_send_new_sms = new Intent(MainActivity.this, ComposeSmsActivity.class);
            startActivity(intent_to_send_new_sms);
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
        Log.d(TAG, " instance(): called");
        return inst;
    }

    public void backToMainActivity(){
        //do nothing
        Log.d(TAG, "backToMainActivity(): called");
    }

}

