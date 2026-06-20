package com.example.pv.util;

import java.util.List;

public class MagicalPower {

	public static int mpFor(String rarity) {
		switch (rarity) {
			case "MYTHIC":       return 22;
			case "LEGENDARY":    return 16;
			case "EPIC":         return 12;
			case "RARE":         return 8;
			case "UNCOMMON":     return 5;
			case "VERY SPECIAL": return 5;
			case "COMMON":       return 3;
			case "SPECIAL":      return 3;
			default:             return 0;
		}
	}

	public static int total(List<ItemData> talismans, boolean inDungeon) {
		if (talismans == null) return 0;
		int sum = 0;
		boolean riftPrism = false;
		for (ItemData it : talismans) {
			if (it == null || it.isEmpty()) continue;
			int mp = mpFor(it.rarity);
			if ("HEGEMONY_ARTIFACT".equals(it.skyblockId)) mp *= 2;
			if (inDungeon && it.dungeon) mp *= 2;
			if ("RIFT_PRISM".equals(it.skyblockId)) riftPrism = true;
			sum += mp;
		}
		if (riftPrism) sum += 11;
		return sum;
	}

	public static double scaling(int mp) {
		return 719.28 * Math.pow(Math.log(1 + 0.0019 * mp), 1.2);
	}
}
