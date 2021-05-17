package com.example.mynewsmsapp_kotlin;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static com.example.mynewsmsapp_kotlin.MainActivity.getContactName;
import static com.example.mynewsmsapp_kotlin.MainActivity.inbox_sync_tableall;
import static com.example.mynewsmsapp_kotlin.MainActivity.table_all_sync_inbox;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.HAM;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.SPAM;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.UNCLASSIFIED;

public class TableAllSyncInboxHandlerThread  extends HandlerThread {
    private final String TAG = "[MY_DEBUG]";
    private Handler handler;
    private SpamBusterdbHelper db_helper;
    private SQLiteDatabase db;
    private List<String> item_ids_inbox = new ArrayList<String>();
    private List<String> item_coressinboxids_tableall = new ArrayList<String>();
    private List<String> item_ids_tableall = new ArrayList<String>();
    private List<String> missing_item_coressinboxids_in_tableall = new ArrayList<String>();
    private List<String> missing_item_ids_in_smsinbox = new ArrayList<String>();

    public static final int TASK_SYNCTABLES = 16;
    public static boolean DONE_TASK_SYNCTABLES = false;
    public static final int TASK_COMPARE_TOP_ID = 11;
    public static boolean DONE_TASK_COMPARETOPID = false;
    public static final int TASK_GET_IDS = 12;
    public static boolean DONE_TASK_GET_IDS_TABLEALL = false;
    public static boolean DONE_TASK_GET_IDS_SMSINBOX = false;
    public static final int TASK_GET_MISSING_IDS = 13;
    public static boolean DONE_TASK_GET_MISSING_IDS = false;
    public static boolean DONE_TASK_GET_MISSING_IDS_IN_TABLEALL = false;
    public static boolean DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX = false;
    public static final int TASK_UPDATE_MISSING_IDS = 14;
    public static boolean DONE_TASK_UPDATE_MISSING_IDS = false;
    public static final int TASK_NEWSMSREC = 15;
    public static final int DUMMY_VAL = 19999;
    public static boolean tableall_is_empty = false;
    public static boolean smsinbox_is_empty = false;

