package com.example.mynewsmsapp_kotlin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.example.mynewsmsapp_kotlin.GetPersonsHandlerThread.DONE_TASK_GETPERSONS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_SYNCTABLES;

// This activity is used for:
// 1. Showing all messages to user
// 2. A button to take user to ComposeSmsActivity to compose a new sms
public class MainActivity extends AppCompatActivity {
    public static  int count_exec_service = 1;
    private static final String TAG = "[MY_DEBUG] " + MainActivity.class.getSimpleName(); //for debugging
    private static final String KEY_LIST_CONTENTS = "ListContent"; //for SavedInstanceState and RestoreInstanceState which turned out of no use
    public static final int TABLE_ALL = 1;
    public static final int TABLE_HAM = 2;
    public static final int TABLE_SPAM = 3;
    public static final int TABLE_CONTENTSMSINBOX = 4;       //same as TABLE_CONTENT_SMS_INBOX
    public static final int TABLE_CONTENT_SMS_INBOX = 4;     //same as TABLE_CONTENTSMSINBOX

    public static final ArrayList<String> persons_list = new ArrayList<>();
    private GetPersonsHandlerThread getPersonsHandlerThread;

    protected DisplayPersonsRunnable displayPersonsRunnable;
    private TableAllSyncInboxHandlerThread tableAllSyncInboxHandlerThread;
    private Handler main_handler = new Handler();
    public static boolean table_all_sync_inbox = false;   //shows whether our TABLE_ALL is in sync with inbuilt sms/inbox
    public static boolean inbox_sync_tableall = false;   //shows whether our CONTENT_SMS_INBOX is in sync with TABLE_ALL
    private Thread thread;
    ArrayList<String> sms_messages_list = new ArrayList<>();
    RecyclerView messages;
    SmsAdapter sms_adapter;
    // store current instance in inst, will be used in SmsBroadCast receiver to  call
    // MainActivity.updateInbox() with the current instance using function instance() defined at the bottom of MainActivity class
    private static MainActivity inst;
    public static boolean active = false;
    //will be used as requestCode parameter in method requestPermissions(new String[]{Manifest.permission.READ_SMS}, REQUESTCODEFORPERMISSIONS_READSMS_ENDOFPERMISSIONS);
    private static final int REQUESTCODEFORPERMISSIONS_READSMS_READCONTACTS_ENDOFPERMISSIONS = 27015; //only for READSMS permission
    private ToggleButton toggleButton_tableall;
    private ToggleButton toggleButton_tableham;
    private ToggleButton toggleButton_tablespam;
    protected static ArrayList<String> messages_list_tableall = new ArrayList();
    public SpamBusterdbHelper spamBusterdbHelper;
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
        spamBusterdbHelper.close();
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        final String TAG_onSaveInstanceState = " onSaveInstanceState(): ";
        Log.d(TAG, TAG_onSaveInstanceState + "called");
        super.onSaveInstanceState(outState);
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        final String TAG_onRestoreInstanceState = " onRestoreInstanceState(): ";
        Log.d(TAG, TAG_onRestoreInstanceState + "called");
        super.onRestoreInstanceState(savedInstanceState);
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
        toggleButton_tableall = (ToggleButton) findViewById(R.id.all_sms_toggle);
        toggleButton_tableham = (ToggleButton) findViewById(R.id.ham_sms_toggle);
        toggleButton_tablespam = (ToggleButton) findViewById(R.id.spam_sms_toggle);
        //enable toggle tableham on creation of activity
        toggleButton_tableham.setChecked(true);
        spamBusterdbHelper = new SpamBusterdbHelper(this);

        //start background service
//        startService(new Intent(getApplicationContext(),ClassificationSyncService.class));

//        ----------------------- DELETE DATABASE --------------------
        //to delete the database. so that everytime a new database is created
//        try {
//            this.deleteDatabase(SpamBusterdbHelper.DATABASE_NAME);
//        }
//        catch (Exception e){
//            Log.d(TAG, TAG_onCreate + " Exception : " + e);
//        }
//        -----------------

        //create TABLE_PENDING start
//        SQLiteDatabase tempdb = spamBusterdbHelper.getWritableDatabase();
//        tempdb.execSQL(SpamBusterdbHelper.SQL_CREATE_TABLEPENDING);
        //create TABLE_PENDING end
        //alter TABLE_ALL to add column_spam START
//        SQLiteDatabase tempdb = spamBusterdbHelper.getWritableDatabase();
//        tempdb.execSQL(SpamBusterdbHelper.SQL_ALTER_TABLEALL_ADDCOLUMNSPAM);
        //alter TABLE_ALL to add column_spam END

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

