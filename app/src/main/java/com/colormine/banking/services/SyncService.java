package com.colormine.banking.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;

public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private Handler handler;
    private Runnable syncRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Syncing data in background...");
                // Repeat every 15 seconds for demonstration
                handler.postDelayed(this, 15000);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Background Sync Started", Toast.LENGTH_SHORT).show();
        handler.post(syncRunnable);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Background Sync Stopped", Toast.LENGTH_SHORT).show();
        handler.removeCallbacks(syncRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
