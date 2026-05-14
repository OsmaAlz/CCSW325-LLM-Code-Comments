package com.example.activitystreak;

public class Milestone {
    public String milestoneName;
    public long tasksRequired;

    public Milestone() {}

    public Milestone(String milestoneName, long tasksRequired) {
        this.milestoneName = milestoneName;
        this.tasksRequired = tasksRequired;
    }
}