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
	final float DISRUPTION_DUR = 1f;
	final float DISRUPTOR_RANGE = 1000f;
	final Color JITTER_COLOR = new Color(250, 235, 215,75);
	final Color JITTER_UNDER_COLOR = new Color(250, 235, 215,155);
	ShipAPI target;
	float targetDistance;
	float actualDisruptorRange;
	boolean isTargetValid;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		//init
		ShipAPI ship = (ShipAPI) stats.getEntity();
		target = ship.getShipTarget();
		if (target == null)
			return;
		targetDistance = MathUtils.getDistance(ship, target);
		actualDisruptorRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(DISRUPTOR_RANGE);
		isTargetValid = !target.isFighter() && target != ship && !target.getFluxTracker().isOverloadedOrVenting() && !(targetDistance > actualDisruptorRange);

		//we're going to run the script while idle because I hate alex
		if (isTargetValid && state.ordinal() < State.COOLDOWN.ordinal()) {
			//set jitter effects for ourself
			float jitterLevel = effectLevel;
			if (state == State.OUT)
				jitterLevel *= jitterLevel;
			float jitterRangeIncrease = jitterLevel * 50;
			ship.setJitter(id, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeIncrease);
			ship.setJitterUnder(id, JITTER_UNDER_COLOR, jitterLevel, 20, 0f, 3f + jitterRangeIncrease);

			//overload the target, note we check effectLevel and not for active state because the system doesn't have an active duration
			if (ship.getSystem().getEffectLevel() == 1) {
				target.getFluxTracker().beginOverloadWithTotalBaseDuration(DISRUPTION_DUR);
				target.getFluxTracker().playOverloadSound();
				target.getFluxTracker().showOverloadFloatyIfNeeded("System Disruption!", JITTER_UNDER_COLOR, 4f, true);
			}
		}
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo() || system.getState() != SystemState.IDLE)
			return "RECHARGING";
		if (targetDistance > actualDisruptorRange)
			return "OUT OF RANGE";
		if (!isTargetValid)
			return "NO TARGET";
		return "READY";
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return isTargetValid;
	}
}