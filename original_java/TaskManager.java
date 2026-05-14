package com.example.activitystreak;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TaskManager {

    private static final String MILESTONE_PATH = "milestones/milestones";

    public static void completeTask(String date, String taskId, Runnable onSuccess) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(date)
                .child(taskId);


        taskRef.child("completed").setValue(true);
        taskRef.child("failed").setValue(false);


        updateStreakAfterCompletion(onSuccess);
    }

    public static void markTaskFailed(String date, String taskId, Runnable onDone) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            if (onDone != null) onDone.run();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(date)
                .child(taskId);


        taskRef.child("failed").setValue(true);


        resetStreakAfterFail(onDone);
    }

    private static void updateStreakAfterCompletion(Runnable onSuccess) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        userRef.get().addOnSuccessListener(snapshot -> {

            Long curStreak = snapshot.child("currentStreak").getValue(Long.class);
            if (curStreak == null) curStreak = 0L;


            long newStreak = curStreak + 1;
            userRef.child("currentStreak").setValue(newStreak);

            Long prevMilestoneId = snapshot.child("currentMilestoneID").getValue(Long.class);
            if (prevMilestoneId == null || prevMilestoneId <= 0) prevMilestoneId = 1L;

            final long streakValue = newStreak;
            final long prevMilestoneIdFinal = prevMilestoneId;


            getMilestones(req -> {

                try {

                    int bestIdx = 1;
                    for (int i = 1; i < req.length; i++) {
                        if (streakValue >= req[i]) {
                            bestIdx = i;
                        }
                    }
                    final int bestIndex = bestIdx;

                    long currentReq = req[bestIndex];
                    long nextReq = (bestIndex + 1 < req.length)
                            ? req[bestIndex + 1]
                            : currentReq;


                    if (bestIndex > prevMilestoneIdFinal) {
                        userRef.child("currentMilestoneID").setValue((long) bestIndex);
                        userRef.child("showMilestonePopup").setValue(true);
                    }


                    double percent;
                    if (nextReq == currentReq) {
                        percent = 100.0;
                    } else {
                        percent = ((double) (streakValue - currentReq)
                                / (double) (nextReq - currentReq)) * 100.0;
                    }

                    if (percent < 0) percent = 0;
                    if (percent > 100) percent = 100;

                    userRef.child("streakBarPercent").setValue(percent);

                } catch (Exception e) {

                    long simplePercent = Math.min(100, streakValue * 10);
                    userRef.child("streakBarPercent").setValue((double) simplePercent);
                }

                if (onSuccess != null) onSuccess.run();
            });
        });
    }

    private static void resetStreakAfterFail(Runnable onDone) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            if (onDone != null) onDone.run();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);


        userRef.child("currentStreak").setValue(0L);
        userRef.child("streakBarPercent").setValue(0.0);

        if (onDone != null) onDone.run();
    }

    private static void getMilestones(MilestoneCallback cb) {

        FirebaseDatabase.getInstance()
                .getReference(MILESTONE_PATH)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {

                        cb.onLoaded(new long[]{0, 1, 5, 20, 35, 60, 90, 120, 170});
                        return;
                    }

                    int size = (int) snapshot.getChildrenCount() + 1;
                    long[] req = new long[size];  // index 0 stays 0

                    for (DataSnapshot child : snapshot.getChildren()) {
                        String key = child.getKey();
                        if (key == null) continue;

                        try {
                            int index = Integer.parseInt(key);
                            if (index >= 0 && index < size) {
                                Long tasksRequired = child.child("tasksRequired").getValue(Long.class);
                                if (tasksRequired == null) tasksRequired = 0L;
                                req[index] = tasksRequired;
                            }
                        } catch (NumberFormatException ignore) {
                        }
                    }

                    cb.onLoaded(req);
                })
                .addOnFailureListener(e ->
                        cb.onLoaded(new long[]{0, 1, 5, 20, 35, 60, 90, 120, 170})
                );
    }

    interface MilestoneCallback {
        void onLoaded(long[] reqList);
    }
}
