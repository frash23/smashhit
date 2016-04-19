package com.frash23.smashhit;

import org.bukkit.entity.Damageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AsyncPreDamageEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private boolean cancelled = false;
	private Player damager;
	private Damageable entity;
	private double damage;

	AsyncPreDamageEvent(Player dmgr, Damageable ent, double dmg) {
		super(true);

		damager = dmgr;
		entity = ent;
		damage = dmg;
	}

	public void setDamage(double dmg) { damage = dmg; }

	public Player getDamager() { return damager; }
	public Damageable getEntity() { return entity; }
	public double getDamage() { return damage; }
	public EntityType getEntityType() { return entity.getType(); }

	public void setCancelled(boolean cancel) { cancelled = cancel; }

	public boolean isCancelled() { return cancelled; }
	public HandlerList getHandlers() { return handlers; }
	public static HandlerList getHandlerList() { return handlers; }
}
