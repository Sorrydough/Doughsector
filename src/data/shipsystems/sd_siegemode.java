package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;

import java.util.ArrayList;
import java.util.List;

public class sd_siegemode extends BaseShipSystemScript {
	final float MISSILE_RANGE_MULT = 0.9f;
	final float MISSILE_SPEED_BONUS = 10;
	final float MISSILE_ACCEL_BONUS = 50f;
	final float MISSILE_TURNACCEL_BONUS = 50f;
	final float MISSILE_TURNRATE_BONUS = 20f;
	final float ENERGY_RANGE_BONUS = 25;
	final float SHIP_MANEUVER_PENALTY = 20f;
	List<MissileAPI> modifiedMissiles = new ArrayList<>();
	float energyRangeBonusModifier;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		boolean isMissileModified = false;
		for (MissileAPI missile : Global.getCombatEngine().getMissiles()) {
			if (missile.getSource() == stats.getEntity()) {
				for (MissileAPI modifiedMissile : modifiedMissiles) {
					if (modifiedMissile == missile) {
						isMissileModified = true;
						break;
					}
				}
				if (!isMissileModified) {
					missile.getEngineStats().getMaxSpeed().modifyPercent(id, MISSILE_SPEED_BONUS);
					missile.getEngineStats().getAcceleration().modifyPercent(id, MISSILE_ACCEL_BONUS);
					missile.getEngineStats().getMaxTurnRate().modifyPercent(id, MISSILE_TURNRATE_BONUS);
					missile.getEngineStats().getTurnAcceleration().modifyPercent(id, MISSILE_TURNACCEL_BONUS);
					missile.setMaxFlightTime(missile.getMaxFlightTime() * MISSILE_RANGE_MULT);
					modifiedMissiles.add(missile);
				}
			}
		}

		energyRangeBonusModifier = (stats.getSystemRangeBonus().computeEffective(ENERGY_RANGE_BONUS) / ENERGY_RANGE_BONUS);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, (ENERGY_RANGE_BONUS * energyRangeBonusModifier) * effectLevel);

		stats.getMaxSpeed().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getAcceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getDeceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getTurnAcceleration().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
		stats.getMaxTurnRate().modifyMult(id, 1f - (SHIP_MANEUVER_PENALTY * effectLevel) * 0.01f);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {

		stats.getEnergyWeaponRangeBonus().unmodify(id);

		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);

		for (MissileAPI modifiedMissile : modifiedMissiles) {
			if (Global.getCombatEngine().isEntityInPlay(modifiedMissile) && modifiedMissile.getSource() == stats.getEntity()) {
				modifiedMissile.getEngineStats().getMaxSpeed().unmodify(id);
				modifiedMissile.getEngineStats().getAcceleration().unmodify(id);
				modifiedMissile.getEngineStats().getMaxTurnRate().unmodify(id);
				modifiedMissile.getEngineStats().getTurnAcceleration().unmodify(id);
				modifiedMissile.setMaxFlightTime(modifiedMissile.getMaxFlightTime() / MISSILE_RANGE_MULT);
			}
		}
		modifiedMissiles.clear();
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Energy weapon range +" + Math.round((ENERGY_RANGE_BONUS * energyRangeBonusModifier) * effectLevel) + "%", false);
		}
		if (index == 1) {
			return new StatusData("Missile speed and manueverability +" + Math.round(MISSILE_SPEED_BONUS) + "%", false);
		}
		if (index == 2) {
			return new StatusData("Ship speed and maneuverability -" + Math.round(SHIP_MANEUVER_PENALTY * effectLevel) + "%", true);
		}
		return null;
	}
}



