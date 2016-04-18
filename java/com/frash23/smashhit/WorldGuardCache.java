package com.frash23.smashhit;

import org.bukkit.World;

/*** CURRENTLY UNUSED ***/
public class WorldGuardCache {
	private SmashHit plugin;

	WorldGuardCache(SmashHit pl) {
		plugin = pl;

	}

	static private class RegionKey {
		World world;
		String region;
		RegionKey(World w, String r) { world = w; region = r; }
	}
}
