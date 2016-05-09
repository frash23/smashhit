package com.frash23.smashhit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SmashHitDebugListener implements Listener {
	SmashHit plugin;

	SmashHitDebugListener(SmashHit pl) {
		plugin = pl;
	}


	@EventHandler (priority = EventPriority.LOWEST)
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
		if( e.getDamager() instanceof Player && e.getEntity() instanceof Player ) {
			plugin.getLogger().info( " --- SmashHit Debug --- " );
			plugin.getLogger().info( "Attacker: " + e.getDamager().getName() + ", victim: " + e.getEntity().getName() );
			plugin.getLogger().info( "Final damage: " + e.getFinalDamage() );
		}
	}
}
