package com.eggman.pv.util;

public class SkillXp {

	public static class Level {
		public final int level;
		public final double progress;
		public Level(int level, double progress) {
			this.level = level;
			this.progress = progress;
		}
	}

	public enum Type { NORMAL, RUNECRAFTING, SOCIAL, CATACOMBS }

	public static final double CATACOMBS_OVERFLOW_XP = 200_000_000.0;

	public static final int CATACOMBS_OVERFLOW_MAX = 200;

	private static final double[] NORMAL = {
		0, 50, 175, 375, 675, 1175, 1925, 2925, 4425, 6425, 9925, 14925, 22425,
		32425, 47425, 67425, 97425, 147425, 222425, 322425, 522425, 822425,
		1222425, 1722425, 2322425, 3022425, 3822425, 4722425, 5722425, 6822425,
		8022425, 9322425, 10722425, 12222425, 13822425, 15522425, 17322425,
		19222425, 21222425, 23322425, 25522425, 27822425, 30222425, 32722425,
		35322425, 38072425, 40972425, 44072425, 47472425, 51172425, 55172425,
		59472425, 64072425, 68972425, 74172425, 79672425, 85472425, 91572425,
		97972425, 104672425, 111672425
	};

	private static final double[] RUNECRAFTING = {
		0, 50, 100, 125, 160, 200, 250, 315, 400, 500, 625, 785, 1000, 1250, 1600,
		2000, 2465, 3125, 4000, 5000, 6200, 7800, 9800, 12200, 15300, 19050
	};

	private static final double[] SOCIAL = {
		0, 50, 150, 300, 550, 1050, 1800, 2800, 4050, 5550, 7550, 10050, 13050,
		16800, 21300, 27300, 35300, 45300, 57800, 72800, 92800, 117800, 147800,
		182800, 222800, 272800
	};

	private static final double[] CATACOMBS = {
		0, 50, 125, 235, 395, 625, 955, 1425, 2095, 3045, 4385, 6275, 8940, 12700,
		17960, 25340, 35640, 50040, 70040, 97640, 135640, 188140, 259640, 356640,
		488640, 668640, 911640, 1239640, 1684640, 2284640, 3084640, 4149640,
		5559640, 7459640, 9959640, 13259640, 17559640, 23159640, 30359640,
		39559640, 51559640, 66559640, 85559640, 109559640, 139559640, 177559640,
		225559640, 285559640, 360559640, 453559640, 569809640
	};

	public static Type typeFor(String skillKey) {
		switch (skillKey) {
			case "runecrafting": return Type.RUNECRAFTING;
			case "social": return Type.SOCIAL;
			case "dungeoneering": return Type.CATACOMBS;
			default: return Type.NORMAL;
		}
	}

	public static double xpForLevel(int targetLevel, Type type) {
		double[] cumulative = tableFor(type);
		int maxIdx = cumulative.length - 1;
		if (targetLevel <= 0) return 0;
		if (targetLevel <= maxIdx) return cumulative[targetLevel];

		if (type == Type.CATACOMBS) {
			return cumulative[maxIdx] + (targetLevel - maxIdx) * CATACOMBS_OVERFLOW_XP;
		}
		return cumulative[maxIdx];
	}

	private static double[] tableFor(Type type) {
		switch (type) {
			case RUNECRAFTING: return RUNECRAFTING;
			case SOCIAL: return SOCIAL;
			case CATACOMBS: return CATACOMBS;
			default: return NORMAL;
		}
	}

	public static Level getLevel(double totalXp, Type type, int maxLevel) {
		double[] cumulative = tableFor(type);
		int tableCap = cumulative.length - 1;

		int loopCap = Math.min(maxLevel, tableCap);
		for (int i = 1; i <= loopCap; i++) {
			if (totalXp < cumulative[i]) {
				int level = i - 1;
				double current = cumulative[i - 1];
				double next = cumulative[i];
				double progress = (totalXp - current) / (next - current);
				return new Level(level, progress);
			}
		}

		if (type == Type.CATACOMBS && maxLevel > tableCap) {
			double over = totalXp - cumulative[tableCap];
			int extra = (int) Math.floor(over / CATACOMBS_OVERFLOW_XP);
			int level = tableCap + extra;
			if (level >= maxLevel) return new Level(maxLevel, 1.0);
			double progress = (over - extra * CATACOMBS_OVERFLOW_XP) / CATACOMBS_OVERFLOW_XP;
			return new Level(level, progress);
		}

		return new Level(loopCap, 1.0);
	}
}
