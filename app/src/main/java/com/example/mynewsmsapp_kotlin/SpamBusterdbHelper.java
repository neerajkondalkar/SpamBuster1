package com.example.mynewsmsapp_kotlin;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class SpamBusterdbHelper  extends SQLiteOpenHelper {

    private  static final String TAG = "[MY_DEBUG] " + SpamBusterdbHelper.class.getSimpleName();
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SpamBuster.db";

    private static final String SQL_CREATE_TABLE_ALL =
            "CREATE TABLE IF NOT EXISTS " + SpamBusterContract.TABLE_ALL.TABLE_NAME + " (" +
                    SpamBusterContract.TABLE_ALL._ID + " INTEGER PRIMARY KEY, " +
                    SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID + " TEXT, " +
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY + " TEXT," +
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS + " TEXT, " +
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " TEXT )" ;

//    private static final String SQL_DELETE_ENTRIES =
//            "DROP TABLE IF EXISTS " + SpamBusterContract.TABLE_ALL.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + SpamBusterContract.TABLE_ALL.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES_ROWS = "DELETE FROM  " + SpamBusterContract.TABLE_ALL.TABLE_NAME;

    private static String selection_for_delete = SpamBusterContract.TABLE_ALL._ID + " LIKE ? ";
    private static String[] selection_args_for_delete = { "*" };

    public  SpamBusterdbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_CREATE_TABLE_ALL);

        //to delete database tables and entries everytime a new database is created
        int deleted_rows = db.delete(SpamBusterContract.TABLE_ALL.TABLE_NAME, selection_for_delete, selection_args_for_delete);
        db.execSQL(SQL_DELETE_ENTRIES_ROWS);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {

        super.onOpen(db);

        //for testing purposes, we will always create a new table and delete all entries just to be sure
//        db.execSQL(SQL_DELETE_ENTRIES);
//        db.execSQL(SQL_CREATE_TABLE_ALL);
//        int deleted_rows = db.delete(SpamBusterContract.TABLE_ALL.TABLE_NAME, selection_for_delete, selection_args_for_delete);
//        db.execSQL(SQL_DELETE_ENTRIES_ROWS);

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
