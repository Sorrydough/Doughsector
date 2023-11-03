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
	final Map<ShipAPI.HullSize, Float> FLUX_COST = new HashMap<>(); {
		FLUX_COST.put(ShipAPI.HullSize.FRIGATE, 0.5f);
		FLUX_COST.put(ShipAPI.HullSize.DESTROYER, 1f);
		FLUX_COST.put(ShipAPI.HullSize.CRUISER, 1.5f);
		FLUX_COST.put(ShipAPI.HullSize.CAPITAL_SHIP, 2.5f);
	}
	final Color Color1 = new Color(250, 235, 215,75);
	final Color Color2 = new Color(250, 235, 215,155);
	boolean doOnce = true;
	ShipAPI target = null;
	final IntervalUtil TIMER = new IntervalUtil(1, 1);
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		CombatEngineAPI engine = Global.getCombatEngine();
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (engine == null || ship.getOwner() == -1 || ship.getVariant() == null)
			return;
		// when I'm interacting with another ship I like to use a plugin even when it's not strictly necessary
		if (doOnce && ship.getSystem().isActive()) {
			target = ship.getShipTarget();
			engine.addPlugin(new sd_hackingsuitePlugin(ship, target));
			doOnce = false;
		}
		// vfx
		ship.setJitter(id, Color1, effectLevel, 2, 0, 5);
		ship.setJitterUnder(id, Color2, effectLevel, 10, 0, 5);

		TIMER.advance(engine.getElapsedInLastFrame());
		if (target != null && TIMER.intervalElapsed()) {
			ship.getFluxTracker().increaseFlux(ship.getHullSpec().getFluxDissipation() * FLUX_COST.get(target.getHullSize()), false);
			if (!isTargetValid(ship, target)) // deactivate the system if the target becomes invalid
				ship.getSystem().deactivate();
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		target = null;
		doOnce = true;
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