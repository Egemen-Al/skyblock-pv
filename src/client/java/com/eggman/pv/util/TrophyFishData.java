package com.eggman.pv.util;

public class TrophyFishData {

	public static final String[] FISH = {
		"gusher", "flyfish", "moldfin", "vanille", "blobfish", "mana_ray",
		"slugfish", "soul_fish", "lava_horse", "golden_fish", "karate_fish", "skeleton_fish",
		"sulphur_skitter", "obfuscated_fish_1", "obfuscated_fish_2", "obfuscated_fish_3",
		"volcanic_stonefish", "steaming_hot_flounder"
	};
	public static final String[] FISH_COLOR = {
		"§f", "§a", "§5", "§9", "§f", "§9",
		"§a", "§5", "§9", "§6", "§5", "§5",
		"§f", "§f", "§a", "§9", "§9", "§f"
	};

	public static final int[][] SLOTS = {
		{277, 46}, {253, 58}, {301, 58}, {229, 70}, {325, 70}, {277, 70},
		{253, 82}, {301, 82}, {229, 94}, {325, 94}, {253, 106}, {301, 106},
		{277, 118}, {229, 118}, {325, 118}, {253, 130}, {301, 130}, {277, 142}
	};

	public static final String[] RARITIES = {"bronze", "silver", "gold", "diamond"};

	public static final String[] RANK_LABELS = {"§aNovice Fisher", "§9Adept Fisher", "§5Expert Fisher", "§6Master Fisher"};
	public static final String[] RANK_HELMETS = {"BRONZE_HUNTER_HELMET", "SILVER_HUNTER_HELMET", "GOLD_HUNTER_HELMET", "DIAMOND_HUNTER_HELMET"};
	public static final int[] RANK_NEEDED = {15, 18, 18, 18};

	public static String requirement(String fish) {
		switch (fish) {
			case "sulphur_skitter": return "Within 4 blocks of Sulphur Ore";
			case "blobfish": return "Anywhere";
			case "obfuscated_fish_1": return "With Corrupted Bait";
			case "steaming_hot_flounder": return "Bobber within 2 blocks of a Geyser (Blazing Volcano)";
			case "gusher": return "Blazing Volcano, 7-16 min after an eruption";
			case "obfuscated_fish_2": return "Using Obfuscated 1 as bait";
			case "slugfish": return "Bobber active for 20s+";
			case "flyfish": return "8+ blocks above lava in Blazing Volcano";
			case "obfuscated_fish_3": return "Using Obfuscated 2 as bait";
			case "vanille": return "Starter Lava Rod with no enchants";
			case "lava_horse": return "Anywhere";
			case "mana_ray": return "With 1,200+ Mana";
			case "volcanic_stonefish": return "In the Blazing Volcano";
			case "skeleton_fish": return "In the Burning Desert";
			case "moldfin": return "In the Mystic Marsh";
			case "soul_fish": return "In the Stronghold";
			case "karate_fish": return "Lava pools near the Dojo";
			case "golden_fish": return "After 8+ minutes of fishing";
			default: return "";
		}
	}

	public static String pretty(String internal) {
		String[] p = internal.split("_");
		StringBuilder b = new StringBuilder();
		for (String s : p) {
			if (s.isEmpty()) continue;
			b.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(' ');
		}
		return b.toString().trim();
	}
}
