package com.example.systemmonitor.common;


import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

public class Methods {
    private String ymlFullPath = "/logs/setting/logmornitoring.yml"; //yml 경로
    private String setupFullPath = "/logs/setting/setup"; //환경설정 파일 경로
    private String setupPath = "/logs/setting/"; //환경설정 파일 경로

    private String osName = System.getProperty("os.name").toLowerCase();
    private String sendSlackMessageUrl; //Slack Message 전송 API URL


    //로그모니터링
    private String propLogMornitoring = "logMonitoring"; //로그모니터링 할 로그파일속성명
    private String propFilename = "filename"; //로그모니터링 할 로그파일속성명
    private String propPath = "path"; //로그모니터링 할 로그파일 path
    private String propKeywords = "keywords"; //로그모니터링에서 확인할 키워드
    private String propTime = "time"; //Crontab 호출 시간
    private String propAddPropertiesName = "_setup.sh"; //로그모니터링 Properties파일 네임
    private String propAddFinderName = "_finder.sh"; //로그모니터링 Properties파일 네임
    private String propLogStartupName = "_logStartup.sh"; //로그모니터링 Properties파일 네임

    //health Check
    private String propHealthCheck = "healthCheck"; //로그모니터링 할 로그파일속성명
    private String propProcessName = "processName"; //로그모니터링 할 로그파일 프로세스명


    //공통
    private String propWebHookUrl = "slackUrl"; //환경설정파일 Slack Webhook Url
    private String propArrayKey = "system"; //Array 키
    private String propMakePath = "makePath"; //로그모니터링 할 로그파일 path


    public void SetSendSlackUrl(String sendSlackMessageUrl) {
        this.sendSlackMessageUrl = sendSlackMessageUrl;
    }

    public String convertYamlToJson() {
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

    public JsonObject convertStringToJsonObject(String content) throws Exception {
        JsonObject jsonObject = null;
        try {
            jsonObject = new Gson().fromJson(content, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
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
            result += inputFIle(setupPath, setupFullPath, convertYamlToJson());

            result += excuteShell("chmod 722 " + setupFullPath);

        } catch (Exception ex) {
            return ex.toString();
        }

        try {

            JsonObject jsonObject = convertStringToJsonObject(convertYamlToJson());

            result += LogMornitoring(jsonObject);
            //result += HealthCheck(jsonObject);
            return result;

        } catch (Exception ex) {
            return "Error - " + ex.toString();
        }
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
            result += addFinderShell(data);
            result += addLogMonitoringShell(data);
            result += addPropertiesShell(data);
        }
        return result;
    }

    public String addLogMonitoringShell(JsonElement system) {

        String result = "";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String filename = getJsonObjectString(jsonObject, propFilename);
            String path = getJsonObjectString(jsonObject, propPath);

            String time = getJsonObjectString(jsonObject, propTime);
            if(filename == "" || path == "" || time == ""){
                throw new Exception("로그모니터링의 설정파일은 filename, path, time 항목이 필수입니다 <br/>");
            }
            String makePath = getJsonObjectString(jsonObject, "makePath");
            String fullPath = makePath + filename.split("\\.")[0].toString() + propLogStartupName;
            StringBuilderPlus sh = setLogmonitoringshString(path, makePath, filename);
            result += inputFIle(path, fullPath, sh.toString());

            // 권한변경, 크론탭 추가
            result += excuteShell("chmod 722 " + fullPath) + addCrontab(makePath, filename, time);
            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus setLogmonitoringshString(String path, String makePath, String fileName) {
        String onlyFileName = fileName.split("\\.")[0].toString();
        String setupFileName = makePath + onlyFileName + propAddPropertiesName;
        String finderFileName = onlyFileName + propAddFinderName;

        StringBuilderPlus result = new StringBuilderPlus();
        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo $(date '+%Y-%m-%d %H:%M:%S') - " + onlyFileName);
        //result.appendLine("echo $keywords");

        result.appendLine("filename=\"" + path + fileName + "\"");
        result.appendLine("command=" + "`ps -ef | grep $filename | grep -v \"grep\"`");
        //result.appendLine("echo $command");
        //result.appendLine("while read file; do");
        result.appendLine("echo $command | while read file; do");
        result.appendLine("      if grep -q \"tail\" <<< \"$file\"; then");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Not Close Process\" " + onlyFileName);
        result.appendLine("      elif [\"\" == \"$file\"]; then");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Excuting...\" " + onlyFileName);
        result.appendLine("         " + makePath + finderFileName);
        result.appendLine("         break");
        result.appendLine("      else");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Error\" " + onlyFileName);
        result.appendLine("      fi");
        result.appendLine("done");
        //result.appendLine("done < <(echo \"$command\")");

