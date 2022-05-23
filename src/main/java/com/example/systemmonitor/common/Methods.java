package com.example.systemmonitor.common;


import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

public class Methods {
    private String readFileFullPath = "/logs/setting/setup";
    private String osName = System.getProperty("os.name").toLowerCase();
    private String sendslackUrl;

    public void SetSendslackUrl(String sendslackUrl){
        this.sendslackUrl = sendslackUrl;
    }

    public String convertYamlToJson() {
        String json = "";
        File file = new File("/logs/setting/logmornitoring.yml");
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
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String setSystemMonitoring() throws Exception {

        String result = "";


        //기존 셋팅 유무 체크
        File setup = new File(readFileFullPath);
        if (setup.isFile()) {
            //기존 셋팅 초기화
            result += initSetup();
        }

        String fullPath = "/logs/setting/setup";
        try {
            result += inputFIle(fullPath, convertYamlToJson());

            result += excuteShell("chmod 722 " + fullPath);

        } catch (Exception ex) {
            return ex.toString();
        }

        try {
             /*
             * Shell 파일 생성
             * 로직 sh
             * 로직실행 sh
             * properties sh
             */
            JsonObject jsonObject = convertStringToJsonObject(convertYamlToJson());
            JsonArray logmonitoring = jsonObject.getAsJsonArray("logMonitoring");
            for (JsonElement data : logmonitoring) {
                result += addFinderShell(data);
                result += addLogmonitoringShell(data);
                result += addPropertiesShell(data);
            }

            return result;

        } catch (Exception ex) {
            return "Error - " + ex.toString();
        }
    }


    public String addLogmonitoringShell(JsonElement system) {

        String result ="";
        try {
            JsonObject jsonObject = system.getAsJsonObject().get("system").getAsJsonObject();
            String filename = jsonObject.get("filename").toString().replace("\"", "");
            String path = jsonObject.get("path").toString().replace("\"", "");
            String time = jsonObject.get("time").toString().replace("\"", "");

            String fullPath = path + filename.split("\\.")[0].toString() + ".sh";

            StringBuilderPlus sh = setLogmonitoringshString(path, filename);

            result += inputFIle(fullPath, sh.toString());

            // 권한변경, 크론탭 추가
            result += excuteShell("chmod 722 " + fullPath) + addCrontab(path, filename, time);
            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus setLogmonitoringshString(String path, String fileName) {
        String setupFileName = path + fileName.split("\\.")[0].toString() + "_setup.sh";
        String finderFileName = fileName.split("\\.")[0].toString() + "_finder.sh";

        StringBuilderPlus result = new StringBuilderPlus();
        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo $(date '+%Y-%m-%d %H:%M:%S') - " + fileName.split("\\.")[0].toString());
        //result.appendLine("echo $keywords");

        result.appendLine("filename=\"" + fileName + "\"");
        result.appendLine("command=" + "`ps -ef | grep $filename | grep -v \"grep\"`");
        //result.appendLine("echo $command");
        //result.appendLine("while read file; do");
        result.appendLine("echo $command | while read file; do");
        result.appendLine("      if grep -q \"tail\" <<< \"$file\"; then");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Not Close Process\"" + fileName.split("\\.")[0].toString());
        result.appendLine("      elif [\"\" == \"$file\"]; then");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Excuting...\"" + fileName.split("\\.")[0].toString());
        result.appendLine("         " + path + finderFileName);
        result.appendLine("         break");
        result.appendLine("      else");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - \"Error\"" + fileName.split("\\.")[0].toString());
        result.appendLine("      fi");
        result.appendLine("done");
        //result.appendLine("done < <(echo \"$command\")");

        return result;
    }

