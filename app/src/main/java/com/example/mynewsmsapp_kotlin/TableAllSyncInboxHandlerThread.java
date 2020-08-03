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
import android.os.Process;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;

import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_ALL;
import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_CONTENT_SMS_INBOX;
import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_INBOX;
import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_SPAM;
import static com.example.mynewsmsapp_kotlin.MainActivity.table_all_sync_inbox;

public class TableAllSyncInboxHandlerThread  extends HandlerThread {
    public static final int TASK_GET_IDS = 1;
    public static boolean DONE_TASK_GET_IDS_TABLEALL = false;
    public static boolean DONE_TASK_GET_IDS_SMSINBOX = false;
    public static final int TASK_GET_MISSING_IDS = 2;
    public static boolean DONE_TASK_GET_MISSING_IDS = false;
    public static final int TASK_UPDATE_MISSING_IDS = 3;
    public static boolean DONE_TASK_UPDATE_MISSING_IDS = false;
    public static final int DUMMY_VAL = 9999;

    private final String TAG = "[MY_DEBUG]";
    private Handler handler;
    private SpamBusterdbHelper db_helper;
    private SQLiteDatabase db;

    private List item_ids_inbox = new ArrayList();
    private List item_ids_tableall = new ArrayList();
    private List missing_item_ids_in_tableall = new ArrayList();

    public TableAllSyncInboxHandlerThread(SpamBusterdbHelper db_helper){
        super("TableAllSyncHandlerThread");
        this.db_helper = db_helper;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): A Message received !");
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): what = " + msg.what);
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): arg1 = " + msg.arg1);
                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): arg2 = " + msg.arg2);
                switch (msg.what){
                    case TASK_GET_IDS:
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS : Inside case TASK_GET_IDS");
                        switch (msg.arg1) { //select TABLE to operate on    msg.arg1 = TABLE

                            case TABLE_ALL:
                                db = db_helper.getReadableDatabase();
                                db.beginTransaction();
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_ALL:        |");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_ALL:        |__ TABLE_ALL");
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
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_ALL: TABLE_ALL is empty! ");
                                } else {
                                    do {
                                        String temp_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID));
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_ALL: _id = " + temp_id_holder);
                                        String temp_corres_inbox_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_ALL: corress_inbox_id = " + temp_corres_inbox_id_holder);
                                        item_ids_tableall.add(temp_corres_inbox_id_holder);
                                    } while (cursor_read_id.moveToNext());
                                    // topmost is largest/latest _ID
                                }
