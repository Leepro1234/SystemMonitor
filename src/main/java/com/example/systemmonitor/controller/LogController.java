package com.example.systemmonitor.controller;

import com.example.systemmonitor.common.Methods;
import com.example.systemmonitor.service.MvCounselling;
import com.example.systemmonitor.vo.SlackVO;
import com.example.systemmonitor.service.SlackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class LogController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Methods methods = new Methods();

    @GetMapping("/appendlog")
    public String appendLog() {
        String result = "";
        logger.error("00009-ERROR ServiceLoggingAspect");

        return result;
    }

    @GetMapping("/appendinfolog")
    public String appendInfoLog() {
        String result = "";
        logger.info("INFO Log");

        return result;
    }

    @GetMapping(value = "/setsystemmonitoring")
    public String setSystemMornitoring(HttpServletRequest request) throws Exception {
        String uri = request.getRequestURL().toString();
/*        methods.SetUrl(uri.replace(request.getRequestURI(),"") + "/demotest2/sendslackmessage",
                uri.replace(request.getRequestURI(),"") + "/demotest2/api/readlimitlist"    );*/

        methods.setUrl(uri.replace(request.getRequestURI(), "") + "/sendslackmessage",
                uri.replace(request.getRequestURI(), "") + "/api/readlimitlist",
                uri.replace(request.getRequestURI(), "") + "/api/mvhealthcheck");
        return methods.setSystemMonitoring();
    }

    @GetMapping(value = "/closesystemmornitoring")
    public String closeSystemMornitoring(HttpServletRequest request) throws Exception {
        return methods.closeSystemMonitoring();
    }

    @GetMapping(value = "/getsystemmornitoringstatus")
    public String getSystemMonitoringStatus(HttpServletRequest request) throws Exception {
        return methods.getSystemMonitoringStatus().replace("\r\n", "<br/>");
    }


    @PostMapping("/sendslackmessage")
    public String sendSlackmessage(@RequestBody SlackVO slackVO) throws Exception {
        SlackService<SlackVO> slackService = new SlackService<SlackVO>();
        slackService.setMethod(SlackService.Method.POST);
        slackService.setJsonBody(slackVO);
        slackService.sendSlackMessage(slackVO.getWebhookUrl(), slackVO);

        return slackService.resposeBody;
    }

    @PostMapping("/api/readlimitlist")
    public String readLimitList(@RequestBody SlackVO slackVO, HttpServletRequest request) throws Exception {
        try {
            return methods.readAndInitLimitList(slackVO);
        } catch (Exception ex) {
            return "ERROR " + ex.getMessage();
        }
    }

    @GetMapping("/api/test/")
    public void test() {
        String a = "\"abc\" == \"def\""; //false
        String b = "3 >=5"; //false
        String c = "\"abc\" == \"abc\""; //true
        String d = "3 <=5"; //true
        String e = "5 ==5"; //true
        String f = "5 ==5 and 5==3"; //false
        String g = "5 ==5 and 5==5"; //true
        String h = "5 ==5 or 5==5"; //true
        String i = "5 ==5 or 5!=5"; //true
        String j = "5 !=5 or 5!=5";//false

        System.out.println(stringConditionCheck(a));
        System.out.println(stringConditionCheck(b));
        System.out.println(stringConditionCheck(c));
        System.out.println(stringConditionCheck(d));
        System.out.println(stringConditionCheck(e));
        System.out.println(stringConditionCheck(f));
        System.out.println(stringConditionCheck(g));
        System.out.println(stringConditionCheck(h));
        System.out.println(stringConditionCheck(i));
        System.out.println(stringConditionCheck(j));
    }

    public boolean stringConditionCheck(String condition) {
        if (condition.replace("&&", "and").replace("||", "or").contains("and") || condition.replace("&&", "and").replace("||", "or").contains("or")) {
            if (condition.contains("or")) {
                //Or
                String[] list = condition.split(("or"));
                boolean isOneTrueCheak = false;
                for (int i = 0; i < list.length; i++) {
                    if (list[i].contains("and")) {
                        String[] andList = list[i].split(("and"));
                        boolean isAllTrueCheck = true;
                        for (String value : andList) {
                            if (!stringConditionCheck(value)) {
                                isAllTrueCheck = false;
                            }
                        }
                        if (isAllTrueCheck) {
                            isOneTrueCheak = true;
                        }
                    } else {
                        isOneTrueCheak = stringConditionCheck(list[i]);
                        if(isOneTrueCheak){
                            return true;
                        }
                    }
                }
                return isOneTrueCheak;
            } else {
                //And
                String[] list = condition.split(("and"));
                boolean isAllTrueCheck = true;
                for (String value : list) {
                    if (!stringConditionCheck(value)) {
                        isAllTrueCheck = false;
                    }
                }
                return isAllTrueCheck;
            }
        } else {
            String sign = "";
            String left = "";
            String right = "";
            boolean isString = false;
            if (condition.contains("\"")) {
                isString = true;
            }
            Pattern pattern = null;
            if (!isString) {
                pattern = Pattern.compile("(?<left>.*)(?<sign>>=|<=|==|!=)(?<right>.*)");
            } else {
                pattern = Pattern.compile("\\\"(?<left>.*?)\\\".*(?<sign>>=|<=|==|!=).*\\\"(?<right>.*?)\\\"");
            }
            Matcher m = pattern.matcher(condition);
            m.matches();
            sign = m.group("sign");
            left = m.group("left");
            right = m.group("right");
            return compare(isString, sign, left, right);
        }
    }

    public boolean compare(boolean isString, String sign, String left, String right) {
        boolean result = false;
        switch (sign) {
            case ">=":
                if (!isString) {
                    if (Long.valueOf(left.replace(" ", "")) >= Long.valueOf(right.replace(" ", ""))) {
                        result = true;
                    }
                }
                break;
            case "<=":
                if (!isString) {
                    if (Long.valueOf(left.replace(" ", "")) <= Long.valueOf(right.replace(" ", ""))) {
                        result = true;
                    }
                }
                break;
            case "==":
                if (!isString) {
                    if (Long.valueOf(left.replace(" ", "")) == Long.valueOf(right.replace(" ", ""))) {
                        result = true;
                    }
                } else {
                    if (left.equals(right)) {
                        result = true;
                    }
                }
                break;
            case "!=":
                result = false;
                if (!isString) {
                    if (Long.valueOf(left.replace(" ", "")) != Long.valueOf(right.replace(" ", ""))) {
                        result = true;
                    }
                } else {
                    if (!left.equals(right)) {
                        result = true;
                    }
                }
                break;
        }
        return result;
    }

    @PostMapping("/api/mvhealthcheck")
    public void mvHealthStatusCheck(@RequestBody SlackVO slackVO) throws Exception {
        MvCounselling mvCounselling = new MvCounselling();
        SlackService<SlackVO> slackService = new SlackService<SlackVO>();
        slackService.setMethod(SlackService.Method.POST);
        if (!mvCounselling.isAlive()) {
            slackService.setJsonBody(slackVO);
            slackService.sendSlackMessage(slackVO.getWebhookUrl(), slackVO);
        }
    }

}
