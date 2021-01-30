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
import android.provider.BaseColumns;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static com.example.mynewsmsapp_kotlin.GetPersonsHandlerThread.DONE_TASK_GETPERSONS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_COMPARETOPID;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_GET_IDS_SMSINBOX;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_GET_IDS_TABLEALL;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_GET_MISSING_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_GET_MISSING_IDS_IN_TABLEALL;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_UPDATE_MISSING_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DUMMY_VAL;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_COMPARE_TOP_ID;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_GET_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_GET_MISSING_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_NEWSMSREC;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_UPDATE_MISSING_IDS;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.TASK_NEWSMSREC;

// This activity is used for:
// 1. Showing all messages to user
// 2. A button to take user to ComposeSmsActivity to compose a new sms
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "[MY_DEBUG] " + MainActivity.class.getSimpleName(); //for debugging
    private static final String KEY_LIST_CONTENTS = "ListContent"; //for SavedInstanceState and RestoreInstanceState which turned out of no use
    public static final int TABLE_ALL = 1;
    public static final int TABLE_HAM = 2;
    public static final int TABLE_SPAM = 3;
    public static final int TABLE_CONTENTSMSINBOX = 4;       //same as TABLE_CONTENT_SMS_INBOX
    public static final int TABLE_CONTENT_SMS_INBOX = 4;     //same as TABLE_CONTENTSMSINBOX

    public static final ArrayList<String> persons_list = new ArrayList<>();
    private GetPersonsHandlerThread getPersonsHandlerThread;

    protected ReadDbTableAllRunnable readDbTableAllRunnable;
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
    public static ArrayList<String> messages_list_tableall = new ArrayList();
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
//        ----------------------- DELETE DATABASE --------------------
        //to delete the database. so that everytime a new database is created
//        try {
//            this.deleteDatabase(SpamBusterdbHelper.DATABASE_NAME);
//        }
//        catch (Exception e){
//            Log.d(TAG, TAG_onCreate + " Exception : " + e);
//        }
//        -----------------
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
        newSmsMessageRunnable.spam = true;//very important field. In future this will be changed after returning result from server
        new Thread(newSmsMessageRunnable).start();
//        handler = tableAllSyncInboxHandlerThread.getHandler();
//        Message msg_newsmsrec = Message.obtain(handler);
//        msg_newsmsrec.what = TASK_NEWSMSREC;
//        msg_newsmsrec.sendToTarget();
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
                    activity.displayPersonsRunnable = new DisplayPersonsRunnable(activity, db);
                    activity.thread = new Thread(activity.displayPersonsRunnable);
                    activity.thread.start();
                    break;
                }
            }
            DONE_TASK_GETPERSONS = false;
