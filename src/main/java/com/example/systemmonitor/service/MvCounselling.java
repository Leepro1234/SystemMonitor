package com.example.systemmonitor.service;

import com.example.systemmonitor.vo.ResponseVO;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MvCounselling {
    public boolean isAlive(){


        ResponseVO createRommRes = isCreateRoom();
        if(createRommRes.getStatusCode() != 200){
            return false;
        }
        if(!isInRoom(createRommRes)){
            return false;
        }
        return false;
    }

    public ResponseVO isCreateRoom(){
        ResponseVO result = new ResponseVO();

        try{

            URI uri = new URIBuilder("")
                    .build();
            HttpPost httpPost = new HttpPost();
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(10 * 1000)
                    .setConnectTimeout(10 * 1000)
                    .setSocketTimeout(10 * 1000)
                    .build();
            httpPost.setConfig(requestConfig);
            httpPost.setURI(uri);

            CloseableHttpClient httpClient;
            //httpClient = CallByLocal();
            httpClient = CallByService();
            if(httpClient == null){
                result.setStatusCode(500);
            }

            HttpResponse httpResponse = httpClient.execute(httpPost);
            String html = EntityUtils.toString(httpResponse.getEntity());
            int StatusCode = httpResponse.getStatusLine().getStatusCode();
            if(StatusCode == 200){
                result.setStatusCode(200);
                result.setBody(html);
            }
        }catch (Exception ex){
            result.setStatusCode(500);
        }

        return result;
    }

    public boolean isInRoom(ResponseVO responseVO){
        boolean result = false;

        Pattern pattern = Pattern.compile(".*\\\"roomId\\\":\\\"(?<roomid>.*?)\\\".*");
        Matcher m = pattern.matcher(responseVO.getBody());
        System.out.println(m.matches());
        String roomId = m.group("roomid");

        try {

            URI uri = new URIBuilder("")
                    .build();
            HttpGet httpGet = new HttpGet();
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(10 * 1000)
                    .setConnectTimeout(10 * 1000)
                    .setSocketTimeout(10 * 1000)
                    .build();
            httpGet.setConfig(requestConfig);
            httpGet.setURI(uri);



            CloseableHttpClient httpClient;
            //httpClient = CallByLocal();
            httpClient = CallByService();
            if(httpClient == null){
                return false;
            }

            HttpResponse httpResponse = httpClient.execute(httpGet);
            String html = EntityUtils.toString(httpResponse.getEntity());
            int StatusCode = httpResponse.getStatusLine().getStatusCode();
            if(StatusCode == 200){
                return true;
            }
        }catch(Exception ex){
            return false;
        }

        return result;
    }

    public CloseableHttpClient CallByLocal(){
        CloseableHttpClient httpClient;
        try{
            HttpHost proxy = new HttpHost("localhost", 8888, "http");
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);


            TrustManager[] trustAllCerts = new TrustManager[] {
                    (TrustManager) new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sc);
            httpClient = HttpClients.custom().setSSLSocketFactory(csf).setRoutePlanner(routePlanner).build();
        }catch (Exception ex){
            return null;
        }

        return httpClient;
    }
    public CloseableHttpClient CallByService(){
        CloseableHttpClient httpClient;
        try{
            httpClient = HttpClients.custom().build();
        }catch (Exception ex){
            return null;
        }
        return httpClient;
    }
}
