package com.bwxor.piejfxsdk.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ModelInfoService {
    private final Map<String, Long> modelContextLengths = new ConcurrentHashMap<>();

    public void load() {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/models"))
                    .GET()
                    .build();
            var response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            JSONArray data = new JSONObject(response.body()).getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject model = data.getJSONObject(i);
                long ctx = model.optLong("context_length", 0);
                if (ctx > 0) {
                    modelContextLengths.put(model.getString("id"), ctx);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getContextLength(String modelId) {
        return modelContextLengths.getOrDefault(modelId, 200_000L);
    }
}