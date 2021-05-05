package com.example.mynewsmsapp_kotlin;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;

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
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): A Message received !");
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): what = " + msg.what);
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): arg1 = " + msg.arg1);
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): arg2 = " + msg.arg2);
                switch (msg.what) {
                    case TASK_SYNCTABLES:

                        //first check if any of the tables TABLE_ALL and CONTENTSMSINBOX are empty
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): Entered case TASK_SYNCTABLES");
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): using db_helper : " + db_helper);
                        db = db_helper.getReadableDatabase();
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): Database object ready, db : " + db);
                        String[] projection_id = {
                                BaseColumns._ID,
                                SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID
                        };
                        String selection_id = null;
                        String[] selection_args = null;
                        String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): reading values from TABLE_ALL");
                        Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                projection_id,             // The array of columns to return (pass null to get all)
                                selection_id,              // The columns for the WHERE clause
                                selection_args,          // The values for the WHERE clause
                                null,                   // don't group the rows
                                null,                   // don't filter by row groups
                                sort_order               // The sort order
                        );
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): cursor_read_id is ready");
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): cursor_read_id :  " + cursor_read_id.toString());
                        if (!cursor_read_id.moveToFirst()) {
                            tableall_is_empty = true;
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): TABLE_ALL is empty");
                        } else {
                            tableall_is_empty = false;
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): TABLE_ALL not empty");
                        }
                        cursor_read_id.close();
                        ContentResolver content_resolver = MainActivity.instance().getContentResolver();
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): reading messages from CONTENTSMSINBOX");
                        Cursor cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");
                        if (cursor_check_sms_id.moveToFirst()) {
                            smsinbox_is_empty = false;
                            int index_id = cursor_check_sms_id.getColumnIndex("_id");
                        } else {
                            smsinbox_is_empty = true;
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): CONTENTSMSINBOX is empty");
                        }
                        //if only one of them is empty, then it means they are out of sync
                        if (tableall_is_empty ^ smsinbox_is_empty) {
                            table_all_sync_inbox = false;
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): TABLE_ALL and CONTENTSMSINBOX are out of sync");
                        }

//----------------------------------------------------------------
                        //get ids table_all
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): getting corresponding inbox ids from TABLE_ALL ");
                        String[] projection_id1 = {
                                BaseColumns._ID,
                                SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID
                        };
                        selection_id = null;
                        selection_args = null;
                        sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
                        Cursor cursor_read_id1 = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                projection_id1,             // The array of columns to return (pass null to get all)
                                selection_id,              // The columns for the WHERE clause
                                selection_args,          // The values for the WHERE clause
                                null,                   // don't group the rows
                                null,                   // don't filter by row groups
                                sort_order               // The sort order
                        );
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): Cursor ready to read TABLE_ALL");
                        if (!cursor_read_id1.moveToFirst()) {
                            tableall_is_empty = true;
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): TABLE_ALL is empty!");
                        } else {
                            tableall_is_empty = false;
                            do {
                                String temp_id_holder = cursor_read_id1.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID));
                                String temp_corres_inbox_id_holder = cursor_read_id1.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): id:" + temp_id_holder);
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): corress_inbox_id:" + temp_corres_inbox_id_holder);
                                item_ids_tableall.add(temp_id_holder);
                                item_coressinboxids_tableall.add(temp_corres_inbox_id_holder);
                            } while (cursor_read_id1.moveToNext());
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): item_ids_tableall is filled with corress_inbox_ids");
                        }

