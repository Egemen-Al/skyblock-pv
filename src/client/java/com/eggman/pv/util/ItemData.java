package com.eggman.pv.util;

import java.util.Collections;
import java.util.List;

public class ItemData {
	public final String name;
	public final List<String> lore;
	public final int count;
	public final String skyblockId;
	public final String rarity;
	public final boolean dungeon;
	public final int legacyId;
	public final int damage;
	public final String skullTexture;
	public final int dyeColor;

	public ItemData(String name, List<String> lore, int count, String skyblockId,
	                String rarity, boolean dungeon, int legacyId, int damage, String skullTexture, int dyeColor) {
		this.name = name == null ? "" : name;
		this.lore = lore == null ? Collections.emptyList() : lore;
		this.count = count;
		this.skyblockId = skyblockId == null ? "" : skyblockId;
		this.rarity = rarity == null ? "" : rarity;
		this.dungeon = dungeon;
		this.legacyId = legacyId;
		this.damage = damage;
		this.skullTexture = skullTexture == null ? "" : skullTexture;
		this.dyeColor = dyeColor;
	}

	public boolean isEmpty() {
		return name.isEmpty() && skyblockId.isEmpty() && legacyId == 0;
	}

	public static String strip(String s) {
		if (s == null) return "";
		return s.replaceAll("(?i)§[0-9A-FK-OR]", "");
	}
}
