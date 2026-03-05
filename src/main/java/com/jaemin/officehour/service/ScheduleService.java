package com.jaemin.officehour.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jaemin.officehour.domain.Assignment;
import com.jaemin.officehour.domain.AssignmentPolicy;
import com.jaemin.officehour.domain.Member;
import com.jaemin.officehour.domain.MemberRole;
import com.jaemin.officehour.domain.Slot;
import com.jaemin.officehour.dto.AssignmentDto;
import com.jaemin.officehour.dto.MemberInput;
import com.jaemin.officehour.dto.ScheduleRequest;
import com.jaemin.officehour.dto.ScheduleResponse;
import com.jaemin.officehour.util.SlotGenerator;

@Service
public class ScheduleService {

    private final Random random = new Random();

    private static final String[] DAY_KO = { "", "월", "화", "수", "목", "금" };
    private static final String[] TIME_LABELS = {
            "10:30~12:00", "12:00~13:30", "13:30~15:00", "15:00~16:30", "16:30~18:00"
    };

    public ScheduleResponse generate(ScheduleRequest req) {
        // 1. 멤버 변환
        List<Member> members = req.getMembers().stream()
                .map(this::toMember)
                .collect(Collectors.toList());

        // 2. 슬롯 생성 및 활성 슬롯 집합 구성
        List<Slot> allSlots = SlotGenerator.generateWeeklySlots();
        Set<Integer> enabledSet;
        if (req.getEnabledSlotIds() != null && !req.getEnabledSlotIds().isEmpty()) {
            enabledSet = new HashSet<>(req.getEnabledSlotIds());
            allSlots = allSlots.stream()
                    .filter(s -> enabledSet.contains(s.getSlotId()))
                    .collect(Collectors.toList());
        } else {
            enabledSet = allSlots.stream().map(Slot::getSlotId).collect(Collectors.toSet());
        }
        final Set<Integer> enabledSlots = enabledSet;

        // slotId → Slot 빠른 조회용
        Map<Integer, Slot> slotById = allSlots.stream()
                .collect(Collectors.toMap(Slot::getSlotId, s -> s));

        // 3. 각 슬롯별 정/부 가용 멤버 계산
        Map<Integer, List<Member>> leadCandidates = new HashMap<>();
        Map<Integer, List<Member>> supportCandidates = new HashMap<>();

        for (Slot slot : allSlots) {
            int id = slot.getSlotId();
            List<Member> leads = new ArrayList<>(), supports = new ArrayList<>();
            for (Member m : members) {
                if (m.getAvailableSlotIds() != null && m.getAvailableSlotIds().contains(id)) {
                    if (m.getRole() == MemberRole.LEAD_ONLY || m.getRole() == MemberRole.BOTH) leads.add(m);
                    if (m.getRole() == MemberRole.SUPPORT_ONLY || m.getRole() == MemberRole.BOTH) supports.add(m);
                }
            }
            leadCandidates.put(id, leads);
            supportCandidates.put(id, supports);
        }

        // 4. 슬롯 정렬 — 랜덤 셔플 후 가용 인원 오름차순 (채우기 어려운 슬롯 먼저)
        List<Slot> sortedSlots = new ArrayList<>(allSlots);
        Collections.shuffle(sortedSlots, random);
        sortedSlots.sort(Comparator.comparingInt(
                s -> leadCandidates.get(s.getSlotId()).size() + supportCandidates.get(s.getSlotId()).size()));

        // 5. 배정 상태 추적
        Map<String, Integer> assignCount = new HashMap<>();
        Map<String, Set<String>> usedDutyRoles = new HashMap<>();
        Map<String, Set<Integer>> assignedSlotIds = new HashMap<>();
        for (Member m : members) {
            assignCount.put(m.getName(), 0);
            usedDutyRoles.put(m.getName(), new HashSet<>());
            assignedSlotIds.put(m.getName(), new HashSet<>());
        }

        List<Assignment> assignments = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // ── Pass 1: 그리디 배정 (max count 제한 준수) ──
        List<Object[]> unfilledPositions = new ArrayList<>(); // {Slot, dutyRole}

        for (Slot slot : sortedSlots) {
            int slotId = slot.getSlotId();

            // 정 배정
            Member leader = pickBest(leadCandidates.get(slotId), "정", slot,
                    assignCount, usedDutyRoles, assignedSlotIds, false, enabledSlots, members);
            if (leader != null) {
                record(leader, "정", slot, assignments, assignCount, usedDutyRoles, assignedSlotIds);
            } else {
                unfilledPositions.add(new Object[]{slot, "정"});
            }

            // 부 배정 (정과 다른 사람)
            List<Member> supportPool = supportCandidates.get(slotId).stream()
                    .filter(m -> leader == null || !m.getName().equals(leader.getName()))
                    .collect(Collectors.toList());
            Member supporter = pickBest(supportPool, "부", slot,
                    assignCount, usedDutyRoles, assignedSlotIds, false, enabledSlots, members);
            if (supporter != null) {
                record(supporter, "부", slot, assignments, assignCount, usedDutyRoles, assignedSlotIds);
            } else {
                unfilledPositions.add(new Object[]{slot, "부"});
            }
        }

        // ── Pass 2a: 미배정 슬롯 강제 배정 (BOTH 역할 제약 우회, max count 준수) ──
        List<Object[]> hardUnfilled = new ArrayList<>();

        for (Object[] unf : unfilledPositions) {
            Slot slot = (Slot) unf[0];
            String dutyRole = (String) unf[1];
            int slotId = slot.getSlotId();

            List<Member> pool = dutyRole.equals("정") ? leadCandidates.get(slotId) : supportCandidates.get(slotId);
            List<Member> available = pool.stream()
                    .filter(m -> !assignedSlotIds.get(m.getName()).contains(slotId))
                    .collect(Collectors.toList());

            Member forced = pickBest(available, dutyRole, slot,
                    assignCount, usedDutyRoles, assignedSlotIds, true, enabledSlots, members);
            if (forced != null) {
                record(forced, dutyRole, slot, assignments, assignCount, usedDutyRoles, assignedSlotIds);
            } else {
                hardUnfilled.add(unf); // 모두 max 도달 → 2b에서 처리
            }
        }

        // ── Pass 2b: 최후 수단 — 가용자가 모두 max 도달 시 max 무시하고 최소 배정자 선택 ──
        List<String> unassigned = new ArrayList<>();

        for (Object[] unf : hardUnfilled) {
            Slot slot = (Slot) unf[0];
            String dutyRole = (String) unf[1];
            int slotId = slot.getSlotId();

            List<Member> pool = dutyRole.equals("정") ? leadCandidates.get(slotId) : supportCandidates.get(slotId);
            List<Member> available = pool.stream()
                    .filter(m -> !assignedSlotIds.get(m.getName()).contains(slotId))
                    .collect(Collectors.toList());

            if (available.isEmpty()) {
                unassigned.add(slotLabel(slot) + " " + dutyRole + " 자리 미배정 (가용자 없음)");
                continue;
            }
            // 가장 적게 배정된 사람 선택 (공평성 극대화, max count 무시)
            available.sort(Comparator.comparingInt(m -> assignCount.get(m.getName())));
            record(available.get(0), dutyRole, slot, assignments, assignCount, usedDutyRoles, assignedSlotIds);
        }

        // ── Pass 3: 목표 배정 횟수 미달 멤버 보충 배정 (특히 국원 2회 보장) ──
        // 현재 채워진 포지션 맵 구성
        Map<Integer, Set<String>> filledMap = new HashMap<>();
        for (Assignment a : assignments) {
            filledMap.computeIfAbsent(a.getSlot().getSlotId(), k -> new HashSet<>()).add(a.getDutyRole());
        }

        // 긴급도 순 정렬: (가용 슬롯 수 - 남은 필요 배정) 오름차순, 같으면 직책 우선순위 오름차순
        List<Member> pass3Queue = new ArrayList<>(members);
        pass3Queue.sort(Comparator
                .comparingInt((Member m) -> {
                    int avail = m.getAvailableSlotIds() == null ? 0
                            : (int) m.getAvailableSlotIds().stream().filter(enabledSlots::contains).count();
                    int needed = Math.max(0, m.getMaxAssignments() - assignCount.get(m.getName()));
                    return avail - needed;
                })
                .thenComparingInt(m -> switch (m.getPosition()) {
                    case 국원 -> 0; case 차장단 -> 1; case 국장단 -> 2; case 회장단 -> 3;
                }));

        for (Member m : pass3Queue) {
            int required = m.getMaxAssignments();
            if (required <= 0) continue;
            int current = assignCount.get(m.getName());
            if (current >= required) continue;

            // 가용+활성 슬롯 목록
            List<Integer> availEnabled = m.getAvailableSlotIds() == null ? List.of()
                    : m.getAvailableSlotIds().stream().filter(enabledSlots::contains).collect(Collectors.toList());

            if (availEnabled.size() < required) continue; // 가용 슬롯 부족 → 경고는 최종 단계에서

            // 열린 포지션(정/부) 찾아 강제 배정
            for (int slotId : availEnabled) {
                if (current >= required) break;
                Slot slot = slotById.get(slotId);
                if (slot == null || assignedSlotIds.get(m.getName()).contains(slotId)) continue;
                Set<String> filled = filledMap.getOrDefault(slotId, new HashSet<>());

                for (String role : new String[]{"정", "부"}) {
                    if (current >= required) break;
                    if (filled.contains(role)) continue;
                    if (role.equals("정") && m.getRole() == MemberRole.SUPPORT_ONLY) continue;
                    if (role.equals("부") && m.getRole() == MemberRole.LEAD_ONLY) continue;

                    record(m, role, slot, assignments, assignCount, usedDutyRoles, assignedSlotIds);
                    filledMap.computeIfAbsent(slotId, k -> new HashSet<>()).add(role);
                    current++;
                }
            }
        }

        // ── Pass 4: 같은 직책 내 배정 불균형 교정 ──
        // 과배정 멤버(B)에서 미달 멤버(A)로 슬롯 이관 (같은 직책 내에서만)
        for (int swapIter = 0; swapIter < members.size() * 4; swapIter++) {
            boolean swapped = false;

            List<Member> underfilled = members.stream()
                    .filter(m -> m.getMaxAssignments() > 0
                            && assignCount.get(m.getName()) < m.getMaxAssignments())
                    .sorted(Comparator.comparingInt(m -> assignCount.get(m.getName())))
                    .collect(Collectors.toList());

            outer:
            for (Member A : underfilled) {
                int currentA = assignCount.get(A.getName());
                if (currentA >= A.getMaxAssignments()) continue;

                List<Integer> aAvail = A.getAvailableSlotIds() == null ? List.of()
                        : A.getAvailableSlotIds().stream()
                            .filter(enabledSlots::contains)
                            .filter(id -> !assignedSlotIds.get(A.getName()).contains(id))
                            .collect(Collectors.toList());

                for (int slotId : aAvail) {
                    Slot slot = slotById.get(slotId);
                    if (slot == null) continue;

                    for (String role : new String[]{"정", "부"}) {
                        if (role.equals("정") && A.getRole() == MemberRole.SUPPORT_ONLY) continue;
                        if (role.equals("부") && A.getRole() == MemberRole.LEAD_ONLY) continue;

                        Set<String> filled = filledMap.getOrDefault(slotId, Collections.emptySet());
                        if (!filled.contains(role)) continue;

                        // 이 포지션의 담당자 B 찾기
                        Assignment target = null;
                        for (Assignment asgn : assignments) {
                            if (asgn.getSlot().getSlotId() == slotId && asgn.getDutyRole().equals(role)) {
                                target = asgn; break;
                            }
                        }
                        if (target == null) continue;

                        Member B = target.getMember();
                        if (B.getPosition() != A.getPosition()) continue; // 같은 직책만 교환
                        int currentB = assignCount.get(B.getName());
                        if (currentB <= currentA) continue; // B가 A보다 많아야 교환 의미 있음

                        // 교환: B에서 제거 → A에게 배정
                        assignments.remove(target);
                        assignCount.merge(B.getName(), -1, Integer::sum);
                        assignedSlotIds.get(B.getName()).remove((Integer) slotId);
                        Set<String> fSet = filledMap.get(slotId);
                        if (fSet != null) fSet.remove(role);

                        record(A, role, slot, assignments, assignCount, usedDutyRoles, assignedSlotIds);
                        filledMap.computeIfAbsent(slotId, k -> new HashSet<>()).add(role);

                        // B에게 다른 빈 슬롯 배정 시도
                        int requiredB = B.getMaxAssignments();
                        if (assignCount.get(B.getName()) < requiredB) {
                            List<Integer> bAvail = B.getAvailableSlotIds() == null ? List.of()
                                    : B.getAvailableSlotIds().stream().filter(enabledSlots::contains)
                                        .collect(Collectors.toList());
                            for (int bId : bAvail) {
                                if (assignCount.get(B.getName()) >= requiredB) break;
                                if (assignedSlotIds.get(B.getName()).contains(bId)) continue;
                                Slot bSlot = slotById.get(bId);
                                if (bSlot == null) continue;
                                for (String bRole : new String[]{"정", "부"}) {
                                    if (assignCount.get(B.getName()) >= requiredB) break;
                                    Set<String> bFilled = filledMap.getOrDefault(bId, Collections.emptySet());
                                    if (bFilled.contains(bRole)) continue;
                                    if (bRole.equals("정") && B.getRole() == MemberRole.SUPPORT_ONLY) continue;
                                    if (bRole.equals("부") && B.getRole() == MemberRole.LEAD_ONLY) continue;
                                    record(B, bRole, bSlot, assignments, assignCount, usedDutyRoles, assignedSlotIds);
                                    filledMap.computeIfAbsent(bId, k -> new HashSet<>()).add(bRole);
                                }
                            }
                        }

                        swapped = true;
                        continue outer; // A가 슬롯 하나 얻었으면 다음 A로
                    }
                }
            }

            if (!swapped) break;
        }

        // ── 최종 경고 생성 ──
        for (Member m : members) {
            if (m.getMaxAssignments() <= 0) continue;
            int current = assignCount.get(m.getName());
            if (current >= m.getMaxAssignments()) continue;

            List<Integer> availEnabled = m.getAvailableSlotIds() == null ? List.of()
                    : m.getAvailableSlotIds().stream().filter(enabledSlots::contains).collect(Collectors.toList());

            if (availEnabled.size() < m.getMaxAssignments()) {
                warnings.add(m.getName() + "님(" + m.getPosition().name() + ")의 가용 슬롯 수("
                        + availEnabled.size() + "개)가 목표 " + m.getMaxAssignments() + "회에 부족합니다.");
            } else {
                warnings.add(m.getName() + "님(" + m.getPosition().name()
                        + ")의 모든 가용 슬롯이 이미 찼습니다. " + current + "/" + m.getMaxAssignments()
                        + "회 배정. 배정을 다시 실행해보세요.");
            }
        }

        return buildResponse(assignments, unassigned, assignCount, warnings, members);
    }

