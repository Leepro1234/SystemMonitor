package com.example.systemmonitor.controller;

import com.example.systemmonitor.common.Methods;
import com.example.systemmonitor.common.StringBuilderPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
public class LogController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Methods methods = new Methods();

    @Value("${test}")
    private String test;

    @GetMapping("/appendlog")
    public String AppendLog(){
        String result ="";
        logger.error("error Log");
        logger.info("info Log");

        return result;
    }

    @GetMapping("/getlog")
    public String AppendLog(@RequestParam("fileName") String fileName){
        String result ="";
        logger.info("fileName - " + fileName);

        return fileName + " - Log Catch !!";
    }

    @GetMapping("/checkLog")
    public String CheckLog(){
        Process p;
        StringBuilder result = new StringBuilder();
        try {
            //이 변수에 명령어를 넣어주면 된다.
            //String[] cmd = {"/logs/test.sh"};
            String[] cmd = {"/bin/bash", "-c", "chmod 722 /logs/demotest2_error.sadah"};
            p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s = "";
            while ((s = br.readLine()) != null)
                result.append(s);
            p.waitFor();
            p.destroy();

        } catch (Exception e) {
            result.append(e.getMessage());
        }
        return result.toString();
    }

    @GetMapping("/getpropertise")
    public String getpropertise() throws IOException {
        StringBuilderPlus stringBuilderPlus = new StringBuilderPlus();
        stringBuilderPlus.appendLine(methods.convertYamlToJson());
        System.out.println(stringBuilderPlus.toString());
        return stringBuilderPlus.toString().replaceAll("\r\n","<br/>");
    }

    @GetMapping("/setsystemmonitoring")
    public String setsystemmonitoring() throws Exception {


        return methods.setSystemMonitoring();

    }
}
