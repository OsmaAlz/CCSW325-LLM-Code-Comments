package com.example.activitystreak;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    public enum DayStatus {
        NONE,
        PENDING,
        COMPLETED,
        FAILED
    }

    private List<String> days;
    private int year;
    private int month;

    private Map<String, DayStatus> statusMap = new HashMap<>();

    public interface OnDayClickListener {
        void onDayClick(int day);
    }

    private OnDayClickListener listener;

    public void setOnDayClickListener(OnDayClickListener l) {
        this.listener = l;
    }

    public void setYearMonth(int y, int m) {
        this.year = y;
        this.month = m;
    }

    public void setDays(List<String> d) {
        this.days = d;
        notifyDataSetChanged();
    }

    public void setStatusMap(Map<String, DayStatus> map) {
        this.statusMap = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DayViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String dayText = days.get(position);

        if (dayText.isEmpty()) {
            holder.txtDay.setText("");
            holder.txtDay.setBackgroundColor(Color.TRANSPARENT);
            holder.itemView.setOnClickListener(null);
            return;
        }

        holder.txtDay.setText(dayText);

        int day = Integer.parseInt(dayText);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(day);
        });

        String dateKey = String.format(Locale.getDefault(),
                "%04d-%02d-%02d",
                year, month + 1, day);

        DayStatus status = statusMap.getOrDefault(dateKey, DayStatus.NONE);
        applyColor(holder.txtDay, status);
    }

    @Override
    public int getItemCount() {
        return days == null ? 0 : days.size();
    }

    private void applyColor(TextView tv, DayStatus status) {

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);

        switch (status) {
            case PENDING:
                bg.setColor(Color.parseColor("#FFEA00"));
                break;
            case COMPLETED:
                bg.setColor(Color.parseColor("#A2F314"));
                break;
            case FAILED:
                bg.setColor(Color.parseColor("#FF706D"));
                break;
            default:
                tv.setBackgroundResource(R.drawable.bg_day_circle);
                return;
        }

        tv.setBackground(bg);
        tv.setTextColor(Color.BLACK);
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView txtDay;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDay = itemView.findViewById(R.id.txtDay);
        }
    }
}