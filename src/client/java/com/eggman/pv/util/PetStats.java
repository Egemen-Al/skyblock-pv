package com.eggman.pv.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PetStats {

	private static JsonObject PETNUMS;
	private static JsonObject PETTYPES;

	private static void load() {
		if (PETNUMS != null) return;
		try {
			PETNUMS = JsonParser.parseReader(new InputStreamReader(
				PetStats.class.getResourceAsStream("/assets/skyblockpv/petnums.json"), StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject pets = JsonParser.parseReader(new InputStreamReader(
				PetStats.class.getResourceAsStream("/assets/skyblockpv/pets.json"), StandardCharsets.UTF_8)).getAsJsonObject();
			PETTYPES = pets.getAsJsonObject("pet_types");
		} catch (Exception e) {
			PETNUMS = new JsonObject();
			PETTYPES = new JsonObject();
		}
	}

	public static String category(String type) {
		load();
		return (PETTYPES != null && PETTYPES.has(type)) ? PETTYPES.get(type).getAsString() : null;
	}

	public static List<String> statLines(String type, String rarity, int level) {
		load();
		List<String> out = new ArrayList<>();
		if (PETNUMS == null || !PETNUMS.has(type)) return out;
		JsonObject byRar = PETNUMS.getAsJsonObject(type);
		if (!byRar.has(rarity)) {

			for (String r : new String[]{"LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON", "MYTHIC"}) {
				if (byRar.has(r)) { rarity = r; break; }
			}
			if (!byRar.has(rarity)) return out;
		}
		JsonObject lv = byRar.getAsJsonObject(rarity);
		if (!lv.has("1") || !lv.has("100")) return out;
		JsonObject s1 = lv.getAsJsonObject("1").getAsJsonObject("statNums");
		JsonObject s100 = lv.getAsJsonObject("100").getAsJsonObject("statNums");
		double t = Math.max(0, (level - 1) / 99.0);

		for (Map.Entry<String, com.google.gson.JsonElement> e : s1.entrySet()) {
			String key = e.getKey();
			double v1 = e.getValue().getAsDouble();
			double v100 = s100.has(key) ? s100.get(key).getAsDouble() : v1;
			double val = v1 + (v100 - v1) * t;
			out.add(format(key, val));
		}
		return out;
	}

	private static String resolveRarity(JsonObject byRar, String rarity) {
		if (byRar.has(rarity)) return rarity;
		for (String r : new String[]{"LEGENDARY", "EPIC", "RARE", "UNCOMMON", "COMMON", "MYTHIC"})
			if (byRar.has(r)) return r;
		return null;
	}

	public static Map<String, Double> interpStats(String type, String rarity, int level) {
		load();
		Map<String, Double> out = new LinkedHashMap<>();
		if (PETNUMS == null || !PETNUMS.has(type)) return out;
		JsonObject byRar = PETNUMS.getAsJsonObject(type);
		rarity = resolveRarity(byRar, rarity);
		if (rarity == null) return out;
		JsonObject lv = byRar.getAsJsonObject(rarity);
		if (!lv.has("1") || !lv.has("100")) return out;
		JsonObject s1 = lv.getAsJsonObject("1").getAsJsonObject("statNums");
		JsonObject s100 = lv.getAsJsonObject("100").getAsJsonObject("statNums");
		double t = Math.max(0, (level - 1) / 99.0);
		for (Map.Entry<String, com.google.gson.JsonElement> e : s1.entrySet()) {
			double v1 = e.getValue().getAsDouble();
			double v100 = s100.has(e.getKey()) ? s100.get(e.getKey()).getAsDouble() : v1;
			out.put(e.getKey(), v1 + (v100 - v1) * t);
		}
		return out;
	}

	public static double[] interpOther(String type, String rarity, int level) {
		load();
		if (PETNUMS == null || !PETNUMS.has(type)) return new double[0];
		JsonObject byRar = PETNUMS.getAsJsonObject(type);
		rarity = resolveRarity(byRar, rarity);
		if (rarity == null) return new double[0];
		JsonObject lv = byRar.getAsJsonObject(rarity);
		if (!lv.has("1") || !lv.has("100")) return new double[0];
		com.google.gson.JsonArray o1 = lv.getAsJsonObject("1").getAsJsonArray("otherNums");
		com.google.gson.JsonArray o100 = lv.getAsJsonObject("100").getAsJsonArray("otherNums");
		double t = Math.max(0, (level - 1) / 99.0);
		double[] out = new double[o1.size()];
		for (int i = 0; i < o1.size(); i++) {
			double v1 = o1.get(i).getAsDouble();
			double v100 = i < o100.size() ? o100.get(i).getAsDouble() : v1;
			out[i] = v1 + (v100 - v1) * t;
		}
		return out;
	}

	public static String num(double v) {
		return (Math.abs(v - Math.round(v)) < 0.05) ? String.valueOf(Math.round(v)) : String.format("%.1f", v);
	}

	private static String format(String stat, double val) {
		String num = (Math.abs(val - Math.round(val)) < 0.05)
			? String.valueOf(Math.round(val))
			: String.format("%.1f", val);
		Sym s = SYM.get(stat);
		if (s == null) return "§7" + pretty(stat) + ": §a+" + num;
		return s.color + s.icon + " " + s.name + ": §a+" + num + s.suffix;
	}

	private static String pretty(String k) {
		String[] p = k.toLowerCase().split("_");
		StringBuilder b = new StringBuilder();
		for (String x : p) b.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1)).append(' ');
		return b.toString().trim();
	}

	private record Sym(String color, String icon, String name, String suffix) {}
	private static final Map<String, Sym> SYM = new LinkedHashMap<>();
	private static void s(String k, String c, String i, String n, String suf) { SYM.put(k, new Sym(c, i, n, suf)); }
	static {
		s("HEALTH", "§c", "❤", "Health", "");
		s("DEFENSE", "§a", "❈", "Defense", "");
		s("TRUE_DEFENSE", "§f", "❂", "True Defense", "");
		s("STRENGTH", "§c", "❁", "Strength", "");
		s("DAMAGE", "§c", "❁", "Damage", "");
		s("INTELLIGENCE", "§b", "✎", "Intelligence", "");
		s("SPEED", "§f", "✦", "Speed", "");
		s("CRIT_CHANCE", "§9", "☣", "Crit Chance", "%");
		s("CRIT_DAMAGE", "§9", "☠", "Crit Damage", "%");
		s("BONUS_ATTACK_SPEED", "§e", "⚔", "Bonus Attack Speed", "%");
		s("FEROCITY", "§c", "⫽", "Ferocity", "");
		s("MAGIC_FIND", "§b", "✯", "Magic Find", "");
		s("PET_LUCK", "§d", "♣", "Pet Luck", "");
		s("SEA_CREATURE_CHANCE", "§3", "α", "Sea Creature Chance", "%");
		s("FISHING_SPEED", "§b", "☂", "Fishing Speed", "");
		s("HEALTH_REGEN", "§c", "❤", "Health Regen", "");
		s("VITALITY", "§4", "♨", "Vitality", "");
		s("MENDING", "§a", "☄", "Mending", "");
		s("MINING_SPEED", "§6", "⸕", "Mining Speed", "");
		s("MINING_FORTUNE", "§6", "☘", "Mining Fortune", "");
		s("FARMING_FORTUNE", "§6", "☘", "Farming Fortune", "");
		s("FORAGING_FORTUNE", "§6", "☘", "Foraging Fortune", "");
		s("ORE_FORTUNE", "§6", "☘", "Ore Fortune", "");
		s("ABILITY_DAMAGE", "§c", "๑", "Ability Damage", "%");
		s("ALCHEMY_WISDOM", "§3", "☯", "Alchemy Wisdom", "");
		s("RESPIRATION", "§b", "≈", "Respiration", "");
		s("TREASURE_CHANCE", "§6", "⛃", "Treasure Chance", "");
		s("TROPHY_CHANCE", "§6", "♔", "Trophy Fish Chance", "");
		s("SWING_RANGE", "§a", "⟿", "Swing Range", "");
	}
}
