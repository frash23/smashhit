package com.frash23.smashhit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import com.frash23.smashhit.damageresolver.DamageResolver;
import org.bukkit.GameMode;
import org.bukkit.World;
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

import static org.bukkit.Bukkit.getPluginManager;

class SmashHitListener extends PacketAdapter {
	private SmashHit plugin;
	private ProtocolManager pmgr;
	private DamageResolver damageResolver;

	private Map<Player, Integer> cps = new HashMap<>();
	private Queue<EntityDamageByEntityEvent> hitQueue = new ConcurrentLinkedQueue<>();

	private static byte MAX_CPS;
	private static float MAX_DISTANCE;

	SmashHitListener(SmashHit pl, boolean useCrits, boolean oldCrits, int maxCps, double maxDistance) {
		super(pl, ListenerPriority.HIGH, Collections.singletonList( PacketType.Play.Client.USE_ENTITY) );

		plugin = pl;
		pmgr = ProtocolLibrary.getProtocolManager();

		damageResolver = DamageResolver.getDamageResolver(useCrits, oldCrits);
		if(damageResolver == null) throw new NullPointerException("Damage resolver is null, unsupported Spigot version?");

		MAX_CPS = (byte)maxCps;
		MAX_DISTANCE = (float)maxDistance * (float)maxDistance;
	}

	private BukkitTask hitQueueProcessor = new BukkitRunnable() {
		@Override
		public void run() {
			while( hitQueue.size() > 0 ) {
				EntityDamageByEntityEvent e = hitQueue.remove();
				getPluginManager().callEvent(e);
				if( !e.isCancelled() ) ( (Damageable)e.getEntity() ).damage( e.getFinalDamage(), e.getDamager() );
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
		Entity entity = packet.getEntityModifier(e).read(0);
		Damageable target = entity instanceof Damageable? (Damageable)entity : null;
		World world = attacker.getWorld();

		/* Huge if() block to verify the hit request */
		if(e.getPacketType() == PacketType.Play.Client.USE_ENTITY			// Packet is for entity interaction
		&& packet.getEntityUseActions().read(0) == EntityUseAction.ATTACK	// Packet is for entity damage
		&& target != null && !target.isDead()                       		// Target entity is damageable
		&& world == target.getWorld() && world.getPVP() 					// Attacker & target are in the same world
		&& attacker.getLocation().distanceSquared( target.getLocation() ) < MAX_DISTANCE 			// Distance sanity check
		&& (!(target instanceof Player) || ((Player) target).getGameMode() != GameMode.CREATIVE)) { // Don't hit Players in creative mode

			/* The check above ensures we can roll our own hits */
			e.setCancelled(true);

			/* Construct the fake packet for making the attacker's
			 * victim appear hit */
			PacketContainer damageAnimation = new PacketContainer(PacketType.Play.Server.ENTITY_STATUS);
			damageAnimation.getIntegers().write(0, target.getEntityId());
			damageAnimation.getBytes().write(0, (byte)2);

			try {
				double damage = damageResolver.getDamage(attacker, target);

				AsyncPreDamageEvent damageEvent = new AsyncPreDamageEvent(attacker, target, damage);
				getPluginManager().callEvent(damageEvent);

				if( !damageEvent.isCancelled() ) {

					pmgr.sendServerPacket(attacker, damageAnimation);

					/* Check if attacker's CPS is within the specified maximum */
					int attackerCps = cps.containsKey(attacker)? cps.get(attacker) : 0;
					cps.put( attacker, attackerCps + 1 );

					/* By handling CPS this way, the recorded CPS will still increment even if the limit is reached.
					 * This should weed out some hackers nicely */
					if(attackerCps <= MAX_CPS) hitQueue.add( new EntityDamageByEntityEvent( attacker, target, DamageCause.ENTITY_ATTACK, damageEvent.getDamage() ) );
				}

			}
			catch(InvocationTargetException err) { throw new RuntimeException("Error while sending damage packet: ", err); }
		}
	}

	void stop() {
		cpsResetter.cancel();
		hitQueueProcessor.cancel();
		damageResolver = null;
	}
}
