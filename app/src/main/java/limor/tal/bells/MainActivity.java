package limor.tal.bells;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SchoolDatabaseHelper dbHelper;
    private Fragment currentFragment;
    private int currentLessonPosition;
    private String userType;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> audioPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new SchoolDatabaseHelper(this);

        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty() && currentFragment instanceof ScheduleFragment) {
                            ((ScheduleFragment) currentFragment).setSpeechResult(matches.get(0));
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        if (currentFragment instanceof ScheduleFragment) {
                            ((ScheduleFragment) currentFragment).setTeacherPhotoForLesson(currentLessonPosition, photo);
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(selectedImage);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if (currentFragment instanceof ScheduleFragment) {
                                ((ScheduleFragment) currentFragment).setTeacherPhotoForLesson(currentLessonPosition, bitmap);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        dispatchTakePictureIntent();
                    } else {
                        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startSpeechRecognition();
                    } else {
                        Toast.makeText(this, "Audio permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        if (savedInstanceState == null) {
            if (dbHelper.isSetupComplete() && dbHelper.hasPersonalSchedule()) {
                DashboardFragment dashboardFragment = new DashboardFragment();
                Bundle args = new Bundle();
                args.putString("userType", dbHelper.isTeacher() ? "teacher" : "student");
                dashboardFragment.setArguments(args);
                currentFragment = dashboardFragment;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, dashboardFragment)
                        .commit();
            } else {
                SetupFragment setupFragment = new SetupFragment();
                currentFragment = setupFragment;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, setupFragment)
                        .commit();
            }
        }
    }

    public void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void dispatchGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    public void setCurrentLessonPosition(int position) {
        this.currentLessonPosition = position;
    }

    public void updateCurrentFragment(Fragment fragment) {
        this.currentFragment = fragment;
    }

    public SchoolDatabaseHelper getDbHelper() {
        return dbHelper;
    }

    public void setUserType(boolean isTeacher) {
        userType = isTeacher ? "teacher" : "student";
        dbHelper.saveSettings("user_type", userType);
    }

    public String getUserType() {
        if (userType == null) {
            userType = dbHelper.getSetting("user_type");
            if (userType == null) {
                userType = "student";
            }
        }
        return userType;
    }

    public void requestSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    public void requestCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    public void requestGallery() {
        dispatchGalleryIntent();
    }
}