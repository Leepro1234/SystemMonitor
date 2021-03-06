package com.example.systemmonitor.service;

import com.example.systemmonitor.common.Methods;
import com.example.systemmonitor.vo.SlackVO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;

@Service
public  class SlackService<T> {

    private  HttpPost httpPost = new HttpPost();
    private HttpGet httpGet = new HttpGet();
    public String method = "";
    public JsonObject jsonBody = new JsonObject();
    public String resposeBody;
    public String contentType = "";

    public SlackService() throws Exception {
        this.contentType="application/json; charset=UTF-8";
    }


    public void SetHeader(String name, String value){
        if(this.method == Method.POST.name()) {
            this.httpPost.addHeader(name, value);
        }else if(this.method == Method.GET.name()){
            this.httpGet.addHeader(name, value);
        }
    }

    public void setMethod(Method method){
        this.method = method.toString();
    }

    public void setJsonBody(String key, String value){
        jsonBody.addProperty(key, value);
    }
    public void setJsonBody(T value){
        Gson gson = new Gson();

        String jsonString = gson.toJson(value);
        Type type = new TypeToken<T>(){}.getType();
        jsonBody =  gson.fromJson(jsonString, JsonObject.class);
    }

    public void setContentType(String value){
        this.contentType=value;
        if(this.method == Method.POST.name()) {
            this.httpPost.addHeader("Content-Type", value);
        }else if(this.method == Method.GET.name()){
            this.httpGet.addHeader("Content-Type", value);
        }
    }

    public void sendSlackMessage(String url, SlackVO slackVO) throws Exception{
        go(url);
        initRetryLimit(slackVO);
    }

    public void test(String url) throws Exception{
        go(url);
    }

    public void go(String url) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(10 * 1000)
                .setConnectTimeout(10 * 1000)
                .setSocketTimeout(10 * 1000)
                .build();

        URI uri = new URIBuilder(url)
                .build();


        if(this.method == Method.POST.name()) {
            httpPost.setConfig(requestConfig);
            httpPost.setURI(uri);
        }else if(this.method == Method.GET.name()){
            httpGet.setConfig(requestConfig);
            httpGet.setURI(uri);
        }

        HttpClient httpClient = HttpClientBuilder.create().build();

        if(this.method == Method.POST.name()){
            String reqeustBody = "";
            if(this.contentType == "application/json; charset=UTF-8"){
                Gson gson = new Gson();
                reqeustBody = gson.toJson(jsonBody);
            }
            httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
            httpPost.setEntity(new StringEntity(reqeustBody, "UTF-8"));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            String html = EntityUtils.toString(httpResponse.getEntity());
            System.out.println(html);
            this.resposeBody = html;
        }else{
            HttpResponse httpResponse = httpClient.execute(httpGet);
            String html = EntityUtils.toString(httpResponse.getEntity());
            System.out.println(html);
            this.resposeBody = html;
        }
    }


    public void initRetryLimit(SlackVO slackVO) throws Exception {
        Methods methods = new Methods();
        ArrayList<String> limitList = methods.readAndInitLimitListReturnArrayList(slackVO);
        methods.writeLimitList(slackVO.getLogFileName(), slackVO.getKeyword(), slackVO.getLimitTime(), limitList);

    }
    public enum Method {
        GET, POST, DELETE, PUT, PATCH
    }

}
