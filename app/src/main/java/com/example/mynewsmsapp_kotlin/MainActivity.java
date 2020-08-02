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
import android.os.Message;
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

import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_GET_IDS_SMSINBOX;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_GET_IDS_TABLEALL;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_GET_MISSING_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_UPDATE_MISSING_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DUMMY_VAL;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_GET_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_GET_MISSING_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_UPDATE_MISSING_IDS;

// This activity is used for:
// 1. Showing all messages to user
// 2. A button to take user to ComposeSmsActivity to compose a new sms
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "[MY_DEBUG] " + MainActivity.class.getSimpleName(); //for debugging
    private static final String KEY_LIST_CONTENTS = "ListContent"; //for SavedInstanceState and RestoreInstanceState which turned out of no use
    public static final int TABLE_ALL = 1;
    public static final int TABLE_INBOX = 2;
    public static final int TABLE_SPAM = 3;
    public static final int TABLE_CONTENT_SMS_INBOX = 4;

    //    protected ReadDbTableAllAsyncTask readDbTableAllAsyncTask;
    protected ReadDbTableAllRunnable readDbTableAllRunnable;
    //    private DbOperationsAsyncTask dbOperationsAsyncTask;
    private TableAllSyncInboxHandlerThread tableAllSyncInboxHandlerThread;
    private Handler main_handler;
    public static boolean table_all_sync_inbox = false;   //shows whether our TABLE_ALL is in sync with inbuilt sms/inbox
    private Thread thread;

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
        try {
            tableAllSyncInboxHandlerThread.quit();
        } catch (Exception e) {
            Log.d(TAG, "MainActivity: onDestroy(): exception " + e);
        }
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

        //inserting a dummy item at index 0 of list   (list will be cleared once SmsAdapter object is created)
        sms_messages_list.add(0, "dummy");
        sms_adapter = new SmsAdapter(this, sms_messages_list);
        messages.setAdapter(sms_adapter);
        messages.setLayoutManager(new LinearLayoutManager(this));

        //running on seperate thread
        DbOperationsRunnable dbOperationsRunnable = new DbOperationsRunnable(this);
        new Thread(dbOperationsRunnable).start();

        // pushing db operations to asynctask
