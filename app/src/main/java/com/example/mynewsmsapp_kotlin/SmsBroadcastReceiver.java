package com.example.mynewsmsapp_kotlin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.annotation.RequiresApi;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    public static final String SMS_BUNDLE = "pdus";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onReceive(Context context, Intent intent){
        Bundle intent_extras = intent.getExtras();

        if(intent_extras != null){
            Object[] sms = (Object[]) intent_extras.get(SMS_BUNDLE);
            String sms_message_str = "";
            for (int i=0; i<sms.length; i++){
                String format = intent_extras.getString("format");
                SmsMessage sms_message = SmsMessage.createFromPdu((byte[]) sms[i], format);

                String sms_body = sms_message.getMessageBody().toString();
                String address = sms_message.getOriginatingAddress();

                sms_message_str += "SMS from: " + address + "\n";
                sms_message_str += sms_body + "\n";
            }

            MainActivity inst = MainActivity.instance();
            inst.updateInbox(sms_message_str);
        }
    }
}