    public String addPropertiesShell(JsonElement system) {
        String result = "";

        try {
            JsonObject jsonObject = system.getAsJsonObject().get("system").getAsJsonObject();
            String fileName = jsonObject.get("filename").toString().replace("\"", "");
            String path = jsonObject.get("path").toString().replace("\"", "");
            JsonArray keywords = jsonObject.getAsJsonArray("keywords");
            fileName = path + fileName.split("\\.")[0].toString() + "_setup.sh";

            StringBuilderPlus sh = setLogmonitoringshString(keywords);

            result += inputFIle(fileName, sh.toString());

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
            JsonObject jsonObject = system.getAsJsonObject().get("system").getAsJsonObject();
            String filename = jsonObject.get("filename").toString().replace("\"", "");
            String path = jsonObject.get("path").toString().replace("\"", "");
            StringBuilderPlus sh = setFindershString(path, filename);
            String fullPath = path + filename.split("\\.")[0].toString() + "_finder.sh";

            result += inputFIle(fullPath, sh.toString());

            result += excuteShell("chmod 722 " + fullPath);

            return result;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public StringBuilderPlus setFindershString(String path, String fileName) {
        StringBuilderPlus result = new StringBuilderPlus();
        String setupFileName = path + fileName.split("\\.")[0].toString() + "_setup.sh";


        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo \"$(date '+%Y-%m-%d %H:%M:%S') - Start " + fileName.split("\\.")[0].toString() + " Finder Shell !!\"" + fileName.split("\\.")[0].toString());

        result.appendLine("sendUrl=\"" + sendslackUrl  + "\"");
        result.appendLine("tail " + path + fileName + " -n0 -F | while read line;");
        result.appendLine("do");
        result.appendLine("     for i in ${!keywords[*]};");
        result.appendLine("     do");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - Keyword = ${keywords[i]} " + fileName.split("\\.")[0].toString());
        result.appendLine("         if grep -q \"${keywords[i]}\" <<< \"$line\" ; then");
        result.appendLine("             echo $(date '+%Y-%m-%d %H:%M:%S') - \"Send Slack !!\" " + fileName.split("\\.")[0].toString());
        result.appendLine("             param=\"{");
        result.appendLine("             \\\"message\\\":\\\"Fine By Keyword - [${keywords[i]}]\\\",");
        result.appendLine("             \\\"keyword\\\":\\\"${keywords[i]}\\\",");
        result.appendLine("             \\\"fileNmae\\\":\\\"" + fileName.split("\\.")[0].toString() + "\\\",");
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


    public String initSetup() throws Exception {
        String result = "";

        try {
            JsonObject setupObject = convertStringToJsonObject(readFile(readFileFullPath));
            JsonArray logMonitoring = setupObject.getAsJsonArray("logMonitoring");
            for (JsonElement data : logMonitoring) {
                JsonObject jsonObject = data.getAsJsonObject().get("system").getAsJsonObject();
                String fileName = jsonObject.get("filename").toString().replace("\"", "");
                String path = jsonObject.get("path").toString().replace("\"", "");
                String time = jsonObject.get("time").toString().replace("\"", "");

                //기존 크론탭 제거
                result += deleteCrontab(path, fileName, time);

            }
        } catch (Exception e) {
            return e.toString();
        }

        return result;
    }

    public String addCrontab(String path, String fileName, String time) {
        String result ="";
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
        String result ="";
        try {
            StringBuilderPlus sh = deleteCtontabshString(path, fileName, time);

            //Crontab 제거 Shell 실행
            result += excuteShell(sh.toString());

            //실행중이런 Ps Kill
            result += excuteShell("kill $(ps aux | grep '" + fileName.split("\\.")[0].toString() + "' | awk '{print $2}')");

            return result;

        } catch (Exception ex) {
            return ex.toString();
        }
    }


    public StringBuilderPlus deleteCtontabshString(String path, String fileName, String time) {
        String crontabName = path + fileName.split("\\.")[0].toString() + ".sh";

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l | ");
        result.append("grep -v \"" + crontabName + "\" | ");
        result.append("crontab -");

        return result;
    }

    public StringBuilderPlus addCrontabshString(String path, String fileName, String time) {
        String crontabName = path + fileName.split("\\.")[0].toString() + ".sh";

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l | ");
        result.append("(cat; echo \"*/" + time + " * * * * " + crontabName + " >> /logs/crontab.log 2>&1\") | ");
        result.append("crontab -");

        return result;
    }

    public String inputFIle(String filename, String content) throws Exception {
        StringBuilderPlus result = new StringBuilderPlus();
        result.append(DateFormmat());
        try {
            File file = new File(filename);
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            fileOutputStream.close();
            bufferedWriter.close();

            result.append("InputFIle Success - " + filename);
        } catch (Exception e) {
            result.append(filename + "InputFIle Fail  - " + e.toString());
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

    public String DateFormmat(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss | "));
    }
}
