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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

// This activity is used for:
// 1. Showing all messages to user
// 2. A button to take user to ComposeSmsActivity to compose a new sms
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "[MY_DEBUG] " + MainActivity.class.getSimpleName(); //for debugging
    private static final String KEY_LIST_CONTENTS = "ListContent"; //for SavedInstanceState and RestoreInstanceState which turned out of no use
    private static final int TABLE_ALL = 1;
    private static final int TABLE_INBOX = 2;
    private static final int TABLE_SPAM = 3;
    private static final int TABLE_CONTENT_SMS_INBOX = 4;

    Handler handler_main = new Handler();

    private static boolean table_all_sync_inbox = false;   //shows whether our TABLE_ALL is in sync with inbuilt sms/inbox

    ArrayList<String> sms_messages_list = new ArrayList<>();
//    ListView messages;
    RecyclerView messages;
    ArrayAdapter array_adapter;
//    EditText input;
    SmsAdapter sms_adapter;
    SpamBusterdbHelper db_helper;

    // store current instance in inst, will be used in SmsBroadCast receiver to  call
    // MainActivity.updateInbox() with the current instance using function instance() defined at the bottom of MainActivity class
    private static MainActivity inst;

    public static boolean active = false;

    //will be used as requestCode parameter in requestPermissions(new String[]{Manifest.permission.READ_SMS}, REQUESTCODEFORPERMISSIONS_READSMS_ENDOFPERMISSIONS);
    private static final int REQUESTCODEFORPERMISSIONS_READSMS_READCONTACTS_ENDOFPERMISSIONS = 27015; //only for READSMS permission


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    public void onStart() {
        String TAG_onstart;
        TAG_onstart = TAG + " onStart(): ";
        Log.d(TAG_onstart, " called ");
        super.onStart();
        active = true; //indicate that activity is live so as to refreshInbox //check in the overriden SmsBroadcastReceiver.onReceive() method
        inst = this;
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    public void onStop() {
        final String TAG_onStop = TAG + " onStop(): ";
        Log.d(TAG_onStop, " called ");
        super.onStop();

    }


    //------------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onRestart() {
        String TAG_onRestart = " onRestart(): ";
        Log.d(TAG, TAG_onRestart + " called ");
        super.onRestart();
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onResume() {
        String TAG_onResume = " onResume(): ";
        Log.d(TAG, TAG_onResume + " called ");
        super.onResume();
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onDestroy() {
        final String TAG_onStop = " onDestroy(): ";
        Log.d(TAG, TAG_onStop + " called ");
        super.onDestroy();
        active = false; //indicate that activity is killed, check bottom of SmsBroadcastReceiver.onReceive() method
        db_helper.close();

    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        final String TAG_onSaveInstanceState = " onSaveInstanceState(): ";
        Log.d(TAG, TAG_onSaveInstanceState + "called");
        super.onSaveInstanceState(outState);
//        outState.putStringArrayList(KEY_LIST_CONTENTS, sms_messages_list);   //<------------TOO LARGE, causes error
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        final String TAG_onRestoreInstanceState = " onRestoreInstanceState(): ";
        Log.d(TAG, TAG_onRestoreInstanceState + "called");
        super.onRestoreInstanceState(savedInstanceState);
//        sms_messages_list = savedInstanceState.getStringArrayList(KEY_LIST_CONTENTS);
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String TAG_onCreate = TAG + " onCreate() ";
        Log.d(TAG_onCreate, " called ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messages = (RecyclerView) findViewById(R.id.messages);

//        ----------------------- DELETE DATABASE --------------------

//        //to delete the database. so that everytime a new database is created

//        try {
//            this.deleteDatabase(SpamBusterdbHelper.DATABASE_NAME);
//        }
//        catch (Exception e){
//            Log.d(TAG, TAG_onCreate + " Exception : " + e);
//        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            //if permission to READ_SMS is not granted
            EditText input;
            getNecessaryPermissions();
        } else {
            //if permission is already granted previously
            refreshSmsInbox();
        }
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    // this is a callback from requestPermissions(new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_REQUEST);
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        final String TAG_onRequestPermissionResult = "onReqestPermissionResult(): ";
        Log.d(TAG, TAG_onRequestPermissionResult + " called ");
        if (requestCode == REQUESTCODEFORPERMISSIONS_READSMS_READCONTACTS_ENDOFPERMISSIONS) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read SMS and Read contacts permission granted", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onRequestPermissionResult + permissions[0] + " " + permissions[1] + " permissions granted");
                refreshSmsInbox();
            } else if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Read SMS and Read contacts permission denied", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onRequestPermissionResult + permissions[0] + " " + permissions[1] + " permissions denied");
            } else if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Read SMS granted and Read contacts permission denied", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onRequestPermissionResult + permissions[0] + " permission granted " + permissions[1] + " permissions denied");
            } else if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read SMS denied and Read contacts permission granted", Toast.LENGTH_SHORT).show();
                Log.d(TAG, TAG_onRequestPermissionResult + permissions[0] + " permission denied " + permissions[1] + " permissions granted");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        }
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    //requesting permissions to read sms, read contacts
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getNecessaryPermissions() {
        final String TAG_getNecessaryPermissions = " getNecessartPermissions(): ";
        Log.d(TAG, TAG_getNecessaryPermissions + " called");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            //if permission is not granted then show an education UI to give reason to user
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)) {
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            //and then let the system request permission from user for your app.
            //results in callback to onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
            requestPermissions(new String[]{Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS}, REQUESTCODEFORPERMISSIONS_READSMS_READCONTACTS_ENDOFPERMISSIONS);
        }
    }


