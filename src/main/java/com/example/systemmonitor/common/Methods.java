package com.example.systemmonitor.common;


import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.security.Permission;
import java.util.LinkedHashMap;

public class Methods {
    private String readFileFullPath ="/logs/setting/setup";
    public String convertYamlToJson() {
        String json="";
        File file = new File("/logs/setting/logmornitoring.yml");
        Yaml yaml = new Yaml();

        try {
            final Object loadedYaml = yaml.load(new FileReader(file));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            json = gson.toJson(loadedYaml, LinkedHashMap.class);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json;
    }

    public JsonObject convertStringToJsonObject(String content) throws  Exception{
        JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);

        return jsonObject;
    }

    public String setSystemMonitoring() throws Exception{

        String result ="";


        /*
        * 파일 유무 체크
         */
        File setup = new File(readFileFullPath);
        if(setup.isFile()){
            //설정된 크론탭 제거
            result += initSetup();

            //크론탭 추가

        }

        String fileName = "/logs/setting/setup";
        try{
            if(!inputFIle(fileName, convertYamlToJson())) {
                return "setup File Input Faild! ";
            }

            if(!excuteShell("chmod 722 " + fileName)){
                return "setup File Chmod Fail! ";
            }
        } catch (Exception ex) {
            return ex.toString();
        }

        try{
            /*
            * Shell 파일 생성
            * proerties.sh
            * 실행 sh
             * 로직 sh
             * */
            JsonObject jsonObject = convertStringToJsonObject(convertYamlToJson());
            JsonArray logmonitoring = jsonObject.getAsJsonArray("logMonitoring");
            for (JsonElement data : logmonitoring) {
                result += addFinderShell(data) + "<br/>";
                result += addLogmonitoringShell(data) + "<br/>";
                result += addPropertiesShell(data) + "<br/>";
            }

            return result;

        } catch (Exception ex) {
            return "Error - " + ex.toString();
        }
    }


