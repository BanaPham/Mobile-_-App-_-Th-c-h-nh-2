package com.example.studyscheduler.activities;
import android.os.Bundle;
import com.example.studyscheduler.base.BaseActivity;

/** Person 1: ADMIN's role-specific home. */
public class AdminActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) { super.onCreate(state);
        if(!guard("ADMIN"))return;
        setup("STUDY SCHEDULER • ADMIN");text(content,"Quản trị viên",18);
        button(content,"Quản lý sinh viên",()->go(StudentManagementActivity.class));
        button(content,"Quản lý môn học",()->go(SubjectManagementActivity.class));
        button(content,"Đăng xuất",()->{session.logout();navigate(LoginActivity.class);});
    }
}
