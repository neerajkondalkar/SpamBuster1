package com.example.mynewsmsapp_kotlin;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.mynewsmsapp_kotlin.NewSmsMessageRunnable.SPAM;

//this class needs to be singleton class
public class DbOperationsUtility {
    private static final String TAG = "[MY_DEBUG]";
    private static DbOperationsUtility dbOperationsUtility = new DbOperationsUtility();
    private static ContentValues values = new ContentValues();
    private static Uri uri = Uri.parse("content://sms/inbox");
    private static Set<String> hashset_tableall_ids;
    private static Set<String> hashset_smsinbox_id;
    private static Set<String> set_corressinboxids;
    private DbOperationsUtility(){

    }
    public static DbOperationsUtility getInstance(){
        return dbOperationsUtility;
    }

    public String getLatestSmsInboxId(ContentResolver cr){
        ContentResolver contentResolver = cr;
        String[] projection_sms_inbox = null;
        String selection_sms_inbox = null;
        String[] selection_args_sms_inbox = null;
        String sort_order_sms_inbox = " _id DESC ";
        Cursor sms_inbox_cursor = contentResolver.query(uri, projection_sms_inbox, selection_sms_inbox, selection_args_sms_inbox, sort_order_sms_inbox);
        String latest_inbox_id = null;
        int index_id = sms_inbox_cursor.getColumnIndex("_id");
        if (sms_inbox_cursor.moveToFirst()) {
            latest_inbox_id = sms_inbox_cursor.getString(index_id);
            Log.d(TAG, "DbOperationsUtility: latest _id in content://sms/inbox is " + latest_inbox_id);
            sms_inbox_cursor.close();
        }
        return latest_inbox_id;
    }

    //returns latest SMSINBOX id after insertion
    public String insertIntoSmsInbox(MySmsMessage mySmsMessage, Context context) throws InsertionFailedException {
        values.clear();
        values.put("address", mySmsMessage.getAddress());
        values.put("person", "2");
        values.put("date", mySmsMessage.getDate());
        values.put("date_sent", mySmsMessage.getDatesent());
        values.put("body", mySmsMessage.getBody());
        Log.d(TAG, "NewSmsMessageRunnable: run(): inserting the new message in content://sms/inbox");
        ContentResolver contentResolver = context.getContentResolver();
        String old_topid = getLatestSmsInboxId(contentResolver);
        contentResolver.insert(uri, values);
        String latest_topid = getLatestSmsInboxId(contentResolver);
        if(Integer.parseInt(latest_topid) > Integer.parseInt(old_topid)){
            return latest_topid;
        }
        else{
            throw new InsertionFailedException("SMSINBOX");
        }
    }

    public String getCorressInboxIdFromTableAll(SQLiteOpenHelper db_helper, String tableallid){
        Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): reading corressinboxid from TABLEALL for tableallid: " + tableallid);
        SQLiteDatabase db = db_helper.getReadableDatabase();
//        SQLiteDatabase db = spamBusterdbHelper.getReadableDatabase();
        String[] projection_id = {
                SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
        };
        String selection_id = SpamBusterContract.TABLE_ALL._ID + " =?";
        String[] selection_args = {tableallid};
        String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
        Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                projection_id,             // The array of columns to return (pass null to get all)
                selection_id,              // The columns for the WHERE clause
                selection_args,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sort_order               // The sort order
        );
        String temp_corres_inbox_id_holder = null;
        if (!cursor_read_id.moveToFirst()) {
            Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): could not retrieve corressinboxid for tableallid: " + tableallid);
        }
        else {
            temp_corres_inbox_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
        }
        cursor_read_id.close();
//        db.close();
        return temp_corres_inbox_id_holder;
    }

    public Set<String> getAllCorressInboxIdsFromTableAll(SpamBusterdbHelper spamBusterdbHelper){
        Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): reading corressinboxids from TABLEALL");
        if(set_corressinboxids!=null){
            Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): already retirved, returning the same");
        }
        else {
            set_corressinboxids = new HashSet<>();
//            SQLiteDatabase db = db_helper.getReadableDatabase();
            SQLiteDatabase db = spamBusterdbHelper.getReadableDatabase();
            String[] projection_id = {
                    SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
            };
            String selection_id = null;
            String[] selection_args = null;
            String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
            Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                    projection_id,             // The array of columns to return (pass null to get all)
                    selection_id,              // The columns for the WHERE clause
                    selection_args,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sort_order               // The sort order
            );
            if (!cursor_read_id.moveToFirst()) {
                Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): TABLEALL is empty");
            } else {
                do {
                    String temp_corres_inbox_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
                    set_corressinboxids.add(temp_corres_inbox_id_holder);
                } while (cursor_read_id.moveToNext());
            }
            cursor_read_id.close();