//        dbOperationsAsyncTask = new DbOperationsAsyncTask(this);
//        dbOperationsAsyncTask.execute();
    }

    //    -------------------------------------------------------------------------------------------------------------------------------------------------

    public static String getContactName(Context context, String phone_number) {
        final String TAG_getContactName = " getContactName(): ";
        Log.d(TAG, TAG_getContactName + " called ");
        ContentResolver content_resolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone_number));
        Cursor cursor = content_resolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        final int index_displayname = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
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
        } catch (Exception e) {
            Log.d(TAG, TAG_updateInbox + " Exception : " + e);
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


    private class DbOperationsRunnable implements Runnable {
        private SpamBusterdbHelper db_helper;
        private SQLiteDatabase db;
        private WeakReference<MainActivity> activityWeakReference;
        private Handler handler;
        private TableAllSyncInboxHandlerThread tableAllSyncInboxHandlerThread;

        DbOperationsRunnable(MainActivity activity) {
            activityWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void run() {
            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            this.db_helper = new SpamBusterdbHelper(activity);
            activity.tableAllSyncInboxHandlerThread = new TableAllSyncInboxHandlerThread(db_helper);
            this.tableAllSyncInboxHandlerThread = activity.tableAllSyncInboxHandlerThread;
            this.tableAllSyncInboxHandlerThread.start();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.handler = tableAllSyncInboxHandlerThread.getHandler();
            Message msg = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg initialized");
            //get all ids from TABLE_ALL
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg.what = TASK_GET_IDS");
            msg.what = TASK_GET_IDS;
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg.arg1 = TABLE_ALL");
            msg.arg1 = TABLE_ALL;
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg.arg2 = DUMMY_VAL");
            msg.arg2 = DUMMY_VAL;
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg preparation complete");
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg sent!");
            msg.sendToTarget();

            Message msg1 = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg1 initialized");
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): loop until DONE_TASK_GET_IDS_TABLEALL is true");
            while (true) {
                //if all ids are read from TABLE_ALL then move ahead
                if (DONE_TASK_GET_IDS_TABLEALL) {
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): checking DONE_TASK_GET_IDS_TABLEALL... " + DONE_TASK_GET_IDS_TABLEALL);
                    //get all ids from SMS/INBOX
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg1.what = TASK_GET_IDS");
                    msg1.what = TASK_GET_IDS;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg1.arg1 = TABLE_CONTENT_SMS_INBOX");
                    msg1.arg1 = TABLE_CONTENT_SMS_INBOX;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg1.arg2 = DUMMY_VAL");
                    msg1.arg2 = DUMMY_VAL;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg1 preparation complete");
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg1 sent!");
                    msg1.sendToTarget();
                    break;
                }
            }
            //reset  to false for next time
            DONE_TASK_GET_IDS_TABLEALL = false;
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): reset  DONE_TASK_GET_IDS_TABLEALL to " + DONE_TASK_GET_IDS_TABLEALL);

            Message msg2 = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg2 initialized");
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): loop until DONE_TASK_GET_IDS_SMSINBOX is true");
            while (true) {
                if (DONE_TASK_GET_IDS_SMSINBOX) {
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): checking DONE_TASK_GET_IDS_SMSINBOX...  " + DONE_TASK_GET_IDS_SMSINBOX);
                    //compare ids and get missing IDs
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg2.what = TASK_GET_MISSING_IDS");
                    msg2.what = TASK_GET_MISSING_IDS;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg2.arg1 = TABLE_ALL");
                    msg2.arg1 = TABLE_ALL;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg2.arg2 = TABLE_CONTENT_SMS_INBOX");
                    msg2.arg2 = TABLE_CONTENT_SMS_INBOX;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg2 preparation complete");
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg2 sent!");
                    msg2.sendToTarget();
                    break;
                }
            }
            //reset  to false for next time
            DONE_TASK_GET_IDS_SMSINBOX = false;
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): reset DONE_TASK_GET_IDS_SMSINBOX to " + DONE_TASK_GET_IDS_SMSINBOX);

            Message msg3 = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg3 initialized");
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): loop until DONE_TASK_GET_MISSING_IDS is true");
            while (true) {
                if (DONE_TASK_GET_MISSING_IDS) {
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): checking DONE_TASK_GET_MISSING_IDS ... " + DONE_TASK_UPDATE_MISSING_IDS);
                    //update the missing messages in TABLE_ALL
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg3.what = TASK_UPDATE_MISSING_IDS");
                    msg3.what = TASK_UPDATE_MISSING_IDS;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg3.arg1 = TABLE_ALL");
                    msg3.arg1 = TABLE_ALL;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): setting msg3.arg2 = TABLE_CONTENT_SMS_INBOX");
                    msg3.arg2 = TABLE_CONTENT_SMS_INBOX;
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg3 preparation complete");
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): msg3 sent!");
                    msg3.sendToTarget();
                    break;
                }
            }
            DONE_TASK_GET_MISSING_IDS = false;
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): reset DONE_TASK_GET_MISSING_IDS to " + DONE_TASK_GET_MISSING_IDS);
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): loop until DONE_TASK_UPDATE_MISSING_IDS is true");
            while (true) {
                //only if all TASKs are done and finally missing messages are updated, then move ahead to show the messages
                if (DONE_TASK_UPDATE_MISSING_IDS) {
                    Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): checking DONE_TASK_UPDATE_MISSING_IDS ... " + DONE_TASK_UPDATE_MISSING_IDS);
                    break;
                }
            }
            DONE_TASK_UPDATE_MISSING_IDS = false;
            Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): reset DONE_TASK_UPDATE_MISSING_IDS to " + DONE_TASK_UPDATE_MISSING_IDS);
            try {
                tableAllSyncInboxHandlerThread.quit();
            } catch (Exception e) {
                Log.d(TAG, "DbOperationsAsyncTask: doInBackground(): exception " + e);
            }
            //READING from database  table TABLE_ALL
            Log.d(TAG, "DbOperationsAsyncTask: onPostExecute(): Now reading values from TABLE_ALL ");
            db = db_helper.getReadableDatabase();
            int table = TABLE_ALL;
            switch (table) {
                case TABLE_ALL:
                    Log.d(TAG, "DbOperationsAsyncTask: onPostExecute(): inside case TABLE_ALL");
//                    activity.readDbTableAllAsyncTask = new ReadDbTableAllAsyncTask(activity, db);
                    activity.readDbTableAllRunnable = new ReadDbTableAllRunnable(activity, activity.main_handler, db);

                    ArrayList msg1_list = new ArrayList();  //dummy
                    Log.d(TAG, "DbOperationsAsyncTask: onPostExecute(): executing readDb thread in background");
//                    activity.readDbTableAllAsyncTask.execute(msg1_list);  //msg1_list is never going to be used
                    activity.thread = new Thread(readDbTableAllRunnable);
                    activity.thread.start();
                    break;
            }
        }
    }


