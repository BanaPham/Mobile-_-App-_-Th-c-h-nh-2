package com.example.studyscheduler;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.example.studyscheduler.auth.SessionManager;
import com.example.studyscheduler.dal.DBHelper;
import com.example.studyscheduler.activities.AdminActivity;
import com.example.studyscheduler.activities.StudentActivity;
import com.example.studyscheduler.activities.LoginActivity;

/** Person 1: only acts as the app launcher/router. */
public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SessionManager session=new SessionManager(this);
        Class<?> next=LoginActivity.class;
        try(DBHelper db=new DBHelper(this)) {
            if (session.isValid(db)) {
                if ("ADMIN".equals(session.role())) next=AdminActivity.class;
                else if ("STUDENT".equals(session.role()) && db.studentId(session.accountId())>=0) next=StudentActivity.class;
                else session.logout();
            }
        }
        startActivity(new Intent(this,next));finish();
    }
}
