package com.frash23.smashhit.damageresolver;

import org.bukkit.Bukkit;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;

public interface DamageResolver {

	public double getDamage(Player p, Damageable entity);

	public static DamageResolver getDamageResolver(boolean USE_CRITS, boolean OLD_CRITS) {
		try {
			String version = Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3];

			switch(version) {
				case "v1_9_R1": return new DamageResolver_1_9_R1(USE_CRITS, OLD_CRITS);
				case "v1_8_R3": return new DamageResolver_1_8_R3(USE_CRITS, OLD_CRITS);
				case "v1_7_R4": return new DamageResolver_1_7_R4(USE_CRITS, OLD_CRITS);
				default: return null;
			}

		} catch(ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException("Error while registering damage resolver (unknown version '" + Bukkit.getServer().getClass().getPackage().getName() + "'?): ", e);
		}
	}

}
