/*
 * Copyright (C) 2022-2024 NotEnoughUpdates contributors
 * Derivative work under GNU LGPL-3.0-or-later.
 */
package com.example.pv.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.example.pv.data.Level;

public class PvCalc {

	public static JsonElement getElement(JsonElement element, String path) {
		if (element instanceof JsonObject) {
			int dot = path.indexOf('.');
			String head = dot >= 0 ? path.substring(0, dot) : path;
			String tail = dot >= 0 ? path.substring(dot + 1) : null;
			JsonElement e = element.getAsJsonObject().get(head);
			if (tail != null && !tail.isEmpty()) {
				return getElement(e, tail);
			}
			return e;
		}
		return element;
	}

	public static float getElementAsFloat(JsonElement element, float def) {
		if (element == null || !element.isJsonPrimitive()) return def;
		JsonPrimitive prim = element.getAsJsonPrimitive();
		return prim.isNumber() ? prim.getAsFloat() : def;
	}

	public static int getElementAsInt(JsonElement element, int def) {
		if (element == null || !element.isJsonPrimitive()) return def;
		JsonPrimitive prim = element.getAsJsonPrimitive();
		return prim.isNumber() ? prim.getAsInt() : def;
	}

	public static long getElementAsLong(JsonElement element, long def) {
		if (element == null || !element.isJsonPrimitive()) return def;
		JsonPrimitive prim = element.getAsJsonPrimitive();
		return prim.isNumber() ? prim.getAsLong() : def;
	}

	public static String getElementAsString(JsonElement element, String def) {
		if (element == null || !element.isJsonPrimitive()) return def;
		JsonPrimitive prim = element.getAsJsonPrimitive();
		return prim.isString() ? prim.getAsString() : def;
	}

	public static boolean getElementAsBool(JsonElement element, boolean def) {
		if (element == null || !element.isJsonPrimitive()) return def;
		JsonPrimitive prim = element.getAsJsonPrimitive();
		return prim.isBoolean() ? prim.getAsBoolean() : def;
	}

	public static int getLevelingCap(JsonObject leveling, String skillName) {
		JsonElement capsElement = getElement(leveling, "leveling_caps");
		return capsElement != null && capsElement.isJsonObject() && capsElement.getAsJsonObject().has(skillName)
			? capsElement.getAsJsonObject().get(skillName).getAsInt()
			: 50;
	}

	public static Level getLevel(JsonArray levelingArray, float xp, int levelCap, boolean cumulative) {
		Level levelObj = new Level();
		levelObj.totalXp = xp;
		levelObj.maxLevel = levelCap;

		for (int level = 0; level < levelingArray.size(); level++) {
			float levelXp = levelingArray.get(level).getAsFloat();

			if (levelXp > xp) {
				if (cumulative) {
					float previous = level > 0 ? levelingArray.get(level - 1).getAsFloat() : 0;
					levelObj.maxXpForLevel = (levelXp - previous);
					levelObj.level = 1 + level + (xp - levelXp) / levelObj.maxXpForLevel;
				} else {
					levelObj.maxXpForLevel = levelXp;
					levelObj.level = level + xp / levelXp;
				}

				if (levelObj.level >= levelCap) {
					levelObj.level = levelCap;
					levelObj.maxed = true;
				}
				return levelObj;
			} else if (!cumulative) {
				xp -= levelXp;
			}
		}

		levelObj.level = Math.min(levelingArray.size(), levelCap);
		levelObj.maxed = true;
		return levelObj;
	}
}
