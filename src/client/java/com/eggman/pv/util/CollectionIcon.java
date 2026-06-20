package com.eggman.pv.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class CollectionIcon {

	private static final int TEX = 64;
	private static final Map<String, Identifier> ICONS = new HashMap<>();

	private static void put(String id, String file) {
		ICONS.put(id, PvRender.tex("collection/" + file + ".png"));
	}

	static {

		put("WHEAT", "wheat"); put("CARROT_ITEM", "carrot"); put("POTATO_ITEM", "potato");
		put("PUMPKIN", "pumpkin"); put("MELON", "melon_slice"); put("SEEDS", "seeds");
		put("MUSHROOM_COLLECTION", "red_mushroom"); put("INK_SACK:3", "cocoa_beans");
		put("CACTUS", "cactus"); put("SUGAR_CANE", "sugar_cane"); put("FEATHER", "feather");
		put("LEATHER", "leather"); put("PORK", "raw_porkchop"); put("RAW_CHICKEN", "raw_chicken");
		put("MUTTON", "raw_mutton"); put("RABBIT", "raw_rabbit"); put("NETHER_STALK", "nether_wart");

		put("COBBLESTONE", "cobblestone"); put("COAL", "coal"); put("IRON_INGOT", "iron_ingot");
		put("GOLD_INGOT", "gold_ingot"); put("DIAMOND", "diamond"); put("INK_SACK:4", "lapis_lazuli");
		put("EMERALD", "emerald"); put("REDSTONE", "redstone_dust"); put("QUARTZ", "nether_quartz");
		put("OBSIDIAN", "obsidian"); put("GLOWSTONE_DUST", "glowstone_dust"); put("GRAVEL", "gravel");
		put("ICE", "ice"); put("NETHERRACK", "netherrack"); put("SAND", "sand"); put("ENDER_STONE", "end_stone");
		put("MITHRIL_ORE", "mithril"); put("HARD_STONE", "stone"); put("GEMSTONE_COLLECTION", "fine_jasper_gemstone");
		put("MYCEL", "mycelium"); put("SAND:1", "red_sand"); put("SULPHUR_ORE", "sulphur");
		put("TUNGSTEN", "tungsten"); put("UMBER", "umber"); put("GLACITE", "packed_ice");

		put("ROTTEN_FLESH", "rotten_flesh"); put("BONE", "bone"); put("STRING", "string");
		put("SPIDER_EYE", "spider_eye"); put("SULPHUR", "gunpowder"); put("ENDER_PEARL", "ender_pearl");
		put("GHAST_TEAR", "ghast_tear"); put("SLIME_BALL", "slimeball"); put("BLAZE_ROD", "blaze_rod");
		put("MAGMA_CREAM", "magma_cream"); put("CHILI_PEPPER", "chili_pepper");

		put("LOG", "oak_log"); put("LOG:1", "spruce_log"); put("LOG:2", "birch_log"); put("LOG:3", "jungle_log");
		put("LOG_2", "acacia_log"); put("LOG_2:1", "dark_oak_log"); put("MANGROVE_LOG", "mangrove_log");
		put("WILD_ROSE", "rose_bush"); put("MOONFLOWER", "blue_orchid"); put("DOUBLE_PLANT", "sunflower");
		put("FIG_LOG", "fig"); put("TENDER_WOOD", "tender_wood");

		put("RAW_FISH", "raw_cod"); put("RAW_FISH:1", "raw_salmon"); put("RAW_FISH:2", "tropical_fish");
		put("RAW_FISH:3", "pufferfish"); put("PRISMARINE_SHARD", "prismarine_shard");
		put("PRISMARINE_CRYSTALS", "prismarine_crystals"); put("CLAY_BALL", "clay_ball");
		put("WATER_LILY", "lily_pad"); put("INK_SACK", "ink_sac"); put("SPONGE", "sponge");
		put("MAGMA_FISH", "magmafish"); put("SEA_LUMIES", "sea_pickle"); put("SEA_LUMINES", "sea_pickle");

		put("AGARICUS_CAP", "agaricus_cap"); put("CADUCOUS_STEM", "cadacous_stem");
		put("HALF_EATEN_CARROT", "half_eaten_carror"); put("HEMOVIBE", "redstone_ore");
		put("TIMITE", "timite"); put("METAL_HEART", "living_metal_heart");
		put("LIVING_METAL_HEART", "living_metal_heart"); put("LUSHLILAC", "lushlilac");
		put("WILTED_BERBERIS", "dead_bush"); put("VINESAP", "vinesap"); put("LOTUS", "lotus");
	}

	public static void draw(GuiGraphics g, String id, int x, int y) {
		Identifier tex = ICONS.get(id);
		if (tex != null) {
			PvRender.drawTexturedRect(g, tex, x, y, 16, 16, 0f, 0f, TEX, TEX, TEX, TEX);
			return;
		}

		g.renderItem(new ItemStack(Items.PAPER), x, y);
	}
}
