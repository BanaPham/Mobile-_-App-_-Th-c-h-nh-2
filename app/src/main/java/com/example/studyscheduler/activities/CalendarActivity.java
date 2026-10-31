package com.example.studyscheduler.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.studyscheduler.base.BaseActivity;
import com.example.studyscheduler.dal.DBHelper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Person 4: agenda grouped by date in daily, weekly and monthly modes. */
public class CalendarActivity extends BaseActivity {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!guard("STUDENT")) return;
        calendarScreen("DAY", LocalDate.now());
    }
    private void calendarScreen(String mode, LocalDate day) {
        setup("Lịch học theo thời gian");
        note(content, "Chọn ngày, tuần hoặc tháng để xem các buổi học.");
        navigation("Lịch học của tôi", this::finish);
        LinearLayout modeRow = horizontal(content);
        Button daily = button(modeRow, "Ngày", () -> calendarScreen("DAY", day));
        Button weekly = button(modeRow, "Tuần", () -> calendarScreen("WEEK", day));
        Button monthly = button(modeRow, "Tháng", () -> calendarScreen("MONTH", day));
        equal(daily, weekly, monthly);
        for (int i = 0; i < 3; i++) {
            Button current = i == 0 ? daily : (i == 1 ? weekly : monthly);
            if ((i == 0 && "DAY".equals(mode)) || (i == 1 && "WEEK".equals(mode)) || (i == 2 && "MONTH".equals(mode))) {
                current.setTextColor(0xFFFFFFFF);
                current.setBackground(shape(BLUE, 11, 0x00000000));
            }
        }
        LocalDate from, to, previous, next;
        switch (mode) {
            case "WEEK":
                from = day.minusDays(day.getDayOfWeek().getValue() - 1); to = from.plusDays(6);
                previous = day.minusWeeks(1); next = day.plusWeeks(1); break;
            case "MONTH":
                from = day.withDayOfMonth(1); to = day.withDayOfMonth(day.lengthOfMonth());
                previous = day.minusMonths(1); next = day.plusMonths(1); break;
            default:
                from = day; to = day; previous = day.minusDays(1); next = day.plusDays(1);
        }
        LinearLayout period = group(content);
        sectionTitle(period, from.format(DATE) + (from.equals(to) ? "" : "\nđến " + to.format(DATE)));
        LinearLayout moves = horizontal(period);
        Button before = button(moves, "← Trước", () -> calendarScreen(mode, previous));
        Button today = button(moves, "Hôm nay", () -> calendarScreen(mode, LocalDate.now()));
        Button after = button(moves, "Sau →", () -> calendarScreen(mode, next));
        equal(before, today, after);

        boolean found = false;
        for (LocalDate current = from; !current.isAfter(to); current = current.plusDays(1)) {
            if (hasSchedule(current)) {
                found = true;
                TextView heading = sectionTitle(content, current.format(DATE));
                heading.setPadding(dp(2), dp(10), 0, dp(8));
                displayDay(current);
            }
        }
        if (!found) {
            LinearLayout empty = group(content);
            sectionTitle(empty, "Chưa có lịch học");
            note(empty, "Không có buổi học nào trong khoảng thời gian đã chọn.");
            button(empty, "+ Đặt lịch học", () -> navigate(BookingActivity.class));
        }
    }
    private void equal(Button... buttons) {
        for (Button button : buttons) {
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) button.getLayoutParams();
            p.width = 0; p.weight = 1;
            p.leftMargin = dp(3); p.rightMargin = dp(3);
            button.setLayoutParams(p);
            button.setTextSize(13);
        }
    }
    private boolean hasSchedule(LocalDate date) {
        try (Cursor c = db.query("SELECT 1 FROM StudySchedule WHERE studentId=? AND studyDate=? LIMIT 1",
                String.valueOf(studentId), date.toString())) { return c.moveToFirst(); }
    }
    private void displayDay(LocalDate date) {
        try (Cursor c = db.query("SELECT sc.id,sc.startTime,sc.endTime,sc.location,sc.status,s.subjectCode,s.subjectName " +
                "FROM StudySchedule sc JOIN Subject s ON s.id=sc.subjectId " +
                "WHERE sc.studentId=? AND sc.studyDate=? ORDER BY sc.startTime",
                String.valueOf(studentId), date.toString())) {
            while (c.moveToNext()) {
                long id = DBHelper.lng(c, "id");
                LinearLayout card = group(content);
                sectionTitle(card, DBHelper.str(c, "subjectCode") + " — " + DBHelper.str(c, "subjectName"));
                text(card, "◷  " + DBHelper.str(c, "startTime") + " - " + DBHelper.str(c, "endTime"), 15);
                String status = DBHelper.str(c, "status");
                String statusLabel = "CONFIRMED".equals(status) ? "Đã xác nhận"
                        : "CANCELLED".equals(status) ? "Đã hủy" : "Chờ xác nhận";
                note(card, "Địa điểm: " + DBHelper.str(c, "location") + "   •   " + statusLabel);
                button(card, "Sửa lịch", () -> {
                    startActivity(new Intent(this, BookingActivity.class).putExtra("scheduleId", id));
                    finish();
                });
            }
        }
    }
}
