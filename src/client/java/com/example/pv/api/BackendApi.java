package com.example.pv.api;

import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BackendApi {

    private static final String BASE_URL =
            "https://YOUR-API-GATEWAY-URL/skyblock-pv-profile";

    public static JsonObject getProfile(String username, String requester) throws Exception {
        String url = BASE_URL
                + "?username="
                + URLEncoder.encode(username, StandardCharsets.UTF_8);
        if (requester != null && !requester.isBlank()) {
            url += "&requester=" + URLEncoder.encode(requester, StandardCharsets.UTF_8);
        }
        return ApiClient.get(url);
    }

    public static JsonObject getUsers() throws Exception {
        return ApiClient.get(BASE_URL + "?command=users");
    }
}
