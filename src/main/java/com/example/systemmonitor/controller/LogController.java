package com.example.systemmonitor.controller;

import com.example.systemmonitor.common.Methods;
import com.example.systemmonitor.common.StringBuilderPlus;
import com.example.systemmonitor.dto.slack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
public class LogController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Methods methods = new Methods();
    @GetMapping("/appendlog")
    public String AppendLog(){
        String result ="";
        logger.error("error Log");
        logger.info("info Log");

        return result;
    }

    @GetMapping("/getpropertise")
    public String getpropertise() throws IOException {
        StringBuilderPlus stringBuilderPlus = new StringBuilderPlus();
        stringBuilderPlus.appendLine(methods.convertYamlToJson());
        System.out.println(stringBuilderPlus.toString());
        return stringBuilderPlus.toString().replaceAll("\r\n","<br/>");
    }

    @GetMapping(value = "/setsystemmonitoring")
    public String setsystemmonitoring(HttpServletRequest request) throws Exception {
        String uri = request.getRequestURL().toString();
        methods.SetSendslackUrl(uri.replace(request.getRequestURI(),"") + "/demotest2/sendslackmessage");
        return methods.setSystemMonitoring();
    }

    @PostMapping("/sendslackmessage")
    public String sendslackmessage(@RequestBody slack slack, HttpServletRequest request) throws Exception {
        String domain = request.getRequestURI();
        String schema = request.getScheme();
        String uri = request.getRequestURL().toString();
        return uri.replace(domain,"") + " || "+ slack.getMessage();
    }


}
