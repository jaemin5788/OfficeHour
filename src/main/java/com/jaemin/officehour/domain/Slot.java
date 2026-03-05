package com.jaemin.officehour.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class Slot {

    private DayOfWeek day;
    private LocalTime start;
    private LocalTime end;
    private int slotIndex; // 하루 내 순서 (0~4)

    public Slot() {
    }

    public Slot(DayOfWeek day, LocalTime start, LocalTime end, int slotIndex) {
        this.day = day;
        this.start = start;
        this.end = end;
        this.slotIndex = slotIndex;
    }

    // slotId = dayValue(1~5) * 10 + slotIndex(0~4)
    // 예) 월요일 0번째 = 10, 금요일 4번째 = 54
    public int getSlotId() {
        return day.getValue() * 10 + slotIndex;
    }

    // 같은 요일, 바로 인접한 슬롯인지 확인
    public boolean isConsecutiveWith(Slot other) {
        return this.day == other.day && Math.abs(this.slotIndex - other.slotIndex) == 1;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
