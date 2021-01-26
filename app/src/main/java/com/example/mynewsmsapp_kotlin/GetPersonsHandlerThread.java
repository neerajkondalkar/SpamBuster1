package com.example.mynewsmsapp_kotlin;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;

public class GetPersonsHandlerThread extends HandlerThread {
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
                switch (msg.what){
                    case TASK_GET_PERSONS:
                        if ()                // EDIT
                        db = db_helper.getReadableDatabase();
                        String sql_getpersons = "SELECT " + ;
                        break;
                }
            }


        };
    }
    public Handler getHandler(){
        return handler;
    }
}
