package com.example.mynewsmsapp_kotlin;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SpamBusterdbHelper  extends SQLiteOpenHelper {

    private  static final String TAG = "[MY_DEBUG] " + SpamBusterdbHelper.class.getSimpleName();
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SpamBuster.db";

    private static final String SQL_CREATE_TABLEALL =
            "CREATE TABLE IF NOT EXISTS " + SpamBusterContract.TABLE_ALL.TABLE_NAME + " (" +
                    SpamBusterContract.TABLE_ALL._ID + " INTEGER PRIMARY KEY, " +
                    SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID + " TEXT, " +
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY + " TEXT," +
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS + " TEXT, " +
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " TEXT )" ;

    private static final String SQL_CREATE_TABLEHAM =
            "CREATE TABLE IF NOT EXISTS " + SpamBusterContract.TABLE_HAM.TABLE_NAME + " (" +
                    SpamBusterContract.TABLE_HAM._ID + " INTEGER PRIMARY KEY, " +
                    SpamBusterContract.TABLE_HAM.COLUMN_CORRES_INBOX_ID + " TEXT, " +
                    SpamBusterContract.TABLE_HAM.COLUMN_SMS_BODY + " TEXT," +
                    SpamBusterContract.TABLE_HAM.COLUMN_SMS_ADDRESS + " TEXT, " +
                    SpamBusterContract.TABLE_HAM.COLUMN_SMS_EPOCH_DATE + " TEXT )" ;

    private static final String SQL_CREATE_TABLESPAM =
            "CREATE TABLE IF NOT EXISTS " + SpamBusterContract.TABLE_SPAM.TABLE_NAME + " (" +
                    SpamBusterContract.TABLE_SPAM._ID + " INTEGER PRIMARY KEY, " +
                    SpamBusterContract.TABLE_SPAM.COLUMN_CORRES_INBOX_ID + " TEXT, " +
                    SpamBusterContract.TABLE_SPAM.COLUMN_SMS_BODY + " TEXT," +
                    SpamBusterContract.TABLE_SPAM.COLUMN_SMS_ADDRESS + " TEXT, " +
                    SpamBusterContract.TABLE_SPAM.COLUMN_SMS_EPOCH_DATE + " TEXT )" ;

//    private static final String SQL_DELETE_ENTRIES =
//            "DROP TABLE IF EXISTS " + SpamBusterContract.TABLE_ALL.TABLE_NAME;
    private static final String SQL_DROP_TABLEALL = "DROP TABLE IF EXISTS " + SpamBusterContract.TABLE_ALL.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES_TABLEALL = "DELETE FROM  " + SpamBusterContract.TABLE_ALL.TABLE_NAME;

    private static final String SQL_DROP_TABLEHAM = "DROP TABLE IF EXISTS " + SpamBusterContract.TABLE_HAM.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES_TABLEHAM = "DELETE FROM  " + SpamBusterContract.TABLE_HAM.TABLE_NAME;

    private static final String SQL_DROP_TABLESPAM = "DROP TABLE IF EXISTS " + SpamBusterContract.TABLE_SPAM.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES_TABLESPAM = "DELETE FROM  " + SpamBusterContract.TABLE_SPAM.TABLE_NAME;

    private static String selection_for_delete_tableall = SpamBusterContract.TABLE_ALL._ID + " LIKE ? ";
    private static String[] selection_args_for_delete_tableall = { "*" };

    private static String selection_for_delete_tableham = SpamBusterContract.TABLE_HAM._ID + " LIKE ? ";
    private static String[] selection_args_for_delete_tableham = { "*" };

    private static String selection_for_delete_tablespam = SpamBusterContract.TABLE_SPAM._ID + " LIKE ? ";
    private static String[] selection_args_for_delete_tablespam = { "*" };

    public  SpamBusterdbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_DROP_TABLEALL);
        db.execSQL(SQL_CREATE_TABLEALL);
        //to delete database tables and entries everytime a new database is created
        int deleted_rows = db.delete(SpamBusterContract.TABLE_ALL.TABLE_NAME, selection_for_delete_tableall, selection_args_for_delete_tableall);
        db.execSQL(SQL_DELETE_ENTRIES_TABLEALL);

        db.execSQL(SQL_DROP_TABLEHAM);
        db.execSQL(SQL_CREATE_TABLEHAM);
        //to delete database tables and entries everytime a new database is created
        deleted_rows = db.delete(SpamBusterContract.TABLE_HAM.TABLE_NAME, selection_for_delete_tableall, selection_args_for_delete_tableall);
        db.execSQL(SQL_DELETE_ENTRIES_TABLEHAM);

        db.execSQL(SQL_DROP_TABLESPAM);
        db.execSQL(SQL_CREATE_TABLESPAM);
        //to delete database tables and entries everytime a new database is created
        deleted_rows = db.delete(SpamBusterContract.TABLE_SPAM.TABLE_NAME, selection_for_delete_tableall, selection_args_for_delete_tableall);
        db.execSQL(SQL_DELETE_ENTRIES_TABLESPAM);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {

        super.onOpen(db);

        //for testing purposes, we will always create a new table and delete all entries just to be sure
//        db.execSQL(SQL_DELETE_ENTRIES);
//        db.execSQL(SQL_CREATE_TABLE_HAM);
//        int deleted_rows = db.delete(SpamBusterContract.TABLE_HAM.TABLE_NAME, selection_for_delete, selection_args_for_delete);
//        db.execSQL(SQL_DELETE_ENTRIES_ROWS);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLEALL);
        db.execSQL(SQL_DROP_TABLEHAM);
        db.execSQL(SQL_DROP_TABLESPAM);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }


}
