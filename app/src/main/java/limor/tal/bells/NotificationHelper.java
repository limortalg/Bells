package limor.tal.bells;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import java.util.UUID;

class NotificationHelper {
    private static final String CHANNEL_ID = "bells_channel";
    private Context context;
    private SchoolDatabaseHelper dbHelper;

    public NotificationHelper(Context context) {
        this.context = context;
        this.dbHelper = new SchoolDatabaseHelper(context);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bells Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public void scheduleNotification(Lesson lesson, long triggerTime, boolean withSound, String user_type) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("user_type", user_type);
        intent.putExtra("subject", lesson.getSubject());
        intent.putExtra("teacher_or_group", lesson.getTeacherOrGroup());
        intent.putExtra("building", lesson.getBuilding());
        intent.putExtra("room", lesson.getRoom());
        intent.putExtra("with_sound", withSound);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                UUID.randomUUID().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    public Notification buildNotification(String subject, String content, boolean withSound) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Next Lesson: " + subject)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (withSound) {
            String ringtoneUri = dbHelper.getSettings("ringtone_uri", null);
            if (ringtoneUri != null) {
                builder.setSound(Uri.parse(ringtoneUri));
            } else {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        } else {
            builder.setSound(null);
        }

        return builder.build();
    }
}