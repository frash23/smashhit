SmashHit
===
Free async hit pre-processor for bukkit

What's wrong with the default hit processor?
---
There's nothing _wrong_ with it, but it can be improved.

To my knowledge, there are two deficiencies:
* Hits are processed in the current tick stack, not on demand *
* There is a (pretty low) max clicks-per-second (aka CPS) limit hardcoded into the server

<sup>* = This is the main focus of the plugin and is explained in the **What does async hit pre-processing mean?** section</sup>

The CPS limit is automatically bypassed when we process hits on our own. SmashHit's configuration allows you to set
your own preferred CPS. The CPS limit in SmashHit will keep counting a player's CPS even if the limit is reached,
however they will be denied further hits - this way a hacker spamming clicks will lock himself out of combat.

What does async hit pre-processing mean?
---
The Minecraft server has a "stack" of operations to carry out, called the "tick stack". When new operations have to be
carried out, they are added to the tick stack. The tick stack is processed 20 times every second. That means any
operation can unnecessarily take an additional 50ms! This can be felt by some people in PvP, which is what this plugin
aims to solve. Some big PvP servers already deploy a similar feature.

SmashHit listens for hit requests and processes them **independent** of the tick stack! Actual damage is still
synchronized to the Minecraft server (as damaging players isn't thread-safe), but the hit animation and hit registering
is carried out by SmashHit - this means you can hit a player as frequently as the network can handle with zero delay.
SmashHit, of course, has an inbuilt configurable rate limiter.

API
---
Since async code works differently from regular code, we need a separate event for cancelling SmashHit's pre-processing.
For this, the "AsyncPreDamageEvent" is provided, which you can hook into. Here is an example for cancelling hits in
protected WorldGuard regions:
```
class WorldGuardListener implements Listener {
	WorldGuardPlugin wg = (WorldGuardPlugin)Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");

	@EventHandler
	public void onAsyncPreDamageEvent(AsyncPreDamageEvent e) {
		Player damager = e.getDamager();
		Damageable entity = e.getEntity();
		World world = damager.getWorld();
		RegionManager rgMgr = wg.getRegionManager(world);

        boolean damagerCanDamage = rgMgr.getApplicableRegions(damager.getLocation()).testState( wg.wrapPlayer(damager), DefaultFlag.PVP)
        boolean entityIsDamageable = rgMgr.getApplicableRegions(entity.getLocation()).testState( wg.wrapPlayer(damager), DefaultFlag.PVP)
		if(!damagerCanDamage || !entityIsDamageable) {
			e.setCancelled(true);
		}
	}

}
```
**Remember that your listener is invoked asynchronously and you can NOT safely write to the Bukkit API**
