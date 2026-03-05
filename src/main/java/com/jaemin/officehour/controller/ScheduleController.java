package com.jaemin.officehour.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.jaemin.officehour.dto.ScheduleRequest;
import com.jaemin.officehour.dto.ScheduleResponse;
import com.jaemin.officehour.service.ScheduleService;

@RestController
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @PostMapping("/schedule")
    public ScheduleResponse generate(@RequestBody ScheduleRequest req) {
        return scheduleService.generate(req);
    }
}
