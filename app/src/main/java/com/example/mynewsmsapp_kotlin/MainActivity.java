package com.example.mynewsmsapp_kotlin;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import static com.example.mynewsmsapp_kotlin.GetPersonsHandlerThread.LOADED_ALL;
import static com.example.mynewsmsapp_kotlin.GetPersonsHandlerThread.LOADED_INBOX;
import static com.example.mynewsmsapp_kotlin.GetPersonsHandlerThread.LOADED_SPAM;
import static com.example.mynewsmsapp_kotlin.TableAllSyncInboxHandlerThread.DONE_TASK_SYNCTABLES;
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
    public static final ArrayList<String> all_list = new ArrayList<>();
    public static final ArrayList<String> spam_list = new ArrayList<>();
    public static final ArrayList<String> inbox_list = new ArrayList<>();
    private static final int VALID_DURATION = 0;
    private static final int INVALID_DURATION_DAYS = -6065;
    private static final int INVALID_DURATION_NOTINTEGER = -6066;

    private GetPersonsHandlerThread getPersonsHandlerThread;

    protected DisplayPersonsRunnable displayPersonsRunnable;
    private TableAllSyncInboxHandlerThread tableAllSyncInboxHandlerThread;
    private Handler main_handler = new Handler();
    public static boolean table_all_sync_inbox = false;   //shows whether our TABLE_ALL is in sync with inbuilt sms/inbox
    public static boolean inbox_sync_tableall = false;   //shows whether our CONTENT_SMS_INBOX is in sync with TABLE_ALL
    private SharedPreferences autodelete_settings;
    public static String auto_delete_duration;
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
    private ToggleButton toggleButton_all;
    private ToggleButton toggleButton_inbox;
    private ToggleButton toggleButton_spam;
    private ImageButton btn_preferences;
    public static int toggled_section = TABLE_ALL;
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
        active = false; //indicate that activity is killed, check bottom of SmsBroadcastReceiver.onReceive() method
        try {
            tableAllSyncInboxHandlerThread.quit();
            getPersonsHandlerThread.quit();
        } catch (Exception e) {
            Log.d(TAG, "MainActivity: onDestroy(): exception " + e);
        }
        finally {
            spamBusterdbHelper.close();
            super.onDestroy();
        }
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
        toggleButton_all = (ToggleButton) findViewById(R.id.all_sms_toggle);
        toggleButton_inbox = (ToggleButton) findViewById(R.id.inbox_sms_toggle);
        toggleButton_spam = (ToggleButton) findViewById(R.id.spam_sms_toggle);
        btn_preferences = (ImageButton) findViewById(R.id.button_preferences);
        //enable toggle tableham on creation of activity
        toggleButton_all.setChecked(true);
        spamBusterdbHelper = new SpamBusterdbHelper(this);

        autodelete_settings = PreferenceManager.getDefaultSharedPreferences(this);
        //set deault autodelete if not set by user to 28 days
        String current_duration = autodelete_settings.getString("autodelete_dur", "");
        if (current_duration.equals("")) {
            Log.d(TAG, "MainActivity: onCreate(): autodelete duration is empty. setting to default 28");
            if(setAutoDeletePreference(28)){
                Log.d(TAG, "MainActivity: onCreate(): Could not set default autodelete duration ");
            }
            else{
                Log.d(TAG, "MainActivity: onCreate(): Autodelete duration set to default 28");
            }
        }
        else{
            auto_delete_duration = current_duration;
        }

        //setting onclick listeners
        toggleButton_all.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (toggleButton_all.isChecked()){
                    toggleButton_inbox.setChecked(false);
                    toggleButton_spam.setChecked(false);
                    toggled_section = TABLE_ALL;
                    showAll();
                }
            }
        });
        toggleButton_inbox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (toggleButton_inbox.isChecked()) {
                    toggleButton_all.setChecked(false);
                    toggleButton_spam.setChecked(false);
                    toggled_section = TABLE_HAM;
                    showInbox();
                }
            }
        });
        toggleButton_spam.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (toggleButton_spam.isChecked()){
                    toggleButton_inbox.setChecked(false);
                    toggleButton_all.setChecked(false);
                    toggled_section = TABLE_SPAM;
                    showSpam();
                }
            }
        });

        btn_preferences.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                preferencesDialog();
            }
        });

        //start background service
        startService(new Intent(getApplicationContext(),ClassificationSyncService.class));

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


    //reads all addresses from database and display  the persons list
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
//        Log.d(TAG, TAG_getContactName + " called ");
        ContentResolver content_resolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone_number));
        Cursor cursor = content_resolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        final int index_displayname = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
        if (cursor == null) {
//            Log.d(TAG, "MainActivity: getContactName(): address: " + phone_number + " not present in address book");
            return phone_number;
        }
        String name = phone_number;
        if (cursor.moveToNext()) {
            name = cursor.getString(index_displayname);
//            Log.d(TAG, "MainActivity: getContactName(): address " + phone_number + " found in address book with name " + name);
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
    public void updateInbox(final String sms_message_str, MySmsMessage mySmsMessage) {
        Handler handler;
        final String TAG_updateInbox = " updateInbox(): ";
        Log.d(TAG, TAG_updateInbox + " called ");

        try {
            //always place new sms at top i.e index 0
            String messagetodisplay = "New Message from : " + MainActivity.getContactName(instance(), mySmsMessage.getAddress()) + "\n\n" + mySmsMessage.getBody() + "\n";
            sms_adapter.insert(0, messagetodisplay);
            sms_adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.d(TAG, TAG_updateInbox + " Exception : " + e);
        }

        //add code to store sms_message inside the sms/inbox and database table
        String sms_body = mySmsMessage.getBody();
        String address = mySmsMessage.getAddress();
        String date_sent = mySmsMessage.getDatesent();
        String date = mySmsMessage.getDate();
        Log.d(TAG, "MainActivity: updateInbox(): sms_body: " + sms_body);
        Log.d(TAG, "MainActivity: updateInbox(): address: " + address);
        Log.d(TAG, "MainActivity: updateInbox(): date_sent: " + date_sent);
        Log.d(TAG, "MainActivity: updateInbox(): date (received): " + date);
        NewSmsMessageRunnable newSmsMessageRunnable = new NewSmsMessageRunnable(this, spamBusterdbHelper, mySmsMessage);
//        newSmsMessageRunnable.sms_body = sms_body;
//        newSmsMessageRunnable.address = address;
//        newSmsMessageRunnable.date_sent = date_sent;
//        newSmsMessageRunnable.date = date;
//        newSmsMessageRunnable.message_is_spam = true;//very important field. In future this will be changed after returning result from server
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
//
////            this.db_helper = new SpamBusterdbHelper(activity);
//            this.db_helper1 = activity.spamBusterdbHelper;
//            //get persons list and put in persons_list
//            activity.getPersonsHandlerThread = new GetPersonsHandlerThread(db_helper1);
//            this.getPersonsHandlerThread = activity.getPersonsHandlerThread;
//            this.getPersonsHandlerThread.start();
//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            this.handler1 = getPersonsHandlerThread.getHandler();
//            Message msg_getpersons = Message.obtain(handler1);
//            msg_getpersons.what = GetPersonsHandlerThread.TASK_GET_PERSONS;
//            int table1 = TABLE_ALL;
//            msg_getpersons.arg1 = table1;
//            Log.d(TAG, "DbOperationsRunnable: run(): preparing message msg_getperson with following attributes:");
//            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.what = " + msg_getpersons.what);
//            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.arg1 = " + msg_getpersons.arg1);
//            Log.d(TAG, "DbOperationsRunnable: run(): sending message...");
//            msg_getpersons.sendToTarget();
//
//            while(true) {
//                if(DONE_TASK_GETPERSONS) {
//                    DONE_TASK_GETPERSONS = false;
//                    activity.displayPersonsRunnable = new DisplayPersonsRunnable(activity);
//                    activity.thread = new Thread(activity.displayPersonsRunnable);
//                    activity.thread.start();
//                    break;
//                }
//            }
            showPersonsList();

            MainActivity.instance().getSpam();
            MainActivity.instance().getInbox();

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
            while(true) {
                if(DONE_TASK_SYNCTABLES) {
                    DONE_TASK_SYNCTABLES = false;
                    showPersonsList();
                    break;
                }
            }
        }

        private void showPersonsList(){
            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
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
                    activity.displayPersonsRunnable = new DisplayPersonsRunnable(activity);
                    activity.thread = new Thread(activity.displayPersonsRunnable);
                    activity.thread.start();
                    break;
                }
            }
        }
    }

    private static class DisplayPersonsRunnable implements Runnable{
        private Handler handler;
        private WeakReference<MainActivity> activityWeakReference;
        private SQLiteDatabase db1;

        DisplayPersonsRunnable(MainActivity activity) {
            handler = activity.main_handler;
            activityWeakReference = new WeakReference<MainActivity>(activity);
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
                            //first clear the sms adapter
                            activity.sms_adapter.clearItems();
                            if(persons_list.size()!=0) {
                                Log.d(TAG, "DisplayPersonsRunnable: run(): inserting persons_list into the sms adapter");
                                //persons_list is global static which has addresses of ALL persons and is filled by class GetPersonsHandlerThread
                                for (int i = 0; i < persons_list.size(); i++) {
//                                    Log.d(TAG, "DisplayPersonsRunnable: run(): inserting " + getContactName(activity, persons_list.get(i)) + " in sms_adapter");
                                    activity.sms_adapter.insertPerson(i, persons_list.get(i));
                                }
                            }
//                            activity.sms_adapter.addAllItems(persons_list);
                        }
                    });
                }
                catch (Exception e) {
                    Log.d(TAG, "run(): exception : " + e);
                }
        }
    }

    //we call this implicitly, whether or not the user had toggled the button
    private void getAll(){
        if (!LOADED_ALL) {
            Message msg_getpersons = Message.obtain(getPersonsHandlerThread.getHandler());
            msg_getpersons.what = GetPersonsHandlerThread.TASK_GET_PERSONS;
            int table1 = TABLE_ALL;
            msg_getpersons.arg1 = table1;
            Log.d(TAG, "DbOperationsRunnable: run(): preparing message msg_getperson with following attributes:");
            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.what = " + msg_getpersons.what);
            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.arg1 = " + msg_getpersons.arg1);
            Log.d(TAG, "DbOperationsRunnable: run(): sending message...");
            msg_getpersons.sendToTarget();
            while (true) {
                if (DONE_TASK_GETPERSONS) {
                    DONE_TASK_GETPERSONS = false;
                    break;
                }
            }
            LOADED_ALL = true;
        } else {
            persons_list.clear();
            persons_list.addAll(all_list);
        }
    }

    //called when toggleAll is clicked
    private void showAll() {
        getAll();
        //show persons
        MainActivity.instance().displayPersonsRunnable = new DisplayPersonsRunnable(MainActivity.instance());
        this.thread = new Thread(MainActivity.instance().displayPersonsRunnable);
        this.thread.start();
    }

    private void getInbox(){
        if(!LOADED_INBOX) {
            Message msg_getpersons = Message.obtain(getPersonsHandlerThread.getHandler());
            msg_getpersons.what = GetPersonsHandlerThread.TASK_GET_PERSONS;
            int table1 = TABLE_HAM;
            msg_getpersons.arg1 = table1;
            Log.d(TAG, "DbOperationsRunnable: run(): preparing message msg_getperson with following attributes:");
            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.what = " + msg_getpersons.what);
            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.arg1 = " + msg_getpersons.arg1);
            Log.d(TAG, "DbOperationsRunnable: run(): sending message...");
            msg_getpersons.sendToTarget();
            while (true) {
                if (DONE_TASK_GETPERSONS) {
                    DONE_TASK_GETPERSONS = false;
                    break;
                }
            }
            LOADED_INBOX = true;
        }
        else{
            persons_list.clear();
            persons_list.addAll(inbox_list);
        }
    }

    //called when toggleInbox is clicked
    private void showInbox(){
        getInbox();
        //display persons
        MainActivity.instance().displayPersonsRunnable = new DisplayPersonsRunnable(MainActivity.instance());
        this.thread = new Thread(MainActivity.instance().displayPersonsRunnable);
        this.thread.start();
    }

    private void getSpam(){
        if(!LOADED_SPAM) {
            Message msg_getpersons = Message.obtain(getPersonsHandlerThread.getHandler());
            msg_getpersons.what = GetPersonsHandlerThread.TASK_GET_PERSONS;
            int table1 = TABLE_SPAM;
            msg_getpersons.arg1 = table1;
            Log.d(TAG, "DbOperationsRunnable: run(): preparing message msg_getperson with following attributes:");
            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.what = " + msg_getpersons.what);
            Log.d(TAG, "DbOperationsRunnable: run(): msg_getpersons.arg1 = " + msg_getpersons.arg1);
            Log.d(TAG, "DbOperationsRunnable: run(): sending message...");
            msg_getpersons.sendToTarget();
            while (true) {
                if (DONE_TASK_GETPERSONS) {
                    DONE_TASK_GETPERSONS = false;
                    break;
                }
            }
            LOADED_SPAM = true;
        }
        else{
            persons_list.clear();
            persons_list.addAll(spam_list);
        }
    }

    //called when toggleSPam is cliekd
    private void showSpam(){
        getSpam();
        //display persons
        MainActivity.instance().displayPersonsRunnable = new DisplayPersonsRunnable(MainActivity.instance());
        this.thread = new Thread(MainActivity.instance().displayPersonsRunnable);
        this.thread.start();
    }

    private void preferencesDialog(){
        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.instance());
        View mView = MainActivity.instance().getLayoutInflater().inflate(R.layout.setting_autodelete,null);
        Button btn_cancel = (Button)mView.findViewById(R.id.btn_cancel_autodelete);
        Button btn_okay = (Button)mView.findViewById(R.id.btn_okay_autodelete);
        final EditText et_autodelete_duration = (EditText)mView.findViewById(R.id.et_autodelete_duration);
        String current_duration = autodelete_settings.getString("autodelete_dur", "");
        if(!current_duration.equals("")){
            et_autodelete_duration.setHint("Current value: " + current_duration + " days");
            btn_okay.setText("Update");
        }
        alert.setView(mView);
        final AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        //cancel button just exits the dialog box
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        btn_okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable readval = et_autodelete_duration.getText();
                String str_duration = readval.toString();
                switch (checkDurationValidity(str_duration)){
                    case VALID_DURATION:
                        if(setAutoDeletePreference(Integer.parseInt(str_duration))){
                            Toast.makeText(MainActivity.instance(), "Autodelete set to " + str_duration, Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(MainActivity.instance(), "Could not update autodelete duration settings", Toast.LENGTH_SHORT).show();
                        }
                        alertDialog.dismiss();
                        break;
                    case INVALID_DURATION_DAYS:
                        Toast.makeText(MainActivity.instance(), "Number of days should be between 7 and 60", Toast.LENGTH_SHORT).show();
                        break;
                    case INVALID_DURATION_NOTINTEGER:
                        Toast.makeText(MainActivity.instance(), "Invalid duration entered", Toast.LENGTH_SHORT).show();
                        break;
                }
                Log.d(TAG, "MainActivity: onClick(): duration entered by user: " + str_duration);
            }
        });
        alertDialog.show();
    }

    private int checkDurationValidity(String val){
        int errorCode = VALID_DURATION;
        if(val.length() == 0){
            errorCode = INVALID_DURATION_NOTINTEGER;
        }
        else {
            for (int i = 0; i < val.length(); i++) {
                if (!Character.isDigit(val.charAt(i))) {
                    Toast.makeText(MainActivity.instance(), "Incorrect value entered. Please enter only digits", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "MainActivity: onClick(): non-digit character encountered. Invalid");
                    errorCode = INVALID_DURATION_NOTINTEGER;
                    break;
                }
            }
            if (errorCode == VALID_DURATION) {
                Log.d(TAG, "MainActivity: onClick(): digits entered ");
                if (Integer.parseInt(val) >= 7 && Integer.parseInt(val) <= 60) {
                    Log.d(TAG, "MainActivity: validDurationEntered(): valid duration entered");
                } else {
                    Log.d(TAG, "MainActivity: validDurationEntered(): Invald duration. Should be greater than 7 and less than 60");
                    errorCode = INVALID_DURATION_DAYS;
                }
            }
        }
        return errorCode;
    }

    private boolean setAutoDeletePreference(int defaultduration) {
            SharedPreferences.Editor editor = autodelete_settings.edit();
            editor.putString("autodelete_dur", String.valueOf(defaultduration));
            editor.apply();
            //check if updated
            String current_duration = autodelete_settings.getString("autodelete_dur", "");
            if(current_duration.equals(String.valueOf(defaultduration))){
                Log.d(TAG, "MainActivity: setDefaultAutoDeleteIfNotPresent(): success");
                auto_delete_duration = current_duration;
                return true;
            }
            return false;
        }

    public Handler getHandler(){
        return this.main_handler;
    }
} //MainActivity class ends


