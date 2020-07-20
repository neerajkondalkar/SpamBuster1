package com.example.mynewsmsapp_kotlin;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class SpamBusterContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private SpamBusterContract() {}

    /* Inner class that defines the table contents */
    public static class TABLE_ALL implements BaseColumns {
        public static final String TABLE_NAME = "table_all";
        public static final String COLUMN_CORRES_INBOX_ID = "corres_inbox_id"; //corresponding _id in sms/inbox
        public static final String COLUMN_SMS_BODY = "column_body";
        public static final String COLUMN_SMS_ADDRESS = "column_address";
        public static final String COLUMN_SMS_EPOCH_DATE = "epoch_date";
    }


}

