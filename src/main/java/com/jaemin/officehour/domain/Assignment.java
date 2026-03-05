package com.jaemin.officehour.domain;

public class Assignment {

    private Slot slot;
    private Member member;
    private String dutyRole; // "정" or "부"

    public Assignment(Slot slot, Member member, String dutyRole) {
        this.slot = slot;
        this.member = member;
        this.dutyRole = dutyRole;
    }

    public Slot getSlot() { return slot; }
    public Member getMember() { return member; }
    public String getDutyRole() { return dutyRole; }
}
