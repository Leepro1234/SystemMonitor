package com.example.systemmonitor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @Value("${tomcat.ajp.port}")
    private int port;

    @RequestMapping(value = {"/"})
    public String index(Model model) {
        model.addAttribute("data", port);
        return "Home/index";
    }
}
