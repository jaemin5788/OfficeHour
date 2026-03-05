package com.jaemin.officehour.domain;

import java.util.List;

public class Schedule {

    private List<Assignment> assignments;
    private List<String> unassignedSlots;
    private List<String> warnings;

    public Schedule(List<Assignment> assignments, List<String> unassignedSlots, List<String> warnings) {
        this.assignments = assignments;
        this.unassignedSlots = unassignedSlots;
        this.warnings = warnings;
    }

    public List<Assignment> getAssignments() { return assignments; }
    public List<String> getUnassignedSlots() { return unassignedSlots; }
    public List<String> getWarnings() { return warnings; }
}
