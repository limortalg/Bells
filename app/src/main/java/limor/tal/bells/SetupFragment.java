package limor.tal.bells;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SetupFragment extends Fragment {

    private SchoolDatabaseHelper dbHelper;
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;
    private Uri selectedRingtoneUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            dbHelper = ((MainActivity) getActivity()).getDbHelper();
        }

        ringtonePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedRingtoneUri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_setup, container, false);

            // Initialize views
            CheckBox teacherCheckBox = view.findViewById(R.id.teacher_check_box);
            Spinner totalDaysSpinner = view.findViewById(R.id.total_days_spinner);
            LinearLayout lessonTimesContainer = view.findViewById(R.id.lesson_times_container);
            Button saveSettingsButton = view.findViewById(R.id.save_settings_button);

            if (teacherCheckBox == null || totalDaysSpinner == null || lessonTimesContainer == null ||
                    saveSettingsButton == null) {
                Toast.makeText(requireContext(), "Error: UI elements not found", Toast.LENGTH_LONG).show();
                return view;
            }

            // Set up total days spinner
            ArrayAdapter<CharSequence> daysAdapter = ArrayAdapter.createFromResource(
                    requireContext(), R.array.total_days_options, android.R.layout.simple_spinner_item);
            daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            totalDaysSpinner.setAdapter(daysAdapter);

            // Pre-populate fields
            if (dbHelper != null && dbHelper.isSetupComplete()) {
                teacherCheckBox.setChecked(dbHelper.isTeacher());
                String totalDays = dbHelper.getSettings("total_days", "5");
                totalDaysSpinner.setSelection(daysAdapter.getPosition(totalDays));
            }

            // Dynamically add lesson time inputs (1–9)
            for (int i = 1; i <= 9; i++) {
                LinearLayout lessonRow = new LinearLayout(requireContext());
                lessonRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                lessonRow.setOrientation(LinearLayout.HORIZONTAL);

                // Lesson time inputs
                View lessonView = inflater.inflate(R.layout.item_lesson_time, lessonTimesContainer, false);
                TextView lessonNumber = lessonView.findViewById(R.id.lesson_title);
                Spinner startHourSpinner = lessonView.findViewById(R.id.start_hour_spinner);
                Spinner startMinuteSpinner = lessonView.findViewById(R.id.start_minute_spinner);
                Spinner endHourSpinner = lessonView.findViewById(R.id.end_hour_spinner);
                Spinner endMinuteSpinner = lessonView.findViewById(R.id.end_minute_spinner);
                EditText breakDurationInput = lessonView.findViewById(R.id.break_duration_input);

                if (startHourSpinner == null || startMinuteSpinner == null || endHourSpinner == null ||
                        endMinuteSpinner == null || breakDurationInput == null) {
                    Log.e("SetupFragment", "Null view found in item_lesson_time for lesson " + i);
                    continue;
                }

                // Lesson number label
                lessonNumber.setText(String.valueOf(i));

                // Set up spinners
                ArrayAdapter<CharSequence> hourAdapter = ArrayAdapter.createFromResource(
                        requireContext(), R.array.hours, android.R.layout.simple_spinner_item);
                hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                startHourSpinner.setAdapter(hourAdapter);
                endHourSpinner.setAdapter(hourAdapter);

                ArrayAdapter<CharSequence> minuteAdapter = ArrayAdapter.createFromResource(
                        requireContext(), R.array.minutes, android.R.layout.simple_spinner_item);
                minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                startMinuteSpinner.setAdapter(minuteAdapter);
                endMinuteSpinner.setAdapter(minuteAdapter);

                // Pre-populate lesson times
                if (dbHelper != null && dbHelper.isSetupComplete()) {
                    String startHour = dbHelper.getSettings("lesson_" + i + "_start_hour", "0");
                    String startMinute = dbHelper.getSettings("lesson_" + i + "_start_minute", "0");
                    String endHour = dbHelper.getSettings("lesson_" + i + "_end_hour", "0");
                    String endMinute = dbHelper.getSettings("lesson_" + i + "_end_minute", "0");
                    String breakDuration = dbHelper.getSettings("lesson_" + i + "_break_duration", "0");

                    startHourSpinner.setSelection(hourAdapter.getPosition(startHour));
                    startMinuteSpinner.setSelection(minuteAdapter.getPosition(startMinute));
                    endHourSpinner.setSelection(hourAdapter.getPosition(endHour));
                    endMinuteSpinner.setSelection(minuteAdapter.getPosition(endMinute));
                    breakDurationInput.setText(breakDuration);
                }

                lessonRow.addView(lessonView);
                lessonRow.setTag(i);
                lessonTimesContainer.addView(lessonRow);
            }

            // Save settings
            saveSettingsButton.setOnClickListener(v -> {
                boolean isTeacher = teacherCheckBox.isChecked();
                String totalDays = totalDaysSpinner.getSelectedItem().toString();

                dbHelper.saveSettings("user_type", isTeacher ? "teacher" : "student");
                dbHelper.saveSettings("total_days", totalDays);
                dbHelper.saveSettings("setup_complete", "true");

                for (int i = 1; i <= 9; i++) {
                    LinearLayout lessonRow = lessonTimesContainer.findViewWithTag(i);
                    if (lessonRow != null) {
                        Spinner startHourSpinner = lessonRow.findViewById(R.id.start_hour_spinner);
                        Spinner startMinuteSpinner = lessonRow.findViewById(R.id.start_minute_spinner);
                        Spinner endHourSpinner = lessonRow.findViewById(R.id.end_hour_spinner);
                        Spinner endMinuteSpinner = lessonRow.findViewById(R.id.end_minute_spinner);
                        EditText breakDurationInput = lessonRow.findViewById(R.id.break_duration_input);

                        String startHour = startHourSpinner.getSelectedItem().toString();
                        String startMinute = startMinuteSpinner.getSelectedItem().toString();
                        String endHour = endHourSpinner.getSelectedItem().toString();
                        String endMinute = endMinuteSpinner.getSelectedItem().toString();
                        String breakDuration = breakDurationInput.getText().toString().trim();

                        dbHelper.saveSettings("lesson_" + i + "_start_hour", startHour);
                        dbHelper.saveSettings("lesson_" + i + "_start_minute", startMinute);
                        dbHelper.saveSettings("lesson_" + i + "_end_hour", endHour);
                        dbHelper.saveSettings("lesson_" + i + "_end_minute", endMinute);
                        dbHelper.saveSettings("lesson_" + i + "_break_duration", breakDuration.isEmpty() ? "0" : breakDuration);
                    }
                }

                if (selectedRingtoneUri != null) {
                    dbHelper.saveSettings("ringtone_uri", selectedRingtoneUri.toString());
                }

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setUserType(isTeacher);
                }

                ScheduleFragment scheduleFragment = new ScheduleFragment();
                Bundle args = new Bundle();
                args.putInt("currentDay", 1);
                args.putInt("totalDays", Integer.parseInt(totalDays));
                args.putString("userType", isTeacher ? "teacher" : "student");
                scheduleFragment.setArguments(args);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, scheduleFragment)
                        .commit();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateCurrentFragment(scheduleFragment);
                }
            });

            return view;
        } catch (Exception e) {
            Log.e("SetupFragment", "Error in onCreateView", e);
            Toast.makeText(requireContext(), "Failed to load setup screen", Toast.LENGTH_LONG).show();
            return null;
        }
    }
}
