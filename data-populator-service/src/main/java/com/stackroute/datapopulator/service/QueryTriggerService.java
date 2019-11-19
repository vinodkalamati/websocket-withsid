package com.stackroute.datapopulator.service;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.parser.ParseException;


@Service
@PropertySource("classpath:application.properties")
public class QueryTriggerService {
    @Value("${url}")
    private String url;
    public void queryServiceTrigger(String query,String domain,String sessionId) throws IOException {
       String url = "http://34.93.245.170:8087/api/v1/query";
       JSONObject object1 = new JSONObject();
       object1.put("domain", domain);
       object1.put("searchTerm", query);
       object1.put("sessionId",sessionId);
       Gson gson = new Gson();
       CloseableHttpClient client = HttpClientBuilder.create().build();
       HttpPost post = new HttpPost(url);
       StringEntity entity = new StringEntity(gson.toJson(object1));
       post.setEntity(entity);
       post.setHeader("Content-type","application/json");
       org.apache.http.HttpResponse response = client.execute(post);
    }
}
