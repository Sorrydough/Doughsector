package data.weapons.mote;

import java.awt.Color;

import data.sd_util;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class sd_moteOnHitEffect implements OnHitEffectPlugin {
	final float ANTI_FIGHTER_DAMAGE = 250;
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (target instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) target;
			if (!ship.isFighter()) {
				float pierceChance = 1;
				pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
				boolean piercedShield = shieldHit && (float) Math.random() < pierceChance;
				
				if (!shieldHit || piercedShield) {
					engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target,
							projectile.getDamageType(), projectile.getDamageAmount(), projectile.getEmpAmount(),
							100000f, "mote_attractor_impact_emp_arc", 20f,
							sd_util.healColor, new Color(255, 255, 255, 255));
				}
			} else
				Global.getCombatEngine().applyDamage(projectile, ship, point, ANTI_FIGHTER_DAMAGE, DamageType.ENERGY, 0f, false, false, projectile.getSource(), true);
		} else if (target instanceof MissileAPI)
			Global.getCombatEngine().applyDamage(projectile, target, point, ANTI_FIGHTER_DAMAGE, DamageType.ENERGY, 0f, false, false, projectile.getSource(), true);
		Global.getSoundPlayer().playSound("mote_attractor_impact_normal", 1f, 1f, point, new Vector2f());
	}
}