package limor.tal.bells;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class AlarmScheduler {

    public static void scheduleLessonNotification(Context context, String time, String subject, boolean hasBreakBefore) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("subject", subject);
        intent.putExtra("withSound", hasBreakBefore);

        long timeMillis = getTimeInMillis(time) - (hasBreakBefore ? 3 : 1) * 60 * 1000;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) timeMillis,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
    }

    public static long getTimeInMillis(String timeString) {
        String[] parts = timeString.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Optional: if the time is already passed for today, move to tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return calendar.getTimeInMillis();
    }

}



