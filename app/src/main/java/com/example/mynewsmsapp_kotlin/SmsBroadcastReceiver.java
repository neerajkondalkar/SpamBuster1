package com.example.mynewsmsapp_kotlin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private String TAG = "[MY_DEBUG]" + SmsBroadcastReceiver.class.getSimpleName();
    public static final String SMS_BUNDLE = "pdus";
    public long date;
    public long date_sent;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {
        final String TAG_onReceive = " onReceive(): ";
        final String action_sms_received = "android.provider.Telephony.SMS_RECEIVED";
 //       final String action_sms_deliver = "android.provider.Telephony.SMS_DELIVER";
        SmsMessage sms_message = null;   // sms_message will be passsed to the MainActivity.updateInbox()
        Log.d(TAG, " onReceive(): Telephony.Sms.Inbox.CONTENT_URI   returns  " + Telephony.Sms.Inbox.CONTENT_URI);
        String intent_getAction = intent.getAction().toString();
        Log.d(TAG, TAG_onReceive + "Action that made the callback is " + intent_getAction);

        //only take action on SMS_RECEIVE, so as to remove the double printing of each new sms because of SMS_DELIVER and SMS_RECEIVE
        if (intent.getAction().toString().matches(action_sms_received)) {
   //     if (intent.getAction().toString().matches(action_sms_deliver)) {

            //since the SMS_RECEIVED_ACTION intent makes a callback with Context and Intent
            // and gives the pdus - which is what the sms message is these days SMS PDU mode - and format - which can be either "3gpp" or "3gpp2" - as extra values
            //so to get the handle to all the extras
            Bundle intent_extras = intent.getExtras();

            if (intent_extras != null) {
                //get only the raw pdus i.e the SMS for now, later we will also extract the 'format' value
                Object[] sms = (Object[]) intent_extras.get(SMS_BUNDLE);
                Log.d(TAG, TAG_onReceive + "Value of sms = " + sms + " which is of type " + sms.getClass());

                String sms_message_str = "";
                String sender_number = ""; //for Toad.makeText()   to show sender number or name

                Log.d(TAG, TAG_onReceive + " sms_message_str = " + sms_message_str);
                for (int i = 0; i < sms.length; i++) {
                    Log.d(TAG, TAG_onReceive + "sms_message_str = " + sms_message_str);
                    Log.d(TAG, TAG_onReceive + "i = " + i);

                    Log.d(TAG, TAG_onReceive + "sms[i] = " + sms[i].toString());

//                    Log.d(TAG, TAG_onReceive + " All fields in intent_extras:  ");


                    //get the format (extra value) which is of String type
                    String format = intent_extras.getString("format");
                    Log.d(TAG, TAG_onReceive + "format = " + format);

                    //create an sms from raw pdu using the format given during the callback by the intent
                    sms_message = SmsMessage.createFromPdu((byte[]) sms[i], format);
                    Log.d(TAG, TAG_onReceive + " sms_message = " + sms_message);

                    //toString() is redundant
                    String sms_body = sms_message.getMessageBody().toString();
                    Log.d(TAG, TAG_onReceive + "sms_body = " + sms_body);

                    //toString() is redundant
                    String address = sms_message.getOriginatingAddress().toString();
                    Log.d(TAG, TAG_onReceive + "address = " + address);

                    long timestampMillis = sms_message.getTimestampMillis();
                    Log.d(TAG, TAG_onReceive + " timestampmillis = " + timestampMillis);

                    int protocol_id = sms_message.getProtocolIdentifier();
                    Log.d(TAG, "SmsBroadcastReceiver: onReceive(): protocol identifier = " + Integer.toString(protocol_id));

                    Calendar calendar = Calendar.getInstance();
                    DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy h:mm a");
                    String printable_date = "";
                    calendar.setTimeInMillis(timestampMillis);
                    printable_date = formatter.format(calendar.getTime());
                    sms_message_str += "SMS from: " + MainActivity.getContactName(context, address) + "\n";
                    sms_message_str += "Received at : " + printable_date + "\n";
                    sms_message_str += sms_body;
                    Log.d(TAG, TAG_onReceive + "sms_message_str = " + sms_message_str);
                    sender_number = address; // for Toast.makeText()   to show sender number or name
                    //timestampMillis is the date_sent to be saved in new column epoch_date_sent
                    // currentTimeinMillis goes in epoch_date column
                    //long currentTimeinMillis = System.currentTimeMillis();
                    //Log.d(TAG, "SmsBroadcastReceiver: onReceive(): currentTimeinMillis = " + currentTimeinMillis);
                    //date_sent = timestampMillis;
                    //date = currentTimeinMillis;
                }
                Toast.makeText(context, "Message received from + " + MainActivity.getContactName(context, sender_number), Toast.LENGTH_SHORT).show();

                //if there is alread yan instance of MainActivy then don't create an instance
                if (MainActivity.active) {
                    Log.d(TAG, TAG_onReceive + "MainActivity.active = " + MainActivity.active);
                    //calling MainActivity.updateInbox() with the same instance as MainActivity's current instance
                    MainActivity inst = MainActivity.instance();

                    //to update the current array adapter view so that the index 0 of list view will show the latest sms received
//                    inst.updateInbox(sms_message_str);
                    try {
                        inst.updateInbox(sms_message_str, sms_message);
                    }
                    catch(Exception e){
                        Log.d(TAG, TAG_onReceive + " exception : " + e);
                    }
                }
                //if no instance of MainActivity is running, then create an instance using intent
                else {
                    Log.d(TAG, TAG_onReceive + "MainActivity.active = " + MainActivity.active);
                    Intent i = new Intent(context, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // indicate android that new activity is to be launched in a new task stack.
                    //we also used android:launchMode="singleInstance" in Manifest so that there is only one instance of MainActivity at any given time.
                    context.startActivity(i);

                    String sms_body = sms_message.getMessageBody().toString();
                    String address = sms_message.getOriginatingAddress().toString();
                    String date_sent = Long.toString(sms_message.getTimestampMillis());
                    String date = Long.toString(System.currentTimeMillis());
                    Log.d(TAG, "MainActivity: updateInbox(): sms_body: " + sms_body);
                    Log.d(TAG, "MainActivity: updateInbox(): address: " + address);
                    Log.d(TAG, "MainActivity: updateInbox(): date_sent: " + date_sent);
                    Log.d(TAG, "MainActivity: updateInbox(): date (received): " + date);
                    SpamBusterdbHelper spamBusterdbHelper;
                    NewSmsMessageRunnable newSmsMessageRunnable;
                    if(MainActivity.active) {
                        spamBusterdbHelper = MainActivity.instance().spamBusterdbHelper;
                        newSmsMessageRunnable = new NewSmsMessageRunnable(MainActivity.instance(), spamBusterdbHelper);
                    }
                    else {
                        spamBusterdbHelper = new SpamBusterdbHelper(context);
                        newSmsMessageRunnable = new NewSmsMessageRunnable(context, spamBusterdbHelper);
                    }
                    newSmsMessageRunnable.sms_body = sms_body;
                    newSmsMessageRunnable.address = address;
                    newSmsMessageRunnable.date_sent = date_sent;
                    newSmsMessageRunnable.date = date;
                    newSmsMessageRunnable.message_is_spam = true;//very important field. In future this will be changed after returning result from server
                    new Thread(newSmsMessageRunnable).start();
                }
//                context.sendBroadcast(intent); //no need to forward the broadcast to other messaging apps as all apps receive this SMS_RECEIVED,
                                                // but phone's inbuilt sms app ignores this when it itself isn't the default sms app
                                                //so inbuilt app won't receive new sms when our app is default
            }
        }
    }

}
