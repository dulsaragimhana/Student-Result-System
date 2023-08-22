package studentmanagement;

import io.vertx.core.json.JsonObject;
public class StudentData {
    private int studentId;
    private String studentName;

    public int getStudentId() {
        return studentId;
    }
    public String getStudentName() {
        return studentName;
    }
    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    public StudentData(JsonObject jsonObject) {
        this.studentId = jsonObject.getInteger("studentId");
        this.studentName = jsonObject.getString("studentName");
    }
}