//    ----------------------------------------------------------------------------------------------------------------------------------------------


    //reads all sms from database and display all the sms using the array_adapter
    public void refreshSmsInbox() {
//        ArrayList<String> sms_list = new ArrayList<String>();
        final String TAG_refreshSmsInbox = " refreshSmsInbox(): ";
        Log.d(TAG, TAG_refreshSmsInbox + " called ");
        db_helper = new SpamBusterdbHelper(this);



        // ------------------------------------       READING from database  table TABLE_ALL      -----------------------------


        Log.d(TAG, TAG_refreshSmsInbox + " reading the table before inserting anything ... ");
        // start here - only to read _ID to determine whether we want to insert into table or not, so that we dont get duplicates
        SQLiteDatabase db_read_for_id = db_helper.getReadableDatabase();
        List item_ids_tableall = new ArrayList();
        item_ids_tableall.clear();
        item_ids_tableall = getAllIdsFromDbTable(db_read_for_id, TABLE_ALL);


//        -------------------- DELETE database ---------------------------

//        try {
//            Log.d(TAG, TAG_refreshSmsInbox + " item_ids_tableall[0] = " + item_ids_tableall.get(0).toString());
//            latest_sms_id_in_table_all = item_ids_tableall.get(0).toString();
//            Log.d(TAG, TAG_refreshSmsInbox + " This means: latest_sms_id_in_table_all = " + latest_sms_id_in_table_all);
//        }
//        catch (Exception e) {
//            Log.d(TAG, TAG_refreshSmsInbox + " fresh db so could not read _ID column from our TABLE_ALL !  i.e could not set latest_sms_id_in_table_all");
//            Log.d(TAG, TAG_refreshSmsInbox + " Exception : " + e);
//        }
//            //so now we have a list of all IDs that are already present in the table, i.e we know what sms are already present in the table

        // end READING from TABLE_ALL here


//         ------------- READING the topmost _id in sms/inbox ------------------
//          ------------------ PRINTING THE WHOLE sms/inbox ------------------
        List item_ids_inbox = new ArrayList();
        item_ids_inbox = getAllIdsFromDbTable(null, TABLE_CONTENT_SMS_INBOX);

        //printing how many messsages are there in SMS/INBOX and TABLE_ALL
        Log.d(TAG, TAG_refreshSmsInbox + " item_ids_tableall.size() = " + item_ids_tableall.size());
        Log.d(TAG, TAG_refreshSmsInbox + " item_ids_inbox.size() = " + item_ids_inbox.size());
        Log.d(TAG, TAG_refreshSmsInbox + "");

        // compare the lists item_ids_tableall and item_ids_inbox
        List missing_item_ids = new ArrayList();
        missing_item_ids.clear();
        //checking if all the  ids present in item_ids_inbox are also present in item_ids_tableall
        //if not, the ids present in items_ids_inbox but not in items_ids_tableall , will be added to list missing_item_ids
        missing_item_ids = compareSmsIdsInLists(TABLE_ALL, TABLE_CONTENT_SMS_INBOX, item_ids_tableall, item_ids_inbox);
        if (missing_item_ids.isEmpty()) {
            table_all_sync_inbox = true;
        } else {
            table_all_sync_inbox = false;
        }
        Log.d(TAG, TAG_refreshSmsInbox + "");


        // Gets the data repository in write mode
        SQLiteDatabase db = db_helper.getWritableDatabase();
        db.beginTransaction();


//     -------------------------- inbox is not  IS NOT IN SYNC, therefore INSERT all the new messages in our db table TABLE_ALL -----------------------

//        array_adapter.clear();

        if (!table_all_sync_inbox) {

            Log.d(TAG, TAG_refreshSmsInbox + "TABLE_ALL  is not in sync  with sms/inbox ! Hence update db TABLE_ALL with new messages ");
            // update the TABLE_ALL according to SMS/INBOX
            updateMissingValuesInDbTable(db, TABLE_ALL, TABLE_CONTENT_SMS_INBOX, missing_item_ids);
            //end of inserting into db
        } else {
            Log.d(TAG, TAG_refreshSmsInbox + " ");
            Log.d(TAG, TAG_refreshSmsInbox + " TABLE_ALL is in sync with SMS/INBOX. So no need to update  TABLE_ALL ");
            Log.d(TAG, TAG_refreshSmsInbox + " ");
        }


//   -----------------------         READING all entried from TABLE_ALL  and filling the  array_adapter  with those messages i.e from reading TABLE_ALL------------------------
        //                          also fill the array_adapter with the values read from the TABLE_ALL

        Log.d(TAG, TAG_refreshSmsInbox + " ");
        Log.d(TAG, TAG_refreshSmsInbox + " Now reading values from TABLE_ALL ");
        Log.d(TAG, TAG_refreshSmsInbox + " ");

        db.endTransaction();

        SQLiteDatabase db1 = db_helper.getReadableDatabase();
        //inserting a dummy item at index 0 of list   (list will be cleared once SmsAdapter object is created)
        sms_messages_list.add(0, "dummy");
        sms_adapter = new SmsAdapter(this, sms_messages_list);
        messages.setAdapter(sms_adapter);
        messages.setLayoutManager(new LinearLayoutManager(this));
        readMessagesFromDbTable(TABLE_ALL, db1);
        //end of READING from table
        //end of all DATABASE operations
    }

