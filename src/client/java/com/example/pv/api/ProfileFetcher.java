package com.example.pv.api;

import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

public class ProfileFetcher {

    public static CompletableFuture<JsonObject> fetchAsync(String username, String requester) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return BackendApi.getProfile(username, requester);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
