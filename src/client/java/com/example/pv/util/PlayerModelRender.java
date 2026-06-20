package com.example.pv.util;

import com.google.common.collect.LinkedHashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerModelRender {

	private static RemotePlayer cached;
	private static String cachedUuid;

	private static RemotePlayer getPlayer(String uuidStr, String name,
	                                       String skinValue, String skinSignature) {
		if (uuidStr == null || uuidStr.isEmpty()) return null;
		if (cached != null && uuidStr.equals(cachedUuid)) return cached;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) return null;
		UUID uuid;
		try {
			uuid = uuidStr.contains("-") ? UUID.fromString(uuidStr) : fromTrimmed(uuidStr);
		} catch (Exception e) {
			return null;
		}

		GameProfile profile;
		if (skinValue != null && !skinValue.isEmpty()) {
			Property prop = (skinSignature != null && !skinSignature.isEmpty())
					? new Property("textures", skinValue, skinSignature)
					: new Property("textures", skinValue);
			LinkedHashMultimap<String, Property> mm = LinkedHashMultimap.create();
			mm.put("textures", prop);
			PropertyMap pm = new PropertyMap(mm);
			profile = new GameProfile(uuid, name, pm);
		} else {
			profile = new GameProfile(uuid, name);
		}

		final Supplier<PlayerSkin> lookup = mc.getSkinManager().createLookup(profile, false);
		cached = new RemotePlayer(level, profile) {
			@Override
			public PlayerSkin getSkin() {
				return lookup.get();
			}
		};
		cachedUuid = uuidStr;
		return cached;
	}

	private static UUID fromTrimmed(String s) {
		String d = s.replaceFirst(
				"(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
		return UUID.fromString(d);
	}

	public static void render(GuiGraphics g, int x1, int y1, int x2, int y2,
	                          int scale, float mouseX, float mouseY,
	                          String uuid, String name, String skinValue, String skinSignature) {
		RemotePlayer p = getPlayer(uuid, name, skinValue, skinSignature);
		if (p == null) return;
		InventoryScreen.renderEntityInInventoryFollowsMouse(
				g, x1, y1, x2, y2, scale, 0.0625f, mouseX, mouseY, p);
	}
}