//    ---------------------------------------------------------------------------------------------------------------------------------------


    // this fucntion returns a list of ids i.e the COLUMN_CORRES_INBOX_ID
    public List getAllIdsFromDbTable(SQLiteDatabase db, int table) {
        final String TAG_getAllIdsFromDbTable = " getAllIdsFromDbTable(): ";
        List item_ids = new ArrayList();
        item_ids.clear();

        switch (table) {

//            ------------for TABLE_ALL ----------------

            case TABLE_ALL:
                String[] projection_id = {
                        BaseColumns._ID,
                        SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID
                };

                String selection_id = null;
                String[] selection_args = null;
                String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID + " DESC";
                Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                        projection_id,             // The array of columns to return (pass null to get all)
                        selection_id,              // The columns for the WHERE clause
                        selection_args,          // The values for the WHERE clause
                        null,                   // don't group the rows
                        null,                   // don't filter by row groups
                        sort_order               // The sort order
                );
                if (!cursor_read_id.moveToFirst()) {
                    Log.d(TAG, TAG_getAllIdsFromDbTable + " TABLE_ALL is empty! ");
                } else {
                    do {
                        String temp_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID));
                        Log.d(TAG, TAG_getAllIdsFromDbTable + " _id = " + temp_id_holder);
                        String temp_corres_inbox_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
                        Log.d(TAG, TAG_getAllIdsFromDbTable + " corress_inbox_id = " + temp_corres_inbox_id_holder);
                        item_ids.add(temp_corres_inbox_id_holder);
                    } while (cursor_read_id.moveToNext());
                    // topmost is largest/latest _ID
                }
                break;

//                -------------- for TABLE_INBOX -------------------

            case TABLE_INBOX:
                //do nothing for now
                break;

