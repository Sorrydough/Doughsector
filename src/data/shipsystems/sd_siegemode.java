package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;

public class sd_siegemode extends BaseShipSystemScript {

	final float ENERGY_RANGE_BONUS = 25;
	final float MISSILE_SPEED_BONUS = 10;
	final float MISSILE_RANGE_MULT = 0.9f;
	final float MISSILE_ACCEL_BONUS = 50f;
	final float MISSILE_TURN_ACCEL_BONUS = 50f;
	final float MISSILE_RATE_BONUS = 20f;
	final float SHIP_MANEUVER_PENALTY = 25f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		stats.getEnergyWeaponRangeBonus().modifyPercent(id, ENERGY_RANGE_BONUS * effectLevel);

		stats.getMissileMaxSpeedBonus().modifyPercent(id, MISSILE_SPEED_BONUS * effectLevel);
		stats.getMissileWeaponRangeBonus().modifyMult(id, 1f - (MISSILE_RANGE_MULT * effectLevel) * 0.01f);
		stats.getMissileAccelerationBonus().modifyPercent(id, MISSILE_ACCEL_BONUS * effectLevel);
		stats.getMissileMaxTurnRateBonus().modifyPercent(id, MISSILE_RATE_BONUS * effectLevel);
		stats.getMissileTurnAccelerationBonus().modifyPercent(id, MISSILE_TURN_ACCEL_BONUS * effectLevel);

		stats.getAcceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getDeceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getTurnAcceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getMaxTurnRate().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponRangeBonus().unmodify(id);

		stats.getMissileMaxSpeedBonus().unmodify(id);
		stats.getMissileWeaponRangeBonus().unmodify(id);
		stats.getMissileAccelerationBonus().unmodify(id);
		stats.getMissileMaxTurnRateBonus().unmodify(id);
		stats.getMissileTurnAccelerationBonus().unmodify(id);

		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Energy weapon range +" + (int)ENERGY_RANGE_BONUS + "%", false);
		}
		if (index == 1) {
			return new StatusData("Missile speed and manueverability +" + (int)MISSILE_SPEED_BONUS + "%", false);
		}
		if (index == 2) {
			return new StatusData("Ship maneuverability -" + (int)SHIP_MANEUVER_PENALTY + "%", true);
		}
		return null;
	}
}



