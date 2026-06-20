package com.eggman.pv.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepoItems {

	private static final Pattern VALUE = Pattern.compile("Value:\\\\?\"([A-Za-z0-9+/=]+)\\\\?\"");

	public static String skull(String internalName) {
		JsonObject o = RepoManager.getItem(internalName);
		if (o == null || !o.has("nbttag")) return null;
		Matcher m = VALUE.matcher(o.get("nbttag").getAsString());
		return m.find() ? m.group(1) : null;
	}

	public static List<String> lore(String internalName) {
		JsonObject o = RepoManager.getItem(internalName);
		if (o == null || !o.has("lore")) return null;
		JsonArray a = o.getAsJsonArray("lore");
		List<String> out = new ArrayList<>();
		for (int i = 0; i < a.size(); i++) out.add(a.get(i).getAsString());
		return out;
	}

	public static String displayName(String internalName) {
		JsonObject o = RepoManager.getItem(internalName);
		return (o != null && o.has("displayname")) ? o.get("displayname").getAsString() : null;
	}
}