//                -------------  for TABLE_SPAM --------------------

            case TABLE_SPAM:
                //do nothing for now
                break;

//                --------------  for inbuilt SMS INBOX  --------------------

            case TABLE_CONTENT_SMS_INBOX:
                ContentResolver content_resolver = getContentResolver();
                Cursor cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, "_id DESC");
                String latest_sms_id_in_inbuilt_sms_inbox = "";
                String latest_sms_thread_id_in_inbuilt_sms_inbox = "";
                String id_inbox = "";
                String threadid_inbox = "";
                String address_inbox = "";
                String body_inbox = "";

                if (cursor_check_sms_id.moveToFirst()) {
                    Log.d(TAG, TAG_getAllIdsFromDbTable + "");
                    Log.d(TAG, TAG_getAllIdsFromDbTable + " dumping the whole sms/inbox : ");
                    Log.d(TAG, TAG_getAllIdsFromDbTable + "");
                    int index_id = cursor_check_sms_id.getColumnIndex("_id");
                    int index_thread_id = cursor_check_sms_id.getColumnIndex("thread_id");
                    int index_address = cursor_check_sms_id.getColumnIndex("address");
                    int index_body = cursor_check_sms_id.getColumnIndex("body");

                    latest_sms_thread_id_in_inbuilt_sms_inbox = cursor_check_sms_id.getString(index_thread_id);
                    latest_sms_id_in_inbuilt_sms_inbox = cursor_check_sms_id.getString(index_id);

                    Log.d(TAG, TAG_getAllIdsFromDbTable + " latest_sms_id_in_inbuilt_sms_inbox = " + latest_sms_id_in_inbuilt_sms_inbox);
                    Log.d(TAG, TAG_getAllIdsFromDbTable + " latest_sms_thread_id_in_inbuilt_sms_inbox = " + latest_sms_thread_id_in_inbuilt_sms_inbox);
                    Log.d(TAG, TAG_getAllIdsFromDbTable + "");
                    do {
                        id_inbox = cursor_check_sms_id.getString(index_id);
                        threadid_inbox = cursor_check_sms_id.getString(index_thread_id);
                        address_inbox = cursor_check_sms_id.getString(index_address);
                        body_inbox = cursor_check_sms_id.getString(index_body);

                        item_ids.add(id_inbox);
                        Log.d(TAG, TAG_getAllIdsFromDbTable + " id_inbox = " + id_inbox);
                        Log.d(TAG, TAG_getAllIdsFromDbTable + " threadid_inbox = " + threadid_inbox);
                        Log.d(TAG, TAG_getAllIdsFromDbTable + " address_inbox = " + address_inbox);
                        Log.d(TAG, TAG_getAllIdsFromDbTable + " body_inbox = " + body_inbox);
                        Log.d(TAG, TAG_getAllIdsFromDbTable + "");
                    } while (cursor_check_sms_id.moveToNext());
                } else {
                    Log.d(TAG, TAG_getAllIdsFromDbTable + "  inbuilt sms/inbox empty! ");
                }
                Log.d(TAG, TAG_getAllIdsFromDbTable + "");
                break;
            default:
                Log.d(TAG, TAG_getAllIdsFromDbTable + " invalid table selection");
        }
        return item_ids;
    }


