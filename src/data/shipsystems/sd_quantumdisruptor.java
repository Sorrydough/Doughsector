package data.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;

public class sd_quantumdisruptor extends BaseShipSystemScript {
	static final float DISRUPTION_RANGE = 1000;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		//set jitter effects for ourselves
		ShipAPI ship = (ShipAPI) stats.getEntity();
		float jitterLevel = effectLevel;
		if (state == State.OUT)
			jitterLevel *= jitterLevel;
		float jitterExtra = jitterLevel * 50;
		ship.setJitter(this, new Color(250, 235, 215,75), jitterLevel, 4, 0f, 0 + jitterExtra);
		ship.setJitterUnder(this, new Color(250, 235, 215,155), jitterLevel, 20, 0f, 3f + jitterExtra);

		//overload the target, note we check effectLevel and not for active state because the system doesn't have an active duration
		if (ship.getSystem().getEffectLevel() == 1) {
			ship.getShipTarget().getFluxTracker().beginOverloadWithTotalBaseDuration(1); //<<- INPUT DISRUPTION DURATION HERE
			ship.getShipTarget().getFluxTracker().playOverloadSound();
			ship.getShipTarget().getFluxTracker().showOverloadFloatyIfNeeded("System Disruption!", new Color(250, 235, 215,155), 4f, true);
		}
	}

	protected static boolean isTargetValid(ShipAPI ship) {
		ShipAPI target = ship.getShipTarget();
		if (target == null)
			return false;
		float targetDistance = MathUtils.getDistance(ship, target);
		return !target.isFighter() && target != ship && !target.getFluxTracker().isOverloadedOrVenting() &&
				!(targetDistance > ship.getMutableStats().getSystemRangeBonus().computeEffective(DISRUPTION_RANGE));
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo() || system.getState() != SystemState.IDLE)
			return null;
		if (ship.getShipTarget() != null && MathUtils.getDistance(ship, ship.getShipTarget()) > ship.getMutableStats().getSystemRangeBonus().computeEffective(DISRUPTION_RANGE))
			return "OUT OF RANGE";
		if (!isTargetValid(ship))
			return "NO TARGET";
		return "READY";
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return isTargetValid(ship);
	}
}