//--------------------------------------------------------------------------------------------------------------------------------------------------

    private static class ReadDbTableAllRunnable implements Runnable {
        private Handler handler;
        private WeakReference<MainActivity> activityWeakReference;
        private SQLiteDatabase db1;
        private int i;
        private int progress_iterator;
        private long milli_seconds = 0;
        private Calendar calendar = Calendar.getInstance();
        private DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
        private String printable_date;
        private ArrayList<String> messages_list = new ArrayList();
        private Cursor cursor_read_from_table_all;
        private boolean cursor_first;
        private int j = 0;

        ReadDbTableAllRunnable(MainActivity activity, Handler handler_main, SQLiteDatabase db) {
            this.handler = handler_main;
            activityWeakReference = new WeakReference<MainActivity>(activity);
            this.db1 = db;
            progress_iterator = 0;
            i = 0;
        }

        @Override
        public void run() {
            final MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            final String TAG_doInBackground = " ReadDbTableAllAsyncTask doInBackground(): ";
            Log.d(TAG, TAG_doInBackground + " called");
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
            } else {
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
                        String str = "ItemID = " + itemId + "\ncorress_inbox_id = " + corress_inbox_id + "\n SMS From: " + getContactName(activity, sms_address) + "\n Recieved at: " + printable_date + "\n" + sms_body;
                        Log.d(TAG, "doInBackground: progress_iterator = " + progress_iterator);
                        Log.d(TAG, "doInBackground: i = " + i);
                        Log.d(TAG, "doInBackground: adding into message_list at index + " + progress_iterator);
                        Log.d(TAG, "doInBackground: messages_list.add(" + progress_iterator + ", " + str + ")");
                        messages_list.add(progress_iterator++, str);
                        Log.d(TAG, "ReadDbTableAllAsyncTask doInBackground: incrementing iterator i to " + ++i);
                    } while (cursor_read_from_table_all.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "doInBackground: exception : " + e);
                }
            }
            cursor_read_from_table_all.close();
//            db1.endTransaction();
            db1.close();
            Log.d(TAG, "onPostExecute: Printing the whole messages_list : ");
            try {
//                j = 0;
//                while (j < messages_list.size()) {
//                    Log.d(TAG, "onPostExecute: j=" + j);
//                    Log.d(TAG, "onPostExecute(): msg_list.get(" + j + ").toString() = \n" +
//                            messages_list.get(j).toString());
//                    activity.sms_adapter.insert(j, messages_list.get(j).toString());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            j = 0;
                            while (j < messages_list.size()) {
                                Log.d(TAG, "onPostExecute: j=" + j);
                                Log.d(TAG, "onPostExecute(): msg_list.get(" + j + ").toString() = \n" +
                                        messages_list.get(j).toString());
                            activity.sms_adapter.insert(j, messages_list.get(j).toString());
                                j++;
                            }
                        }
                    });
                Log.d(TAG, "ReadDbTableAllAsyncTask: onPostExecute(): appending " + messages_list.size() + " items to sms_adapter... ");
            } catch (Exception e) {
                Log.d(TAG, "onPostExecute: exception : " + e);
            }
            Log.d(TAG, "onPostExecute: Finished reading TABLE_ALL");
        }
    }
} //MainActivity


//    ------------------------------------------------------------------------------------------------------------------------------------------


