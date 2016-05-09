package com.frash23.smashhit;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldGuardListener implements Listener {
	WorldGuardPlugin wg = null;

	WorldGuardListener() {
		wg = (WorldGuardPlugin)Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
	}

	@EventHandler
	public void onAsyncPreDamageEvent(AsyncPreDamageEvent e) {
		Player damager= e.getDamager();
		Damageable entity = e.getEntity();
		World world = damager.getWorld();

		if(!wg.getRegionManager(world).getApplicableRegions(damager.getLocation()).testState( wg.wrapPlayer(damager), DefaultFlag.PVP)
		|| !wg.getRegionManager(world).getApplicableRegions(entity.getLocation()).testState( wg.wrapPlayer(damager), DefaultFlag.PVP)
		) {
			e.setCancelled(true);
		}
	}

}
