package com.example.studyscheduler.activities;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import com.example.studyscheduler.base.BaseActivity;

/** Person 1: STUDENT's role-specific home. */
public class StudentActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) { super.onCreate(state);
        if(!guard("STUDENT"))return;
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
        setup("STUDY SCHEDULER • STUDENT");text(content,"Sinh viên",18);
        button(content,"Đặt lịch học",()->go(BookingActivity.class));
        button(content,"Lịch học của tôi",()->go(ScheduleActivity.class));
        button(content,"Xem lịch theo ngày / tuần / tháng",()->go(CalendarActivity.class));
        button(content,"Đăng xuất",()->{session.logout();navigate(LoginActivity.class);});
    }
}
