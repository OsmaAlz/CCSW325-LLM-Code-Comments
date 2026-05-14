package com.example.activitystreak;

public class SelectedDateManager {

    private static String selectedDate;

    public static void setSelectedDate(String date) {
        selectedDate = date;
    }

    public static String getSelectedDate() {
        return selectedDate;
    }

    public static void clear() {
        selectedDate = null;
    }
}