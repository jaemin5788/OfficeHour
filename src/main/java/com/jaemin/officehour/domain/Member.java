package com.jaemin.officehour.domain;

import java.util.List;

public class Member {

    private String name;
    private Position position;           // 직책: 회장단/국장단/차장단(1회) / 국원(2회)
    private MemberRole role;             // 근무 역할: 정(LEAD_ONLY) / 부(SUPPORT_ONLY) / 둘다(BOTH)
    private List<Integer> availableSlotIds; // slotId = dayValue*10 + slotIndex
    private List<Integer> preferredSlotIds; // 선호 순위 순서 (앞=높은 선호)
    private Boolean prefersConsecutive;     // 연속 근무 선호 여부 (null=상관없음, true=선호, false=비선호)
    private int maxAssignments;             // 최대 배정 횟수 (직책 기본값 오버라이드 가능)

    public Member() {
    }

    public Member(String name, Position position, MemberRole role,
            List<Integer> availableSlotIds,
            List<Integer> preferredSlotIds,
            Boolean prefersConsecutive,
            int maxAssignments) {
        this.name = name;
        this.position = position;
        this.role = role;
        this.availableSlotIds = availableSlotIds;
        this.preferredSlotIds = preferredSlotIds;
        this.prefersConsecutive = prefersConsecutive;
        this.maxAssignments = maxAssignments;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public MemberRole getRole() {
        return role;
    }

    public List<Integer> getAvailableSlotIds() {
        return availableSlotIds;
    }

    public List<Integer> getPreferredSlotIds() {
        return preferredSlotIds;
    }

    public Boolean isPrefersConsecutive() {
        return prefersConsecutive;
    }

    public int getMaxAssignments() {
        return maxAssignments;
    }
}
