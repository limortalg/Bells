package limor.tal.bells;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ScheduleFragment extends Fragment {

    private SchoolDatabaseHelper dbHelper;
    private int currentDay;
    private int totalDays;
    private String userType;
    private static final String[] DAY_NAMES = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי"};
    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private int currentLessonForPhoto;
    private int currentLessonForSpeech;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            dbHelper = ((MainActivity) getActivity()).getDbHelper();
        }
        if (getArguments() != null) {
            currentDay = getArguments().getInt("currentDay", 1);
            totalDays = getArguments().getInt("totalDays", 5);
            userType = getArguments().getString("userType", "student");
        }

        // Initialize speech recognition
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> speechResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (speechResults != null && !speechResults.isEmpty()) {
                            String spokenText = speechResults.get(0);
                            setSpeechResult(spokenText); // Call your method with the result
                        }
                    }
                }
        );

        if (!userType.equals("teacher"))
        {
            // Initialize photo picker launcher
            photoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();
                    if (photoUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), photoUri);
                            setTeacherPhotoForLesson(currentLessonForPhoto, bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        TextView currentDayTextView = view.findViewById(R.id.current_day_text_view);
        LinearLayout lessonContainer = view.findViewById(R.id.lesson_container);

        // Set current day indication
        String dayName = currentDay <= DAY_NAMES.length ? DAY_NAMES[currentDay - 1] : "Day " + currentDay;
        currentDayTextView.setText("מערכת שעות ליום " + dayName);

        // Clear existing views to prevent duplicates
        lessonContainer.removeAllViews();

        // TODO - add header

        // Add lesson inputs (1–9)
        for (int i = 1; i <= 9; i++) {
            View lessonRow = inflater.inflate(R.layout.lesson_row, lessonContainer, false);

            TextView lessonLabel = lessonRow.findViewById(R.id.lesson_label);
            Spinner subjectSpinner = lessonRow.findViewById(R.id.subject_spinner);
            ImageView teacherPhoto = lessonRow.findViewById(R.id.teacher_photo);
            Button setPhotoButton = lessonRow.findViewById(R.id.set_photo_button);
            EditText teacherGroupInput = lessonRow.findViewById(R.id.teacher_group_input);
            EditText building = lessonRow.findViewById(R.id.building_input);
            EditText room = lessonRow.findViewById(R.id.room_input);

            lessonLabel.setText(String.valueOf(i));
            //subjectSpinner.setTag(i);

            // Set up subject spinner
            ArrayAdapter<CharSequence> subjectAdapter = ArrayAdapter.createFromResource(
                    requireContext(), R.array.school_subjects, android.R.layout.simple_spinner_item);
            subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            subjectSpinner.setAdapter(subjectAdapter);
            subjectSpinner.setSelection(0);

            if (userType.equals("teacher")) {
                teacherGroupInput.setHint("קבוצה");
                teacherPhoto.setVisibility(View.GONE);
                setPhotoButton.setVisibility(View.GONE);
            }
            else
                teacherGroupInput.setHint("מורה");

            if (dbHelper != null && dbHelper.isSetupComplete() && dbHelper.hasPersonalSchedule()) {

                // Load saved subject
                String savedSubject = dbHelper.getSettings("day_" + currentDay + "_lesson_" + i + "_subject", "");
                if (!savedSubject.isEmpty())
                    subjectSpinner.setSelection(subjectAdapter.getPosition(savedSubject));

                // Load saved building
                String savedBuilding = dbHelper.getSettings("day_" + currentDay + "_lesson_" + i + "_building", "");
                if (!savedBuilding.isEmpty())
                    building.setText(savedBuilding);

                // Load saved room
                String savedRoom = dbHelper.getSettings("day_" + currentDay + "_lesson_" + i + "_room", "");
                if (!savedRoom.isEmpty())
                    room.setText(savedRoom);

                // Load saved teacher/group
                String savedTeacherGroup = dbHelper.getSettings("day_" + currentDay + "_lesson_" + i + "_teacher_group", "");
                if (!savedTeacherGroup.isEmpty())
                    teacherGroupInput.setText(savedTeacherGroup);

                // Load saved teacher photo
                String savedPhotoPath = dbHelper.getSettings("day_" + currentDay + "_lesson_" + i + "_teacher_photo", "");
                if (!savedPhotoPath.isEmpty()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(savedPhotoPath);
                    if (bitmap != null) {
                        teacherPhoto.setImageBitmap(bitmap);
                    }
                }
            }

            // Set photo button listener
            // TODO add camera !!
            final int lesson = i;
            setPhotoButton.setOnClickListener(v -> {
                currentLessonForPhoto = lesson;
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                photoPickerLauncher.launch(intent);
            });

            ImageButton speechInputButton = lessonRow.findViewById(R.id.speech_input_button); // Assuming you add this ID

            if (speechInputButton != null) {
                speechInputButton.setOnClickListener(v -> {
                    currentLessonForSpeech = lesson; // Store the index of the row where speech was initiated
                    // Launch speech recognition intent
                    try {
                        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
                        // Make sure speechRecognizerLauncher is initialized in onCreate
                        if (speechRecognizerLauncher != null) {
                            speechRecognizerLauncher.launch(speechIntent);
                        } else {
                            Toast.makeText(getContext(), "Speech recognizer not available", Toast.LENGTH_SHORT).show();
                        }
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), "Speech recognition not supported on this device.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            lessonContainer.addView(lessonRow);
        }

        // Save settings
        Button saveSettingsButton = view.findViewById(R.id.save_settings_button);

        if (currentDay == totalDays)
        {
            saveSettingsButton.setText("שמור (סיים)");
        }

        saveSettingsButton.setOnClickListener(v -> {

            for (int i = 1; i <= 9; i++) {
                //LinearLayout lessonRow = lessonContainer.findViewWithTag(i);
                LinearLayout lessonRow = (LinearLayout) lessonContainer.getChildAt(i - 1);
                if (lessonRow != null) {
                    Spinner subjectSpinner = lessonRow.findViewById(R.id.subject_spinner);
                    String subject = subjectSpinner.getSelectedItem().toString();

                    EditText teacherGroupInput = lessonRow.findViewById(R.id.teacher_group_input);
                    EditText buildingInput = lessonRow.findViewById(R.id.building_input);
                    EditText roomInput = lessonRow.findViewById(R.id.room_input);

                    String building = buildingInput.getText().toString().trim();
                    String room = roomInput.getText().toString().trim();
                    String teacherGroup = teacherGroupInput.getText().toString().trim();

                    if (subject.equals("בחר"))
                    {
                        subject = "";
                        building = "";
                        room = "";
                        teacherGroup = "";
                    }
                    // TODO handle photo
                    //ImageView teacherPhoto = lessonRow.findViewById(R.id.teacher_photo);
                    //Button setPhotoButton = lessonRow.findViewById(R.id.set_photo_button);

                    dbHelper.saveSettings("day_" + currentDay + "_lesson_" + i + "_subject", subject);
                    dbHelper.saveSettings("day_" + currentDay + "_lesson_" + i + "_building", building);
                    dbHelper.saveSettings("day_" + currentDay + "_lesson_" + i + "_room", room);
                    dbHelper.saveSettings("day_" + currentDay + "_lesson_" + i + "_teacher_group", teacherGroup);

                    // handle notifcations
                    NotificationHelper notificationHelper = new NotificationHelper(requireContext());
                    // TODO - calulate next lesson time - should be in the helper to allow re-calculating?

                }
            }

            if (currentDay == totalDays)
            {
                DashboardFragment dashboardFragment = new DashboardFragment();
                Bundle args = new Bundle();
                args.putInt("totalDays", totalDays);
                args.putString("userType", userType);
                dashboardFragment.setArguments(args);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, dashboardFragment)
                        .commit();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateCurrentFragment(dashboardFragment);
                }
            }
            else {
                ScheduleFragment scheduleFragment = new ScheduleFragment();

                Bundle args = new Bundle();
                args.putInt("currentDay", currentDay + 1);
                args.putInt("totalDays", totalDays);
                args.putString("userType", userType);
                scheduleFragment.setArguments(args);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, scheduleFragment)
                        .commit();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateCurrentFragment(scheduleFragment);
                }
            }
        });
        return view;
    }

    @Override
    public void onPause() {

        super.onPause();

        // Save subjects and teacher/group
        LinearLayout lessonContainer = getView().findViewById(R.id.lesson_container);

        if (lessonContainer != null) {
            for (int i = 1; i <= Math.min(9, lessonContainer.getChildCount()); i++) {
                LinearLayout lessonRow = (LinearLayout) lessonContainer.getChildAt(i - 1);
                Spinner subjectSpinner = lessonRow.findViewById(R.id.subject_spinner);
                String subject = subjectSpinner.getSelectedItem().toString();
                dbHelper.saveSettings("day_" + currentDay + "_lesson_" + i + "_subject", subject);
                String teacherGroup = ((EditText)lessonRow.findViewById(R.id.teacher_group_input)).getText().toString();
                dbHelper.saveSettings("day_" + currentDay + "_lesson_" + i + "_teacher_group", teacherGroup);
                String building = ((EditText)lessonRow.findViewById(R.id.building_input)).getText().toString();
                dbHelper.saveSettings("day_" + currentDay + "_lesson_" + i + "_building", building);
                String room = ((EditText)lessonRow.findViewById(R.id.room_input)).getText().toString();
                dbHelper.saveSettings("day_" + currentDay + "_lesson_" + i + "_room", room);
            }
        }
    }

    public void setSpeechResult(String speechResult) {
        if (getView() == null || currentLessonForSpeech < 1 || currentLessonForSpeech > 9) return;

        LinearLayout lessonContainer = getView().findViewById(R.id.lesson_container);
        if (lessonContainer == null || lessonContainer.getChildCount() < currentLessonForSpeech) {
            return; // Prevent IndexOutOfBoundsException
        }
        LinearLayout lessonRow = (LinearLayout) lessonContainer.getChildAt(currentLessonForSpeech - 1);
        EditText teacherGroupInput = lessonRow.findViewById(R.id.teacher_group_input);
        teacherGroupInput.setText(speechResult);
    }

    public void setTeacherPhotoForLesson(int lesson, Bitmap bitmap) {
        if (getView() == null || lesson < 1 || lesson > 9 || bitmap == null) return;

        LinearLayout lessonContainer = getView().findViewById(R.id.lesson_container);
        if (lessonContainer == null || lessonContainer.getChildCount() < lesson) {
            return; // Prevent IndexOutOfBoundsException
        }

        LinearLayout lessonRow = (LinearLayout) lessonContainer.getChildAt(lesson - 1);
        ImageView teacherPhoto = lessonRow.findViewById(R.id.teacher_photo);

        if (teacherPhoto == null) {
            return; // Prevent NullPointerException
        }

        try {
            // Set bitmap to ImageView
            teacherPhoto.setImageBitmap(bitmap);

            // Save bitmap to internal storage
            String fileName = "teacher_photo_day_" + currentDay + "_lesson_" + lesson + ".png";
            File file = new File(requireContext().getFilesDir(), fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            // Save file path to database
            dbHelper.saveSettings("day_" + currentDay + "_lesson_" + lesson + "_teacher_photo", file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