//            db.close();
        }
        return set_corressinboxids;
    }

    public Set<String> getInboxIdsfromSmsInbox(SpamBusterdbHelper spamBusterdbHelper){
        //3. Get all IDs from SMSINBOX and put them in hashset_smsinbox_id.
        Log.d(TAG, "DbOperationsUtility: getInboxIdsfromSmsInbox(): reading IDs from SMSINBOX");
        if(hashset_smsinbox_id!=null){
            Log.d(TAG, "DbOperationsUtility: getInboxIdsfromSmsInbox(): already retrived, returning the same");
        }
        else {
            hashset_smsinbox_id = new HashSet<>();
            ContentResolver content_resolver = MainActivity.instance().getContentResolver();
            String[] projection = {
                    "_id"
            };
            Cursor cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), projection, null, null, "date DESC");
            if (!cursor_check_sms_id.moveToFirst()) {
                Log.d(TAG, "DbOperationsUtility: getInboxIdsfromSmsInbox(): SMSINBOX is empty");
            } else {
                int index_inboxid = cursor_check_sms_id.getColumnIndex("_id");
                do {
                    String temp_smsinboxid_holder = cursor_check_sms_id.getString(index_inboxid);
                    hashset_smsinbox_id.add(temp_smsinboxid_holder);
                } while (cursor_check_sms_id.moveToNext());
            }
            cursor_check_sms_id.close();
        }
        return hashset_smsinbox_id;
    }

    public Set<String> getAllTableAllIds(SpamBusterdbHelper spamBusterdbHelper){
        Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): reading corressinboxid from TABLEALL for tableallid: ");
        if(hashset_tableall_ids!=null){
            Log.d(TAG, "DbOperationsUtility: getAllTableAllIds(): already retrieved, returning the same");
        }
        else {
//            SQLiteDatabase db = db_helper.getReadableDatabase();
            SQLiteDatabase db = spamBusterdbHelper.getReadableDatabase();
            hashset_tableall_ids = new HashSet<>();
            String[] projection_id = {
                    SpamBusterContract.TABLE_ALL._ID,
            };
            String selection_id = null;
            String[] selection_args = null;
            String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
            Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                    projection_id,             // The array of columns to return (pass null to get all)
                    selection_id,              // The columns for the WHERE clause
                    selection_args,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sort_order               // The sort order
            );
            if (!cursor_read_id.moveToFirst()) {
                Log.d(TAG, "DbOperationsUtility: getAllTableAllIds(): TABLEALL is empty");
            } else {
                do {
                    String temp_id_holder = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID));
                    hashset_tableall_ids.add(temp_id_holder);
                } while (cursor_read_id.moveToNext());
            }
            cursor_read_id.close();