//    ----------------------------------------------------------------------------------------------------------------------------------------------


    //checking whether table1 has all the items that are present in table2
    //OR
    //checking whether list1 has all items that are present in list2
    //any item that is present in list2 but not in list1, must be put in missing_item_ids list;

    public List compareSmsIdsInLists(int Table1, int Table2, List list1, List list2) {
        final String TAG_compareSmsIdsInLists = " compareSmsIdsInLists(): ";
        List missing_item_ids = new ArrayList();
        missing_item_ids.clear();

        // check if TABLE_ALL has all messages that are present in SMS/INBOX
        // any message that is present in SMS/INBOX but not present in TABLE_ALL should be put in missing_ids_tableall;
        if (Table1 == TABLE_ALL && Table2 == TABLE_CONTENT_SMS_INBOX) {
            List item_ids_tableall = list1;
            List item_ids_inbox = list2;
            ListIterator iterator_item_ids_inbox = item_ids_inbox.listIterator();
            String current_list_item_ids_inbox;
            if (!iterator_item_ids_inbox.hasNext()) {
                Log.d(TAG, TAG_compareSmsIdsInLists + " List item_ids_inbox is empty ! ");
            } else {
                Log.d(TAG, TAG_compareSmsIdsInLists + " ");
                Log.d(TAG, TAG_compareSmsIdsInLists + " first element in item_ids_tableall = " + item_ids_tableall.get(0).toString());
                Log.d(TAG, TAG_compareSmsIdsInLists + " ");
                Log.d(TAG, TAG_compareSmsIdsInLists + " iterating in item_ids_inbox ");
                Log.d(TAG, TAG_compareSmsIdsInLists + " ");
                for (int i = 0; i < item_ids_inbox.size(); i++) {
                    current_list_item_ids_inbox = iterator_item_ids_inbox.next().toString();
                    Log.d(TAG, TAG_compareSmsIdsInLists + " iterator_item_ids_inbox.next().toString() = " + current_list_item_ids_inbox);
                    try {
                        if (item_ids_tableall.contains(current_list_item_ids_inbox)) {
                            Log.d(TAG, TAG_compareSmsIdsInLists + " present in items_ids_tableall");
                            Log.d(TAG, TAG_compareSmsIdsInLists + " -----------");
                        } else {
                            Log.d(TAG, TAG_compareSmsIdsInLists + " not present in items_ids_tableall ");
                            Log.d(TAG, TAG_compareSmsIdsInLists + " -----------");
                            missing_item_ids.add(current_list_item_ids_inbox);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, TAG_compareSmsIdsInLists + " exception while item_ids_tableall.contains(current_list_item_ids_inbox) :  " + e);
                    }
//            Log.d(TAG, TAG_compareSmsIdsInLists + " " + item_ids_tableall.conta)
                }
            }
            Log.d(TAG, TAG_compareSmsIdsInLists + "");

        }

        return missing_item_ids;
    }