        //DEBUG code start
        Log.d(TAG, "MainActivity: refreshSmsInbox(): Debug Probing prediction API");
//        String[] id_str = new String[5];
//        id_str[0] = "1000";
//        id_str[1] = "1001";
//        id_str[2] = "1002";
//        id_str[3] = "1003";
//        id_str[4] = "1004";
//        String address = "9999988888";
//        String[] message_body = new String[5];
//        message_body[0] = "Hi, I am in a meeting. Will call back later.";
//        message_body[1] = "IMPORTANT - You could be entitled up to £3,160 in compensation from mis-sold PPI on a credit card or loan. Please reply PPI for info or STOP to opt out.";
//        message_body[2] = "A [redacted] loan for £950 is approved for you if you receive this SMS. 1 min verification & cash in 1 hr at www.[redacted].co.uk to opt out reply stop";
//        message_body[3] = "You have still not claimed the compensation you are due for the accident you had. To start the process please reply YES. To opt out text STOP";
//        message_body[4] = "Our records indicate your Pension is under performing to see higher growth and up to 25% cash release reply PENSION for a free review. To opt out reply STOP";
//        Thread predictspam = new Thread(new PredictionProbingRunnable(this, id_str, address, message_body));
//        predictspam.start();
        //DEBUG code end

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
        Handler handler;
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
        String date_sent = Long.toString(sms_message.getTimestampMillis());
        String date = Long.toString(System.currentTimeMillis());
        Log.d(TAG, "MainActivity: updateInbox(): sms_body: " + sms_body);
        Log.d(TAG, "MainActivity: updateInbox(): address: " + address);
        Log.d(TAG, "MainActivity: updateInbox(): date_sent: " + date_sent);
        Log.d(TAG, "MainActivity: updateInbox(): date (received): " + date);
        NewSmsMessageRunnable newSmsMessageRunnable = new NewSmsMessageRunnable(this, spamBusterdbHelper);
        newSmsMessageRunnable.sms_body = sms_body;
        newSmsMessageRunnable.address = address;
        newSmsMessageRunnable.date_sent = date_sent;
        newSmsMessageRunnable.date = date;
        newSmsMessageRunnable.message_is_spam = true;//very important field. In future this will be changed after returning result from server
        new Thread(newSmsMessageRunnable).start();
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    //just to preserve the current instance so that it is not lost when we return from SmsBroadcastReciever class
    public static MainActivity instance() {
        Log.d(TAG, " instance(): called");
        return inst;
    }


    //    ----------------------------------------------------------------------------------------------------------------------------------------------


    private static class DbOperationsRunnable implements Runnable {
        private SpamBusterdbHelper db_helper;
        private SpamBusterdbHelper db_helper1;
        private SQLiteDatabase db;
        private WeakReference<MainActivity> activityWeakReference;
        private Handler handler;
        private Handler handler1;
        private TableAllSyncInboxHandlerThread tableAllSyncInboxHandlerThread;
        private GetPersonsHandlerThread getPersonsHandlerThread;

        DbOperationsRunnable(MainActivity activity) {
            activityWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void run() {
            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

//            this.db_helper = new SpamBusterdbHelper(activity);
            this.db_helper1 = activity.spamBusterdbHelper;
            //get persons list and put in persons_list
            activity.getPersonsHandlerThread = new GetPersonsHandlerThread(db_helper1);
            this.getPersonsHandlerThread = activity.getPersonsHandlerThread;
            this.getPersonsHandlerThread.start();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.handler1 = getPersonsHandlerThread.getHandler();
            Message msg_getpersons = Message.obtain(handler1);
            msg_getpersons.what = GetPersonsHandlerThread.TASK_GET_PERSONS;
            int table1 = TABLE_ALL;
            msg_getpersons.arg1 = table1;
            Log.d(TAG, "DbOperationsRunnable: run(): preparing message msg_getperson with following attributes:");
            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.what = " + msg_getpersons.what);
            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.arg1 = " + msg_getpersons.arg1);
            Log.d(TAG, "DbOperationsRunnable: run(): sending message...");
            msg_getpersons.sendToTarget();

            while(true) {
                if(DONE_TASK_GETPERSONS) {
                    DONE_TASK_GETPERSONS = false;
                    activity.displayPersonsRunnable = new DisplayPersonsRunnable(activity, db);
                    activity.thread = new Thread(activity.displayPersonsRunnable);
                    activity.thread.start();
                    break;
                }
            }


            //carry out sync of tables everytime the app starts
//            this.db_helper = activity.spamBusterdbHelper;
            activity.tableAllSyncInboxHandlerThread = new TableAllSyncInboxHandlerThread(db_helper1);
            this.tableAllSyncInboxHandlerThread = activity.tableAllSyncInboxHandlerThread;
            this.tableAllSyncInboxHandlerThread.start();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.handler = tableAllSyncInboxHandlerThread.getHandler();
            Message msg_synctables = Message.obtain(handler);
            Log.d(TAG, "DbOperationsRunnable: run(): msg_synctables initialized");
            msg_synctables.what = TASK_SYNCTABLES;
            Log.d(TAG, "DbOperationsRunnable: run(): setting msg_synctables.what = " + msg_synctables);
            Log.d(TAG, "DbOperationsRunnable: run(): msg_synctables preparation complete");
            Log.d(TAG, "DbOperationsRunnable: run(): msg_synctables sent!");
            msg_synctables.sendToTarget();
        }
    }

    private static class DisplayPersonsRunnable implements Runnable{
        private Handler handler;
        private WeakReference<MainActivity> activityWeakReference;
        private SQLiteDatabase db1;

        DisplayPersonsRunnable(MainActivity activity, SQLiteDatabase db) {
            handler = activity.main_handler;
            activityWeakReference = new WeakReference<MainActivity>(activity);
            this.db1 = db;
        }

        @Override
        public void run() {
            final MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
                try {
                    //pass the Runnable to MainThread handler because UI elements(sms_adapter) are on MainThread
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            for(int i=0; i<persons_list.size(); i++) {
                                Log.d(TAG, "DisplayPersonsRunnable: run(): inserting " + getContactName(activity, persons_list.get(i)) + " in sms_adapter");
//                                activity.sms_adapter.insert(i, getContactName(activity, persons_list.get(i)));
                                activity.sms_adapter.insertPerson(i,  persons_list.get(i));
                            }
                        }
                    });
                }
                catch (Exception e) {
                    Log.d(TAG, "run(): exception : " + e);
                }
        }
    }

} //MainActivity class ends


