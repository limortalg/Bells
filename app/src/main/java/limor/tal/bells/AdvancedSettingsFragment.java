package limor.tal.bells;

import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AdvancedSettingsFragment extends Fragment {

    private SchoolDatabaseHelper dbHelper;
    private ActivityResultLauncher<Intent> ringtonePickerLauncher;
    private TextView ringtoneNameTextView;
    private EditText notificationWithoutBreakInput;
    private EditText notificationWithBreakInput;

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
                        Uri ringtoneUri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if (ringtoneUri != null) {
                            dbHelper.saveSettings("ringtone_uri", ringtoneUri.toString());
                            updateRingtoneName(ringtoneUri);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advanced_settings, container, false);

        ringtoneNameTextView = view.findViewById(R.id.ringtone_name_text_view);
        Button selectRingtoneButton = view.findViewById(R.id.select_ringtone_button);
        Button saveButton = view.findViewById(R.id.save_button);
        notificationWithoutBreakInput = view.findViewById(R.id.notification_without_break);
        notificationWithBreakInput = view.findViewById(R.id.notification_with_break);

        // Load current settings
        String ringtoneUriStr = dbHelper.getSettings("ringtone_uri", null);
        if (ringtoneUriStr != null) {
            updateRingtoneName(Uri.parse(ringtoneUriStr));
        } else {
            ringtoneNameTextView.setText("ברירת מחדל");
        }

        String notification1Min = dbHelper.getSettings("notification_without_break", "1");
        String notification3Min = dbHelper.getSettings("notification_with_break", "3");
        notificationWithoutBreakInput.setText(notification1Min);
        notificationWithBreakInput.setText(notification3Min);

        // Save button
        saveButton.setOnClickListener(v -> {
            String value = notificationWithoutBreakInput.getText().toString().trim();
            dbHelper.saveSettings("notification_without_break", value.isEmpty() ? "1" : value);
            value = notificationWithBreakInput.getText().toString().trim();
            dbHelper.saveSettings("notification_with_break", value.isEmpty() ? "3" : value);
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Ringtone selection
        selectRingtoneButton.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
            String existingRingtone = dbHelper.getSettings("ringtone_uri", null);
            if (existingRingtone != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingRingtone));
            }
            ringtonePickerLauncher.launch(intent);
        });

        return view;
    }

    private void updateRingtoneName(Uri ringtoneUri) {
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), ringtoneUri);
            String name = ringtone != null ? ringtone.getTitle(requireContext()) : "Unknown";
            ringtoneNameTextView.setText(name);
        } catch (Exception e) {
            ringtoneNameTextView.setText("שגיאה");
        }
    }
}