//                                item_ids_tableall = item_ids;
                                DONE_TASK_GET_IDS_TABLEALL = true;
                                db.setTransactionSuccessful();
                                db.endTransaction();
                                break;  // end of inner case TABLE_ALL   [ still inside case  TASK_GET_IDS ]

                            case TABLE_INBOX:
                                //do nothing for now
                                break;

                            case TABLE_SPAM:
                                //do nothing for now
                                break;

                            case TABLE_CONTENT_SMS_INBOX:
                                db = db_helper.getReadableDatabase();
                                db.beginTransaction();
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: | ");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: |_ TABLE_CONTENT_SMS_INBOX ");
                                ContentResolver content_resolver = MainActivity.instance().getContentResolver();
                                Cursor cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), null, null, null, "_id DESC");
                                String latest_sms_id_in_inbuilt_sms_inbox = "";
                                String latest_sms_thread_id_in_inbuilt_sms_inbox = "";
                                String id_inbox = "";
                                String threadid_inbox = "";
                                String address_inbox = "";
                                String body_inbox = "";
                                if (cursor_check_sms_id.moveToFirst()) {
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: dumping the whole sms/inbox :");
                                    int index_id = cursor_check_sms_id.getColumnIndex("_id");
                                    int index_thread_id = cursor_check_sms_id.getColumnIndex("thread_id");
                                    int index_address = cursor_check_sms_id.getColumnIndex("address");
                                    int index_body = cursor_check_sms_id.getColumnIndex("body");
                                    latest_sms_thread_id_in_inbuilt_sms_inbox = cursor_check_sms_id.getString(index_thread_id);
                                    latest_sms_id_in_inbuilt_sms_inbox = cursor_check_sms_id.getString(index_id);
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  latest_sms_id_in_inbuilt_sms_inbox = " + latest_sms_id_in_inbuilt_sms_inbox);
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: latest_sms_thread_id_in_inbuilt_sms_inbox = " + latest_sms_thread_id_in_inbuilt_sms_inbox);
                                    do {
                                        id_inbox = cursor_check_sms_id.getString(index_id);
                                        threadid_inbox = cursor_check_sms_id.getString(index_thread_id);
                                        address_inbox = cursor_check_sms_id.getString(index_address);
                                        body_inbox = cursor_check_sms_id.getString(index_body);
                                        item_ids_inbox.add(id_inbox);
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: id_inbox = " + id_inbox);
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: threadid_inbox = " + threadid_inbox);
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  address_inbox = " + address_inbox);
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: body_inbox = " + body_inbox);
                                    } while (cursor_check_sms_id.moveToNext());
                                } else {
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX:  inbuilt sms/inbox empty! ");
                                }
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case TABLE_CONTENT_SMS_INBOX: ");
                                DONE_TASK_GET_IDS_SMSINBOX = true;
                                db.setTransactionSuccessful();
                                db.endTransaction();
                                break;   // end of inner case TABLE_CONTENT_SMS_INBOX    [ still inside case  TASK_GET_IDS ]

                            default:
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: case default: invalid table selection");
                        }
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: item_ids_tableall.size() = " + item_ids_tableall.size());
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_IDS: item_ids_inbox.size() = " + item_ids_inbox.size());
                        break;  //  end of case TASK_GET_IDS

//                -----------------------------------------------------------------------------------------------------------------------

                    //checking if all the  ids present in item_ids_inbox are also present in item_ids_tableall
                    //if not, the ids present in items_ids_inbox but not in items_ids_tableall , will be added to list missing_item_ids
                    case TASK_GET_MISSING_IDS:
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: inside case TASK_GET_MISSING_IDS");
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
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: List item_ids_inbox is empty");
                            } else {
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: first element in item_ids_tableall "
                                   + item_ids_tableall.get(0).toString());
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: iterating in item_ids_inbox");
                                Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                                for (int i = 0; i < item_ids_inbox.size(); i++) {
                                    current_list_item_ids_inbox = iterator_item_ids_inbox.next().toString();
                                    Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: iterator_item_ids_inbox.next().toString() = " + current_list_item_ids_inbox);
                                    try {
                                        if (item_ids_tableall.contains(current_list_item_ids_inbox)) {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: present in items_ids_tableall");
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: --------------");
                                        } else {
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: not present in items_ids_tableall");
                                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS:  ----------");
                                            missing_item_ids_in_tableall.add(current_list_item_ids_inbox);
                                        }
                                    } catch (Exception e) {
                                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: exception while item_ids_tableall.contains(current_list_item_ids_inbox) :  " + e);
                                    }
                                }
                            }
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage() case TASK_GET_MISSING_IDS: ");
                            if (missing_item_ids_in_tableall.isEmpty()) {
                                table_all_sync_inbox = true;
                            } else {
                                table_all_sync_inbox = false;
                            }
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_GET_MISSING_IDS: ");
                        }
                        DONE_TASK_GET_MISSING_IDS = true;
                        break;  //  end of case TASK_GET_MISSING_IDS

