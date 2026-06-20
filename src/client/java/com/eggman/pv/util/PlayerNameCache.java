package com.eggman.pv.util;

import net.minecraft.client.Minecraft;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class PlayerNameCache {

	private static final LinkedHashSet<String> NAMES = new LinkedHashSet<>();
	private static final int MAX = 200;

	public static void add(String n) {
		if (n == null) return;
		n = n.trim();
		if (n.isEmpty() || n.length() > 16 || !n.matches("[A-Za-z0-9_]+")) return;
		NAMES.remove(n);
		NAMES.add(n);
		while (NAMES.size() > MAX) {
			Iterator<String> it = NAMES.iterator();
			it.next();
			it.remove();
		}
	}

	public static void refreshOnline() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) return;
		try {
			for (var p : mc.getConnection().getOnlinePlayers()) {
				add(p.getProfile().name());
			}
		} catch (Exception ignored) {}
	}

	public static String complete(String prefix) {
		if (prefix == null || prefix.isEmpty()) return null;
		String lp = prefix.toLowerCase();
		for (String n : NAMES) {
			if (n.length() > prefix.length() && n.toLowerCase().startsWith(lp)) return n;
		}
		return null;
	}

	public static Set<String> names() { return NAMES; }
}
