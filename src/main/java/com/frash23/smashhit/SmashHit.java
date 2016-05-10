package com.frash23.smashhit;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.async.AsyncListenerHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * SmashHit - Async hit preprocessor for the Bukkit API
 * Catches hit request packets asynchronously and processes
 * them in an async thread
 *
 * @author frash23 / Jacob Pedersen
 * @version 0.5b
 *
 * Licensed under <a href="https://github.com/frash23/smashhit/blob/master/LICENSE">NBPL v2</a>
 */
public class SmashHit extends JavaPlugin implements Listener {
	private boolean listening = false;
	private boolean debugging = false;
	private static SmashHit instance;
	private SmashHitListener hitListener = null;
	private SmashHitDebugListener debugListener = null;
	private WorldGuardListener wgListener = null;
	private AsyncListenerHandler hitListenerHandler;
	private ProtocolManager pmgr;

	@Override
	public void onEnable() {
		instance = this;
		pmgr = ProtocolLibrary.getProtocolManager();

		getCommand("smashhit").setExecutor( new SmashHitCommand(this) );
		reload();
	}

	@Override
	public void onDisable() {
		if( getHitListener() != null) unregisterHitListener();

		pmgr = null;
		instance = null;
	}

	void registerHitListener() {

		if( getHitListener() == null) {


			/* We're doing this in a separate thread as we want instantiation in the same thread as the listener itself */
			new BukkitRunnable() {
				@Override public void run() {
					setHitListener( new SmashHitListener(
							instance,
							getConfig().getBoolean("enable-criticals"),
							getConfig().getBoolean("old-criticals"),
							getConfig().getInt("max-cps"),
							getConfig().getDouble("max-reach")
					) );

					setHitListenerHandler( pmgr.getAsynchronousManager().registerAsyncHandler( getHitListener() ) );
					getHitListenerHandler().start();
				}
			}.runTaskAsynchronously(this);

			listening = true;
		}
	}

	void unregisterHitListener() {
		if( getHitListener() != null) {
			pmgr.getAsynchronousManager().unregisterAsyncHandler( getHitListenerHandler() );
			listening = false;
			setHitListener(null);
		}
	}

	void registerDebugListener() {
		if(debugListener == null) debugListener = new SmashHitDebugListener(this);
		getServer().getPluginManager().registerEvents(debugListener, this);
		debugging = true;
	}
	void unregisterDebugListener() {
		if(debugListener != null) {
			HandlerList.unregisterAll(debugListener);
			debugging = false;
			debugListener = null;
		}
	}

	private void registerWgListener() {
		if(wgListener == null) wgListener = new WorldGuardListener();
		getServer().getPluginManager().registerEvents(wgListener, this);
	}
	public void unregisterWgListener() {
		if(wgListener != null) {
			HandlerList.unregisterAll(wgListener);
			wgListener = null;
		}
	}

	void reload() {
		saveDefaultConfig();
		reloadConfig();

		if( getHitListener() != null ) getHitListener().stop();
		unregisterHitListener();
		registerHitListener();


		if(getConfig().getBoolean("use-bridge.worldguard")
		&& getServer().getPluginManager().getPlugin("WorldGuard") != null ) {
			registerWgListener();
		}
	}

	/* These are synchronized as we're setting it from another thread */
	private synchronized SmashHitListener getHitListener() { return hitListener; }
	private synchronized void setHitListener(SmashHitListener hl) { hitListener = hl; }
	private synchronized AsyncListenerHandler getHitListenerHandler() { return hitListenerHandler; }
	private synchronized void setHitListenerHandler(AsyncListenerHandler hl) { hitListenerHandler = hl; }

	boolean isListening() { return listening; }
	boolean isDebug() { return debugging; }

	static SmashHit getInstance() { return instance; }
}