//            db.close();
        }
        return hashset_tableall_ids;
    }

    public boolean deleteConversation(String address, Context context){
        //delete from TABLEALL
        boolean deletionsuccess = false;
        SpamBusterdbHelper spamBusterdbHelper = new SpamBusterdbHelper(context);
        SQLiteDatabase db = spamBusterdbHelper.getWritableDatabase();
        ContentResolver contentResolver = context.getContentResolver();
        String whereClause = SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS + " LIKE '" + address + "'";
//        String[] whereArgs = new String[] {address};
        String [] whereArgs = null;
        Log.d(TAG, "DbOperationsUtility: deleteConversation(): deleting conversation of " + address + " from TABLEALL");
        db.beginTransaction();
        int numberOFEntriesDeleted = db.delete(SpamBusterContract.TABLE_ALL.TABLE_NAME, whereClause, whereArgs);
        if(numberOFEntriesDeleted<=0){
            Log.d(TAG, "DbOperationsUtility: deleteConversation(): Could not delete from TABLEALL.");
        }
        else{
            Log.d(TAG, "DbOperationsUtility: deleteConversation(): successfully deleted conversation of " + address + " from TABLEALL");
            // now delete from SMSINBOX
            whereClause = "address LIKE '" + address + "'";
//            whereArgs = new String[]{address};
            Uri uri = Uri.parse("content://sms");
            contentResolver.delete(uri, whereClause, whereArgs);
            //check if deleted from SMSINBOX
            Set<String> ids = getMessageInboxIdsFromAddress(address);
            if(ids.isEmpty()) {
                //if empty set is returned then it means conversation is successfully deleted from SMSINBOX
                db.setTransactionSuccessful();
                deletionsuccess = true;
                Log.d(TAG, "DbOperationsUtility: deleteConversation(): successfully deleted conversation from SMSINBOX of address " + address);
            }
            else{
                Log.d(TAG, "DbOperationsUtility: deleteConversation(): Coulbt not delete conversation from SMSINBOX. Rolling back changes done in TABLEALL");
            }
        }
        db.endTransaction();
        return deletionsuccess;
//        db.close();
    }

    //will delete the message with tableallid from both TABLEALL and SMSINBOX
    public boolean deleteMessage(String tableallid, Context context){
        //delete from TABLEALL
        boolean deletionsuccess = false;
        SpamBusterdbHelper spamBusterdbHelper = new SpamBusterdbHelper(context);
        SQLiteDatabase db = spamBusterdbHelper.getWritableDatabase();
        ContentResolver contentResolver = context.getContentResolver();
        String whereClause = SpamBusterContract.TABLE_ALL._ID + " =?";
        String[] whereArgs = new String[] {tableallid};
//        String [] whereArgs = null;
        Log.d(TAG, "DbOperationsUtility: deleteConversation(): deleting message with id: " + tableallid + " from TABLEALL");
        db.beginTransaction();
        int numberOFEntriesDeleted = db.delete(SpamBusterContract.TABLE_ALL.TABLE_NAME, whereClause, whereArgs);
        if(numberOFEntriesDeleted<=0){
            Log.d(TAG, "DbOperationsUtility: deleteConversation(): Could not delete from TABLEALL.");
        }
        else{
            Log.d(TAG, "DbOperationsUtility: deleteConversation(): successfully deleted message with id:" + tableallid + " from TABLEALL");
            // now delete from SMSINBOX
            whereClause = "_id=?";
            String corressinboxid = getCorressInboxIdFromTableAll(spamBusterdbHelper, tableallid);
            if(corressinboxid==null){
                Log.d(TAG, "DbOperationsUtility: deleteMessage(): corressinboxid = null");
                Log.d(TAG, "DbOperationsUtility: deleteMessage(): This means we dont need to delete it from SMSINBOX as it is not there in SMSINBOX");
                db.setTransactionSuccessful();
                deletionsuccess = true;
            }
            else {
                Log.d(TAG, "DbOperationsUtility: deleteMessage(): attempting to delete message from SMSINBOX with inboxid :" + corressinboxid);
                whereArgs = new String[]{corressinboxid};
                Uri uri = Uri.parse("content://sms");
                contentResolver.delete(uri, whereClause, whereArgs);
                //check if deleted from SMSINBOX
                Set<String> ids = getInboxIdsfromSmsInbox(spamBusterdbHelper);
                if (!ids.contains(corressinboxid)) {
                    //if empty set is returned then it means conversation is successfully deleted from SMSINBOX
                    db.setTransactionSuccessful();
                    deletionsuccess = true;
                    Log.d(TAG, "DbOperationsUtility: deleteConversation(): successfully deleted message from SMSINBOX with inboxid " + corressinboxid);
                } else {
                    Log.d(TAG, "DbOperationsUtility: deleteConversation(): Coulbt not delete message from SMSINBOX. Rolling back changes done in TABLEALL");
                }
            }
        }
        db.endTransaction();
        return deletionsuccess;
//        db.close();
    }


    public Set<String> getMessageInboxIdsFromAddress(String address){
        Set<String> idsfromaddress = new HashSet<>();
        Log.d(TAG, "DbOperationsUtility: getMessageIdsFromAddress(): reading IDs from SMSINBOX where address = " + address);
        ContentResolver content_resolver = MainActivity.instance().getContentResolver();
        String[] projection = {
                "_id"
        };
        String whereClause = "address LIKE '" + address + "'";
//        String[] whereArgs = new String[]{address};
        String [] whereArgs = null;
        Cursor cursor_check_sms_id = content_resolver.query(Uri.parse("content://sms/inbox"), projection, whereClause, whereArgs, null);
        if (!cursor_check_sms_id.moveToFirst()) {
            Log.d(TAG, "DbOperationsUtility: getMessageIdsFromAddress(): SMSINBOX is empty");
        }
        else {
            int index_inboxid = cursor_check_sms_id.getColumnIndex("_id");
            do {
                String temp_smsinboxid_holder = cursor_check_sms_id.getString(index_inboxid);
                idsfromaddress.add(temp_smsinboxid_holder);
            } while (cursor_check_sms_id.moveToNext());
        }
        cursor_check_sms_id.close();
        return idsfromaddress;
    }

    public MySmsMessage getMessageFromTablallId(String tableallid, SpamBusterdbHelper spamBusterdbHelper){
        MySmsMessage mySmsMessage = null;
        Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): reading corressinboxid from TABLEALL for tableallid: " + tableallid);
