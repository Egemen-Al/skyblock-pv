package com.example.pv.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class LegacyItems {

	public static Item get(int id, int damage) {
		switch (id) {

			case 1:
				switch (damage) {
					case 1: return Items.GRANITE;
					case 3: return Items.DIORITE;
					case 5: return Items.ANDESITE;
					default: return Items.STONE;
				}
			case 2:   return Items.GRASS_BLOCK;
			case 3:   return damage == 1 ? Items.COARSE_DIRT : Items.DIRT;
			case 4:   return Items.COBBLESTONE;
			case 5:
				switch (damage) {
					case 1: return Items.SPRUCE_PLANKS;
					case 2: return Items.BIRCH_PLANKS;
					case 3: return Items.JUNGLE_PLANKS;
					case 4: return Items.ACACIA_PLANKS;
					case 5: return Items.DARK_OAK_PLANKS;
					default: return Items.OAK_PLANKS;
				}
			case 6:   return Items.OAK_SAPLING;
			case 7:   return Items.BEDROCK;
			case 12:  return damage == 1 ? Items.RED_SAND : Items.SAND;
			case 13:  return Items.GRAVEL;
			case 14:  return Items.GOLD_ORE;
			case 15:  return Items.IRON_ORE;
			case 16:  return Items.COAL_ORE;
			case 17:
				switch (damage) {
					case 1: return Items.SPRUCE_LOG;
					case 2: return Items.BIRCH_LOG;
					case 3: return Items.JUNGLE_LOG;
					default: return Items.OAK_LOG;
				}
			case 18:  return Items.OAK_LEAVES;
			case 19:  return Items.SPONGE;
			case 20:  return Items.GLASS;
			case 21:  return Items.LAPIS_ORE;
			case 22:  return Items.LAPIS_BLOCK;
			case 23:  return Items.DISPENSER;
			case 24:  return Items.SANDSTONE;
			case 25:  return Items.NOTE_BLOCK;
			case 26:  return Items.RED_BED;
			case 27:  return Items.POWERED_RAIL;
			case 28:  return Items.DETECTOR_RAIL;
			case 29:  return Items.STICKY_PISTON;
			case 30:  return Items.COBWEB;
			case 31:  return Items.SHORT_GRASS;
			case 32:  return Items.DEAD_BUSH;
			case 33:  return Items.PISTON;
			case 35:  return wool(damage);
			case 37:  return Items.DANDELION;
			case 38:  return Items.POPPY;
			case 39:  return Items.BROWN_MUSHROOM;
			case 40:  return Items.RED_MUSHROOM;
			case 41:  return Items.GOLD_BLOCK;
			case 42:  return Items.IRON_BLOCK;
			case 44:  return Items.STONE_SLAB;
			case 45:  return Items.BRICKS;
			case 46:  return Items.TNT;
			case 47:  return Items.BOOKSHELF;
			case 48:  return Items.MOSSY_COBBLESTONE;
			case 49:  return Items.OBSIDIAN;
			case 50:  return Items.TORCH;
			case 53:  return Items.OAK_STAIRS;
			case 54:  return Items.CHEST;
			case 56:  return Items.DIAMOND_ORE;
			case 57:  return Items.DIAMOND_BLOCK;
			case 58:  return Items.CRAFTING_TABLE;
			case 60:  return Items.FARMLAND;
			case 61:  return Items.FURNACE;
			case 65:  return Items.LADDER;
			case 66:  return Items.RAIL;
			case 67:  return Items.COBBLESTONE_STAIRS;
			case 73:  return Items.REDSTONE_ORE;
			case 76:  return Items.REDSTONE_TORCH;
			case 78:  return Items.SNOW;
			case 79:  return Items.ICE;
			case 80:  return Items.SNOW_BLOCK;
			case 81:  return Items.CACTUS;
			case 82:  return Items.CLAY;
			case 84:  return Items.JUKEBOX;
			case 85:  return Items.OAK_FENCE;
			case 86:  return Items.CARVED_PUMPKIN;
			case 87:  return Items.NETHERRACK;
			case 88:  return Items.SOUL_SAND;
			case 89:  return Items.GLOWSTONE;
			case 91:  return Items.JACK_O_LANTERN;
			case 98:
				switch (damage) {
					case 1: return Items.MOSSY_STONE_BRICKS;
					case 2: return Items.CRACKED_STONE_BRICKS;
					case 3: return Items.CHISELED_STONE_BRICKS;
					default: return Items.STONE_BRICKS;
				}
			case 99:  return Items.BROWN_MUSHROOM_BLOCK;
			case 100: return Items.RED_MUSHROOM_BLOCK;
			case 101: return Items.IRON_BARS;
			case 102: return Items.GLASS_PANE;
			case 103: return Items.MELON;
			case 106: return Items.VINE;
			case 108: return Items.BRICK_STAIRS;
			case 109: return Items.STONE_BRICK_STAIRS;
			case 110: return Items.MYCELIUM;
			case 111: return Items.LILY_PAD;
			case 112: return Items.NETHER_BRICKS;
			case 114: return Items.NETHER_BRICK_STAIRS;
			case 116: return Items.ENCHANTING_TABLE;
			case 120: return Items.END_PORTAL_FRAME;
			case 121: return Items.END_STONE;
			case 122: return Items.DRAGON_EGG;
			case 123: return Items.REDSTONE_LAMP;
			case 125: return Items.OAK_SLAB;
			case 129: return Items.EMERALD_ORE;
			case 130: return Items.ENDER_CHEST;
			case 133: return Items.EMERALD_BLOCK;
			case 134: return Items.SPRUCE_STAIRS;
			case 135: return Items.BIRCH_STAIRS;
			case 136: return Items.JUNGLE_STAIRS;
			case 137: return Items.COMMAND_BLOCK;
			case 138: return Items.BEACON;
			case 139: return Items.COBBLESTONE_WALL;
			case 145: return Items.ANVIL;
			case 146: return Items.TRAPPED_CHEST;
			case 152: return Items.REDSTONE_BLOCK;
			case 153: return Items.NETHER_QUARTZ_ORE;
			case 154: return Items.HOPPER;
			case 155: return Items.QUARTZ_BLOCK;
			case 156: return Items.QUARTZ_STAIRS;
			case 158: return Items.DROPPER;
			case 159: return stainedClay(damage);
			case 162: return Items.ACACIA_LOG;
			case 163: return Items.ACACIA_STAIRS;
			case 164: return Items.DARK_OAK_STAIRS;
			case 165: return Items.SLIME_BLOCK;
			case 168: return Items.PRISMARINE;
			case 169: return Items.SEA_LANTERN;
			case 170: return Items.HAY_BLOCK;
			case 172: return Items.TERRACOTTA;
			case 173: return Items.COAL_BLOCK;
			case 174: return Items.PACKED_ICE;

			case 256: return Items.IRON_SHOVEL;
			case 257: return Items.IRON_PICKAXE;
			case 258: return Items.IRON_AXE;
			case 259: return Items.FLINT_AND_STEEL;
			case 261: return Items.BOW;
			case 267: return Items.IRON_SWORD;
			case 268: return Items.WOODEN_SWORD;
			case 269: return Items.WOODEN_SHOVEL;
			case 270: return Items.WOODEN_PICKAXE;
			case 271: return Items.WOODEN_AXE;
			case 272: return Items.STONE_SWORD;
			case 273: return Items.STONE_SHOVEL;
			case 274: return Items.STONE_PICKAXE;
			case 275: return Items.STONE_AXE;
			case 276: return Items.DIAMOND_SWORD;
			case 277: return Items.DIAMOND_SHOVEL;
			case 278: return Items.DIAMOND_PICKAXE;
			case 279: return Items.DIAMOND_AXE;
			case 283: return Items.GOLDEN_SWORD;
			case 284: return Items.GOLDEN_SHOVEL;
			case 285: return Items.GOLDEN_PICKAXE;
			case 286: return Items.GOLDEN_AXE;
			case 290: return Items.WOODEN_HOE;
			case 291: return Items.STONE_HOE;
			case 292: return Items.IRON_HOE;
			case 293: return Items.DIAMOND_HOE;
			case 294: return Items.GOLDEN_HOE;
			case 346: return Items.FISHING_ROD;
			case 359: return Items.SHEARS;

			case 260: return Items.APPLE;
			case 262: return Items.ARROW;
			case 263: return damage == 1 ? Items.CHARCOAL : Items.COAL;
			case 264: return Items.DIAMOND;
			case 265: return Items.IRON_INGOT;
			case 266: return Items.GOLD_INGOT;
			case 280: return Items.STICK;
			case 281: return Items.BOWL;
			case 282: return Items.MUSHROOM_STEW;
			case 287: return Items.STRING;
			case 288: return Items.FEATHER;
			case 289: return Items.GUNPOWDER;
			case 295: return Items.WHEAT_SEEDS;
			case 296: return Items.WHEAT;
			case 297: return Items.BREAD;
			case 298: return Items.LEATHER_HELMET;
			case 299: return Items.LEATHER_CHESTPLATE;
			case 300: return Items.LEATHER_LEGGINGS;
			case 301: return Items.LEATHER_BOOTS;
			case 302: return Items.CHAINMAIL_HELMET;
			case 303: return Items.CHAINMAIL_CHESTPLATE;
			case 304: return Items.CHAINMAIL_LEGGINGS;
			case 305: return Items.CHAINMAIL_BOOTS;
			case 306: return Items.IRON_HELMET;
			case 307: return Items.IRON_CHESTPLATE;
			case 308: return Items.IRON_LEGGINGS;
			case 309: return Items.IRON_BOOTS;
			case 310: return Items.DIAMOND_HELMET;
			case 311: return Items.DIAMOND_CHESTPLATE;
			case 312: return Items.DIAMOND_LEGGINGS;
			case 313: return Items.DIAMOND_BOOTS;
			case 314: return Items.GOLDEN_HELMET;
			case 315: return Items.GOLDEN_CHESTPLATE;
			case 316: return Items.GOLDEN_LEGGINGS;
			case 317: return Items.GOLDEN_BOOTS;
			case 318: return Items.FLINT;
			case 319: return Items.PORKCHOP;
			case 320: return Items.COOKED_PORKCHOP;
			case 321: return Items.PAINTING;
			case 322: return damage == 1 ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
			case 323: return Items.OAK_SIGN;
			case 325: return Items.BUCKET;
			case 326: return Items.WATER_BUCKET;
			case 327: return Items.LAVA_BUCKET;
			case 328: return Items.MINECART;
			case 329: return Items.SADDLE;
			case 330: return Items.IRON_DOOR;
			case 331: return Items.REDSTONE;
			case 332: return Items.SNOWBALL;
			case 333: return Items.OAK_BOAT;
			case 334: return Items.LEATHER;
			case 335: return Items.MILK_BUCKET;
			case 336: return Items.BRICK;
			case 337: return Items.CLAY_BALL;
			case 338: return Items.SUGAR_CANE;
			case 339: return Items.PAPER;
			case 340: return Items.BOOK;
			case 341: return Items.SLIME_BALL;
			case 342: return Items.CHEST_MINECART;
			case 343: return Items.FURNACE_MINECART;
			case 344: return Items.EGG;
			case 345: return Items.COMPASS;
			case 347: return Items.CLOCK;
			case 348: return Items.GLOWSTONE_DUST;
			case 349: return Items.COD;
			case 350: return Items.COOKED_COD;
			case 351: return dye(damage);
			case 352: return Items.BONE;
			case 353: return Items.SUGAR;
			case 354: return Items.CAKE;
			case 355: return Items.RED_BED;
			case 356: return Items.REPEATER;
			case 357: return Items.COOKIE;
			case 358: return Items.FILLED_MAP;
			case 360: return Items.MELON_SLICE;
			case 361: return Items.PUMPKIN_SEEDS;
			case 362: return Items.MELON_SEEDS;
			case 363: return Items.BEEF;
			case 364: return Items.COOKED_BEEF;
			case 365: return Items.CHICKEN;
			case 366: return Items.COOKED_CHICKEN;
			case 367: return Items.ROTTEN_FLESH;
			case 368: return Items.ENDER_PEARL;
			case 369: return Items.BLAZE_ROD;
			case 370: return Items.GHAST_TEAR;
			case 371: return Items.GOLD_NUGGET;
			case 372: return Items.NETHER_WART;
			case 373: return Items.POTION;
			case 374: return Items.GLASS_BOTTLE;
			case 375: return Items.SPIDER_EYE;
			case 376: return Items.FERMENTED_SPIDER_EYE;
			case 377: return Items.BLAZE_POWDER;
			case 378: return Items.MAGMA_CREAM;
			case 379: return Items.BREWING_STAND;
			case 380: return Items.CAULDRON;
			case 381: return Items.ENDER_EYE;
			case 382: return Items.GLISTERING_MELON_SLICE;
			case 383: return Items.PIG_SPAWN_EGG;
			case 384: return Items.EXPERIENCE_BOTTLE;
			case 385: return Items.FIRE_CHARGE;
			case 386: return Items.WRITABLE_BOOK;
			case 387: return Items.WRITTEN_BOOK;
			case 388: return Items.EMERALD;
			case 389: return Items.ITEM_FRAME;
			case 390: return Items.FLOWER_POT;
			case 391: return Items.CARROT;
			case 392: return Items.POTATO;
			case 393: return Items.BAKED_POTATO;
			case 394: return Items.POISONOUS_POTATO;
			case 395: return Items.MAP;
			case 396: return Items.GOLDEN_CARROT;
			case 397: return skull(damage);
			case 398: return Items.CARROT_ON_A_STICK;
			case 399: return Items.NETHER_STAR;
			case 400: return Items.PUMPKIN_PIE;
			case 402: return Items.FIREWORK_STAR;
			case 403: return Items.ENCHANTED_BOOK;
			case 404: return Items.COMPARATOR;
			case 405: return Items.NETHER_BRICK;
			case 406: return Items.QUARTZ;
			case 407: return Items.TNT_MINECART;
			case 408: return Items.HOPPER_MINECART;
			case 409: return Items.PRISMARINE_SHARD;
			case 410: return Items.PRISMARINE_CRYSTALS;
			case 411: return Items.RABBIT;
			case 412: return Items.COOKED_RABBIT;
			case 413: return Items.RABBIT_STEW;
			case 414: return Items.RABBIT_FOOT;
			case 415: return Items.RABBIT_HIDE;
			case 416: return Items.ARMOR_STAND;
			case 417: return Items.IRON_HORSE_ARMOR;
			case 418: return Items.GOLDEN_HORSE_ARMOR;
			case 419: return Items.DIAMOND_HORSE_ARMOR;
			case 420: return Items.LEAD;
			case 421: return Items.NAME_TAG;
			case 422: return Items.COMMAND_BLOCK_MINECART;
			case 425: return Items.WHITE_BANNER;

			default:  return null;
		}
	}

	private static Item wool(int d) {
		switch (d) {
			case 1: return Items.ORANGE_WOOL;
			case 2: return Items.MAGENTA_WOOL;
			case 3: return Items.LIGHT_BLUE_WOOL;
			case 4: return Items.YELLOW_WOOL;
			case 5: return Items.LIME_WOOL;
			case 6: return Items.PINK_WOOL;
			case 7: return Items.GRAY_WOOL;
			case 8: return Items.LIGHT_GRAY_WOOL;
			case 9: return Items.CYAN_WOOL;
			case 10: return Items.PURPLE_WOOL;
			case 11: return Items.BLUE_WOOL;
			case 12: return Items.BROWN_WOOL;
			case 13: return Items.GREEN_WOOL;
			case 14: return Items.RED_WOOL;
			case 15: return Items.BLACK_WOOL;
			default: return Items.WHITE_WOOL;
		}
	}

	private static Item stainedClay(int d) {
		switch (d) {
			case 1: return Items.ORANGE_TERRACOTTA;
			case 2: return Items.MAGENTA_TERRACOTTA;
			case 3: return Items.LIGHT_BLUE_TERRACOTTA;
			case 4: return Items.YELLOW_TERRACOTTA;
			case 5: return Items.LIME_TERRACOTTA;
			case 6: return Items.PINK_TERRACOTTA;
			case 7: return Items.GRAY_TERRACOTTA;
			case 8: return Items.LIGHT_GRAY_TERRACOTTA;
			case 9: return Items.CYAN_TERRACOTTA;
			case 10: return Items.PURPLE_TERRACOTTA;
			case 11: return Items.BLUE_TERRACOTTA;
			case 12: return Items.BROWN_TERRACOTTA;
			case 13: return Items.GREEN_TERRACOTTA;
			case 14: return Items.RED_TERRACOTTA;
			case 15: return Items.BLACK_TERRACOTTA;
			default: return Items.WHITE_TERRACOTTA;
		}
	}

	private static Item dye(int d) {
		switch (d) {
			case 0:  return Items.INK_SAC;
			case 1:  return Items.RED_DYE;
			case 2:  return Items.GREEN_DYE;
			case 3:  return Items.COCOA_BEANS;
			case 4:  return Items.LAPIS_LAZULI;
			case 5:  return Items.PURPLE_DYE;
			case 6:  return Items.CYAN_DYE;
			case 7:  return Items.LIGHT_GRAY_DYE;
			case 8:  return Items.GRAY_DYE;
			case 9:  return Items.PINK_DYE;
			case 10: return Items.LIME_DYE;
			case 11: return Items.YELLOW_DYE;
			case 12: return Items.LIGHT_BLUE_DYE;
			case 13: return Items.MAGENTA_DYE;
			case 14: return Items.ORANGE_DYE;
			case 15: return Items.BONE_MEAL;
			default: return Items.INK_SAC;
		}
	}

	private static Item skull(int d) {

		switch (d) {
			case 0: return Items.SKELETON_SKULL;
			case 1: return Items.WITHER_SKELETON_SKULL;
			case 2: return Items.ZOMBIE_HEAD;
			case 4: return Items.CREEPER_HEAD;
			default: return Items.PLAYER_HEAD;
		}
	}
}
