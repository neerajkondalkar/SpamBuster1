package com.example.mynewsmsapp_kotlin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClassificationSyncService extends Service {
//    public Context context = getApplicationContext();
    private static final String TAG = "[MY_DEBUG]";


    public ClassificationSyncService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        String[] id_str = {"1000"};
        String address = "9999988888";
        String[] sms_body = {"URGENT! Your Mobile No 07808726822 was awarded a L2,000 Bonus Caller Prize on 02/09/03! This is our 2nd attempt to contact YOU! Call 0871-872-9758 BOX95QU"};
//        Thread predictspam = new Thread(new PredictionProbingRunnable(this, id_str, address, sms_body));
//        predictspam.start();
        Runnable worker = new PredictionProbingRunnable(this, id_str, address, sms_body);
//        while(MainActivity.count_exec_service>0){
//            executor.execute(worker);
//            MainActivity.count_exec_service--;
//        }

        // all this code will mostly go into TableAllSyncHandlerThread, in that case instead of spawning PredictionProbingRunnable, we will have the code as a method in TableAllSyncHandlerThread
        // create String[] arrlist_to_predict_messsages
        // create HashMap map_tablepending <String id_tableall, String id_tablepending>
        // create HashMap map_rev_tablepending <String id_tablepending, String id_tableall>
        // first extract TABLE_ALL ids where coress_inbox_id == UNCLASSIFIED
        // put them in HashSet hashset_id_tableall
        // then extract all the id_tableall, id_tablepending from TABLE_PENDING
        // put all id_tableall in ArrayList arrlist_id_tableall_unclass. Also add entries into the map_tablepending <id_tableall, id_tablepending> and map_rev_tablepending<id_tablepending, id_tableall>
        // iterate through the ArrayList arrlist_id_tableall
        //      if arrlist_id_tableall_unclass(i) is not present in HashSet hashset_id_tableall (meaning it is already classified),
        //              then remove arrlist_id_tableall_unclass(i) and spawn a runnable to remove that entry with corresponding id_tablepending (by getting from map_tablepending) from TABLE_PENDING
        //      else if arrlist_id_tableall_unclass(i) is present in HashSet hashset_id_tableall(meaning it is not yet classified),
        //              then query tableall for message_body and add the the corresponding messages_body  to index i of arrlist_to_predict_messages
        // thus, all tableall ids to predict are in arrlist_id_tableall_unclass and all corressponding messages are in arrlist_to_predict_messages
        // iterate through all ids and messages
        //      put  map_tablepending.get(arrlist_id_tableall_unclass[i]) into  strarr_ids[i] , and arrlist_to_predict_messages into strarr_messages[i] where
        //      index i should same for both strarr_ids and strarr_messages
        // thus strarr_ids will have all the table_pending ids and strarr_messages will have all the messages which have to be predicted
        // if strarr_ids and strarr_messages are not empty
        //     spawn PredictionProbingRunnable(context, strarr_ids, "number", strarr_messages, map_rev_tablepending)
        // strarr_id has id and predictionarr has the corressponding prediction, index matching, strarr_id[i] prediction is at predictionarr[i]
        // loop through the predictionarr
        //      if predictionarr[i] != -1,
        //          then remove the strarr_id[i] from TABLE_PENDING
        //          if predictionarr[i] == 1
        //              then update TABLE_ALL corress_inbox_id =




        Toast.makeText(getApplicationContext(),"This is a Service running in Background",
                Toast.LENGTH_SHORT).show();
//        Log.d(TAG, "ClassificationSyncService: onStartCommand(): Service is running in background");
        return START_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(),this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

}
