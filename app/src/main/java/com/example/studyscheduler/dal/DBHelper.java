package com.example.studyscheduler.dal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context c) { super(c, "study_scheduler.db", null, 1); }

    @Override public void onConfigure(SQLiteDatabase db) { super.onConfigure(db); db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Account(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('ADMIN','STUDENT')))");
        db.execSQL("CREATE TABLE Student(id INTEGER PRIMARY KEY AUTOINCREMENT, studentCode TEXT NOT NULL UNIQUE, fullname TEXT NOT NULL, email TEXT NOT NULL, phone TEXT NOT NULL, accountId INTEGER NOT NULL UNIQUE REFERENCES Account(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE Subject(id INTEGER PRIMARY KEY AUTOINCREMENT, subjectCode TEXT NOT NULL UNIQUE, subjectName TEXT NOT NULL, description TEXT NOT NULL, credits INTEGER NOT NULL CHECK(credits>0))");
        db.execSQL("CREATE TABLE StudySchedule(id INTEGER PRIMARY KEY AUTOINCREMENT, studentId INTEGER NOT NULL REFERENCES Student(id) ON DELETE CASCADE, subjectId INTEGER NOT NULL REFERENCES Subject(id) ON DELETE RESTRICT, studyDate TEXT NOT NULL, startTime TEXT NOT NULL, endTime TEXT NOT NULL, location TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('PENDING','CONFIRMED','CANCELLED')), CHECK(startTime<endTime))");
        db.execSQL("CREATE INDEX idx_schedule_student_date ON StudySchedule(studentId,studyDate)");
        ContentValues a=new ContentValues(); a.put("username","admin"); a.put("password",hash("admin123")); a.put("role","ADMIN"); db.insertOrThrow("Account",null,a);
        ContentValues u=new ContentValues(); u.put("username","student"); u.put("password",hash("student123")); u.put("role","STUDENT"); long aid=db.insertOrThrow("Account",null,u);
        ContentValues s=new ContentValues(); s.put("studentCode","SV001"); s.put("fullname","Sinh viên mẫu"); s.put("email","student@example.com"); s.put("phone","0900000000"); s.put("accountId",aid); db.insertOrThrow("Student",null,s);
        ContentValues sub=new ContentValues(); sub.put("subjectCode","INT101"); sub.put("subjectName","Lập trình Java"); sub.put("description","Môn học nhập môn Java"); sub.put("credits",3); db.insertOrThrow("Subject",null,sub);
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { /* Database schema version 1 */ }

    // Salted PBKDF2 formatted salt:hash; old unseeded SHA-256 is not used.
    public static String hash(String password) {
        try {
            byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
            return hex(salt)+":"+derive(password,salt);
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    private static String derive(String pass,byte[] salt) throws Exception {
        javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(pass.toCharArray(),salt,120000,256);
        try { return hex(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
        finally { spec.clearPassword(); }
    }
    private static String hex(byte[] in) { StringBuilder s=new StringBuilder(); for(byte b:in)s.append(String.format(java.util.Locale.US,"%02x",b&0xff)); return s.toString(); }
    private static byte[] unhex(String s) { byte[] b=new byte[s.length()/2]; for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16); return b; }
    private static boolean matches(String password,String saved) {
        try { String[] p=saved.split(":",-1); return p.length==2 && MessageDigest.isEqual(unhex(p[1]),unhex(derive(password,unhex(p[0])))); }
        catch(Exception e) { return false; }
    }
    public Cursor query(String sql,String... args) { return getReadableDatabase().rawQuery(sql,args); }
    public long insert(String table,ContentValues cv) { return getWritableDatabase().insertOrThrow(table,null,cv); }
    public int update(String table,ContentValues cv,long id) { return getWritableDatabase().update(table,cv,"id=?",new String[]{String.valueOf(id)}); }
    public int delete(String table,long id) { return getWritableDatabase().delete(table,"id=?",new String[]{String.valueOf(id)}); }
    public static String str(Cursor c,String col) { return c.getString(c.getColumnIndexOrThrow(col)); }
    public static long lng(Cursor c,String col) { return c.getLong(c.getColumnIndexOrThrow(col)); }
    public String[] authenticate(String username,String password) {
        try(Cursor c=query("SELECT id,password,role FROM Account WHERE username=?",username)) {
            if(c.moveToFirst() && matches(password,str(c,"password"))) return new String[]{str(c,"id"),str(c,"role")};
        } return null;
    }
    public boolean activeSession(long accountId,String role) {
        try(Cursor c=query("SELECT 1 FROM Account WHERE id=? AND role=?",String.valueOf(accountId),role)){ return c.moveToFirst(); }
    }
    public long studentId(long accountId) {
        try(Cursor c=query("SELECT id FROM Student WHERE accountId=?",String.valueOf(accountId))){ return c.moveToFirst()?c.getLong(0):-1; }
    }
    public long createStudent(String code,String name,String email,String phone,String username,String password){
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try { ContentValues a=new ContentValues();a.put("username",username);a.put("password",hash(password));a.put("role","STUDENT");long acc=db.insertOrThrow("Account",null,a);
            ContentValues s=new ContentValues();s.put("studentCode",code);s.put("fullname",name);s.put("email",email);s.put("phone",phone);s.put("accountId",acc);long id=db.insertOrThrow("Student",null,s);
            db.setTransactionSuccessful(); return id;
        } finally { db.endTransaction(); }
    }
    public void updateStudent(long id,String code,String name,String email,String phone,String username,String newPassword){
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try { long accountId;try(Cursor c=query("SELECT accountId FROM Student WHERE id=?",String.valueOf(id))){if(!c.moveToFirst())throw new IllegalArgumentException("Không tìm thấy sinh viên"); accountId=c.getLong(0);}
            ContentValues s=new ContentValues();s.put("studentCode",code);s.put("fullname",name);s.put("email",email);s.put("phone",phone);db.update("Student",s,"id=?",new String[]{String.valueOf(id)});
            ContentValues a=new ContentValues();a.put("username",username);if(!newPassword.isEmpty())a.put("password",hash(newPassword));db.update("Account",a,"id=?",new String[]{String.valueOf(accountId)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    /** Delete both Student and linked Account in one transaction (avoids orphan logins). */
    public void deleteStudent(long id) {
        SQLiteDatabase database=getWritableDatabase();
        database.beginTransaction();
        try {
            long accountId;
            try(Cursor c=database.rawQuery("SELECT accountId FROM Student WHERE id=?",new String[]{String.valueOf(id)})) {
                if(!c.moveToFirst())throw new IllegalArgumentException("Không tìm thấy sinh viên");
                accountId=c.getLong(0);
            }
            database.delete("Student","id=?",new String[]{String.valueOf(id)});
            database.delete("Account","id=?",new String[]{String.valueOf(accountId)});
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }
    public String usernameForStudent(long id) {
        try(Cursor c=query("SELECT a.username FROM Student s JOIN Account a ON s.accountId=a.id WHERE s.id=?",String.valueOf(id))){return c.moveToFirst()?c.getString(0):"";}
    }
    public boolean conflicts(long studentId,String date,String start,String end,long excludeId) {
        try(Cursor c=query("SELECT 1 FROM StudySchedule WHERE studentId=? AND studyDate=? AND status!='CANCELLED' AND id!=? AND startTime<? AND endTime>? LIMIT 1",String.valueOf(studentId),date,String.valueOf(excludeId),end,start)) {return c.moveToFirst();}
    }
    public List<Long> upcomingScheduleIds() {
        List<Long> ids=new ArrayList<>();
        try(Cursor c=query("SELECT id FROM StudySchedule WHERE status!='CANCELLED'")){while(c.moveToNext())ids.add(c.getLong(0));}
        return ids;
    }
}
