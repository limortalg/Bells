package limor.tal.bells;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "school_notifications";
    private static final String CHANNEL_NAME = "School Bells";

    @Override
    public void onReceive(Context context, Intent intent) {
        int lessonNumber = intent.getIntExtra("lesson_number", -1);
        String ringtoneUri = intent.getStringExtra("ringtone_uri");

        // Create notification channel
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        String userType = intent.getStringExtra("user_type");
        String subject = intent.getStringExtra("subject");
        String teacherOrGroup = intent.getStringExtra("teacher_or_group");
        String building = intent.getStringExtra("building");
        String room = intent.getStringExtra("room");

        String title = "Lesson " + lessonNumber + " Starting";
        String content = subject + " in " + building + ", Room " + room;
        if ("teacher".equals(userType)) {
            content += ", Group: " + (teacherOrGroup != null ? teacherOrGroup : "N/A");
        } else {
            content += ", Teacher: " + (teacherOrGroup != null ? teacherOrGroup : "N/A");
        }

        builder.setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Set ringtone
        if (ringtoneUri != null) {
            builder.setSound(Uri.parse(ringtoneUri));
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        }

        // Show notification
        int notificationId = (lessonNumber * 10);
        notificationManager.notify(notificationId, builder.build());

        // Play ringtone
        if (ringtoneUri != null) {
            try {
                Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(ringtoneUri));
                if (ringtone != null) {
                    ringtone.play();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