//---------------------------------------------------------------------
                        //get ids of contentsmsinbox
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): geting ids from CONTENTSMSINBOX");
                        db = db_helper.getReadableDatabase();
                        content_resolver = MainActivity.instance().getContentResolver();
                        cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, "_id DESC");
                        String latest_sms_id_in_inbuilt_sms_inbox = "";
                        String latest_sms_thread_id_in_inbuilt_sms_inbox = "";
                        String id_inbox = "";
                        String threadid_inbox = "";
                        String address_inbox = "";
                        String body_inbox = "";
                        String person_inbox = "";
                        String date_inbox = "";
                        String date_sent_inbox = "";
                        String protocol_inbox = "";
                        String read_inbox = "";
                        String status_inbox = "";
                        String type_inbox = "";
                        String reply_path_present_inbox = "";
                        String subject_inbox = "";
                        String service_center_inbox = "";
                        String locked_inbox = "";
                        String sub_id_inbox = "";
                        String error_code_inbox = "";
                        String creator_inbox = "";
                        String seen_inbox = "";
                        if (cursor_check_sms_id.moveToFirst()) {
                            smsinbox_is_empty = false;
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: dumping the whole sms/inbox :");
                            int index_thread_id = cursor_check_sms_id.getColumnIndex("thread_id");
                            int index_id = cursor_check_sms_id.getColumnIndex("_id");
                            int index_address = cursor_check_sms_id.getColumnIndex("address");
                            int index_person = cursor_check_sms_id.getColumnIndex("person"); //DEBUG
                            int index_date = cursor_check_sms_id.getColumnIndex("date");
                            int index_date_sent = cursor_check_sms_id.getColumnIndexOrThrow("date_sent");
                            int index_protocol = cursor_check_sms_id.getColumnIndex("protocol"); //DEBUG
                            int index_read = cursor_check_sms_id.getColumnIndex("read"); //DEBUG
                            int index_status = cursor_check_sms_id.getColumnIndex("status"); //DEBUG
                            int index_type = cursor_check_sms_id.getColumnIndex("type"); //DEBUG
                            int index_replypathpresent = cursor_check_sms_id.getColumnIndex("reply_path_present"); //DEBUG
                            int index_subject = cursor_check_sms_id.getColumnIndex("subject"); //DEBUG
                            int index_body = cursor_check_sms_id.getColumnIndex("body");
                            int index_servicecenter = cursor_check_sms_id.getColumnIndex("service_center"); //DEBUG
                            int index_locked = cursor_check_sms_id.getColumnIndex("locked"); //DEBUG
                            int index_subid = cursor_check_sms_id.getColumnIndex("sub_id"); //DEBUG
                            int index_errorcode = cursor_check_sms_id.getColumnIndex("error_code"); //DEBUG
                            int index_creator = cursor_check_sms_id.getColumnIndex("creator"); //DEBUG
                            int index_seen = cursor_check_sms_id.getColumnIndex("seen"); //DEBUG
                            latest_sms_thread_id_in_inbuilt_sms_inbox = cursor_check_sms_id.getString(index_thread_id);
                            latest_sms_id_in_inbuilt_sms_inbox = cursor_check_sms_id.getString(index_id);
                            do {
                                id_inbox = cursor_check_sms_id.getString(index_id);
                                threadid_inbox = cursor_check_sms_id.getString(index_thread_id);
                                address_inbox = cursor_check_sms_id.getString(index_address);
                                person_inbox = cursor_check_sms_id.getString(index_person);
                                date_inbox = cursor_check_sms_id.getString(index_date);
                                date_sent_inbox = cursor_check_sms_id.getString(index_date_sent);
                                protocol_inbox = cursor_check_sms_id.getString(index_protocol);
                                read_inbox = cursor_check_sms_id.getString(index_read);
                                status_inbox = cursor_check_sms_id.getString(index_status);
                                type_inbox = cursor_check_sms_id.getString(index_type);
                                reply_path_present_inbox = cursor_check_sms_id.getString(index_replypathpresent);
                                subject_inbox = cursor_check_sms_id.getString(index_subject);
                                body_inbox = cursor_check_sms_id.getString(index_body);
                                service_center_inbox = cursor_check_sms_id.getString(index_servicecenter);
                                locked_inbox = cursor_check_sms_id.getString(index_locked);
                                sub_id_inbox = cursor_check_sms_id.getString(index_subid);
                                error_code_inbox = cursor_check_sms_id.getString(index_errorcode);
                                creator_inbox = cursor_check_sms_id.getString(index_creator);
                                seen_inbox = cursor_check_sms_id.getString(index_seen);

                                item_ids_inbox.add(id_inbox);
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: id_inbox = " + id_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: threadid_inbox = " + threadid_inbox);
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  address_inbox = " + address_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  person_inbox = " + person_inbox);
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  date_inbox = " + date_inbox);
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  date_sent_inbox = " + date_sent_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  protocol_inbox = " + protocol_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  read_inbox = " + read_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  status_inbox = " + status_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  type_inbox = " + type_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  reply_path_present_inbox " + reply_path_present_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  subject_inbox = " + subject_inbox);
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: body_inbox = " + body_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  service_center_inbox = " + service_center_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: locked_inbox = " + locked_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  sub_id_inbox = " + sub_id_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  error_code_inbox = " + error_code_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  creator_inbox = " + creator_inbox);
//                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  seen_inbox = " + seen_inbox);
                            } while (cursor_check_sms_id.moveToNext());
                        } else {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  inbuilt sms/inbox empty! ");
                            smsinbox_is_empty = true;
                        }
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: ");
//                    case TABLE_HAM:
//                        //do nothing for now
//                        break;
//
//                    case TABLE_SPAM:
//                        //do nothing for now
//                        break;
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: item_ids_tableall.size() = " + item_coressinboxids_tableall.size());
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: item_ids_inbox.size() = " + item_ids_inbox.size());


