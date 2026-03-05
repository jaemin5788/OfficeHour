package com.jaemin.officehour.domain;

public enum Position {
    회장단(1),   // 임원진 — 오피스아워 1회
    국장단(1),   // 임원진 — 오피스아워 1회
    차장단(1),   // 임원진 — 오피스아워 1회
    국원(2);     // 국원    — 오피스아워 2회

    private final int maxAssignments;

    Position(int maxAssignments) {
        this.maxAssignments = maxAssignments;
    }

    public int getMaxAssignments() {
        return maxAssignments;
    }
}
