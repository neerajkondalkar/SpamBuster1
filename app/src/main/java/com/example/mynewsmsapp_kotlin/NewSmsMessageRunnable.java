package com.example.mynewsmsapp_kotlin;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;
import java.lang.ref.WeakReference;

public class NewSmsMessageRunnable implements Runnable{
    Context context;
    private final String TAG = " [MY_DEBUG] ";
    private WeakReference<MainActivity> activityWeakReference;
    public String sms_body;
    public String address;
    public String date_sent;
    public String date;
    public static final String UNCLASSIFIED = "-7";
    public static final String SPAM = "-9";
    public boolean message_is_spam = true;  //very important field. In future this will be changed after returning result from server
    private boolean http_req_success = false;

    private SpamBusterdbHelper spamBusterdbHelper;
    private SQLiteDatabase db;
//    NewSmsMessageRunnable(MainActivity activity, SpamBusterdbHelper spamBusterdbHelper) {
    NewSmsMessageRunnable(Context context, SpamBusterdbHelper spamBusterdbHelper) {
        this.spamBusterdbHelper = spamBusterdbHelper;
        this.context = context;
//        activityWeakReference = new WeakReference<MainActivity>(activity);
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
//        MainActivity activity = activityWeakReference.get();
//        if (activity == null || activity.isFinishing()) {
//            return;
//        }
//        this.spamBusterdbHelper = new SpamBusterdbHelper(activity);
        db = spamBusterdbHelper.getWritableDatabase();
        //first mark it as unclassified and then insert it in TABLE_ALL
        String corress_inbox_id = UNCLASSIFIED;
        Log.d(TAG, "NewSmsMessageRunnable: run():  ");
        ContentValues values = new ContentValues();
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id); //inserting unclassified value for now
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS, address); //insert value contact_name into COLUMN_SMS_ADDRESS
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY, sms_body);  // insert value sms_body in COLUMN_SMS_BODY
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE, date);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT, date_sent);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
        Log.d(TAG, "NewSmsMessageRunnable: run(): Inserting the new message in TABLE_ALL...");
        db.beginTransaction();
        long newRowId_tableall = db.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, null, values);
        db.setTransactionSuccessful();
        db.endTransaction();
        if (newRowId_tableall == -1) {
            Log.d(TAG, "NewSmsMessageRunnable: run(): insert failed!");
        } else {
            Log.d(TAG, "  Insert Complete! returned newRowId_tableall = " + newRowId_tableall);
        }
        String newRowId_tableall_str = String.valueOf(newRowId_tableall);

        //inserting this new message into table_pending since it is pending to be classified
        values.clear();
        values.put(SpamBusterContract.TABLE_PENDING.COLUMN_ID_TABLEALL, newRowId_tableall);
        Log.d(TAG, "NewSmsMessageRunnable: run(): Inserting the new message id_TABLE_ALL into TABLE_PENDING ...");
        db.beginTransaction();
        long newRowId_tablepending = db.insert(SpamBusterContract.TABLE_PENDING.TABLE_NAME, null, values);
        db.setTransactionSuccessful();
        db.endTransaction();
        if (newRowId_tablepending == -1) {
            Log.d(TAG, "NewSmsMessageRunnable: run(): insert failed!");
        } else {
            Log.d(TAG, "  Insert Complete! returned newRowId_tablepending] = " + newRowId_tablepending);
        }

//        STARTING FROM HERE TAG1
//        all the code in TAG1 should be, in future, enclose by an if() statement to check http response was sucessfull or not.
//        --------------
//         here comes the http request to check whether messages is spam  or noet http_req_success accordingly
        this.http_req_success = false;
        Log.d(TAG, "NewSmsMessageRunnable: run(): http_req_success is currently set to : " + this.http_req_success);
        if(!this.http_req_success){
            Log.d(TAG, "NewSmsMessageRunnable: run(): message will only be present in TABLE ALL and NOT in TABLE_HAM or TABLE_SPAM");
        }
        else{
            Log.d(TAG, "NewSmsMessageRunnable: run(): message will be inserted in TABLE_HAM or TABLE_SPAM accordingly");
        }
        this.message_is_spam = !this.message_is_spam;//to keep it changing alternatively
        this.message_is_spam = true;//temp code
        Log.d(TAG, "NewSmsMessageRunnable: run(): message is spam currently set to  : " + this.message_is_spam);