//---------------------------------------------------------------
                        //get missing ids in tableall (present in smsinbox but absent in tableall)
                        //checking if all the  ids present in item_ids_inbox are also present in item_ids_tableall
                        //if not, the ids present in items_ids_inbox but not in items_ids_tableall , will be added to list missing_item_ids
                        // check if TABLE_ALL has all messages that are present in SMS/INBOX
                        // any message that is present in SMS/INBOX but not present in TABLE_ALL should be put in missing_ids_tableall;
                        missing_item_coressinboxids_in_tableall.clear();
                        ListIterator iterator_item_ids_inbox = item_ids_inbox.listIterator();
                        String currentlistitem_item_ids_inbox;
                        if (!iterator_item_ids_inbox.hasNext() || smsinbox_is_empty) {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: List item_ids_inbox is empty");
                        } else {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                            if (tableall_is_empty) {
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS TABLE_ALL is completely empty, so mark all IDs in SMS/INBOX as missing in TABLE_ALL");
                                //add IDs that are present in sms/inbox are missing in table_all
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS Adding all ids from item_ids_inbox to missing_item_ids_in_tableall");
                                missing_item_coressinboxids_in_tableall.addAll(item_ids_inbox);
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS printing missing_item_ids_in_tablell: ");
                                ListIterator iterator_missing_item_ids_tableall = missing_item_coressinboxids_in_tableall.listIterator();
                                if (!iterator_missing_item_ids_tableall.hasNext()) {
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS missing_item_ids_in_tableall is empty!");
                                } else {
                                    do {
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS : " + iterator_missing_item_ids_tableall.next());
                                    } while (iterator_missing_item_ids_tableall.hasNext());
                                }
                            } else {
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: first element in item_ids_tableall "
                                        + item_coressinboxids_tableall.get(0));
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: finding missing IDs in item_ids_tableall " +
                                        " by comparing to each ID in item_ids_inbox");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                                for (int i = 0; i < item_ids_inbox.size(); i++) {
                                    currentlistitem_item_ids_inbox = iterator_item_ids_inbox.next().toString();
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: iterator_item_ids_inbox.next().toString() = " + currentlistitem_item_ids_inbox);
                                    try {
                                        if (item_coressinboxids_tableall.contains(currentlistitem_item_ids_inbox)) {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: present in item_coressinboxids_tableall");
                                        } else {
                                            //adding ids from smsinbox into corressinboxids list only if not present in the list
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: not present in item_corressinboxids_tableall, thus adding " + currentlistitem_item_ids_inbox + " to missing_item_ids_in_tableall");
                                            missing_item_coressinboxids_in_tableall.add(currentlistitem_item_ids_inbox);
                                        }
                                    } catch (Exception e) {
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: exception while item_ids_tableall.contains(currentlistitem_item_ids_inbox) :  " + e);
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage() case TASK_GET_MISSING_IDS: ");
                        if (missing_item_coressinboxids_in_tableall.isEmpty()) {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): missing_item_corresinboxids_in_tableall is EMPTY!");
                            table_all_sync_inbox = true;
                        } else {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): missing_item_coressinboxids_in_tableall is  NOT EMPTY!");
                            table_all_sync_inbox = false;
                        }
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");

