package com.eggman.pv.gui;

import com.eggman.pv.api.ProfileFetcher;
import com.eggman.pv.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GuiProfileViewer extends Screen {

	public static final int SIZE_X = 431;
	public static final int SIZE_Y = 202;

	private static final ResourceLocation PV_BG = PvRender.tex("pv_bg.png");

	private final String username;

	private int guiLeft;
	private int guiTop;

	private String status = "Loading...";
	private boolean loading = true;
	private JsonObject data;

	private static final java.util.Map<String, CacheEntry> PROFILE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static final long CACHE_TTL_MS = 3 * 60 * 1000L;
	private static final class CacheEntry {
		final JsonObject data; final long time;
		CacheEntry(JsonObject d) { this.data = d; this.time = System.currentTimeMillis(); }
	}

	private int selectedProfileIndex = 0;
	private boolean profileDropdownOpen = false;

	public enum Page {
		BASIC("Skills", 0x55FF55),
		DUNGEON("Dungeons", 0x55FFFF),
		INVENTORY("Storage", 0xFFAA00),
		COLLECTIONS("Collections", 0xFFAA00),
		PETS("Pets", 0xFF55FF),
		BESTIARY("Bestiary", 0xFF5555),
		MINING("Mining", 0xAA00AA),
		TROPHY("Trophy Fishing", 0x55FFFF),
		CRIMSON("Crimson Isle", 0xFF5555),
		RIFT("Rift", 0xAA00AA);

		public final String title;
		public final int color;
		Page(String title, int color) { this.title = title; this.color = color; }
	}

	private Page currentPage = Page.BASIC;

	private int hoverMouseX = 0;
	private int hoverMouseY = 0;
	private String hoverText = null;

	private boolean dungeonMasterMode = false;

	private String cataTargetInput = "";
	private boolean cataInputFocused = false;
	private String cataCalcResult = null;

	private String searchInput = "";
	private boolean searchFocused = false;

	private int storageType = 0;
	private int storagePage = 0;
	private int storageCacheProfile = -1;
	private List<ItemData> sInv, sEnder, sTalisman, sVault, sWardrobe, sFishing, sPotion, sArmor, sEquip;
	private final List<List<ItemData>> sBackpacks = new ArrayList<>();

	private final List<int[]> storageBtnRects = new ArrayList<>();

	private int colCatIndex = 0;
	private int colPage = 0;
	private final List<int[]> colBtnRects = new ArrayList<>();

	private int miningScroll = 0;
	private final List<int[]> miningBtnRects = new ArrayList<>();

	private int bestiaryIsland = 0;
	private final List<int[]> bestiaryBtnRects = new ArrayList<>();

	private int petSelected = -1;
	private int petCacheProfile = -1;
	private int petPage = 0;
	private int petSort = 0;
	private final List<int[]> petBtnRects = new ArrayList<>();

	private int riftStorageType = 0;
	private int riftPage = 0;
	private int riftCacheProfile = -1;
	private List<ItemData> rInv, rEnder, rArmor, rEquip;
	private final List<int[]> riftBtnRects = new ArrayList<>();

	public GuiProfileViewer(String username) {
		super(Component.literal("ProfileViewer"));
		this.username = username;
		com.eggman.pv.util.RepoManager.init();
	}

	@Override
	protected void init() {
		this.guiLeft = (this.width - SIZE_X) / 2;
		this.guiTop = (this.height - SIZE_Y) / 2;

		this.status = "Loading " + username + "...";
		this.loading = true;
		this.data = null;
		this.selectedProfileIndex = 0;
		this.profileDropdownOpen = false;

		String cacheKey = username.toLowerCase();
		CacheEntry cached = PROFILE_CACHE.get(cacheKey);
		if (cached != null && System.currentTimeMillis() - cached.time < CACHE_TTL_MS) {
			this.data = cached.data;
			this.loading = false;
			this.selectedProfileIndex = getBestProfileIndex(cached.data);
			this.status = "";
			return;
		}

		String requester;
		try { requester = Minecraft.getInstance().getUser().getName(); } catch (Throwable t) { requester = "?"; }
		ProfileFetcher.fetchAsync(username, requester).thenAccept(json -> {
			Minecraft.getInstance().execute(() -> {
				this.loading = false;

				if (json == null || !json.has("success") || !json.get("success").getAsBoolean()) {
					this.status = "Failed: " + getString(json, "error", "Unknown error");
					return;
				}

				this.data = json;
				PROFILE_CACHE.put(cacheKey, new CacheEntry(json));
				this.selectedProfileIndex = getBestProfileIndex(json);
				this.status = "";
			});
		}).exceptionally(error -> {
			Minecraft.getInstance().execute(() -> {
				this.loading = false;
				Throwable cause = error.getCause() != null ? error.getCause() : error;
				this.status = "Failed: " + cause.getMessage();
			});

			error.printStackTrace();
			return null;
		});
	}

	@Override
	protected void renderBlurredBackground(GuiGraphics g) {

	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.hoverMouseX = mouseX;
		this.hoverMouseY = mouseY;
		this.hoverText = null;

		this.renderBackground(g, mouseX, mouseY, partialTick);

		PvRender.drawTexturedRect(
				g,
				PV_BG,
				guiLeft,
				guiTop,
				SIZE_X,
				SIZE_Y,
				SIZE_X,
				SIZE_Y
		);

		if (loading || data == null) {
			PvRender.drawStringCentered(
					g,
					status,
					guiLeft + SIZE_X / 2f,
					guiTop + SIZE_Y / 2f,
					true,
					0xFFFFFF
			);
		} else {
			renderTabs(g);
			switch (currentPage) {
				case BASIC: renderBasicPage(g); break;
				case DUNGEON: renderDungeonPage(g); break;
				case INVENTORY: renderStoragePage(g); break;
				case COLLECTIONS: renderCollectionsPage(g); break;
				case PETS: renderPetsPage(g); break;
				case BESTIARY: renderBestiaryPage(g); break;
				case MINING: renderMiningPage(g); break;
				case TROPHY: renderTrophyPage(g); break;
				case CRIMSON: renderCrimsonPage(g); break;
				case RIFT: renderRiftPage(g); break;
			}
			renderProfileDropdown(g);
			drawSearchBox(g);

			if (hoverText != null) {
				drawTooltip(g, hoverText, hoverMouseX, hoverMouseY);
			}
		}

		super.render(g, mouseX, mouseY, partialTick);
	}

	private int[] searchBoxRect() {
		return new int[]{guiLeft + SIZE_X - 130, guiTop + SIZE_Y + 6, 122, 17};
	}

	private void drawSearchBox(GuiGraphics g) {
		int[] r = searchBoxRect();
		g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], searchFocused ? 0xFF333355 : 0xFF1C1C1C);
		g.fill(r[0], r[1], r[0] + r[2], r[1] + 1, 0xFF8888FF);
		if (searchInput.isEmpty()) {
			PvRender.drawString(g, searchFocused ? "_" : "Search player...", r[0] + 5, r[1] + 5, searchFocused ? 0xFFFFFF : 0x808080, false);
		} else {
			PvRender.drawString(g, searchInput, r[0] + 5, r[1] + 5, 0xFFFFFF, false);

			String comp = com.eggman.pv.util.PlayerNameCache.complete(searchInput);
			if (comp != null) {
				int w = PvRender.font().width(searchInput);
				PvRender.drawString(g, "§8" + comp.substring(searchInput.length()), r[0] + 5 + w, r[1] + 5, 0x808080, false);
			}
		}
	}

	private void drawTooltip(GuiGraphics g, String text, int mx, int my) {
		String[] lines = text.split("\n");
		int w = 0;
		for (String l : lines) w = Math.max(w, PvRender.font().width(l));
		int lh = 10;
		int h = lines.length * lh;
		int pad = 4;
		int x = mx + 10;
		int y = my - 12;

		if (x + w + pad + 2 > this.width) x = mx - w - pad - 8;
		if (x < 2) x = 2;
		if (y + h + pad + 2 > this.height) y = this.height - h - pad - 2;
		if (y < 2) y = 2;

		int bg = 0xEE191A20, border = 0xFF313440;
		g.fill(x - pad - 2, y - pad - 2, x + w + pad + 2, y + h + pad, border);
		g.fill(x - pad, y - pad, x + w + pad, y + h + pad - 2, bg);
		for (int i = 0; i < lines.length; i++) {
			PvRender.drawString(g, lines[i], x, y + i * lh, 0xFFFFFF, false);
		}
	}

	private void checkHover(int x, int y, int w, int h, String text) {
		if (hoverMouseX >= x && hoverMouseX <= x + w && hoverMouseY >= y && hoverMouseY <= y + h) {
			hoverText = text;
		}
	}

	private void renderTabs(GuiGraphics g) {
		Page[] pages = Page.values();
		int tabW = 28;
		int tabH = 22;
		int startX = guiLeft + 5;
		int tabY = guiTop - tabH + 4;

		for (int i = 0; i < pages.length; i++) {
			Page p = pages[i];
			int tx = startX + i * (tabW + 1);
			boolean active = (p == currentPage);

			int bg = active ? 0xFF2A2A2A : 0xFF1A1A1A;
			g.fill(tx, tabY, tx + tabW, tabY + tabH, bg);

			g.fill(tx, tabY, tx + tabW, tabY + 1, active ? p.color | 0xFF000000 : 0xFF000000);

			int iconX = tx + tabW / 2 - 8;
			int iconY = tabY + tabH / 2 - 8;
			g.renderItem(tabIcon(p), iconX, iconY);

			if (hoverMouseX >= tx && hoverMouseX <= tx + tabW && hoverMouseY >= tabY && hoverMouseY <= tabY + tabH) {
				hoverText = p.title;
			}
		}
	}

	private ItemStack tabIcon(Page p) {
		switch (p) {
			case BASIC:       return new ItemStack(Items.PAPER);
			case DUNGEON:     return new ItemStack(Items.DEAD_BUSH);
			case INVENTORY:   return new ItemStack(Items.ENDER_CHEST);
			case COLLECTIONS: return new ItemStack(Items.PAINTING);
			case PETS:        return new ItemStack(Items.BONE);
			case BESTIARY:    return new ItemStack(Items.IRON_SWORD);
			case MINING:      return new ItemStack(Items.DIAMOND_PICKAXE);
			case TROPHY:      return new ItemStack(Items.FISHING_ROD);
			case CRIMSON:     return new ItemStack(Items.NETHERRACK);
			case RIFT:        return new ItemStack(Items.ENDER_EYE);
			default:          return new ItemStack(Items.BARRIER);
		}
	}

	private void renderDungeonPage(GuiGraphics g) {
		JsonObject selected = getSelectedProfile();
		if (selected == null || !selected.has("dungeons")) {
			PvRender.drawStringCentered(g, "No dungeon data", guiLeft + SIZE_X / 2f, guiTop + SIZE_Y / 2f, true, 0xFF5555);
			return;
		}
		JsonObject d = selected.getAsJsonObject("dungeons");
		int x = guiLeft;
		int y = guiTop;

		int btnY = y + 22;
		drawModeButton(g, x + 8, btnY, 62, "Normal", !dungeonMasterMode, 0x55FF55);
		drawModeButton(g, x + 74, btnY, 62, "Master", dungeonMasterMode, 0xFF5555);

		double cataXp = getDouble(d, "catacombs_xp", 0);
		com.eggman.pv.util.SkillXp.Level cl =
			com.eggman.pv.util.SkillXp.getLevel(cataXp, com.eggman.pv.util.SkillXp.Type.CATACOMBS, com.eggman.pv.util.SkillXp.CATACOMBS_OVERFLOW_MAX);

		int lx = x + 8;
		int ly = y + 38;

		g.fill(lx, ly, lx + 130, ly + 50, 0x66000000);
		PvRender.drawString(g, "\u2694 Catacombs", lx + 4, ly + 4, 0xFF5555, true);
		PvRender.drawString(g, String.valueOf(cl.level), lx + 110, ly + 4, 0xFFFFFF, true);
		int cbX = lx + 4, cbY = ly + 14, cbW = 122, cbH = 4;
		g.fill(cbX, cbY, cbX + cbW, cbY + cbH, 0xFF2A002A);
		g.fill(cbX, cbY, cbX + (int)(cbW * cl.progress), cbY + cbH, 0xFFFF5555);
		checkHover(cbX, cbY - 12, cbW, 16, "Catacombs XP: " + formatNumber(cataXp));

		int inX = lx + 4, inY = ly + 24, inW = 30, inH = 14;
		g.fill(inX, inY, inX + inW, inY + inH, cataInputFocused ? 0xFF333355 : 0xFF222222);
		g.fill(inX, inY, inX + inW, inY + 1, 0xFF8888FF);
		String shownInput = cataTargetInput.isEmpty() ? (cataInputFocused ? "_" : "lvl") : cataTargetInput;
		PvRender.drawString(g, shownInput, inX + 4, inY + 4, 0xFFFFFF, false);

		int calcX = inX + inW + 4, calcW = 56;
		g.fill(calcX, inY, calcX + calcW, inY + inH, 0xFF3A5A3A);
		g.fill(calcX, inY, calcX + calcW, inY + 1, 0xFF55FF55);
		PvRender.drawString(g, "Calculate", calcX + 4, inY + 4, 0x55FF55, false);

		if (cataCalcResult != null) {
			PvRender.drawString(g, cataCalcResult, lx + 4, ly + 41, 0xFFFF55, true);
		} else {
			PvRender.drawString(g, "Until " + (cl.level+1) + ": " + formatNumber(com.eggman.pv.util.SkillXp.xpForLevel(cl.level+1, com.eggman.pv.util.SkillXp.Type.CATACOMBS) - cataXp), lx + 4, ly + 41, 0xAAAAAA, true);
		}

		int ly2 = ly + 56;
		g.fill(lx, ly2, lx + 130, ly2 + 46, 0x66000000);
		int runs = (int) getDouble(d, "total_runs", 0);
		int runsM = (int) getDouble(d, "total_runs_master", 0);
		double secrets = getDouble(d, "secrets_found", 0);

		double secretsPerRun = getDouble(d, "secrets_per_run", -1);
		if (secretsPerRun < 0) {
			int allRuns = runs + runsM;
			secretsPerRun = secrets / Math.max(1, allRuns);
		}
		drawKV(g, lx + 4, ly2 + 4, "Total Runs", String.valueOf(runs));
		drawKV(g, lx + 4, ly2 + 14, "Total Runs M", String.valueOf(runsM));
		drawKV(g, lx + 4, ly2 + 24, "Secrets (Total)", formatNumber(secrets));
		drawKV(g, lx + 4, ly2 + 34, "Secrets (/Run)", String.format("%.2f", secretsPerRun));

		int mx = x + 150;
		int my = y + 38;
		g.fill(mx, my, mx + 120, my + 150, 0x66000000);
		PvRender.drawStringCentered(g, "Boss Collections", mx + 60, my + 6, true, 0xFFAA00);
		String[] bosses = {"Bonzo","Scarf","Professor","Thorn","Livid","Sadan","Necron"};
		JsonObject bc = d.has("boss_collections") ? d.getAsJsonObject("boss_collections") : null;
		for (int i = 0; i < bosses.length; i++) {
			String key = (dungeonMasterMode ? "m" : "f") + (i + 1);
			int val = bc != null ? (int) getDouble(bc, key, 0) : 0;
			int by = my + 22 + i * 18;
			String tier = (dungeonMasterMode ? "M" : "F") + (i + 1);

			net.minecraft.resources.ResourceLocation head = com.eggman.pv.util.BossHeads.getFloor(i + 1);
			if (head != null) {
				try {
					PvRender.drawTexturedRect(g, head, mx + 6, by - 2, 12, 12, 12, 12);
				} catch (Throwable ignored) {}
			}

			PvRender.drawString(g, bosses[i] + " (" + tier + ")", mx + 22, by, 0xFFFFFF, true);
			PvRender.drawString(g, String.valueOf(val), mx + 105, by, 0xFFFF55, true);
		}

		int rx = x + 288;
		int ry = y + 38;
		g.fill(rx, ry, rx + 135, ry + 150, 0x66000000);
		PvRender.drawStringCentered(g, "Class Levels", rx + 67, ry + 6, true, 0x55FFFF);
		JsonObject classes = d.has("classes") ? d.getAsJsonObject("classes") : null;
		String[] classNames = {"healer","mage","berserk","archer","tank"};
		String[] classLabels = {"Healer","Mage","Berserk","Archer","Tank"};
		int[] classColors = {0xFF55FF, 0x55FFFF, 0xFFAA00, 0xFF5555, 0x55FF55};
		double avgLevel = 0;
		for (int i = 0; i < classNames.length; i++) {
			double cxp = classes != null ? getDouble(classes, classNames[i], 0) : 0;
			com.eggman.pv.util.SkillXp.Level lvl =
				com.eggman.pv.util.SkillXp.getLevel(cxp, com.eggman.pv.util.SkillXp.Type.CATACOMBS, 50);
			avgLevel += lvl.level;
			int cy = ry + 24 + i * 22;
			PvRender.drawString(g, classLabels[i], rx + 6, cy, classColors[i], true);
			PvRender.drawString(g, String.valueOf(lvl.level), rx + 118, cy, 0xFFFFFF, true);
			int bX = rx + 6, bY = cy + 10, bW = 124, bH = 4;
			g.fill(bX, bY, bX + bW, bY + bH, 0xFF2A002A);
			g.fill(bX, bY, bX + (int)(bW * lvl.progress), bY + bH, 0xFF000000 | classColors[i]);
			String remLine;
			if (lvl.level >= 50) {
				remLine = "Maxed!";
			} else {
				double next = com.eggman.pv.util.SkillXp.xpForLevel(lvl.level + 1, com.eggman.pv.util.SkillXp.Type.CATACOMBS);
				remLine = "Next " + (lvl.level + 1) + ": " + formatNumber(Math.max(0, next - cxp)) + " XP";
			}
			checkHover(bX, bY - 12, bW, 16,
					classLabels[i] + " XP: " + formatNumber(cxp) + "\n" + remLine);
		}
		avgLevel /= classNames.length;
		PvRender.drawString(g, "Class Average: " + String.format("%.1f", avgLevel), rx + 6, ry + 138, 0xAAAAAA, true);
	}

	private void drawModeButton(GuiGraphics g, int x, int y, int w, String label, boolean active, int color) {
		g.fill(x, y, x + w, y + 15, active ? 0xFF2A2A2A : 0xFF161616);

		if (active) g.fill(x, y - 3, x + w, y - 1, 0xFF000000 | color);
		PvRender.drawStringCentered(g, label, x + w / 2f, y + 5, true, active ? color : 0xAAAAAA);
	}

	private String formatTime(double ms) {
		long totalSec = (long) (ms / 1000.0);
		long m = totalSec / 60;
		long s = totalSec % 60;
		return m + ":" + (s < 10 ? "0" + s : s);
	}

	private void doCataCalculate() {
		cataCalcResult = null;
		if (cataTargetInput.isEmpty()) return;
		try {
			int target = Integer.parseInt(cataTargetInput);
			if (target < 1 || target > com.eggman.pv.util.SkillXp.CATACOMBS_OVERFLOW_MAX) { cataCalcResult = "Enter 1-200"; return; }
			JsonObject selected = getSelectedProfile();
			JsonObject d = selected.getAsJsonObject("dungeons");
			double cataXp = getDouble(d, "catacombs_xp", 0);
			double needed = com.eggman.pv.util.SkillXp.xpForLevel(target, com.eggman.pv.util.SkillXp.Type.CATACOMBS);
			double remaining = needed - cataXp;
			if (remaining <= 0) {
				cataCalcResult = "Cata " + target + " completed!";
			} else {
				cataCalcResult = "Cata " + target + "'e: " + formatNumber(remaining) + " XP";
			}
		} catch (NumberFormatException e) {
			cataCalcResult = "Invalid number";
		}
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent characterEvent) {
		if (cataInputFocused) {
			char chr = characterEvent.codepoint() != 0 ? (char) characterEvent.codepoint() : 0;
			if (Character.isDigit(chr) && cataTargetInput.length() < 3) {
				cataTargetInput += chr;
				return true;
			}
		}
		if (searchFocused) {
			char chr = characterEvent.codepoint() != 0 ? (char) characterEvent.codepoint() : 0;
			if ((Character.isLetterOrDigit(chr) || chr == '_') && searchInput.length() < 16) {
				searchInput += chr;
				return true;
			}
		}
		return super.charTyped(characterEvent);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
		if (cataInputFocused) {
			int key = keyEvent.key();
			if (key == 259 && !cataTargetInput.isEmpty()) {
				cataTargetInput = cataTargetInput.substring(0, cataTargetInput.length() - 1);
				return true;
			}
			if (key == 257 || key == 335) {
				doCataCalculate();
				return true;
			}
		}
		if (searchFocused) {
			int key = keyEvent.key();
			if (keyEvent.hasControlDown()) {
				if (key == 86) {
					String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
					if (clip != null) {
						searchInput += clip.replaceAll("[^A-Za-z0-9_]", "");
						if (searchInput.length() > 16) searchInput = searchInput.substring(0, 16);
					}
					return true;
				}
				if (key == 67) {
					Minecraft.getInstance().keyboardHandler.setClipboard(searchInput);
					return true;
				}
				if (key == 65) {
					searchInput = "";
					return true;
				}
			}
			if (key == 258) {
				String comp = com.eggman.pv.util.PlayerNameCache.complete(searchInput);
				if (comp != null) searchInput = comp;
				return true;
			}
			if (key == 259 && !searchInput.isEmpty()) {
				searchInput = searchInput.substring(0, searchInput.length() - 1);
				return true;
			}
			if ((key == 257 || key == 335) && !searchInput.isEmpty()) {
				Minecraft.getInstance().setScreen(new GuiProfileViewer(searchInput.trim()));
				return true;
			}
		}
		return super.keyPressed(keyEvent);
	}

	private void drawKV(GuiGraphics g, int x, int y, String key, String val) {
		PvRender.drawString(g, key, x, y, 0xAAAAAA, true);
		PvRender.drawString(g, val, x + 92, y, 0xFFFFFF, true);
	}

	private static final String[] ROMANS = {
		"I","II","III","IV","V","VI","VII","VIII","IX","X",
		"XI","XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX"
	};
	private static final String[] COL_CATS = {"FARMING","MINING","COMBAT","FORAGING","FISHING","RIFT"};
	private static final String[] COL_CAT_LABELS = {"Farming","Mining","Combat","Foraging","Fishing","Rift"};

	private ItemStack colCatIcon(String cat) {
		switch (cat) {
			case "FARMING":  return new ItemStack(Items.GOLDEN_HOE);
			case "MINING":   return new ItemStack(Items.IRON_PICKAXE);
			case "COMBAT":   return new ItemStack(Items.IRON_SWORD);
			case "FORAGING": return new ItemStack(Items.OAK_SAPLING);
			case "FISHING":  return new ItemStack(Items.FISHING_ROD);
			case "RIFT":     return new ItemStack(Items.MYCELIUM);
			default:         return new ItemStack(Items.BARRIER);
		}
	}

	private void renderCollectionsPage(GuiGraphics g) {
		colBtnRects.clear();
		int guiL = guiLeft, guiT = guiTop;
		JsonObject sel = getSelectedProfile();
		JsonObject colData = (sel != null && sel.has("collections") && sel.get("collections").isJsonObject())
				? sel.getAsJsonObject("collections") : null;
		if (colData == null) {
			PvRender.drawStringCentered(g, "§cCollection API disabled", guiL + SIZE_X / 2f, guiT + SIZE_Y / 2f, true, 0);
			return;
		}

		List<String> cats = new ArrayList<>();
		for (String c : COL_CATS) if (colData.has(c) && colData.get(c).isJsonArray()) cats.add(c);
		if (cats.isEmpty()) {
			PvRender.drawStringCentered(g, "§cNo collection data", guiL + SIZE_X / 2f, guiT + SIZE_Y / 2f, true, 0);
			return;
		}
		if (colCatIndex >= cats.size()) colCatIndex = 0;

		int stepY = (SIZE_Y - 24) / cats.size();
		for (int i = 0; i < cats.size(); i++) {
			int sx = guiL + 8;
			int sy = guiT + 16 + i * stepY;
			boolean active = i == colCatIndex;
			g.fill(sx - 2, sy - 2, sx + 18, sy + 18, active ? 0xFFAAAAAA : 0xFF373737);
			g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF8B8B8B);
			g.renderItem(colCatIcon(cats.get(i)), sx, sy);
			colBtnRects.add(new int[]{sx, sy, 18, 18, i});
			if (hoverMouseX >= sx && hoverMouseX <= sx + 16 && hoverMouseY >= sy && hoverMouseY <= sy + 16) {
				hoverText = COL_CAT_LABELS[indexOfCat(cats.get(i))];
			}
		}

		int divX = guiL + 270;
		g.fill(divX, guiT + 14, divX + 1, guiT + SIZE_Y - 10, 0x33FFFFFF);

		String catLabel = COL_CAT_LABELS[indexOfCat(cats.get(colCatIndex))];
		renderMinions(g, cats.get(colCatIndex), catLabel);

		PvRender.drawStringCentered(g, catLabel + " Collections", guiL + 150, guiT + 14, true, 0xFFAA00);

		JsonArray list = colData.getAsJsonArray(cats.get(colCatIndex));
		int perPage = 20, cols = 5;
		int maxPage = Math.max(0, (list.size() - 1) / perPage);
		if (colPage > maxPage) colPage = maxPage;
		if (colPage < 0) colPage = 0;

		int slot = 16;
		int startX = guiL + 42, stepX = 44, startY = guiT + 36, gStepY = 38;
		for (int k = colPage * perPage, j = 0; k < Math.min((colPage + 1) * perPage, list.size()); k++, j++) {
			JsonObject c = list.get(k).getAsJsonObject();
			String id = getString(c, "id", "");
			String name = getString(c, "name", id);
			double amount = getDouble(c, "amount", 0);
			int tier = (int) getDouble(c, "tier", 0);
			int maxTier = (int) getDouble(c, "maxTier", 1);

			int col = j % cols, row = j / cols;
			int x = startX + col * stepX;
			int y = startY + row * gStepY;

			float completeness = maxTier > 0 ? Math.min(1f, tier / (float) maxTier) : 0f;
			boolean maxed = tier >= maxTier && maxTier > 0;

			g.fill(x, y, x + slot, y + slot, 0xFF373737);
			int fillH = (int) (slot * completeness);
			g.fill(x, y + slot - fillH, x + slot, y + slot, maxed ? 0xFFFFAA00 : 0x88FFAA00);

			com.eggman.pv.util.CollectionIcon.draw(g, id, x, y);

			String tierStr = (tier > 0 && tier <= 20) ? ROMANS[tier - 1] : String.valueOf(tier);
			PvRender.drawStringCentered(g, tierStr, x + slot / 2f, y - 8, true, maxed ? 0xFFD700 : 0xAAAAAA);
			PvRender.drawStringCentered(g, formatNumber(amount), x + slot / 2f, y + slot + 2, true, 0xAAAAAA);

			if (hoverMouseX >= x && hoverMouseX <= x + slot && hoverMouseY >= y && hoverMouseY <= y + slot) {
				hoverText = (maxed ? "§6" : "§e") + name + " " + tierStr
						+ "\n§7Total: §f" + formatNumber(amount)
						+ "\n§7Tier: §f" + tier + "§7/§f" + maxTier;
			}
		}

		if (maxPage > 0) {
			int ay = guiT + SIZE_Y - 14;
			if (colPage > 0) {
				drawPageArrow(g, guiL + 150 - 14, ay, "<");
				colBtnRects.add(new int[]{guiL + 150 - 14, ay, 12, 14, 200});
			}
			if (colPage < maxPage) {
				drawPageArrow(g, guiL + 150 + 2, ay, ">");
				colBtnRects.add(new int[]{guiL + 150 + 2, ay, 12, 14, 201});
			}
			PvRender.drawStringCentered(g, "Page " + (colPage + 1) + "/" + (maxPage + 1), guiL + 150, ay - 9, false, 0x909090);
		}
	}

	private int indexOfCat(String cat) {
		for (int i = 0; i < COL_CATS.length; i++) if (COL_CATS[i].equals(cat)) return i;
		return 0;
	}

	private static final java.util.Map<String, String[]> MINIONS = new java.util.HashMap<>();
	static {
		MINIONS.put("FARMING", new String[]{"WHEAT","CARROT","POTATO","PUMPKIN","MELON","MUSHROOM","COCOA","CACTUS","SUGAR_CANE","CHICKEN","COW","PIG","SHEEP","RABBIT","NETHER_WARTS"});
		MINIONS.put("MINING", new String[]{"COBBLESTONE","COAL","IRON","GOLD","DIAMOND","LAPIS","EMERALD","REDSTONE","QUARTZ","OBSIDIAN","GLOWSTONE","GRAVEL","ICE","SAND","ENDER_STONE","SNOW","MITHRIL","HARD_STONE","MYCELIUM","RED_SAND"});
		MINIONS.put("COMBAT", new String[]{"ZOMBIE","SKELETON","SPIDER","CAVESPIDER","CREEPER","ENDERMAN","GHAST","SLIME","BLAZE","MAGMA_CUBE","REVENANT","TARANTULA","VOIDLING","INFERNO"});
		MINIONS.put("FORAGING", new String[]{"OAK","SPRUCE","BIRCH","DARK_OAK","ACACIA","JUNGLE","FLOWER"});
		MINIONS.put("FISHING", new String[]{"FISHING","CLAY"});
		MINIONS.put("RIFT", new String[]{"VAMPIRE"});
	}

	private java.util.Map<String, Integer> craftedMinions() {
		java.util.Map<String, Integer> out = new java.util.HashMap<>();
		JsonObject sel = getSelectedProfile();
		if (sel == null || !sel.has("crafted_generators") || !sel.get("crafted_generators").isJsonArray()) return out;
		JsonArray arr = sel.getAsJsonArray("crafted_generators");
		for (int i = 0; i < arr.size(); i++) {
			String s = arr.get(i).getAsString();
			int u = s.lastIndexOf('_');
			if (u <= 0) continue;
			try {
				String type = s.substring(0, u);
				int tier = Integer.parseInt(s.substring(u + 1));
				out.merge(type, tier, Math::max);
			} catch (NumberFormatException ignored) {}
		}
		return out;
	}

	private void renderMinions(GuiGraphics g, String cat, String label) {
		String[] mins = MINIONS.get(cat);
		PvRender.drawStringCentered(g, label + " Minions", guiLeft + 352, guiTop + 14, true, 0xFFAA00);
		if (mins == null) return;
		java.util.Map<String, Integer> crafted = craftedMinions();

		int cols = 4, slot = 16, startX = guiLeft + 288, stepX = 34, startY = guiTop + 32, stepY = 32;
		for (int i = 0; i < mins.length; i++) {
			String type = mins[i];
			int tier = crafted.getOrDefault(type, 0);
			String internal = type + "_GENERATOR_" + (tier == 0 ? 1 : tier);
			int col = i % cols, row = i / cols;
			int x = startX + col * stepX, y = startY + row * stepY;
			if (y + slot > guiTop + SIZE_Y - 6) break;

			boolean maxed = tier >= 11;
			g.fill(x - 1, y - 1, x + slot + 1, y + slot + 1, maxed ? 0xFFFFAA00 : 0xFF555555);
			g.fill(x, y, x + slot, y + slot, 0xFF202020);
			String skull = com.eggman.pv.util.RepoItems.skull(internal);
			if (skull != null) com.eggman.pv.util.ItemIcon.drawSkull(g, skull, x, y);
			else g.renderItem(new ItemStack(Items.PAPER), x, y);

			String tierStr = (tier > 0 && tier <= 20) ? ROMANS[tier - 1] : String.valueOf(tier);
			PvRender.drawStringCentered(g, tierStr, x + slot / 2f, y - 7, true, maxed ? 0xFFD700 : 0xAAAAAA);

			if (hoverMouseX >= x && hoverMouseX <= x + slot && hoverMouseY >= y && hoverMouseY <= y + slot) {
				List<String> lore = com.eggman.pv.util.RepoItems.lore(internal);
				String dn = com.eggman.pv.util.RepoItems.displayName(internal);
				StringBuilder sb = new StringBuilder(dn != null ? dn : (prettyPet(type) + " Minion " + tierStr));
				if (lore != null) for (String l : lore) sb.append("\n").append(l);
				hoverText = sb.toString();
			}
		}
	}

	private int rarityRank(String r) {
		switch (r) {
			case "MYTHIC": return 5;
			case "LEGENDARY": return 4;
			case "EPIC": return 3;
			case "RARE": return 2;
			case "UNCOMMON": return 1;
			default: return 0;
		}
	}

	private String prettyPet(String type) {
		if (type == null || type.isEmpty()) return "Unknown";
		String[] parts = type.toLowerCase().split("_");
		StringBuilder sb = new StringBuilder();
		for (String p : parts) {
			if (p.isEmpty()) continue;
			sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
		}
		return sb.toString().trim();
	}

	private void renderPetsPage(GuiGraphics g) {
		petBtnRects.clear();
		int guiL = guiLeft, guiT = guiTop;
		JsonObject sel = getSelectedProfile();
		JsonArray pets = (sel != null && sel.has("pets") && sel.get("pets").isJsonArray())
				? sel.getAsJsonArray("pets") : null;
		if (pets == null || pets.size() == 0) {
			PvRender.drawStringCentered(g, "§cNo pets", guiL + SIZE_X / 2f, guiT + SIZE_Y / 2f, true, 0);
			return;
		}

		List<JsonObject> list = new ArrayList<>();
		for (int i = 0; i < pets.size(); i++) list.add(pets.get(i).getAsJsonObject());
		list.sort((a, b) -> {
			if (petSort == 2) return prettyPet(getString(a, "type", "")).compareTo(prettyPet(getString(b, "type", "")));
			if (petSort == 1) {
				double la = PetLevel.get(getDouble(a, "exp", 0), getString(a, "tier", "COMMON"), getString(a, "type", "")).level;
				double lb = PetLevel.get(getDouble(b, "exp", 0), getString(b, "tier", "COMMON"), getString(b, "type", "")).level;
				return Double.compare(lb, la);
			}
			int ra = rarityRank(getString(a, "tier", "COMMON")), rb = rarityRank(getString(b, "tier", "COMMON"));
			return rb - ra;
		});

		if (petCacheProfile != selectedProfileIndex) { petSelected = -1; petPage = 0; petCacheProfile = selectedProfileIndex; }
		if (petSelected < 0 && !list.isEmpty()) {
			int act = 0;
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).has("active") && list.get(i).get("active").getAsBoolean()) { act = i; break; }
			}
			petSelected = act;
		}

		PvRender.drawStringCentered(g, "Pets", guiL + 150, guiT + 14, true, 0xFF55FF);
		int funnelX = guiL + 252, funnelY = guiT + 8;
		g.fill(funnelX - 2, funnelY - 2, funnelX + 18, funnelY + 18, 0xFF373737);
		g.fill(funnelX - 1, funnelY - 1, funnelX + 17, funnelY + 17, 0xFF8B8B8B);
		g.renderItem(new ItemStack(Items.HOPPER), funnelX, funnelY);
		petBtnRects.add(new int[]{funnelX, funnelY, 18, 18, 310});
		if (hoverMouseX >= funnelX && hoverMouseX <= funnelX + 16 && hoverMouseY >= funnelY && hoverMouseY <= funnelY + 16) {
			String[] sn = {"Rarity", "Level", "Name"};
			hoverText = "§7Sort: §f" + sn[petSort] + "\n§8(click to change)";
		}

		int divX = guiL + 270;
		g.fill(divX, guiT + 14, divX + 1, guiT + SIZE_Y - 10, 0x33FFFFFF);
		int skx1 = guiL + 286, sky1 = guiT + 28, skx2 = guiL + 422, sky2 = guiT + 190;
		drawPanoramaBg(g, skx1, sky1, skx2, sky2);

		float pcx = (skx1 + skx2) / 2f;
		if (petSelected >= 0 && petSelected < list.size()) {
			JsonObject p = list.get(petSelected);
			String type = getString(p, "type", "");
			String tier = getString(p, "tier", "COMMON");
			PetLevel.Result lvl = PetLevel.get(getDouble(p, "exp", 0), tier, type);
			int rc = 0xFF000000 | rarityColor(tier);
			boolean maxed = lvl.level >= lvl.maxLevel;

			String dn = com.eggman.pv.util.RepoPets.displayName(type, tier, lvl.level);
			if (dn == null) dn = rarityCode(tier) + "[Lvl " + lvl.level + "] " + prettyPet(type);
			PvRender.drawStringCentered(g, dn, pcx, sky1 + 4, true, 0xFFFFFF);

			int barX = skx1 + 6, barW = (skx2 - skx1) - 12;

			PvRender.drawString(g, "§7To Next LVL", barX, sky1 + 16, 0xAAAAAA, true);
			String nx = maxed ? "MAX" : formatNumber(lvl.xpToNext);
			PvRender.drawString(g, (maxed ? "§a" : "§b") + nx, barX + barW - PvRender.font().width(nx), sky1 + 16, 0xFFFFFF, true);
			drawBar(g, barX, sky1 + 26, barW, maxed ? 1.0 : lvl.progress, rc);

			PvRender.drawString(g, "§7To Max LVL", barX, sky1 + 33, 0xAAAAAA, true);
			String mx = maxed ? "MAX" : formatNumber(lvl.xpToMax);
			PvRender.drawString(g, (maxed ? "§a" : "§b") + mx, barX + barW - PvRender.font().width(mx), sky1 + 33, 0xFFFFFF, true);
			double maxFrac = (lvl.totalXp + lvl.xpToMax) > 0 ? lvl.totalXp / (lvl.totalXp + lvl.xpToMax) : 1.0;
			drawBar(g, barX, sky1 + 43, barW, maxFrac, rc);

			String skullTex = com.eggman.pv.util.RepoPets.skull(type, tier);
			ItemStack head = skullTex != null ? com.eggman.pv.util.ItemIcon.skullStack(skullTex)
					: new ItemStack(PetIcon.get(type));
			float bob = (float) Math.sin(System.currentTimeMillis() / 400.0) * 3f;
			float cy = guiT + 108;
			g.pose().pushMatrix();
			g.pose().translate(pcx, cy + bob);
			g.pose().scale(3.8f, 3.8f);
			g.renderItem(head, -8, -8);
			g.pose().popMatrix();

			int sy = sky2 - 36;
			drawKV(g, barX, sy, "§7Total XP", "§f" + formatNumber(lvl.totalXp));
			drawKV(g, barX, sy + 11, "§7Current LVL XP", "§f" + formatNumber(lvl.currentLevelXp));
			drawKV(g, barX, sy + 22, "§7Required LVL XP", maxed ? "§aMAX" : "§f" + formatNumber(lvl.requiredLevelXp));
		} else {
			String msg = com.eggman.pv.util.RepoManager.isReady() ? "§7Select a pet" : "§7Downloading repo...";
			PvRender.drawStringCentered(g, msg, pcx, (sky1 + sky2) / 2f, true, 0xAAAAAA);
		}

		int perPage = 20, cols = 4, slot = 16;
		int maxPage = Math.max(0, (list.size() - 1) / perPage);
		if (petPage > maxPage) petPage = maxPage;
		if (petPage < 0) petPage = 0;
		int startX = guiL + 42, stepX = 44, startY = guiT + 34, gStepY = 36;

		for (int k = petPage * perPage, j = 0; k < Math.min((petPage + 1) * perPage, list.size()); k++, j++) {
			JsonObject p = list.get(k);
			String type = getString(p, "type", "");
			String tier = getString(p, "tier", "COMMON");
			PetLevel.Result lvl = PetLevel.get(getDouble(p, "exp", 0), tier, type);
			boolean active = p.has("active") && p.get("active").getAsBoolean();
			int rc = rarityColor(tier);

			int col = j % cols, row = j / cols;
			int x = startX + col * stepX, y = startY + row * gStepY;

			g.fill(x - 1, y - 1, x + slot + 1, y + slot + 1, 0xFF000000 | rc);
			g.fill(x, y, x + slot, y + slot, 0xFF202020);
			String gSkull = com.eggman.pv.util.RepoPets.skull(type, tier);
			if (gSkull != null) com.eggman.pv.util.ItemIcon.drawSkull(g, gSkull, x, y);
			else g.renderItem(new ItemStack(PetIcon.get(type)), x, y);
			if (active) PvRender.drawString(g, "§a●", x + slot - 5, y - 1, 0x55FF55, true);
			PvRender.drawStringCentered(g, String.valueOf(lvl.level), x + slot / 2f, y + slot + 1, true, 0xFFFFFF);

			if (k == petSelected) g.fill(x - 1, y - 1, x + slot + 1, y, 0xFFFFFFFF);

			if (hoverMouseX >= x && hoverMouseX <= x + slot && hoverMouseY >= y && hoverMouseY <= y + slot) {
				List<String> repoLore = com.eggman.pv.util.RepoPets.lore(type, tier, lvl.level);
				StringBuilder sb;
				if (repoLore != null && !repoLore.isEmpty()) {

					String held = p.has("heldItem") && !p.get("heldItem").isJsonNull() ? p.get("heldItem").getAsString() : null;
					List<String> heldLines = com.eggman.pv.util.RepoPets.heldItemLines(held);
					List<String> lore = new ArrayList<>(repoLore);
					if (heldLines != null) {

						int insert = lore.size() - 1;
						while (insert > 0 && lore.get(insert).trim().isEmpty()) insert--;
						lore.addAll(insert, heldLines);
						lore.add(insert, "");
					}
					String dn = com.eggman.pv.util.RepoPets.displayName(type, tier, lvl.level);
					sb = new StringBuilder(dn != null ? dn : (rarityCode(tier) + "[Lvl " + lvl.level + "] " + prettyPet(type)));
					for (String line : lore) sb.append("\n").append(line);
				} else {

					sb = new StringBuilder(rarityCode(tier) + "[Lvl " + lvl.level + "] " + prettyPet(type));
					String cat = com.eggman.pv.util.PetStats.category(type);
					if (cat != null) sb.append("\n§7").append(cap(cat)).append(" Pet");
					for (String line : com.eggman.pv.util.PetStats.statLines(type, tier, lvl.level)) sb.append("\n").append(line);
					String held = p.has("heldItem") && !p.get("heldItem").isJsonNull() ? p.get("heldItem").getAsString() : null;
					if (held != null) sb.append("\n§6Held Item: §f").append(prettyPet(held.replace("PET_ITEM_", "")));
					sb.append("\n").append(rarityCode(tier)).append("§l").append(tier);
				}
				hoverText = sb.toString();
			}
			petBtnRects.add(new int[]{x, y, slot, slot, k});
		}

		if (maxPage > 0) {
			int cx = guiL + 228, ay = guiT + 150;
			if (petPage > 0) { drawPageArrow(g, cx - 14, ay, "<"); petBtnRects.add(new int[]{cx - 14, ay, 12, 14, 300}); }
			if (petPage < maxPage) { drawPageArrow(g, cx + 2, ay, ">"); petBtnRects.add(new int[]{cx + 2, ay, 12, 14, 301}); }
			PvRender.drawStringCentered(g, "Page " + (petPage + 1) + "/" + (maxPage + 1), cx, ay - 11, false, 0x909090);
		}
	}

	private String cap(String s) {
		if (s == null || s.isEmpty()) return "";
		return s.charAt(0) + s.substring(1).toLowerCase();
	}

	private String rarityCode(String tier) {
		switch (tier) {
			case "MYTHIC": return "§d";
			case "LEGENDARY": return "§6";
			case "EPIC": return "§5";
			case "RARE": return "§9";
			case "UNCOMMON": return "§a";
			default: return "§f";
		}
	}

	private static final String[] CRYSTALS = {
		"jade","amber","topaz","sapphire","amethyst","jasper",
		"ruby","opal","aquamarine","peridot","onyx","citrine"
	};

	private static final String[] CRYSTAL_COLORS = {
		"§a","§6","§e","§9","§5","§d",
		"§c","§f","§b","§2","§8","§6"
	};

	private void renderMiningPage(GuiGraphics g) {
		miningBtnRects.clear();
		int guiL = guiLeft, guiT = guiTop;
		JsonObject sel = getSelectedProfile();
		JsonObject m = (sel != null && sel.has("mining") && sel.get("mining").isJsonObject())
				? sel.getAsJsonObject("mining") : null;
		if (m == null) {
			PvRender.drawStringCentered(g, "§cNo mining data", guiL + SIZE_X / 2f, guiT + SIZE_Y / 2f, true, 0);
			return;
		}

		int lx = guiL + 10;
		double exp = getDouble(m, "experience", 0);
		int hotmLevel = com.eggman.pv.util.Hotm.level(exp);
		int barY = guiT + 22;
		g.fill(lx - 4, barY - 4, lx + 134, barY + 22, 0xC0000000);
		PvRender.drawString(g, "§d❤ HOTM §f" + hotmLevel + "§8/§7" + com.eggman.pv.util.Hotm.maxLevel(), lx, barY, 0xFF55FF, true);
		int pbX = lx, pbY = barY + 11, pbW = 126;

		double overall = Math.min(1.0, (hotmLevel - 1 + com.eggman.pv.util.Hotm.progress(exp)) / com.eggman.pv.util.Hotm.maxLevel());
		g.fill(pbX, pbY, pbX + pbW, pbY + 5, 0xFF2A002A);
		g.fill(pbX, pbY, pbX + (int) (pbW * overall), pbY + 5, 0xFFFF55FF);

		int powY = barY + 26;
		g.fill(lx - 4, powY - 4, lx + 134, powY + 34, 0xC0000000);
		PvRender.drawString(g, "§2Mithril §7" + formatNumber(getDouble(m, "powder_mithril", 0))
				+ " §8/ " + formatNumber(getDouble(m, "powder_mithril_total", 0)), lx, powY, 0xFFFFFF, true);
		PvRender.drawString(g, "§dGemstone §7" + formatNumber(getDouble(m, "powder_gemstone", 0))
				+ " §8/ " + formatNumber(getDouble(m, "powder_gemstone_total", 0)), lx, powY + 10, 0xFFFFFF, true);
		PvRender.drawString(g, "§bGlacite §7" + formatNumber(getDouble(m, "powder_glacite", 0))
				+ " §8/ " + formatNumber(getDouble(m, "powder_glacite_total", 0)), lx, powY + 20, 0xFFFFFF, true);

		int crY = powY + 40;
		g.fill(lx - 4, crY - 4, lx + 134, guiT + SIZE_Y - 6, 0xC0000000);
		PvRender.drawString(g, "§5Crystal Nucleus", lx, crY, 0xAA00AA, true);
		JsonObject crystals = m.has("crystals") && m.get("crystals").isJsonObject() ? m.getAsJsonObject("crystals") : null;
		for (int i = 0; i < CRYSTALS.length; i++) {
			boolean has = false;
			if (crystals != null && crystals.has(CRYSTALS[i]) && crystals.get(CRYSTALS[i]).isJsonObject()) {
				JsonObject c = crystals.getAsJsonObject(CRYSTALS[i]);
				has = getDouble(c, "total_placed", 0) > 0 || !"NOT_FOUND".equals(getString(c, "state", "NOT_FOUND"));
			}
			int cy = crY + 12 + (i / 2) * 10;
			int cx = lx + (i % 2) * 66;
			String name = CRYSTALS[i].substring(0, 1).toUpperCase() + CRYSTALS[i].substring(1);
			PvRender.drawString(g, (has ? "§a✔ " : "§c✖ ") + CRYSTAL_COLORS[i] + name, cx, cy, 0xFFFFFF, true);
		}
		PvRender.drawString(g, "§7Nucleus Runs: §f" + (int) getDouble(m, "nucleus_runs", 0), lx, crY + 12 + 6 * 10 + 2, 0xAAAAAA, true);

		JsonObject nodes = m.has("nodes") && m.get("nodes").isJsonObject() ? m.getAsJsonObject("nodes") : new JsonObject();
		g.fill(guiL + 156, guiT + 18, guiL + SIZE_X - 6, guiT + SIZE_Y - 6, 0x66000000);
		int cell = 22, cols = 7;
		int visibleRows = 7;
		int treeW = cols * cell;
		int treeX = guiL + 156 + ((SIZE_X - 6 - 156) - treeW) / 2 + 4;
		int treeY = guiT + 28;
		int maxScroll = Math.max(0, 10 - visibleRows);
		if (miningScroll > maxScroll) miningScroll = maxScroll;
		if (miningScroll < 0) miningScroll = 0;

		for (com.eggman.pv.util.Hotm.Perk p : com.eggman.pv.util.Hotm.perks().values()) {
			int displayRow = p.y - miningScroll;
			if (displayRow < 0 || displayRow >= visibleRows) continue;
			int x = treeX + p.x * cell;
			int y = treeY + displayRow * cell;
			int lvl = nodes.has(p.key) && nodes.get(p.key).isJsonPrimitive() ? nodes.get(p.key).getAsInt() : 0;
			ItemStack icon = hotmIcon(p, lvl);
			g.renderItem(icon, x, y);
			if (lvl > 0 && p.maxLevel > 1) {
				PvRender.drawString(g, String.valueOf(lvl), x + 16 - PvRender.font().width(String.valueOf(lvl)), y + 9, 0xFFFFFF, true);
			}
			if (hoverMouseX >= x && hoverMouseX <= x + 16 && hoverMouseY >= y && hoverMouseY <= y + 16) {
				boolean maxed = lvl >= p.maxLevel;
				hoverText = (maxed ? "§a" : "§e") + p.name
						+ "\n§7Level: §f" + lvl + "§7/§f" + p.maxLevel
						+ (p.kind.equals("core") ? "" : "\n§8" + p.powder.toLowerCase() + " powder");
			}
		}

		if (maxScroll > 0) {
			PvRender.drawStringCentered(g, "§8scroll ▲▼", guiL + SIZE_X / 2f + 70, guiT + SIZE_Y - 12, false, 0x555555);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (currentPage == Page.MINING) {
			if (scrollY < 0) miningScroll++;
			else if (scrollY > 0) miningScroll--;
			if (miningScroll < 0) miningScroll = 0;
			if (miningScroll > 3) miningScroll = 3;
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private ItemStack hotmIcon(com.eggman.pv.util.Hotm.Perk p, int lvl) {
		if (p.kind.equals("core")) return new ItemStack(Items.REDSTONE_BLOCK);
		if (p.kind.equals("ability")) return new ItemStack(lvl > 0 ? Items.EMERALD_BLOCK : Items.COAL_BLOCK);
		if (lvl <= 0) return new ItemStack(Items.COAL);
		if (lvl >= p.maxLevel) return new ItemStack(Items.DIAMOND);
		return new ItemStack(Items.EMERALD);
	}

	private int killsFor(JsonObject kills, com.eggman.pv.util.BestiaryData.Mob m) {
		int s = 0;
		for (String k : m.killKeys) s += (int) getDouble(kills, k, 0);
		return s;
	}

	private void renderBestiaryPage(GuiGraphics g) {
		bestiaryBtnRects.clear();
		int guiL = guiLeft, guiT = guiTop;
		java.util.List<com.eggman.pv.util.BestiaryData.Island> islands = com.eggman.pv.util.BestiaryData.islands();
		if (islands == null || islands.isEmpty()) {
			String msg = com.eggman.pv.util.RepoManager.isReady() ? "§cNo bestiary data" : "§7Downloading repo...";
			PvRender.drawStringCentered(g, msg, guiL + SIZE_X / 2f, guiT + SIZE_Y / 2f, true, 0);
			return;
		}
		if (bestiaryIsland >= islands.size()) bestiaryIsland = 0;

		JsonObject sel = getSelectedProfile();
		JsonObject best = (sel != null && sel.has("bestiary") && sel.get("bestiary").isJsonObject())
				? sel.getAsJsonObject("bestiary") : new JsonObject();
		JsonObject kills = best.has("kills") && best.get("kills").isJsonObject() ? best.getAsJsonObject("kills") : new JsonObject();

		int ix = guiL + 8, iy = guiT + 4;
		for (int i = 0; i < islands.size(); i++) {
			com.eggman.pv.util.BestiaryData.Island is = islands.get(i);
			int x = ix + i * 20;
			if (x + 16 > guiL + 290) break;
			boolean active = i == bestiaryIsland;
			g.fill(x - 1, iy - 1, x + 17, iy + 17, active ? 0xFFAAAAAA : 0xFF373737);
			g.fill(x, iy, x + 16, iy + 16, 0xFF8B8B8B);
			if (is.iconTexture != null) com.eggman.pv.util.ItemIcon.drawSkull(g, is.iconTexture, x, iy);
			bestiaryBtnRects.add(new int[]{x, iy, 16, 16, i});
			if (hoverMouseX >= x && hoverMouseX <= x + 16 && hoverMouseY >= iy && hoverMouseY <= iy + 16) hoverText = is.name;
		}

		int totalTiers = 0;
		for (com.eggman.pv.util.BestiaryData.Island is : islands)
			for (com.eggman.pv.util.BestiaryData.Mob m : is.mobs)
				totalTiers += com.eggman.pv.util.BestiaryData.tier(killsFor(kills, m), m.bracket);

		com.eggman.pv.util.BestiaryData.Island isl = islands.get(bestiaryIsland);
		PvRender.drawString(g, "§e" + ItemData.strip(isl.name), guiL + 10, guiT + 24, 0xFFFF55, true);
		int gx = guiL + 10, gy = guiT + 36, slot = 20, cols = 9;
		int found = 0, completed = 0;
		for (int i = 0; i < isl.mobs.size(); i++) {
			com.eggman.pv.util.BestiaryData.Mob m = isl.mobs.get(i);
			int k = killsFor(kills, m);
			int tier = com.eggman.pv.util.BestiaryData.tier(k, m.bracket);
			int maxT = com.eggman.pv.util.BestiaryData.maxTier(m.cap, m.bracket);
			if (tier > 0) found++;
			if (maxT > 0 && tier >= maxT) completed++;

			int col = i % cols, row = i / cols;
			int x = gx + col * slot, y = gy + row * slot;
			boolean maxed = maxT > 0 && tier >= maxT;
			g.fill(x - 1, y - 1, x + 17, y + 17, maxed ? 0xFFFFAA00 : (tier > 0 ? 0xFF55AA55 : 0xFF555555));
			g.fill(x, y, x + 16, y + 16, 0xFF202020);
			if (m.texture != null) com.eggman.pv.util.ItemIcon.drawSkull(g, m.texture, x, y);
			else if (m.item != null && !m.item.isEmpty()) {

				net.minecraft.world.item.Item bItem = net.minecraft.core.registries.BuiltInRegistries.ITEM
						.getValue(net.minecraft.resources.ResourceLocation.withDefaultNamespace(m.item));
				g.renderItem(new ItemStack(bItem), x, y);
			} else g.renderItem(new ItemStack(Items.PAPER), x, y);
			PvRender.drawStringCentered(g, tier > 0 ? String.valueOf(tier) : "", x + 8, y + 17, true, maxed ? 0xFFD700 : 0xFFFFFF);

			if (hoverMouseX >= x && hoverMouseX <= x + 16 && hoverMouseY >= y && hoverMouseY <= y + 16) {
				int deaths = 0;
				JsonObject dk = best.has("deaths") && best.get("deaths").isJsonObject() ? best.getAsJsonObject("deaths") : new JsonObject();
				for (String kk : m.killKeys) deaths += (int) getDouble(dk, kk, 0);
				int next = com.eggman.pv.util.BestiaryData.nextThreshold(k, m.bracket, m.cap);
				double tierFrac = maxed ? 1.0 : (next > 0 ? Math.min(1.0, (double) k / next) : 0);
				double overallFrac = m.cap > 0 ? Math.min(1.0, (double) k / m.cap) : 0;
				StringBuilder sb = new StringBuilder("§a" + ItemData.strip(m.name) + " " + tier);
				sb.append("\n§7Kills: §f").append(formatNumber(k));
				sb.append("\n§7Deaths: §f").append(formatNumber(deaths));
				sb.append("\n");
				if (maxed) {
					sb.append("\n§6§lMAXED!");
				} else {
					sb.append("\n§7Progress to Tier ").append(tier + 1).append(": §a").append(String.format("%.1f", tierFrac * 100)).append("%");
					sb.append("\n").append(textBar(tierFrac)).append(" §7").append(formatNumber(k)).append("/").append(formatNumber(next));
				}
				sb.append("\n§7Overall Progress: §a").append(String.format("%.1f", overallFrac * 100)).append("%");
				sb.append("\n").append(textBar(overallFrac)).append(" §7").append(formatNumber(k)).append("/").append(formatNumber(m.cap));
				hoverText = sb.toString();
			}
		}

		int px = guiL + 296, py = guiT + 36, pw = guiL + SIZE_X - 6 - px;
		g.fill(px - 4, py - 4, guiL + SIZE_X - 6, py + 86, 0x88000000);
		double ms = totalTiers / 10.0;
		PvRender.drawString(g, "§dBestiary Milestone", px, py, 0xFF55FF, true);
		PvRender.drawString(g, "§f" + String.format("%.1f", ms), px + pw - 4 - PvRender.font().width(String.format("%.1f", ms)), py, 0xFFFFFF, true);
		drawBar(g, px, py + 10, pw, ms - Math.floor(ms), 0xFFFF55FF);

		int found2 = found, total = isl.mobs.size();
		PvRender.drawString(g, "§aFamilies Found", px, py + 26, 0x55FF55, true);
		PvRender.drawString(g, "§7" + found2 + "/" + total, px + pw - 4 - PvRender.font().width(found2 + "/" + total), py + 26, 0xAAAAAA, true);
		drawBar(g, px, py + 36, pw, total > 0 ? (double) found2 / total : 0, 0xFFFF55FF);

		PvRender.drawString(g, "§6Families Completed", px, py + 52, 0xFFAA00, true);
		PvRender.drawString(g, "§7" + completed + "/" + total, px + pw - 4 - PvRender.font().width(completed + "/" + total), py + 52, 0xAAAAAA, true);
		drawBar(g, px, py + 62, pw, total > 0 ? (double) completed / total : 0, 0xFFFF55FF);
	}

	private String textBar(double frac) {
		frac = Math.max(0, Math.min(1, frac));
		int n = (int) Math.round(frac * 10);
		StringBuilder b = new StringBuilder("§a");
		for (int i = 0; i < n; i++) b.append('█');
		b.append("§8");
		for (int i = n; i < 10; i++) b.append('█');
		return b.toString();
	}

	private void drawBar(GuiGraphics g, int x, int y, int w, double frac, int color) {
		frac = Math.max(0, Math.min(1, frac));
		g.fill(x, y, x + w, y + 4, 0xFF2A002A);
		g.fill(x, y, x + (int) (w * frac), y + 4, color);
	}

	private void renderTrophyPage(GuiGraphics g) {
		int guiL = guiLeft, guiT = guiTop;
		JsonObject sel = getSelectedProfile();
		JsonObject tf = (sel != null && sel.has("trophy_fish") && sel.get("trophy_fish").isJsonObject())
				? sel.getAsJsonObject("trophy_fish") : null;
		if (tf == null) {
			PvRender.drawStringCentered(g, "§cNo trophy fishing data", guiL + SIZE_X / 2f, guiT + SIZE_Y / 2f, true, 0);
			return;
		}

		if (!com.eggman.pv.util.RepoManager.isReady()) {
			PvRender.drawStringCentered(g, "§eDownloading NEU repo...", guiL + SIZE_X / 2f, guiT + 12, true, 0xFFFF55);
		}

		int tc = (int) getDouble(tf, "total_caught", 0);
		g.fill(guiL + 8, guiT + 18, guiL + 150, guiT + 32, 0x88000000);
		g.renderItem(new ItemStack(Items.FISHING_ROD), guiL + 12, guiT + 19);
		PvRender.drawString(g, "§bTotal Caught: §f" + tc, guiL + 32, guiT + 22, 0x55FFFF, true);

		int[] perTier = new int[4];
		for (String fish : TrophyFishData.FISH) {
			int hi = 0;
			for (int r = 0; r < 4; r++) if (tf.has(fish + "_" + TrophyFishData.RARITIES[r])) hi = r + 1;
			for (int r = 0; r < hi; r++) perTier[r]++;
		}
		JsonArray rewards = tf.has("rewards") && tf.get("rewards").isJsonArray() ? tf.getAsJsonArray("rewards") : new JsonArray();
		g.fill(guiL + 8, guiT + 38, guiL + 150, guiT + 92, 0x88000000);
		for (int i = 0; i < 4; i++) {
			int y = guiT + 42 + i * 12;
			String hs = com.eggman.pv.util.RepoItems.skull(TrophyFishData.RANK_HELMETS[i]);
			if (hs != null) com.eggman.pv.util.ItemIcon.drawSkull(g, hs, guiL + 11, y);
			PvRender.drawString(g, TrophyFishData.RANK_LABELS[i], guiL + 30, y + 4, 0xFFFFFF, true);
			boolean claimed = rewards.size() > i && !rewards.get(i).isJsonNull();
			String right;
			if (claimed) right = "§a✔";
			else {
				int has = perTier[i], need = TrophyFishData.RANK_NEEDED[i];
				right = (has >= need ? "§a" : "§c") + has + "/" + need;
			}
			PvRender.drawString(g, right, guiL + 140 - PvRender.font().width(ItemData.strip(right)), y + 4, 0xFFFFFF, true);
		}

		JsonObject best = (sel.has("bestiary") && sel.get("bestiary").isJsonObject()) ? sel.getAsJsonObject("bestiary") : new JsonObject();
		JsonObject bk = best.has("kills") && best.get("kills").isJsonObject() ? best.getAsJsonObject("kills") : new JsonObject();
		g.fill(guiL + 8, guiT + 98, guiL + 150, guiT + 124, 0x88000000);
		PvRender.drawString(g, "§bThunder Kills: §f" + (int) getDouble(bk, "thunder_400", 0), guiL + 12, guiT + 102, 0x55FFFF, true);
		PvRender.drawString(g, "§bLord Jawbus Kills: §f" + (int) getDouble(bk, "lord_jawbus_600", 0), guiL + 12, guiT + 114, 0x55FFFF, true);

		List<Integer> discovered = new ArrayList<>(), undiscovered = new ArrayList<>();
		int[] totals = new int[TrophyFishData.FISH.length];
		int[] highest = new int[TrophyFishData.FISH.length];
		for (int f = 0; f < TrophyFishData.FISH.length; f++) {
			String fish = TrophyFishData.FISH[f];
			int tot = 0, hi = 0;
			for (int r = 0; r < 4; r++) {
				int c = (int) getDouble(tf, fish + "_" + TrophyFishData.RARITIES[r], 0);
				tot += c;
				if (c > 0) hi = r + 1;
			}
			totals[f] = tot; highest[f] = hi;
			if (hi > 0) discovered.add(f); else undiscovered.add(f);
		}
		discovered.sort((a, b) -> Integer.compare(totals[b], totals[a]));
		List<Integer> order = new ArrayList<>(discovered);
		order.addAll(undiscovered);

		for (int slot = 0; slot < order.size() && slot < TrophyFishData.SLOTS.length; slot++) {
			int f = order.get(slot);
			int x = guiL + TrophyFishData.SLOTS[slot][0];
			int y = guiT + TrophyFishData.SLOTS[slot][1];
			int hi = highest[f];
			int rc = hi == 1 ? 0xFF8200 : hi == 2 ? 0xC0C0C0 : hi == 3 ? 0xFFD100 : hi == 4 ? 0x1FD8F1 : 0x555555;
			g.fill(x - 2, y - 2, x + 18, y + 18, 0xFF000000 | rc);
			g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF202020);
			if (hi > 0) {
				PvRender.drawTexturedRect(g, PvRender.tex("trophyfish/" + TrophyFishData.FISH[f] + ".png"), x, y, 16, 16, 0f, 0f, 64, 64, 64, 64);
			} else {
				g.renderItem(new ItemStack(Items.GRAY_DYE), x, y);
			}
			if (hoverMouseX >= x && hoverMouseX <= x + 16 && hoverMouseY >= y && hoverMouseY <= y + 16) {
				StringBuilder sb = new StringBuilder(TrophyFishData.FISH_COLOR[f] + TrophyFishData.pretty(TrophyFishData.FISH[f]));
				String req = TrophyFishData.requirement(TrophyFishData.FISH[f]);
				if (!req.isEmpty()) sb.append("\n§7").append(req);
				if (hi == 0) sb.append("\n§c✖ Not Discovered");
				for (int r = 3; r >= 0; r--) {
					int c = (int) getDouble(tf, TrophyFishData.FISH[f] + "_" + TrophyFishData.RARITIES[r], 0);
					String rcol = r == 3 ? "§b" : r == 2 ? "§6" : r == 1 ? "§7" : "§c";
					String rn = TrophyFishData.RARITIES[r].substring(0, 1).toUpperCase() + TrophyFishData.RARITIES[r].substring(1);
					sb.append("\n").append(rcol).append(rn).append(": §f").append(c);
				}
				hoverText = sb.toString();
			}
		}
	}

	private static final String[] KUUDRA_NAMES = {"Basic", "Hot", "Burning", "Fiery", "Infernal"};
	private static final String[] KUUDRA_KEYS = {"none", "hot", "burning", "fiery", "infernal"};
	private static final String[] DOJO_KEYS = {"mob_kb", "wall_jump", "archer", "sword_swap", "snake", "lock_head", "fireball"};
	private static final String[] DOJO_NAMES = {"Test of Force", "Test of Stamina", "Test of Mastery", "Test of Discipline", "Test of Swiftness", "Test of Control", "Test of Tenacity"};
	private static final String[] DOJO_GRADES = {"F", "D", "C", "B", "A", "S"};

	private String factionTitle(int rep) {
		if (rep >= 12000) return "Hero";
		if (rep >= 6000) return "Honored";
		if (rep >= 3000) return "Trusted";
		if (rep >= 1000) return "Friendly";
		if (rep >= 0) return "Neutral";
		if (rep >= -1000) return "Unfriendly";
		return "Hostile";
	}

	private String dojoRank(int pts) {
		if (pts >= 7000) return "§8Black";
		if (pts >= 6000) return "§6Brown";
		if (pts >= 4000) return "§9Blue";
		if (pts >= 2000) return "§aGreen";
		if (pts >= 1000) return "§eYellow";
		return "§7None";
	}

	private int dojoNext(int pts) {
		int[] th = {1000, 2000, 4000, 6000, 7000};
		for (int t : th) if (pts < t) return t - pts;
		return 0;
	}

	private void renderCrimsonPage(GuiGraphics g) {
		int guiL = guiLeft, guiT = guiTop;
		JsonObject sel = getSelectedProfile();
		JsonObject c = (sel != null && sel.has("crimson") && sel.get("crimson").isJsonObject())
				? sel.getAsJsonObject("crimson") : null;
		if (c == null) {
			PvRender.drawStringCentered(g, "§cNo Crimson Isle data", guiL + SIZE_X / 2f, guiT + SIZE_Y / 2f, true, 0);
			return;
		}

		int top = guiT + 22, bot = guiT + SIZE_Y - 8;
		g.fill(guiL + 6, top, guiL + 144, bot, 0x88000000);
		g.fill(guiL + 148, top, guiL + 292, bot, 0x88000000);
		g.fill(guiL + 296, top, guiL + SIZE_X - 6, bot, 0x88000000);

		int kx = guiL + 8;
		PvRender.drawStringCentered(g, "§cKuudra Stats", guiL + (int) (431 * 0.18f), guiT + 14, true, 0xFF5555);
		JsonObject k = c.getAsJsonObject("kuudra");
		int total = 0;
		for (int i = 0; i < 5; i++) {
			int y = guiT + 28 + i * 20;
			int runs = (int) getDouble(k, KUUDRA_KEYS[i], 0);
			int hw = (int) getDouble(k, "hw_" + KUUDRA_KEYS[i], 0);
			total += runs;
			PvRender.drawString(g, "§c" + KUUDRA_NAMES[i] + ":", kx, y, 0xFF5555, true);
			PvRender.drawString(g, "§f" + runs, kx + 110, y, 0xFFFFFF, true);
			PvRender.drawString(g, "§8Highest Wave: §7" + hw, kx + 6, y + 9, 0xAAAAAA, true);
		}
		PvRender.drawString(g, "§cTotal runs:", kx, guiT + 28 + 5 * 20 + 2, 0xFF5555, true);
		PvRender.drawString(g, "§f" + total, kx + 110, guiT + 28 + 5 * 20 + 2, 0xFFFFFF, true);

		int dx = guiL + 150;
		PvRender.drawStringCentered(g, "§eDojo Stats", guiL + (int) (431 * 0.49f), guiT + 14, true, 0xFFFF55);
		JsonObject d = c.getAsJsonObject("dojo");
		int pts = 0;
		for (int i = 0; i < DOJO_KEYS.length; i++) {
			int y = guiT + 28 + i * 11;
			int p = (int) getDouble(d, DOJO_KEYS[i], 0);
			pts += p;
			String grade = DOJO_GRADES[Math.min(5, p / 200)];
			PvRender.drawString(g, "§7" + DOJO_NAMES[i] + ":", dx, y, 0xAAAAAA, true);
			PvRender.drawString(g, "§f" + p + " §7(" + grade + ")", dx + 100, y, 0xFFFFFF, true);
		}
		int dy = guiT + 28 + DOJO_KEYS.length * 11 + 6;
		PvRender.drawString(g, "§7Points: §6" + pts, dx, dy, 0xFFAA00, true);
		PvRender.drawString(g, "§7Rank: " + dojoRank(pts), dx, dy + 10, 0xFFFFFF, true);
		int next = dojoNext(pts);
		PvRender.drawString(g, "§7Points to next: §f" + (next == 0 ? "MAX" : next), dx, dy + 20, 0xFFFFFF, true);

		int fx = guiL + 300;
		PvRender.drawStringCentered(g, "§dFaction Reputation", guiL + (int) (431 * 0.82f), guiT + 14, true, 0xFF55FF);
		String faction = getString(c, "faction", "N/A");
		String facName = faction.equals("mages") ? "§5Mages" : faction.equals("barbarians") ? "§cBarbarians" : "§7N/A";
		int mageRep = (int) getDouble(c, "mages_rep", 0);
		int barbRep = (int) getDouble(c, "barbarians_rep", 0);
		PvRender.drawString(g, "§7Faction: " + facName, fx, guiT + 28, 0xFFFFFF, true);
		PvRender.drawString(g, "§5Mage Rep: §f" + formatNumber(mageRep), fx, guiT + 42, 0xFF55FF, true);
		PvRender.drawString(g, "§8Title: §7" + factionTitle(mageRep), fx, guiT + 51, 0xAAAAAA, true);
		PvRender.drawString(g, "§cBarbarian Rep: §f" + formatNumber(barbRep), fx, guiT + 64, 0xFF5555, true);
		PvRender.drawString(g, "§8Title: §7" + factionTitle(barbRep), fx, guiT + 73, 0xAAAAAA, true);

		PvRender.drawString(g, "§6Last Matriarch Attempt", fx, guiT + 90, 0xFFAA00, true);
		PvRender.drawString(g, "§7Heavy Pearls: §f" + (int) getDouble(c, "matriarch_pearls", 0), fx, guiT + 102, 0xFFFFFF, true);
		long last = (long) getDouble(c, "matriarch_last", 0);
		if (last > 0) {
			String date = new java.text.SimpleDateFormat("d MMM yyyy").format(new java.util.Date(last));
			PvRender.drawString(g, "§7Last: §f" + date, fx, guiT + 111, 0xFFFFFF, true);
		}
	}

	private void renderPlaceholderPage(GuiGraphics g, String name) {
		PvRender.drawStringCentered(g, name, guiLeft + SIZE_X / 2f, guiTop + SIZE_Y / 2f - 8,
			true, 0xFFFFFF);
		PvRender.drawStringCentered(g, "(soon)", guiLeft + SIZE_X / 2f, guiTop + SIZE_Y / 2f + 6,
			true, 0xAAAAAA);
	}

	private void ensureStorage() {
		if (storageCacheProfile == selectedProfileIndex) return;
		sInv = sEnder = sTalisman = sVault = sWardrobe = sFishing = sPotion = sArmor = sEquip = null;
		sBackpacks.clear();
		JsonObject sel = getSelectedProfile();
		if (sel != null && sel.has("inventory") && sel.get("inventory").isJsonObject()) {
			JsonObject inv = sel.getAsJsonObject("inventory");
			sInv = NbtParser.parse(getString(inv, "inv_contents", ""));
			sEnder = NbtParser.parse(getString(inv, "ender_chest", ""));
			sTalisman = NbtParser.parse(getString(inv, "talisman_bag", ""));
			sVault = NbtParser.parse(getString(inv, "personal_vault", ""));
			sWardrobe = NbtParser.parse(getString(inv, "wardrobe", ""));
			sFishing = NbtParser.parse(getString(inv, "fishing_bag", ""));
			sPotion = NbtParser.parse(getString(inv, "potion_bag", ""));
			sArmor = NbtParser.parse(getString(inv, "armor", ""));
			sEquip = NbtParser.parse(getString(inv, "equipment", ""));
			if (inv.has("backpacks") && inv.get("backpacks").isJsonArray()) {
				JsonArray bps = inv.getAsJsonArray("backpacks");
				for (int i = 0; i < bps.size(); i++) {
					List<ItemData> bp = NbtParser.parse(bps.get(i).getAsString());
					if (hasItems(bp)) sBackpacks.add(bp);
				}
			}
		}
		if (sInv == null) sInv = new ArrayList<>();
		if (sEnder == null) sEnder = new ArrayList<>();
		if (sTalisman == null) sTalisman = new ArrayList<>();
		if (sVault == null) sVault = new ArrayList<>();
		if (sWardrobe == null) sWardrobe = new ArrayList<>();
		if (sFishing == null) sFishing = new ArrayList<>();
		if (sPotion == null) sPotion = new ArrayList<>();
		if (sArmor == null) sArmor = new ArrayList<>();
		if (sEquip == null) sEquip = new ArrayList<>();
		storageCacheProfile = selectedProfileIndex;
	}

	private boolean hasItems(List<ItemData> list) {
		if (list == null) return false;
		for (ItemData it : list) if (it != null && !it.isEmpty()) return true;
		return false;
	}

	private static final int STORAGE_TYPES = 8;
	private static final int T_BACKPACK = 3, T_ACCESSORY = 4;
	private static final String[] STORAGE_LABELS = {
		"Inventory", "Ender Chest", "Personal Vault", "Backpacks",
		"Accessory Bag", "Wardrobe", "Fishing Bag", "Potion Bag"
	};

	private List<ItemData> storageBase(int type) {
		switch (type) {
			case 0: return sInv;
			case 1: return sEnder;
			case 2: return sVault;
			case 4: return sTalisman;
			case 5: return sWardrobe;
			case 6: return sFishing;
			case 7: return sPotion;
			default: return new ArrayList<>();
		}
	}

	private List<List<ItemData>> buildStoragePages() {
		List<List<ItemData>> pages = new ArrayList<>();
		if (storageType == T_BACKPACK) {
			pages.addAll(sBackpacks);
		} else {
			List<ItemData> all = storageBase(storageType);

			if (storageType == 0 && all.size() == 36) {
				List<ItemData> reordered = new ArrayList<>(all.subList(9, 36));
				reordered.addAll(all.subList(0, 9));
				all = reordered;
			}
			int per = 54;
			for (int i = 0; i < all.size(); i += per) {
				pages.add(all.subList(i, Math.min(i + per, all.size())));
			}
		}
		if (pages.isEmpty()) pages.add(new ArrayList<>());
		return pages;
	}

	private ItemStack storageIcon(int type) {
		switch (type) {
			case 0:  return new ItemStack(Items.CHEST);
			case 1:  return new ItemStack(Items.ENDER_CHEST);
			case 2:  return new ItemStack(Items.IRON_BLOCK);
			case 3:  return new ItemStack(Items.BUNDLE);
			case 4:  return new ItemStack(Items.GOLDEN_APPLE);
			case 5:  return new ItemStack(Items.LEATHER_CHESTPLATE);
			case 6:  return new ItemStack(Items.FISHING_ROD);
			case 7:  return new ItemStack(Items.POTION);
			default: return new ItemStack(Items.BARRIER);
		}
	}

	private static final double[] VAMPIRE_XP = {0, 20, 95, 335, 1175, 3575};

	private void ensureRift() {
		if (riftCacheProfile == selectedProfileIndex) return;
		rInv = rEnder = rArmor = rEquip = null;
		JsonObject sel = getSelectedProfile();
		if (sel != null && sel.has("rift") && sel.get("rift").isJsonObject()) {
			JsonObject rift = sel.getAsJsonObject("rift");
			if (rift.has("inventory") && rift.get("inventory").isJsonObject()) {
				JsonObject ri = rift.getAsJsonObject("inventory");
				rInv = NbtParser.parse(getString(ri, "inv_contents", ""));
				rEnder = NbtParser.parse(getString(ri, "ender_chest", ""));
				rArmor = NbtParser.parse(getString(ri, "armor", ""));
				rEquip = NbtParser.parse(getString(ri, "equipment", ""));
			}
		}
		if (rInv == null) rInv = new ArrayList<>();
		if (rEnder == null) rEnder = new ArrayList<>();
		if (rArmor == null) rArmor = new ArrayList<>();
		if (rEquip == null) rEquip = new ArrayList<>();
		riftCacheProfile = selectedProfileIndex;
	}

	private void renderRiftPage(GuiGraphics g) {
		ensureRift();
		riftBtnRects.clear();
		int guiL = guiLeft, guiT = guiTop;
		JsonObject sel = getSelectedProfile();
		JsonObject rift = (sel != null && sel.has("rift") && sel.get("rift").isJsonObject())
				? sel.getAsJsonObject("rift") : null;

		double motes = rift != null ? getDouble(rift, "motes_purse", 0) : 0;
		PvRender.drawString(g, "§dMotes: §f" + formatNumber(motes), guiL + 12, guiT + 12, 0xFF55FF, true);

		int armX = guiL + 44, eqX = guiL + 64, loadY = guiT + 46;
		PvRender.drawStringCentered(g, "§7Gear", guiL + 62, loadY - 9, true, 0xAAAAAA);
		for (int i = 0; i < 4; i++) {
			int ai = 3 - i;
			drawLoadoutSlot(g, ai < rArmor.size() ? rArmor.get(ai) : null, armX, loadY + i * 19);
			drawLoadoutSlot(g, i < rEquip.size() ? rEquip.get(i) : null, eqX, loadY + i * 19);
		}
		int wepY = loadY + 4 * 19 + 16;
		PvRender.drawStringCentered(g, "§7Weapon", guiL + 62, wepY - 9, true, 0xAAAAAA);
		drawLoadoutSlot(g, rInv.size() > 0 ? rInv.get(0) : null, guiL + 54, wepY);

		g.fill(guiL + 104, guiT + 18, guiL + 105, guiT + SIZE_Y - 12, 0x33FFFFFF);
		g.fill(guiL + 296, guiT + 18, guiL + 297, guiT + SIZE_Y - 12, 0x33FFFFFF);

		int cmid = guiL + 200;
		int tgX = cmid - 18, tgY = guiT + 20;
		for (int t = 0; t < 2; t++) {
			int sx = tgX + t * 34;
			boolean active = riftStorageType == t;
			g.fill(sx - 2, tgY - 2, sx + 18, tgY + 18, active ? 0xFFAAAAAA : 0xFF373737);
			g.fill(sx - 1, tgY - 1, sx + 17, tgY + 17, 0xFF8B8B8B);
			g.renderItem(new ItemStack(t == 0 ? Items.CHEST : Items.ENDER_CHEST), sx, tgY);
			riftBtnRects.add(new int[]{sx, tgY, 18, 18, t});
		}
		List<ItemData> items = riftStorageType == 0 ? rInv : rEnder;
		if (riftStorageType == 0 && items.size() == 36) {
			List<ItemData> re = new ArrayList<>(items.subList(9, 36));
			re.addAll(items.subList(0, 9));
			items = re;
		}
		int cols = 9, slot = 18, per = 54;
		int riftMaxPage = Math.max(0, (items.size() - 1) / per);
		if (riftPage > riftMaxPage) riftPage = riftMaxPage;
		if (riftPage < 0) riftPage = 0;
		List<ItemData> pageItems = items.isEmpty() ? items
				: items.subList(riftPage * per, Math.min((riftPage + 1) * per, items.size()));
		int rows = 6;
		int panelW = cols * slot + 14;
		int panelH = rows * slot + 17 + 7;
		int px = cmid - panelW / 2;
		int py = guiT + 108 - panelH / 2;
		g.fill(px, py, px + panelW, py + panelH, 0xFFC6C6C6);
		g.fill(px + 1, py + 1, px + panelW - 1, py + panelH - 1, 0xFF8B8B8B);
		PvRender.drawString(g, riftStorageType == 0 ? "Inventory" : "Ender Chest", px + 7, py + 6, 0x404040, false);
		if (!hasItems(items)) {
			String msg = (rift == null) ? "§cRift API disabled" : "§7Empty";
			PvRender.drawStringCentered(g, msg, cmid, py + panelH / 2f, true, 0);
		} else {
			int gx = px + 7, gy = py + 17;
			ItemData hov = null;
			for (int i = 0; i < pageItems.size(); i++) {
				int col = i % cols, row = i / cols;
				if (row >= rows) break;
				int ix = gx + col * slot, iy = gy + row * slot;
				g.fill(ix, iy, ix + 16, iy + 16, 0xFF373737);
				ItemData it = pageItems.get(i);
				if (it == null || it.isEmpty()) continue;
				com.eggman.pv.util.ItemIcon.draw(g, it, ix, iy);
				if (it.count > 1) PvRender.drawString(g, String.valueOf(it.count), ix + 16 - PvRender.font().width(String.valueOf(it.count)), iy + 9, 0xFFFFFF, true);
				if (hoverMouseX >= ix && hoverMouseX <= ix + 16 && hoverMouseY >= iy && hoverMouseY <= iy + 16) hov = it;
			}
			if (hov != null) {
				StringBuilder sb = new StringBuilder(hov.name);
				for (String l : hov.lore) sb.append("\n").append(l);
				hoverText = sb.toString();
			}
		}

		if (riftMaxPage > 0) {
			int ay = py + panelH + 4;
			if (riftPage > 0) { drawPageArrow(g, cmid - 14, ay, "<"); riftBtnRects.add(new int[]{cmid - 14, ay, 12, 14, 100}); }
			if (riftPage < riftMaxPage) { drawPageArrow(g, cmid + 2, ay, ">"); riftBtnRects.add(new int[]{cmid + 2, ay, 12, 14, 101}); }
			PvRender.drawStringCentered(g, "Page " + (riftPage + 1) + "/" + (riftMaxPage + 1), cmid, ay - 9, false, 0x909090);
		}

		int rx = guiL + 306, ry = guiT + 42, rw = 110;
		double vampXp = rift != null ? getDouble(rift, "vampire_xp", 0) : 0;
		int vlvl = 0;
		for (int i = 1; i < VAMPIRE_XP.length; i++) if (vampXp >= VAMPIRE_XP[i]) vlvl = i;
		int vmax = VAMPIRE_XP.length - 1;
		double vProg, vToNext;
		if (vlvl >= vmax) { vProg = 1; vToNext = 0; }
		else {
			double base = VAMPIRE_XP[vlvl], next = VAMPIRE_XP[vlvl + 1];
			vProg = (next - base) > 0 ? (vampXp - base) / (next - base) : 0;
			vToNext = next - vampXp;
		}
		PvRender.drawString(g, "§6Vampire", rx, ry, 0xFFAA00, true);
		PvRender.drawString(g, "§f" + vlvl, rx + rw - PvRender.font().width(String.valueOf(vlvl)), ry, 0xFFFFFF, true);
		drawBar(g, rx, ry + 10, rw, vlvl >= vmax ? 1.0 : vProg, 0xFFFF5555);
		if (hoverMouseX >= rx && hoverMouseX <= rx + rw && hoverMouseY >= ry && hoverMouseY <= ry + 14) {
			hoverText = "§6Vampire Slayer §7LVL " + vlvl
					+ "\n§7Total XP: §b" + formatNumber(vampXp)
					+ (vlvl >= vmax ? "\n§aMAXED" : "\n§7To Next (LVL " + (vlvl + 1) + "): §b" + formatNumber(vToNext));
		}
		PvRender.drawString(g, "§7Total XP: §b" + formatNumber(vampXp), rx, ry + 18, 0xAAAAAA, true);

		int burgers = rift != null ? (int) getDouble(rift, "burgers", 0) : 0;
		PvRender.drawTexturedRect(g, PvRender.tex("rift/burger.png"), rx, ry + 32, 16, 16, 0f, 0f, 64, 64, 64, 64);
		PvRender.drawString(g, "§eBurger: §f" + burgers + "§7/" + RIFT_MAX_BURGERS, rx + 20, ry + 36, 0xFFFF55, true);

		int enigma = rift != null ? (int) getDouble(rift, "enigma_found", 0) : 0;
		PvRender.drawTexturedRect(g, PvRender.tex("rift/enigma_soul.png"), rx, ry + 50, 16, 16, 0f, 0f, 64, 64, 64, 64);
		PvRender.drawString(g, "§5Enigma Souls: §f" + enigma + "§7/" + RIFT_ENIGMA_TOTAL, rx + 20, ry + 54, 0xAA00AA, true);

		int tc = rift != null ? (int) getDouble(rift, "timecharms", 0) : 0;
		g.renderItem(new ItemStack(Items.CLOCK), rx, ry + 68);
		PvRender.drawString(g, "§bTimecharms: §f" + tc + "§7/" + RIFT_TIMECHARMS.length, rx + 20, ry + 72, 0x55FFFF, true);
		boolean tcHover = hoverMouseX >= rx && hoverMouseX <= rx + rw && hoverMouseY >= ry + 68 && hoverMouseY <= ry + 80;

		if (tcHover) drawTimecharmPanel(g, rift, guiL, guiT);
	}

	private static final int RIFT_ENIGMA_TOTAL = 52;
	private static final int RIFT_MAX_BURGERS = 5;

	private static final String[][] RIFT_TIMECHARMS = {
		{"wyldly_supreme", "Supreme"},
		{"mirrored", "esrevrorriM"},
		{"chicken_n_egg", "Chicken N Egg"},
		{"citizen", "SkyBlock Citizen"},
		{"lazy_living", "Living"},
		{"slime", "Globulate"},
		{"vampiric", "Vampiric"},
		{"mountain", "Celestial"},
	};

	private void drawTimecharmPanel(GuiGraphics g, JsonObject rift, int guiL, int guiT) {
		java.util.Set<String> owned = new java.util.HashSet<>();
		if (rift != null && rift.has("timecharms_obtained") && rift.get("timecharms_obtained").isJsonArray()) {
			for (var e : rift.getAsJsonArray("timecharms_obtained")) owned.add(e.getAsString());
		}
		int W = 240, H = 122;
		int x0 = guiL + (SIZE_X - W) / 2, y0 = guiT + 22;
		g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, 0xFF2A2A33);
		g.fill(x0, y0, x0 + W, y0 + H, 0xF0101015);
		PvRender.drawString(g, "§f§lTimecharms", x0 + 8, y0 + 7, 0xFFFFFF, true);
		PvRender.drawString(g, "§7Obtained: §f" + owned.size() + "§7/" + RIFT_TIMECHARMS.length, x0 + 8, y0 + 18, 0xAAAAAA, true);
		int cellW = (W - 12) / 2, startX = x0 + 8, startY = y0 + 32, cellH = 22;
		for (int i = 0; i < RIFT_TIMECHARMS.length; i++) {
			int col = i % 2, row = i / 2;
			int cx = startX + col * cellW, cy = startY + row * cellH;
			boolean has = owned.contains(RIFT_TIMECHARMS[i][0]);
			g.renderItem(timecharmStack(RIFT_TIMECHARMS[i][0]), cx, cy);
			if (!has) g.fill(cx, cy, cx + 16, cy + 16, 0x77000000);
			PvRender.drawString(g, (has ? "§f" : "§8") + RIFT_TIMECHARMS[i][1], cx + 20, cy, has ? 0xFFFFFF : 0x666666, true);
			PvRender.drawString(g, has ? "§aObtained" : "§7Not Obtained", cx + 20, cy + 9, has ? 0x55FF55 : 0x888888, true);
		}
	}

	private ItemStack timecharmStack(String id) {
		net.minecraft.world.item.Item item;
		switch (id) {
			case "wyldly_supreme": item = Items.SPRUCE_LEAVES; break;
			case "mirrored":       item = Items.GLASS; break;
			case "chicken_n_egg":  item = Items.SOUL_SAND; break;
			case "citizen":        item = Items.JUKEBOX; break;
			case "lazy_living":    item = Items.LAPIS_ORE; break;
			case "slime":          item = Items.SLIME_BLOCK; break;
			case "vampiric":       item = Items.REDSTONE_BLOCK; break;
			case "mountain":       item = Items.LAPIS_BLOCK; break;
			default:               item = Items.CLOCK; break;
		}
		ItemStack st = new ItemStack(item);
		st.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return st;
	}

	private void renderStoragePage(GuiGraphics g) {
		ensureStorage();
		int guiL = guiLeft, guiT = guiTop;
		storageBtnRects.clear();

		if (storageType >= STORAGE_TYPES) storageType = 0;

		for (int t = 0; t < STORAGE_TYPES; t++) {
			int col = t % 3, row = t / 3;
			int sx = guiL + 19 + 34 * col;
			int sy = guiT + 26 + 34 * row;
			boolean active = storageType == t;
			g.fill(sx - 2, sy - 2, sx + 18, sy + 18, active ? 0xFFAAAAAA : 0xFF373737);
			g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF8B8B8B);
			g.renderItem(storageIcon(t), sx, sy);
			storageBtnRects.add(new int[]{sx, sy, 18, 18, t});
			if (hoverMouseX >= sx && hoverMouseX <= sx + 16 && hoverMouseY >= sy && hoverMouseY <= sy + 16) {
				if (t == T_ACCESSORY) {
					int mp = MagicalPower.total(sTalisman, false);
					hoverText = "§7Accessory Bag\n§8Magical Power: §6" + mp
							+ "\n§8Selected Power: §a" + getStorageSelectedPower()
							+ "\n§8Stat bonus: §7+" + String.format("%.1f", MagicalPower.scaling(mp));
				} else {
					hoverText = STORAGE_LABELS[t];
				}
			}
		}

		int armX = guiL + 150, eqX = guiL + 170, loadY = guiT + 32;
		PvRender.drawString(g, "§7Armor", armX - 2, loadY - 10, 0xAAAAAA, true);

		for (int i = 0; i < 4; i++) {
			int ai = 3 - i;
			drawLoadoutSlot(g, ai < sArmor.size() ? sArmor.get(ai) : null, armX, loadY + i * 21);
			drawLoadoutSlot(g, i < sEquip.size() ? sEquip.get(i) : null, eqX, loadY + i * 21);
		}

		List<List<ItemData>> pageList = buildStoragePages();
		if (storagePage >= pageList.size()) storagePage = pageList.size() - 1;
		if (storagePage < 0) storagePage = 0;
		List<ItemData> items = pageList.get(storagePage);
		int cols = 9;
		int totalRows = Math.max(1, (items.size() + cols - 1) / cols);
		int maxRows = 6;
		int rows = Math.min(totalRows, maxRows);

		int slot = 18;
		int panelW = cols * slot + 14;
		int panelH = rows * slot + 17 + 7;
		int px = guiL + 320 - panelW / 2;
		int py = guiT + 101 - panelH / 2;

		g.fill(px, py, px + panelW, py + panelH, 0xFFC6C6C6);
		g.fill(px + 1, py + 1, px + panelW - 1, py + panelH - 1, 0xFF8B8B8B);

		PvRender.drawString(g, STORAGE_LABELS[storageType], px + 7, py + 6, 0x404040, false);

		if (!hasItems(items)) {

			String msg = hasItems(sInv) ? "§7Empty" : "§cInventory API disabled";
			PvRender.drawStringCentered(g, msg, guiL + 320, guiT + 101, true, 0);
		} else {
			int gx = px + 7, gy = py + 17;
			ItemData hov = null;
			for (int i = 0; i < items.size(); i++) {
				int col = i % cols, row = i / cols;
				if (row >= rows) break;
				int ix = gx + col * slot, iy = gy + row * slot;
				g.fill(ix, iy, ix + 16, iy + 16, 0xFF373737);
				ItemData it = items.get(i);
				if (it == null || it.isEmpty()) continue;
				com.eggman.pv.util.ItemIcon.draw(g, it, ix, iy);
				if (it.count > 1) {
					PvRender.drawString(g, String.valueOf(it.count), ix + 16 - PvRender.font().width(String.valueOf(it.count)), iy + 9, 0xFFFFFF, true);
				}
				if (hoverMouseX >= ix && hoverMouseX <= ix + 16 && hoverMouseY >= iy && hoverMouseY <= iy + 16) {
					hov = it;
				}
			}
			if (hov != null) {
				StringBuilder sb = new StringBuilder(hov.name);
				for (String l : hov.lore) sb.append("\n").append(l);
				hoverText = sb.toString();
			}
		}

		int pages = pageList.size();
		if (pages > 1) {
			int selH = py + panelH + 4;
			if (storagePage > 0) {
				drawPageArrow(g, guiL + 320 - 14, selH, "<");
				storageBtnRects.add(new int[]{guiL + 320 - 14, selH, 12, 14, 100});
			}
			if (storagePage < pages - 1) {
				drawPageArrow(g, guiL + 320 + 2, selH, ">");
				storageBtnRects.add(new int[]{guiL + 320 + 2, selH, 12, 14, 101});
			}
			String txt = "Page " + (storagePage + 1) + "/" + pages;
			PvRender.drawStringCentered(g, txt, guiL + 320, selH - 9, false, 0x909090);
		}
	}

	private static final ResourceLocation PANORAMA = PvRender.tex("panorama.png");
	private static final int PANO_W = 512, PANO_H = 288;

	private void drawPanoramaBg(GuiGraphics g, int x1, int y1, int x2, int y2) {
		int w = x2 - x1, h = y2 - y1;
		int dispW = (int) (h * (PANO_W / (float) PANO_H));
		if (dispW <= 0) dispW = w;
		int scroll = (int) ((System.currentTimeMillis() / 40) % dispW);
		g.enableScissor(x1, y1, x2, y2);
		for (int dx = -scroll; dx < w; dx += dispW) {
			PvRender.drawTexturedRect(g, PANORAMA, x1 + dx, y1, dispW, h, 0f, 0f, PANO_W, PANO_H, PANO_W, PANO_H);
		}
		g.disableScissor();

		g.fill(x1, y1, x2, y1 + 1, 0xFF000000);
		g.fill(x1, y2 - 1, x2, y2, 0xFF000000);
		g.fill(x1, y1, x1 + 1, y2, 0xFF000000);
		g.fill(x2 - 1, y1, x2, y2, 0xFF000000);
	}

	private void drawLoadoutSlot(GuiGraphics g, ItemData it, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
		g.fill(x, y, x + 16, y + 16, 0xFF373737);
		if (it == null || it.isEmpty()) return;
		com.eggman.pv.util.ItemIcon.draw(g, it, x, y);
		if (hoverMouseX >= x && hoverMouseX <= x + 16 && hoverMouseY >= y && hoverMouseY <= y + 16) {
			StringBuilder sb = new StringBuilder(it.name);
			for (String l : it.lore) sb.append("\n").append(l);
			hoverText = sb.toString();
		}
	}

	private void drawPageArrow(GuiGraphics g, int x, int y, String s) {
		g.fill(x, y, x + 12, y + 14, 0xFF2A2A2A);
		PvRender.drawStringCentered(g, s, x + 6, y + 3, false, 0xFFFFFF);
	}

	private String getStorageSelectedPower() {
		JsonObject sel = getSelectedProfile();
		if (sel != null && sel.has("accessories") && sel.get("accessories").isJsonObject()) {
			String p = getString(sel.getAsJsonObject("accessories"), "selected_power", "");
			if (!p.isEmpty()) return p.substring(0, 1).toUpperCase() + p.substring(1);
		}
		return "None";
	}

	private int rarityColor(String rarity) {
		switch (rarity) {
			case "MYTHIC":       return 0xFF55FF;
			case "LEGENDARY":    return 0xFFAA00;
			case "EPIC":         return 0xAA00AA;
			case "RARE":         return 0x5555FF;
			case "UNCOMMON":     return 0x55FF55;
			case "VERY SPECIAL":
			case "SPECIAL":      return 0xFF5555;
			default:             return 0xFFFFFF;
		}
	}

	private void renderBasicPage(GuiGraphics g) {
		JsonObject selected = getSelectedProfile();

		if (selected == null) {
			PvRender.drawStringCentered(
					g,
					"No SkyBlock profile found",
					guiLeft + SIZE_X / 2f,
					guiTop + SIZE_Y / 2f,
					true,
					0xFF5555
			);
			return;
		}

		String displayName = getString(data, "displayname", username);
		String rank = formatRank(getString(data, "rank", "DEFAULT"));
		String profileName = getString(selected, "cute_name", "Unknown");
		String gameMode = getString(selected, "game_mode", "normal");

		double purse = getDouble(selected, "purse", 0);
		double sbXp = getDouble(selected, "skyblock_level_xp", 0);

		JsonObject skills = selected.getAsJsonObject("skills");

		int x = guiLeft;
		int y = guiTop;

		drawLeftInfo(g, x, y, displayName, rank, profileName, gameMode, purse, sbXp);
		drawCenterPanel(g, x, y, sbXp, getSkillXp(skills, "social"));
		drawSkills(g, x, y, skills);
	}

	private void drawLeftInfo(
			GuiGraphics g,
			int x,
			int y,
			String displayName,
			String rank,
			String profileName,
			String gameMode,
			double purse,
			double sbXp
	) {

		int rankColor = getRankColor(rank);
		int nameX = x + 10;
		int yName = y + 38;

		JsonObject nwSel = getSelectedProfile();
		if (nwSel != null && nwSel.has("networth") && nwSel.get("networth").isJsonObject()) {
			JsonObject nw = nwSel.getAsJsonObject("networth");
			double total = getDouble(nw, "total", 0);
			int bx = nameX, by = yName - 16;
			g.fill(bx - 2, by - 2, bx + 132, by + 11, 0xC0000000);
			PvRender.drawString(g, "§6✎ Networth: §e" + formatNumber(total), bx, by, 0xFFAA00, true);
			if (hoverMouseX >= bx - 2 && hoverMouseX <= bx + 132 && hoverMouseY >= by - 2 && hoverMouseY <= by + 11) {
				StringBuilder sb = new StringBuilder();
				sb.append("§6§lNetworth: §e").append(formatNumber(total));
				sb.append("\n§7Purse: §6").append(formatNumber(getDouble(nw, "purse", 0)));
				sb.append("\n§7Bank: §6").append(formatNumber(getDouble(nw, "bank", 0)));
				if (nw.has("types") && nw.get("types").isJsonObject()) {
					JsonObject nwTypes = nw.getAsJsonObject("types");
					java.util.List<String> tkeys = new java.util.ArrayList<>(nwTypes.keySet());
					java.util.Collections.sort(tkeys);
					for (String tk : tkeys) {
						double tv = getDouble(nwTypes, tk, 0);
						StringBuilder lbl = new StringBuilder();
						for (String part : tk.split("_")) {
							if (part.isEmpty()) continue;
							if (lbl.length() > 0) lbl.append(' ');
							lbl.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
						}
						sb.append("\n§7").append(lbl).append(": §6").append(formatNumber(tv));
					}
				}
				sb.append("\n§7Unsoulbound: §6").append(formatNumber(getDouble(nw, "unsoulbound", 0)));
				hoverText = sb.toString();
			}
		}

		if (!rank.isEmpty()) {
			PvRender.drawString(g, rank, nameX, yName, rankColor, true);
			int rankW = PvRender.font().width(rank + " ");
			PvRender.drawString(g, displayName, nameX + rankW, yName, rankColor, true);
		} else {
			PvRender.drawString(g, displayName, nameX, yName, rankColor, true);
		}

		int sbx1 = x + 10, sby1 = y + 50, sbx2 = x + 80, sby2 = y + 172;
		drawPanoramaBg(g, sbx1, sby1, sbx2, sby2);
		String uuid = getString(data, "uuid", "");
		String skinValue = getString(data, "skin_value", "");
		String skinSignature = getString(data, "skin_signature", "");
		com.eggman.pv.util.PlayerModelRender.render(
				g, sbx1 + 2, sby1 + 2, sbx2 - 2, sby2 - 2, 60,
				hoverMouseX, hoverMouseY, uuid, username, skinValue, skinSignature);

		g.fill(x + 8, y + 181, x + 116, y + 198, 0xFF1C1C1C);
		g.fill(x + 9, y + 182, x + 115, y + 197, 0xFF2A2A2A);

		drawProfileIcon(g, gameMode, x + 14, y + 185);
		PvRender.drawString(g, profileName, x + 29, y + 187, 0x55FFFF, true);
		PvRender.drawString(g, profileDropdownOpen ? "v" : ">", x + 105, y + 187, 0xFFFFFF, true);

	}

	private void drawCenterPanel(GuiGraphics g, int x, int y, double sbXp, double socialXp) {
		int sbLevel = (int) (sbXp / 100.0);
		int xpInLevel = (int) (sbXp % 100);
		double progress = xpInLevel / 100.0;
		int levelColor = getSbLevelColor(sbLevel);

		int px = x + 140;
		int py = y + 40;
		int pw = 100;
		int ph = 120;
		g.fill(px, py, px + pw, py + ph, 0x66000000);

		int cx = px + pw / 2;

		PvRender.drawStringCentered(g, String.valueOf(sbLevel), cx, py + 22, true, levelColor);

		PvRender.drawStringCentered(g, xpInLevel + "/100", cx, py + 48, true, 0xFFFFFF);

		int barX = px + 12;
		int barY = py + 60;
		int barW = pw - 24;
		int barH = 5;
		g.fill(barX, barY, barX + barW, barY + barH, 0xFF2A002A);
		int filled = (int) (barW * progress);
		g.fill(barX, barY, barX + filled, barY + barH, 0xFFFF00FF);

		g.fill(barX, barY, barX + barW, barY + 1, 0xFF000000);
		g.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0xFF000000);

		com.eggman.pv.util.SkillXp.Level social =
			com.eggman.pv.util.SkillXp.getLevel(socialXp, com.eggman.pv.util.SkillXp.Type.SOCIAL, 25);
		PvRender.drawStringCentered(g, "Social " + social.level, cx, py + 78, true, 0x55FF55);
		int sBarX = px + 12, sBarY = py + 90, sBarW = pw - 24;
		g.fill(sBarX, sBarY, sBarX + sBarW, sBarY + 5, 0xFF2A002A);
		int sFilled = (int) (sBarW * social.progress);
		g.fill(sBarX, sBarY, sBarX + sFilled, sBarY + 5, 0xFFFF00FF);
	}

	private int getSbLevelColor(int level) {
		if (level >= 480) return 0xAA0000;
		if (level >= 440) return 0xFF5555;
		if (level >= 400) return 0xFFAA00;
		if (level >= 360) return 0xAA00AA;
		if (level >= 320) return 0xFF55FF;
		if (level >= 280) return 0x5555FF;
		if (level >= 240) return 0x00AAAA;
		if (level >= 200) return 0x55FFFF;
		if (level >= 160) return 0x00AA00;
		if (level >= 120) return 0x55FF55;
		if (level >= 80)  return 0xFFFF55;
		if (level >= 40)  return 0xFFFFFF;
		return 0xAAAAAA;
	}

	private void drawSkills(GuiGraphics g, int x, int y, JsonObject skills) {
		int panelX = x + 247;
		int panelY = y + 35;

		g.fill(panelX - 3, panelY - 4, panelX + 165, panelY + 124, 0x66000000);

		drawSkill(g, "Taming", "taming", getSkillXp(skills, "taming"), 60, panelX, panelY, 0xFF55FF);
		drawSkill(g, "Mining", "mining", getSkillXp(skills, "mining"), 60, panelX, panelY + 22, 0xAAAAAA);
		drawSkill(g, "Foraging", "foraging", getSkillXp(skills, "foraging"), 60, panelX, panelY + 44, 0x55FF55);
		drawSkill(g, "Enchanting", "enchanting", getSkillXp(skills, "enchanting"), 60, panelX, panelY + 66, 0x55FF55);
		drawSkill(g, "Carpentry", "carpentry", getSkillXp(skills, "carpentry"), 50, panelX, panelY + 88, 0xAA5500);
		drawSkill(g, "Farming", "farming", getSkillXp(skills, "farming"), 60, panelX, panelY + 110, 0xFFFF55);

		int rightX = panelX + 86;

		drawSkill(g, "Fishing", "fishing", getSkillXp(skills, "fishing"), 50, rightX, panelY, 0x55FFFF);
		drawSkill(g, "Alchemy", "alchemy", getSkillXp(skills, "alchemy"), 50, rightX, panelY + 22, 0xAA00AA);
		drawSkill(g, "Runecraft", "runecrafting", getSkillXp(skills, "runecrafting"), 25, rightX, panelY + 44, 0xFF55FF);
		drawSkill(g, "Hunting", "hunting", getSkillXp(skills, "hunting"), 60, rightX, panelY + 66, 0xFFAA00);
		drawSkill(g, "Dungeons", "dungeoneering", getSkillXp(skills, "dungeoneering"), 50, rightX, panelY + 88, 0x55FFFF);
		drawSkill(g, "Combat", "combat", getSkillXp(skills, "combat"), 60, rightX, panelY + 110, 0xFF5555);
	}

	private void drawSkill(GuiGraphics g, String name, String skillKey, double xp, int maxLevel, int x, int y, int color) {
		com.eggman.pv.util.SkillXp.Level level = com.eggman.pv.util.SkillXp.getLevel(xp, com.eggman.pv.util.SkillXp.typeFor(skillKey), maxLevel);

		PvRender.drawString(g, name, x, y, color, true);
		PvRender.drawString(g, String.valueOf(level.level), x + 54, y, 0xFFFFFF, true);

		int barX = x;
		int barY = y + 10;
		int barW = 70;
		int barH = 5;

		g.fill(barX, barY, barX + barW, barY + barH, 0xFF2A002A);

		int filled = (int) (barW * level.progress);
		if (filled < 0) filled = 0;
		if (filled > barW) filled = barW;

		boolean maxed = level.level >= maxLevel;

		if (maxed) {

			for (int i = 0; i < barW; i++) {
				float hue = (i / (float) barW);
				int rgb = hsvToRgb(hue, 0.9f, 1.0f);
				g.fill(barX + i, barY, barX + i + 1, barY + barH, 0xFF000000 | (rgb & 0xFFFFFF));
			}
		} else {
			g.fill(barX, barY, barX + filled, barY + barH, 0xFFFF00FF);
		}

		g.fill(barX, barY, barX + barW, barY + 1, 0xFF000000);
		g.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0xFF000000);
		g.fill(barX, barY, barX + 1, barY + barH, 0xFF000000);
		g.fill(barX + barW - 1, barY, barX + barW, barY + barH, 0xFF000000);

		String tip = maxed ? (name + " MAX (" + formatNumber(xp) + " XP)")
			: (name + ": " + formatNumber(xp) + " XP (" + (int)(level.progress * 100) + "%)");
		checkHover(barX, y, barW, barH + 12, tip);
	}

	private void renderProfileDropdown(GuiGraphics g) {
		if (!profileDropdownOpen || data == null || !data.has("profiles") || !data.get("profiles").isJsonArray()) {
			return;
		}

		JsonArray profiles = data.getAsJsonArray("profiles");

		int x = guiLeft + 8;
		int y = guiTop + 199;
		int width = 132;
		int rowHeight = 16;
		int height = profiles.size() * rowHeight + 2;

		g.fill(x, y, x + width, y + height, 0xDD000000);

		for (int i = 0; i < profiles.size(); i++) {
			JsonObject profile = profiles.get(i).getAsJsonObject();

			String name = getString(profile, "cute_name", "Unknown");
			String mode = getString(profile, "game_mode", "normal");

			int rowY = y + 1 + i * rowHeight;

			if (i == selectedProfileIndex) {
				g.fill(x + 1, rowY, x + width - 1, rowY + rowHeight - 1, 0xAA333333);
			} else {
				g.fill(x + 1, rowY, x + width - 1, rowY + rowHeight - 1, 0xAA111111);
			}

			drawProfileIcon(g, mode, x + 5, rowY + 2);
			PvRender.drawString(g, name, x + 20, rowY + 4, 0x55FFFF, true);
			PvRender.drawString(g, formatGameModeShort(mode), x + 90, rowY + 4, 0xAAAAAA, true);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();

		if (button == 0) {

			int[] sb = searchBoxRect();
			if (isInside(mouseX, mouseY, sb[0], sb[1], sb[2], sb[3])) {
				searchFocused = true;
				return true;
			}
			searchFocused = false;

			Page[] pages = Page.values();
			int tabW = 28, tabH = 22;
			int startX = guiLeft + 5;
			int tabY = guiTop - tabH + 4;
			for (int i = 0; i < pages.length; i++) {
				int tx = startX + i * (tabW + 1);
				if (isInside(mouseX, mouseY, tx, tabY, tabW, tabH)) {
					currentPage = pages[i];
					profileDropdownOpen = false;
					return true;
				}
			}

			if (currentPage == Page.DUNGEON) {
				int btnY = guiTop + 22;

				if (isInside(mouseX, mouseY, guiLeft + 8, btnY, 62, 15)) {
					dungeonMasterMode = false; cataCalcResult = null; return true;
				}
				if (isInside(mouseX, mouseY, guiLeft + 74, btnY, 62, 15)) {
					dungeonMasterMode = true; cataCalcResult = null; return true;
				}

				int ly = guiTop + 38;
				int inX = guiLeft + 14, inY = ly + 24, inW = 30, inH = 14;
				if (isInside(mouseX, mouseY, inX, inY, inW, inH)) {
					cataInputFocused = true; return true;
				}
				int calcX = inX + inW + 4, calcW = 56;
				if (isInside(mouseX, mouseY, calcX, inY, calcW, inH)) {
					doCataCalculate();
					return true;
				}
				cataInputFocused = false;
			}

			if (currentPage == Page.INVENTORY) {
				for (int[] r : storageBtnRects) {
					if (isInside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
						int code = r[4];
						if (code == 100) { storagePage = Math.max(0, storagePage - 1); }
						else if (code == 101) { storagePage++; }
						else { storageType = code; storagePage = 0; }
						return true;
					}
				}
			}

			if (currentPage == Page.RIFT) {
				for (int[] r : riftBtnRects) {
					if (isInside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
						int code = r[4];
						if (code == 100) { riftPage = Math.max(0, riftPage - 1); }
						else if (code == 101) { riftPage++; }
						else { riftStorageType = code; riftPage = 0; }
						return true;
					}
				}
			}

			if (currentPage == Page.COLLECTIONS) {
				for (int[] r : colBtnRects) {
					if (isInside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
						int code = r[4];
						if (code == 200) { colPage = Math.max(0, colPage - 1); }
						else if (code == 201) { colPage++; }
						else { colCatIndex = code; colPage = 0; }
						return true;
					}
				}
			}

			if (currentPage == Page.PETS) {
				for (int[] r : petBtnRects) {
					if (isInside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
						int code = r[4];
						if (code == 300) petPage = Math.max(0, petPage - 1);
						else if (code == 301) petPage++;
						else if (code == 310) petSort = (petSort + 1) % 3;
						else petSelected = code;
						return true;
					}
				}
			}

			if (currentPage == Page.MINING) {
				for (int[] r : miningBtnRects) {
					if (isInside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
						if (r[4] == 400) miningScroll = Math.max(0, miningScroll - 1);
						else if (r[4] == 401) miningScroll++;
						return true;
					}
				}
			}

			if (currentPage == Page.BESTIARY) {
				for (int[] r : bestiaryBtnRects) {
					if (isInside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
						bestiaryIsland = r[4];
						return true;
					}
				}
			}

			int profileButtonX = guiLeft + 8;
			int profileButtonY = guiTop + 181;
			int profileButtonW = 108;
			int profileButtonH = 17;

			if (isInside(mouseX, mouseY, profileButtonX, profileButtonY, profileButtonW, profileButtonH)) {
				profileDropdownOpen = !profileDropdownOpen;
				return true;
			}

			if (profileDropdownOpen && data != null && data.has("profiles") && data.get("profiles").isJsonArray()) {
				JsonArray profiles = data.getAsJsonArray("profiles");

				int x = guiLeft + 8;
				int y = guiTop + 199;
				int width = 132;
				int rowHeight = 16;

				for (int i = 0; i < profiles.size(); i++) {
					int rowY = y + 1 + i * rowHeight;

					if (isInside(mouseX, mouseY, x, rowY, width, rowHeight)) {
						selectedProfileIndex = i;
						profileDropdownOpen = false;
						return true;
					}
				}
			}

			profileDropdownOpen = false;
		}

		return super.mouseClicked(event, doubleClick);
	}

	private JsonObject getSelectedProfile() {
		if (data == null || !data.has("profiles") || !data.get("profiles").isJsonArray()) {
			return null;
		}

		JsonArray profiles = data.getAsJsonArray("profiles");

		if (profiles.isEmpty()) {
			return null;
		}

		if (selectedProfileIndex < 0) {
			selectedProfileIndex = 0;
		}

		if (selectedProfileIndex >= profiles.size()) {
			selectedProfileIndex = profiles.size() - 1;
		}

		return profiles.get(selectedProfileIndex).getAsJsonObject();
	}

	private int getBestProfileIndex(JsonObject json) {
		if (json == null || !json.has("profiles") || !json.get("profiles").isJsonArray()) {
			return 0;
		}

		JsonArray profiles = json.getAsJsonArray("profiles");

		if (profiles.isEmpty()) {
			return 0;
		}

		int bestIndex = 0;
		double bestScore = -1;

		for (int i = 0; i < profiles.size(); i++) {
			JsonObject profile = profiles.get(i).getAsJsonObject();

			double sbXp = getDouble(profile, "skyblock_level_xp", 0);
			double purse = getDouble(profile, "purse", 0);

			double score = sbXp * 1_000_000 + purse;

			if (score > bestScore) {
				bestScore = score;
				bestIndex = i;
			}
		}

		return bestIndex;
	}

	private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
	}

	private void drawProfileIcon(GuiGraphics g, String gameMode, int x, int y) {
		String mode = (gameMode == null) ? "normal" : gameMode.toLowerCase();
		String file;
		switch (mode) {
			case "ironman": file = "ironman"; break;
			case "bingo": file = "bingo"; break;
			case "stranded": file = "stranded"; break;
			case "island": file = "island"; break;
			default: file = "normal"; break;
		}
		ResourceLocation icon = PvRender.tex("profile/" + file + ".png");
		try {
			PvRender.drawTexturedRect(g, icon, x, y, 12, 12, 12, 12);
		} catch (Throwable t) {

			PvRender.drawString(g, getProfileIcon(gameMode), x + 1, y + 2, 0xFFFFFF, true);
		}
	}

	private String getProfileIcon(String gameMode) {
		if (gameMode == null) {
			return "■";
		}

		String mode = gameMode.toLowerCase();

		if (mode.equals("ironman")) {
			return "▣";
		}

		if (mode.equals("bingo")) {
			return "✪";
		}

		if (mode.equals("stranded") || mode.equals("island")) {
			return "◆";
		}

		return "■";
	}

	private String formatGameModeShort(String gameMode) {
		if (gameMode == null || gameMode.equals("normal")) {
			return "";
		}

		String mode = gameMode.toLowerCase();

		if (mode.equals("ironman")) {
			return "IM";
		}

		if (mode.equals("bingo")) {
			return "B";
		}

		if (mode.equals("stranded")) {
			return "S";
		}

		return mode;
	}

	private double getSkillXp(JsonObject skills, String name) {
		if (skills == null || !skills.has(name) || skills.get(name).isJsonNull()) {
			return 0;
		}

		return skills.get(name).getAsDouble();
	}

	private String getString(JsonObject obj, String key, String fallback) {
		if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
			return fallback;
		}

		return obj.get(key).getAsString();
	}

	private double getDouble(JsonObject obj, String key, double fallback) {
		if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
			return fallback;
		}

		return obj.get(key).getAsDouble();
	}

	private String formatNumber(double value) {
		if (value >= 1_000_000_000) {
			return String.format("%.2fb", value / 1_000_000_000.0);
		}

		if (value >= 1_000_000) {
			return String.format("%.2fm", value / 1_000_000.0);
		}

		if (value >= 1_000) {
			return String.format("%.2fk", value / 1_000.0);
		}

		return String.valueOf((int) value);
	}

	private static int hsvToRgb(float h, float s, float v) {
		float r = 0, g = 0, b = 0;
		int i = (int) (h * 6);
		float f = h * 6 - i;
		float p = v * (1 - s);
		float q = v * (1 - f * s);
		float t = v * (1 - (1 - f) * s);
		switch (i % 6) {
			case 0: r = v; g = t; b = p; break;
			case 1: r = q; g = v; b = p; break;
			case 2: r = p; g = v; b = t; break;
			case 3: r = p; g = q; b = v; break;
			case 4: r = t; g = p; b = v; break;
			case 5: r = v; g = p; b = q; break;
		}
		return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}

	private int getRankColor(String formattedRank) {
		if (formattedRank == null || formattedRank.isEmpty()) return 0xAAAAAA;
		String r = formattedRank.toUpperCase();
		if (r.contains("MVP++")) return 0xFFAA00;
		if (r.contains("MVP")) return 0x55FFFF;
		if (r.contains("VIP")) return 0x55FF55;
		if (r.contains("ADMIN")) return 0xFF5555;
		if (r.contains("YOUTUBE")) return 0xFF5555;
		if (r.contains("OWNER")) return 0xFF5555;
		return 0xAAAAAA;
	}

	private String formatRank(String rank) {
		if (rank == null || rank.isBlank()) return "";

		if (rank.contains("[") || rank.indexOf('\u00a7') >= 0) {
			String cleaned = rank.replaceAll("\u00a7.", "").trim();
			return cleaned;
		}

		switch (rank) {
			case "MVP_PLUS": return "[MVP+]";
			case "SUPERSTAR": return "[MVP++]";
			case "MVP": return "[MVP]";
			case "VIP_PLUS": return "[VIP+]";
			case "VIP": return "[VIP]";
			case "ADMIN": return "[ADMIN]";
			case "YOUTUBER": return "[YOUTUBE]";
			default: return "";
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public int getGuiLeft() {
		return guiLeft;
	}

	public int getGuiTop() {
		return guiTop;
	}

	public String getUsername() {
		return username;
	}
}
