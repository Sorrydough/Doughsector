package data.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;

public class sd_quantumdisruptor extends BaseShipSystemScript {



	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return; // TODO: DEBUG THIS VS. MONITOR, MAKE TEXT SIZE DEPEND ON HULLSIZE, HAVE SOUND AND VFX PLAY WITH A LISTENER PLUGIN STOLEN FROM ALEX DISRUPTOR CODE

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
			float DISRUPTION_DURATION = 5;
			float duration = target.getSystem().getCooldownRemaining() + DISRUPTION_DURATION;
			target.getSystem().setCooldown(duration);
			target.getFluxTracker().playOverloadSound();
			Global.getCombatEngine().addFloatingText(target.getLocation(), "System disabled for "+ Math.round(duration) +" seconds!", 20, Color.LIGHT_GRAY, target, 1, 10);
		}
	}
	private static boolean isTargetValid(ShipAPI ship) {
		ShipAPI target = ship.getShipTarget();
		if (target == null)
			return false;
		float targetDistance = MathUtils.getDistance(ship, target);
		return target.getSystem() != null && !target.isFighter() && target != ship && !target.getFluxTracker().isOverloadedOrVenting() &&
				!(targetDistance > ship.getMutableStats().getSystemRangeBonus().computeEffective(getOptimalRange(ship)));
	}
	private static float getOptimalRange(ShipAPI ship) {
		float totalDPS = 0;
		float totalWeightedRange = 0;
		float optimalWeaponRange = 0;
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			float weaponDPS = weapon.getSpec().getDerivedStats().getDps();
			float weaponRange = weapon.getRange();

			//adjust the weight based on DPS
			totalWeightedRange += weaponRange * weaponDPS;
			totalDPS += weaponDPS;
		}
		if (totalDPS > 0) {
			optimalWeaponRange = totalWeightedRange / totalDPS;
		}
		return optimalWeaponRange;
	}
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo() || system.getState() == SystemState.COOLDOWN)
			return "RECHARGING";
		if (!isTargetValid(ship))
			return "NO TARGET";
		if (MathUtils.getDistance(ship, ship.getShipTarget()) > ship.getMutableStats().getSystemRangeBonus().computeEffective(getOptimalRange(ship)))
			return "OUT OF RANGE";
		if (system.getState() != SystemState.IDLE)
			return "DISABLING";
		return "READY";
	}
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return isTargetValid(ship);
	}
}