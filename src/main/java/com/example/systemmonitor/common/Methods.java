package com.example.systemmonitor.common;


import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;

public class Methods {
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

    public JsonObject convertStringToJsonObject(String content){
        JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);

        return jsonObject;
    }

    public String setSystemMonitoring() throws Exception{
        String result ="";
        String fileName = "/logs/setting/setup";
        try{
            if(!inputFIle(fileName, convertYamlToJson())) {
                return "setup File Input Faild! ";
            }

            if(!changeMod(fileName, "722")){
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

            if(!changeMod(fullPath, "722")){
                return "LogmonitoringShell File Chmod Fail! ";
            }

            return "addLogmonitoringShell Success! ";

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public StringBuilderPlus setLogmonitoringshString(String path, String fileName){
        String setupFileName = path + fileName.split("\\.")[0].toString() + "_setup.sh";
        String finderFileName = fileName.split("\\.")[0].toString() + "_finder.sh";

        StringBuilderPlus result = new StringBuilderPlus();
        result.appendLine("source " + setupFileName + "");
        result.appendLine("echo $keywords");

        result.appendLine("filename=\"" + fileName + "\"");
        result.appendLine("command=" + "`ps -ef | grep $filename | grep -v \"grep\"`");
        result.appendLine("echo $command");
        result.appendLine("while read file; do");
        result.appendLine("      if grep -q \"tail\" <<< \"$file\"; then");
        result.appendLine("         echo \"Not Close Process\"");
        result.appendLine("      elif [\"\" == \"$file\"]; then");
        result.appendLine("         echo \"Excuting...\"");
        result.appendLine("         ./" + finderFileName);
        result.appendLine("         break");
        result.appendLine("      else");
        result.appendLine("         echo \"Error\"");
        result.appendLine("      fi");
        result.appendLine("done < <(echo \"$command\")");

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

            if(!changeMod(fileName, "722")){
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

            if(!changeMod(fullPath, "722")){
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
        result.appendLine("echo \"Start "+fileName.split("\\.")[0].toString()+" Finder Shell !!\"");
        result.appendLine("tail " + path + fileName + " -n0 -F | while read line;");
        result.appendLine("do");
        result.appendLine("     for i in ${!keywords[*]};");
        result.appendLine("     do");
        result.appendLine("         echo ${keywords[i]}");
        result.appendLine("         if grep -q \"${keywords[i]}\" <<< \"$line\" ; then");
        result.appendLine("             echo \"Send Slack !!\"");
        result.appendLine("             #pkill -9 -P $$ tail");
        result.appendLine("             break");
        result.appendLine("         fi");
        result.appendLine("     done");
        result.appendLine("done;");

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

    public Boolean changeMod(String filename, String Permission){
        StringBuilderPlus sp = new StringBuilderPlus();
        Process p;

        try {
            //이 변수에 명령어를 넣어주면 된다.
            String[] cmd = {"/bin/bash", "-c", "chmod " + Permission + " " + filename};
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
}
