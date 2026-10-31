package com.example.studyscheduler.models;
/** Person 1/4 contract: model maps 1:1 to Account SQLite table. */
public class Account {
    public long id;
    public String username;
    public String password; // stored in DB as a salted PBKDF2 hash
    public String role;
}