//    ------------------------------------------------------------------------------------------------------------------------------------------------


    //table1 will be updated with values present in table2 if table1 elemets < table2 elements
    public void updateMissingValuesInDbTable(SQLiteDatabase db, int table1, int table2, List missing_item_ids) {
        final String TAG_updateMissingValuesInDbTable = " updateMissingValuesInDbTable(): ";
        Log.d(TAG, TAG_updateMissingValuesInDbTable + " called ");

        String date_str = "";
        long milli_seconds = 0;
        Calendar calendar = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
        String printable_date;

        if (table1 == TABLE_ALL && table2 == TABLE_CONTENT_SMS_INBOX) {
            ContentResolver content_resolver = getContentResolver();
            String[] projection_sms_inbox = null;
            String selection_sms_inbox = null;
            String[] selection_args_sms_inbox = null;
            String sort_order_sms_inbox = " _id DESC ";

            Cursor sms_inbox_cursor = content_resolver.query(Uri.parse("content://sms/inbox"), projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);

            //[DEBUG] start

            //print all columns of sms/inbox

//        System.out.print(TAG + TAG_updateMissingValuesInDbTable + " [DEBUG] "+ " updateMissingValuesInDbTable() :  all columns in sms/inbox : \n [DEBUG]");
//            Log.d(TAG, TAG_updateMissingValuesInDbTable + " all columns in sms/inbox : ");
//            int column_index = 0;
//            for (String str_col : sms_inbox_cursor.getColumnNames()) {
//                //System.out.print(" " + str_col);
//                Log.d(TAG, TAG_updateMissingValuesInDbTable + " [ " + column_index + " ] " + str_col);
//                column_index++;
//            }
//            System.out.println();
            //[DEBUG] end

            int index_id = sms_inbox_cursor.getColumnIndex("_id");
            int index_body = sms_inbox_cursor.getColumnIndex("body");
            int index_date = sms_inbox_cursor.getColumnIndex("date");
            int index_date_sent = sms_inbox_cursor.getColumnIndexOrThrow("date_sent");
//            Log.d(TAG, TAG_updateMissingValuesInDbTable + "index body = " + index_body + '\n');
            int index_address = sms_inbox_cursor.getColumnIndex("address");
//            Log.d(TAG, TAG_updateMissingValuesInDbTable + "index_address = " + index_address + '\n');
            if (index_body < 0 || !sms_inbox_cursor.moveToFirst()) {
                Log.d(TAG, TAG_updateMissingValuesInDbTable + " sms/inbox empty!");
                return;
            }


            date_str = sms_inbox_cursor.getString(index_date);
            milli_seconds = Long.parseLong(date_str);
//            Log.d(TAG, TAG_updateMissingValuesInDbTable + "milli_seconds = " + Long.toString(milli_seconds));
            calendar.setTimeInMillis(milli_seconds);
//            Log.d(TAG, TAG_updateMissingValuesInDbTable + "formatter.format(calender.getTime()) returns " + formatter.format((calendar.getTime())));
            printable_date = formatter.format(calendar.getTime());


            do {

                String address = sms_inbox_cursor.getString(index_address); //actual phone number
                String contact_name = getContactName(this, address); //contact name retirved from phonelookup
                String corress_inbox_id = sms_inbox_cursor.getString(index_id);
                Log.d(TAG, TAG_updateMissingValuesInDbTable + "getContactName() returns = " + contact_name);
                String sms_body = sms_inbox_cursor.getString(index_body);
                date_str = sms_inbox_cursor.getString(index_date);
                String date_sent = sms_inbox_cursor.getString(index_date_sent);

//                String str = "SMS From: " + contact_name + "\n Recieved at: " + printable_date + "\n" + sms_body;

                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();

                Log.d(TAG, TAG_updateMissingValuesInDbTable + " checking if sms is already present, by comaprin _ID of sms/inbox and corres_inbox_id of TABLE_ALL");
//                Log.d(TAG, TAG_updateMissingValuesInDbTable + " item_ids_tableall.contains(corress_inbox_id) =  " + item_ids_tableall.contains(corress_inbox_id));
                Log.d(TAG, TAG_updateMissingValuesInDbTable + " missing_item_ids.contains(corress_inbox_id) =  " + missing_item_ids.contains(corress_inbox_id));

                //insert only the messages which are not already present in the TABLE_ALL i.e insert only the new sms i.e sms which which new _ID

                if (missing_item_ids.contains(corress_inbox_id)) {
                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " new sms confirmed!    _ID = " + corress_inbox_id);
                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " inserting value of corress_inbox_id = " + corress_inbox_id + " into COLUMN_CORRESS_INBOX_ID ");
                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " inserting value of address = " + address + " into COLUMN_SMS_ADDRESS");
                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS, address); //insert value contact_name into COLUMN_SMS_ADDRESS
                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " inserting value of sms_body = " + sms_body + " into COLUMN_SMS_BODY");
                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY, sms_body);  // insert value sms_body in COLUMN_SMS_BODY
                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " inserting value of date_str = " + date_str + " into COLUMN_SMS_EPOCH_DATE");
                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " value of date_sent = " + date_sent);
                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE, date_str);  // insert value date_str in COLUMN_SMS_EPOCH_DATE

                    // Insert the new row, returning the primary key value of the new row
                    long newRowId = db.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, null, values);
                    if (newRowId == -1) {
                        Log.d(TAG, TAG_updateMissingValuesInDbTable + " insert failed\n\n");
                    } else {
                        Log.d(TAG, TAG_updateMissingValuesInDbTable + " Insert Complete! returned newRowId = " + newRowId);
                        Log.d(TAG, TAG_updateMissingValuesInDbTable + " ");
                    }
                } else {
                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " not a new sms. Hence skipping insertion ");
                }

//                array_adapter.add(str); //add the message to adapter list view
//            sms_list.add(str);
            } while (sms_inbox_cursor.moveToNext());

            Log.d(TAG, TAG_updateMissingValuesInDbTable + " Done inserting values! \n");
            Log.d(TAG, TAG_updateMissingValuesInDbTable + " ");
            Log.d(TAG, TAG_updateMissingValuesInDbTable + " ");
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        }

        //end of inserting into db
    }


