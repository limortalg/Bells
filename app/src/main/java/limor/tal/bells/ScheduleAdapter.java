package limor.tal.bells;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_LESSON = 1;

    private final int totalDays;
    private final List<List<SchoolLesson>> scheduleData;
    private final String[] DAY_NAMES = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי"};
    private final String userType;
    private final SchoolDatabaseHelper dbHelper;

    public ScheduleAdapter(int totalDays, String userType, SchoolDatabaseHelper dbHelper) {
        this.totalDays = totalDays;
        this.userType = userType;
        this.dbHelper = dbHelper;
        this.scheduleData = new ArrayList<>();

        // Populate lessons (1–9)
        for (int lesson = 1; lesson <= 9; lesson++) {
            List<SchoolLesson> lessonRow = new ArrayList<>();
            for (int day = 1; day <= totalDays; day++) {
                String subject = dbHelper.getSettings("day_" + day + "_lesson_" + lesson + "_subject", "-");
                String startHour = dbHelper.getSettings("lesson_" + lesson + "_start_hour", "0");
                String startMinute = dbHelper.getSettings("lesson_" + lesson + "_start_minute", "0");
                String endHour = dbHelper.getSettings("lesson_" + lesson + "_end_hour", "0");
                String endMinute = dbHelper.getSettings("lesson_" + lesson + "_end_minute", "0");
                String time = String.format("%s:%s–%s:%s", startHour, startMinute, endHour, endMinute);
                String teacherGroup = dbHelper.getSettings("day_" + day + "_lesson_" + lesson + "_teacher_group", "");
                String building = dbHelper.getSettings("day_" + day + "_lesson_" + lesson + "_building", "");
                String room = dbHelper.getSettings("day_" + day + "_lesson_" + lesson + "_room", "");
                // TODO - check if has break before

                lessonRow.add(new SchoolLesson(day, lesson, subject, teacherGroup, building, room, startHour + ":" + startMinute, endHour + ":" + endMinute, true));
            }
            scheduleData.add(lessonRow);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_LESSON;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_row, parent, false);
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(view);
        } else {
            return new LessonViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.lessonTime.setText("");
            headerHolder.subjectContainer.removeAllViews();

            int spaceBefore = 80; // TODO - better implementation
            for (int day = 1; day <= totalDays; day++) {
                TextView dayView = new TextView(holder.itemView.getContext());
                dayView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                dayView.setText(day <= DAY_NAMES.length ? DAY_NAMES[day - 1] : "Day " + day);
                dayView.setPadding(0, 0, spaceBefore, 0);
                //dayView.setPadding(0, 0, 0, 0);
                spaceBefore = 80;
                dayView.setTextSize(14);
                headerHolder.subjectContainer.addView(dayView);
            }
        } else {
            LessonViewHolder lessonHolder = (LessonViewHolder) holder;
            List<SchoolLesson> lessons = scheduleData.get(position - 1);
            SchoolLesson firstLesson = lessons.get(0);
            lessonHolder.lessonTime.setSingleLine(false);
            //lessonHolder.lessonTime.setText(firstLesson.getLessonNumber() + " (" + firstLesson.getStartTime() + "–" + firstLesson.getEndTime() + ")");
            lessonHolder.lessonTime.setText(firstLesson.getLessonNumber() + "\n" + firstLesson.getStartTime() + "\n" + firstLesson.getEndTime() + "");
            lessonHolder.subjectContainer.removeAllViews();

            for (SchoolLesson lesson : lessons) {
                LinearLayout cell = new LinearLayout(holder.itemView.getContext());
                cell.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setPadding(0, 0, 40, 0);
                //cell.setPadding(0, 0, 0, 0);

                TextView subjectView = new TextView(holder.itemView.getContext());
                if (!lesson.getSubject().isEmpty() && !lesson.getSubject().equals("-") && !lesson.getSubject().equals("בחר"))
                {
                    subjectView.setText(lesson.getSubject() + "\n" + lesson.getTeacherGroup() + "\n" + lesson.getBuilding() + " (" + lesson.getRoom() + ")");
                }
                else
                    subjectView.setText("");
                subjectView.setTextSize(12);
                subjectView.setSingleLine(false);
                subjectView.setHorizontallyScrolling(false);
                cell.addView(subjectView);

                if ("student".equals(userType) && !lesson.getTeacherGroup().isEmpty()) {
                    TextView teacherView = new TextView(holder.itemView.getContext());
                    teacherView.setText(lesson.getTeacherGroup());
                    teacherView.setTextSize(12);
                    cell.addView(teacherView);
                }

                lessonHolder.subjectContainer.addView(cell);
            }
        }
    }

    @Override
    public int getItemCount() {
        return scheduleData.size() + 1; // +1 for header
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView lessonTime;
        LinearLayout subjectContainer;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            lessonTime = itemView.findViewById(R.id.lesson_time);
            subjectContainer = itemView.findViewById(R.id.subject_container);
        }
    }

    public static class LessonViewHolder extends RecyclerView.ViewHolder {
        TextView lessonTime;
        LinearLayout subjectContainer;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            lessonTime = itemView.findViewById(R.id.lesson_time);
            subjectContainer = itemView.findViewById(R.id.subject_container);
        }
    }
}