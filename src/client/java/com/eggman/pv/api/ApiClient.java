package com.eggman.pv.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(40);
    private static final int MAX_ATTEMPTS = 3;

    public static JsonObject get(String url) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("User-Agent", "Skyblock-PV")
                .build();

        IOException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + " : " + url);
                }

                return JsonParser.parseString(response.body()).getAsJsonObject();
            } catch (IOException e) {

                last = e;
                if (attempt < MAX_ATTEMPTS) {
                    Thread.sleep(1500L * attempt);
                }
            }
        }
        throw last != null ? last : new IOException("Request failed: " + url);
    }

}
