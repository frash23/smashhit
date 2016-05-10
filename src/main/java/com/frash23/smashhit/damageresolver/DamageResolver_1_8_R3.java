package com.frash23.smashhit.damageresolver;

import net.minecraft.server.v1_8_R3.GenericAttributes;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

class DamageResolver_1_8_R3 implements DamageResolver {

	private boolean USE_CRITS, OLD_CRITS;

	DamageResolver_1_8_R3(boolean useCrits, boolean oldCrits) {
		USE_CRITS = useCrits;
		OLD_CRITS = oldCrits;
	}

	@Override
	public double getDamage(Player damager, Damageable entity) {
		double damage = ((CraftPlayer)damager).getHandle().getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).getValue();
		if(USE_CRITS
		&& !( (Entity)damager ).isOnGround()
		&& damager.getVelocity().getY() < 0
		&& OLD_CRITS || !damager.isSprinting()
		) damage *= 1.5;

		return damage;
	}

}
