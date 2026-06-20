package com.eggman.pv.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class NbtParser {

	private static final String[] RARITIES = {
			"VERY SPECIAL", "ULTIMATE", "DIVINE", "SUPREME", "MYTHIC", "LEGENDARY",
			"EPIC", "RARE", "UNCOMMON", "COMMON", "SPECIAL"
	};

	public static List<ItemData> parse(String base64) {
		List<ItemData> out = new ArrayList<>();
		if (base64 == null || base64.isEmpty()) return out;
		try {
			byte[] bytes = Base64.getDecoder().decode(base64);
			CompoundTag root = NbtIo.readCompressed(
					new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
			ListTag items = root.getListOrEmpty("i");
			for (Tag t : items) {
				if (!(t instanceof CompoundTag c) || c.isEmpty()) {
					out.add(null);
					continue;
				}
				out.add(readItem(c));
			}
		} catch (Exception e) {

		}
		return out;
	}

	private static ItemData readItem(CompoundTag c) {
		int count = c.getByteOr("Count", (byte) 1);
		int legacyId = c.getShortOr("id", (short) 0);
		int damage = c.getShortOr("Damage", (short) 0);
		CompoundTag tag = c.getCompoundOrEmpty("tag");
		CompoundTag display = tag.getCompoundOrEmpty("display");
		CompoundTag extra = tag.getCompoundOrEmpty("ExtraAttributes");

		String name = display.getStringOr("Name", "");
		String sbId = extra.getStringOr("id", "");
		int dyeColor = display.getIntOr("color", -1);

		String skullTex = "";
		CompoundTag skullOwner = tag.getCompoundOrEmpty("SkullOwner");
		ListTag texs = skullOwner.getCompoundOrEmpty("Properties").getListOrEmpty("textures");
		if (!texs.isEmpty() && texs.get(0) instanceof CompoundTag t0) {
			skullTex = t0.getStringOr("Value", "");
		}

		List<String> lore = new ArrayList<>();
		ListTag loreTag = display.getListOrEmpty("Lore");
		for (Tag lt : loreTag) {
			lore.add(lt.asString().orElse(""));
		}

		String rarity = "";
		boolean dungeon = false;
		for (int i = lore.size() - 1; i >= 0; i--) {
			String line = ItemData.strip(lore.get(i)).toUpperCase();
			if (line.trim().isEmpty()) continue;
			for (String r : RARITIES) {
				if (line.contains(r)) { rarity = r; break; }
			}
			if (!rarity.isEmpty()) {
				dungeon = line.contains("DUNGEON");
				break;
			}
		}

		return new ItemData(name, lore, count, sbId, rarity, dungeon, legacyId, damage, skullTex, dyeColor);
	}
}
