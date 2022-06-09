package com.example.systemmonitor.controller;

import com.example.systemmonitor.common.Methods;
import com.example.systemmonitor.common.StringBuilderPlus;
import com.example.systemmonitor.dto.Slack;
import com.example.systemmonitor.service.SlackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
public class LogController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Methods methods = new Methods();
    @GetMapping("/appendlog")
    public String AppendLog(){
        String result ="";
        logger.error("ERROR Log");

        return result;
    }
    @GetMapping("/appendinfolog")
    public String AppendInfoLog(){
        String result ="";
        logger.info("INFO Log");

        return result;
    }

    @GetMapping("/getpropertise")
    public String GetPropertise() throws IOException {
        StringBuilderPlus stringBuilderPlus = new StringBuilderPlus();
        stringBuilderPlus.appendLine(methods.ConvertYamlToJson());
        System.out.println(stringBuilderPlus.toString());
        return stringBuilderPlus.toString().replaceAll("\r\n","<br/>");
    }

    @GetMapping(value = "/setsystemmonitoring")
    public String SetSystemMornitoring(HttpServletRequest request) throws Exception {
        String uri = request.getRequestURL().toString();
        methods.SetUrl(uri.replace(request.getRequestURI(),"") + "/demotest2/sendslackmessage",
                uri.replace(request.getRequestURI(),"") + "/demotest2/api/readlimitlist"    );
        return methods.SetSystemMonitoring();
    }

    @GetMapping(value = "/closesystemmornitoring")
    public String CloseSystemMornitoring(HttpServletRequest request) throws Exception {
        return methods.CloseSystemMonitoring();
    }


    @PostMapping("/sendslackmessage")
    public String SendSlackmessage(@RequestBody Slack slack, HttpServletRequest request) throws Exception {
        SlackService<Slack> slackService = new SlackService<Slack>();
        slackService.SetMethod(SlackService.Method.POST);
        slackService.SetJsonBody(slack);
        slackService.SendSlackMessage(slack.getWebhookUrl(), slack);

        return slackService.resposeBody;
    }

    @PostMapping("/api/readlimitlist")
    public String ReadLimitList(@RequestBody Slack slack, HttpServletRequest request) throws Exception {
        try {
            return methods.ReadAndInitLimitList(slack);
        }catch (Exception ex){
            return "ERROR " + ex.getMessage();
        }
    }


}
