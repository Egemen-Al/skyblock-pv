package com.eggman.pv.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepoPets {

	private static final Pattern VALUE = Pattern.compile("Value:\\\\?\"([A-Za-z0-9+/=]+)\\\\?\"");

	public static int rarityNum(String tier) {
		switch (tier) {
			case "UNCOMMON": return 1;
			case "RARE": return 2;
			case "EPIC": return 3;
			case "LEGENDARY": return 4;
			case "MYTHIC": return 5;
			default: return 0;
		}
	}

	private static JsonObject item(String type, String tier) {
		JsonObject o = RepoManager.getItem(type + ";" + rarityNum(tier));
		if (o == null && !"COMMON".equals(tier)) {

			o = RepoManager.getItem(type + ";4");
		}
		return o;
	}

	private static String fill(String line, int level, Map<String, Double> stats, double[] other) {
		String s = line.replace("{LVL}", String.valueOf(level));
		for (Map.Entry<String, Double> e : stats.entrySet()) {
			if (s.contains("{" + e.getKey() + "}")) {
				s = s.replace("{" + e.getKey() + "}", PetStats.num(e.getValue()));
			}
		}
		for (int i = 0; i < other.length; i++) {
			s = s.replace("{" + i + "}", PetStats.num(other[i]));
		}
		return s;
	}

	public static List<String> lore(String type, String tier, int level) {
		JsonObject o = item(type, tier);
		if (o == null || !o.has("lore")) return null;
		Map<String, Double> stats = PetStats.interpStats(type, tier, level);
		double[] other = PetStats.interpOther(type, tier, level);
		JsonArray arr = o.getAsJsonArray("lore");
		List<String> out = new ArrayList<>();
		for (int i = 0; i < arr.size(); i++) out.add(fill(arr.get(i).getAsString(), level, stats, other));
		return out;
	}

	public static String displayName(String type, String tier, int level) {
		JsonObject o = item(type, tier);
		if (o == null || !o.has("displayname")) return null;
		return o.get("displayname").getAsString().replace("{LVL}", String.valueOf(level));
	}

	public static List<String> heldItemLines(String heldId) {
		if (heldId == null || heldId.isEmpty()) return null;
		JsonObject o = RepoManager.getItem(heldId);
		String name;
		if (o != null && o.has("displayname")) name = o.get("displayname").getAsString();
		else {
			String[] parts = heldId.replace("PET_ITEM_", "").toLowerCase().split("_");
			StringBuilder b = new StringBuilder();
			for (String s : parts) if (!s.isEmpty()) b.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(' ');
			name = "§a" + b.toString().trim();
		}
		List<String> out = new ArrayList<>();
		out.add("§6Held Item: " + name);
		if (o != null && o.has("lore")) {
			JsonArray la = o.getAsJsonArray("lore");
			boolean started = false;
			for (int i = 0; i < la.size(); i++) {
				String l = la.get(i).getAsString();
				if (l.trim().isEmpty()) { if (started) break; else continue; }

				String stripped = l.replaceAll("(?i)§[0-9a-fk-or]", "").trim();
				if (stripped.matches("(?i)(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|SPECIAL|VERY SPECIAL).*")) continue;
				out.add(l);
				started = true;
			}
		}
		return out;
	}

	public static String skull(String type, String tier) {
		JsonObject o = item(type, tier);
		if (o == null || !o.has("nbttag")) return null;
		Matcher m = VALUE.matcher(o.get("nbttag").getAsString());
		return m.find() ? m.group(1) : null;
	}
}
