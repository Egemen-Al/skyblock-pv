/*
 * Copyright (C) 2022-2024 NotEnoughUpdates contributors
 * Derivative work under GNU LGPL-3.0-or-later.
 */
package com.example.pv.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

import com.example.pv.SkyblockPvClient;

public class PvRender {

	public static ResourceLocation tex(String name) {
		return ResourceLocation.fromNamespaceAndPath(SkyblockPvClient.MOD_ID, "textures/gui/" + name);
	}

	public static void drawTexturedRect(GuiGraphics g, ResourceLocation texture,
	                                    int x, int y, int width, int height,
	                                    int texW, int texH) {
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, width, height, texW, texH);
	}

	public static void drawTexturedRect(GuiGraphics g, ResourceLocation texture,
	                                    int x, int y, int width, int height,
	                                    float u, float v, int regionW, int regionH,
	                                    int texW, int texH) {
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionW, regionH, texW, texH);
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	public static void drawStringCentered(GuiGraphics g, String str, float x, float y,
	                                      boolean shadow, int colour) {
		Font fr = font();
		int w = fr.width(str);
		int drawX = (int) (x - w / 2f);
		int drawY = (int) (y - fr.lineHeight / 2f);

		g.drawString(fr, str, drawX, drawY, fixAlpha(colour), shadow);
	}

	public static void drawString(GuiGraphics g, String str, int x, int y, int colour, boolean shadow) {
		g.drawString(font(), str, x, y, fixAlpha(colour), shadow);
	}

	public static void renderAlignedString(GuiGraphics g, String first, String second,
	                                       int x, int y, int length) {
		Font fr = font();

		int color = fixAlpha(0x404040);

		g.drawString(fr, first, x, y, color, false);

		int secondLen = fr.width(second);
		g.drawString(fr, second, x + length - secondLen, y, color, false);
	}

	private static int fixAlpha(int colour) {
		if ((colour & 0xFF000000) == 0) {
			return colour | 0xFF000000;
		}

		return colour;
	}
}
