package com.example.activitystreak;

import android.widget.CheckBox;

public class TaskItem {

    public String name;
    public String desc;
    public String time;
    public String date;
    public String taskId;

    public Boolean completed;
    public Boolean failed;
    public Boolean deleted;


    public transient CheckBox checkBox;


    public TaskItem() {
    }


    public TaskItem(String name, String desc, String time, String date,
                    String taskId, Boolean completed, Boolean failed) {

        this.name = name;
        this.desc = desc;
        this.time = time;
        this.date = date;
        this.taskId = taskId;

        this.completed = completed != null && completed;
        this.failed = failed != null && failed;

        this.deleted = false; // default
    }
}