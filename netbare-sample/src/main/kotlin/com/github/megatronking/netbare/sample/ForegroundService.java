package com.github.megatronking.netbare.sample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.github.megatronking.netbare.sample.util.Logger;

public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "com.github.megatronking.netbare.sample.ffNOTIFICATION_CHANNEL_ID";

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.e("ForegroundService onCreate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                        getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW));
            }
        }else {
//            startForeground(101, createNotification());
        }
    }

    private Notification createNotification(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.netbare_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_name))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.e("ForegroundService onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.e("ForegroundService onStartCommand");
        startForeground(101, createNotification());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean stopService(Intent name) {
        stopForeground(true);
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        Logger.e("ForegroundService  onDestroy");
        super.onDestroy();
    }



}
