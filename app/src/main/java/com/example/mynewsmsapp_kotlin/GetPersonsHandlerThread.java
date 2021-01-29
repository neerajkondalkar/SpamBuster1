package com.example.mynewsmsapp_kotlin;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_ALL;
import static com.example.mynewsmsapp_kotlin.MainActivity.persons_list;

public class GetPersonsHandlerThread extends HandlerThread {
    public static final String TAG = "[MY_DEBUG] ";
    public static final int TASK_GET_PERSONS = 21;
    private Handler handler;
    private SpamBusterdbHelper db_helper;
    private SQLiteDatabase db;
    public GetPersonsHandlerThread(SpamBusterdbHelper spamBusterdbHelper) {
        super("GetPersonsHandlerThread");
        this.db_helper = spamBusterdbHelper;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case TASK_GET_PERSONS:
                        if (msg.arg1 == TABLE_ALL) {                // EDIT
                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Received message : ");
                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): msg.what = TASK_GET_PERSONS");
                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): msg.arg1 = TABLE_ALL");
                            db = db_helper.getReadableDatabase();

                            //fill persons_list with all the persons
                            persons_list.clear();
                            Cursor cursor;
                            String[] projection = {
                                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS,
                            };
                            String selection = null;
                            String[] selectionArgs = null;
                            String groupBy = SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS;
                            String sortOrder =
                                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " desc ";   //latest one appears on top of array_adapter
                            db.beginTransaction();
                            cursor = db.query(
                                    SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                    projection,             // The array of columns to return (pass null to get all)
                                    selection,              // The columns for the WHERE clause
                                    selectionArgs,          // The values for the WHERE clause
                                    groupBy,                   // don't group the rows
                                    null,                   // don't filter by row groups
                                    sortOrder               // The sort order
                            );
                            if(!cursor.moveToFirst()){
                                Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): No messages in Table ALL");
                            }
                            else{
                                Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): reading output of GROUP BY address ");
                                int index_sms_address = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS);
                                do{
                                    String current_address = cursor.getString(index_sms_address);
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): " + current_address);
                                    Pattern pattern = Pattern.compile("^\\+\\d{1,3}\\d{10}", Pattern.CASE_INSENSITIVE);
                                    Matcher matcher = pattern.matcher(current_address);
                                    boolean matchFound = matcher.find();
                                    if(matchFound) {
                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number starts with +91");
                                        //strip the first few digits based on length of string
                                    } else {
                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number does not start with +91");
                                    }
                                    Pattern pattern1 = Pattern.compile("^\\d{1,3}\\d{10}", Pattern.CASE_INSENSITIVE);
                                    matcher = pattern1.matcher(current_address);
                                    matchFound = matcher.find();
                                    if(matchFound) {
                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number starts with 91");
                                    } else {
                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number does not start with 91");
                                    }
                                    if(current_address.length() > 10) {
                                        current_address = current_address.substring(current_address.length() - 10, current_address.length());
                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Stripped number = " + current_address);
                                    }
                                    if(!persons_list.contains(current_address)) {
                                        persons_list.add(current_address);
                                    }
                                }while (cursor.moveToNext());
                                Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Total addresses found : ");
                                for (int i=0; i<persons_list.size(); i++){
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): " + persons_list.get(i));
                                }
                            }
                            break;
                        }
                }
            }
        };
    }

    public Handler getHandler(){
        return handler;
    }
}