//    -----------------------------------------------------------------------------------------------------------------------------------------------


    public void readMessagesFromDbTable(int table, SQLiteDatabase db1) {
        final String TAG_readMessagesFromDbTable = " readMessagesFromDbTable(): ";
        int i=0;
        boolean cursor_first = false;
        boolean cursor_next = false;
        Log.d(TAG, TAG_readMessagesFromDbTable + " called ");
        switch (table) {

            case TABLE_ALL:
            ReadDbTableAllAsyncTask readDbTableAllAsyncTask = new ReadDbTableAllAsyncTask(this, db1);
            Log.d(TAG, "readMessagesFromDbTable: executing readDb thread in background");
            ArrayList msg1_list = new ArrayList();
            readDbTableAllAsyncTask.execute(msg1_list);
            break;
        }

    }

    //    -------------------------------------------------------------------------------------------------------------------------------------------------
    public static String getContactName(Context context, String phone_number) {
        final String TAG_getContactName = " getContactName(): ";
        Log.d(TAG, TAG_getContactName + " called ");
        ContentResolver content_resolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone_number));
        Cursor cursor = content_resolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        final int index_displayname = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
//        Log.d(TAG, TAG_getContactName + "index_displayname = " + index_displayname);
//        Log.d(TAG, TAG_getContactName + "cursor = " + cursor);
        if (cursor == null) {
            return phone_number;
        }
        String name = phone_number;
        if (cursor.moveToNext()) {
            name = cursor.getString(index_displayname);
            Log.d(TAG, TAG_getContactName + "name = " + name);
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return name;
    }


//    ----------------------------------------------------------------------------------------------------------------------------------------------


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onClickComposeSms(View view) {
        final String TAG_onClickSendButton = " onClickSendButton(): ";
        Log.d(TAG, TAG_onClickSendButton + " called ");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            getNecessaryPermissions();
        } else {
            Intent intent_to_send_new_sms = new Intent(MainActivity.this, ComposeSmsActivity.class);
            startActivity(intent_to_send_new_sms);
        }
    }


