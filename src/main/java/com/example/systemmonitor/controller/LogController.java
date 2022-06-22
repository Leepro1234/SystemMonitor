package com.example.systemmonitor.controller;

import com.example.systemmonitor.common.Methods;
import com.example.systemmonitor.service.MvCounselling;
import com.example.systemmonitor.vo.SlackVO;
import com.example.systemmonitor.service.SlackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
public class LogController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Methods methods = new Methods();
    @GetMapping("/appendlog")
    public String AppendLog(){
        String result ="";
        logger.error("00009-ERROR ServiceLoggingAspect");

        return result;
    }
    @GetMapping("/appendinfolog")
    public String AppendInfoLog(){
        String result ="";
        logger.info("INFO Log");

        return result;
    }

    @GetMapping(value = "/setsystemmonitoring")
    public String SetSystemMornitoring(HttpServletRequest request) throws Exception {
        String uri = request.getRequestURL().toString();
/*        methods.SetUrl(uri.replace(request.getRequestURI(),"") + "/demotest2/sendslackmessage",
                uri.replace(request.getRequestURI(),"") + "/demotest2/api/readlimitlist"    );*/

                methods.setUrl(uri.replace(request.getRequestURI(),"") + "/sendslackmessage",
                               uri.replace(request.getRequestURI(),"") + "/api/readlimitlist",
                               uri.replace(request.getRequestURI(),"") + "/api/mvhealthcheck");
        return methods.setSystemMonitoring();
    }

    @GetMapping(value = "/closesystemmornitoring")
    public String CloseSystemMornitoring(HttpServletRequest request) throws Exception {
        return methods.closeSystemMonitoring();
    }

    @GetMapping(value = "/getsystemmornitoringstatus")
    public String GetSystemMonitoringStatus(HttpServletRequest request) throws Exception {
        return methods.getSystemMonitoringStatus().replace("\r\n","<br/>");
    }


    @PostMapping("/sendslackmessage")
    public String SendSlackmessage(@RequestBody SlackVO slackVO, HttpServletRequest request) throws Exception {
        SlackService<SlackVO> slackService = new SlackService<SlackVO>();
        slackService.SetMethod(SlackService.Method.POST);
        slackService.SetJsonBody(slackVO);
        slackService.SendSlackMessage(slackVO.getWebhookUrl(), slackVO);

        return slackService.resposeBody;
    }

    @PostMapping("/api/readlimitlist")
    public String ReadLimitList(@RequestBody SlackVO slackVO, HttpServletRequest request) throws Exception {
        try {
            return methods.readAndInitLimitList(slackVO);
        }catch (Exception ex){
            return "ERROR " + ex.getMessage();
        }
    }

    @GetMapping("/api/test/")
    public String test(HttpServletRequest request) throws Exception {
        SlackService<SlackVO> slackService = new SlackService<SlackVO>();
        slackService.SetMethod(SlackService.Method.GET);
        slackService.test("https://data.bigdragon.shop/demotest2/getsystemmornitoringstatus");

        return "";
    }

    @PostMapping("/api/mvhealthcheck")
    public void mvHealthStatusCheck(@RequestBody SlackVO slackVO) throws Exception {
        MvCounselling mvCounselling = new MvCounselling();
        SlackService<SlackVO> slackService = new SlackService<SlackVO>();
        slackService.SetMethod(SlackService.Method.POST);
        if (!mvCounselling.isAlive()) {
            slackService.SetJsonBody(slackVO);
            slackService.SendSlackMessage(slackVO.getWebhookUrl(), slackVO);
        }else{
            slackVO.setText("Success");
            slackService.SetJsonBody(slackVO);
            slackService.SendSlackMessage(slackVO.getWebhookUrl(), slackVO);
        }
    }

}
