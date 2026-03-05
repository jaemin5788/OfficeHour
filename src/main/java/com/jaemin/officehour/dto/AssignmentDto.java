package com.jaemin.officehour.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class AssignmentDto {

    private DayOfWeek day;
    private String dayKorean;
    private int slotIndex;
    private int slotId;
    private LocalTime start;
    private LocalTime end;
    private String memberName;
    private String dutyRole;   // "정" or "부"

    public AssignmentDto() {
    }

    public DayOfWeek getDay() { return day; }
    public void setDay(DayOfWeek day) { this.day = day; }

    public String getDayKorean() { return dayKorean; }
    public void setDayKorean(String dayKorean) { this.dayKorean = dayKorean; }

    public int getSlotIndex() { return slotIndex; }
    public void setSlotIndex(int slotIndex) { this.slotIndex = slotIndex; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public LocalTime getStart() { return start; }
    public void setStart(LocalTime start) { this.start = start; }

    public LocalTime getEnd() { return end; }
    public void setEnd(LocalTime end) { this.end = end; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getDutyRole() { return dutyRole; }
    public void setDutyRole(String dutyRole) { this.dutyRole = dutyRole; }
}
