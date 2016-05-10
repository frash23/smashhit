package com.frash23.smashhit.damageresolver;

import net.minecraft.server.v1_7_R4.EnchantmentManager;
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.GenericAttributes;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

class DamageResolver_1_7_R4 implements DamageResolver {

	private boolean USE_CRITS, OLD_CRITS;

	DamageResolver_1_7_R4(boolean useCrits, boolean oldCrits) {
		USE_CRITS = useCrits;
		OLD_CRITS = oldCrits;
	}

	@Override
	public double getDamage(Player damager, Damageable entity) {

		EntityPlayer nmsp = ( (CraftPlayer)damager ).getHandle();
		EntityPlayer nmse = ( (CraftPlayer)entity ).getHandle();
		double damage = nmsp.getAttributeInstance(GenericAttributes.e).getValue();
		damage += EnchantmentManager.a( (EntityLiving)nmsp, (EntityLiving) nmse);


		if(USE_CRITS
		&& !( (Entity)damager ).isOnGround()
		&& damager.getVelocity().getY() < 0
		&& OLD_CRITS || !damager.isSprinting()
		) damage *= 1.5;

		return damage;
	}

}
