package com.example.studyscheduler.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Person 3: booking form with clear sections and automatic time-conflict checks. */
public class BookingActivity extends BaseActivity {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!guard("STUDENT")) return;
        scheduleForm(getIntent().getLongExtra("scheduleId", -1));
    }
    private static class Item {
        long id;
        String label;
        Item(long id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }
    private List<Item> subjects() {
        List<Item> list = new ArrayList<>();
        try (Cursor cursor = db.query("SELECT id,subjectCode,subjectName FROM Subject ORDER BY subjectName")) {
            while (cursor.moveToNext())
                list.add(new Item(cursor.getLong(0), cursor.getString(1) + " — " + cursor.getString(2)));
        }
        return list;
    }
    private String localizedStatus(String value) {
        if ("CONFIRMED".equals(value)) return "Đã xác nhận";
        if ("CANCELLED".equals(value)) return "Đã hủy";
        return "Chờ xác nhận";
    }
    private void scheduleForm(long id) {
        final boolean editing = id >= 0;
        setup(editing ? "Sửa lịch học" : "Đặt lịch học");
        note(content, editing ? "Cập nhật thông tin cho buổi học của bạn." :
                "Tạo một buổi học mới chỉ với vài thông tin cơ bản.");
        navigation("Lịch học của tôi", () -> navigate(ScheduleActivity.class));

        List<Item> items = subjects();
        if (items.isEmpty()) {
            LinearLayout empty = group(content);
            sectionTitle(empty, "Chưa có môn học");
            note(empty, "Hãy liên hệ quản trị viên để thêm môn học trước khi đặt lịch.");
            return;
        }

        long chosen = items.get(0).id;
        String date = LocalDate.now().toString();
        String start = "08:00", end = "09:00", place = "", status = "CONFIRMED";
        if (editing) {
            try (Cursor cursor = db.query("SELECT * FROM StudySchedule WHERE id=? AND studentId=?",
                    String.valueOf(id), String.valueOf(studentId))) {
                if (!cursor.moveToFirst()) {
                    message("Không tìm thấy lịch học"); navigate(ScheduleActivity.class); return;
                }
                chosen = DBHelper.lng(cursor, "subjectId");
                date = DBHelper.str(cursor, "studyDate");
                start = DBHelper.str(cursor, "startTime");
                end = DBHelper.str(cursor, "endTime");
                place = DBHelper.str(cursor, "location");
                status = DBHelper.str(cursor, "status");
            }
        }
        final String savedStatus = status;
        final Runnable[] refresh = new Runnable[1];
        LinearLayout detail = group(content);
        sectionTitle(detail, "01  Thông tin buổi học");
        label(detail, "Môn học");
        Spinner subject = new Spinner(this);
        ArrayAdapter<Item> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subject.setAdapter(adapter);
        subject.setPadding(dp(11), dp(9), dp(10), dp(9));
        subject.setMinimumHeight(dp(52));
        detail.addView(subject, margins(10));
        for (int i = 0; i < items.size(); i++) if (items.get(i).id == chosen) subject.setSelection(i);
        subject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long rowId) {
                if (refresh[0] != null) refresh[0].run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        label(detail, "Ngày học");
        Button day = dateButton(detail, date, d -> { if (refresh[0] != null) refresh[0].run(); });
        LinearLayout times = horizontal(detail);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, -2, 1);
        leftParams.rightMargin = dp(6);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, -2, 1);
        rightParams.leftMargin = dp(6);
        times.addView(left, leftParams);
        times.addView(right, rightParams);
        label(left, "Giờ bắt đầu");
        Button startButton = timeButton(left, start, t -> { if (refresh[0] != null) refresh[0].run(); });
        label(right, "Giờ kết thúc");
        Button endButton = timeButton(right, end, t -> { if (refresh[0] != null) refresh[0].run(); });
        label(detail, "Địa điểm học");
        EditText location = field(detail, "VD: Phòng A101, thư viện...", place);
        location.setSingleLine(true);
        note(detail, "* Các trường trên đều cần được điền đầy đủ.");

        LinearLayout previewCard = group(content);
        sectionTitle(previewCard, "02  Xem trước lịch học");
        TextView preview = text(previewCard, "", 15);
        preview.setLineSpacing(dp(5), 1f);
        TextView statusNote = note(previewCard, editing
                ? "Trạng thái hiện tại: " + localizedStatus(savedStatus) + ". Đổi trạng thái ở màn hình Lịch học của tôi."
                : "Trạng thái sau khi lưu: Đã xác nhận.");
        statusNote.setTextColor(BLUE);
        refresh[0] = () -> {
            Item item = (Item) subject.getSelectedItem();
            if (item == null) return;
            String locationText = location.getText().toString().trim();
            String summary = item.label + "\n"
                    + "Ngày: " + LocalDate.parse(day.getTag().toString()).format(DATE_FORMAT) + "\n"
                    + "Thời gian: " + startButton.getTag() + " - " + endButton.getTag() + "\n"
                    + "Địa điểm: " + (locationText.isEmpty() ? "Chưa nhập" : locationText);
            preview.setText(summary);
        };
        refresh[0].run();
        location.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refresh[0].run(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        note(content, "Ứng dụng sẽ tự động kiểm tra trùng lịch trước khi lưu.");
        primaryButton(content, editing ? "Lưu thay đổi" : "Xác nhận đặt lịch", () -> {
            String dd = day.getTag().toString();
            String ss = startButton.getTag().toString();
            String ee = endButton.getTag().toString();
            String ll = location.getText().toString().trim();
            if (ll.isEmpty()) {
                location.setError("Vui lòng nhập địa điểm học");
                location.requestFocus();
                return;
            }
            if (ss.compareTo(ee) >= 0) {
                new AlertDialog.Builder(this).setTitle("Thời gian chưa hợp lệ")
                        .setMessage("Giờ kết thúc cần muộn hơn giờ bắt đầu.")
                        .setPositiveButton("Đã hiểu", null).show();
                return;
            }
            try {
                LocalDate.parse(dd); LocalTime.parse(ss); LocalTime.parse(ee);
                // Do not reactivate a cancelled booking merely by editing its details.
                if (!"CANCELLED".equals(savedStatus) && db.conflicts(studentId, dd, ss, ee, id)) {
                    new AlertDialog.Builder(this).setTitle("Lịch học bị trùng")
                            .setMessage("Bạn đã có một lịch học trong khoảng " + ss + " - " + ee
                                    + " ngày " + LocalDate.parse(dd).format(DATE_FORMAT)
                                    + ". Hãy chọn thời gian khác.")
                            .setPositiveButton("Chọn lại giờ", null).show();
                    return;
                }
                ContentValues values = new ContentValues();
                values.put("studentId", studentId);
                values.put("subjectId", ((Item) subject.getSelectedItem()).id);
                values.put("studyDate", dd);
                values.put("startTime", ss);
                values.put("endTime", ee);
                values.put("location", ll);
                values.put("status", savedStatus);
                long saved = editing ? id : db.insert("StudySchedule", values);
                if (editing) db.update("StudySchedule", values, id);
                ReminderScheduler.schedule(this, db, saved);
                ReminderReceiver.notify(this, (int) saved + 100000, "Lịch học được cập nhật",
                        dd + " " + ss + " - " + ee + " (" + savedStatus + ")");
                message(editing ? "Đã cập nhật lịch học" : "Đặt lịch học thành công");
                navigate(ScheduleActivity.class);
            } catch (Exception ex) { fail(ex); }
        });
        button(content, "Quay lại", () -> navigate(ScheduleActivity.class));
    }
}
