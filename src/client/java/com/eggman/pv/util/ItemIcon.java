package com.eggman.pv.util;

import com.google.common.collect.LinkedHashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemIcon {

	private static final Map<String, ItemStack> SKULL_CACHE = new HashMap<>();

	public static void draw(GuiGraphics g, ItemData it, int x, int y) {
		if (it == null || it.isEmpty()) return;

		if (!it.skullTexture.isEmpty()) {
			ItemStack head = skull(it.skullTexture);
			if (head != null) {
				g.renderItem(head, x, y);
				return;
			}
		}

		Item item = LegacyItems.get(it.legacyId, it.damage);
		if (item != null) {
			ItemStack stack = new ItemStack(item);
			if (it.dyeColor >= 0) {
				stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
						new net.minecraft.world.item.component.DyedItemColor(it.dyeColor));
			}
			g.renderItem(stack, x, y);
		}
	}

	public static void drawSkull(GuiGraphics g, String textureValue, int x, int y) {
		ItemStack head = skull(textureValue);
		if (head != null) g.renderItem(head, x, y);
	}

	public static ItemStack skullStack(String textureValue) {
		return skull(textureValue);
	}

	private static ItemStack skull(String textureValue) {
		ItemStack cached = SKULL_CACHE.get(textureValue);
		if (cached != null) return cached;
		try {
			UUID uuid = UUID.nameUUIDFromBytes(textureValue.getBytes());
			LinkedHashMultimap<String, Property> mm = LinkedHashMultimap.create();
			mm.put("textures", new Property("textures", textureValue));
			GameProfile profile = new GameProfile(uuid, "head", new PropertyMap(mm));
			ItemStack head = new ItemStack(Items.PLAYER_HEAD);
			head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
			SKULL_CACHE.put(textureValue, head);
			return head;
		} catch (Exception e) {
			return null;
		}
	}
}
