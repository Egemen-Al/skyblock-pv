package com.example.pv.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RepoManager {

	private static final String ZIP_URL =
		"https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/archive/refs/heads/master.zip";

	private static Path itemsDir;
	private static Path constantsDir;
	private static Path doneMarker;
	private static volatile boolean ready = false;
	private static volatile boolean downloading = false;
	private static final Map<String, JsonObject> CACHE = new HashMap<>();

	public static void init() {
		if (ready || downloading) return;
		Path base = FabricLoader.getInstance().getGameDir().resolve("skyblockpv/repo");
		itemsDir = base.resolve("items");
		constantsDir = base.resolve("constants");
		doneMarker = base.resolve(".complete_v2");

		if (Files.exists(doneMarker)) {
			ready = true;
			return;
		}
		download();
	}

	public static boolean isReady() { return ready; }
	public static boolean isDownloading() { return downloading; }

	private static void download() {
		downloading = true;
		Thread t = new Thread(() -> {
			try {
				Files.createDirectories(itemsDir);
				Files.createDirectories(constantsDir);
				HttpClient c = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.NORMAL).build();
				HttpRequest req = HttpRequest.newBuilder(URI.create(ZIP_URL))
					.header("User-Agent", "skyblock-pv").build();
				HttpResponse<InputStream> resp = c.send(req, HttpResponse.BodyHandlers.ofInputStream());
				try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(resp.body()))) {
					ZipEntry e;
					while ((e = zis.getNextEntry()) != null) {
						String n = e.getName();
						if (e.isDirectory() || !n.endsWith(".json")) continue;
						int ii = n.indexOf("/items/");
						int ci = n.indexOf("/constants/");
						Path out = null;
						if (ii >= 0) out = itemsDir.resolve(n.substring(ii + 7));
						else if (ci >= 0) out = constantsDir.resolve(n.substring(ci + 11));
						if (out == null) continue;
						if (out.getParent() != null) Files.createDirectories(out.getParent());
						Files.write(out, zis.readAllBytes());
					}
				}
				Files.write(doneMarker, new byte[]{1});
				ready = true;
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				downloading = false;
			}
		}, "skyblock-pv-repo-dl");
		t.setDaemon(true);
		t.start();
	}

	public static JsonObject getItem(String internalName) {
		if (!ready) return null;
		if (CACHE.containsKey(internalName)) return CACHE.get(internalName);
		try {
			Path p = itemsDir.resolve(internalName + ".json");
			if (!Files.exists(p)) { CACHE.put(internalName, null); return null; }
			JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
			CACHE.put(internalName, o);
			return o;
		} catch (Exception e) {
			CACHE.put(internalName, null);
			return null;
		}
	}

	public static JsonObject getConstant(String name) {
		if (!ready) return null;
		String key = "const:" + name;
		if (CACHE.containsKey(key)) return CACHE.get(key);
		try {
			Path p = constantsDir.resolve(name + ".json");
			if (!Files.exists(p)) { CACHE.put(key, null); return null; }
			JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
			CACHE.put(key, o);
			return o;
		} catch (Exception e) {
			CACHE.put(key, null);
			return null;
		}
	}
}