//    ----------------------------------------------------------------------------------------------------------------------------------------------


    //to update the array adapter view so that the index 0 of list view will show the latest sms received
    public void updateInbox(final String sms_message_str, SmsMessage sms_message) {
        final String TAG_updateInbox = " updateInbox(): ";
        Log.d(TAG, TAG_updateInbox + " called ");

        try {
            //always place new sms at top i.e index 0
            sms_adapter.insert(0, sms_message_str);
            sms_adapter.notifyDataSetChanged();
        }
        catch(Exception e){
            Log.d(TAG, TAG_updateInbox + " Exception : " + e );
        }

        //add code to store sms_message inside the sms/inbox and database table
        String sms_body = sms_message.getMessageBody().toString();
        String address = sms_message.getOriginatingAddress().toString();
        long date_sent = sms_message.getTimestampMillis();
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    //just to preserve the current instance so that it is not lost when we return from SmsBroadcastReciever class
    public static MainActivity instance() {
        Log.d(TAG, " instance(): called");
        return inst;
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    public void backToMainActivity() {
        //do nothing
        Log.d(TAG, "backToMainActivity(): called");
    }



//  ----------------------------------------------------------------------------------------------------------------------------------------


//    private static class ReadDbTableAllAsyncTask extends AsyncTask<ArrayList, Void, ReadDbTableAllAsyncTask.ProgressObject>{
private static class ReadDbTableAllAsyncTask extends AsyncTask<ArrayList<String>, Void, ArrayList<String>>{
    private static final String TAG = "[MY_DEBUG]";

        private WeakReference<MainActivity> activityWeakReference;
        private SQLiteDatabase db1;
        private long milli_seconds = 0;
        private Calendar calendar = Calendar.getInstance();
        private DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
        private String printable_date;
        private int i=0;
        private  ArrayList<String> messages_list = new ArrayList();
        private int progress_iterator;
        private  Cursor cursor_read_from_table_all;
        private boolean cursor_first;

    ReadDbTableAllAsyncTask(MainActivity activity, SQLiteDatabase db2){
            activityWeakReference = new WeakReference<MainActivity>(activity);
            progress_iterator = 0;
            db1 = db2;
            i=0;
    }

        @Override
        protected ArrayList<String> doInBackground(ArrayList<String>... arrayList) {
            final String TAG_doInBackground = " ReadDbTableAllAsyncTask doInBackground(): ";
            Log.d(TAG, "doInBackground: called");
            db1.beginTransaction();
            String[] projection = {
                    BaseColumns._ID,
                    SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE
            };

            String selection = null;
            String[] selectionArgs = null;

// How you want the results sorted in the resulting Cursor
            String sortOrder =
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " desc ";   //latest one appears on top of array_adapter


            cursor_read_from_table_all = db1.query(
                    SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                    projection,             // The array of columns to return (pass null to get all)
                    selection,              // The columns for the WHERE clause
                    selectionArgs,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sortOrder               // The sort order
            );

            i = 0;
            cursor_first = cursor_read_from_table_all.moveToFirst();
            if (!cursor_first) {
                Log.d(TAG, "ReadDbTableAllAsyncTask: doInBackground(): TABLE_ALL is empty");
            }
            else {
                Log.d(TAG, "ReadDbTableAllAsyncTask doInBackground: interator i = " + i);
                int index_id = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID);
                int index_corres_id = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID);
                int index_sms_body = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY);
                int index_sms_address = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS);
                int index_sms_epoch_date = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE);
                try {
                    messages_list.clear();
                    do {
                        long itemId = cursor_read_from_table_all.getLong(index_id);
                        String corress_inbox_id = cursor_read_from_table_all.getString(index_corres_id);
                        String sms_body = cursor_read_from_table_all.getString(index_sms_body);
                        String sms_address = cursor_read_from_table_all.getString(index_sms_address);
                        String epoch_date = cursor_read_from_table_all.getString(index_sms_epoch_date);
                        Log.d(TAG, TAG_doInBackground + " itemId = " + itemId);
                        Log.d(TAG, TAG_doInBackground + " corress_inbox_id = " + corress_inbox_id);
                        Log.d(TAG, TAG_doInBackground + " sms_body = " + sms_body);
                        Log.d(TAG, TAG_doInBackground + " sms_address = " + sms_address);
                        milli_seconds = Long.parseLong(epoch_date);
                        calendar.setTimeInMillis(milli_seconds);
                        printable_date = formatter.format(calendar.getTime());
                        Log.d(TAG, TAG_doInBackground + " epoch_date = " + epoch_date + " which is : " + printable_date);
                        Log.d(TAG, TAG_doInBackground + " ");
                        String str = "ItemID = " + itemId + "\ncorress_inbox_id = " + corress_inbox_id + "\n SMS From: " + getContactName(activityWeakReference.get(), sms_address) + "\n Recieved at: " + printable_date + "\n" + sms_body;
                        Log.d(TAG, "doInBackground: progress_iterator = " + progress_iterator);
                        Log.d(TAG, "doInBackground: i = " + i);
                        Log.d(TAG, "doInBackground: adding into message_list at index + " + progress_iterator);
                        Log.d(TAG, "doInBackground: messages_list.add(" + progress_iterator + ", " + str + ")");
                        messages_list.add(progress_iterator++, str);
                        Log.d(TAG, "ReadDbTableAllAsyncTask doInBackground: incrementing iterator i to " + ++i);
                    } while (cursor_read_from_table_all.moveToNext());
                }
                catch(Exception e){
                    Log.d(TAG, "doInBackground: exception : " + e);
                }
            }
            cursor_read_from_table_all.close();
            db1.endTransaction();
            db1.close();
            return messages_list;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: called");

            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            messages_list.clear();
        }

        @Override
        protected void onPostExecute(ArrayList<String> msg_list) {
            super.onPostExecute(msg_list);
            Log.d(TAG, "onPostExecute: called");
            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            Log.d(TAG, "onPostExecute: Printing the whole messages_list : ");
            int j=0;
            try {
                    while (j < msg_list.size()) {
                    Log.d(TAG, "onPostExecute: j=" + j);
                    Log.d(TAG, "onPostExecute(): msg_list.get(" + j + ").toString() = \n" +
                            msg_list.get(j).toString());
                    activity.sms_adapter.insert(j, msg_list.get(j).toString());
                    j++;
                }
                Log.d(TAG, "ReadDbTableAllAsyncTask: onPostExecute(): appending " + msg_list.size() + " items to sms_adapter... ");
            }
            catch (Exception e){
                Log.d(TAG, "onPostExecute: exception : " + e);
            }
            Log.d(TAG, "onPostExecute: Finished reading TABLE_ALL");
        }
    }
}


