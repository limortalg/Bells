package limor.tal.bells;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SchoolDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "school.db";
    private static final int DATABASE_VERSION = 2;

    public SchoolDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE personal_schedule (" +
                "day INTEGER, lesson_number INTEGER, subject TEXT, teacher_or_group TEXT, building TEXT, room TEXT, photo BLOB, " +
                "PRIMARY KEY (day, lesson_number))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
/*            db.execSQL("DELETE FROM settings");
            db.execSQL("DELETE FROM personal_schedule");
        }*/
    }

    public void saveSettings(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        db.insertWithOnConflict("settings", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getSettings(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("settings", new String[]{"value"}, "key = ?",
                new String[]{key}, null, null, null);
        if (cursor.moveToFirst()) {
            String value = cursor.getString(0);
            cursor.close();
            return value;
        }
        cursor.close();
        return defaultValue;
    }

    public boolean isSetupComplete() {
        return "true".equalsIgnoreCase(getSettings("setup_complete", "false"));
    }

    public boolean isTeacher() {
        return "teacher".equals(getSettings("user_type", "student"));
    }

    public String getSetting(String key) {
        return getSettings(key, null);
    }

    public boolean hasPersonalSchedule() {
        SQLiteDatabase db = this.getReadableDatabase();
        //Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM personal_schedule", null);
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM settings WHERE key LIKE 'day_%'", null);
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            cursor.close();
            return count > 0;
        }
        cursor.close();
        return false;
    }
}