package com.jaemin.officehour.dto;

import java.util.List;
import java.util.Map;

public class ScheduleResponse {

    private List<AssignmentDto> assignments;
    private List<String> unassignedSlots;              // 배정 못 된 슬롯 설명
    private Map<String, Integer> memberAssignmentCount; // 멤버별 배정 횟수
    private List<String> warnings;                     // 경고 메시지

    public ScheduleResponse() {
    }

    public List<AssignmentDto> getAssignments() { return assignments; }
    public void setAssignments(List<AssignmentDto> assignments) { this.assignments = assignments; }

    public List<String> getUnassignedSlots() { return unassignedSlots; }
    public void setUnassignedSlots(List<String> unassignedSlots) { this.unassignedSlots = unassignedSlots; }

    public Map<String, Integer> getMemberAssignmentCount() { return memberAssignmentCount; }
    public void setMemberAssignmentCount(Map<String, Integer> memberAssignmentCount) { this.memberAssignmentCount = memberAssignmentCount; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
