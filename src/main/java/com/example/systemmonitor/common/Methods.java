package com.example.systemmonitor.common;


import com.example.systemmonitor.vo.SlackVO;
import com.example.systemmonitor.vo.SystemVO;
import com.google.gson.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;

public class Methods {
    //private String path = "/logs/";
    private String path = "/usr/local/dy/";
    private String ymlFullPath = path + "setting/logmornitoring.yml"; //yml 경로
    private String setupFullPath = path + "setting/setup"; //환경설정 파일
    private String limitListFullPath = path + "setting/retryInfoList"; //재알림 설정파일 전체경로
    private String setupPath = path + "setting/"; //환경설정 파일 경로
    private String deleteCrontabName = "deleteCorontab.sh";
    private String addCrontabName = "addCrontab.sh";
    private String limitListFileName = "retryInfoList"; //재알림 설정파일 파일명


    private String sendSlackMessageUrl; //SlackVO Message 전송 API URL
    private String retryInfoUrl; //SlackVO Message 전송 API URL
    private String mvHealthCheckUrl;


    //로그모니터링
    private String propLogMornitoring = "logMonitoring"; //로그모니터링 할 로그파일속성명
    private String propFilename = "logFileName"; //로그모니터링 할 로그파일명
    private String propPath = "logPath"; //로그모니터링 할 로그파일 path
    private String propKeywords = "keywords"; //로그모니터링에서 확인할 키워드
    private String propAddPropertiesName = "_setup.sh"; //로그모니터링 Properties파일 네임
    private String propAddFinderName = "_finder.sh"; //로그모니터링 Finder파일 네임
    private String propLogStartupName = "_logStartup.sh"; //로그모니터링 메인파일 네임

    //health Check
    private String propHealthCheck = "healthCheck"; //로그모니터링 할 로그파일속성명
    private String propProcessName = "processName"; //로그모니터링 할 로그파일 프로세스명

    //health Check
    private String propMvHealthCheck = "mvHealthCheck"; //로그모니터링 할 로그파일속성명

    //공통
    private String propWebHookUrl = "slackUrl"; //환경설정파일 SlackVO Webhook Url
    private String propLimitTime = "LimitTime"; //재시도 제한 시간
    private String propArrayKey = "system"; //Array 키
    private String propMakePath = "shMakePath"; //sh 생성 경로
    private String propMakeFileName = "makeFileName"; //sh 파일 이름
    private String propTime = "crontabTime"; //Crontab 호출 시간
    private String propSystemName = "systemName"; //시스템이름


    public void setUrl(String sendSlackMessageUrl, String retryInfoUrl, String mvHealthCheckUrl) {
        this.sendSlackMessageUrl = sendSlackMessageUrl;
        this.retryInfoUrl = retryInfoUrl;
        this.mvHealthCheckUrl = mvHealthCheckUrl;
    }



