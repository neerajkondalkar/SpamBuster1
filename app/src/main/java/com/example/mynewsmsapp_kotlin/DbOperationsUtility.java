package com.example.mynewsmsapp_kotlin;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

//this class needs to be singleton class
public class DbOperationsUtility {
    private static final String TAG = "[MY_DEBUG]";
    private static DbOperationsUtility dbOperationsUtility = new DbOperationsUtility();
    private static ContentResolver contentResolver;
    private static ContentValues values = new ContentValues();
    private static Uri uri = Uri.parse("content://sms/inbox");
    private DbOperationsUtility(){

    }
    public static DbOperationsUtility getInstance(){
        return dbOperationsUtility;
    }

    public String getLatestSmsInboxId(){
        contentResolver = MainActivity.instance().getContentResolver();
        String[] projection_sms_inbox = null;
        String selection_sms_inbox = null;
        String[] selection_args_sms_inbox = null;
        String sort_order_sms_inbox = " _id DESC ";
        Cursor sms_inbox_cursor = contentResolver.query(uri, projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
        String latest_inbox_id = null;
        int index_id = sms_inbox_cursor.getColumnIndex("_id");
        if (sms_inbox_cursor.moveToFirst()) {
            latest_inbox_id = sms_inbox_cursor.getString(index_id);
            Log.d(TAG, "DbOperationsUtility: latest _id in content://sms/inbox is " + latest_inbox_id);
            sms_inbox_cursor.close();
        }
        return latest_inbox_id;
    }

    //returns latest SMSINBOX id after insertion
    public String insertIntoSmsInbox(MySmsMessage mySmsMessage) throws InsertionFailedException {
        values.clear();
        values.put("address", mySmsMessage.getAddress());
        values.put("person", "2");
        values.put("date", mySmsMessage.getDate());
        values.put("date_sent", mySmsMessage.getDatesent());
        values.put("body", mySmsMessage.getBody());
        Log.d(TAG, "NewSmsMessageRunnable: run(): inserting the new message in content://sms/inbox");
        String old_topid = getLatestSmsInboxId();
        contentResolver.insert(uri, values);
        String latest_topid = getLatestSmsInboxId();
        if(Integer.parseInt(latest_topid) > Integer.parseInt(old_topid)){
            return latest_topid;
        }
        else{
            throw new InsertionFailedException();
        }
    }

    public String getCorressInboxIdFromTableAll(SQLiteOpenHelper db_helper, String tableallid){
        Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): reading corressinboxid from TABLEALL for tableallid: " + tableallid);
        SQLiteDatabase db = db_helper.getReadableDatabase();
        String[] projection_id = {
                SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
        };
        String selection_id = SpamBusterContract.TABLE_ALL._ID + " =?";
        String[] selection_args = {tableallid};
        String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
        Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                projection_id,             // The array of columns to return (pass null to get all)
                selection_id,              // The columns for the WHERE clause
                selection_args,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sort_order               // The sort order
        );
        String temp_corres_inbox_id_holder = null;
        if (!cursor_read_id.moveToFirst()) {
            Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): could not retrieve corressinboxid for tableallid: " + tableallid);
        }
        else {
            temp_corres_inbox_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
        }
        cursor_read_id.close();
        db.close();
        return temp_corres_inbox_id_holder;
    }

    public Set<String> getAllCorressInboxIdsFromTableAll(SQLiteOpenHelper db_helper){
        Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): reading corressinboxids from TABLEALL");
        Set<String> set_corressinboxids = new HashSet<>();
        SQLiteDatabase db = db_helper.getReadableDatabase();
        String[] projection_id = {
                SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
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
            Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): TABLEALL is empty");
        }
        else {
            do {
                String temp_corres_inbox_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
                set_corressinboxids.add(temp_corres_inbox_id_holder);
            } while (cursor_read_id.moveToNext());
        }
        cursor_read_id.close();
        db.close();
        return set_corressinboxids;
    }

    public Set<String> getInboxIdsfromSmsInbox(SQLiteOpenHelper db_helper){
        //3. Get all IDs from SMSINBOX and put them in hashset_smsinbox_id.
        Set<String> hashset_smsinbox_id = new HashSet<>();
        Log.d(TAG, "DbOperationsUtility: getInboxIdsfromSmsInbox(): reading IDs from SMSINBOX");
        ContentResolver content_resolver = MainActivity.instance().getContentResolver();
        String[] projection = {
                "_id"
        };
        Cursor cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), projection, null, null, "date DESC");
        if(!cursor_check_sms_id.moveToFirst()){
            Log.d(TAG, "DbOperationsUtility: getInboxIdsfromSmsInbox(): SMSINBOX is empty");
        }
        else{
            int index_inboxid = cursor_check_sms_id.getColumnIndex("_id");
            do{
                String temp_smsinboxid_holder = cursor_check_sms_id.getString(index_inboxid);
                hashset_smsinbox_id.add(temp_smsinboxid_holder);
            }while(cursor_check_sms_id.moveToNext());
        }
        cursor_check_sms_id.close();
        return hashset_smsinbox_id;
    }
}
