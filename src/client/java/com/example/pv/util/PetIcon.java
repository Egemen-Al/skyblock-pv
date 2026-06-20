package com.example.pv.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class PetIcon {

	private static final Map<String, Item> MAP = new HashMap<>();
	private static void p(String t, Item i) { MAP.put(t, i); }
	static {
		p("BAT", Items.BAT_SPAWN_EGG); p("WITHER_SKELETON", Items.WITHER_SKELETON_SPAWN_EGG);
		p("SILVERFISH", Items.SILVERFISH_SPAWN_EGG); p("ENDERMITE", Items.ENDERMITE_SPAWN_EGG);
		p("BEE", Items.BEE_SPAWN_EGG); p("CHICKEN", Items.CHICKEN_SPAWN_EGG); p("PIG", Items.PIG_SPAWN_EGG);
		p("RABBIT", Items.RABBIT_SPAWN_EGG); p("DOLPHIN", Items.DOLPHIN_SPAWN_EGG); p("SQUID", Items.SQUID_SPAWN_EGG);
		p("SHEEP", Items.SHEEP_SPAWN_EGG); p("PARROT", Items.PARROT_SPAWN_EGG); p("OCELOT", Items.OCELOT_SPAWN_EGG);
		p("BLACK_CAT", Items.CAT_SPAWN_EGG); p("BLAZE", Items.BLAZE_SPAWN_EGG); p("ENDER_DRAGON", Items.DRAGON_HEAD);
		p("ENDERMAN", Items.ENDERMAN_SPAWN_EGG); p("GHOUL", Items.ZOMBIE_SPAWN_EGG); p("GOLEM", Items.IRON_GOLEM_SPAWN_EGG);
		p("MITHRIL_GOLEM", Items.IRON_GOLEM_SPAWN_EGG); p("HORSE", Items.HORSE_SPAWN_EGG);
		p("HOUND", Items.WOLF_SPAWN_EGG); p("WOLF", Items.WOLF_SPAWN_EGG); p("GRANDMA_WOLF", Items.WOLF_SPAWN_EGG);
		p("MAGMA_CUBE", Items.MAGMA_CUBE_SPAWN_EGG); p("PIGMAN", Items.ZOMBIFIED_PIGLIN_SPAWN_EGG);
		p("SKELETON", Items.SKELETON_SPAWN_EGG); p("SKELETON_HORSE", Items.SKELETON_HORSE_SPAWN_EGG);
		p("SNOWMAN", Items.SNOWBALL); p("SPIDER", Items.SPIDER_SPAWN_EGG); p("TARANTULA", Items.CAVE_SPIDER_SPAWN_EGG);
		p("TURTLE", Items.TURTLE_SPAWN_EGG); p("ZOMBIE", Items.ZOMBIE_SPAWN_EGG); p("GUARDIAN", Items.GUARDIAN_SPAWN_EGG);
		p("ARMADILLO", Items.ARMADILLO_SPAWN_EGG); p("MOOSHROOM_COW", Items.MOOSHROOM_SPAWN_EGG);
		p("FROG", Items.FROG_SPAWN_EGG); p("WITCH", Items.WITCH_SPAWN_EGG); p("ROCK", Items.COBBLESTONE);
		p("PHOENIX", Items.BLAZE_POWDER); p("GOLDEN_DRAGON", Items.GOLD_BLOCK); p("JADE_DRAGON", Items.EMERALD_BLOCK);
		p("ROSE_DRAGON", Items.PINK_WOOL); p("MEGALODON", Items.PRISMARINE_SHARD); p("BLUE_WHALE", Items.COD);
		p("FLYING_FISH", Items.COD); p("JELLYFISH", Items.SLIME_BALL); p("AMMONITE", Items.NAUTILUS_SHELL);
		p("SEAL", Items.COD); p("HERMIT_CRAB", Items.NAUTILUS_SHELL); p("SCATHA", Items.STONE);
		p("SNAIL", Items.SLIME_BALL); p("GLACITE_GOLEM", Items.PACKED_ICE); p("OWL", Items.FEATHER);
		p("CROW", Items.FEATHER); p("RAT", Items.WHEAT_SEEDS); p("KUUDRA", Items.MAGMA_CREAM);
		p("ELEPHANT", Items.HAY_BLOCK); p("MONKEY", Items.JUNGLE_SAPLING); p("LION", Items.GOLDEN_APPLE);
		p("GIRAFFE", Items.OAK_SAPLING); p("TIGER", Items.GOLDEN_APPLE); p("GRIFFIN", Items.FEATHER);
		p("JERRY", Items.CARVED_PUMPKIN); p("SPIRIT", Items.GHAST_TEAR); p("BAL", Items.MAGMA_CREAM);
	}

	public static Item get(String type) {
		Item i = MAP.get(type);
		return i != null ? i : Items.BONE;
	}
}
