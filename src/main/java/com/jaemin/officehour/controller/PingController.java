package com.jaemin.officehour.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/ping")
    public String ping(){
        return "pong";
    }
}

// 요청이 들어오면 문자열을 반환하는 가장 단순한 REST API

// 
