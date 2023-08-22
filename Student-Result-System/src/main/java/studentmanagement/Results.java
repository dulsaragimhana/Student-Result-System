package studentmanagement;

import io.vertx.core.json.JsonObject;

public class Results {
    private int studentId;
    private int marks;
    private String grade;
    public int getStudentId() {
        return studentId;
    }
    public int getMarks() {
        return marks;
    }
    public String getGrade(){
        return grade;
    }

    public void setStudentID(int studentID) {
        this.studentId =studentId;
    }
    public void setMarks(int marks) {
        this.marks = marks;
    }
    public void setGrade(String grade){
        this.grade = grade;
    }

    public Results(JsonObject jsonObject) {
        this.studentId = Integer.parseInt(jsonObject.getString("studentId"));
        this.marks = Integer.parseInt(jsonObject.getString("marks"));
        this.grade = jsonObject.getString("grade");
    }

}