//        --------------
        if(this.http_req_success) {
        //insert in contentsmsminbox only if not spam
//        if (this.message_is_spam == false) {
            if (this.message_is_spam == false) {
                Log.d(TAG, "NewSmsMessageRunnable: run(): getting inbox _id of latest sms from content://sms/inbox");
                String[] projection_sms_inbox = null;
                String selection_sms_inbox = null;
                String[] selection_args_sms_inbox = null;
                String sort_order_sms_inbox = " _id DESC ";
                Uri uri = Uri.parse("content://sms/inbox");
//                ContentResolver contentResolver = activity.getContentResolver();
                ContentResolver contentResolver = context.getContentResolver();
                Cursor sms_inbox_cursor = contentResolver.query(uri, projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
                String latest_inbox_id = "";
                int index_id = sms_inbox_cursor.getColumnIndex("_id");
                if (sms_inbox_cursor.moveToFirst()) {
                    latest_inbox_id = sms_inbox_cursor.getString(index_id);
                    Log.d(TAG, "NewSmsMessageRunnable: run(): latest _id in content://sms/inbox is " + latest_inbox_id);
                    sms_inbox_cursor.close();
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

                sms_inbox_cursor = contentResolver.query(uri, projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
                index_id = sms_inbox_cursor.getColumnIndex("_id");
                if (sms_inbox_cursor.moveToFirst()) {
                    corress_inbox_id = sms_inbox_cursor.getString(index_id);
                    Log.d(TAG, "NewSmsMessageRunnable: run(): latest _id in content://sms/inbox is " + corress_inbox_id);
                    Log.d(TAG, "NewSmsMessageRunnable: run(): comparing latest_inbox_id and corress_inbox_id...");
                    if (Integer.parseInt(corress_inbox_id) > Integer.parseInt(latest_inbox_id)) {
                        Log.d(TAG, "NewSmsMessageRunnable: run(): corress_inbox_id > latest_inbox_id");
                        Log.d(TAG, "NewSmsMessageRunnable: run(): This means, new SMS successfully inserted in content://sms/inbox");
                    } else {
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
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    if (newRowId_tableham == -1) {
                        Log.d(TAG, "NewSmsMessageRunnable: run(): insert failed!");
                    } else {
                        Log.d(TAG, "  Insert Complete! returned newRowId = " + newRowId_tableham);
                    }

                    //and now update the corress_inbox_id of that message in table_all;
                    values.clear();
                    Log.d(TAG, "NewSmsMessageRunnable: run(): corress_inbox_id set to " + corress_inbox_id + "  (not spam, so valid corressinboxid)");
                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
                    db.beginTransaction();
                    Log.d(TAG, "NewSmsMessageRunnable: run(): updating corress_inbox_id at _id '" + newRowId_tableall_str + "' in table_all to " + corress_inbox_id);
                    String[] whereArgs = new String[]{newRowId_tableall_str};
                    db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                }
            } else {//if this.message_is_spam==true
                values.clear();
                corress_inbox_id = SPAM;
                Log.d(TAG, "NewSmsMessageRunnable: run(): corress_inbox_id set to " + corress_inbox_id + "  (spam)");
                values.put(SpamBusterContract.TABLE_SPAM.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
                values.put(SpamBusterContract.TABLE_SPAM.COLUMN_SMS_ADDRESS, address); //insert value contact_name into COLUMN_SMS_ADDRESS
                values.put(SpamBusterContract.TABLE_SPAM.COLUMN_SMS_BODY, sms_body);  // insert value sms_body in COLUMN_SMS_BODY
                values.put(SpamBusterContract.TABLE_SPAM.COLUMN_SMS_EPOCH_DATE, date);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
                values.put(SpamBusterContract.TABLE_SPAM.COLUMN_SMS_EPOCH_DATE_SENT, date_sent);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
                Log.d(TAG, "NewSmsMessageRunnable: run(): Inserting the new message in TABLE_SPAM...");
                db.beginTransaction();
                long newRowId_tablespam = db.insert(SpamBusterContract.TABLE_SPAM.TABLE_NAME, null, values);
                db.setTransactionSuccessful();
                db.endTransaction();
                if (newRowId_tablespam == -1) {
                    Log.d(TAG, "NewSmsMessageRunnable: run(): insert failed!");
                } else {
                    Log.d(TAG, "  Insert Complete! returned newRowId = " + newRowId_tablespam);
                }

                //and now update the corress_inbox_id of that message in table_all; change it to spam i.e -9
                values.clear();
//            values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
//                db.beginTransaction();
                Log.d(TAG, "NewSmsMessageRunnable: run(): updating corress_inbox_id at _id '" + newRowId_tableall_str + "' in table_all to " + corress_inbox_id);
                String values_str = SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID + " = " + corress_inbox_id;
//            String[] whereArgs = new String[]{newRowId_tableall_str};
                String whereClause = SpamBusterContract.TABLE_ALL._ID + " = " + newRowId_tableall_str;
                spamBusterdbHelper.updateValues(db, SpamBusterContract.TABLE_ALL.TABLE_NAME, values_str, whereClause);
//            db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values,  SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
//                db.endTransaction();
                db.close();
            }

        }
//                END HERE TAG1

        //check latest corress_id in table_all if it really worked!
            SQLiteDatabase db1;
            db1 = spamBusterdbHelper.getReadableDatabase();
            db1.beginTransaction();
            String[] projection_id = {
                    SpamBusterContract.TABLE_ALL._ID,
                    SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID
            };
            String selection_id = null;
            String[] selection_args = null;
            String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
            Cursor cursor_read_id = db1.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                    projection_id,             // The array of columns to return (pass null to get all)
                    selection_id,              // The columns for the WHERE clause
                    selection_args,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sort_order               // The sort order
            );
            if (cursor_read_id.moveToFirst()) {
                Log.d(TAG, "NewSmsMessageRunnable: run(): corress_inbox_id = " +
                        cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID))
                        + " _id = " + cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID)));
            }
            db1.setTransactionSuccessful();
            db1.endTransaction();
            db1.close();


    }
}
