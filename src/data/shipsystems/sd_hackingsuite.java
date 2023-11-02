package data.shipsystems;

import java.awt.Color;
import java.util.*;

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
	boolean doOnce = true;
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;
		// set jitter effects for ourselves
		ShipAPI ship = (ShipAPI) stats.getEntity();
//		ship.setJitterUnder(this, Color2, effectLevel, 20, 0, 10 * effectLevel);
//		ship.setJitter(this, Color1, effectLevel, 5, 0, 5 * effectLevel);

		ship.setJitter(id, Color1, effectLevel, 2, 0, 5);
		ship.setJitterUnder(id, Color2, effectLevel, 10, 0, 5);

		// I like to use a plugin even when it's not strictly necessary
		if (doOnce && ship.getSystem().isActive()) {
			Global.getCombatEngine().addPlugin(new sd_hackingsuitePlugin(ship, ship.getShipTarget()));
			doOnce = false;
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		doOnce = true;
	}
	public static boolean isTargetValid(ShipAPI ship, ShipAPI target) { // checks whether the target is in range and whether it's likely to even have a system that we'd want to disable
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