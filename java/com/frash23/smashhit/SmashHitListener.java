package com.frash23.smashhit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import net.minecraft.server.v1_9_R1.GenericAttributes;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SmashHitListener extends PacketAdapter {
	private SmashHit plugin;
	private ProtocolManager pmgr;
	WorldGuardPlugin worldGuard = null;

	private Map<Player, Integer> cps = new HashMap<>();
	private Queue<EntityDamageByEntityEvent> hitQueue = new ConcurrentLinkedQueue<>();

	public static byte MAX_CPS;
	public static float MAX_DISTANCE;
	public static boolean USE_CRITICALS;
	public static boolean BRIDGE_WORLDGUARD;

	SmashHitListener(SmashHit pl, boolean useCrits, int maxCps, double maxDistance, boolean bridgeWg) {
		super(pl, ListenerPriority.HIGH, Collections.singletonList( PacketType.Play.Client.USE_ENTITY) );
		plugin = pl;
		pmgr = ProtocolLibrary.getProtocolManager();

		worldGuard = (WorldGuardPlugin)plugin.getServer().getPluginManager().getPlugin("WorldGuard");

		USE_CRITICALS = useCrits;
		MAX_CPS = (byte)maxCps;
		MAX_DISTANCE = (float)maxDistance * (float)maxDistance;
		BRIDGE_WORLDGUARD = bridgeWg && worldGuard != null;
	}

	private BukkitTask hitQueueProcessor = new BukkitRunnable() {
		@Override
		public void run() {
			while( hitQueue.size() > 0 ) {
				EntityDamageByEntityEvent e = hitQueue.remove();
				Bukkit.getPluginManager().callEvent(e);
				if( !e.isCancelled() ) {
					Damageable target = (Damageable)e.getEntity();
					Player attacker = (Player)e.getDamager();
					target.damage( e.getFinalDamage(), e.getDamager() );

					if( cps.get(attacker) == null ) cps.put(attacker, 1);
					else cps.put( attacker, cps.get(attacker) + 1 );
				}
			}
		}
	}.runTaskTimer(SmashHit.getInstance(), 1, 1);

	private BukkitTask cpsResetter = new BukkitRunnable() {
		@Override public void run() { cps.clear(); }
	}.runTaskTimer(SmashHit.getInstance(), 20, 20);

	@SuppressWarnings("deprecation")
	@Override
	public void onPacketReceiving(PacketEvent e) {

		PacketContainer packet = e.getPacket();
		Player attacker = e.getPlayer();
		Damageable target = (Damageable)packet.getEntityModifier(e).read(0);
		World world = attacker.getWorld();

		int attackerCps = cps.containsKey(attacker)? cps.get(attacker) : 0;

		/* Huge if() block to verify the hit request */
		if(e.getPacketType() == PacketType.Play.Client.USE_ENTITY			// Packet is for entity interaction
		&& packet.getEntityUseActions().read(0) == EntityUseAction.ATTACK	// Packet is for entity damage
		&&	packet.getEntityModifier(e).read(0) instanceof Damageable		// Target entity is damageable
		&&	!target.isDead() && attackerCps < 30									// We want the damage effect to show if a player
		&& world == target.getWorld() && world.getPVP() 						// Attacker & target are in the same world
		&& attacker.getLocation().distanceSquared( target.getLocation() ) < MAX_DISTANCE) {	// Distance sanity check

			/* Check if WorldGuard allows us to do this */
			if(BRIDGE_WORLDGUARD
			&& (  !worldGuard.getRegionManager(world).getApplicableRegions(attacker.getLocation()).testState( worldGuard.wrapPlayer(attacker), DefaultFlag.PVP)
				|| !worldGuard.getRegionManager(world).getApplicableRegions(target.getLocation()).testState( worldGuard.wrapPlayer(attacker), DefaultFlag.PVP)
			   )
			) return;

			/* The check above ensures we can roll our own hits */
			e.setCancelled(true);

			/* Construct the fake packet for making the attacker's
			 * victim appear hit */
			PacketContainer damageAnimation = new PacketContainer(PacketType.Play.Server.ENTITY_STATUS);
			damageAnimation.getIntegers().write(0, target.getEntityId());
			damageAnimation.getBytes().write(0, (byte)2);

			try {
				pmgr.sendServerPacket(attacker, damageAnimation);

				/* Check if attacker's CPS is within the specified maximum */
				if(attackerCps <= MAX_CPS) {
					double damage = ((CraftPlayer)attacker).getHandle().getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).getValue();
					if( USE_CRITICALS && !attacker.isOnGround() && attacker.getVelocity().getY() < 0 ) damage *= 1.5;

					hitQueue.add( new EntityDamageByEntityEvent(attacker, target, DamageCause.ENTITY_ATTACK, damage) );
				}

			}
			catch(InvocationTargetException err) { throw new RuntimeException("Error while sending damage packet: ", err); }
		}
	}

	private double entityDistanceSquared(Entity e1, Entity e2) {
		return e1.getLocation().distanceSquared( e2.getLocation() );
	}

	public void stop() {
		cpsResetter.cancel();
		hitQueueProcessor.cancel();
	}
}

/* --- Non-deprecated EntityDamageByEntityEvent ---

In class:
	//private final Function<? super Double, Double> ZERO = Functions.constant(-0.0);
	//Map<DamageModifier, Function<? super Double, Double>> modFuncs = new EnumMap<>(DamageModifier.class);


In constructor:
	//for( DamageModifier dm : DamageModifier.values() ) modFuncs.put(dm, ZERO);

In onPacketReceiving try-catch:
	//Map<DamageModifier, Double> mods = new EnumMap<>(DamageModifier.class);
	//for( DamageModifier dm : DamageModifier.values() ) mods.put(dm, 0D);
	//mods.put(DamageModifier.BASE, damage);
	//hitQueue.add( new EntityDamageByEntityEvent(attacker, target, DamageCause.ENTITY_ATTACK, mods, modFuncs) );
 */