//                   -------------------------------------------------------------------------------------------------------------------

                    case TASK_UPDATE_MISSING_IDS:
                        Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_UPDATE_MISSING_IDS: inside case TASK_UPDATE_MISSING_IDS");
                        db = db_helper.getWritableDatabase();
                        db.beginTransaction();
                        if (!table_all_sync_inbox) {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage(): case TASK_UPDATE_MISSING_IDS: TABLE_ALL  is not in sync  with sms/inbox ! Hence update db TABLE_ALL with new messages");
                            // update the TABLE_ALL according to SMS/INBOX
                            int table1 = msg.arg1;
                            int table2 = msg.arg2;
                            final String TAG_updateMissingValuesInDbTable = " updateMissingValuesInDbTable(): ";
                            Log.d(TAG, TAG_updateMissingValuesInDbTable + " case TASK_UPDATE_MISSING_IDS: called ");
                            String date_str = "";
                            long milli_seconds = 0;
                            Calendar calendar = Calendar.getInstance();
                            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
                            String printable_date;
                            if (table1 == TABLE_ALL && table2 == TABLE_CONTENT_SMS_INBOX) {
                                List missing_item_ids = missing_item_ids_in_tableall;
                                ContentResolver content_resolver = MainActivity.instance().getContentResolver();
                                String[] projection_sms_inbox = null;
                                String selection_sms_inbox = null;
                                String[] selection_args_sms_inbox = null;
                                String sort_order_sms_inbox = " _id DESC ";
                                Cursor sms_inbox_cursor = content_resolver.query(Uri.parse("content://sms/inbox"), projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
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
                                int index_id = sms_inbox_cursor.getColumnIndex("_id");
                                int index_body = sms_inbox_cursor.getColumnIndex("body");
                                int index_date = sms_inbox_cursor.getColumnIndex("date");
                                int index_date_sent = sms_inbox_cursor.getColumnIndexOrThrow("date_sent");
                                int index_address = sms_inbox_cursor.getColumnIndex("address");
                                if (index_body < 0 || !sms_inbox_cursor.moveToFirst()) {
                                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " sms/inbox empty!");
                                    return;
                                }
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
                                    // Create a new map of values, where column names are the keys
                                    ContentValues values = new ContentValues();
                                    Log.d(TAG, TAG_updateMissingValuesInDbTable + " case TASK_UPDATE_MISSING_IDS: checking if sms is already present, by comaprin _ID of sms/inbox and corres_inbox_id of TABLE_ALL");
                                    Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: missing_item_ids.contains(corress_inbox_id) =  " + missing_item_ids.contains(corress_inbox_id));
                                    //insert only the messages which are not already present in the TABLE_ALL i.e insert only the new sms i.e sms which which new _ID
                                    if (missing_item_ids.contains(corress_inbox_id)) {
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
                                        // Insert the new row, returning the primary key value of the new row
                                        long newRowId = db.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, null, values);
                                        if (newRowId == -1) {
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: insert failed\n\n");
                                        } else {
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: Insert Complete! returned newRowId = " + newRowId);
                                            Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: ");
                                        }
                                    } else {
                                        Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: not a new sms. Hence skipping insertion ");
                                    }
                                } while (sms_inbox_cursor.moveToNext());
                                Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: Done inserting values! \n");
                                Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: ");
                                Log.d(TAG, TAG_updateMissingValuesInDbTable + "  case TASK_UPDATE_MISSING_IDS: ");
                            }
                            //end of inserting into db
                        } else {
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage():  case TASK_UPDATE_MISSING_IDS: ");
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage():  case TASK_UPDATE_MISSING_IDS: TABLE_ALL is in sync with SMS/INBOX. So no need to update  TABLE_ALL ");
                            Log.d(TAG, "TableAllSyncInboxHandlerThread: handleMessage():  case TASK_UPDATE_MISSING_IDS: ");
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();
                        DONE_TASK_UPDATE_MISSING_IDS = true;
                        break; // end of case TASK_UPDATE_MISSING_IDS
                }
                db.close();
            }
        };
    }


    public Handler getHandler(){
        return handler;
    }
}
