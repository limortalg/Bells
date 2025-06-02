package limor.tal.bells;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;

public class DashboardFragment extends Fragment {

    private SchoolDatabaseHelper dbHelper;
    private static final String[] DAY_NAMES = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};
    private String userType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            dbHelper = ((MainActivity) getActivity()).getDbHelper();
        }
        if (getArguments() != null) {
            userType = getArguments().getString("userType", "student");
        }

        // initialize notifications
        SchoolLesson next = getNextClass();
        String notificationSubject = "Prepare to go to " + next.getBuilding() + ", Room " + next.getRoom() + " for " + next.getSubject();
        AlarmScheduler.scheduleLessonNotification(
                getContext(),
                next.getStartTime(),
                notificationSubject,
                next.getHasBreakBefore()
        );

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        TextView currentClassTextView = view.findViewById(R.id.current_class_text_view);
        TextView nextClassTextView = view.findViewById(R.id.next_class_text_view);
        ImageView currentClassPhoto = view.findViewById(R.id.current_class_photo);
        ImageView nextClassPhoto = view.findViewById(R.id.next_class_photo);
        RecyclerView recyclerView = view.findViewById(R.id.schedule_recycler_view);
        Button menuButton = view.findViewById(R.id.menu_button);

        // Set up popup menu
        menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.dashboard_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                Fragment newFragment = null;
                if (id == R.id.menu_school_config) {
                    newFragment = new SetupFragment();
                } else if (id == R.id.menu_schedule) {
                    Bundle args = new Bundle();
                    args.putInt("currentDay", 1);
                    args.putInt("totalDays", Integer.parseInt(dbHelper.getSettings("total_days", "5")));
                    args.putString("userType", userType);
                    newFragment = new ScheduleFragment();
                    newFragment.setArguments(args);
                } else if (id == R.id.menu_advanced_settings) {
                    newFragment = new AdvancedSettingsFragment();
                }
                if (newFragment != null && getActivity() instanceof MainActivity) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, newFragment)
                            .addToBackStack(null)
                            .commit();
                    ((MainActivity) getActivity()).updateCurrentFragment(newFragment);
                    return true;
                }
                return false;
            });
            popup.show();
        });

        // Get total days
        String totalDaysStr = dbHelper.getSettings("total_days", "5");
        int totalDays = Integer.parseInt(totalDaysStr);

        // Set current and next class with photos
        setCurrentAndNextClass(currentClassTextView, nextClassTextView, currentClassPhoto, nextClassPhoto, totalDays);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        ScheduleAdapter adapter = new ScheduleAdapter(totalDays, userType, dbHelper);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private SchoolLesson getNextClass() {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int totalDays = Integer.parseInt(dbHelper.getSettings("total_days", "5"));
        int currentDay = Math.min(dayOfWeek, totalDays);

        String currentClass = "";
        String nextClass = "";
        String currentPhotoPath = "";
        String nextPhotoPath = "";

        boolean inLesson = false;
        int nextLesson = 1;
        boolean hasBreakBefore = true;

        // Find current class
        for (int lesson = 1; lesson <= 9; lesson++) {
            String startHourStr = dbHelper.getSettings("lesson_" + lesson + "_start_hour", "0");
            String startMinuteStr = dbHelper.getSettings("lesson_" + lesson + "_start_minute", "0");
            String endHourStr = dbHelper.getSettings("lesson_" + lesson + "_end_hour", "0");
            String endMinuteStr = dbHelper.getSettings("lesson_" + lesson + "_end_minute", "0");

            int startHour = Integer.parseInt(startHourStr);
            int startMinute = Integer.parseInt(startMinuteStr);
            int endHour = Integer.parseInt(endHourStr);
            int endMinute = Integer.parseInt(endMinuteStr);

            int startTime = startHour * 60 + startMinute;
            int endTime = endHour * 60 + endMinute;
            int currentTime = currentHour * 60 + currentMinute;

            if (currentTime < startTime) {
                nextLesson = 1;
                break;
            } else {
                if (currentTime >= startTime && currentTime < endTime) {
                    nextLesson = lesson + 1;
                    // TODO check if has break before
                    break;
                }
            }
        }
        if (nextLesson == 10) // next lession is next day
        {
            nextLesson = 1;
            if (currentDay < totalDays)
            {
                currentDay++;
            }
            else {
                // next lesson is the first one next week
                currentDay = 1;
            }
        }

        String subject = dbHelper.getSettings("day_" + currentDay + "_lesson_" + nextLesson + "_subject", "-");
        String room = dbHelper.getSettings("day_" + currentDay + "_lesson_" + nextLesson + "_room", "");
        String building = dbHelper.getSettings("day_" + currentDay + "_lesson_" + nextLesson + "_building", "");
        String teacherGroup = dbHelper.getSettings("day_" + currentDay + "_lesson_" + nextLesson + "_teacher_group", "");

        String startHour = dbHelper.getSettings("lesson_" + nextLesson + "_start_hour", "0");
        String startMinute = dbHelper.getSettings("lesson_" + nextLesson + "_start_minute", "0");
        String endHour= dbHelper.getSettings("lesson_" + nextLesson + "_end_hour", "0");
        String endMinute = dbHelper.getSettings("lesson_" + nextLesson + "_end_minute", "0");
        String startTimeStr = String.format("%s:%s", startHour, startMinute);
        String endTimeStr = String.format("%s:%s", endHour, endMinute);
        return new SchoolLesson(currentDay, nextLesson, subject, teacherGroup, building, room, startTimeStr, endTimeStr, hasBreakBefore);

    }

        private void setCurrentAndNextClass(TextView currentClassTextView, TextView nextClassTextView,
                                        ImageView currentClassPhoto, ImageView nextClassPhoto, int totalDays) {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 2=Monday, etc.
        int currentDay = Math.min(dayOfWeek, totalDays);

        String currentClass = "";
        String nextClass = "";
        String currentPhotoPath = "";
        String nextPhotoPath = "";

        // Find current class
        for (int lesson = 1; lesson <= 9; lesson++) {
            String startHourStr = dbHelper.getSettings("lesson_" + lesson + "_start_hour", "0");
            String startMinuteStr = dbHelper.getSettings("lesson_" + lesson + "_start_minute", "0");
            String endHourStr = dbHelper.getSettings("lesson_" + lesson + "_end_hour", "0");
            String endMinuteStr = dbHelper.getSettings("lesson_" + lesson + "_end_minute", "0");

            int startHour = Integer.parseInt(startHourStr);
            int startMinute = Integer.parseInt(startMinuteStr);
            int endHour = Integer.parseInt(endHourStr);
            int endMinute = Integer.parseInt(endMinuteStr);

            int startTime = startHour * 60 + startMinute;
            int endTime = endHour * 60 + endMinute;
            int currentTime = currentHour * 60 + currentMinute;

            if (currentTime >= startTime && currentTime < endTime) {
                String subject = dbHelper.getSettings("day_" + currentDay + "_lesson_" + lesson + "_subject", "-");
                if (!subject.isEmpty() && !subject.equals("-"))
                {
                    String room = dbHelper.getSettings("day_" + currentDay + "_lesson_" + lesson + "_room", "");
                    String building = dbHelper.getSettings("day_" + currentDay + "_lesson_" + lesson + "_building", "");

                    String time = String.format("%s:%s–%s:%s", startHourStr, startMinuteStr, endHourStr, endMinuteStr);
                    currentClass = lesson + ", " + subject + ", חדר " + room + " ( בניין " + building + " )";
                    currentPhotoPath = dbHelper.getSettings("day_" + currentDay + "_lesson_" + lesson + "_teacher_photo", "");
                }

                // Look for next class
                if (lesson < 9) {
                    String nextSubject = dbHelper.getSettings("day_" + currentDay + "_lesson_" + (lesson + 1) + "_subject", "-");
                    if (nextSubject.isEmpty() || nextSubject.equals("-") || nextSubject.equals("בחר"))
                        continue;
                    String nextRoom = dbHelper.getSettings("day_" + currentDay + "_lesson_" + (lesson+1) + "_room", "");
                    String nextBuilding = dbHelper.getSettings("day_" + currentDay + "_lesson_" + (lesson+1) + "_building", "");
                    String nextStartHour = dbHelper.getSettings("lesson_" + (lesson + 1) + "_start_hour", "0");
                    String nextStartMinute = dbHelper.getSettings("lesson_" + (lesson + 1) + "_start_minute", "0");
                    String nextEndHour = dbHelper.getSettings("lesson_" + (lesson + 1) + "_end_hour", "0");
                    String nextEndMinute = dbHelper.getSettings("lesson_" + (lesson + 1) + "_end_minute", "0");
                    String nextTime = String.format("%s:%s–%s:%s", nextStartHour, nextStartMinute, nextEndHour, nextEndMinute);
                    nextClass = (lesson+1) + ", " + nextSubject + ", חדר " + nextRoom + " ( בניין " + nextBuilding + " )";

                    nextPhotoPath = dbHelper.getSettings("day_" + currentDay + "_lesson_" + (lesson + 1) + "_teacher_photo", "");
                }
                break;
            }
        }

        currentClassTextView.setText("שיעור נוכחי: " + currentClass);
        nextClassTextView.setText("השיעור הבא: " + nextClass);

        // Show teacher photos in student mode
        if ("student".equals(userType)) {
            if (!currentPhotoPath.isEmpty()) {
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                if (bitmap != null) {
                    currentClassPhoto.setImageBitmap(bitmap);
                    currentClassPhoto.setVisibility(View.VISIBLE);
                }
            }
            if (!nextPhotoPath.isEmpty()) {
                Bitmap bitmap = BitmapFactory.decodeFile(nextPhotoPath);
                if (bitmap != null) {
                    nextClassPhoto.setImageBitmap(bitmap);
                    nextClassPhoto.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
