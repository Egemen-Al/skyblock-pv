package com.eggman.pv.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BestiaryData {

	public static class Mob {
		public String name, texture, item;
		public int cap, bracket;
		public List<String> killKeys = new ArrayList<>();
	}
	public static class Island {
		public String key, name, iconTexture;
		public List<Mob> mobs = new ArrayList<>();
	}

	private static List<Island> ISLANDS;
	private static Map<Integer, int[]> BRACKETS;

	public static List<Island> islands() { load(); return ISLANDS; }

	private static void load() {
		if (ISLANDS != null) return;
		JsonObject root = RepoManager.getConstant("bestiary");
		if (root == null) return;
		List<Island> islandList = new ArrayList<>();
		Map<Integer, int[]> brMap = new HashMap<>();
		try {
			JsonObject br = root.getAsJsonObject("brackets");
			for (Map.Entry<String, JsonElement> e : br.entrySet()) {
				JsonArray a = e.getValue().getAsJsonArray();
				int[] arr = new int[a.size()];
				for (int i = 0; i < a.size(); i++) arr[i] = a.get(i).getAsInt();
				brMap.put(Integer.parseInt(e.getKey()), arr);
			}
			for (Map.Entry<String, JsonElement> e : root.entrySet()) {
				if (e.getKey().equals("brackets")) continue;
				if (!e.getValue().isJsonObject()) continue;
				JsonObject io = e.getValue().getAsJsonObject();
				if (!io.has("mobs")) continue;
				Island is = new Island();
				is.key = e.getKey();
				is.name = io.has("name") ? io.get("name").getAsString() : e.getKey();
				if (io.has("icon") && io.getAsJsonObject("icon").has("texture"))
					is.iconTexture = io.getAsJsonObject("icon").get("texture").getAsString();
				for (JsonElement me : io.getAsJsonArray("mobs")) {
					JsonObject mo = me.getAsJsonObject();
					Mob m = new Mob();
					m.name = mo.has("name") ? mo.get("name").getAsString() : "?";
					m.texture = mo.has("texture") ? mo.get("texture").getAsString() : null;
					m.item = mo.has("item") ? mo.get("item").getAsString() : null;
					m.cap = mo.has("cap") ? mo.get("cap").getAsInt() : 0;
					m.bracket = mo.has("bracket") ? mo.get("bracket").getAsInt() : 1;
					if (mo.has("mobs")) for (JsonElement k : mo.getAsJsonArray("mobs")) m.killKeys.add(k.getAsString());
					is.mobs.add(m);
				}
				islandList.add(is);
			}
			BRACKETS = brMap;
			ISLANDS = islandList;
		} catch (Exception ignored) {}
	}

	public static int tier(int kills, int bracket) {
		int[] th = BRACKETS.get(bracket);
		if (th == null) return 0;
		int t = 0;
		for (int x : th) if (kills >= x) t++;
		return t;
	}

	public static int maxTier(int cap, int bracket) {
		int[] th = BRACKETS.get(bracket);
		if (th == null) return 0;
		int t = 0;
		for (int x : th) if (x <= cap) t++;
		return t;
	}

	public static int nextThreshold(int kills, int bracket, int cap) {
		int[] th = BRACKETS.get(bracket);
		if (th == null) return cap;
		for (int x : th) if (kills < x) return x;
		return cap;
	}
}
