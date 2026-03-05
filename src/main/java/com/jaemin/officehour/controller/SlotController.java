package com.jaemin.officehour.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jaemin.officehour.domain.Slot;
import com.jaemin.officehour.util.SlotGenerator;

@RestController
public class SlotController {

    @GetMapping("/slots")
    public List<Slot> slots() {
        return SlotGenerator.generateWeeklySlots();
    }
}
