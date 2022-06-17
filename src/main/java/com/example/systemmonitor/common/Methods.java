package com.example.systemmonitor.common;


import com.example.systemmonitor.dto.Slack;
import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class Methods {
    //private String path = "/usr/local/dy/";
    private String path = "/usr/local/dy/";
    private String ymlFullPath = path + "setting/logmornitoring.yml"; //yml 경로
    private String setupFullPath = path + "setting/setup"; //환경설정 파일
    private String limitListFullPath = path + "setting/retryInfoList"; //재알림 설정파일 전체경로
    private String setupPath = path + "setting/"; //환경설정 파일 경로
    private String deleteCrontabName = "deleteCorontab.sh";
    private String addCrontabName = "addCrontab.sh";
    private String limitListFileName = "retryInfoList"; //재알림 설정파일 파일명


    private String osName = System.getProperty("os.name").toLowerCase();
    private String sendSlackMessageUrl; //Slack Message 전송 API URL
    private String retryInfoUrl; //Slack Message 전송 API URL


    //로그모니터링
    private String propLogMornitoring = "logMonitoring"; //로그모니터링 할 로그파일속성명
    private String propFilename = "logFileName"; //로그모니터링 할 로그파일명
    private String propPath = "logPath"; //로그모니터링 할 로그파일 path
    private String propKeywords = "keywords"; //로그모니터링에서 확인할 키워드
    private String propTime = "crontabTime"; //Crontab 호출 시간
    private String propAddPropertiesName = "_setup.sh"; //로그모니터링 Properties파일 네임
    private String propAddFinderName = "_finder.sh"; //로그모니터링 Finder파일 네임
    private String propLogStartupName = "_logStartup.sh"; //로그모니터링 메인파일 네임

    //health Check
    private String propHealthCheck = "healthCheck"; //로그모니터링 할 로그파일속성명
    private String propProcessName = "processName"; //로그모니터링 할 로그파일 프로세스명


    //공통
    private String propWebHookUrl = "slackUrl"; //환경설정파일 Slack Webhook Url
    private String propRetryTime = "LimitTime"; //재시도 제한 시간
    private String propArrayKey = "system"; //Array 키
    private String propMakePath = "shMakePath"; //sh 생성 경로
    private String propMakeFileName = "makeFileName"; //sh 파일 이름


    public void SetUrl(String sendSlackMessageUrl, String retryInfoUrl) {
        this.sendSlackMessageUrl = sendSlackMessageUrl;
        this.retryInfoUrl = retryInfoUrl;
    }

    public String ConvertYamlToJson() {
        String json = "";
        File file = new File(ymlFullPath);
        Yaml yaml = new Yaml();

        try {
            Object loadedYaml = yaml.load(new FileReader(file));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            json = gson.toJson(loadedYaml, LinkedHashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    public JsonObject ConvertStringToJsonObject(String content) throws Exception {
        JsonObject jsonObject = null;
        try {
            jsonObject = new Gson().fromJson(content, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String SetSystemMonitoring() throws Exception {

        String result = "";


        //기존 셋팅 유무 체크
        File setup = new File(setupFullPath);
        if (setup.isFile()) {
            //기존 셋팅 초기화
            result += InitSetup();
        }

        try {
            result += InputFIle(setupPath, setupFullPath, ConvertYamlToJson());
            result += InputFIle(setupPath, limitListFullPath, "");
            result += ExcuteShell("chmod 771 " + setupFullPath);

        } catch (Exception ex) {
            return "셋팅 시 에러 발생 " + ex.toString();
        }

        try {

            JsonObject jsonObject = ConvertStringToJsonObject(ConvertYamlToJson());

            result += LogMornitoring(jsonObject);
            //result += HealthCheck(jsonObject);
            return result;

        } catch (Exception ex) {
            return "Error - " + ex.toString();
        }
    }

    public String CloseSystemMonitoring() throws Exception{
        //기존 셋팅 유무 체크
        File setup = new File(setupFullPath);
        if (setup.isFile()) {
            //기존 셋팅 초기화
            return InitSetup();
        }
        return "Setting File is Empty";
    }

    public String GetSystemMonitoringStatus() throws Exception {
        String result = "";

        try {
            JsonObject setupObject = ConvertStringToJsonObject(ReadFile(setupFullPath));
            JsonArray logMonitoring = setupObject.getAsJsonArray(propLogMornitoring);
            for (JsonElement data : logMonitoring) {
                JsonObject jsonObject = data.getAsJsonObject().get(propArrayKey).getAsJsonObject();
                String logFileName = GetJsonObjectString(jsonObject, propFilename);

                String shResult = ExcuteShell("ps -ef | grep "+logFileName+" | grep -v \"grep\"");
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
    public String LogMornitoring(JsonObject jsonObject) {
        /*
         * 로그모니터링 Shell 생성
         * 로그모니터링 로직 Shell
         * 로그모니터링 시작 Shell
         * 로그모니터링 환경설정 Shell
         */
        String result = "";
        JsonArray logMonitoring = jsonObject.getAsJsonArray(propLogMornitoring);
        for (JsonElement data : logMonitoring) {
            result += AddFinderShell(data); //Find 로직
            result += AddLogMonitoringShell(data); //로그모니터링 시작설정
            result += AddPropertiesShell(data); //키워드리스트
        }
        return result;
    }

    public String AddLogMonitoringShell(JsonElement system) {

        String result = "";
        String addCrontabString = "";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String filename = GetJsonObjectString(jsonObject, propFilename);
            String path = GetJsonObjectString(jsonObject, propPath);
            String makeFileName = GetJsonObjectString(jsonObject, propMakeFileName);
            String makePath = GetJsonObjectString(jsonObject, propMakePath);

            String time = GetJsonObjectString(jsonObject, propTime);
            if (filename == "" || path == "" || time == "" || makeFileName =="" || makePath=="") {
                throw new Exception("로그모니터링의 설정파일은 filename, path, time, makeFileName, makePath 항목이 필수입니다 <br/>");
            }

            String fullPath = makePath + makeFileName.split("\\.")[0].toString() + propLogStartupName;
            StringBuilderPlus sh = SetLogMornitoringString(path, makePath, filename, makeFileName);

            result += InputFIle(path, fullPath, sh.toString());

            // 권한변경, 크론탭 추가
            //result += ExcuteShell("chmod 771 " + fullPath) + AddCrontab(makePath, makeFileName, time);
            result += ExcuteShell("chmod 771 " + fullPath);


            addCrontabString += AddCrontabshString(makePath, makeFileName, time);
            InputFIle(setupPath, setupPath + addCrontabName, addCrontabString);
            result += ExcuteShell("chmod 771 " + setupPath + addCrontabName); //권한변경
            result += ExcuteShell(setupPath + addCrontabName);


            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus SetLogMornitoringString(String path, String makePath, String fileName, String makeFileName) {
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

    public String AddPropertiesShell(JsonElement system) {
        String result = "";

        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String fileName = GetJsonObjectString(jsonObject, propFilename);
            String makePath = GetJsonObjectString(jsonObject, propMakePath);
            String makeFileName = GetJsonObjectString(jsonObject, propMakeFileName);

            if(fileName == "" || makePath == "" ){
                throw new Exception("로그모니터링의 설정파일은 filename, path, time 항목이 필수입니다<br/>");
            }

            JsonArray keywords = jsonObject.getAsJsonArray(propKeywords);
            fileName = makePath + makeFileName.split("\\.")[0].toString() + propAddPropertiesName;
            StringBuilderPlus sh = SetPropertiesString(keywords);

            result += InputFIle(makePath, fileName, sh.toString());

            result += ExcuteShell("chmod 771 " + fileName);

            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus SetPropertiesString(JsonArray keywords) {
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

    public String AddFinderShell(JsonElement system) {

        String result = "";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String fileName = GetJsonObjectString(jsonObject, propFilename);
            String path = GetJsonObjectString(jsonObject, propPath);
            String makePath = GetJsonObjectString(jsonObject, propMakePath);
            String makeFileName = GetJsonObjectString(jsonObject, propMakeFileName);
            String webhookUrl = GetJsonObjectString(jsonObject, propWebHookUrl);

            if(fileName.isEmpty() || path.isEmpty() || webhookUrl.isEmpty() ){
                throw new Exception("로그모니터링의 설정파일은 filename, path, webhookUrl 항목이 필수입니다<br/>");
            }
            StringBuilderPlus sh = SetFindershString(path, makePath, fileName, makeFileName, webhookUrl);
            String fullPath = makePath + makeFileName.split("\\.")[0].toString() + propAddFinderName;

            result += InputFIle(path, fullPath, sh.toString());

            result += ExcuteShell("chmod 771 " + fullPath);

            return result;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus SetFindershString(String path, String makePath, String logFileName, String makeFileName, String webhookUrl) {
        StringBuilderPlus result = new StringBuilderPlus();
        String onlyMakeFileName = makeFileName.split("\\.")[0].toString();
        String onlyLogFileName =logFileName.split("\\.")[0].toString();
        String setupFileName = makePath + onlyMakeFileName + propAddPropertiesName;


        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo \"$(date '+%Y-%m-%d %H:%M:%S') - Start " + onlyLogFileName + " Finder Shell !!\"");

        result.appendLine("sendUrl=\"" + sendSlackMessageUrl + "\"");
        result.appendLine("retryInfoUrl=\"" + retryInfoUrl + "\"");
        result.appendLine("tail " + path + logFileName + " -n0 -F | while read line;");
        result.appendLine("do");
        result.appendLine("     for i in ${!keywords[*]};");
        result.appendLine("     do");
        result.appendLine("         if grep -q \"${keywords[i]}\" <<< \"$line\" ; then");
        result.appendLine("             param=\"{");
        result.appendLine("             \\\"text\\\":\\\"$(date '+%Y-%m-%d %H:%M:%S')  Fine By Keyword - [${keywords[i]}] => " + logFileName + "\\\",");
        result.appendLine("             \\\"keyword\\\":\\\"${keywords[i]}\\\",");
        result.appendLine("             \\\"logFileName\\\":\\\"" + onlyLogFileName + "\\\",");
        result.appendLine("             \\\"webhookUrl\\\":\\\"" + webhookUrl + "\\\",");
        result.appendLine("             \\\"limitTime\\\":\\\"5\\\",");
        result.appendLine("             \\\"system\\\":\\\"LogMornitoring\\\"");
        result.appendLine("             }\"");

        //Limit Time 초과된 키워드들 초기화
        result.appendLine("             if grep -q \"${keywords[i]} " + onlyLogFileName + "\" <<< `curl $retryInfoUrl -H \"Content-Type: application/json\" -d \"$param\"` ; then ");
        result.appendLine("                echo $(date '+%Y-%m-%dT%H:%M:%S') - \"retry read ERROR KEYWORK - ${keywords[i]}\" " + logFileName);
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
    public String HealthCheck(JsonObject jsonObject) {
        String result = "";
        JsonArray healthCheck = jsonObject.getAsJsonArray("healthCheck");
        for (JsonElement data : healthCheck) {
            result += AddHealthCheckShell(data);
        }
        return result;
    }

    public String AddHealthCheckShell(JsonElement system) {
        String result = "";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String fileName = GetJsonObjectString(jsonObject, propMakeFileName);
            String time = GetJsonObjectString(jsonObject, propTime);
            String makePath = GetJsonObjectString(jsonObject, propMakePath);
            String processName = jsonObject.get(propProcessName).toString().replace("\"", "");
            String fullPath = makePath + fileName.split("\\.")[0].toString() + ".sh";


            if(fileName.isEmpty() || makePath.isEmpty() || processName.isEmpty() || time.isEmpty()){
                throw new Exception("Health Check 의 설정파일은 filename, makePath, ctontabTime, processName 항목이 필수입니다 <br/>");
            }

            StringBuilderPlus sh = SetLogMornitoringString(makePath, makePath, fileName, "");

            result += InputFIle(makePath, fullPath, sh.toString());

            // 권한변경, 크론탭 추가
            //result += excuteShell("chmod 722 " + fullPath) +
             //         addCrontab(makePath, fileName, time);
            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }


    public String InitSetup() throws Exception {
        String result = "";
        String deleteCrontabString = "";

        try {
            JsonObject setupObject = ConvertStringToJsonObject(ReadFile(setupFullPath));
            JsonArray logMonitoring = setupObject.getAsJsonArray(propLogMornitoring);

            for (JsonElement data : logMonitoring) {
                JsonObject jsonObject = data.getAsJsonObject().get(propArrayKey).getAsJsonObject();
                String time = GetJsonObjectString(jsonObject, propTime);
                String logFileName = GetJsonObjectString(jsonObject, propFilename);
                String makePath = GetJsonObjectString(jsonObject, propMakePath);
                String makeFileName = GetJsonObjectString(jsonObject, propMakeFileName);
                String fullPath = makePath + makeFileName;
                if(makeFileName == "" || makePath == "" || time == ""){
                    throw new Exception("로그모니터링의 설정파일은 makeFileName, makePath, time 항목이 필수입니다 <br/>");
                }

                result += DeleteFIle(makePath, makeFileName.split("\\.")[0] + propAddPropertiesName);
                result += DeleteFIle(makePath, makeFileName.split("\\.")[0] + propAddFinderName);
                result += DeleteFIle(makePath, makeFileName.split("\\.")[0] + propLogStartupName);
                result += DeleteFIle(setupPath, limitListFileName );

                //기존 크론탭 제거
                deleteCrontabString += DeleteCtontabshString(makeFileName);
            }

            InputFIle(setupPath, setupPath + deleteCrontabName, deleteCrontabString);
            result += ExcuteShell("chmod 771 " + setupPath + deleteCrontabName); //권한변경
            result += ExcuteShell(setupPath + deleteCrontabName);
            for (JsonElement data : logMonitoring) {
                JsonObject jsonObject = data.getAsJsonObject().get(propArrayKey).getAsJsonObject();
                String logFileName = GetJsonObjectString(jsonObject, propFilename);
                //실행중이런 Ps Kill
                result += ExcuteShell("sudo kill -9 $(ps aux | grep '" + logFileName.split("\\.")[0].toString() + "' | awk '{print $2}')");
            }

        } catch (Exception e) {
            return e.toString();
        }

        return result;
    }

    public String AddCrontab(String path, String makeFileName, String time) {
        String result = "";
        try {
            StringBuilderPlus sh = AddCrontabshString(path, makeFileName, time);
            //크론탭 추가

            result += ExcuteShell(sh.toString());
            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public StringBuilderPlus AddCrontabshString(String path, String fileName, String time) {
        String crontabName = path + fileName.split("\\.")[0].toString() + propLogStartupName;

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l | ");
        result.append("(cat; echo \"*/" + time + " * * * * " + crontabName + " >> /usr/local/dy/logs/crontab.log 2>&1\")  ");
        result.appendLine("| crontab -");

        return result;
    }

    public String DeleteCrontab(String path, String logFileName, String makeFileName, String time) {
        String result = "";
        try {
            StringBuilderPlus sh = DeleteCtontabshString(makeFileName);

            //Crontab 제거 Shell 실행
            result += ExcuteShell(sh.toString());

            //실행중이런 Ps Kill
            result += ExcuteShell("sudo kill -9 $(ps aux | grep '" + logFileName.split("\\.")[0].toString() + "' | awk '{print $2}')");

            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public StringBuilderPlus DeleteCtontabshString(String fileName) {
        String crontabName = fileName.split("\\.")[0].toString() + propLogStartupName;

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l");
        result.append("| grep -v \"" + crontabName + "\"  ");
        result.appendLine("| crontab -");

        return result;
    }



    public String ReadAndInitLimitList(Slack slack) throws Exception {

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
                    Long limitTime = Long.parseLong(slack.getLimitTime());

                    if (!fileNmae.equals(slack.getLogFileName()) || !keyword.equals(slack.getKeyword())) {
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

    public ArrayList<String> ReadAndInitLimitListReturnArrayList(Slack slack) throws Exception {

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
                    Long limitTime = Long.parseLong(slack.getLimitTime());

                    if(!fileNmae.equals(slack.getLogFileName()) || !keyword.equals(slack.getKeyword())){
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
    public void WriteLimitList(String filename, String keyword, String limitTime, ArrayList<String> contents) throws Exception {

        try {

            StringBuilderPlus result = new StringBuilderPlus();
            for (String limit : contents) {
                result.appendLine(limit);
            }
            result.appendLine(LocalDateTime.now().toString() + " " + keyword + " " + filename);
            InputFIle(setupPath, limitListFullPath, result.toString());

        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    public String InputFIle(String path, String fullPath, String content) throws Exception {
        StringBuilderPlus result = new StringBuilderPlus();
        result.append(DateFormmat());
        try {
            File file = new File(path);
            if(!file.isDirectory()){
                file.mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(new File(fullPath), false);

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fullPath));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            fileOutputStream.close();
            bufferedWriter.close();

            result.append("InputFIle Success - " + fullPath);
        } catch (Exception e) {
            result.append(fullPath + "InputFIle Fail  - " + e.toString());
        }

        result.append("<br/>");
        return result.toString();
    }

    public String DeleteFIle(String path, String fileName) throws Exception {
        StringBuilderPlus result = new StringBuilderPlus();
        result.append(DateFormmat());
        try {
            File file = new File(path);
            if(!file.isDirectory()){
                file.mkdirs();
            }

            file = new File(path + fileName);
            if(file.exists()){
                file.delete();
            }
            result.appendLine("DeleteFIle Success - " + path + fileName);

        } catch (Exception e) {
            result.append(fileName + "DeleteFIle Fail  - " + e.toString());
        }

        result.append("<br/>");
        return result.toString();
    }

    public String ExcuteShell(String sh) {
        StringBuilderPlus result = new StringBuilderPlus();
        result.append(DateFormmat());

        StringBuilderPlus sp = new StringBuilderPlus();
        Process p;

        try {

            //이 변수에 명령어를 넣어주면 된다.
            String[] cmd = {"/bin/bash", "-c", sh};
            p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null)
                sp.appendLine(line);

            // shell 실행시 에러 발생한경우
            BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            line = "";
            while ((line = errorBufferedReader.readLine()) != null)
                sp.appendLine(line);

            p.waitFor();
            p.destroy();

            result.appendLine("ExcuteShell Success -  " + sh.toString() + "\r\n");
            result.appendLine("shell result=" + sp.toString()+ "\r\n");

        } catch (Exception e) {
            result.append("ExcuteShell Fail - ");
            if (osName.contains("win"))
                result.append("Not Support OS! (windows) =>  ");
            result.append(sh.toString());
        }
        result.append("<br/>");
        return result.toString();
    }

    public String ReadFile(String fullPath) throws IOException {
        StringBuilderPlus result = new StringBuilderPlus();
        BufferedReader reader = new BufferedReader(
                new FileReader(fullPath)
        );
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();

        return result.toString();
    }

    public String DateFormmat() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss | "));
    }

    public String GetJsonObjectString(JsonObject jsonObject, String key){
        String result = "";
        if(jsonObject.get(key) != null) {
            result = jsonObject.get(key).toString().replace("\"", "");
        }else{
            switch (key){
                case "propMakePath":
                    if(jsonObject.get(propPath) != null) {
                        result = jsonObject.get(propPath).toString().replace("\"", "");
                    }else{
                        result = "/usr/local/dy/sh";
                    }
            }
        }
        return result;
    }
}
