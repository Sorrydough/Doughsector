package data.shipsystems;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.sd_util;
import org.lazywizard.lazylib.MathUtils;

public class sd_hackingsuite extends BaseShipSystemScript {
	final Map<ShipAPI.HullSize, Integer> FONT_SIZE = new HashMap<>(); {
		FONT_SIZE.put(ShipAPI.HullSize.FIGHTER, 20);
		FONT_SIZE.put(ShipAPI.HullSize.FRIGATE, 30);
		FONT_SIZE.put(ShipAPI.HullSize.DESTROYER, 40);
		FONT_SIZE.put(ShipAPI.HullSize.CRUISER, 50);
		FONT_SIZE.put(ShipAPI.HullSize.CAPITAL_SHIP, 60);
	}
	final float DISRUPTION_DURATION = 5;
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;
		// TODO: DEBUG THIS VS. MONITOR, HAVE SOUND AND VFX PLAY WITH A LISTENER PLUGIN STOLEN FROM ALEX DISRUPTOR CODE, DRAW A RING INDICATING SYSTEM RANGE
		// TODO: MAKE THE REMAINDER COOLDOWN SCALE OFF THE TARGET'S HULL SIZE
		// set jitter effects for ourselves
		ShipAPI ship = (ShipAPI) stats.getEntity();
		float jitterLevel = effectLevel;
		if (state == State.OUT)
			jitterLevel *= jitterLevel;
		float jitterExtra = jitterLevel * 50;
		ship.setJitter(this, new Color(250, 235, 215,75), jitterLevel, 4, 0f, 0 + jitterExtra);
		ship.setJitterUnder(this, new Color(250, 235, 215,155), jitterLevel, 20, 0f, 3f + jitterExtra);

		// force the target's system on cooldown for its base cooldown time plus an amount of seconds, note we check effectLevel and not for active state because our system doesn't have an active duration
		if (ship.getSystem().getEffectLevel() == 1) {
			ShipAPI target = ship.getShipTarget();
			if (target.getSystem().isOn())
				target.getSystem().deactivate();
			float duration = target.getSystem().getCooldownRemaining() + DISRUPTION_DURATION;
			//target.setShipSystemDisabled(true); // TODO: PUT THIS INTO A PLUGIN SO SHIT WORKS ON MONITOR ETC
			target.getSystem().setCooldown(duration);
			target.getFluxTracker().playOverloadSound();
			Global.getCombatEngine().addFloatingText(target.getLocation(), "System disabled for "+ Math.round(duration) +" seconds!",
					FONT_SIZE.get(target.getHullSize()), Color.LIGHT_GRAY, target, 1, 10);
		}
	}
	public static boolean isTargetValid(ShipAPI ship) {
		ShipAPI target = ship.getShipTarget();
		if (target == null)
			return false;
		float targetDistance = MathUtils.getDistance(ship, target);
		return target.getSystem() != null && !target.isFighter() && target != ship && !target.getFluxTracker().isOverloadedOrVenting() &&
				!(targetDistance > ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship)) && target.getOwner() != ship.getOwner());
	}
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo() || system.getState() == SystemState.COOLDOWN)
			return "RECHARGING"; // TODO: FIGURE OUT A BETTER TEXT TO PUT HERE
		if (!isTargetValid(ship))
			return "NO TARGET";
		if (MathUtils.getDistance(ship, ship.getShipTarget()) > ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship)))
			return "OUT OF RANGE";
		if (system.getState() != SystemState.IDLE)
			return "INTRUDING";
		return "READY";
	}
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return isTargetValid(ship);
	}
}