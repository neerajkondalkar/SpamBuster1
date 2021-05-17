package com.example.mynewsmsapp_kotlin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.mynewsmsapp_kotlin.MainActivity.auto_delete_duration;

public class ClassificationSyncService extends Service {
//    private Context context = getApplicationContext();
    private static final String TAG = "[MY_DEBUG]";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);
            //auto-delete SPAM after certain amount of time mentioned in shared preferences
            //for now just show the differences in dates of all ID
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
////                    Log.d(TAG, "ClassificationSyncService: run(): check for autodelete of spam");
//                    DbOperationsUtility.getInstance().autoDelete(getApplicationContext(), auto_delete_duration);
//                }
//            }).start();
//        Toast.makeText(getApplicationContext(),"This is a Service running in Background", Toast.LENGTH_SHORT).show();
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

//    public static ClassificationSyncService getInstance(){
//        return classificationSyncService;
//    }
}
