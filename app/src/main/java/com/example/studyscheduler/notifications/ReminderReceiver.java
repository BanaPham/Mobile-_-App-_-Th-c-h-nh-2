package com.example.studyscheduler.notifications;

import com.example.studyscheduler.MainActivity;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.Notification;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL="study_reminders";
    public static void notify(Context ctx,int id,String title,String message) {
        NotificationManager manager=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Nhắc lịch học",NotificationManager.IMPORTANCE_DEFAULT));
        if(Build.VERSION.SDK_INT>=33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        Intent i=new Intent(ctx,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(ctx,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(ctx,CHANNEL).setContentTitle(title).setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setAutoCancel(true).setContentIntent(pi).build();
        manager.notify(id,n);
    }
    @Override public void onReceive(Context ctx,Intent i) {
        notify(ctx,i.getIntExtra("id",0),"Sắp đến giờ học",i.getStringExtra("message"));
    }
}
