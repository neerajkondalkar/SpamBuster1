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

        String id_str = "1000";
        String address = "9999988888";
        String sms_body = "URGENT! Your Mobile No 07808726822 was awarded a L2,000 Bonus Caller Prize on 02/09/03! This is our 2nd attempt to contact YOU! Call 0871-872-9758 BOX95QU";
//        Thread predictspam = new Thread(new PredictionProbingRunnable(this, id_str, address, sms_body));
//        predictspam.start();
        Runnable worker = new PredictionProbingRunnable(this, id_str, address, sms_body);
//        while(MainActivity.count_exec_service>0){
            executor.execute(worker);
//            MainActivity.count_exec_service--;

//        }


        Toast.makeText(getApplicationContext(),"This is a Service running in Background",
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "ClassificationSyncService: onStartCommand(): Service is running in background");
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
