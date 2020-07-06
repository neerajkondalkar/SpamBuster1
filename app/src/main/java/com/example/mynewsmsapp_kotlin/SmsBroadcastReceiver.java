package com.example.mynewsmsapp_kotlin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = SmsBroadcastReceiver.class.getSimpleName();
    public static final String SMS_BUNDLE = "pdus";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onReceive(Context context, Intent intent){
        //since the SMS_RECEIVED_ACTION intent makes a callback with Context and Intent
        // and gives the pdus - which is what the sms message is these days SMS PDU mode - and format - which can be either "3gpp" or "3gpp2" - as extra values
        //so to get the handle to all the extras
        Bundle intent_extras = intent.getExtras();

        if(intent_extras != null){
            //get only the raw pdus i.e the SMS
            Object[] sms = (Object[]) intent_extras.get(SMS_BUNDLE);
            System.out.println("[DEBUG] + " + TAG + " onReceive(): value of sms = " + sms);
            String sms_message_str = "";
            Log.d(TAG, " onReceive(): sms_message_str = " + sms_message_str);
            for (int i=0; i<sms.length; i++){
                System.out.println("[DEBUG] + " + TAG + " onReceive(): sms_message_str = " + sms_message_str);
                System.out.println("[DEBUG] + " + TAG + " onReceive(): i = " + i);

                System.out.println("[DEBUG] + " + TAG + " onReceive(): sms[i] = " + sms[i]);
                //get the format (extra value) which is of String type
                String format = intent_extras.getString("format");
                System.out.println("[DEBUG] + " + TAG + " onReceive(): format = " + format);
                //create an sms from raw pdu using the format given during the callback by the intent
                SmsMessage sms_message = SmsMessage.createFromPdu((byte[]) sms[i], format);
                System.out.println("[DEBUG] + " + TAG + " onReceive(): sms_message = " + sms_message);

                //toString() is redundant
                String sms_body = sms_message.getMessageBody().toString();
                System.out.println("[DEBUG] + " + TAG + " onReceive(): sms_body = " + sms_body);
                //toString() is redundant
                String address = sms_message.getOriginatingAddress().toString();
                System.out.println("[DEBUG] + " + TAG + " onReceive(): address = " + address);

                sms_message_str += "SMS from: " + address + "\n";
                sms_message_str += sms_body + "\n";
                Log.d(TAG, " onReceive(): sms_message_str = " + sms_message_str);
            }

            //calling MainActivity.updateInbox() with the same instance as MainActivity's current instance
            MainActivity inst = MainActivity.instance();
            inst.updateInbox(sms_message_str);
        }
    }
}
