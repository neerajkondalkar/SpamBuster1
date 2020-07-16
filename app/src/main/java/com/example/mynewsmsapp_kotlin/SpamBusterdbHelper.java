package com.example.mynewsmsapp_kotlin;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SpamBusterdbHelper  extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";

    private static final String SQL_CREATE_TABLE_ALL =
            "CREATE TABLE " + SpamBusterContract.TABLE_ALL.TABLE_NAME + " (" +
                    SpamBusterContract.TABLE_ALL._ID + " INTEGER PRIMARY KEY," +
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY + " TEXT," +
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS + " TEXT)";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SpamBusterContract.TABLE_ALL.TABLE_NAME;


    public  SpamBusterdbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_ALL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }
}
