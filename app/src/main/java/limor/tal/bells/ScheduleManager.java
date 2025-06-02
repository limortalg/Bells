package limor.tal.bells;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ScheduleManager {

    private SchoolDatabaseHelper dbHelper;
    private Context context;

    public ScheduleManager(Context context) {
        this.context = context;
        this.dbHelper = new SchoolDatabaseHelper(context);
    }
}
