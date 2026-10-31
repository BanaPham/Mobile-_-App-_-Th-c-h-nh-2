package com.example.studyscheduler.models;
/** Person 2: matches Subject table. */
public class Subject {
    public long id;
    public String subjectCode, subjectName, description;
    public int credits;
    @Override public String toString() { return subjectCode+" — "+subjectName; }
}
