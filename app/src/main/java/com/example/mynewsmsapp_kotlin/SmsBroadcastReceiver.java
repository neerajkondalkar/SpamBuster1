package com.example.mynewsmsapp_kotlin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private String TAG = "[MY_DEBUG]" + SmsBroadcastReceiver.class.getSimpleName();
    public static final String SMS_BUNDLE = "pdus";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onReceive(Context context, Intent intent) {
        final String TAG_onReceive = " onReceive(): ";
        String action_sms_received = "android.provider.Telephony.SMS_RECEIVED";
        String intent_getAction = intent.getAction().toString();
        Log.d(TAG, TAG_onReceive + "Action that made the callback is " + intent_getAction);

        //only take action on SMS_RECEIVE, so as to remove the double printing of each new sms because of SMS_DELIVER and SMS_RECEIVE
        if (intent.getAction().toString().matches(action_sms_received)) {

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

                    Log.d(TAG, TAG_onReceive + "sms[i] = " + sms[i]);

                    //get the format (extra value) which is of String type
                    String format = intent_extras.getString("format");
                    Log.d(TAG, TAG_onReceive + "format = " + format);

                    //create an sms from raw pdu using the format given during the callback by the intent
                    SmsMessage sms_message = SmsMessage.createFromPdu((byte[]) sms[i], format);
                    Log.d(TAG, TAG_onReceive + " sms_message = " + sms_message);

                    //toString() is redundant
                    String sms_body = sms_message.getMessageBody().toString();
                    Log.d(TAG, TAG_onReceive + "sms_body = " + sms_body);

                    //toString() is redundant
                    String address = sms_message.getOriginatingAddress().toString();
                    Log.d(TAG, TAG_onReceive + "address = " + address);

                    sms_message_str += "SMS from: " + MainActivity.getContactName(context, sender_number) + "\n";
                    sms_message_str += sms_body + "\n";
                    Log.d(TAG, TAG_onReceive + "sms_message_str = " + sms_message_str);

                    sender_number = address; // for Toast.makeText()   to show sender number or name
                }


                Toast.makeText(context, "Message received from + " + MainActivity.getContactName(context, sender_number), Toast.LENGTH_SHORT).show();

                //if there is alread yan instance of MainActivy then don't create an instance
                if (MainActivity.active) {
                    Log.d(TAG, TAG_onReceive + "MainActivity.active = " + MainActivity.active);
                    //calling MainActivity.updateInbox() with the same instance as MainActivity's current instance
                    MainActivity inst = MainActivity.instance();

                    //to update the current array adapter view so that the index 0 of list view will show the latest sms received
                    inst.updateInbox(sms_message_str);
                }
                //if no instance of MainActivity is running, then create an instance using intent
                else {
                    Log.d(TAG, TAG_onReceive + "MainActivity.active = " + MainActivity.active);
                    Intent i = new Intent(context, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // indicate android that new activity is to be launched in a new task stack.
                    //we also used android:launchMode="singleInstance" in Manifest so that there is only one instance of MainActivity at any given time.
                    context.startActivity(i);
                }
//                context.sendBroadcast(intent); //no need to forward the broadcast to other messaging apps as all apps receive this,
                                                // but phone's inbuilt sms app ignores this when it itself isn't the default sms app
                                                //so inbuilt app won't receive new sms when our app is default
            }
        }
    }
}
