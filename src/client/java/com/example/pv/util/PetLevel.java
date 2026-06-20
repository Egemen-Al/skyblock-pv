package com.example.pv.util;

public class PetLevel {

	private static final int[] PET_LEVELS = {
		100,110,120,130,145,160,175,190,210,230,250,275,300,330,360,400,440,490,540,600,
		660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,2920,3160,3420,
		3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,12000,13300,14700,16200,17800,19500,21300,23200,
		25200,27400,29800,32400,35200,38200,41400,44800,48400,52200,56200,60400,64800,69400,74200,79200,84700,90700,97200,104200,
		111700,119700,128200,137200,146700,156700,167700,179700,192700,206700,221700,237700,254700,272700,291700,311700,333700,357700,383700,411700,
		441700,476700,516700,561700,611700,666700,726700,791700,861700,936700,1016700,1101700,1191700,1286700,1386700,1496700,1616700,1746700,1886700
	};

	private static final int[] DRAGON_LEVELS = {0, 5555};
	private static final int DRAGON_FLAT = 1886700;

	public static int offset(String rarity) {
		switch (rarity) {
			case "UNCOMMON": return 6;
			case "RARE":     return 11;
			case "EPIC":     return 16;
			case "LEGENDARY":
			case "MYTHIC":   return 20;
			default:         return 0;
		}
	}

	private static boolean isDragon(String type) {
		return "GOLDEN_DRAGON".equals(type) || "JADE_DRAGON".equals(type) || "ROSE_DRAGON".equals(type);
	}

	public static class Result {
		public final int level;
		public final double progress;
		public final int maxLevel;
		public final double totalXp;
		public final double currentLevelXp;
		public final double requiredLevelXp;
		public final double xpToNext;
		public final double xpToMax;
		public Result(int level, double progress, int maxLevel,
				double totalXp, double currentLevelXp, double requiredLevelXp,
				double xpToNext, double xpToMax) {
			this.level = level; this.progress = progress; this.maxLevel = maxLevel;
			this.totalXp = totalXp; this.currentLevelXp = currentLevelXp;
			this.requiredLevelXp = requiredLevelXp; this.xpToNext = xpToNext; this.xpToMax = xpToMax;
		}
	}

	public static Result get(double exp, String rarity, String type) {
		boolean dragon = isDragon(type);
		int maxLevel = dragon ? 200 : 100;
		int off = dragon ? 0 : offset(rarity);

		double xpForMax = 0;
		for (int i = 0; i < maxLevel - 1; i++) xpForMax += dragon ? dragonNeed(i) : PET_LEVELS[off + i];

		double remaining = exp;
		for (int i = 0; i < maxLevel - 1; i++) {
			double need = dragon ? dragonNeed(i) : PET_LEVELS[off + i];
			if (remaining >= need) {
				remaining -= need;
			} else {
				double xpToMax = Math.max(0, xpForMax - exp);
				return new Result(i + 1, need > 0 ? remaining / need : 0, maxLevel,
						exp, remaining, need, Math.max(0, need - remaining), xpToMax);
			}
		}
		return new Result(maxLevel, 1.0, maxLevel, exp, 0, 0, 0, 0);
	}

	private static double dragonNeed(int i) {
		if (i < DRAGON_LEVELS.length) return DRAGON_LEVELS[i];
		return DRAGON_FLAT;
	}
}
