package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class sd_antimatterarray implements OnFireEffectPlugin {

	final Map<ShipAPI.HullSize, Float> RANGE_MULT = new HashMap<>();
	{
		RANGE_MULT.put(ShipAPI.HullSize.FIGHTER, 0.95f);
		RANGE_MULT.put(ShipAPI.HullSize.FRIGATE, 1f);
		RANGE_MULT.put(ShipAPI.HullSize.DESTROYER, 1.05f);
		RANGE_MULT.put(ShipAPI.HullSize.CRUISER, 1.1f);
		RANGE_MULT.put(ShipAPI.HullSize.CAPITAL_SHIP, 1.15f);
	}

	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		MutableShipStatsAPI stats = weapon.getShip().getMutableStats();
		//make projectile speed depend on hull size, randomize on top
		float speedMult = ((0.67f + 0.33f * (float)Math.random()) * RANGE_MULT.get(stats.getVariant().getHullSize()));
		projectile.getVelocity().scale(speedMult);

		//should it benefit from missile spec and EWM? not sure
//		float damageMult = stats.getEnergyWeaponDamageMult().computeMultMod() + stats.getMissileWeaponDamageMult().computeMultMod();
//		weapon.getDamage().setDamage(weapon.getDamage().getDamage() * damageMult);
		
		float angVel = (float)(Math.signum((float)Math.random() - 0.5f) * (0.5f + Math.random()) * 720f);
		projectile.setAngularVelocity(angVel);

		float delay = 0.25f + 0.75f * (float)Math.random();
		weapon.setRefireDelay(delay);

		//makes it influenced by systems expertise range mult
		//actually you know what fuck this bullshit what the FUCK is going on
//		float rangeBonus = stats.getSystemRangeBonus().computeEffective(weapon.getRange()) / weapon.getRange();
//		MissileAPI missile = (MissileAPI) projectile;
//		float flightTimeMult = ((0.75f * rangeBonus) + 0.25f * (float)Math.random());
//		missile.setMaxFlightTime(missile.getMaxFlightTime() * 0.1f);

		//loc, vel, size,
		//brightness, duration, color
		Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(24f, 48f),
				MathUtils.getRandomNumberInRange(0.33f, 1.33f), MathUtils.getRandomNumberInRange(0.33f, 1.33f), new Color(150,100,255, 150));
		Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(24f, 48f),
				MathUtils.getRandomNumberInRange(0.33f, 1.33f), MathUtils.getRandomNumberInRange(0.33f, 1.33f), new Color(155,100,255,255));
	}
}