    public String addLogmonitoringShell(JsonElement system) {
        String filename="";
        String path ="";
        String keywords ="";
        JsonObject jsonObject = system.getAsJsonObject().get("system").getAsJsonObject();
        filename = jsonObject.get("filename").toString().replace("\"","");
        path = jsonObject.get("path").toString().replace("\"","");

        StringBuilderPlus sh = setLogmonitoringshString(path, filename);

        try{
            String fullPath = path + filename.split("\\.")[0].toString() + ".sh";
            if(!inputFIle(fullPath, sh.toString())) {
                return "LogmonitoringShell File Input Faild! ";
            }

            if(!excuteShell("chmod 722 " + fullPath)){
                return "LogmonitoringShell File Chmod Fail! ";
            }


            //크론탭 추가
            return "addLogmonitoringShell Success! " + addCrontab(path, filename);

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public StringBuilderPlus setLogmonitoringshString(String path, String fileName){
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
        JsonObject jsonObject = system.getAsJsonObject().get("system").getAsJsonObject();
        String fileName = jsonObject.get("filename").toString().replace("\"","");
        String path = jsonObject.get("path").toString().replace("\"","");
        JsonArray keywords = jsonObject.getAsJsonArray("keywords");

        StringBuilderPlus sh = setLogmonitoringshString(keywords);

        try{
            fileName= path + fileName.split("\\.")[0].toString() + "_setup.sh";
            if(!inputFIle(fileName, sh.toString())) {
                return "Properties File Input Faild! ";
            }

            if(!excuteShell("chmod 722 " + fileName)){
                return "Properties File Chmod Fail! ";
            }

            return "addPropertiesShell Success! ";

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public StringBuilderPlus setLogmonitoringshString(JsonArray keywords){
        StringBuilderPlus result = new StringBuilderPlus();
        String gbn = "";
        result.append("keywords=(");
        for(JsonElement keyword : keywords){
            result.append(gbn + keyword.toString());
            gbn = " ";
        }
        result.append(")");
        return result;
    }

    public String addFinderShell(JsonElement system) {
        JsonObject jsonObject = system.getAsJsonObject().get("system").getAsJsonObject();
        String filename = jsonObject.get("filename").toString().replace("\"","");
        String path = jsonObject.get("path").toString().replace("\"","");

        StringBuilderPlus sh = setFindershString(path, filename);

        try{
            String fullPath = path + filename.split("\\.")[0].toString() + "_finder.sh";
            if(!inputFIle(fullPath, sh.toString())) {
                return "FinderShell File Input Faild! ";
            }

            if(!excuteShell("chmod 722 " + fullPath)){
                return "Finder File Chmod Fail! ";
            }

            return "addFinderShell Success! ";

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public StringBuilderPlus setFindershString(String path, String fileName){
        StringBuilderPlus result = new StringBuilderPlus();
        String setupFileName = path + fileName.split("\\.")[0].toString() + "_setup.sh";


        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo \"$(date '+%Y-%m-%d %H:%M:%S') - Start "+fileName.split("\\.")[0].toString()+" Finder Shell !!\""+ fileName.split("\\.")[0].toString());
        result.appendLine("tail " + path + fileName + " -n0 -F | while read line;");
        result.appendLine("do");
        result.appendLine("     for i in ${!keywords[*]};");
        result.appendLine("     do");
        result.appendLine("         echo $(date '+%Y-%m-%d %H:%M:%S') - Keyword = ${keywords[i]} "+ fileName.split("\\.")[0].toString());
        result.appendLine("         if grep -q \"${keywords[i]}\" <<< \"$line\" ; then");
        result.appendLine("             echo $(date '+%Y-%m-%d %H:%M:%S') - \"Send Slack !!\" "+ fileName.split("\\.")[0].toString());
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
            JsonArray logmonitoring = setupObject.getAsJsonArray("logMonitoring");
            for (JsonElement data : logmonitoring) {
                JsonObject jsonObject = data.getAsJsonObject().get("system").getAsJsonObject();
                String fileName = jsonObject.get("filename").toString().replace("\"", "");
                String path = jsonObject.get("path").toString().replace("\"", "");

                //기존 크론탭 제거
                result += "<br/> " + deleteCrontab(path, fileName);

            }
        } catch (Exception e) {
            return e.toString();
        }

        return result;
    }

    public String addCrontab(String path, String fileName) {
        StringBuilderPlus sh = addCrontabshString(path, fileName, "1");

        try{

            if(!excuteShell(sh.toString())){
                return "ExcuteShell Fail! ";
            }
            String crontabName = path + fileName.split("\\.")[0].toString() + ".sh";
            StringBuilderPlus result = new StringBuilderPlus();
            result.append("crontab -l | ");
            result.append("(cat; echo \"" + "*/1" + " * * * * " + crontabName + "\") | ");
            result.append("crontab -");
            result.append(" Success!!");
            return result.toString();

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public String deleteCrontab(String path, String fileName) {
        StringBuilderPlus sh = deleteCtontabshString(path, fileName, "1");

        try{

            if(!excuteShell(sh.toString())){
                return "ExcuteShell Fail! ";
            }
            if(!excuteShell("kill $(ps aux | grep '"+fileName.split("\\.")[0].toString()+"' | awk '{print $2}')")){
                return "ps Init Fail! " + "kill $(ps aux | grep '"+fileName.split("\\.")[0].toString()+"' | awk '{print $2}')";
            }

            StringBuilderPlus result = new StringBuilderPlus();
            String crontabName = path + fileName.split("\\.")[0].toString() + ".sh";

            result.append("crontab -l | ");
            result.append("grep -v \"" + "1" + " \\* \\* \\* \\* " + crontabName + "\" | ");
            result.append("crontab -");
            result.append(" Success!!");

            return result.toString();// "deleteCtontab Success! ";

        } catch (Exception ex) {
            return ex.toString();
        }
    }


    public StringBuilderPlus deleteCtontabshString(String path, String fileName, String time){
        String crontabName = path + fileName.split("\\.")[0].toString() + ".sh";

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l | ");
        result.append("grep -v \"" + time + " \\* \\* \\* \\* " + crontabName + "\" | ");
        result.append("crontab -");

        return result;
    }

    public StringBuilderPlus addCrontabshString(String path, String fileName, String time){
        String crontabName = path + fileName.split("\\.")[0].toString() + ".sh";

        StringBuilderPlus result = new StringBuilderPlus();
        result.append("crontab -l | ");
        result.append("(cat; echo \"*/" +time + " * * * * " + crontabName + " >> /logs/crontab.log 2>&1\") | " );
        result.append("crontab -");

        return result;
    }

    public Boolean inputFIle(String filename, String content) throws Exception {
        try {
            File file = new File(filename);
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            fileOutputStream.close();
            bufferedWriter.close();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean excuteShell(String sh){
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

            return true;

        } catch (Exception e) {
            return false;
        }
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
}