//------------------------------------------------------------------------------


                        //get missing ids in smsinbox (present in tableall but absent in smsinbox)
                        // check if CONTENT_SMS_INBOX has all messages that are present in TABLE_ALL
                        // any message that is present in TABLE_ALL but not present in SMS/INBOX should be put in missing_ids_in_smsinbox;
                        // the missing_ids_in_smsinbox has the _ids of TABLE_ALL where corress_inbox_id != SPAM && corress_inbox_id != UNCLASSIFIED (possible only after prediction)
                        missing_item_ids_in_smsinbox.clear();
                        ListIterator iterator_item_ids_tableall = item_coressinboxids_tableall.listIterator();

                        String currentlistitem_item_coressinboxids_tableall;
                        if (!iterator_item_ids_tableall.hasNext() || tableall_is_empty) {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: List item_ids_tableall is empty");
                        } else {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                            if (smsinbox_is_empty) {
//                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): TABLE_CONTENT_SMS_INBOX is completely empty, so mark all IDs in TABLE_ALL as missing in TABLE_CONTENT_SMS_INBOX");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): TABLE_CONTENT_SMS_INBOX is completely empty");
                                //add IDs that are present in table_all are missing in sms_inbox, we don't handle such situation rn
//                                    missing_item_ids_in_smsinbox.addAll(item_ids_tableall);
                            }
//                                else {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: first element in item_ids_smsinbox "
                                    + item_ids_inbox.get(0).toString());
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: finding missing IDs in item_ids_inbox " +
                                    " by comparing to each ID in item_ids_tableall");
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");

                            boolean result_bool_spam = true; //result from http response

                            for (int i = 0; i < item_coressinboxids_tableall.size(); i++) {
                                try {
                                    currentlistitem_item_coressinboxids_tableall = iterator_item_ids_tableall.next().toString();
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: iterator_item_ids_tableall.next().toString() = " + currentlistitem_item_coressinboxids_tableall);
                                    try {
                                        if (item_ids_inbox.contains(currentlistitem_item_coressinboxids_tableall)) {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: present in items_ids_inbox");
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: --------------");
                                        } else { //only add not spam and not unclassified tagged messages
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: not present in items_ids_inbox");
                                            boolean spam = currentlistitem_item_coressinboxids_tableall.toString().equals(SPAM.toString());
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): checking if SPAM : " + spam);
                                            if (!spam) { // since not present in items_ids_inbox and not spam, means we are looking for corress_inbox_id == UNCLASSIFIED
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS:  ----------");
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): not spam, meaning message is unclassified. Hence first decide whether spam or ham.");

                                                // first process them, segregate whether spam or not, them put in CONTENTSMSINBOX
//                                                    -----------------------
//                                                    here comes the http request to backend server with the message as data
//                                                    -----------------------

                                                //for now we will statically set them as SPAM or HAM
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): result_bool_spam : " + result_bool_spam);
                                                if (!result_bool_spam) { //add if result is false i.e not spam
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): result says not spam, thus adding to missing_item_ids_in_smsinbox");
                                                    int indexof_currentcoressinboxids_tableall = item_coressinboxids_tableall.indexOf(currentlistitem_item_coressinboxids_tableall.toString());
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): index of currentlisitem_item_coressinboxids_tableall in item_coressinboxids_tableall : " + indexof_currentcoressinboxids_tableall);
                                                    String coressponsing_id_tableall = item_ids_tableall.get(indexof_currentcoressinboxids_tableall);
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): adding coressponding_id_tableall : " + coressponsing_id_tableall + " to missing_ids_in_smsinbox");
                                                    missing_item_ids_in_smsinbox.add(coressponsing_id_tableall);
                                                } else {
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): result says spam, so not adding to missing items ids smsinbox");
                                                }
                                                //this will get removed once we perform http request
                                                result_bool_spam = !result_bool_spam; //since we dont know which ones are actually spam, we will set alternate messages as spam
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): setting result_bool_spam to " + result_bool_spam);
                                            } else {
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): spam , so not adding to missing item ids in smsinbox");
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: exception while item_ids_inbox.contains(currentlistitem_item_coressinboxids_tableall) :  " + e);
                                    }
