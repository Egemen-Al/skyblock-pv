/*
 * Copyright (C) 2022-2024 NotEnoughUpdates contributors
 * Derivative work under GNU LGPL-3.0-or-later.
 */
package com.eggman.pv.data;

import com.google.gson.JsonObject;

/**
 * Holds the static weight/leveling data tables that the weight calculators
 * read from. In NEU these come from the NotEnoughUpdates-REPO at runtime
 * (constants/weight.json, constants/leveling.json).
 *
 * TODO: load these from a bundled resource or fetch from the NEU repo.
 * For now they are empty JsonObjects so the project compiles; populate
 * before weight calculation will return real numbers.
 */
public class Constants {
	public static JsonObject WEIGHT = new JsonObject();
	public static JsonObject LEVELING = new JsonObject();
}
