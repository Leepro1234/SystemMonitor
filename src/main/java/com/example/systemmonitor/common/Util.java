package com.example.systemmonitor.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

public class Util {
    private static String osName = System.getProperty("os.name").toLowerCase();

    public static String inputFIle(String path, String fullPath, String content) throws Exception {
        StringBuilderPlus result = new StringBuilderPlus();
        result.append(dateFormmat());
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

    public static String deleteFIle(String path, String fileName) throws Exception {
        StringBuilderPlus result = new StringBuilderPlus();
        result.append(dateFormmat());
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

    public static String excuteShell(String sh) {
        StringBuilderPlus result = new StringBuilderPlus();
        result.append(dateFormmat());

        StringBuilderPlus sp = new StringBuilderPlus();
        Process p;

        try {

            //??? ????????? ???????????? ???????????? ??????.
            String[] cmd = {"/bin/bash", "-c", sh};
            p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null)
                sp.appendLine(line);

            // shell ????????? ?????? ???????????????
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

    public static String readFile(String fullPath) throws IOException {
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

    public static String dateFormmat() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss | "));
    }

    public static String getJsonObjectString(JsonObject jsonObject, String key, String propPath){
        String result = "";
        if(jsonObject.get(key) != null) {
            result = jsonObject.get(key).toString().replace("\"", "");
        }else{
            switch (key){
                case "propMakePath":
                    if(jsonObject.get(propPath) != null) {
                        result = jsonObject.get(propPath).toString().replace("\"", "");
                        break;
                    }else{
                        result = "/usr/local/dy/sh";
                        break;
                    }
                case "propLimitTime":
                    result = "0";
                    break;
                default:
                    result="";
            }
        }
        return result;
    }

    public static String convertYamlToJson(String ymlFullPath) {
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

    public static JsonObject convertStringToJsonObject(String content) throws Exception {
        JsonObject jsonObject = null;
        try {
            jsonObject = new Gson().fromJson(content, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
