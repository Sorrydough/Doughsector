package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;

public class sd_siegemode extends BaseShipSystemScript {

	final float MISSILE_RANGE_MULT = 0.9f;
	final float MISSILE_SPEED_BONUS = 10;
	final float MISSILE_ACCEL_BONUS = 50f;
	final float MISSILE_TURNACCEL_BONUS = 50f;
	final float MISSILE_TURNRATE_BONUS = 20f;
	final float ENERGY_RANGE_BONUS = 25;
	final float SHIP_MANEUVER_PENALTY = 20f;
	float energyRangeBonusModifier;
	boolean doOnce = true;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		stats.getMissileMaxSpeedBonus().modifyPercent(id, MISSILE_SPEED_BONUS);
		stats.getMissileAccelerationBonus().modifyPercent(id, MISSILE_ACCEL_BONUS);
		stats.getMissileMaxTurnRateBonus().modifyPercent(id, MISSILE_TURNRATE_BONUS);
		stats.getMissileTurnAccelerationBonus().modifyPercent(id, MISSILE_TURNACCEL_BONUS);
		stats.getMissileWeaponRangeBonus().modifyMult(id, MISSILE_RANGE_MULT);

		if (doOnce) {
			for (MissileAPI missile : Global.getCombatEngine().getMissiles())
			{
				if (missile.getSource() == stats.getEntity())
				{
					missile.getEngineStats().getMaxSpeed().modifyPercent(id, MISSILE_SPEED_BONUS);
					missile.getEngineStats().getAcceleration().modifyPercent(id, MISSILE_ACCEL_BONUS);
					missile.getEngineStats().getMaxTurnRate().modifyPercent(id, MISSILE_TURNRATE_BONUS);
					missile.getEngineStats().getTurnAcceleration().modifyPercent(id, MISSILE_TURNACCEL_BONUS);
					missile.setMaxFlightTime(missile.getMaxFlightTime() * MISSILE_RANGE_MULT);
				}
			}
			doOnce = false;
		}

		energyRangeBonusModifier = (stats.getSystemRangeBonus().computeEffective(ENERGY_RANGE_BONUS) / ENERGY_RANGE_BONUS);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, (ENERGY_RANGE_BONUS * energyRangeBonusModifier) * effectLevel);

		stats.getMaxSpeed().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getAcceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getDeceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getTurnAcceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getMaxTurnRate().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);

		ShipAPI ship = (ShipAPI) stats.getEntity();
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.getSpec().hasTag("sd_sensor")) {
				weapon.setForceFireOneFrame(true);
				weapon.setCurrAngle(weapon.getCurrAngle() + 135 * Global.getCombatEngine().getElapsedInLastFrame());
			}
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {

		stats.getMissileMaxSpeedBonus().unmodify(id);
		stats.getMissileWeaponRangeBonus().unmodify(id);
		stats.getMissileAccelerationBonus().unmodify(id);
		stats.getMissileMaxTurnRateBonus().unmodify(id);
		stats.getMissileTurnAccelerationBonus().unmodify(id);
		stats.getMissileWeaponRangeBonus().unmodify(id);

		for (MissileAPI missile : Global.getCombatEngine().getMissiles())
		{
			if (missile.getSource() == stats.getEntity())
			{
				missile.getEngineStats().getMaxSpeed().unmodify(id);
				missile.getEngineStats().getAcceleration().unmodify(id);
				missile.getEngineStats().getMaxTurnRate().unmodify(id);
				missile.getEngineStats().getTurnAcceleration().unmodify(id);
				missile.setMaxFlightTime(missile.getMaxFlightTime() / MISSILE_RANGE_MULT);
			}
		}

		stats.getEnergyWeaponRangeBonus().unmodify(id);

		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);

		ShipAPI ship = (ShipAPI) stats.getEntity();
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.getSpec().hasTag("sd_sensor")) {
				weapon.setCurrAngle(0);
			}
		}

		doOnce = true;
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Energy weapon range +" + Math.round((ENERGY_RANGE_BONUS * energyRangeBonusModifier) * effectLevel) + "%", false);
		}
		if (index == 1) {
			return new StatusData("Missile speed and manueverability +" + Math.round(MISSILE_SPEED_BONUS) + "%", false);
		}
		if (index == 3) {
			return new StatusData("Ship speed and maneuverability -" + Math.round(SHIP_MANEUVER_PENALTY * effectLevel) + "%", true);
		}
		return null;
	}
}



