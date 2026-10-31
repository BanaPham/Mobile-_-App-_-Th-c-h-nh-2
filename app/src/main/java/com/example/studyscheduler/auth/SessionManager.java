package com.example.studyscheduler.auth;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.studyscheduler.dal.DBHelper;

/** Person 1: session only; passwords are never persisted in SharedPreferences. */
public class SessionManager {
    private final SharedPreferences prefs;
    public SessionManager(Context context) { prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE); }
    public void login(long accountId, String role) { prefs.edit().putLong("accountId", accountId).putString("role", role).apply(); }
    public long accountId() { return prefs.getLong("accountId", -1); }
    public String role() { return prefs.getString("role", ""); }
    public void logout() { prefs.edit().clear().apply(); }
    public boolean isValid(DBHelper db) { return accountId() > 0 && db.activeSession(accountId(), role()); }
}
