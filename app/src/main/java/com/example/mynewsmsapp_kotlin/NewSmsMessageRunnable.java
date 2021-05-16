package com.example.mynewsmsapp_kotlin;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import static com.example.mynewsmsapp_kotlin.MainActivity.getContactName;

//import static com.example.mynewsmsapp_kotlin.ClassificationSyncService.executor;

public class NewSmsMessageRunnable implements Runnable{
    Context context;
    private final String TAG = " [MY_DEBUG] ";
    private WeakReference<MainActivity> activityWeakReference;
    private String sms_body;
    private String address;
    private String date_sent;
    private String date;
    public static final String UNCLASSIFIED = "-7";
    public static final String HAM = "-11";
    public static final String SPAM = "-9";
    private boolean message_is_spam = true;  //very important field.
    private boolean http_req_success = false;

    private SpamBusterdbHelper spamBusterdbHelper;
    private SQLiteDatabase db;
//    NewSmsMessageRunnable(MainActivity activity, SpamBusterdbHelper spamBusterdbHelper) {
    NewSmsMessageRunnable(Context context, SpamBusterdbHelper spamBusterdbHelper, MySmsMessage mySmsMessage) {
        this.spamBusterdbHelper = spamBusterdbHelper;
        this.context = context;
        this.sms_body = mySmsMessage.getBody();
        this.address = mySmsMessage.getAddress();
        this.date = mySmsMessage.getDate();
        this.date_sent = mySmsMessage.getDatesent();
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
        String corress_inbox_id = null;
        Log.d(TAG, "NewSmsMessageRunnable: run():  ");
        ContentValues values = new ContentValues();
//        values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id); //inserting unclassified value for now
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS, address); //insert value contact_name into COLUMN_SMS_ADDRESS
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY, sms_body);  // insert value sms_body in COLUMN_SMS_BODY
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE, date);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT, date_sent);  // insert value date_str in COLUMN_SMS_EPOCH_DATE
        //if sender is in our contacts, it means their message can be trusted as HAM
        if(!getContactName(context, address).equals(address)){
            Log.d(TAG, String.format("NewSmsMessageRunnable: run(): sender '%s' is in our address book named '%s', hence declaring as HAM",
                    address, getContactName(context, address)));
            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, HAM);
            message_is_spam = false;
        }
        else {
            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, UNCLASSIFIED);
        }
        Log.d(TAG, "NewSmsMessageRunnable: run(): Inserting the new message in TABLE_ALL...");
        db.beginTransaction();
        //insert null value in corressinboxid
        long newRowId_tableall = db.insert(SpamBusterContract.TABLE_ALL.TABLE_NAME, SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, values);
        db.setTransactionSuccessful();
        db.endTransaction();
        if (newRowId_tableall == -1) {
            Log.d(TAG, "NewSmsMessageRunnable: run(): insert failed!");
        }
        else {
            Log.d(TAG, "  Insert Complete! returned newRowId_tableall = " + newRowId_tableall);

            String newRowId_tableall_str = String.valueOf(newRowId_tableall);

            this.http_req_success = false;
            Log.d(TAG, "NewSmsMessageRunnable: run(): checking for internet connection... " + new SBNetworkUtility().checkNetwork(context));
            int prediction = -1;

            //if message has the word OTP in it, then declare it as HAM
            String[] checkotpstr = sms_body.split(" ");
            for(String str : checkotpstr){
                if (str.equalsIgnoreCase("otp")){
                    http_req_success = true;
                    message_is_spam = false;
                    prediction = 0;
                    Log.d(TAG, "NewSmsMessageRunnable: run(): Message is an OTP. So declaring it as HAM right away!");
                    break;
                }
            }

            //if we have already declared the message as HAM (message from sender in our contact list), then we don't need to predict
            //by default message_is_spam is true, we only set it to false when the message is from sender from our address book
            if (new SBNetworkUtility().checkNetwork(context) && message_is_spam) {
//                prediction = makePrediction(newRowId_tablepending_str, sms_body);
                prediction = makePrediction(newRowId_tableall_str, sms_body);
            }

            if (prediction == -1) {
                http_req_success = false;
                Log.d(TAG, "NewSmsMessageRunnable: run(): API probing failed/no internet connection detected, message will only be present in TABLE ALL and NOT in TABLE_HAM or TABLE_SPAM");
            } else if (prediction == 1) {
                Log.d(TAG, "NewSmsMessageRunnable: run(): API probing successful");
                Log.d(TAG, "NewSmsMessageRunnable: run(): message is spam : " + this.message_is_spam);
                http_req_success = true;
                message_is_spam = true;
            } else if (prediction == 0) {
                http_req_success = true;
                message_is_spam = false;
            } else {
                //do nothing
            }

//        --------------
            if (this.http_req_success) {
                //insert in contentsmsminbox only if not spam
                //before inserting, first read the topmost smsinbox id : latest_inbox_id
                // latest_inbox_id will be later compared to topmost inbox id after inserting the message in smsinbox so that we know insertion is successfull
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

                            //and now update the corress_inbox_id of that message in table_all;
                            values.clear();
                            Log.d(TAG, "NewSmsMessageRunnable: run(): corress_inbox_id set to " + corress_inbox_id + "  (not spam, so valid corressinboxid)");
                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, corress_inbox_id);
                            values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, HAM);
                            db.beginTransaction();
                            Log.d(TAG, "NewSmsMessageRunnable: run(): updating corress_inbox_id at _id '" + newRowId_tableall_str + "' in table_all to " + corress_inbox_id);
                            String[] whereArgs = new String[]{newRowId_tableall_str};
                            db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
                            db.setTransactionSuccessful();
                            db.endTransaction();

                        } else {
                            Log.d(TAG, "NewSmsMessageRunnable: run(): insertion in content://sms/inbox failed, thus not updating TABLEALL for that sms");
                            Log.d(TAG, "NewSmsMessageRunnable: run(): This could be because the SpamBusters app isn't set to default SMS app." +
                                    " In that case it is fine because we don't wan't duplicates in contentsmsinbox anyways.");
                        }
                        sms_inbox_cursor.close();

                    }
                } else {//if this.message_is_spam==true
                    //and now update the corress_inbox_id of that message in table_all; change it to spam i.e -9
                    values.clear();
                    //corressinboxid will remain null
                    values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, SPAM);
                    db.beginTransaction();
                    Log.d(TAG, "NewSmsMessageRunnable: run(): updating column_spam at _id '" + newRowId_tableall_str + "' in table_all to " + SPAM);
                    String[] whereArgs = new String[]{newRowId_tableall_str};
                    db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                }

            }