//                                    }
                                } catch (NullPointerException np) {
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): corressinboxid == null, will be skipped");
                                }
                            }
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage() case TASK_GET_MISSING_IDS: ");
                            if (missing_item_ids_in_smsinbox.isEmpty()) {
                                inbox_sync_tableall = true;
                            } else {
                                inbox_sync_tableall = false;
                            }
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: inbox_sync_tableall = " + inbox_sync_tableall);
                            DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX = true;

                            if (DONE_TASK_GET_MISSING_IDS_IN_SMSINBOX && DONE_TASK_GET_MISSING_IDS_IN_TABLEALL)
                                DONE_TASK_GET_MISSING_IDS = true;


//-------------------------------------------------------------------------------------------------------------------

                            //update the missing ids in table_all , by comparing to contentsmsinbox
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_UPDATE_MISSING_IDS: inside case TASK_UPDATE_MISSING_IDS");
                            db = db_helper.getWritableDatabase();
                            if (!table_all_sync_inbox) {
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_UPDATE_MISSING_IDS: TABLE_ALL  is not in sync  with sms/inbox ! Hence update db TABLE_ALL with new messages");
                                // update the TABLE_ALL according to SMS/INBOX
                                final String TAG_updateMissingValuesInDbTable = " updateMissingValuesInDbTable(): ";
                                String date_str = "";
                                long milli_seconds = 0;
                                Calendar calendar = Calendar.getInstance();
                                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
                                String printable_date;
                                List missing_item_ids = missing_item_coressinboxids_in_tableall;
//                                ContentResolver content_resolver = MainActivity.instance().getContentResolver();
                                content_resolver = MainActivity.instance().getContentResolver();
                                String[] projection_sms_inbox = null;
                                String selection_sms_inbox = null;
                                String[] selection_args_sms_inbox = null;
                                String sort_order_sms_inbox = " date DESC ";
                                Cursor sms_inbox_cursor = content_resolver.query(Uri.parse("content://sms/inbox"), projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
                                int index_id = sms_inbox_cursor.getColumnIndex("_id");
                                int index_address = sms_inbox_cursor.getColumnIndex("address");
                                int index_person = sms_inbox_cursor.getColumnIndex("person"); //DEBUG
                                int index_date = sms_inbox_cursor.getColumnIndex("date");
                                int index_date_sent = sms_inbox_cursor.getColumnIndexOrThrow("date_sent");
                                int index_protocol = sms_inbox_cursor.getColumnIndex("protocol"); //DEBUG
                                int index_read = sms_inbox_cursor.getColumnIndex("read"); //DEBUG
                                int index_status = sms_inbox_cursor.getColumnIndex("status"); //DEBUG
                                int index_type = sms_inbox_cursor.getColumnIndex("type"); //DEBUG
                                int index_replypathpresent = sms_inbox_cursor.getColumnIndex("reply_path_present"); //DEBUG
                                int index_subject = sms_inbox_cursor.getColumnIndex("subject"); //DEBUG
                                int index_body = sms_inbox_cursor.getColumnIndex("body");
                                int index_servicecenter = sms_inbox_cursor.getColumnIndex("service_center"); //DEBUG
                                int index_locked = sms_inbox_cursor.getColumnIndex("locked"); //DEBUG
                                int index_subid = sms_inbox_cursor.getColumnIndex("sub_id"); //DEBUG
                                int index_errorcode = sms_inbox_cursor.getColumnIndex("error_code"); //DEBUG
                                int index_creator = sms_inbox_cursor.getColumnIndex("creator"); //DEBUG
                                int index_seen = sms_inbox_cursor.getColumnIndex("seen"); //DEBUG
                                if (index_body < 0 || !sms_inbox_cursor.moveToFirst()) {
                                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " sms/inbox empty!");
                                    smsinbox_is_empty = true;
                                } else {
                                    smsinbox_is_empty = false;
                                    date_str = sms_inbox_cursor.getString(index_date);
                                    milli_seconds = Long.parseLong(date_str);
                                    calendar.setTimeInMillis(milli_seconds);
                                    printable_date = formatter.format(calendar.getTime());
                                    do {
                                        String address = sms_inbox_cursor.getString(index_address); //actual phone number
                                        String contact_name = MainActivity.getContactName(MainActivity.instance(), address); //contact name retirved from phonelookup
                                        String corress_inbox_id = sms_inbox_cursor.getString(index_id);
                                        Log.d(TAG, TAG_updateMissingValuesInDbTable + "getContactName() returns = " + contact_name);
                                        String sms_body = sms_inbox_cursor.getString(index_body);
                                        date_str = sms_inbox_cursor.getString(index_date);
                                        String date_sent = sms_inbox_cursor.getString(index_date_sent);
                                        ContentValues values = new ContentValues();
                                        Log.d(TAG, TAG_updateMissingValuesInDbTable + " case TASK_UPDATE_MISSING_IDS: checking if sms is already present, by comaprin _ID of sms/inbox and corres_inbox_id of TABLE_ALL");
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_UPDATE_MISSING_IDS all missing_item_ids_in_tableall : ");
                                        ListIterator iterator_missing_item_ids_tableall = missing_item_coressinboxids_in_tableall.listIterator();
                                        if (!iterator_missing_item_ids_tableall.hasNext()) {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS missing_item_ids_in_tableall is empty!");
                                        } else {
                                            do {
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS : " + iterator_missing_item_ids_tableall.next());
                                            } while (iterator_missing_item_ids_tableall.hasNext());
                                        }
                                        Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: missing_item_ids_in_tableall.contains('" + corress_inbox_id + "') = " + missing_item_coressinboxids_in_tableall.contains(corress_inbox_id));
                                        Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: missing_item_ids_in_tableall.indexOf('" + corress_inbox_id + "') = " + missing_item_coressinboxids_in_tableall.indexOf(corress_inbox_id));
                                        //insert only the messages which are not already present in the TABLE_ALL i.e insert only the new sms i.e sms which which new _ID
                                        if (missing_item_coressinboxids_in_tableall.contains(corress_inbox_id)) {
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + " case TASK_UPDATE_MISSING_IDS:  new sms confirmed!    _ID = " + corress_inbox_id);
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: inserting value of corress_inbox_id = " + corress_inbox_id + " into COLUMN_CORRESS_INBOX_ID ");
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: inserting value of address = " + address + " into COLUMN_SMS_ADDRESS");
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS, address); //insert value contact_name into COLUMN_SMS_ADDRESS
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: inserting value of sms_body = " + sms_body + " into COLUMN_SMS_BODY");
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY, sms_body);  // insert value sms_body in COLUMN_SMS_BODY
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: inserting value of date_str = " + date_str + " into COLUMN_SMS_EPOCH_DATE");
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: value of date_sent = " + date_sent);
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE, date_str);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT, date_sent);
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, UNCLASSIFIED); //put UNCLASSIFIED for now, will update when classified
                                            // Insert the new row, returning the primary key value of the new row
                                            db.beginTransaction();
                                            long newRowId = db.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, null, values);
                                            db.setTransactionSuccessful();
                                            db.endTransaction();
                                            if (newRowId == -1) {
                                                Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: insert into TABLE_ALL failed\n\n");
                                            } else {
                                                Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: Insert into TABLE_ALL Complete! returned newRowId = " + newRowId);
                                                Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: ");
                                            }
                                        } else {
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: not a new sms. Hence skipping insertion ");
                                        }
                                    } while (sms_inbox_cursor.moveToNext());


                                    Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: Done inserting values in TABLE_ALL! \n");
                                    Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: ");
                                    Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: ");
                                }
                            } else {
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage():  case TASK_UPDATE_MISSING_IDS: ");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage():  case TASK_UPDATE_MISSING_IDS: TABLE_ALL is in sync with SMS/INBOX. So no need to update  TABLE_ALL ");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage():  case TASK_UPDATE_MISSING_IDS: ");
                            }

