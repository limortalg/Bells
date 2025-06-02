package limor.tal.bells;

import android.graphics.Bitmap;

public class Lesson {
    private int day;
    private int lessonNumber;
    private String subject;
    private String teacherOrGroup;
    private String building;
    private String room;
    private Bitmap photo;

    public Lesson(int day, int lessonNumber, String subject, String teacherOrGroup, String building, String room, Bitmap photo) {
        this.day = day;
        this.lessonNumber = lessonNumber;
        this.subject = subject;
        this.teacherOrGroup = teacherOrGroup;
        this.building = building;
        this.room = room;
        this.photo = photo;
    }

    public int getDay() {
        return day;
    }

    public int getLessonNumber() {
        return lessonNumber;
    }

    public String getSubject() {
        return subject != null ? subject : "";
    }

    public String getTeacherOrGroup() {
        return teacherOrGroup != null ? teacherOrGroup : "";
    }

    public String getBuilding() {
        return building != null ? building : "";
    }

    public String getRoom() {
        return room != null ? room : "";
    }

    public Bitmap getPhoto() {
        return photo;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTeacherOrGroup(String teacherOrGroup) {
        this.teacherOrGroup = teacherOrGroup;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
    }
}