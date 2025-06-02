package limor.tal.bells;

public class SchoolLesson {
    private int day;
    private int lessonNumber;
    private String subject;
    private String teacherGroup;
    private String building;
    private String room;
    private String startTime;
    private String endTime;
    private String teacherPhotoPath;
    private boolean hasBreakBefore;

    public SchoolLesson(int day, int lessonNumber, String subject, String teacherGroup, String building, String room, String startTime, String endTime, boolean hasBreakBefore) {
        this.day = day;
        this.lessonNumber = lessonNumber;
        this.subject = subject;
        this.startTime = startTime;
        this.endTime = endTime;
        this.teacherGroup = teacherGroup;
        this.building = building;
        this.room = room;
        this.hasBreakBefore = hasBreakBefore;
        this.teacherPhotoPath = ""; // TODO - handle photo
    }

    public int getDay() { return day; }
    public int getLessonNumber() { return lessonNumber; }
    public String getSubject() { return subject; }
    public String getTeacherGroup() { return teacherGroup; }
    public String getBuilding() { return building; }
    public String getRoom() { return room; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public boolean getHasBreakBefore() { return hasBreakBefore; }
    public String getTeacherPhotoPath() { return teacherPhotoPath; }
}