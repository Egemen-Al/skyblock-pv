package com.example.pv.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Hotm {

	public static class Perk {
		public final String key, name, powder, kind;
		public final int x, y, maxLevel;
		public Perk(String key, String name, int x, int y, int max, String powder, String kind) {
			this.key = key; this.name = name; this.x = x; this.y = y;
			this.maxLevel = max; this.powder = powder; this.kind = kind;
		}
	}

	private static Map<String, Perk> PERKS;

	public static Map<String, Perk> perks() {
		if (PERKS != null) return PERKS;
		PERKS = new LinkedHashMap<>();
		try {
			JsonObject root = JsonParser.parseReader(new InputStreamReader(
				Hotm.class.getResourceAsStream("/assets/skyblockpv/hotm_perks.json"), StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, com.google.gson.JsonElement> e : root.entrySet()) {
				JsonObject o = e.getValue().getAsJsonObject();
				PERKS.put(e.getKey(), new Perk(
					e.getKey(), o.get("n").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(),
					o.get("m").getAsInt(), o.get("p").getAsString(), o.get("k").getAsString()));
			}
		} catch (Exception ignored) {}
		return PERKS;
	}

	private static final double[] XP = {0, 3000, 9000, 25000, 60000, 135000, 285000, 535000, 935000, 1535000};

	public static int level(double xp) {
		int lv = 1;
		for (int i = 0; i < XP.length; i++) if (xp >= XP[i]) lv = i + 1;
		return lv;
	}

	public static int maxLevel() { return XP.length; }

	public static double progress(double xp) {
		int lv = level(xp);
		if (lv >= XP.length) return 1.0;
		double cur = XP[lv - 1], next = XP[lv];
		return next > cur ? Math.min(1.0, (xp - cur) / (next - cur)) : 1.0;
	}
}
