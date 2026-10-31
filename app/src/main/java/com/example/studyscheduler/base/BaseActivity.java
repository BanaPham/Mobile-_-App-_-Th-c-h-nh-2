package com.example.studyscheduler.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.studyscheduler.auth.SessionManager;
import com.example.studyscheduler.dal.DBHelper;
import com.example.studyscheduler.activities.LoginActivity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.function.Consumer;

/** Person 1: shared activity navigation, basic UI widgets and permission checks. */
public abstract class BaseActivity extends Activity {
    protected DBHelper db;
    protected SessionManager session;
    protected LinearLayout content;
    protected long studentId = -1;
    private static final int BLUE = Color.rgb(37,99,235);
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DBHelper(this);
        session = new SessionManager(this);
    }
    @Override protected void onDestroy() { db.close(); super.onDestroy(); }
    protected boolean guard(String role) {
        if (!session.isValid(db) || !role.equals(session.role())) {
            session.logout(); navigate(LoginActivity.class); return false;
        }
        if ("STUDENT".equals(role)) {
            studentId = db.studentId(session.accountId());
            if (studentId < 0) { session.logout(); navigate(LoginActivity.class); return false; }
        }
        return true;
    }
    protected void navigate(Class<?> cls) { startActivity(new Intent(this, cls)); finish(); }
    protected void go(Class<?> cls) { startActivity(new Intent(this, cls)); }
    protected int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    protected void setup(String title) {
        ScrollView scroll=new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(35));
        root.setBackgroundColor(0xFFF5F7FB);
        scroll.addView(root); setContentView(scroll);
        TextView heading=new TextView(this);heading.setText(title);heading.setTextSize(25);
        heading.setTextColor(0xFF18335A);heading.setTypeface(null,1);
        heading.setPadding(0,0,0,dp(15));root.addView(heading);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);root.addView(content);
    }
    public TextView text(LinearLayout parent,String message,int size) {
        TextView t=new TextView(this);t.setText(message);t.setTextSize(size);
        t.setTextColor(0xFF23344E);t.setPadding(dp(7),dp(8),dp(7),dp(8));parent.addView(t);return t;
    }
    public Button button(LinearLayout parent,String title,Runnable callback) {
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(BLUE);
        parent.addView(b);b.setOnClickListener(v->callback.run());return b;
    }
    public EditText field(LinearLayout parent,String hint,String value) {
        EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);
        e.setText(value);e.setTextSize(16);parent.addView(e);return e;
    }
    public LinearLayout group(LinearLayout p) {
        LinearLayout g=new LinearLayout(this);g.setOrientation(LinearLayout.VERTICAL);
        g.setPadding(dp(10),dp(7),dp(10),dp(8));g.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.bottomMargin=dp(12);p.addView(g,params);return g;
    }
    protected void message(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    protected boolean required(String... values) {
        for(String v:values) if(v.trim().isEmpty()) { message("Vui lòng nhập đầy đủ thông tin");return false; }
        return true;
    }
    protected void fail(Exception e) { message("Không thể lưu: "+(e.getMessage()==null?"Dữ liệu không hợp lệ":e.getMessage())); }
    protected void navigation(String name,Runnable callback) { button(content,"‹ "+name,callback); }
    protected void askDelete(String what,Runnable yes) {
        new AlertDialog.Builder(this).setTitle("Xác nhận")
            .setMessage("Bạn có chắc muốn xóa/hủy "+what+"?")
            .setNegativeButton("Không",null).setPositiveButton("Đồng ý",(d,w)->yes.run()).show();
    }
    protected void searchInput(LinearLayout p,String current,Consumer<String> callback) {
        EditText q=field(p,"Tìm theo mã, tên...",current);
        button(p,"Tìm kiếm",()->callback.accept(q.getText().toString().trim()));
    }
    protected Button dateButton(LinearLayout p,String date) {
        Button b=button(p,"Ngày: "+date,()->{});b.setTag(date);
        b.setOnClickListener(v->{
            LocalDate d=LocalDate.parse(b.getTag().toString());
            new DatePickerDialog(this,(picker,y,m,day)->{
                String s=LocalDate.of(y,m+1,day).toString();b.setTag(s);b.setText("Ngày: "+s);
            },d.getYear(),d.getMonthValue()-1,d.getDayOfMonth()).show();
        });return b;
    }
    protected Button timeButton(LinearLayout p,String time) {
        Button b=button(p,time,()->{});b.setTag(time);
        b.setOnClickListener(v->{
            LocalTime t=LocalTime.parse(b.getTag().toString());
            new TimePickerDialog(this,(picker,h,m)->{
                String s=String.format(Locale.US,"%02d:%02d",h,m);b.setTag(s);b.setText(s);
            },t.getHour(),t.getMinute(),true).show();
        });return b;
    }
}
