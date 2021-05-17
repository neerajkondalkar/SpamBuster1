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
import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_HAM;
import static com.example.mynewsmsapp_kotlin.MainActivity.TABLE_SPAM;
import static com.example.mynewsmsapp_kotlin.MainActivity.all_list;
import static com.example.mynewsmsapp_kotlin.MainActivity.inbox_list;
import static com.example.mynewsmsapp_kotlin.MainActivity.persons_list;
import static com.example.mynewsmsapp_kotlin.MainActivity.spam_list;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.HAM;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.SPAM;

public class GetPersonsHandlerThread extends HandlerThread {
    public static final String TAG = "[MY_DEBUG] ";
    public static final int TASK_GET_PERSONS = 21;

    public static boolean LOADED_ALL = false; //to indicate that, if already loaded then dont query the database again
    public static boolean LOADED_SPAM = false;
    public static boolean LOADED_INBOX = false;

    private Handler handler;
    private SpamBusterdbHelper db_helper;
    private SQLiteDatabase db;
    public static boolean DONE_TASK_GETPERSONS = false;
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
                        if (msg.arg1 == TABLE_ALL) {
                            //query databse only if not already loaded
                            if (!LOADED_ALL) {
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
//                            db.beginTransaction();
                                cursor = db.query(
                                        SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                        projection,             // The array of columns to return (pass null to get all)
                                        selection,              // The columns for the WHERE clause
                                        selectionArgs,          // The values for the WHERE clause
                                        groupBy,                   // group by address
                                        null,                   // don't filter by row groups
                                        sortOrder               // The sort order
                                );
                                if (!cursor.moveToFirst()) {
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): No messages in Table ALL");
                                } else {
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): reading output of GROUP BY address ");
                                    int index_sms_address = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS);
                                    do {
                                        String current_address = cursor.getString(index_sms_address);
//                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): " + current_address);
                                        Pattern pattern = Pattern.compile("^\\+\\d{1,3}\\d{10}", Pattern.CASE_INSENSITIVE);
                                        Matcher matcher = pattern.matcher(current_address);
                                        boolean matchFound = matcher.find();
                                        if (matchFound) {
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number starts with +91");
                                            //strip the first few digits based on length of string
                                        } else {
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number does not start with +91");
                                        }
                                        Pattern pattern1 = Pattern.compile("^\\d{1,3}\\d{10}", Pattern.CASE_INSENSITIVE);
                                        matcher = pattern1.matcher(current_address);
                                        matchFound = matcher.find();
                                        if (matchFound) {
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number starts with 91");
                                        } else {
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number does not start with 91");
                                        }
                                        if (current_address.length() > 10) {
                                            current_address = current_address.substring(current_address.length() - 10, current_address.length());
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Stripped number = " + current_address);
                                        }
                                        if (!all_list.contains(current_address)) {
                                            all_list.add(current_address);
                                        }
                                    } while (cursor.moveToNext());
                                    persons_list.clear();
                                    persons_list.addAll(all_list);
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Total addresses found : ");
                                    for (int i = 0; i < persons_list.size(); i++) {
                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): " + persons_list.get(i));
                                    }
                                }
                                LOADED_ALL = true;
                            }
                            //already loaded in all_list
                            else{
                                persons_list.clear();
                                persons_list.addAll(all_list);
                            }
                        }


                        if (msg.arg1 == TABLE_HAM) {
                            if(!LOADED_INBOX) {
                                Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Received message : ");
                                Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): msg.what = TASK_GET_PERSONS");
                                Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): msg.arg1 = TABLE_HAM");
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
//                            db.beginTransaction();
                                String havingClause = SpamBusterContract.TABLE_ALL.COLUMN_SPAM + " LIKE '" + HAM + "'";
                                cursor = db.query(
                                        SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                        projection,             // The array of columns to return (pass null to get all)
                                        selection,              // The columns for the WHERE clause
                                        selectionArgs,          // The values for the WHERE clause
                                        groupBy,
                                        havingClause,
                                        sortOrder               // The sort order
                                );
                                if (!cursor.moveToFirst()) {
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): No messages in Table ALL");
                                } else {
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): reading output of GROUP BY address ");
                                    int index_sms_address = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS);
                                    do {
                                        String current_address = cursor.getString(index_sms_address);
//                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): " + current_address);
                                        Pattern pattern = Pattern.compile("^\\+\\d{1,3}\\d{10}", Pattern.CASE_INSENSITIVE);
                                        Matcher matcher = pattern.matcher(current_address);
                                        boolean matchFound = matcher.find();
                                        if (matchFound) {
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number starts with +91");
                                            //strip the first few digits based on length of string
                                        } else {
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number does not start with +91");
                                        }
                                        Pattern pattern1 = Pattern.compile("^\\d{1,3}\\d{10}", Pattern.CASE_INSENSITIVE);
                                        matcher = pattern1.matcher(current_address);
                                        matchFound = matcher.find();
                                        //if it is a number, then crop it down to 10 digits so that both the numbers starting with +(XX) and
                                        // the ones not starting with +XX, both map to same number
                                        if (matchFound) {
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number starts with 91");
                                            if (current_address.length() > 10) {
                                                current_address = current_address.substring(current_address.length() - 10, current_address.length());
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Stripped number = " + current_address);
                                            }
                                        } else {
//                                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number does not start with 91");
                                        }
//                                        if (!inbox_list.contains(current_address) && !spam_list.contains(current_address)) {
                                        //insert if not present
                                        if(!inbox_list.contains(current_address)){
                                            inbox_list.add(current_address);
                                            //if the address was there in spam_list, then remove it from there.
                                            if(spam_list.contains(current_address)){
                                                spam_list.remove(current_address);
                                            }
                                        }
                                    } while (cursor.moveToNext());
                                    //fill persons_list with all_list because persons_list is what is going to be displayed in the adapter
                                    persons_list.addAll(inbox_list);
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Total addresses found : ");
                                    for (int i = 0; i < persons_list.size(); i++) {
                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): " + persons_list.get(i));
                                    }
                                }
                                LOADED_INBOX = true;
                            }
                            //if already loaded
                            else{
                                persons_list.clear();
                                persons_list.addAll(inbox_list);
                            }
                        }


                        if (msg.arg1 == TABLE_SPAM) {
                            if(!LOADED_SPAM){
                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Received message : ");
                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): msg.what = TASK_GET_PERSONS");
                            Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): msg.arg1 = TABLE_SPAM");
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
//                            db.beginTransaction();
                            String havingClause = SpamBusterContract.TABLE_ALL.COLUMN_SPAM + " LIKE '" + SPAM + "'";
                            cursor = db.query(
                                    SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                                    projection,             // The array of columns to return (pass null to get all)
                                    selection,              // The columns for the WHERE clause
                                    selectionArgs,          // The values for the WHERE clause
                                    groupBy,                   // don't group the rows
                                    havingClause,                   // don't filter by row groups
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
//                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): " + current_address);
                                    Pattern pattern = Pattern.compile("^\\+\\d{1,3}\\d{10}", Pattern.CASE_INSENSITIVE);
                                    Matcher matcher = pattern.matcher(current_address);
                                    boolean matchFound = matcher.find();
                                    if(matchFound) {
//                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number starts with +91");
                                        //strip the first few digits based on length of string
                                    } else {
//                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number does not start with +91");
                                    }
                                    Pattern pattern1 = Pattern.compile("^\\d{1,3}\\d{10}", Pattern.CASE_INSENSITIVE);
                                    matcher = pattern1.matcher(current_address);
                                    matchFound = matcher.find();
                                    if(matchFound) {
//                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number starts with 91");
                                    } else {
//                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): number does not start with 91");
                                    }
                                    if(current_address.length() > 10) {
                                        current_address = current_address.substring(current_address.length() - 10, current_address.length());
//                                        Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Stripped number = " + current_address);
                                    }
                                    //remove any address which has even one HAM message
                                    if(inbox_list.contains(current_address)){
                                        spam_list.remove(current_address);
                                    }
                                    if(!spam_list.contains(current_address) && !inbox_list.contains(current_address)) {
                                        spam_list.add(current_address);
//                                        inbox_list.remove(current_address);
                                    }
                                }while (cursor.moveToNext());
                                persons_list.addAll(spam_list);
                                Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Total addresses found : ");
                                for (int i=0; i<persons_list.size(); i++){
                                    Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): " + persons_list.get(i));
                                }
                            }
                            LOADED_SPAM = true;
                            }
                            //if already loaded
                            else{
                                persons_list.clear();
                                persons_list.addAll(spam_list);
                            }
                        }
                        DONE_TASK_GETPERSONS = true;
                        break;
                }
            }
        };
    }

    public Handler getHandler(){
        return handler;
    }
}
