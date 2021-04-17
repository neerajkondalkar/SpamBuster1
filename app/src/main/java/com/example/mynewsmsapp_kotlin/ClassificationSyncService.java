package com.example.mynewsmsapp_kotlin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
public class ClassificationSyncService extends Service {
    private static final String TAG = "[MY_DEBUG]";
    public ClassificationSyncService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);
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