    public TableAllSyncInboxHandlerThread(SpamBusterdbHelper db_helper) {
        super("TableAllSyncHandlerThread");
        this.db_helper = db_helper;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        handler = new Handler() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void handleMessage(@NonNull Message msg) {
                Context context = MainActivity.instance();
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): A Message received !");
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): what = " + msg.what);
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): arg1 = " + msg.arg1);
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): arg2 = " + msg.arg2);
                switch (msg.what) {
                    case TASK_SYNCTABLES:

                        //	1. read columns _id, corress_inbox_id and spam  from TABLEALL
                        //	and put them in hashset_tableall_id and  hashset_tableall_corressinboxid respt
                        Set<String> hashset_tableall_id = new HashSet<>();
                        Set<String> hashset_tableall_corressinboxid = new HashSet<>();
                        Map<String, String> hashmap_corressinboxid_to_tableallid = new HashMap<>();
                        Map<String, String> hashmap_tableallid_to_corressinboxid = new HashMap<>();
                        Map<String, String> hashmap_tableallid_to_spam = new HashMap<>();

                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): reading _id, corresinboxid and spam from TABLEALL");
                        db = db_helper.getReadableDatabase();
                        String[] projection_id = {
                                BaseColumns._ID,
                                SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
                                SpamBusterContract.TABLE_ALL.COLUMN_SPAM
                        };
                        String selection_id = null;
                        String[] selection_args = null;
                        String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
                        Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                projection_id,             // The array of columns to return (pass null to get all)
                                selection_id,              // The columns for the WHERE clause
                                selection_args,          // The values for the WHERE clause
                                null,                   // don't group the rows
                                null,                   // don't filter by row groups
                                sort_order               // The sort order
                        );
                        if (!cursor_read_id.moveToFirst()) {
                            tableall_is_empty = true;
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): TABLE_ALL is empty");
                        }
                        else {
                            tableall_is_empty = false;
                            do {
                                String temp_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID));
                                String temp_spam_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SPAM));
                                String temp_corres_inbox_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
                                //2. also put them in hashsets and hashmaps
                                hashset_tableall_id.add(temp_id_holder);
                                hashset_tableall_corressinboxid.add(temp_corres_inbox_id_holder);
                                hashmap_tableallid_to_corressinboxid.put(temp_id_holder, temp_corres_inbox_id_holder);
                                hashmap_corressinboxid_to_tableallid.put(temp_corres_inbox_id_holder, temp_id_holder);
                                hashmap_tableallid_to_spam.put(temp_id_holder, temp_spam_holder);
                            } while (cursor_read_id.moveToNext());
                        }
                        cursor_read_id.close();

                        //3. Get all IDs from SMSINBOX and put them in hashset_smsinbox_id.
                        Set<String> hashset_smsinbox_id = new HashSet<>();
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): reading IDs from SMSINBOX");
                        ContentResolver content_resolver = MainActivity.instance().getContentResolver();
                        String[] projection = {
                          "_id"
                        };
                        Cursor cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), projection, null, null, "date DESC");
                        if(!cursor_check_sms_id.moveToFirst()){
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): SMSINBOX is empty");
                            smsinbox_is_empty = true;
                        }
                        else{
                            smsinbox_is_empty = false;
                            int index_inboxid = cursor_check_sms_id.getColumnIndex("_id");
                            do{
                                String temp_smsinboxid_holder = cursor_check_sms_id.getString(index_inboxid);
                                hashset_smsinbox_id.add(temp_smsinboxid_holder);
                            }while(cursor_check_sms_id.moveToNext());
                        }
                        cursor_check_sms_id.close();

                        //4. Find missing messages in TABLEALL
                        List<String> list_missing_in_tableall = new ArrayList<>(); //will contain the missing corressinboxids
                        Iterator<String> iterator_smsinboxid = hashset_smsinbox_id.iterator();
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): finding missing messages in TABLEALL");
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): missing corressinboxids in TABLEALL:");
                        while(iterator_smsinboxid.hasNext()){
                            String inboxid = iterator_smsinboxid.next();
                            if(!hashset_tableall_corressinboxid.contains(inboxid)){
                                list_missing_in_tableall.add(inboxid);
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): ["+inboxid+"]");
                            }
                        }
                        if (list_missing_in_tableall.isEmpty()){
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): no missing messages in TABLEALL");
                        }

                        //5. Update TABLEALL using the list_missing_in_tableall
                        if (!list_missing_in_tableall.isEmpty())
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): updating TABLE_ALL with those missing messages");
                        for(String missingcorressinboxid : list_missing_in_tableall){
                            //read from SMSINBOX the columns address, date and date_sent
                            String[] projection1 = {
                                    "address",
                                    "date",
                                    "date_sent",
                                    "body"
                            };
                            String selection1 = "_id=?";
                            String[] selectionargs1 = {
                                    missingcorressinboxid
                            };
                            cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), projection1, selection1, selectionargs1, "date DESC");

                            if(!cursor_check_sms_id.moveToFirst()){
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): SMSINBOX is empty");
                            }
                            else {
                                //read values  from cursor_check_sms_id
                                String address = cursor_check_sms_id.getString(cursor_check_sms_id.getColumnIndexOrThrow("address"));
                                String date  = cursor_check_sms_id.getString(cursor_check_sms_id.getColumnIndexOrThrow("date"));
                                String date_sent = cursor_check_sms_id.getString(cursor_check_sms_id.getColumnIndexOrThrow("date_sent"));
                                String body = cursor_check_sms_id.getString(cursor_check_sms_id.getColumnIndexOrThrow("body"));

                                //insert all the above in TABLEALL
                                SQLiteDatabase db1 = db_helper.getWritableDatabase();
                                ContentValues values = new ContentValues();
                                do {
                                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, missingcorressinboxid);
                                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS, address);
                                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE, date);
                                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY, body);
                                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT, date_sent);
                                    //if sender is in our contacts, it means their message can be trusted as HAM
                                    if(!getContactName(MainActivity.instance(), address).equals(address)){
                                        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, HAM);
                                    }
                                    else {
                                        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, UNCLASSIFIED);
                                    }
                                    db1.beginTransaction();
                                    long newtableallrowid = db1.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, null, values);
                                    db1.setTransactionSuccessful();
                                    db1.endTransaction();
                                    if (newtableallrowid < 0) {
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): insertion in TABLEALL unsuccessfull");
                                    } else {
                                        hashset_tableall_id.add(String.valueOf(newtableallrowid));
                                        hashset_tableall_corressinboxid.add(missingcorressinboxid);
                                        String newtableallrowid_str = String.valueOf(newtableallrowid);
                                        hashmap_tableallid_to_corressinboxid.put(newtableallrowid_str, missingcorressinboxid);
                                        hashmap_corressinboxid_to_tableallid.put(missingcorressinboxid, newtableallrowid_str);
                                        //if sender is in our contacts, it means their message can be trusted as HAM
                                        if(!getContactName(MainActivity.instance(), address).equals(address)) {
                                            hashmap_tableallid_to_spam.put(newtableallrowid_str, HAM);
                                        }
                                        else {
                                            hashmap_tableallid_to_spam.put(newtableallrowid_str, UNCLASSIFIED);
                                        }
                                    }
                                } while (cursor_check_sms_id.moveToNext());
                            }
                        }
                        if(!list_missing_in_tableall.isEmpty())
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): Done updating TABLEALL");
                        cursor_check_sms_id.close();

                        //8 check if internet connection is present
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): checking internet connection...");
                        if(new SBNetworkUtility().checkNetwork(MainActivity.instance())){
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): internet connection present ");
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): preparing for classification");

                            List<String> list_unclassified_tableallid = new ArrayList<>();
                            Map<String, String> hashmap_tableallid_to_message_forclassification = new HashMap<>();

                            for (String tableallid : hashset_tableall_id) {
                                //select only UNCLASSIFIED tableallids
                                if (hashmap_tableallid_to_spam.get(tableallid).equals(UNCLASSIFIED)) {
                                    list_unclassified_tableallid.add(tableallid);
                                }
                            }

                            //get message body of those unclassified tableallids
                            for(String tableallid_unclass: list_unclassified_tableallid) {
                                db = db_helper.getReadableDatabase();
                                projection = new String[]{
                                        SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY
                                };
                                String selection = SpamBusterContract.TABLE_ALL._ID + "=?";
                                selection_args = new String[] { tableallid_unclass };
                                Cursor cursor = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                        projection,             // The array of columns to return (pass null to get all)
                                        selection,              // The columns for the WHERE clause
                                        selection_args,          // The values for the WHERE clause
                                        null,                   // don't group the rows
                                        null,                   // don't filter by row groups
                                        null               // The sort order
                                );
                                if (!cursor.moveToFirst()){
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): unable to read from TABLE_ALL, probably empty");
                                }
                                else{
//                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): mapping TABLEALL ids to messages ");
                                    do{
                                        String temp_message_holder = cursor.getString(cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY));
                                        //do not put OTP message for classification
                                        //check if it is an OTP
                                        //if message is OTP, this means it should be declared HAM
                                        if(MySmsMessage.isMessageOTP(temp_message_holder)) {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): Message is an OTP so do not send for prediction. Declaring HAM");
                                            //get the address, date, date_sent of the tableallid
                                            MySmsMessage mySmsMessage = DbOperationsUtility.getInstance().getMessageFromTablallId(tableallid_unclass, db_helper);
                                            //insert into SMSINBOX, get latestinboxid
                                            try {
                                                //get corressinboxid of the message from SMSINBOX
                                                String newinboxid = hashmap_tableallid_to_corressinboxid.get(tableallid_unclass);
                                                //insert in SMSINBOX only if not already present in SMSINBOX
                                                if(!hashset_smsinbox_id.contains(hashmap_tableallid_to_corressinboxid.get(tableallid_unclass))) {
                                                    newinboxid = DbOperationsUtility.getInstance().insertIntoSmsInbox(mySmsMessage, context);
                                                }
                                                //prepare mySmsMessage to be inserted into TABLEALL
                                                mySmsMessage.setTableallid(tableallid_unclass);
                                                mySmsMessage.setCorressinboxid(newinboxid);
                                                mySmsMessage.setSpam(HAM);
                                                //update  TABLEALL with spam=HAM and corressinboxid=latestinboxid
                                                //if updation was successfull, then remove the tableallid from list_unclassified_tableallid
                                                if(DbOperationsUtility.getInstance().updateMessageInTableAll(mySmsMessage, db_helper)){
                                                    list_unclassified_tableallid.remove(mySmsMessage.getTableallid());
                                                }
                                                else{
//                                                    This means message has been successfully inserted in SMSINBOX, but in TABLEALL it is still marked as UNCLASSIFIED
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): could not update TABLEALL, maybe next restart of app will do the trick");
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): This means message has been successfully inserted in SMSINBOX, but in TABLEALL it is still marked as UNCLASSIFIED");
                                                }
                                            }
                                            catch (InsertionFailedException e){
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): the OTP could not be inserted in SMSINBOX. Not updating TABLEALL either");
                                            }
                                        }
                                        //else, if message is not OTP, then we have to prepare for classification
                                        else {
                                            hashmap_tableallid_to_message_forclassification.put(tableallid_unclass, temp_message_holder);
//                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): " + tableallid_unclass + " : " + temp_message_holder);
                                        }
                                    }while (cursor.moveToNext());
                                }
                            }

                            //classify
                            Map<String, Integer> hashmap_classification_result = new PredictionUtility(MainActivity.instance()).makePrediction(hashmap_tableallid_to_message_forclassification);
                            if(hashmap_classification_result != null){
                                for(String tableallid : list_unclassified_tableallid) {
                                    int spamresultint = hashmap_classification_result.get(tableallid);
                                    String spamresult;
                                    if (spamresultint == 1) {
                                        spamresult = SPAM;
                                    } else if (spamresultint == 0) {
                                        spamresult = HAM;
                                    } else {
                                        spamresult = UNCLASSIFIED;
                                    }

                                    //if HAM then insert into SMSINBOX, and update TABLEALL with corressinboxid = newrowsmsid
                                    if (spamresult.equals(HAM)) {
                                        //insert only if not present in SMSINBOX already
                                        if (!hashset_smsinbox_id.contains(hashmap_tableallid_to_corressinboxid.get(tableallid))) {
                                            //first get the latest_smsinboxid, so that we can know insertion is successfull after inserting
                                            String latest_smsinboxid = getLatestSMSINBOXid();

                                            //now insert into SMSINBOX the HAM message
                                            //for that we have to first read the address, body, date, date_sent from TABLEALL
                                            db = db_helper.getReadableDatabase();
                                            projection = new String[]{
                                                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS,
                                                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE,
                                                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT
                                            };
                                            String selection = SpamBusterContract.TABLE_ALL._ID + "=?";
                                            selection_args = new String[]{tableallid};
                                            Cursor cursor = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                                    projection,             // The array of columns to return (pass null to get all)
                                                    selection,              // The columns for the WHERE clause
                                                    selection_args,          // The values for the WHERE clause
                                                    null,                   // don't group the rows
                                                    null,                   // don't filter by row groups
                                                    null               // The sort order
                                            );
                                            String temp_address_holder = null;
                                            String temp_date_holder = null;
                                            String temp_datesent_holder = null;
                                            if (!cursor.moveToFirst()) {
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): unable to read from TABLE_ALL, probably empty");
                                            } else {
                                                temp_address_holder = cursor.getString(cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS));
                                                temp_date_holder = cursor.getString(cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE));
                                                temp_datesent_holder = cursor.getString(cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT));
                                            }
                                            cursor.close();

                                            //prepare to insert into SMSINBOX
                                            ContentResolver contentResolver = MainActivity.instance().getContentResolver();
                                            ContentValues values = new ContentValues();
                                            values.clear();
                                            values.put("address", temp_address_holder);
                                            values.put("person", "2");
                                            values.put("date", temp_date_holder);
                                            values.put("date_sent", temp_datesent_holder);
                                            values.put("body", hashmap_tableallid_to_message_forclassification.get(tableallid));
                                            db = db_helper.getWritableDatabase();
                                            Uri uri = Uri.parse("content://sms/inbox");
                                            db.beginTransaction();
                                            contentResolver.insert(uri, values);
                                            db.setTransactionSuccessful();
                                            db.endTransaction();

                                            //again get latest smsinbox id to see if it is actually greater than our previous latest smsinboxid to confirm successfull insertion
                                            String newlatestsmsinboxid = getLatestSMSINBOXid();
                                            if (Integer.parseInt(newlatestsmsinboxid) > Integer.parseInt(latest_smsinboxid)) {
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): insertion in SMSINBOX successfull");
                                                String corressinboxid = newlatestsmsinboxid;

                                                //now update the TABLEALL with column_spam = HAM and column_corressinboxid = corressinboxid
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): updating TABLE_ALL with the classified messages");
                                                values.clear();
                                                values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corressinboxid);
                                                values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, HAM);
                                                db.beginTransaction();
                                                String[] whereArgs = new String[]{tableallid};
                                                db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
                                                db.setTransactionSuccessful();
                                                db.endTransaction();
                                            }
                                            else{
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): insertion in SMSINBOX failed, so not updating TABLEALL either");
                                            }
                                        }
                                        //else the messages which were originally present in SMSINBOX but unclassified(because the messaegs resided in the phone before the app was installed)
                                        // these messages need to be classified but not put in SMSINBOX even if HAM, which in this case they are since we are inside the spamresult==HAM condition
                                        // only update TABLEALL with column_spam = HAM
                                        else{
                                            ContentValues values = new ContentValues();
                                            values.clear();
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, HAM);
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): updating TABLEALL ID: " + tableallid + " to HAM");
                                            try {
//                                                db.beginTransaction();
//                                                String[] whereArgs = new String[]{tableallid};
//                                                db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
//                                                db.setTransactionSuccessful();
//                                                db.endTransaction();
                                                MySmsMessage mySmsMessage = new MySmsMessage();
                                                mySmsMessage.setTableallid(tableallid);
                                                mySmsMessage.setCorressinboxid(hashmap_tableallid_to_corressinboxid.get(tableallid));
                                                mySmsMessage.setSpam(HAM);
                                                if(DbOperationsUtility.getInstance().updateMessageInTableAll(mySmsMessage, db_helper)){
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): updated TABLEALL");
                                                }
                                                else{
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): Could not update TABLEALL");
                                                }
                                            }
                                            catch (IllegalStateException e){
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): could not update TABLEALL");
                                            }
                                        }
                                    }

                                    //else if SPAM, then just update TABLEALL with column_spam = SPAM
                                    else if (spamresult.equals(SPAM)) {
                                        ContentValues values = new ContentValues();
                                        values.clear();
                                        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, SPAM);
                                        db.beginTransaction();
                                        String[] whereArgs = new String[]{tableallid};
                                        db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
                                        db.setTransactionSuccessful();
                                        db.endTransaction();
                                    } else {
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): classification failed for tableallid : " + tableallid);
                                    }
                                }
                            }
                            else{
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): API probing failed");
                            }
                        }
                        else{
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): No internet connection detected");
                        }

