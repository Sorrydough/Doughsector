package data.shipsystems;

import java.awt.Color;
import java.util.*;

import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.sd_util;

public class sd_hackingsuite extends BaseShipSystemScript {
	final Color Color1 = new Color(250, 235, 215,75);
	final Color Color2 = new Color(250, 235, 215,155);
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;
		// set jitter effects for ourselves
		ShipAPI ship = (ShipAPI) stats.getEntity();
		float jitterLevel = effectLevel;
		if (state == State.OUT) // ensures jitter level doesn't deteriorate during OUT
			jitterLevel *= jitterLevel;
		float jitterExtra = jitterLevel * 50;
		ship.setJitter(this, Color1, jitterLevel, 4, 0, 0 + jitterExtra);
		ship.setJitterUnder(this, Color2, jitterLevel, 20, 0, 3 + jitterExtra);

		// apply the effect to the target, note we check effectLevel and not for active state because our system doesn't have an active duration
		if (ship.getSystem().getEffectLevel() == 1)
			Global.getCombatEngine().addPlugin(new sd_hackingsuitePlugin(ship.getShipTarget()));
	}
	public static boolean isTargetValid(ShipAPI ship, ShipAPI target) { // checks whether the target is in range, blah blah blah
		if (target == null)												// needs to take target as an input to work in the AI script
			return false;
		float targetDistance = MathUtils.getDistance(ship, target);
		float systemRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship) + ship.getCollisionRadius());
		return target.getSystem() != null && !target.getSystem().isOn() && !target.getCustomData().containsKey("sd_hackingsuite")
				&& !target.isFighter() && target != ship && !target.getFluxTracker().isOverloadedOrVenting()
				&& !(targetDistance > systemRange) && target.getOwner() != ship.getOwner();
	}
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo() || system.getState() == SystemState.COOLDOWN)
			return "COOLDOWN";
		if (system.isActive())
			return "INTRUDING";
		if (!AIUtils.canUseSystemThisFrame(ship))
			return "STANDBY";
		if (!isTargetValid(ship, ship.getShipTarget()))
			return "NO TARGET";
		return "READY";
	}
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return isTargetValid(ship, ship.getShipTarget()) && AIUtils.canUseSystemThisFrame(ship);
	}
}