package com.eggman.pv.util;

import net.minecraft.resources.Identifier;

public class BossHeads {

	private static final Identifier[] FLOORS = {
			PvRender.tex("profile/boss/bonzo.png"),
			PvRender.tex("profile/boss/scarf.png"),
			PvRender.tex("profile/boss/professor.png"),
			PvRender.tex("profile/boss/thorn.png"),
			PvRender.tex("profile/boss/livid.png"),
			PvRender.tex("profile/boss/sadan.png"),
			PvRender.tex("profile/boss/necron.png")
	};

	public static Identifier getFloor(int floor) {
		if (floor < 1 || floor > 7) return null;
		return FLOORS[floor - 1];
	}

	public static Identifier getBonzo() {
		return FLOORS[0];
	}

	public static Identifier getScarf() {
		return FLOORS[1];
	}

	public static Identifier getProfessor() {
		return FLOORS[2];
	}

	public static Identifier getThorn() {
		return FLOORS[3];
	}

	public static Identifier getLivid() {
		return FLOORS[4];
	}

	public static Identifier getSadan() {
		return FLOORS[5];
	}

	public static Identifier getNecron() {
		return FLOORS[6];
	}
}
