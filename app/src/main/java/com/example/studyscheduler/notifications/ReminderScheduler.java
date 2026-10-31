package com.example.studyscheduler.notifications;

import com.example.studyscheduler.dal.DBHelper;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ReminderScheduler {
    private static PendingIntent pending(Context ctx,long id,String message) {
        Intent i=new Intent(ctx,ReminderReceiver.class);i.putExtra("id",(int)id);i.putExtra("message",message);
        return PendingIntent.getBroadcast(ctx,(int)id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void cancel(Context ctx,long id) {
        ((AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE)).cancel(pending(ctx,id,""));
    }
    public static void schedule(Context ctx,DBHelper db,long id) {
        cancel(ctx,id);
        try(Cursor c=db.query("SELECT sc.studyDate,sc.startTime,sc.location,sc.status,s.subjectName FROM StudySchedule sc JOIN Subject s ON s.id=sc.subjectId WHERE sc.id=?",String.valueOf(id))) {
            if(!c.moveToFirst() || "CANCELLED".equals(DBHelper.str(c,"status")))return;
            LocalDateTime start=LocalDateTime.parse(DBHelper.str(c,"studyDate")+"T"+DBHelper.str(c,"startTime"));
            long fire=start.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if(fire<=System.currentTimeMillis())return;
            String message=DBHelper.str(c,"subjectName")+" • "+DBHelper.str(c,"startTime")+" • "+DBHelper.str(c,"location");
            ((AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE)).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,fire,pending(ctx,id,message));
        } catch(Exception ignored) { }
    }
    public static void scheduleAll(Context ctx) {
        DBHelper db=new DBHelper(ctx);for(long id:db.upcomingScheduleIds())schedule(ctx,db,id);db.close();
    }
}
