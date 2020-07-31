package com.example.mynewsmsapp_kotlin;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_ALL;
import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_CONTENT_SMS_INBOX;
import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_INBOX;
import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_SPAM;

public class TableAllSyncInboxHandlerThread  extends HandlerThread {
    public static final int TASK_GET_IDS = 1;
    public static boolean DONE_TASK_GET_IDS = false;
    public static final int TASK_GET_MISSING_IDS = 2;
    public static boolean DONE_TASK_GET_MISSING_IDS = false;

    private final String TAG = "[MY_DEBUG]";
    private static boolean need_to_sync = true;
    private Handler handler;
    private SpamBusterdbHelper db_helper;

    private List item_ids_inbox = new ArrayList();
    private List item_ids_tableall = new ArrayList();
    private List missing_item_ids_in_tableall = new ArrayList();

    public TableAllSyncInboxHandlerThread(){
        super("TableAllSyncHandlerThread");
        this.db_helper = db_helper;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what){
                    case TASK_GET_IDS:
                        SQLiteDatabase db = db_helper.getReadableDatabase();
                        final String TAG_getAllIdsFromDbTable = " getAllIdsFromDbTable(): ";

                        switch (msg.arg1) { //select TABLE to operate on    msg.arg1 = TABLE

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
                                        item_ids_tableall.add(temp_corres_inbox_id_holder);
                                    } while (cursor_read_id.moveToNext());
                                    // topmost is largest/latest _ID
                                }
//                                item_ids_tableall = item_ids;
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
                                ContentResolver content_resolver = MainActivity.instance().getContentResolver();
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

                                        item_ids_inbox.add(id_inbox);
                                        Log.d(TAG, TAG_getAllIdsFromDbTable + " id_inbox = " + id_inbox);
                                        Log.d(TAG, TAG_getAllIdsFromDbTable + " threadid_inbox = " + threadid_inbox);
                                        Log.d(TAG, TAG_getAllIdsFromDbTable + " address_inbox = " + address_inbox);
                                        Log.d(TAG, TAG_getAllIdsFromDbTable + " body_inbox = " + body_inbox);
                                        Log.d(TAG, TAG_getAllIdsFromDbTable + "");
                                    } while (cursor_check_sms_id.moveToNext());
                                } else {
                                    Log.d(TAG, TAG_getAllIdsFromDbTable + "  inbuilt sms/inbox empty! ");
                                }
//                                item_ids_inbox = item_ids;
                                Log.d(TAG, TAG_getAllIdsFromDbTable + "");
                                break;

//                                ---------- INVALID  TABLE selected ------------

                            default:
                                Log.d(TAG, TAG_getAllIdsFromDbTable + " invalid table selection");
                        }

                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): item_ids_tableall.size() = " + item_ids_tableall.size());
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): item_ids_inbox.size() = " + item_ids_inbox.size());

                        //indicate that TASK has been completed
                        DONE_TASK_GET_IDS = true;
                        break;

                    //checking if all the  ids present in item_ids_inbox are also present in item_ids_tableall
                    //if not, the ids present in items_ids_inbox but not in items_ids_tableall , will be added to list missing_item_ids
                    case TASK_GET_MISSING_IDS:
                        // compare the lists item_ids_tableall and item_ids_inbox
                        missing_item_ids_in_tableall.clear();
                        int Table1 = msg.arg1;
                        int Table2 = msg.arg2;
                        // check if TABLE_ALL has all messages that are present in SMS/INBOX
                        // any message that is present in SMS/INBOX but not present in TABLE_ALL should be put in missing_ids_tableall;
                        if (Table1 == TABLE_ALL && Table2 == TABLE_CONTENT_SMS_INBOX) {
                            ListIterator iterator_item_ids_inbox = item_ids_inbox.listIterator();
                            String current_list_item_ids_inbox;
                            if (!iterator_item_ids_inbox.hasNext()) {
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): List item_ids_inbox is empty");
                            } else {
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): ");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): first element in item_ids_tableall "
                                   + item_ids_tableall.get(0).toString());
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): ");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): iterating in item_ids_inbox");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): ");
                                for (int i = 0; i < item_ids_inbox.size(); i++) {
                                    current_list_item_ids_inbox = iterator_item_ids_inbox.next().toString();
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): iterator_item_ids_inbox.next().toString() = " + current_list_item_ids_inbox);
                                    try {
                                        if (item_ids_tableall.contains(current_list_item_ids_inbox)) {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): present in items_ids_tableall");
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): --------------");
                                        } else {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): not present in items_ids_tableall");
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage():  ----------");
                                            missing_item_ids_in_tableall.add(current_list_item_ids_inbox);
                                        }
                                    } catch (Exception e) {
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): exception while item_ids_tableall.contains(current_list_item_ids_inbox) :  " + e);
                                    }
                                }
                            }
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): ");
                            if (missing_item_ids_in_tableall.isEmpty()) {
                                table_all_sync_inbox = true;
                            } else {
                                table_all_sync_inbox = false;
                            }
                            Log.d(TAG, TAG_refreshSmsInbox + "");
                        }
                        break;


                    case TASK_CHECK_SYNC:

                        switch (msg.arg1){

                        }

                        break;





















                }
            }
        };
    }

    public Handler getHandler(){
        return handler;
    }
}