        return result;
    }

    public String addPropertiesShell(JsonElement system) {
        String result = "";

        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String fileName = getJsonObjectString(jsonObject, propFilename);
            String makePath = getJsonObjectString(jsonObject, propMakePath);

            if(fileName == "" || makePath == "" ){
                throw new Exception("로그모니터링의 설정파일은 filename, path, time 항목이 필수입니다<br/>");
            }

            JsonArray keywords = jsonObject.getAsJsonArray(propKeywords);
            fileName = makePath + fileName.split("\\.")[0].toString() + propAddPropertiesName;

            StringBuilderPlus sh = setLogmonitoringshString(keywords);

            result += inputFIle(makePath, fileName, sh.toString());

            result += excuteShell("chmod 722 " + fileName);

            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus setLogmonitoringshString(JsonArray keywords) {
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
            String fileName = getJsonObjectString(jsonObject, propFilename);
            String path = getJsonObjectString(jsonObject, propPath);
            String makePath = getJsonObjectString(jsonObject, propMakePath);
            String webhookUrl = getJsonObjectString(jsonObject, propWebHookUrl);

            if(fileName.isEmpty() || path.isEmpty() || webhookUrl.isEmpty() ){
                throw new Exception("로그모니터링의 설정파일은 filename, path, webhookUrl 항목이 필수입니다<br/>");
            }
            StringBuilderPlus sh = setFindershString(path, makePath, fileName, webhookUrl);
            String fullPath = makePath + fileName.split("\\.")[0].toString() + propAddFinderName;

            result += inputFIle(path, fullPath, sh.toString());

            result += excuteShell("chmod 722 " + fullPath);

            return result;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus setFindershString(String path, String makePath, String fileName, String webhookUrl) {
        StringBuilderPlus result = new StringBuilderPlus();
        String onlyFileName =fileName.split("\\.")[0].toString();
        String setupFileName = makePath + onlyFileName + propAddPropertiesName;


        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo \"$(date '+%Y-%m-%d %H:%M:%S') - Start " + onlyFileName + " Finder Shell !!\"");

        result.appendLine("sendUrl=\"" + sendSlackMessageUrl + "\"");
        result.appendLine("tail " + path + fileName + " -n0 -F | while read line;");
        result.appendLine("do");
        result.appendLine("     for i in ${!keywords[*]};");
        result.appendLine("     do");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - Keyword = ${keywords[i]} " + onlyFileName);
        result.appendLine("         if grep -q \"${keywords[i]}\" <<< \"$line\" ; then");
        result.appendLine("             echo $(date '+%Y-%m-%d %H:%M:%S') - \"Send Slack !!\" " + onlyFileName);
        result.appendLine("             param=\"{");
        result.appendLine("             \\\"text\\\":\\\"$(date '+%Y-%m-%d %H:%M:%S')  Fine By Keyword - [${keywords[i]}] => " + fileName + "\\\",");
        result.appendLine("             \\\"keyword\\\":\\\"${keywords[i]}\\\",");
        result.appendLine("             \\\"fileNmae\\\":\\\"" + onlyFileName + "\\\",");
        result.appendLine("             \\\"webhookUrl\\\":\\\"" + webhookUrl + "\\\",");
        result.appendLine("             \\\"system\\\":\\\"LogMornitoring\\\"");
        result.appendLine("             }\"");
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
            result += addHealthCheckShell(data);
        }
        return result;
    }

    public String addHealthCheckShell(JsonElement system) {
        String result = "";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get(propArrayKey).getAsJsonObject();
            String fileName = getJsonObjectString(jsonObject, propFilename);
            String time = getJsonObjectString(jsonObject, propTime);
            String makePath = getJsonObjectString(jsonObject, propMakePath);
            String processName = jsonObject.get(propProcessName).toString().replace("\"", "");
            String fullPath = makePath + fileName.split("\\.")[0].toString() + ".sh";

            if(fileName.isEmpty() || makePath.isEmpty() || processName.isEmpty() || time.isEmpty()){
                throw new Exception("Health Check 의 설정파일은 filename, makePath, time, processName 항목이 필수입니다 <br/>");
            }

            StringBuilderPlus sh = setLogmonitoringshString(makePath, makePath, fileName);

            //result += inputFIle(makePath, fullPath, sh.toString());

            // 권한변경, 크론탭 추가
            //result += excuteShell("chmod 722 " + fullPath) +
             //         addCrontab(makePath, fileName, time);
            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }


    public String initSetup() throws Exception {
        String result = "";

        try {
            JsonObject setupObject = convertStringToJsonObject(readFile(setupFullPath));
            JsonArray logMonitoring = setupObject.getAsJsonArray(propLogMornitoring);
            for (JsonElement data : logMonitoring) {
                JsonObject jsonObject = data.getAsJsonObject().get(propArrayKey).getAsJsonObject();
                String fileName = getJsonObjectString(jsonObject, propFilename);
                String path = getJsonObjectString(jsonObject, propMakePath);
                String time = getJsonObjectString(jsonObject, propTime);
                if(fileName == "" || path == "" || time == ""){
                    throw new Exception("로그모니터링의 설정파일은 filename, path, time 항목이 필수입니다 <br/>");
                }

                //기존 크론탭 제거
                result += deleteCrontab(path, fileName, time);

            }
        } catch (Exception e) {
            return e.toString();
        }

        return result;
    }

    public String addCrontab(String path, String fileName, String time) {
        String result = "";
        try {
            StringBuilderPlus sh = addCrontabshString(path, fileName, time);
            //크론탭 추가

            result += excuteShell(sh.toString());
            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public String deleteCrontab(String path, String fileName, String time) {
        String result = "";
        try {
            StringBuilderPlus sh = deleteCtontabshString(path, fileName, time);

            //Crontab 제거 Shell 실행
            result += excuteShell(sh.toString());

            //실행중이런 Ps Kill
            result += excuteShell("kill -9 $(ps aux | grep '" + fileName.split("\\.")[0].toString() + "' | awk '{print $2}')");

            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus deleteCtontabshString(String path, String fileName, String time) {
        String crontabName = path + fileName.split("\\.")[0].toString() + propLogStartupName;

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l | ");
        result.append("grep -v \"" + crontabName + "\" | ");
        result.append("crontab -");

        return result;
    }

    public StringBuilderPlus addCrontabshString(String path, String fileName, String time) {
        String crontabName = path + fileName.split("\\.")[0].toString() + propLogStartupName;

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l | ");
        result.append("(cat; echo \"*/" + time + " * * * * " + crontabName + " >> /logs/crontab.log 2>&1\") | ");
        result.append("crontab -");

        return result;
    }


    public String inputFIle(String path, String fullPath, String content) throws Exception {
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

    public String excuteShell(String sh) {
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
                sp.append(line);
            p.waitFor();
            p.destroy();

            result.append("ExcuteShell Success -  " + sh.toString());

        } catch (Exception e) {
            result.append("ExcuteShell Fail - ");
            if (osName.contains("win"))
                result.append("Not Support OS! (windows) =>  ");
            result.append(sh.toString());
        }
        result.append("<br/>");
        return result.toString();
    }

    public String readFile(String fullPath) throws IOException {
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

    public String getJsonObjectString(JsonObject jsonObject, String key){
        String result = "";
        if(jsonObject.get(key) != null) {
            result = jsonObject.get(key).toString().replace("\"", "");
        }else{
            switch (key){
                case "makePath":
                    if(jsonObject.get(propPath) != null) {
                        result = jsonObject.get(propPath).toString().replace("\"", "");
                    }else{
                        result = "/logs/sh";
                    }
            }
        }
        return result;
    }
}
