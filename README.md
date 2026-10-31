# Student Study Schedule Management and Booking Application

Dự án Android (Java + SQLite), phát triển nhóm 4 người.

> **Trạng thái repository:** Commit `Initial Android project structure` chỉ chứa **cấu hình chung và cấu trúc package** từ bản `StudentStudyScheduler_UI_Improved`. Các file Java sẽ được từng thành viên thêm qua branch riêng và Pull Request. Trước khi merge các phần Java, ứng dụng **chưa thể chạy**; đây là dự kiến, không phải lỗi.

## Cấu hình chung

- Java/JDK: 17
- Android SDK: compileSdk 35, targetSdk 35, minSdk 26
- Android Gradle Plugin: 8.13.0
- Gradle: 8.13
- Package / applicationId: `com.example.studyscheduler`
- Giao diện: Android Views, theme AppTheme từ bản UI Improved
- Dữ liệu: SQLite và SharedPreferences; chưa có Java nên database chưa được tạo ở commit này.

**Gradle Wrapper:** Bản nguồn UI Improved chỉ có `gradle-wrapper.properties` mà chưa có `gradle-wrapper.jar`, `gradlew`, `gradlew.bat`. Trưởng nhóm chạy `bash scripts/setup_gradle_wrapper.sh` (cần Internet) để lấy các file Wrapper gốc từ mã nguồn chính thức Gradle 8.13 trước khi commit cấu hình.

## Nhóm / nhánh

| Người | Branch đề xuất | Module thêm vào |
|---|---|---|
| 1 | `feature/auth-navigation` | MainActivity, LoginActivity, AdminActivity, StudentActivity, auth/SessionManager, base/BaseActivity, models/Account |
| 2 | `feature/student-subject` | StudentManagementActivity, SubjectManagementActivity, models/Student, models/Subject |
| 3 | `feature/booking-schedule` | BookingActivity, ScheduleActivity, models/StudySchedule |
| 4 | `feature/database-calendar-notifications` | dal/DBHelper, activities/CalendarActivity, notifications/* |

## File dùng chung

- `AndroidManifest.xml` là **bản đầy đủ từ UI Improved**, khai báo trước cả các Activity và Receiver sẽ được thêm ở các PR về sau. **Không sửa/chỉnh tên các class đã khai báo khi chép code thành viên.**
- `styles.xml` là bộ theme màu xanh nhạt của bản UI Improved.
- `build.gradle`, `settings.gradle`, `app/build.gradle`, `gradle.properties` là phần cấu hình Gradle chung.
- `.gitignore` loại trừ file cục bộ (`local.properties`) và thư mục build/IDE.

## Để có ứng dụng chạy được

Merge đầy đủ 4 module, sau đó mở project bằng Android Studio và chạy Sync / Build / Run. Các file `.gitkeep` chỉ để GitHub hiển thị package trống, có thể xóa sau khi thêm code Java.