    private Member pickBest(List<Member> pool, String dutyRole, Slot slot,
            Map<String, Integer> assignCount,
            Map<String, Set<String>> usedDutyRoles,
            Map<String, Set<Integer>> assignedSlotIds,
            boolean forceMode,
            Set<Integer> enabledSlots,
            List<Member> allMembers) {
        List<Member> candidates = pool.stream()
                .filter(m -> {
                    // 같은 슬롯 중복 불가
                    if (assignedSlotIds.get(m.getName()).contains(slot.getSlotId())) return false;
                    int cnt = assignCount.get(m.getName());
                    int max = m.getMaxAssignments();
                    if (cnt >= max) return false; // 항상 maxAssignments 준수
                    if (forceMode) return true; // 강제 모드: BOTH 역할 제약만 우회
                    // BOTH 멤버: 마지막 회차에는 반드시 다른 역할로 배정
                    if (m.getRole() == MemberRole.BOTH && max >= 2 && cnt == max - 1) {
                        return !usedDutyRoles.get(m.getName()).contains(dutyRole);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return null;

        // 점수 계산 후 최고 점수 파악
        int maxScore = candidates.stream()
                .mapToInt(m -> score(m, dutyRole, slot, assignCount, usedDutyRoles, assignedSlotIds, enabledSlots, allMembers))
                .max().getAsInt();

        // 동점자 중 랜덤 선택
        List<Member> best = candidates.stream()
                .filter(m -> score(m, dutyRole, slot, assignCount, usedDutyRoles, assignedSlotIds, enabledSlots, allMembers) == maxScore)
                .collect(Collectors.toList());

        return best.get(random.nextInt(best.size()));
    }

    private int score(Member m, String dutyRole, Slot slot,
            Map<String, Integer> assignCount,
            Map<String, Set<String>> usedDutyRoles,
            Map<String, Set<Integer>> assignedSlotIds,
            Set<Integer> enabledSlots,
            List<Member> allMembers) {
        int s = 0;
        int slotId = slot.getSlotId();

        // 희소성 보너스: 남은 필요 배정 수 대비 가용 슬롯이 빡빡할수록 우선 배정
        int cnt = assignCount.get(m.getName());
        int remaining = m.getMaxAssignments() - cnt;
        if (remaining > 0) {
            long availCount = m.getAvailableSlotIds() == null ? 0
                    : m.getAvailableSlotIds().stream().filter(enabledSlots::contains).count();
            long gap = availCount - remaining;
            if (gap <= 0)      s += 100;
            else if (gap == 1) s += 60;
            else if (gap == 2) s += 30;
            else if (gap <= 4) s += 10;
        }

        // 직책 내 공평 배분 보너스: 같은 직책 중 배정이 적은 사람 우선
        if (remaining > 0) {
            int posGroupMin = allMembers.stream()
                    .filter(p -> p.getPosition() == m.getPosition() && p.getMaxAssignments() > 0)
                    .mapToInt(p -> assignCount.getOrDefault(p.getName(), 0))
                    .min().orElse(0);
            if (cnt == posGroupMin) {
                s += 80; // 직책 그룹 내 최소 배정 상태 → 우선 배정
            }
        }

        // 선호도 점수
        List<Integer> prefs = m.getPreferredSlotIds();
        if (prefs != null) {
            int idx = prefs.indexOf(slotId);
            if (idx >= 0 && idx < AssignmentPolicy.PREF_SCORES.length) {
                s += AssignmentPolicy.PREF_SCORES[idx];
            }
        }

        // 직책 우선순위 (국원→차장단→국장단→회장단 순)
        switch (m.getPosition()) {
            case 국원:   s += AssignmentPolicy.PRIORITY_국원;   break;
            case 차장단:  s += AssignmentPolicy.PRIORITY_차장단;  break;
            case 국장단:  s += AssignmentPolicy.PRIORITY_국장단;  break;
            case 회장단:  s += AssignmentPolicy.PRIORITY_회장단;  break;
        }

        // 연속 근무 보너스 / 페널티 (null = 상관없음)
        Set<Integer> mySlots = assignedSlotIds.get(m.getName());
        boolean adjacent = mySlots.stream()
                .anyMatch(id -> id / 10 == slotId / 10 && Math.abs(id % 10 - slotId % 10) == 1);

        if (adjacent) {
            Boolean pref = m.isPrefersConsecutive();
            if (Boolean.TRUE.equals(pref)) s += AssignmentPolicy.CONSECUTIVE_PREFER_BONUS;
            else if (Boolean.FALSE.equals(pref)) s += AssignmentPolicy.CONSECUTIVE_AVOID_PENALTY;
            // null = 상관없음 → 보정 없음
        }

        // 이미 1타임 배정 → 약간 페널티 (균등 배분 유도)
        if (cnt == 1) {
            s += AssignmentPolicy.ALREADY_ONE_PENALTY;
        }

        // BOTH 멤버: 2타임째에 다른 역할 배정 권장
        if (m.getRole() == MemberRole.BOTH && cnt == 1) {
            if (!usedDutyRoles.get(m.getName()).contains(dutyRole)) {
                s += AssignmentPolicy.ROLE_VARIETY_BONUS;
            }
        }

        return s;
    }

    private void record(Member m, String dutyRole, Slot slot,
            List<Assignment> assignments,
            Map<String, Integer> assignCount,
            Map<String, Set<String>> usedDutyRoles,
            Map<String, Set<Integer>> assignedSlotIds) {
        assignments.add(new Assignment(slot, m, dutyRole));
        assignCount.merge(m.getName(), 1, Integer::sum);
        usedDutyRoles.get(m.getName()).add(dutyRole);
        assignedSlotIds.get(m.getName()).add(slot.getSlotId());
    }

    private ScheduleResponse buildResponse(List<Assignment> assignments,
            List<String> unassigned,
            Map<String, Integer> assignCount,
            List<String> warnings,
            List<Member> members) {
        List<AssignmentDto> dtos = assignments.stream().map(a -> {
            AssignmentDto dto = new AssignmentDto();
            Slot slot = a.getSlot();
            dto.setDay(slot.getDay());
            dto.setDayKorean(DAY_KO[slot.getDay().getValue()]);
            dto.setSlotIndex(slot.getSlotIndex());
            dto.setSlotId(slot.getSlotId());
            dto.setStart(slot.getStart());
            dto.setEnd(slot.getEnd());
            dto.setMemberName(a.getMember().getName());
            dto.setDutyRole(a.getDutyRole());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (Member m : members) {
            countMap.put(m.getName(), assignCount.getOrDefault(m.getName(), 0));
        }

        ScheduleResponse resp = new ScheduleResponse();
        resp.setAssignments(dtos);
        resp.setUnassignedSlots(unassigned);
        resp.setMemberAssignmentCount(countMap);
        resp.setWarnings(warnings);
        return resp;
    }

    private Member toMember(MemberInput input) {
        com.jaemin.officehour.domain.Position pos =
                input.getPosition() != null ? input.getPosition() : com.jaemin.officehour.domain.Position.국원;
        // maxAssignments: 명시적으로 전달된 값 사용 (null이면 직책 기본값)
        int maxAssign = (input.getMaxAssignments() != null) ? input.getMaxAssignments() : pos.getMaxAssignments();
        return new Member(
                input.getName(),
                pos,
                input.getRole() != null ? input.getRole() : MemberRole.BOTH,
                input.getAvailableSlotIds() != null ? input.getAvailableSlotIds() : List.of(),
                input.getPreferredSlotIds() != null ? input.getPreferredSlotIds() : List.of(),
                input.isPrefersConsecutive(), // null 그대로 전달 (상관없음)
                maxAssign);
    }

    private String slotLabel(Slot slot) {
        return DAY_KO[slot.getDay().getValue()] + "요일 " + TIME_LABELS[slot.getSlotIndex()];
    }
}
