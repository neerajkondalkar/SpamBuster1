package com.example.mynewsmsapp_kotlin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.HAM;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.SPAM;

public class MoveToRunnable implements Runnable {
    private static final String TAG = "[MY_DEBUG]";
    String id;
    String moveTo;
    SQLiteOpenHelper sqLiteOpenHelper;
    SQLiteDatabase db;
    Context context;

    MoveToRunnable(Context context, String id, String moveTo){
        this.id = id;
        this.moveTo = moveTo;
        this.context = context;
        sqLiteOpenHelper = new SpamBusterdbHelper(context);
    }

    @Override
    public void run() {
        ContentValues values = new ContentValues();
        //if we want to move it to SPAM, we just need to update TABLEALL
        //corressinboxid will remain samed
        if(moveTo.equals(SPAM)) {
            values.clear();
            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, SPAM);
            Log.d(TAG, "MoveToRunnable: run(): updating column_spam at _id '" + id + "' in table_all to " + SPAM + " [SPAM]");
            String[] whereArgs = new String[]{id};
            db = sqLiteOpenHelper.getWritableDatabase();
            db.beginTransaction();
            db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        //if we want to move it to INBOX
        else if(moveTo.equals(HAM)){
            //first insert into SMSINBOX
            //for that we first have to read the address, date, datesent and body from TABLEALL
            db = sqLiteOpenHelper.getReadableDatabase();
            values.clear();
            String[] projection_id = {
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY
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
                Log.d(TAG, "MoveToRunnable: run(): TABLEALL is empty");
            }
            else {
                String temp_address_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS));
                String temp_date_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE));
                String temp_datesent_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT));
                String temp_body_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY));

                //insert into SMSINBOX
                try {
                    String corressinboxid = SmsInboxUtility.getInstance().
                            insertIntoSmsInbox(new MySmsMessage(temp_address_holder, temp_date_holder,
                                    temp_datesent_holder, temp_body_holder));

                    //if successfull, update TABLEALL
                    values.clear();
                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corressinboxid);
                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, HAM);
                    Log.d(TAG, "MoveToRunnable: run(): updating column_spam at _id '" + id + "' in table_all to " + HAM  + " [HAM]");
                    String[] whereArgs = new String[]{id};
                    db = sqLiteOpenHelper.getWritableDatabase();
                    db.beginTransaction();
                    db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                }
                //if insertion fails, then don't update TABLEALL
                catch (InsertionFailedException e){
                    Log.d(TAG, "MoveToRunnable: run(): Insertion into SMSINBOX failed");
                    Log.d(TAG, "MoveToRunnable: run(): Move to INBOX failed!");
                    Toast.makeText(MainActivity.instance(), String.format("Message with id:%s could not be moved to INBOX", id), Toast.LENGTH_SHORT).show();
                }
            }
            cursor_read_id.close();
        }
        else{
            Log.d(TAG, "MoveToRunnable: run(): UNCLASSIFIED messages cannot be moved");
        }
        db.close();
    }
}
