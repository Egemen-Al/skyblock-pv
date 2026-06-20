/*
 * Copyright (C) 2022-2024 NotEnoughUpdates contributors
 * Copyright (C) 2026 Skyblock ProfileViewer port contributors
 * Derivative work under GNU LGPL-3.0-or-later.
 */
package com.example.pv;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import com.example.pv.util.PlayerNameCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.pv.gui.GuiProfileViewer;

public class SkyblockPvClient implements ClientModInitializer {

	public static final String MOD_ID = "skyblockpv";
	public static final Logger LOGGER = LoggerFactory.getLogger("SkyblockPV");

	private static volatile String pendingUsername = null;
	private static int tickCounter = 0;

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
				ClientCommandManager.literal("pve")
					.executes(ctx -> {
						openProfileViewer(Minecraft.getInstance().getUser().getName());
						return 1;
					})
					.then(ClientCommandManager.argument("username", StringArgumentType.word())
						.suggests((ctx, builder) -> {
							String rem = builder.getRemaining().toLowerCase();
							for (String n : PlayerNameCache.names()) {
								if (rem.isEmpty() || n.toLowerCase().startsWith(rem)) builder.suggest(n);
							}
							return builder.buildFuture();
						})
						.executes(ctx -> {
							openProfileViewer(StringArgumentType.getString(ctx, "username"));
							return 1;
						}))
			);

			dispatcher.register(
				ClientCommandManager.literal("users")
					.executes(ctx -> { fetchUsers(); return 1; })
			);
		});

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.level != null && (++tickCounter % 40 == 0)) {
				PlayerNameCache.refreshOnline();
			}
		});

		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
			if (overlay) return message;
			try {
				recordNames(message.getString());
				return makeNamesClickable(message);
			} catch (Throwable t) {
				return message;
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (pendingUsername != null && !(mc.screen instanceof ChatScreen)) {
				String name = pendingUsername;
				pendingUsername = null;
				try {
					mc.setScreen(new GuiProfileViewer(name));
					LOGGER.info("[PV] screen opened. mc.screen={}", mc.screen);
				} catch (Throwable t) {
					LOGGER.error("[PV] Failed to open ProfileViewer screen", t);
				}
			}
		});

		LOGGER.info("Skyblock ProfileViewer initialized. Use /pve <username>.");
	}

	private static Component makeNamesClickable(Component c) {
		if (PlayerNameCache.names().isEmpty()) return c;
		return rebuild(c, 0);
	}

	private static Component rebuild(Component c, int depth) {
		if (depth > 12) return c;
		MutableComponent base = MutableComponent.create(c.getContents());
		Style style = c.getStyle();
		String own = MutableComponent.create(c.getContents()).getString();
		String stripped = own.replaceAll("(?i)§[0-9a-fk-or]", "").trim();
		String matched = matchName(stripped);
		if (matched != null && style.getClickEvent() == null) {
			style = style
				.withClickEvent(new ClickEvent.RunCommand("/pve " + matched))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("§eClick to view " + matched + "'s profile")));
		}
		base.setStyle(style);
		for (Component sib : c.getSiblings()) base.append(rebuild(sib, depth + 1));
		return base;
	}

	private static final java.util.regex.Pattern CHAT_NAME =
		java.util.regex.Pattern.compile("(?:\\[[^\\]]+\\]\\s*)*([A-Za-z0-9_]{2,16})\\s*[:>]");

	private static void recordNames(String plain) {
		java.util.regex.Matcher m = CHAT_NAME.matcher(plain.replaceAll("(?i)§[0-9a-fk-or]", ""));
		while (m.find()) {
			PlayerNameCache.add(m.group(1));
		}
	}

	private static String matchName(String text) {
		if (text.isEmpty()) return null;
		for (String n : PlayerNameCache.names()) {
			if (text.equals(n)) return n;
			int i = text.indexOf(n);
			if (i >= 0) {
				char b = i > 0 ? text.charAt(i - 1) : ' ';
				int e = i + n.length();
				char a = e < text.length() ? text.charAt(e) : ' ';
				if (!Character.isLetterOrDigit(b) && b != '_' && !Character.isLetterOrDigit(a) && a != '_') return n;
			}
		}
		return null;
	}

	public static void openProfileViewer(String username) {
		LOGGER.info("[PV] queued ProfileViewer for: {}", username);
		pendingUsername = username;
	}

	private static void fetchUsers() {
		java.util.concurrent.CompletableFuture.supplyAsync(() -> {
			try { return com.example.pv.api.BackendApi.getUsers(); }
			catch (Exception e) { return null; }
		}).thenAccept(json -> Minecraft.getInstance().execute(() -> {
			net.minecraft.client.gui.components.ChatComponent chat = Minecraft.getInstance().gui.getChat();
			if (json == null || !json.has("success") || !json.get("success").getAsBoolean()) {
				chat.addMessage(Component.literal("§c[PV] /users failed (is LOG_TABLE set?)"));
				return;
			}
			int total = json.has("total_requests") ? json.get("total_requests").getAsInt() : 0;
			int uniq = json.has("unique_users") ? json.get("unique_users").getAsInt() : 0;
			chat.addMessage(Component.literal("§6[PV] §eTotal requests: §f" + total + " §7| §eUnique users: §f" + uniq));
			if (json.has("users") && json.get("users").isJsonArray()) {
				com.google.gson.JsonArray arr = json.getAsJsonArray("users");
				int n = Math.min(15, arr.size());
				for (int i = 0; i < n; i++) {
					JsonObject u = arr.get(i).getAsJsonObject();
					String name = u.has("requester") ? u.get("requester").getAsString() : "?";
					int count = u.has("count") ? u.get("count").getAsInt() : 0;
					String last = (u.has("lastTarget") && !u.get("lastTarget").isJsonNull()) ? u.get("lastTarget").getAsString() : "-";
					chat.addMessage(Component.literal("§7" + (i + 1) + ". §b" + name + " §7- §f" + count + " §7requests (last: " + last + ")"));
				}
			}
		}));
	}
}