//                END HERE TAG1

            //check latest corress_id, id, and column_spam in table_all if it really worked!
            SQLiteDatabase db1;
            db1 = spamBusterdbHelper.getReadableDatabase();
            db1.beginTransaction();
            String[] projection_id = {
                    SpamBusterContract.TABLE_ALL._ID,
                    SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
                    SpamBusterContract.TABLE_ALL.COLUMN_SPAM
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
                Log.d(TAG, "NewSmsMessageRunnable: run(): finally, checking if all updations are reflecting in TABLEALL:");
                String str = String.format("corressinboxid = %s,\n_id = %s,\ncolumn_spam = %s\n",
                        cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID)),
                        cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID)),
                        cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SPAM)));
                Log.d(TAG, "NewSmsMessageRunnable: run(): corress_inbox_id = " + str);
            }
            db1.setTransactionSuccessful();
            db1.endTransaction();
            db1.close();
        }

    }

    private boolean checkNetwork() {
        boolean wifiAvailable = false;
        boolean mobileAvailable = false;
        ConnectivityManager conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfo = conManager.getAllNetworkInfo();
        for (NetworkInfo netInfo : networkInfo) {
            if (netInfo.getTypeName().equalsIgnoreCase("WIFI"))
                if (netInfo.isConnected())
                    wifiAvailable = true;
            if (netInfo.getTypeName().equalsIgnoreCase("MOBILE"))
                if (netInfo.isConnected())
                    mobileAvailable = true;
        }
        return wifiAvailable || mobileAvailable;
    }




    //make prediction on this thread if possible
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private int makePrediction(String id_str, String sms_body_str){
        http_req_success = new SBNetworkUtility().checkNetwork(context);

        if(!http_req_success){
            Log.d(TAG + "[API]", "NewSmsMessageRunnable: run(): internet not available, skipping classification for now");
            return -1;
        }
        else{
            URL url = null;
            try {
                url = new URL("http://192.168.1.102:5000/predict");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection)url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                con.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }

            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");

            con.setDoOutput(true);

//            String id = "1000";
            String[] id = new String[1];
            id[0] = id_str;
//            String[] message_body = new String[5];
            String[] message_body = new String[1];
            message_body[0] = sms_body_str;
//            message_body[0] = "Hi, I am in a meeting. Will call back later.";
//            message_body[1] = "IMPORTANT - You could be entitled up to £3,160 in compensation from mis-sold PPI on a credit card or loan. Please reply PPI for info or STOP to opt out.";
//            message_body[2] = "A [redacted] loan for £950 is approved for you if you receive this SMS. 1 min verification & cash in 1 hr at www.[redacted].co.uk to opt out reply stop";
//            message_body[3] = "You have still not claimed the compensation you are due for the accident you had. To start the process please reply YES. To opt out text STOP";
//            message_body[4] = "Our records indicate your Pension is under performing to see higher growth and up to 25% cash release reply PENSION for a free review. To opt out reply STOP";
            // String number = "\"9999977777\"";
            JSONArray ja = new JSONArray();

            for(int i=0; i<message_body.length; i++){
                JSONObject jo = new JSONObject();
//                Integer idint = Integer.parseInt(id) + i;
                try {
                    jo.put("id", id[i]);
                    jo.put("message_body", message_body[i]);
                    ja.put(jo);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            JSONObject mainObj = new JSONObject();
            try {
                mainObj.put("entries", ja);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String jsonInputString = mainObj.toString();

            Log.d(TAG + "[API]", "NewSmsMessageRunnable: makePrediction(): \"Printing json main object");
            Log.d(TAG + "[API]", "NewSmsMessageRunnable: makePrediction(): " + jsonInputString);

            try(OutputStream os = con.getOutputStream()){
                byte[] input = new byte[0];
                try {
                    input = jsonInputString.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    os.write(input, 0, input.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            int code = 0;
            try {
                code = con.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): ");
            System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): HTTP POST request done.");
            System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): Response code: " + code);

            try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))){
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                // System.out.println(response.toString());
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): Complete response:");
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): " + response.toString());
                JSONObject obj = null;
                try {
                    obj = new JSONObject(response.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): length of JSON obj : " + obj.length());

                JSONArray result_ja = (JSONArray) obj.get("result");
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): Printing <JSONOArray> result_ja :" + result_ja);
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): length of result_ja : " + result_ja.length());
                System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): looping through the json array ");

                for(int i=0; i<result_ja.length(); i++){
                    JSONObject tempjo = (JSONObject) result_ja.get(i);
                    System.out.printf("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): [%d]  %s\n", i, tempjo.toString());
                    try{
                        System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): extract ID and spam prediction from the JSON object");
                        System.out.printf("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): [%d]  %s  -   %s\n", i, tempjo.get("id").toString(), tempjo.get("spam").toString());
                        if(tempjo.get("spam").toString().equals("1")){
                            return 1;
                        }
                        else if(tempjo.get("spam").toString().equals("0")){
                            return 0;
                        }
                        else{
                            return -1;
                        }

                    }
                    catch(Exception e){
                        e.printStackTrace();
                        try{
                            System.out.println("[MY_DEBUG] [API] NewSmsMessageRunnable: makePrediction(): got error : " + tempjo.get("error").toString());
                            return -1;
                        }
                        catch(Exception e1){
                            e1.printStackTrace();
                            return -1;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return  -1;
    }
}

