package com.example.systemmonitor.common;


import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;

import javax.swing.text.StyledEditorKit;
import java.io.*;
import java.util.LinkedHashMap;

public class Methods {
    public String convertYamlToJson() {
        String json="";
        //File file = new File("C:\\logs\\logmornitoring.yml");
        File file = new File("/logs/logmornitoring.yml");
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


    public String setsystemmonitoring() throws Exception{
        String result ="";
        try{
            //File file = new File("C:\\logs\\setup.txt");
            File file = new File("/logs/setup");
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(convertYamlToJson());
            fileOutputStream.close();
            bufferedWriter.close();
             JsonObject jsonObject = convertStringToJsonObject(convertYamlToJson());
             JsonArray logmonitoring = jsonObject.getAsJsonArray("logMonitoring");
            for (JsonElement data: logmonitoring) {
                //result += addLogmonitoringShell(data);
                result += addPropertiesShell(data) + "<br/>";
            }


             /*           getLogMonitoringSetup(logmonitoring);*/

            return result;

        } catch (Exception ex) {
            return "Error - " + ex.toString();
        }
    }

    public JsonObject convertStringToJsonObject(String content){
        JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);

        return jsonObject;
    }


    public String addLogmonitoringShell(JsonElement system) {
        StringBuilderPlus result = new StringBuilderPlus();
        String filename="";
        String path ="";
        String keywords ="";
        StringBuilderPlus sh = new StringBuilderPlus();
        JsonObject jsonObject = system.getAsJsonObject().get("system").getAsJsonObject();
        filename = jsonObject.get("filename").toString().replace("\"","");
        path = jsonObject.get("path").toString().replace("\"","");

        sh.appendLine("filename=\"" + filename + "\"");
        sh.appendLine("command=" + "`ps -ef | grep $filename`");
        sh.appendLine("echo $command");
        sh.appendLine("while read file; do");
        sh.appendLine("      if grep -q \"tail\" <<< \"$file\"; then");
        sh.appendLine("         echo \"Not Close Process\"");
        sh.appendLine("      elif grep -q \"grep\" <<< \"$file\"; then");
        sh.appendLine("         echo \"Continue...\"");
        sh.appendLine("      elif [\"\" == \"$file\"]; then");
        sh.appendLine("         echo \"Excuting...\"");
        sh.appendLine("         ./test.sh");
        sh.appendLine("         break");
        sh.appendLine("      else");
        sh.appendLine("         echo \"$file\"");
        sh.appendLine("         ./test.sh");
        sh.appendLine("      fi");
        sh.appendLine("done < <(echo \"$command\")");
        try{
            //File file = new File("/logs/" + filename.split(".")[0].toString() + ".sh");
            File file = new File("/logs/" + filename.split("\\.")[0].toString() + ".sh");
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(sh.toString());
            bufferedWriter.flush();
            fileOutputStream.close();
            bufferedWriter.close();
            Process p;
            try {
                //이 변수에 명령어를 넣어주면 된다.
                String[] cmd = {"/bin/bash", "-c", "chmod 722 "+ path + filename.split("\\.")[0].toString() + ".sh"};
                p = Runtime.getRuntime().exec(cmd);
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null)
                    result.append(line);
                p.waitFor();
                p.destroy();

            } catch (Exception e) {
                result.append(e.toString());
            }

            return result.toString();

        } catch (Exception ex) {
            return ex.toString();
        }
    }
    public String addPropertiesShell(JsonElement system) {
        StringBuilderPlus sh = new StringBuilderPlus();
        JsonObject jsonObject = system.getAsJsonObject().get("system").getAsJsonObject();
        String fileName = jsonObject.get("filename").toString().replace("\"","");
        JsonArray keywords = jsonObject.getAsJsonArray("keywords");

        String gbn = "";
        sh.append("keywords=(");
        for(JsonElement keyword : keywords){
            sh.append(gbn + keyword.toString());
            gbn = " ";
        }
        sh.append(")");


        try{
            fileName= "/logs/" + fileName.split("\\.")[0].toString() + "_setup.sh";
            if(!inputFIle(fileName, sh.toString())) {
                return "sh File Input Faild! ";
            }

            if(!changeMod(fileName, "722")){
                return "Chmod Fail! ";
            }

            return "addPropertiesShell Success! ";

        } catch (Exception ex) {
            return ex.toString();
        }
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
