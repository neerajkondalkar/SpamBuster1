package com.example.mynewsmsapp_kotlin;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import java.lang.ref.WeakReference;

public class NewSmsMessageRunnable implements Runnable{
    private final String TAG = " [MY_DEBUG] ";
    private WeakReference<MainActivity> activityWeakReference;
    public String sms_body;
    public String address;
    public String date_sent;
    public String date;
    private SpamBusterdbHelper spamBusterdbHelper;
    private SQLiteDatabase db;
    NewSmsMessageRunnable(MainActivity activity) {
        activityWeakReference = new WeakReference<MainActivity>(activity);
    }
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
        long newRowId = db.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, null, values);

        if (newRowId == -1) {
            Log.d(TAG, "NewSmsMessageRunnable: run(): insert failed!");
        } else {
            Log.d(TAG,  "  Insert Complete! returned newRowId = " + newRowId);
        }
    }
}
