/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.pv.weight.senither;

import com.example.pv.data.Level;
import com.example.pv.weight.weight.DungeonsWeight;
import com.example.pv.weight.weight.WeightStruct;
import com.example.pv.data.Constants;
import com.example.pv.util.PvCalc;

import java.util.Map;

public class SenitherDungeonsWeight extends DungeonsWeight {

	public SenitherDungeonsWeight(Map<String, Level> player) {
		super(player);
	}

	public void getClassWeight(String className) {
		Level currentClass = player.get(className);
		double base =
			Math.pow(currentClass.level, 4.5) *
			PvCalc.getElementAsFloat(PvCalc.getElement(Constants.WEIGHT, "senither.dungeons.classes." + className), 0);

		if (currentClass.totalXp <= CATACOMBS_LEVEL_50_XP) {
			weightStruct.add(new WeightStruct(base));
			return;
		}

		double remaining = currentClass.totalXp - CATACOMBS_LEVEL_50_XP;
		double splitter = (4 * CATACOMBS_LEVEL_50_XP) / base;
		weightStruct.add(new WeightStruct(Math.floor(base), Math.pow(remaining / splitter, 0.968)));
	}

	@Override
	public void getDungeonWeight() {
		Level catacombs = player.get("catacombs");
		double base =
			Math.pow(catacombs.level, 4.5) * PvCalc.getElementAsFloat(PvCalc.getElement(Constants.WEIGHT, "senither.dungeons.catacombs"), 0);

		if (catacombs.totalXp <= CATACOMBS_LEVEL_50_XP) {
			weightStruct.add(new WeightStruct(base));
			return;
		}

		double remaining = catacombs.totalXp - CATACOMBS_LEVEL_50_XP;
		double splitter = (4 * CATACOMBS_LEVEL_50_XP) / base;
		weightStruct.add(new WeightStruct(Math.floor(base), Math.pow(remaining / splitter, 0.968)));
	}
}