    public String initSetup() throws Exception {
        String result = "";
        String deleteCrontabString = "";

        try {
            JsonObject setupObject = Util.ConvertStringToJsonObject(Util.ReadFile(setupFullPath));
            JsonArray logMonitoring = setupObject.getAsJsonArray(propLogMornitoring);
            if( logMonitoring != null) {
                for (JsonElement data : logMonitoring) {
                    JsonObject jsonObject = data.getAsJsonObject().get(propArrayKey).getAsJsonObject();
                    String time = Util.GetJsonObjectString(jsonObject, propTime, propPath);
                    String makePath = Util.GetJsonObjectString(jsonObject, propMakePath, propPath);
                    String makeFileName = Util.GetJsonObjectString(jsonObject, propMakeFileName, propPath);
                    if (makeFileName == "" || makePath == "" || time == "") {
                        throw new Exception("로그모니터링의 설정파일은 makeFileName, makePath, crontabTime 항목이 필수입니다 <br/>");
                    }

                    result += Util.DeleteFIle(makePath, makeFileName.split("\\.")[0] + propAddPropertiesName);
                    result += Util.DeleteFIle(makePath, makeFileName.split("\\.")[0] + propAddFinderName);
                    result += Util.DeleteFIle(makePath, makeFileName.split("\\.")[0] + propLogStartupName);
                    result += Util.DeleteFIle(setupPath, limitListFileName);

                    //기존 크론탭 제거
                    deleteCrontabString += deleteCtontabshString(makeFileName);
                }

                Util.InputFIle(setupPath, setupPath + deleteCrontabName, deleteCrontabString);
                result += Util.ExcuteShell("chmod 771 " + setupPath + deleteCrontabName); //권한변경
                result += Util.ExcuteShell(setupPath + deleteCrontabName);
                for (JsonElement data : logMonitoring) {
                    JsonObject jsonObject = data.getAsJsonObject().get(propArrayKey).getAsJsonObject();
                    String logFileName = Util.GetJsonObjectString(jsonObject, propFilename, propPath);
                    //실행중이런 Ps Kill
                    result += Util.ExcuteShell("sudo kill -9 $(ps aux | grep '" + logFileName.split("\\.")[0].toString() + "' | awk '{print $2}')");
                }
            }
            try {
                //영상상담 헬스체크 설정 삭제
                if(setupObject.getAsJsonObject().get(propMvHealthCheck) != null) {
                    JsonObject jsonObject = setupObject.getAsJsonObject().get(propMvHealthCheck).getAsJsonObject();
                    String time = Util.GetJsonObjectString(jsonObject, propTime, propPath);
                    String makePath = Util.GetJsonObjectString(jsonObject, propMakePath, propPath);
                    String makeFileName = Util.GetJsonObjectString(jsonObject, propMakeFileName, propPath);
                    result += Util.DeleteFIle(makePath, makeFileName.split("\\.")[0] + propLogStartupName);

                    //기존 크론탭 제거
                    deleteCrontabString += deleteCtontabshString(makeFileName);
                    Util.InputFIle(setupPath, setupPath + deleteCrontabName, deleteCrontabString);
                    result += Util.ExcuteShell("chmod 771 " + setupPath + deleteCrontabName); //권한변경
                    result += Util.ExcuteShell(setupPath + deleteCrontabName);
                }
            }catch(Exception ex){
                return ex.toString();
            }
        } catch (Exception ex) {
            return ex.toString();
        }

        return result;
    }

    public StringBuilderPlus addCrontabshString(String path, String fileName, String time) {
        String crontabName = path + fileName.split("\\.")[0].toString() + propLogStartupName;

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l | ");
        result.append("(cat; echo \"*/" + time + " * * * * " + crontabName + " >> /usr/local/dy/logs/crontab.log 2>&1\")  ");
        result.appendLine("| crontab -");

        return result;
    }

    public StringBuilderPlus deleteCtontabshString(String fileName) {
        String crontabName = fileName.split("\\.")[0].toString() + propLogStartupName;

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l");
        result.append("| grep -v \"" + crontabName + "\"  ");
        result.appendLine("| crontab -");

        return result;
    }

    public String setSystemMonitoring() throws Exception {

        String result = "";


        //기존 셋팅 유무 체크
        File setup = new File(setupFullPath);
        if (setup.isFile()) {
            //기존 셋팅 초기화
            result += initSetup();
        }

        try {
            result += Util.InputFIle(setupPath, setupFullPath, Util.ConvertYamlToJson(ymlFullPath));
            result += Util.InputFIle(setupPath, limitListFullPath, "");
            result += Util.ExcuteShell("chmod 771 " + setupFullPath);

        } catch (Exception ex) {
            return "셋팅 시 에러 발생 " + ex.toString();
        }

        try {

            JsonObject jsonObject = Util.ConvertStringToJsonObject(Util.ConvertYamlToJson(ymlFullPath));

            result += logMornitoring(jsonObject);
            //result += HealthCheck(jsonObject);
            result += MvHealthCheck(jsonObject);
            return result;

        } catch (Exception ex) {
            return "Error - " + ex.toString();
        }
    }

    public String closeSystemMonitoring() throws Exception{
        //기존 셋팅 유무 체크
        File setup = new File(setupFullPath);
        if (setup.isFile()) {
            //기존 셋팅 초기화
            return initSetup();
        }
        return "Setting File is Empty";
    }

    public String getSystemMonitoringStatus() throws Exception {
        String result = "";

        try {
            JsonObject setupObject = Util.ConvertStringToJsonObject(Util.ReadFile(setupFullPath));
            JsonArray logMonitoring = setupObject.getAsJsonArray(propLogMornitoring);
            for (JsonElement data : logMonitoring) {
                JsonObject jsonObject = data.getAsJsonObject().get(propArrayKey).getAsJsonObject();
                String logFileName = Util.GetJsonObjectString(jsonObject, propFilename, propPath);

                String shResult = Util.ExcuteShell("ps -ef | grep "+logFileName+" | grep -v \"grep\"");
                if(shResult.split("\r\n")[1].replace("shell result=","").replace("\n","").isEmpty()){
                    result += logFileName + " => Status <font style='color:red;'>[No Active]</font>\r\n";
                }else{
                    result += logFileName + " => Status <font style='color:green;'>[Active]</font>\r\n";
                }

            }
        } catch (Exception e) {
            return e.toString();
        }

        return result;
    }


