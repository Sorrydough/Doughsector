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

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		stats.getEnergyWeaponRangeBonus().modifyPercent(id, ENERGY_RANGE_BONUS * effectLevel);
		stats.getMissileMaxSpeedBonus().modifyPercent(id, MISSILE_SPEED_BONUS * effectLevel);
		stats.getMissileWeaponRangeBonus().modifyMult(id, MISSILE_RANGE_MULT * effectLevel);
		stats.getMissileAccelerationBonus().modifyPercent(id, MISSILE_ACCEL_BONUS * effectLevel);
		stats.getMissileMaxTurnRateBonus().modifyPercent(id, MISSILE_RATE_BONUS * effectLevel);
		stats.getMissileTurnAccelerationBonus().modifyPercent(id, MISSILE_TURN_ACCEL_BONUS * effectLevel);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponRangeBonus().unmodify(id);
		stats.getMissileMaxSpeedBonus().unmodify(id);
		stats.getMissileWeaponRangeBonus().unmodify(id);
		stats.getMissileAccelerationBonus().unmodify(id);
		stats.getMissileMaxTurnRateBonus().unmodify(id);
		stats.getMissileTurnAccelerationBonus().unmodify(id);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Energy weapon range +" + (int)ENERGY_RANGE_BONUS + "%", false);
		}
		if (index == 1) {
			return new StatusData("Missile speed and manueverability +" + (int)MISSILE_SPEED_BONUS + "%", false);
		}
		return null;
	}
}



