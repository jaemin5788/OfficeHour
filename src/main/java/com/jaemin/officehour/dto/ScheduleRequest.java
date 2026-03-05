package com.jaemin.officehour.dto;

import java.util.List;

public class ScheduleRequest {

    private List<MemberInput> members;
    private List<Integer> enabledSlotIds; // null 또는 빈 경우 전체 25슬롯 사용

    public ScheduleRequest() {
    }

    public List<MemberInput> getMembers() { return members; }
    public void setMembers(List<MemberInput> members) { this.members = members; }

    public List<Integer> getEnabledSlotIds() { return enabledSlotIds; }
    public void setEnabledSlotIds(List<Integer> enabledSlotIds) { this.enabledSlotIds = enabledSlotIds; }
}