    // 로그 모니터링
    public String logMornitoring(JsonObject jsonObject) {
        /*
         * 로그모니터링 Shell 생성
         * 로그모니터링 로직 Shell
         * 로그모니터링 시작 Shell
         * 로그모니터링 환경설정 Shell
         */
        String result = "";
        JsonArray logMonitoring = jsonObject.getAsJsonArray(propLogMornitoring);
        for (JsonElement data : logMonitoring) {
            result += addFinderShell(data); //Find 로직
            result += addLogMonitoringShell(data); //로그모니터링 시작설정
            result += addPropertiesShell(data); //키워드리스트
        }
        return result;
    }

    public String addLogMonitoringShell(JsonElement system) {

        String result = "";
        String addCrontabString = "";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String filename = Util.GetJsonObjectString(jsonObject, propFilename, propPath);
            String path = Util.GetJsonObjectString(jsonObject, propPath, propPath);
            String makeFileName = Util.GetJsonObjectString(jsonObject, propMakeFileName, propPath);
            String makePath = Util.GetJsonObjectString(jsonObject, propMakePath, propPath);

            String time = Util.GetJsonObjectString(jsonObject, propTime, propPath);
            if (filename == "" || path == "" || time == "" || makeFileName =="" || makePath=="") {
                throw new Exception("로그모니터링의 설정파일은 filename, path, time, makeFileName, makePath 항목이 필수입니다 <br/>");
            }

            String fullPath = makePath + makeFileName.split("\\.")[0].toString() + propLogStartupName;
            StringBuilderPlus sh = setLogMornitoringString(path, makePath, filename, makeFileName);

            result += Util.InputFIle(makePath, fullPath, sh.toString());

            // 권한변경, 크론탭 추가
            //result += ExcuteShell("chmod 771 " + fullPath) + AddCrontab(makePath, makeFileName, time);
            result += Util.ExcuteShell("chmod 771 " + fullPath);


            addCrontabString += addCrontabshString(makePath, makeFileName, time);
            Util.InputFIle(setupPath, setupPath + addCrontabName, addCrontabString);
            result += Util.ExcuteShell("chmod 771 " + setupPath + addCrontabName); //권한변경
            result += Util.ExcuteShell(setupPath + addCrontabName);


            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus setLogMornitoringString(String path, String makePath, String fileName, String makeFileName) {
        String onlyMakeFileName = makeFileName.split("\\.")[0].toString();
        String onlyLogFileName = fileName.split("\\.")[0].toString();

        String setupFileName = makePath + onlyMakeFileName + propAddPropertiesName;
        String finderFileName = onlyMakeFileName + propAddFinderName;

        StringBuilderPlus result = new StringBuilderPlus();
        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo $(date '+%Y-%m-%d %H:%M:%S') - Logfile [" + onlyLogFileName + "]");

        result.appendLine("filename=\"" + path + fileName + "\"");
        result.appendLine("command=" + "`ps -ef | grep $filename | grep -v \"grep\"`");

        result.appendLine("echo $command | while read file; do");
        result.appendLine("      if grep -q \"tail\" <<< \"$file\"; then");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Not Close Process\" " + onlyLogFileName);
        result.appendLine("      elif [\"\" == \"$file\"]; then");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Excuting...\" " + onlyLogFileName);
        result.appendLine("         " + makePath + finderFileName);
        result.appendLine("         break");
        result.appendLine("      else");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Error\" " + onlyLogFileName);
        result.appendLine("      fi");
        result.appendLine("done");

        return result;
    }

    public String addPropertiesShell(JsonElement system) {
        String result = "";

        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String fileName = Util.GetJsonObjectString(jsonObject, propFilename, propPath);
            String makePath = Util.GetJsonObjectString(jsonObject, propMakePath, propPath);
            String makeFileName = Util.GetJsonObjectString(jsonObject, propMakeFileName, propPath);

            if(fileName == "" || makePath == "" ){
                throw new Exception("로그모니터링의 설정파일은 filename, path, time 항목이 필수입니다<br/>");
            }

            JsonArray keywords = jsonObject.getAsJsonArray(propKeywords);
            fileName = makePath + makeFileName.split("\\.")[0].toString() + propAddPropertiesName;
            StringBuilderPlus sh = setPropertiesString(keywords);

            result += Util.InputFIle(makePath, fileName, sh.toString());

            result += Util.ExcuteShell("chmod 771 " + fileName);

            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus setPropertiesString(JsonArray keywords) {
        StringBuilderPlus result = new StringBuilderPlus();
        String gbn = "";
        result.append("keywords=(");
        for (JsonElement keyword : keywords) {
            result.append(gbn + keyword.toString());
            gbn = " ";
        }
        result.append(")");
        return result;
    }

    public String addFinderShell(JsonElement system) {

        String result = "";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            SystemVO systemVO = new SystemVO();
            setSystemVO(jsonObject, systemVO);
            if(systemVO.getFileName().isEmpty() || systemVO.getPath().isEmpty() || systemVO.getWebhookUrl().isEmpty() ){
                throw new Exception("로그모니터링의 설정파일은 filename, logPath, webhookUrl 항목이 필수입니다<br/>");
            }
            StringBuilderPlus sh = setFindershString(systemVO);
            String fullPath = systemVO.getMakePath() + systemVO.getMakeFileName().split("\\.")[0].toString() + propAddFinderName;

            result += Util.InputFIle(systemVO.getMakePath(), fullPath, sh.toString());

            result += Util.ExcuteShell("chmod 771 " + fullPath);

            return result;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus setFindershString(SystemVO vo) {
        StringBuilderPlus result = new StringBuilderPlus();
        String onlyMakeFileName = vo.getMakeFileName().split("\\.")[0].toString();
        String onlyLogFileName =vo.getFileName().split("\\.")[0].toString();
        String setupFileName = vo.getMakePath() + onlyMakeFileName + propAddPropertiesName;


        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo \"$(date '+%Y-%m-%d %H:%M:%S') - Start " + onlyLogFileName + " Finder Shell !!\"");

        result.appendLine("sendUrl=\"" + sendSlackMessageUrl + "\"");
        result.appendLine("retryInfoUrl=\"" + retryInfoUrl + "\"");
        result.appendLine("tail " + vo.getPath() + vo.getFileName() + " -n0 -F | while read line;");
        result.appendLine("do");
        result.appendLine("     for i in ${!keywords[*]};");
        result.appendLine("     do");
        result.appendLine("         if grep -q \"${keywords[i]}\" <<< \"$line\" ; then");
        result.appendLine("             param=\"{");
        result.appendLine("             \\\"text\\\":\\\"[ " + vo.getSystemName() + "| $(date '+%Y-%m-%d %H:%M:%S') ]  - Fine By Keyword - [${keywords[i]}] => " + vo.getFileName() + "\\\",");
        result.appendLine("             \\\"keyword\\\":\\\"${keywords[i]}\\\",");
        result.appendLine("             \\\"logFileName\\\":\\\"" + onlyLogFileName + "\\\",");
        result.appendLine("             \\\"webhookUrl\\\":\\\"" + vo.getWebhookUrl() + "\\\",");
        result.appendLine("             \\\"limitTime\\\":\\\"" + vo.getLimitTime() + "\\\",");
        result.appendLine("             \\\"system\\\":\\\"LogMornitoring\\\"");
        result.appendLine("             }\"");

        //Limit Time 초과된 키워드들 초기화
        result.appendLine("             if grep -q \"${keywords[i]} " + onlyLogFileName + "\" <<< `curl $retryInfoUrl -H \"Content-Type: application/json\" -d \"$param\"` ; then ");
        result.appendLine("                echo $(date '+%Y-%m-%dT%H:%M:%S') - \"retry read ERROR KEYWORK - ${keywords[i]}\" " + vo.getFileName());
        result.appendLine("                break");
        result.appendLine("             fi");

        result.appendLine("             curl $sendUrl -H \"Content-Type: application/json\" -d \"$param\"");
        result.appendLine("             echo \"\"");
        result.appendLine("             #pkill -9 -P $$ tail");
        result.appendLine("             break");
        result.appendLine("         fi");
        result.appendLine("     done");
        result.appendLine("done;");

        return result;
    }

    // HealthCheck Mornitoring
    public String healthCheck(JsonObject jsonObject) {
        String result = "";
        JsonArray healthCheck = jsonObject.getAsJsonArray("healthCheck");
        for (JsonElement data : healthCheck) {
            result += addHealthCheckShell(data);
        }
        return result;
    }

    public String addHealthCheckShell(JsonElement system) {
        String result = "";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            SystemVO systemVO = new SystemVO();
            setSystemVO(jsonObject, systemVO);


            String fileName = Util.GetJsonObjectString(jsonObject, propMakeFileName, propPath);
            String time = Util.GetJsonObjectString(jsonObject, propTime, propPath);
            String makePath = Util.GetJsonObjectString(jsonObject, propMakePath, propPath);
            String processName = jsonObject.get(propProcessName).toString().replace("\"", "");
            String fullPath = makePath + fileName.split("\\.")[0].toString() + ".sh";


            if (systemVO.getMakeFileName().isEmpty() || systemVO.getMakePath().isEmpty() || systemVO.getProcessName().isEmpty() || systemVO.getCrontabTime().isEmpty()) {
                throw new Exception("Health Check 의 설정파일은 filename, makePilemakePath, ctontabTime, processName 항목이 필수입니다 <br/>");
            }

            StringBuilderPlus sh = setLogMornitoringString(makePath, makePath, fileName, "");

            result += Util.InputFIle(makePath, fullPath, sh.toString());

            // 권한변경, 크론탭 추가
            //result += excuteShell("chmod 722 " + fullPath) +
            //         addCrontab(makePath, fileName, time);
            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    //MvMonitoring
    public String MvHealthCheck(JsonElement mvSystem){
        String result = "";
        try {
            JsonObject jsonObject = mvSystem.getAsJsonObject().get(propMvHealthCheck).getAsJsonObject();
            SystemVO systemVO = new SystemVO();
            setSystemVO(jsonObject, systemVO);
            String startFileName = systemVO.getMakeFileName().split("\\.")[0].toString() + propLogStartupName;
            String fullPath = systemVO.getMakePath() + startFileName;

            if (startFileName.isEmpty() || systemVO.getMakePath().isEmpty() || systemVO.getCrontabTime().isEmpty()) {
                throw new Exception("Health Check 의 설정파일은 filename, makePath, ctontabTime, processName 항목이 필수입니다 <br/>");
            }

            String content = getMvHealthCheckString(systemVO);

            result += Util.InputFIle(systemVO.getMakePath(), fullPath, content.toString());
            result += Util.ExcuteShell("chmod 771 " + fullPath);

            String addCrontabString = "";
            addCrontabString += addCrontabshString(systemVO.getMakePath(), systemVO.getMakeFileName(), systemVO.getCrontabTime());
            Util.InputFIle(setupPath, setupPath + addCrontabName, addCrontabString);
            result += Util.ExcuteShell("chmod 771 " + setupPath + addCrontabName); //권한변경
            result += Util.ExcuteShell(setupPath + addCrontabName);

            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public String getMvHealthCheckString(SystemVO systemVO){
        String makeFileName =systemVO.getMakeFileName().split("\\.")[0].toString() + propLogStartupName;

        StringBuilderPlus stringBuilderPlus = new StringBuilderPlus();
        //Limit Time 초과된 키워드들 초기화
        stringBuilderPlus.appendLine("param=\"{");
        stringBuilderPlus.appendLine("  \\\"text\\\":\\\"[" + systemVO.getSystemName() + "|$(date '+%Y-%m-%d %H:%M:%S')] - MvCounSelling Is Die !!! \\\",");
        stringBuilderPlus.appendLine("  \\\"keyword\\\":\\\"MvCounSelling\\\",");
        stringBuilderPlus.appendLine("  \\\"logFileName\\\":\\\""  + makeFileName + "\\\",");
        stringBuilderPlus.appendLine("  \\\"webhookUrl\\\":\\\"" + systemVO.getWebhookUrl() + "\\\",");
        stringBuilderPlus.appendLine("  \\\"limitTime\\\":\\\"" + systemVO.getCrontabTime() + "\\\"");
        stringBuilderPlus.appendLine("}\"");
        stringBuilderPlus.appendLine("retryInfoUrl=\"" + retryInfoUrl + "\"");
        stringBuilderPlus.appendLine("sendUrl=\"" + mvHealthCheckUrl + "\"");
        stringBuilderPlus.appendLine("if grep -q \"MvCounSelling " + makeFileName + "\" <<< `curl $retryInfoUrl -H \"Content-Type: application/json\" -d \"$param\"` ; then ");
        stringBuilderPlus.appendLine("   echo $(date '+%Y-%m-%dT%H:%M:%S') - \"retry Limit MvCounselling\" " + makeFileName);
        stringBuilderPlus.appendLine("else");
        stringBuilderPlus.appendLine("curl $sendUrl -H \"Content-Type: application/json\" -d \"$param\"");
        stringBuilderPlus.appendLine("fi");
        return stringBuilderPlus.toString();
    }


    public String readAndInitLimitList(SlackVO slackVO) throws Exception {

        try {

            ArrayList<String> limitList = new ArrayList<String>();
            StringBuilderPlus sb = new StringBuilderPlus();
            BufferedReader reader = new BufferedReader(
                    new FileReader(limitListFullPath)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                limitList.add(line);
            }
            reader.close();


            Iterator<String> iterator = limitList.iterator();
            while (iterator.hasNext()) {
                try {
                    String data = iterator.next();
                    if (data.isEmpty()) {
                        continue;
                    }

                    String strLimitDate = data.split(" ")[0];
                    String keyword = data.split(" ")[1];
                    String fileNmae = data.split(" ")[2];
                    Long limitTime = Long.parseLong(slackVO.getLimitTime());

                    if (!fileNmae.equals(slackVO.getLogFileName()) || !keyword.equals(slackVO.getKeyword())) {
                        continue;
                    }

                    LocalDateTime beforeDate = LocalDateTime.now().minusMinutes(limitTime);
                    LocalDateTime limitDate = LocalDateTime.parse(strLimitDate);
                    if (beforeDate.isAfter(limitDate)) {
                        iterator.remove();
                    }
                } catch (Exception ex) {
                }
            }

            sb.append(String.join("\n", limitList));
            return sb.toString();
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    public ArrayList<String> readAndInitLimitListReturnArrayList(SlackVO slackVO) throws Exception {

        try {

            ArrayList<String> limitList = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(
                    new FileReader(limitListFullPath)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                limitList.add(line);
            }
            reader.close();


            Iterator<String> iterator = limitList.iterator();
            while (iterator.hasNext()) {
                try {
                    String data = iterator.next();
                    if (data.isEmpty()) {
                        continue;
                    }

                    String strLimitDate = data.split(" ")[0];
                    String keyword = data.split(" ")[1];
                    String fileNmae = data.split(" ")[2];
                    Long limitTime = Long.parseLong(slackVO.getLimitTime());

                    if(!fileNmae.equals(slackVO.getLogFileName()) || !keyword.equals(slackVO.getKeyword())){
                        continue;
                    }

                    LocalDateTime beforeDate = LocalDateTime.now().minusMinutes(limitTime);
                    LocalDateTime limitDate = LocalDateTime.parse(strLimitDate);
                    if (beforeDate.isAfter(limitDate)) {
                        iterator.remove();
                    }
                } catch (Exception ex) {
                }
            }
            return limitList;
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    public void writeLimitList(String filename, String keyword, String limitTime, ArrayList<String> contents) throws Exception {

        try {

            StringBuilderPlus result = new StringBuilderPlus();
            for (String limit : contents) {
                result.appendLine(limit);
            }
            result.appendLine(LocalDateTime.now().toString() + " " + keyword + " " + filename);
            Util.InputFIle(setupPath, limitListFullPath, result.toString());

        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    private void setSystemVO(JsonObject jsonObject, SystemVO systemVO) {
        systemVO.setFileName(Util.GetJsonObjectString(jsonObject, propFilename, propPath));
        systemVO.setPath(Util.GetJsonObjectString(jsonObject, propPath, propPath));
        systemVO.setMakePath(Util.GetJsonObjectString(jsonObject, propMakePath, propPath));
        systemVO.setMakeFileName(Util.GetJsonObjectString(jsonObject, propMakeFileName, propPath));
        systemVO.setWebhookUrl(Util.GetJsonObjectString(jsonObject, propWebHookUrl, propPath));
        systemVO.setLimitTime(Util.GetJsonObjectString(jsonObject, propLimitTime, propPath));
        systemVO.setSystemName(Util.GetJsonObjectString(jsonObject, propSystemName, propPath));
        systemVO.setCrontabTime(Util.GetJsonObjectString(jsonObject, propTime, propPath));
        systemVO.setProcessName(Util.GetJsonObjectString(jsonObject, propProcessName, propPath));

    }


}
