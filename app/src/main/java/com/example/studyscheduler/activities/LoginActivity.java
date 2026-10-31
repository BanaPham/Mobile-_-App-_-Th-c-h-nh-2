package com.example.studyscheduler.activities;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import com.example.studyscheduler.base.BaseActivity;

/** Person 1: Admin/Student login, persistence delegated to SessionManager. */
public class LoginActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) { super.onCreate(state);showLogin(); }
    private void showLogin() {
        setup("ĐĂNG NHẬP");text(content,"Ứng dụng quản lý và đặt lịch học",17);
        EditText username=field(content,"Tên đăng nhập","");
        EditText password=field(content,"Mật khẩu","");
        password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        button(content,"Đăng nhập",()->{
            String[] a=db.authenticate(username.getText().toString().trim(),password.getText().toString());
            if(a==null){message("Sai tài khoản hoặc mật khẩu");return;}
            session.login(Long.parseLong(a[0]),a[1]);
            if("ADMIN".equals(a[1])) navigate(AdminActivity.class);
            else if("STUDENT".equals(a[1])) navigate(StudentActivity.class);
        });
        text(content,"Demo: admin/admin123 hoặc student/student123",14);
    }
}
