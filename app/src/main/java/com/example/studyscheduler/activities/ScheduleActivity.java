package com.example.studyscheduler.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.studyscheduler.base.BaseActivity;
import com.example.studyscheduler.dal.DBHelper;
import com.example.studyscheduler.notifications.ReminderReceiver;
import com.example.studyscheduler.notifications.ReminderScheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Person 3: personal schedules, search, status change and deletion. */
public class ScheduleActivity extends BaseActivity {
    private static final String[] STATUSES = {"ALL", "PENDING", "CONFIRMED", "CANCELLED"};
    private static final String[] STATUS_NAMES = {"Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đã hủy"};
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!guard("STUDENT")) return;
        listSchedules("", "ALL");
    }
    private String statusName(String value) {
        for (int i = 0; i < STATUSES.length; i++) if (STATUSES[i].equals(value)) return STATUS_NAMES[i];
        return value;
    }
    private void listSchedules(String query, String filter) {
        setup("Lịch học của tôi");
        note(content, "Theo dõi các buổi học đã đặt và cập nhật khi cần.");
        navigation("Trang chủ", this::finish);
        primaryButton(content, "+ Đặt lịch học mới", () -> navigate(BookingActivity.class));
        button(content, "Xem lịch ngày / tuần / tháng", () -> go(CalendarActivity.class));

        LinearLayout searchCard = group(content);
        label(searchCard, "Tìm kiếm lịch học");
        EditText search = field(searchCard, "Nhập tên môn, mã môn hoặc địa điểm", query);
        label(searchCard, "Trạng thái");
        Spinner statusPicker = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, STATUS_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusPicker.setAdapter(adapter);
        statusPicker.setPadding(dp(11), dp(9), dp(9), dp(9));
        statusPicker.setMinimumHeight(dp(50));
        searchCard.addView(statusPicker, margins(10));
        for (int i = 0; i < STATUSES.length; i++) if (STATUSES[i].equals(filter)) statusPicker.setSelection(i);
        button(searchCard, "Tìm / Lọc lịch", () -> listSchedules(search.getText().toString().trim(),
                STATUSES[statusPicker.getSelectedItemPosition()]));
        sectionTitle(content, "Danh sách buổi học");
        showSchedules(query, filter);
    }
    private void showSchedules(String query, String filter) {
        StringBuilder sql = new StringBuilder("SELECT sc.*,s.subjectName,s.subjectCode FROM StudySchedule sc "
                + "JOIN Subject s ON sc.subjectId=s.id WHERE sc.studentId=? "
                + "AND (s.subjectName LIKE ? OR s.subjectCode LIKE ? OR sc.location LIKE ?)");
        List<String> args = new ArrayList<>();
        String term = "%" + query + "%";
        args.add(String.valueOf(studentId)); args.add(term); args.add(term); args.add(term);
        if (!"ALL".equals(filter)) { sql.append(" AND sc.status=?"); args.add(filter); }
        sql.append(" ORDER BY sc.studyDate ASC, sc.startTime ASC");
        int count = 0;
        try (Cursor c = db.query(sql.toString(), args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                count++;
                long id = DBHelper.lng(c, "id");
                String st = DBHelper.str(c, "status");
                String day = DBHelper.str(c, "studyDate");
                String start = DBHelper.str(c, "startTime");
                String end = DBHelper.str(c, "endTime");
                LinearLayout card = group(content);
                TextView name = sectionTitle(card, DBHelper.str(c, "subjectName"));
                name.setPadding(0, dp(2), 0, dp(2));
                note(card, DBHelper.str(c, "subjectCode") + "   •   " + LocalDate.parse(day).format(FORMAT));
                text(card, "◷  " + start + " - " + end + "\n⌖  " + DBHelper.str(c, "location"), 15);
                TextView tag = text(card, "Trạng thái: " + statusName(st), 14);
                tag.setTextColor("CANCELLED".equals(st) ? 0xFF98524D :
                        "CONFIRMED".equals(st) ? 0xFF26794A : 0xFF91671F);
                LinearLayout row = horizontal(card);
                Button edit = button(row, "Sửa lịch", () -> {
                    startActivity(new Intent(this, BookingActivity.class).putExtra("scheduleId", id));
                    finish();
                });
                Button change = button(row, "Đổi trạng thái", () -> chooseStatus(id, st));
                equallySized(edit, change);
                button(card, "Xóa lịch", () -> askDelete("lịch học", () -> {
                    ReminderScheduler.cancel(this, id);
                    db.delete("StudySchedule", id);
                    listSchedules(query, filter);
                }));
            }
        }
        if (count == 0) {
            LinearLayout empty = group(content);
            sectionTitle(empty, "Chưa có lịch học phù hợp");
            note(empty, "Bạn có thể thử tìm kiếm khác hoặc thêm một buổi học mới.");
        }
    }
    private void equallySized(Button a, Button b) {
        LinearLayout.LayoutParams left = (LinearLayout.LayoutParams) a.getLayoutParams();
        left.width = 0; left.weight = 1; left.rightMargin = dp(4);
        a.setLayoutParams(left);
        LinearLayout.LayoutParams right = (LinearLayout.LayoutParams) b.getLayoutParams();
        right.width = 0; right.weight = 1; right.leftMargin = dp(4);
        b.setLayoutParams(right);
    }
    private void chooseStatus(long id, String current) {
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật trạng thái")
                .setSingleChoiceItems(new String[]{"Chờ xác nhận", "Đã xác nhận", "Đã hủy"},
                        "PENDING".equals(current) ? 0 : "CONFIRMED".equals(current) ? 1 : 2,
                        (dialog, which) -> {
                            dialog.dismiss();
                            String target = STATUSES[which + 1];
                            if (target.equals(current)) return;
                            try (Cursor c = db.query("SELECT studyDate,startTime,endTime FROM StudySchedule WHERE id=? AND studentId=?",
                                    String.valueOf(id), String.valueOf(studentId))) {
                                if (!c.moveToFirst()) { message("Không tìm thấy lịch học"); return; }
                                if (!"CANCELLED".equals(target) && db.conflicts(studentId,
                                        DBHelper.str(c, "studyDate"), DBHelper.str(c, "startTime"),
                                        DBHelper.str(c, "endTime"), id)) {
                                    new AlertDialog.Builder(this).setTitle("Không thể đổi trạng thái")
                                            .setMessage("Buổi học này bị trùng với một lịch đang hoạt động.")
                                            .setPositiveButton("Đóng", null).show();
                                    return;
                                }
                            }
                            ContentValues cv = new ContentValues();
                            cv.put("status", target);
                            db.update("StudySchedule", cv, id);
                            if ("CANCELLED".equals(target)) ReminderScheduler.cancel(this, id);
                            else ReminderScheduler.schedule(this, db, id);
                            ReminderReceiver.notify(this, (int) id + 100000,
                                    "Trạng thái lịch học thay đổi", "Lịch #" + id + ": " + statusName(target));
                            listSchedules("", "ALL");
                        })
                .setNegativeButton("Đóng", null).show();
    }
}
