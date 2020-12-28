package com.example.mynewsmsapp_kotlin;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;
import androidx.annotation.RequiresApi;
import java.lang.ref.WeakReference;

public class NewSmsMessageRunnable implements Runnable{
    private final String TAG = " [MY_DEBUG] ";
    private WeakReference<MainActivity> activityWeakReference;
    public String sms_body;
    public String address;
    public String date_sent;
    public String date;
    public boolean spam;  //very important field. In future this will be changed after returning result from server

    private SpamBusterdbHelper spamBusterdbHelper;
    private SQLiteDatabase db;
    NewSmsMessageRunnable(MainActivity activity) {
        activityWeakReference = new WeakReference<MainActivity>(activity);
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        MainActivity activity = activityWeakReference.get();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        this.spamBusterdbHelper = new SpamBusterdbHelper(activity);
        db = spamBusterdbHelper.getWritableDatabase();
        String corress_inbox_id = "";
        ContentValues values = new ContentValues();
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS, address); //insert value contact_name into COLUMN_SMS_ADDRESS
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY, sms_body);  // insert value sms_body in COLUMN_SMS_BODY
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE, date);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT, date_sent);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
        Log.d(TAG, "NewSmsMessageRunnable: run(): Inserting the new message in TABLE_ALL...");
        db.beginTransaction();
        long newRowId_tableall = db.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, null, values);
        db.endTransaction();
        if (newRowId_tableall == -1) {
            Log.d(TAG, "NewSmsMessageRunnable: run(): insert failed!");
        } else {
            Log.d(TAG,  "  Insert Complete! returned newRowId_tableall = " + newRowId_tableall);
        }

        //insert in contentsmsminbox only if not spam
        if (this.spam == false) {
            Log.d(TAG, "NewSmsMessageRunnable: run(): getting inbox _id of latest sms from content://sms/inbox");
            String[] projection_sms_inbox = null;
            String selection_sms_inbox = null;
            String[] selection_args_sms_inbox = null;
            String sort_order_sms_inbox = " _id DESC ";
            Uri uri = Uri.parse("content://sms/inbox");
            ContentResolver contentResolver = activity.getContentResolver();
            Cursor sms_inbox_cursor = contentResolver.query(uri, projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
            String latest_inbox_id = "";
            int index_id = sms_inbox_cursor.getColumnIndex("_id");
            if(sms_inbox_cursor.moveToFirst()){
                latest_inbox_id = sms_inbox_cursor.getString(index_id);
                Log.d(TAG, "NewSmsMessageRunnable: run(): latest _id in content://sms/inbox is " + latest_inbox_id);
//                sms_inbox_cursor.close();
            }

            values.clear();
            values.put("address", address);
            values.put("person", "2");
            values.put("date", date);
            values.put("date_sent", date_sent);
            values.put("body", sms_body);
            Log.d(TAG, "NewSmsMessageRunnable: run(): inserting the new message in content://sms/inbox");
            contentResolver.insert(uri, values);
            Log.d(TAG, "NewSmsMessageRunnable: run(): (hopefully) insertion is done, let's check by comparing previous latest _id and new _id");

            //get ID of latest sms inserted in contentsmsinbox to update the corressinboxid column of the same message in table_all
            Log.d(TAG, "NewSmsMessageRunnable: run(): getting inbox _id of latest sms from content://sms/inbox");

            sms_inbox_cursor.moveToFirst();
            if(sms_inbox_cursor.moveToFirst()) {
                corress_inbox_id = sms_inbox_cursor.getString(index_id);
                Log.d(TAG, "NewSmsMessageRunnable: run(): latest _id in content://sms/inbox is " + corress_inbox_id);
                Log.d(TAG, "NewSmsMessageRunnable: run(): comparing latest_inbox_id and corress_inbox_id...");
                if (Integer.parseInt(corress_inbox_id) > Integer.parseInt(latest_inbox_id)) {
                    Log.d(TAG, "NewSmsMessageRunnable: run(): corress_inbox_id > latest_inbox_id");
                    Log.d(TAG, "NewSmsMessageRunnable: run(): This means, new SMS successfully inserted in content://sms/inbox");
                }
                else{
                    Log.d(TAG, "NewSmsMessageRunnable: run(): insertion in content://sms/inbox failed.");
                    Log.d(TAG, "NewSmsMessageRunnable: run(): This could be because the SpamBusters app isn't set to default SMS app." +
                            " In that case it is fine because we don't wan't duplicates in contentsmsinbox anyways.");
                }

                sms_inbox_cursor.close();

                //now insert the same message in table_ham
                values.clear();
                values.put(SpamBusterContract.TABLE_HAM.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
                values.put(SpamBusterContract.TABLE_HAM.COLUMN_SMS_ADDRESS, address); //insert value contact_name into COLUMN_SMS_ADDRESS
                values.put(SpamBusterContract.TABLE_HAM.COLUMN_SMS_BODY, sms_body);  // insert value sms_body in COLUMN_SMS_BODY
                values.put(SpamBusterContract.TABLE_HAM.COLUMN_SMS_EPOCH_DATE, date);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
                values.put(SpamBusterContract.TABLE_HAM.COLUMN_SMS_EPOCH_DATE_SENT, date_sent);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
                Log.d(TAG, "NewSmsMessageRunnable: run(): Inserting the new message in TABLE_HAM...");
                db.beginTransaction();
                long newRowId_tableham = db.insert(SpamBusterContract.TABLE_HAM.TABLE_NAME, null, values);
                db.endTransaction();
                if (newRowId_tableham == -1) {
                    Log.d(TAG, "NewSmsMessageRunnable: run(): insert failed!");
                } else {
                    Log.d(TAG, "  Insert Complete! returned newRowId = " + newRowId_tableham);
                }

                //and now update the corress_inbox_id of that message in table_all;
                values.clear();
                values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
                String newRowId_tableall_str = Long.toString(newRowId_tableall);
                db.beginTransaction();
                db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, " _id = ?", new String[]{newRowId_tableall_str});
                db.endTransaction();
                db.close();
            }

        }
    }
}
