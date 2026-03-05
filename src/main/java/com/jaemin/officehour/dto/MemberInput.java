package com.jaemin.officehour.dto;

import java.util.List;

import com.jaemin.officehour.domain.MemberRole;
import com.jaemin.officehour.domain.Position;

public class MemberInput {

    private String name;
    private Position position;               // 직책: 회장단/국장단/차장단/국원
    private MemberRole role;                 // LEAD_ONLY / SUPPORT_ONLY / BOTH
    private List<Integer> availableSlotIds;  // slotId = dayValue*10 + slotIndex
    private List<Integer> preferredSlotIds;  // 선호 순위순 (앞=높은 선호)
    private Boolean prefersConsecutive;      // 연속 근무 선호 여부 (null=상관없음)
    private Integer maxAssignments;          // null이면 직책 기본값 사용

    public MemberInput() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public MemberRole getRole() { return role; }
    public void setRole(MemberRole role) { this.role = role; }

    public List<Integer> getAvailableSlotIds() { return availableSlotIds; }
    public void setAvailableSlotIds(List<Integer> availableSlotIds) { this.availableSlotIds = availableSlotIds; }

    public List<Integer> getPreferredSlotIds() { return preferredSlotIds; }
    public void setPreferredSlotIds(List<Integer> preferredSlotIds) { this.preferredSlotIds = preferredSlotIds; }

    public Boolean isPrefersConsecutive() { return prefersConsecutive; }
    public void setPrefersConsecutive(Boolean prefersConsecutive) { this.prefersConsecutive = prefersConsecutive; }

    public Integer getMaxAssignments() { return maxAssignments; }
    public void setMaxAssignments(Integer maxAssignments) { this.maxAssignments = maxAssignments; }
}