//        SQLiteDatabase db = db_helper.getReadableDatabase();
        SQLiteDatabase db = spamBusterdbHelper.getReadableDatabase();
        String[] projection_id = {
                SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY,
                SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE,
                SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT,
                SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS
        };
        String selection_id = SpamBusterContract.TABLE_ALL._ID + " =?";
        String[] selection_args = {tableallid};
        String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
        Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                projection_id,             // The array of columns to return (pass null to get all)
                selection_id,              // The columns for the WHERE clause
                selection_args,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sort_order               // The sort order
        );
        String temp_address = null;
        String temp_date = null;
        String temp_datesent = null;
        String temp_body=null;
        if (!cursor_read_id.moveToFirst()) {
            Log.d(TAG, "DbOperationsUtility: getAllCorressInboxIdsFromTableAll(): could not retrieve corressinboxid for tableallid: " + tableallid);
        }
        else {
                temp_address = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_ADDRESS));
                temp_date = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE));
                temp_datesent = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE_SENT));
                temp_body = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_BODY));
                mySmsMessage = new MySmsMessage(temp_address, temp_date, temp_datesent, temp_body);
        }
        cursor_read_id.close();
//        db.close();
        return mySmsMessage;
    }

    //will return true if updation was successfull, else false
    public boolean updateMessageInTableAll(MySmsMessage mySmsMessage, SpamBusterdbHelper spamBusterdbHelper){
        SQLiteDatabase db = spamBusterdbHelper.getWritableDatabase();
        values.clear();
        Log.d(TAG, "NewSmsMessageRunnable: run(): corress_inbox_id set to " + mySmsMessage.getCorressinboxid());
        if(mySmsMessage.getCorressinboxid()!=null) {
            Log.d(TAG, "NewSmsMessageRunnable: run(): updating corress_inbox_id at _id '" + mySmsMessage.getTableallid() + "' in table_all to " + mySmsMessage.getCorressinboxid());
            values.put(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID, mySmsMessage.getCorressinboxid());
        }
        else{
            Log.d(TAG, "DbOperationsUtility: updateMessageInTableAll(): corressinbox id is null, will not be updated");
        }
        values.put(SpamBusterContract.TABLE_ALL.COLUMN_SPAM, mySmsMessage.getSpam());
        String[] whereArgs = new String[]{mySmsMessage.getTableallid()};
        db.beginTransaction();
        db.update(SpamBusterContract.TABLE_ALL.TABLE_NAME, values, SpamBusterContract.TABLE_ALL._ID + "=?", whereArgs);
        db.setTransactionSuccessful();
        db.endTransaction();

        //check if tableall is updated with the required value (code in newsmsrunnable)
        boolean updated = false;
        Log.d(TAG, "DbOperationsUtility: updateMessageInTableAll(): checking if TABLEALL has been updated");
        db = spamBusterdbHelper.getReadableDatabase();
        db.beginTransaction();
        String[] projection_id = {
                SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID,
                SpamBusterContract.TABLE_ALL.COLUMN_SPAM
        };
        String selection_id = SpamBusterContract.TABLE_ALL._ID + " =?";
        String[] selection_args = {mySmsMessage.getTableallid()};
        Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                projection_id,             // The array of columns to return (pass null to get all)
                selection_id,              // The columns for the WHERE clause
                selection_args,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        if (cursor_read_id.moveToFirst()) {
            String corressinboxid = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_CORRES_INBOX_ID));
            String spam = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SPAM));
            if(((corressinboxid==null && mySmsMessage.getCorressinboxid()==null) || corressinboxid.equals(mySmsMessage.getCorressinboxid()) )
                    && spam.equals(mySmsMessage.getSpam())){
                updated = true;
            }
        }
        else{
            Log.d(TAG, "DbOperationsUtility: updateMessageInTableAll(): error reading TABLEALL");
        }
        db.setTransactionSuccessful();
        db.endTransaction();