/*
//            this.db_helper = new SpamBusterdbHelper(activity);
            this.db_helper = activity.spamBusterdbHelper;
            activity.tableAllSyncInboxHandlerThread = new TableAllSyncInboxHandlerThread(db_helper);
            this.tableAllSyncInboxHandlerThread = activity.tableAllSyncInboxHandlerThread;
            this.tableAllSyncInboxHandlerThread.start();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.handler = tableAllSyncInboxHandlerThread.getHandler();
            Message msg_comparetopids = Message.obtain(handler);
            Log.d(TAG, "DbOperationsRunnable: run(): msg_comparetopids initialized");
            //compare only top IDs of TABLE_ALL[corres_inbox_id] and SMS/INBOX[_id]
            msg_comparetopids.what = TASK_COMPARE_TOP_ID;
            Log.d(TAG, "DbOperationsRunnable: run(): setting msg_comparetopids.what = TASK_COMPARE_TOPIDS");
            msg_comparetopids.arg1 = TABLE_ALL;
            Log.d(TAG, "DbOperationsRunnable: run(): setting msg_compatetopids.arg1 = TABLE_ALL");
            msg_comparetopids.arg2 = TABLE_CONTENTSMSINBOX;
            Log.d(TAG, "DbOperationsRunnable: run(): setting msg_comparetopids.arg2 = TABLE_CONTENTSMSINBOX");
            Log.d(TAG, "DbOperationsRunnable: run(): msg_comparetopids preparation complete");
            Log.d(TAG, "DbOperationsRunnable: run(): msg_comparetopids sent!");
            msg_comparetopids.sendToTarget();

            while(true) {
                if (DONE_TASK_COMPARETOPID) {
                    //if top IDs don't match
                    if (!table_all_sync_inbox) {
                        Message msg = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
                        Log.d(TAG, "DbOperationsRunnable: run(): msg initialized");
                        //get all ids from TABLE_ALL
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg.what = TASK_GET_IDS");
                        msg.what = TASK_GET_IDS;
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg.arg1 = TABLE_ALL");
                        msg.arg1 = TABLE_ALL;
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg.arg2 = DUMMY_VAL");
                        msg.arg2 = DUMMY_VAL;
                        Log.d(TAG, "DbOperationsRunnable: run(): msg preparation complete");
                        Log.d(TAG, "DbOperationsRunnable: run(): msg sent!");
                        msg.sendToTarget();

                        Message msg1 = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
                        //get all ids from SMS/INBOX
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg1.what = TASK_GET_IDS");
                        msg1.what = TASK_GET_IDS;
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg1.arg1 = TABLE_CONTENT_SMS_INBOX");
                        msg1.arg1 = TABLE_CONTENT_SMS_INBOX;
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg1.arg2 = DUMMY_VAL");
                        msg1.arg2 = DUMMY_VAL;
                        Log.d(TAG, "DbOperationsRunnable: run(): msg1 preparation complete");
                        Log.d(TAG, "DbOperationsRunnable: run(): msg1 initialized");
                        Log.d(TAG, "DbOperationsRunnable: run(): loop until DONE_TASK_GET_IDS_TABLEALL is true");
                        while (true) {
                            //if all ids are read from TABLE_ALL then move ahead
                            if (DONE_TASK_GET_IDS_TABLEALL) {
                                Log.d(TAG, "DbOperationsRunnable: run(): checking DONE_TASK_GET_IDS_TABLEALL... " + DONE_TASK_GET_IDS_TABLEALL);
                                Log.d(TAG, "DbOperationsRunnable: run(): msg1 sent!");
                                msg1.sendToTarget();
                                break;
                            }
                        }
                        //reset  to false for next time
                        DONE_TASK_GET_IDS_TABLEALL = false;
                        Log.d(TAG, "DbOperationsRunnable: run(): reset  DONE_TASK_GET_IDS_TABLEALL to " + DONE_TASK_GET_IDS_TABLEALL);

                        Message msg21 = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
                        //compare ids and get missing IDs
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg21.what = TASK_GET_MISSING_IDS");
                        msg21.what = TASK_GET_MISSING_IDS;
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg21.arg1 = TABLE_ALL");
                        msg21.arg1 = TABLE_ALL;
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg21.arg2 = TABLE_CONTENT_SMS_INBOX");
                        msg21.arg2 = TABLE_CONTENT_SMS_INBOX;
                        Log.d(TAG, "DbOperationsRunnable: run(): msg21 preparation complete");
                        Log.d(TAG, "DbOperationsRunnable: run(): msg21 initialized");
                        Log.d(TAG, "DbOperationsRunnable: run(): loop until DONE_TASK_GET_IDS_SMSINBOX is true");
                        while (true) {
                            if (DONE_TASK_GET_IDS_SMSINBOX) {
                                Log.d(TAG, "DbOperationsRunnable: run(): checking DONE_TASK_GET_IDS_SMSINBOX...  " + DONE_TASK_GET_IDS_SMSINBOX);
                                Log.d(TAG, "DbOperationsRunnable: run(): msg21 sent!");
                                msg21.sendToTarget();
                                break;
                            }
                        }
                        //reset  to false for next time
                        DONE_TASK_GET_IDS_SMSINBOX = false;
                        Log.d(TAG, "DbOperationsRunnable: run(): reset DONE_TASK_GET_IDS_SMSINBOX to " + DONE_TASK_GET_IDS_SMSINBOX);

                        Message msg22 = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
                        //compare ids and get missing IDs
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg22.what = TASK_GET_MISSING_IDS");
                        msg22.what = TASK_GET_MISSING_IDS;
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg22.arg1 = TABLE_CONTENT_SMS_INBOX");
                        msg22.arg1 = TABLE_CONTENT_SMS_INBOX;
                        Log.d(TAG, "DbOperationsRunnable: run(): setting msg22.arg2 = TABLE_ALL");
                        msg22.arg2 = TABLE_ALL;
                        Log.d(TAG, "DbOperationsRunnable: run(): msg22 preparation complete");
                        Log.d(TAG, "DbOperationsRunnable: run(): msg22 initialized");
                        Log.d(TAG, "DbOperationsRunnable: run(): loop until DONE_TASK_GET_MISSING_IDS_IN_TABLEALL is true");
                        while (true) {
                            if (DONE_TASK_GET_MISSING_IDS_IN_TABLEALL) {
                                Log.d(TAG, "DbOperationsRunnable: run(): checking DONE_TASK_GET_MISSING_IDS_IN_TABLEALL...  " + DONE_TASK_GET_MISSING_IDS_IN_TABLEALL);
                                Log.d(TAG, "DbOperationsRunnable: run(): msg22 sent!");
                                msg22.sendToTarget();
                                break;
                            }
                        }
                        //reset  to false for next time
                        DONE_TASK_GET_MISSING_IDS_IN_TABLEALL = false;
                        Log.d(TAG, "DbOperationsRunnable: run(): reset DONE_TASK_GET_MISSING_IDS_IN_TABLEALL to " + DONE_TASK_GET_MISSING_IDS_IN_TABLEALL);

                        Message msg3 = Message.obtain(handler);  //thus the target handler for this message is handler which is the handler of tableAllSyncInboxHandlerThread
                        Log.d(TAG, "DbOperationsRunnable: run(): msg3 initialized");
                        Log.d(TAG, "DbOperationsRunnable: run(): loop until DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX is true");
                        while (true) {
//                            if (DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX || DONE_TASK_GET_MISSING_IDS) {
                            if (DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX) {
                                Log.d(TAG, "DbOperationsRunnable: run(): checking DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX ... " + DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX);
                                //update the missing messages in TABLE_ALL
                                Log.d(TAG, "DbOperationsRunnable: run(): setting msg3.what = TASK_UPDATE_MISSING_IDS");
                                msg3.what = TASK_UPDATE_MISSING_IDS;
                                Log.d(TAG, "DbOperationsRunnable: run(): setting msg3.arg1 = TABLE_ALL");
                                msg3.arg1 = TABLE_ALL;
                                Log.d(TAG, "DbOperationsRunnable: run(): setting msg3.arg2 = TABLE_CONTENT_SMS_INBOX");
                                msg3.arg2 = TABLE_CONTENT_SMS_INBOX;
                                Log.d(TAG, "DbOperationsRunnable: run(): msg3 preparation complete");
                                Log.d(TAG, "DbOperationsRunnable: run(): msg3 sent!");
                                msg3.sendToTarget();
                                break;
                            }
                        }
//                        DONE_TASK_GET_MISSING_IDS = false;
                        DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX = false;
                        Log.d(TAG, "DbOperationsRunnable: run(): reset DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX to " + DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX);
                        Log.d(TAG, "DbOperationsRunnable: run(): loop until DONE_TASK_UPDATE_MISSING_IDS is true");
                        while (true) {
                            //only if all TASKs are done and finally missing messages are updated, then move ahead to show the messages
                            if (DONE_TASK_UPDATE_MISSING_IDS) {
                                Log.d(TAG, "DbOperationsRunnable: run(): checking DONE_TASK_UPDATE_MISSING_IDS ... " + DONE_TASK_UPDATE_MISSING_IDS);
                                break;
                            }
                        }
                        DONE_TASK_UPDATE_MISSING_IDS = false;
                        Log.d(TAG, "DbOperationsRunnable: run(): reset DONE_TASK_UPDATE_MISSING_IDS to " + DONE_TASK_UPDATE_MISSING_IDS);
                        try {
                            tableAllSyncInboxHandlerThread.quit();
                        } catch (Exception e) {
                            Log.d(TAG, "DbOperationsRunnable: run(): exception " + e);
                        }
                    }
                    break;
                }
            }
            DONE_TASK_COMPARETOPID = false;
                //READING from database  table TABLE_ALL
                Log.d(TAG, "DbOperationsRunnable: run(): Now reading values from TABLE_ALL ");
                db = db_helper.getReadableDatabase();
                int table = TABLE_ALL;
                switch (table) {
                    case TABLE_ALL:
                        Log.d(TAG, "DbOperationsRunnable: run(): inside case TABLE_ALL");
                        activity.readDbTableAllRunnable = new ReadDbTableAllRunnable(activity, db);
                        Log.d(TAG, "DbOperationsRunnable: run(): executing readDb thread in background");
                        activity.thread = new Thread(activity.readDbTableAllRunnable);
//                        activity.thread.start();    EDIT
                        break;
                }
 */
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
        private Cursor cursor_read_from_table_all;
        private boolean cursor_first;
        private int j = 0;

        ReadDbTableAllRunnable(MainActivity activity, SQLiteDatabase db) {
            handler = activity.main_handler;
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
            final String TAG_run = " ReadDbTableAllRunnable run(): ";
            Log.d(TAG, TAG_run + " called");
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
            db1.beginTransaction();
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
                Log.d(TAG, "ReadDbTableAllRunnable: run(): TABLE_ALL is empty");
            } else {
                Log.d(TAG, "ReadDbTableAllRunnable run: interator i = " + i);
                int index_id = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID);
                int index_corres_id = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID);
                int index_sms_body = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY);
                int index_sms_address = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS);
                int index_sms_epoch_date = cursor_read_from_table_all.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE);
                try {
                    messages_list_tableall.clear();
                    do {
                        Log.d(TAG, "ReadDbTableAllRunnable: run(): reading messages from TABLE_ALL");
                        long itemId = cursor_read_from_table_all.getLong(index_id);
                        String corress_inbox_id = cursor_read_from_table_all.getString(index_corres_id);
                        String sms_body = cursor_read_from_table_all.getString(index_sms_body);
                        String sms_address = cursor_read_from_table_all.getString(index_sms_address);
                        String epoch_date = cursor_read_from_table_all.getString(index_sms_epoch_date);
                        Log.d(TAG, TAG_run + " itemId = " + itemId);
                        Log.d(TAG, TAG_run + " corress_inbox_id = " + corress_inbox_id);
                        Log.d(TAG, TAG_run + " sms_body = " + sms_body);
                        Log.d(TAG, TAG_run + " sms_address = " + sms_address);
                        milli_seconds = Long.parseLong(epoch_date);
                        calendar.setTimeInMillis(milli_seconds);
                        printable_date = formatter.format(calendar.getTime());
                        Log.d(TAG, TAG_run + " epoch_date = " + epoch_date + " which is : " + printable_date);
                        Log.d(TAG, TAG_run + " ");
                        String str = "ItemID = " + itemId + "\ncorress_inbox_id = " + corress_inbox_id + "\n SMS From: " + getContactName(activity, sms_address) + "\n Recieved at: " + printable_date + "\n" + sms_body;
                        Log.d(TAG, "run(): progress_iterator = " + progress_iterator);
                        Log.d(TAG, "run(): i = " + i);
                        Log.d(TAG, "run(): adding into message_list_tableall at index + " + progress_iterator);
                        Log.d(TAG, "run(): messages_list.add(" + progress_iterator + ", " + str + ")");
                        messages_list_tableall.add(progress_iterator++, str);
                        Log.d(TAG, "ReadDbTableAllRunnable run(): incrementing iterator i to " + ++i);
                    } while (cursor_read_from_table_all.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "run(): exception : " + e);
                }
            }
            cursor_read_from_table_all.close();
            db1.endTransaction();
            db1.close();
            Log.d(TAG, "ReadDbTableAllRunnable: run():  Printing the whole messages_list : ");
            try {
                //pass the Runnable to MainThread handler because UI elements(sms_adapter) are on MainThread
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
//                            try {
                                j = 0;
                                while (j < messages_list_tableall.size()) {
                                    Log.d(TAG, "ReadDbTableAllRunnable: run(): j=" + j);
                                    Log.d(TAG, "ReadDbTableAllRunnable: run(): msg_list.get(" + j + ").toString() = \n" +
                                            messages_list_tableall.get(j).toString().substring(0, 20));
                                    if (j <= 200) {
                                        Log.d(TAG, "ReadDbTableAllRunnable: run(): j <= 200   calling  sms_adapter.insert(" + j +
                                                 ", messages_list_tableall.get("+j+").tostring()");
                                        activity.sms_adapter.insert(j, messages_list_tableall.get(j).toString());
                                    } else {
                                        Log.d(TAG, "ReadDbTableAllRunnable: run(): j>200");
                                        if (j % 50 == 0) {
                                            Log.d(TAG, "ReadDbTableAllRunnable: run(): j % 50 = " + j%50 + " == 0");
                                            Log.d(TAG, "ReadDbTableAllRunnable: run(): sms_adapter.append("+ (j-50) + ", " +
                                                    "messages_list_tableall.subList(" + (j-50) + ", " + j + "))" );
                                            activity.sms_adapter.append(j-50, messages_list_tableall.subList(j-50, j));
                                        }
                                        else{
                                            Log.d(TAG, "ReadDbTableAllRunnable: run(): j % 50 = " + j%50 + " != 0");
                                        }
                                    }
                                    j++;
                                }
                                //append the rest of the messages
                                if(j>200 && j%50!=0){
                                    Log.d(TAG, "ReadDbTableAllRunnable: run(): j>200 && j%50   true");
                                    Log.d(TAG, "ReadDbTableAllRunnable: run(): j%50 = " + j%50);
                                    Log.d(TAG, "ReadDbTableAllRunnable: run(): sms_adapter.append(" + (j-j%50) +
                                            ", messages_list_tableall.subList(" + (j-j%50) + ", " + j + "))");
                                    activity.sms_adapter.append(j - j%50, messages_list_tableall.subList(j-j%50, j));
                                }
//                            }
//                            catch (Exception e){
//                                    Log.d(TAG, "ReadDbTableAllRunnable: run(): exception : "+ e);
//                                }

                            }
                    });
            } catch (Exception e) {
                Log.d(TAG, "run(): exception : " + e);
            }
            Log.d(TAG, "run(): Finished reading TABLE_ALL");
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


