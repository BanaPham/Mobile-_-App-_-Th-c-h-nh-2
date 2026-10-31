package com.example.studyscheduler.activities;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.example.studyscheduler.base.BaseActivity;
import com.example.studyscheduler.dal.DBHelper;

/** Person 2: admin manages subjects with CRUD + search. */
public class SubjectManagementActivity extends BaseActivity {
    @Override protected void onCreate(Bundle saved) {super.onCreate(saved);if(!guard("ADMIN"))return;listSubjects("");}
    private void listSubjects(String query){
        setup("QUẢN LÝ MÔN HỌC");navigation("Trang chủ",this::finish);primaryButton(content,"+ Thêm môn",()->subjectForm(-1));searchInput(content,query,this::listSubjects);
        String term="%"+query+"%";
        try(Cursor c=db.query("SELECT * FROM Subject WHERE subjectCode LIKE ? OR subjectName LIKE ? ORDER BY id DESC",term,term)){
            while(c.moveToNext()){
                long id=DBHelper.lng(c,"id");LinearLayout box=group(content);
                text(box,DBHelper.str(c,"subjectCode")+" — "+DBHelper.str(c,"subjectName"),18);
                text(box,DBHelper.str(c,"description")+"\nSố tín chỉ: "+DBHelper.str(c,"credits"),14);
                button(box,"Sửa",()->subjectForm(id));button(box,"Xóa",()->askDelete("môn học",()->{
                    try {db.delete("Subject",id);listSubjects(query);}catch(Exception e){message("Không thể xóa môn đang được sử dụng trong lịch học");}
                }));
            }
        }
    }
    private void subjectForm(long id){
        setup(id<0?"THÊM MÔN HỌC":"SỬA MÔN HỌC");navigation("Danh sách",()->listSubjects(""));
        String code="",name="",desc="",credits="3";
        if(id>0)try(Cursor c=db.query("SELECT * FROM Subject WHERE id=?",String.valueOf(id))){if(c.moveToFirst()){
            code=DBHelper.str(c,"subjectCode");name=DBHelper.str(c,"subjectName");desc=DBHelper.str(c,"description");credits=DBHelper.str(c,"credits");
        }}
        EditText a=field(content,"Mã môn học",code),b=field(content,"Tên môn học",name),c=field(content,"Mô tả",desc),d=field(content,"Số tín chỉ",credits);d.setInputType(InputType.TYPE_CLASS_NUMBER);
        primaryButton(content,"Lưu môn học",()->{
            String aa=a.getText().toString().trim(),bb=b.getText().toString().trim(),cc=c.getText().toString().trim(),dd=d.getText().toString().trim();if(!required(aa,bb,dd))return;
            try {int cr=Integer.parseInt(dd);if(cr<=0)throw new IllegalArgumentException("Số tín chỉ phải > 0");ContentValues cv=new ContentValues();cv.put("subjectCode",aa);cv.put("subjectName",bb);cv.put("description",cc);cv.put("credits",cr);
                if(id<0)db.insert("Subject",cv);else db.update("Subject",cv,id);listSubjects("");
            }catch(Exception ex){fail(ex);}
        });
    }
}