//        db.close();
        return updated;
    }

    public void autoDelete(Context context, String duration){
        List<String> idstodelete = getSpamIdsToAutoDelete(context, duration);
//        Log.d(TAG, "DbOperationsUtility: autoDelete(): SPAM ids that need to be deleted:");
        for(String id : idstodelete){
//            Log.d(TAG, "DbOperationsUtility: autoDelete(): " + id);
            // uncomment to delete
            //            deleteMessage(id, context);
        }
    }

    private List<String> getSpamIdsToAutoDelete(Context context, String duration){
        List<String> idstodelete = new ArrayList<>();
            SpamBusterdbHelper spamBusterdbHelper = new SpamBusterdbHelper(context);
//            SQLiteDatabase db = db_helper.getReadableDatabase();
            SQLiteDatabase db = spamBusterdbHelper.getReadableDatabase();
            String[] projection_id = {
                    SpamBusterContract.TABLE_ALL._ID,
                    SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE
            };
            String selection_id = SpamBusterContract.TABLE_ALL.COLUMN_SPAM + " =?";
            String[] selection_args = {SPAM};
            String sort_order = SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE + " DESC";
            Cursor cursor_read_id = db.query(SpamBusterContract.TABLE_ALL.TABLE_NAME,   // The table to query
                    projection_id,             // The array of columns to return (pass null to get all)
                    selection_id,              // The columns for the WHERE clause
                    selection_args,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sort_order               // The sort order
            );
            if (!cursor_read_id.moveToFirst()) {
                Log.d(TAG, "DbOperationsUtility: showDateDiff(): TABLEALL is empty");
            } else {
                String current_date = String.valueOf(Calendar.getInstance().getTimeInMillis());
//                Log.d(TAG, "DbOperationsUtility: showDateDiff(): Current date: " + current_date);
                long durationinmillis = TimeUnit.MILLISECONDS.convert(Long.parseLong(duration), TimeUnit.DAYS);
//                Log.d(TAG, "DbOperationsUtility: showDateDiff(): Duration allowed: " + durationinmillis + " i.e " + duration + " days");
                do {
                    String temp_id = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL._ID));
                    String temp_date = cursor_read_id.getString(cursor_read_id.getColumnIndexOrThrow(SpamBusterContract.TABLE_ALL.COLUMN_SMS_EPOCH_DATE));
//                    Log.d(TAG, "DbOperationsUtility: showDateDiff(): date : " + temp_date);
                    Long diff = Long.parseLong(current_date) - Long.parseLong(temp_date);
                    String difference = String.valueOf(diff);
                    Long timetodelete_long = durationinmillis - Long.parseLong(difference);
                    String timelefttodelete = String.valueOf(timetodelete_long);
//                    Log.d(TAG, String.format("DbOperationsUtility: showDateDiff(): id:%s  difference in date:%s  time left to delete %s",
//                            temp_id, difference, timelefttodelete));
                    long days_todelete = TimeUnit.MILLISECONDS.toDays(timetodelete_long);
//                    Log.d(TAG, "DbOperationsUtility: showDateDiff(): Time left to delete in days " + days_todelete  + " for ID : " + temp_id);
                    if(days_todelete<=0) {
                        idstodelete.add(temp_id);
                    }
                } while (cursor_read_id.moveToNext());
            }
            cursor_read_id.close();
//            db.close();
        return idstodelete;
    }
}