//-------------------------------------
//
                            //updating missing ids in contentsmsinbox, by comparing to table_all
                            // only put those with corress_inboxid != SPAM OR coress_inboxid ==UNCLASSIFIED   (already done)
                            if (!inbox_sync_tableall) {
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_UPDATE_MISSING_IDS: contentsmsinbox  is not in sync  with non-spam in TABLE_ALL ! Hence update db CONTENTSMSINBOX with new messages");
                                // update the CONTENTSMSINBOX according to TABLE_ALL
                                String date_str = "";
                                long milli_seconds = 0;
                                Calendar calendar = Calendar.getInstance();
                                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
                                String printable_date;
                                content_resolver = MainActivity.instance().getContentResolver();
                                //everything in missing_item_ids_in_smsinbox is non-spam, so add to contentsmsinbox
//                          missing_item_ids_in_smsinbox has all the _ids of TABLEALL where the coressinboxid is non-spam (after filtering)
//                          item_ids_inbox has all the _ids of contentsmsinbox, first item in this list is the largest _id
                                //we will keep this to check whether upcoming insertion into contetnsmsinbox was successfull
                                String latest_inboxid = item_ids_inbox.get(0).toString();
                                //for contentsmsinbox
                                String[] projection_sms_inbox = null;
                                String selection_sms_inbox = null;
                                String[] selection_args_sms_inbox = null;
                                String sort_order_sms_inbox = " _id DESC ";
                                Uri uri = Uri.parse("content://sms/inbox");
                                ContentValues values = new ContentValues();
                                values.clear();
                                ContentResolver contentResolver = MainActivity.instance().getContentResolver();
                                Cursor sms_inbox_cursor;
                                //first print missing_item_ids_in_smsinbox
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): printing missing_item_ids_in_smsinbox");
                                //missing_item_ids_in_smsinbox has all the tableall IDs which are not spam
                                for (int i = 0; i < missing_item_ids_in_smsinbox.size(); i++) {
                                    String current_id_tableall = missing_item_ids_in_smsinbox.get(i);
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): " + i + ": " + current_id_tableall);
                                    //for tableall
                                    String[] columns = {SpamBusterContract.TABLE_ALL._ID,
                                            SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
                                            SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS,
                                            SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY,
                                            SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE,
                                            SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT,
                                            SpamBusterContract.TABLE_ALL.COLUMN_SPAM};
                                    String selection = SpamBusterContract.TABLE_ALL._ID + "=?";
                                    String[] selection_args1 = {current_id_tableall};
                                    //get the sms in tableall where _id = current_id_tableall
                                    Cursor cursor = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME, columns, selection, selection_args1, null, null, null);
                                    int indexofid = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID);
                                    int indexofcoressinboxid = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID);
                                    int indexofaddress = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS);
                                    int indexofbody = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY);
                                    int indexofdate = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE);
                                    int indexofdatesent = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT);
                                    int indexofspam = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SPAM);
                                    if (cursor.moveToFirst()) {
                                        do {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): found the record with _id = " + current_id_tableall);
                                            String id = cursor.getString(indexofid);
                                            String coressinboxid = cursor.getString(indexofcoressinboxid); //will be -7 which means unclassified, but we found out that it is not spam.
                                            String address = cursor.getString(indexofaddress);
                                            String body = cursor.getString(indexofbody);
                                            String date = cursor.getString(indexofdate);
                                            String datesent = cursor.getString(indexofdatesent);
                                            String spam_str = cursor.getString(indexofspam);
                                            //although we know it is going to be only HAM
                                            if (spam_str.equals(HAM)) {
                                                spam_str = "HAM";
                                            } else if (spam_str.equals(SPAM)) {
                                                spam_str = "SPAM";
                                            } else {
                                                spam_str = "UNCLASSIFIED";
                                            }
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): corressinboxid = " + coressinboxid); //update it after inserting the message into contentsmsinbox
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): body = " + body);
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): address = " + address);
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): date = " + date);
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): date_sent = " + datesent);
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): spam = " + spam_str);

                                            //preparing to insert into contentsmsinbox
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): preparing to insert into contentsmsinbox");
                                            values.put("address", address);
                                            values.put("person", "2");
                                            values.put("date", date);
                                            values.put("date_sent", datesent);
                                            values.put("body", body);
                                            contentResolver.insert(uri, values);
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): done. checking if insertion was successfull");
                                            sms_inbox_cursor = contentResolver.query(uri, projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
                                            int index_id = sms_inbox_cursor.getColumnIndex("_id");
                                            if (sms_inbox_cursor.moveToFirst()) {
                                                String corress_inbox_id = sms_inbox_cursor.getString(index_id);
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): new latest _id in contentsmsinbox is " + corress_inbox_id);
                                                if (Integer.parseInt(corress_inbox_id) > Integer.parseInt(latest_inboxid)) {
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): insertion successfull!");
                                                    latest_inboxid = corress_inbox_id;
                                                } else {
                                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): insertion failed!");
                                                }
                                                sms_inbox_cursor.close();
                                            }

                                            //now update the TABLE_ALL value at _id = id with new corressinboxid = latest_inboxid and column_spam as HAM;
                                            values.clear();
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, latest_inboxid);
                                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, HAM);
                                            String whereclause = SpamBusterContract.TABLE_ALL._ID + "=?";
                                            String[] whereargs = {id};
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): updating coressinboxid in tableall at _id = " + id);
                                            db.beginTransaction();
                                            int result = db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, whereclause, whereargs);
                                            db.setTransactionSuccessful();
                                            db.endTransaction();
                                            if (result != -1) {
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): update successfull");
                                            } else {
                                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): update unsuccessfull");
                                            }
                                            item_ids_inbox.add(0, latest_inboxid);
                                        } while (cursor.moveToNext());
                                    } else {
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): reading from TABLE_ALL unsuccessfull");
                                    }
                                    cursor.close();
                                }


                            }
                            DONE_TASK_UPDATE_MISSING_IDS = true;
//                            break; // end of case TASK_UPDATE_MISSING_IDS
                        }
                        db.close();
//                        Toast.makeText(MainActivity.instance(), "Trying to make toast from TableAllSyncInboxHandlerThread", Toast.LENGTH_LONG);
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): printing Mainactivity.instance(): " + MainActivity.instance());
                        break; //break from case TASK_SYNCTABLES
                }
            }

            ;
        };
    }

        public Handler getHandler () {
            return handler;
        }
}

