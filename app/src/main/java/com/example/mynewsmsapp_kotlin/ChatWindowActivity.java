package com.example.mynewsmsapp_kotlin;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.HAM;
import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.SPAM;

public class ChatWindowActivity extends AppCompatActivity {
    public static final String TAG = "[MY_DEBUG]";
    public ArrayList<String> sms_messages_list = new ArrayList<>();
    protected ChatWindowSmsAdapter chatWindowSmsAdapter;
    protected RecyclerView messages_chatwindow;
    private Handler main_handler = new Handler();
    protected static ArrayList<String> messages_list = new ArrayList<>();
    public static Map<Integer, String> hashmap_indexofmessage_to_tableallid_ChatWindowActivity = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatwindow);
        messages_chatwindow = (RecyclerView) findViewById(R.id.messages_chatwindow);
        TextView heading = (TextView) findViewById(R.id.heading);
        String address = getIntent().getStringExtra("address");
        Log.d(TAG, "ChatWindowActivity: onCreate(): address received through extras : " + address);
        heading.setText(MainActivity.getContactName(this, address));

        sms_messages_list.add(0, "dummy");
        chatWindowSmsAdapter = new ChatWindowSmsAdapter(this, sms_messages_list);
        messages_chatwindow.setAdapter(chatWindowSmsAdapter);
        messages_chatwindow.setLayoutManager(new LinearLayoutManager(this));
        SQLiteDatabase sqLiteDatabase = new SpamBusterdbHelper(this).getReadableDatabase();
        LoadMessagesRunnable loadMessagesRunnable = new LoadMessagesRunnable(ChatWindowActivity.this, sqLiteDatabase, address);
        new Thread(loadMessagesRunnable).start();
    }

    private static class LoadMessagesRunnable implements Runnable {
        private Handler handler;
        private WeakReference<ChatWindowActivity> activityWeakReference;
        private SQLiteDatabase db1;
        private Cursor cursor;
        private String address;
        private Calendar calendar = Calendar.getInstance();
        private DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");

        LoadMessagesRunnable(ChatWindowActivity activity, SQLiteDatabase db, String address) {
            handler = activity.main_handler;
            activityWeakReference = new WeakReference<ChatWindowActivity>(activity);
            this.db1 = db;
            this.address = address;
        }

        String getStrippedAddress(){
            if(address.length() > 10) {
                address = address.substring(address.length() - 10, address.length());
                Log.d(TAG, "GetPersonsHandlerThread: handleMessage(): Stripped number = " + address);
            }
            return address;
        }

        @Override
        public void run() {
            final ChatWindowActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            //fill the messages_list
            fillMessagesList(activity);
            Log.d(TAG, "LoadMessagesRunnable: run(): Displaying all items in messages_list : ");
            for (int i=0; i<messages_list.size(); i++){
                Log.d(TAG, "LoadMessagesRunnable: run(): [" + i + "] : " + messages_list.get(i));
            }

            //insert into adapter which can be access only from the main thread, so use main thread handler
                try {
                    //pass the Runnable to MainThread handler because UI elements(sms_adapter) are on MainThread
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            int j = 0;
                            while (j < messages_list.size()) {
                                activity.chatWindowSmsAdapter.insert(j, messages_list.get(j).toString());
                                j++;
                            }
                        }
                    });
                }
                catch (Exception e) {
                    Log.d(TAG, "LoadMessagesRunnable: run(): Exception :" + e);
                }
            }

        //will fill the messages_list
        private void fillMessagesList(ChatWindowActivity activity){
            String[] projection = {
                    BaseColumns._ID,
                    SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE,
                    SpamBusterContract.TABLE_ALL.COLUMN_SPAM
            };
            String selection = SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS + " LIKE '%" + address + "%'";
//            String[] selectionArgs = {"\'%" + address + "\'"};
            String[] selectionArgs = null;
// How you want the results sorted in the resulting Cursor
            String sortOrder =
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " desc ";   //latest one appears on top of array_adapter
            cursor = db1.query(
                    SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                    projection,             // The array of columns to return (pass null to get all)
                    selection,              // The columns for the WHERE clause
                    selectionArgs,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sortOrder               // The sort order
            );
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "LoadMessagesRunnable: run(): TABLE ALL empty!");
            } else {
                int index_id = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID);
                int index_corres_id = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID);
                int index_sms_body = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY);
                int index_sms_address = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS);
                int index_sms_epoch_date = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE);
                int index_spam = cursor.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SPAM);
                try {
                    messages_list.clear();
                    do {
                        long itemId = cursor.getLong(index_id);
                        String corress_inbox_id = cursor.getString(index_corres_id);
                        String sms_body = cursor.getString(index_sms_body);
                        String sms_address = cursor.getString(index_sms_address);
                        String epoch_date = cursor.getString(index_sms_epoch_date);
                        String spam_str = cursor.getString(index_spam);
                        if (spam_str.equals(HAM)){
                            spam_str = "HAM";
                        }
                        else if (spam_str.equals(SPAM)){
                            spam_str = "SPAM";
                        }
                        else{
                            spam_str = "UNCLASSIFIED";
                        }
                        Long milli_seconds = Long.parseLong(epoch_date);
                        calendar.setTimeInMillis(milli_seconds);
                        String printable_date = formatter.format(calendar.getTime());
                        @SuppressLint("DefaultLocale")
                        String str = String.format("itemID = %d\ncorressinboxid: %s\nSender: %s\nReceived at: %s\nMessage: %s,\nSpam: %s\n",
                                itemId, corress_inbox_id, MainActivity.getContactName(activity, sms_address), printable_date,
                                sms_body, spam_str);
//                        String str = String.format("ItemID = " + itemId + "\ncorress_inbox_id = " +
//                                corress_inbox_id + "\n SMS From: " + MainActivity.getContactName(activity, sms_address) +
//                                "\n Recieved at: " + printable_date + "\n" + sms_body);
                        Log.d(TAG, "LoadMessagesRunnable: run(): Adding message " + str.substring(0, 10) + " to messages_list");
                        //messages_list.size() gives the next index which is about to be filled
                        hashmap_indexofmessage_to_tableallid_ChatWindowActivity.put(messages_list.size(), String.valueOf(itemId));
                        messages_list.add(str);
                    } while (cursor.moveToNext());
                } catch (Exception e) {
                    Log.d(TAG, "LoadMessagesRunnable: run(): Exception : " + e);
                }
            }
        }
    }
}