//                        db.close();
//                        Toast.makeText(MainActivity.instance(), "Trying to make toast from TableAllSyncInboxHandlerThread", Toast.LENGTH_LONG);
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): printing Mainactivity.instance(): " + MainActivity.instance());
                        DONE_TASK_SYNCTABLES = true;
                        break; //break from case TASK_SYNCTABLES
                }
                }
            };
    }

        private String getLatestSMSINBOXid(){
            String[] projection_sms_inbox = null;
            String selection_sms_inbox = null;
            String[] selection_args_sms_inbox = null;
            String sort_order_sms_inbox = " _id DESC ";
            Uri uri = Uri.parse("content://sms/inbox");
            ContentResolver contentResolver = MainActivity.instance().getContentResolver();
            Cursor sms_inbox_cursor = contentResolver.query(uri, projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
            String latest_inbox_id = "";
            int index_id = sms_inbox_cursor.getColumnIndex("_id");
            if (sms_inbox_cursor.moveToFirst()) {
                latest_inbox_id = sms_inbox_cursor.getString(index_id);
                sms_inbox_cursor.close();
            }
            else{
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): SMSINBOX empty");
            }
            sms_inbox_cursor.close();
            return latest_inbox_id;
        }

        public Handler getHandler () {
            return handler;
        }
}

