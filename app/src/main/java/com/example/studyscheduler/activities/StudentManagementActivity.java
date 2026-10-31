package com.example.studyscheduler.activities;
import android.os.Bundle;
import android.database.Cursor;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.example.studyscheduler.base.BaseActivity;
import com.example.studyscheduler.dal.DBHelper;

/** Person 2: admin manages students with CRUD + search; account creation is transactional. */
public class StudentManagementActivity extends BaseActivity {
    @Override protected void onCreate(Bundle saved) { super.onCreate(saved);if(!guard("ADMIN"))return;listStudents(""); }
    private void listStudents(String query){
        setup("QUẢN LÝ SINH VIÊN");navigation("Trang chủ",this::finish);primaryButton(content,"+ Thêm sinh viên",()->studentForm(-1));searchInput(content,query,this::listStudents);
        String term="%"+query+"%";
        try(Cursor c=db.query("SELECT * FROM Student WHERE studentCode LIKE ? OR fullname LIKE ? OR email LIKE ? ORDER BY id DESC",term,term,term)){
            while(c.moveToNext()){
                long id=DBHelper.lng(c,"id");LinearLayout box=group(content);
                text(box,DBHelper.str(c,"studentCode")+" — "+DBHelper.str(c,"fullname"),18);
                text(box,"Email: "+DBHelper.str(c,"email")+"\nSĐT: "+DBHelper.str(c,"phone")+"\nTài khoản: "+db.usernameForStudent(id),14);
                button(box,"Sửa",()->studentForm(id));button(box,"Xóa",()->askDelete("sinh viên",()->{
                    try {db.deleteStudent(id);listStudents(query);}catch(Exception e){fail(e);}
                }));
            }
        }
    }
    private void studentForm(long id){
        setup(id<0?"THÊM SINH VIÊN":"SỬA SINH VIÊN");navigation("Danh sách",()->listStudents(""));
        String code="",name="",email="",phone="",user="";
        if(id>0)try(Cursor c=db.query("SELECT * FROM Student WHERE id=?",String.valueOf(id))){if(c.moveToFirst()){
            code=DBHelper.str(c,"studentCode");name=DBHelper.str(c,"fullname");email=DBHelper.str(c,"email");phone=DBHelper.str(c,"phone");user=db.usernameForStudent(id);
        }}
        EditText a=field(content,"Mã sinh viên",code),b=field(content,"Họ và tên",name),c=field(content,"Email",email),d=field(content,"Số điện thoại",phone);
        EditText e=field(content,"Tên đăng nhập",user),f=field(content,id<0?"Mật khẩu":"Mật khẩu mới (để trống nếu giữ nguyên)","");
        f.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        primaryButton(content,"Lưu sinh viên",()->{
            String aa=a.getText().toString().trim(),bb=b.getText().toString().trim(),cc=c.getText().toString().trim(),dd=d.getText().toString().trim(),ee=e.getText().toString().trim(),ff=f.getText().toString();
            if(!required(aa,bb,cc,dd,ee)||id<0&&!required(ff))return;
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(cc).matches()){message("Email không hợp lệ");return;}
            try {if(id<0)db.createStudent(aa,bb,cc,dd,ee,ff);else db.updateStudent(id,aa,bb,cc,dd,ee,ff);listStudents("");}catch(Exception ex){fail(ex);}
        });
    }
}