//    private static class ReadDbTableAllAsyncTask extends AsyncTask<ArrayList, Void, ReadDbTableAllAsyncTask.ProgressObject>{
//private static class ReadDbTableAllAsyncTask extends AsyncTask<ArrayList<String>, Void, ArrayList<String>>{
//    private static final String TAG = "[MY_DEBUG]";
//
//        private WeakReference<MainActivity> activityWeakReference;
//        private SQLiteDatabase db1;
//        private long milli_seconds = 0;
//        private Calendar calendar = Calendar.getInstance();
//        private DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
//        private String printable_date;
//        private int i=0;
//        private  ArrayList<String> messages_list = new ArrayList();
//        private int progress_iterator;
//        private  Cursor cursor_read_from_table_all;
//        private boolean cursor_first;
//
//    ReadDbTableAllAsyncTask(MainActivity activity, SQLiteDatabase db2){
//            activityWeakReference = new WeakReference<MainActivity>(activity);
//            progress_iterator = 0;
//            db1 = db2;
//            i=0;
//    }
//
//        @Override
//        protected ArrayList<String> doInBackground(ArrayList<String>... arrayList) {
//            final String TAG_doInBackground = " ReadDbTableAllAsyncTask doInBackground(): ";
//            Log.d(TAG, TAG_doInBackground + " called");
////            db1.beginTransaction();
//            String[] projection = {
//                    BaseColumns._ID,
//                    SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
//                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY,
//                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS,
//                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE
//            };
//            String selection = null;
//            String[] selectionArgs = null;
//// How you want the results sorted in the resulting Cursor
//            String sortOrder =
//                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " desc ";   //latest one appears on top of array_adapter
//            cursor_read_from_table_all = db1.query(
//                    SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
//                    projection,             // The array of columns to return (pass null to get all)
//                    selection,              // The columns for the WHERE clause
//                    selectionArgs,          // The values for the WHERE clause
//                    null,                   // don't group the rows
//                    null,                   // don't filter by row groups
//                    sortOrder               // The sort order
//            );
//            i = 0;
//            cursor_first = cursor_read_from_table_all.moveToFirst();
//            if (!cursor_first) {
//                Log.d(TAG, "ReadDbTableAllAsyncTask: doInBackground(): TABLE_ALL is empty");
//            }
//            else {
//                Log.d(TAG, "ReadDbTableAllAsyncTask doInBackground: interator i = " + i);
//                int index_id = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID);
//                int index_corres_id = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID);
//                int index_sms_body = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY);
//                int index_sms_address = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS);
//                int index_sms_epoch_date = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE);
//                try {
//                    messages_list.clear();
//                    do {
//                        long itemId = cursor_read_from_table_all.getLong(index_id);
//                        String corress_inbox_id = cursor_read_from_table_all.getString(index_corres_id);
//                        String sms_body = cursor_read_from_table_all.getString(index_sms_body);
//                        String sms_address = cursor_read_from_table_all.getString(index_sms_address);
//                        String epoch_date = cursor_read_from_table_all.getString(index_sms_epoch_date);
//                        Log.d(TAG, TAG_doInBackground + " itemId = " + itemId);
//                        Log.d(TAG, TAG_doInBackground + " corress_inbox_id = " + corress_inbox_id);
//                        Log.d(TAG, TAG_doInBackground + " sms_body = " + sms_body);
//                        Log.d(TAG, TAG_doInBackground + " sms_address = " + sms_address);
//                        milli_seconds = Long.parseLong(epoch_date);
//                        calendar.setTimeInMillis(milli_seconds);
//                        printable_date = formatter.format(calendar.getTime());
//                        Log.d(TAG, TAG_doInBackground + " epoch_date = " + epoch_date + " which is : " + printable_date);
//                        Log.d(TAG, TAG_doInBackground + " ");
//                        String str = "ItemID = " + itemId + "\ncorress_inbox_id = " + corress_inbox_id + "\n SMS From: " + getContactName(activityWeakReference.get(), sms_address) + "\n Recieved at: " + printable_date + "\n" + sms_body;
//                        Log.d(TAG, "doInBackground: progress_iterator = " + progress_iterator);
//                        Log.d(TAG, "doInBackground: i = " + i);
//                        Log.d(TAG, "doInBackground: adding into message_list at index + " + progress_iterator);
//                        Log.d(TAG, "doInBackground: messages_list.add(" + progress_iterator + ", " + str + ")");
//                        messages_list.add(progress_iterator++, str);
//                        Log.d(TAG, "ReadDbTableAllAsyncTask doInBackground: incrementing iterator i to " + ++i);
//                    } while (cursor_read_from_table_all.moveToNext());
//                }
//                catch(Exception e){
//                    Log.d(TAG, "doInBackground: exception : " + e);
//                }
//            }
//            cursor_read_from_table_all.close();
////            db1.endTransaction();
//            db1.close();
//            return messages_list;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            Log.d(TAG, "ReadDbTableAllAsyncTask: onPreExecute(): called");
//            MainActivity activity = activityWeakReference.get();
//            if (activity == null || activity.isFinishing()) {
//                return;
//            }
//            messages_list.clear();
//        }
//
//        @Override
//        protected void onPostExecute(ArrayList<String> msg_list) {
//            super.onPostExecute(msg_list);
//            Log.d(TAG, "onPostExecute: called");
//            MainActivity activity = activityWeakReference.get();
//            if (activity == null || activity.isFinishing()) {
//                return;
//            }
//            Log.d(TAG, "onPostExecute: Printing the whole messages_list : ");
//            int j=0;
//            try {
//                    while (j < msg_list.size()) {
//                    Log.d(TAG, "onPostExecute: j=" + j);
//                    Log.d(TAG, "onPostExecute(): msg_list.get(" + j + ").toString() = \n" +
//                            msg_list.get(j).toString());
//                    activity.sms_adapter.insert(j, msg_list.get(j).toString());
//                    j++;
//                }
//                Log.d(TAG, "ReadDbTableAllAsyncTask: onPostExecute(): appending " + msg_list.size() + " items to sms_adapter... ");
//            }
//            catch (Exception e){
//                Log.d(TAG, "onPostExecute: exception : " + e);
//            }
//            Log.d(TAG, "onPostExecute: Finished reading TABLE_ALL");
//        }
//    }
//}
//
//
