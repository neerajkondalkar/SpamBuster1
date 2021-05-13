package com.example.mynewsmsapp_kotlin;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

//this class needs to be singleton class
public class SmsInboxUtility {
    private static final String TAG = "[MY_DEBUG]";
    private static SmsInboxUtility smsInboxUtility = new SmsInboxUtility();
    private static ContentResolver contentResolver;
    private static ContentValues values = new ContentValues();
    private static Uri uri = Uri.parse("content://sms/inbox");
    private SmsInboxUtility(){

    }
    public static SmsInboxUtility getInstance(){
        return smsInboxUtility;
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
            Log.d(TAG, "SmsInboxUtility: latest _id in content://sms/inbox is " + latest_inbox_id);
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
}
