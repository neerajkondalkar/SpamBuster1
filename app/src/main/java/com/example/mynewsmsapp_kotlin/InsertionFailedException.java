package com.example.mynewsmsapp_kotlin;

import android.util.Log;

import static com.example.mynewsmsapp_kotlin.GetPersonsHandlerThread.TAG;

class InsertionFailedException extends Exception{
    InsertionFailedException(){
        Log.d(TAG, "InsertionFailed: InsertionFailed(): failed to insert message in SmsInbox");
    }
}
