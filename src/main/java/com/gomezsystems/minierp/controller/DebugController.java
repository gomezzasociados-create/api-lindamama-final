package com.gomezsystems.minierp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DebugController {
    
    @GetMapping("/debug/test")
    @ResponseBody
    public String test() {
        return "DEBUG CONTROLLER IS ALIVE";
    }
}
