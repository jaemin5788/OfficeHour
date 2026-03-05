package com.jaemin.officehour.domain;

public class AssignmentPolicy {

    public static final int MAX_PER_MEMBER = 2;  // 1인 최대 배정 타임

    // 선호도 점수 (1순위가 가장 높음)
    public static final int[] PREF_SCORES = {10, 7, 5, 3, 1};

    // 연속 근무 보너스/페널티
    public static final int CONSECUTIVE_PREFER_BONUS = 8;
    public static final int CONSECUTIVE_AVOID_PENALTY = -5;

    // 이미 1타임 배정된 경우 패널티 (균등 배분 유도)
    public static final int ALREADY_ONE_PENALTY = -3;

    // BOTH 멤버가 2타임에서 다른 역할(정↔부) 배정 시 보너스
    public static final int ROLE_VARIETY_BONUS = 5;

    // 직책 우선순위 점수 (인력 > 슬롯일 때 국원→차장단→국장단→회장단 순 우선 배치)
    public static final int PRIORITY_국원    = 40;
    public static final int PRIORITY_차장단   = 30;
    public static final int PRIORITY_국장단   = 20;
    public static final int PRIORITY_회장단   = 10;
}
