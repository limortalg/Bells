package limor.tal.bells;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class ScheduleManager {

    private SchoolDatabaseHelper dbHelper;
    private Context context;

    public ScheduleManager(Context context) {
        this.context = context;
        this.dbHelper = new SchoolDatabaseHelper(context);
    }

    public List<SchoolLesson> getLessonsForDay(int day, int totalLessons) {
        List<SchoolLesson> lessons = new ArrayList<>();
        for (int lessonNum = 1; lessonNum <= totalLessons; lessonNum++) {
            String startHour = dbHelper.getSettings("lesson_" + lessonNum + "_start_hour", "0");
            String startMinute = dbHelper.getSettings("lesson_" + lessonNum + "_start_minute", "0");
            String endHour = dbHelper.getSettings("lesson_" + lessonNum + "_end_hour", "0");
            String endMinute = dbHelper.getSettings("lesson_" + lessonNum + "_end_minute", "0");
            String startTime = String.format("%s:%s", startHour, startMinute);
            String endTime = String.format("%s:%s", endHour, endMinute);
            String subject = dbHelper.getSettings("day_" + day + "_lesson_" + lessonNum + "_subject", "-");
            String teacherGroup = dbHelper.getSettings("day_" + day + "_lesson_" + lessonNum + "_teacher_group", "");
            String building = dbHelper.getSettings("day_" + day + "_lesson_" + lessonNum + "_building", "");
            String room = dbHelper.getSettings("day_" + day + "_lesson_" + lessonNum + "_room", "");
            if (subject.isEmpty()) {
                subject = "";
                teacherGroup="";
                building = "";
                room = "";
            }
            // TODO - handle photo
            // String teacherPhotoPath = dbHelper.getSettings("day_" + day + "_lesson_" + lessonNum + "_teacher_photo", "");

            lessons.add(new SchoolLesson(day, lessonNum, subject, teacherGroup, building, room, startTime, endTime));
        }
        return lessons;
    }
}
