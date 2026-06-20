package com.eggman.pv.util;

import net.minecraft.resources.ResourceLocation;

public class BossHeads {

	private static final ResourceLocation[] FLOORS = {
			PvRender.tex("profile/boss/bonzo.png"),
			PvRender.tex("profile/boss/scarf.png"),
			PvRender.tex("profile/boss/professor.png"),
			PvRender.tex("profile/boss/thorn.png"),
			PvRender.tex("profile/boss/livid.png"),
			PvRender.tex("profile/boss/sadan.png"),
			PvRender.tex("profile/boss/necron.png")
	};

	public static ResourceLocation getFloor(int floor) {
		if (floor < 1 || floor > 7) return null;
		return FLOORS[floor - 1];
	}

	public static ResourceLocation getBonzo() {
		return FLOORS[0];
	}

	public static ResourceLocation getScarf() {
		return FLOORS[1];
	}

	public static ResourceLocation getProfessor() {
		return FLOORS[2];
	}

	public static ResourceLocation getThorn() {
		return FLOORS[3];
	}

	public static ResourceLocation getLivid() {
		return FLOORS[4];
	}

	public static ResourceLocation getSadan() {
		return FLOORS[5];
	}

	public static ResourceLocation getNecron() {
		return FLOORS[6];
	}
}
