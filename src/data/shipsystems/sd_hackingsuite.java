package data.shipsystems;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.graphics.sd_decoSystemRangePlugin;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.sd_util;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class sd_hackingsuite extends BaseShipSystemScript {
	boolean runOnce = true;
	boolean applyDeco = true;
	ShipAPI enemy = null;
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (!sd_util.isCombatSituation(ship))
			return;

		// needed to add this because it was possible for the ship to deselect its target during system chargeup, which would pass a null target to the effect plugin
		if (runOnce && state == State.IN) {
			enemy = ship.getShipTarget();
			runOnce = false;
		}

		// set jitter effects for ourselves
		float jitterLevel = effectLevel;
		if (state == State.OUT) // ensures jitter level doesn't deteriorate during OUT
			jitterLevel *= jitterLevel;
		float jitterExtra = jitterLevel * 50;
		ship.setJitter(this, sd_util.systemColor2, jitterLevel, 4, 0, jitterExtra);
		ship.setJitterUnder(this, sd_util.damageColor1, jitterLevel, 20, 0, 3 + jitterExtra);

		// apply the effect to the target, note we check effectLevel and not for active state because our system doesn't have an active duration
		if (ship.getSystem().getEffectLevel() == 1)
			Global.getCombatEngine().addPlugin(new sd_hackingsuitePlugin(enemy));
	}
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		if (applyDeco) {
			Global.getCombatEngine().addPlugin(new sd_decoSystemRangePlugin((ShipAPI) stats.getEntity()));
			applyDeco = false;
		}
		runOnce = true;
	}
	public static boolean isTargetValid(ShipAPI ship, ShipAPI target) { // checks whether the target is in range, blah blah blah
		if (target == null)                                                // needs to take target as an input to work in the AI script
			return false;
		float targetDistance = MathUtils.getDistance(ship, target);
		float systemRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship) + ship.getCollisionRadius());
		return target.isAlive() && target.getSystem() != null && !target.getSystem().isOn() && !target.isFighter() && !target.isPhased() && target != ship
				&& !target.getFluxTracker().isOverloadedOrVenting() && target.getMutableStats().getDynamic().getMod("sd_hackingsuite").getFlatBonuses().isEmpty()
				&& !(targetDistance > systemRange) && target.getOwner() != ship.getOwner() && target.getCurrentCR() > 0;
	}
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo() || system.getState() == SystemState.COOLDOWN)
			return "COOLDOWN";
		if (system.isActive())
			return "INTRUDING";
		if (!sd_util.canUseSystemThisFrame(ship))
			return "STANDBY";
		if (!isTargetValid(ship, ship.getShipTarget()))
			return "NO TARGET";
		return "READY";
	}
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return isTargetValid(ship, ship.getShipTarget()) && sd_util.canUseSystemThisFrame(ship);
	}
	static class sd_hackingsuitePlugin extends BaseEveryFrameCombatPlugin {
		final ShipAPI target;
		public sd_hackingsuitePlugin(ShipAPI target) {
			this.target = target;
		}
		final Map<ShipAPI.HullSize, Integer> DURATION = new HashMap<>(); {
			DURATION.put(ShipAPI.HullSize.FRIGATE, 20);
			DURATION.put(ShipAPI.HullSize.DESTROYER, 15);
			DURATION.put(ShipAPI.HullSize.CRUISER, 10);
			DURATION.put(ShipAPI.HullSize.CAPITAL_SHIP, 5);
		}
		final float AUTOFIRE_PENALTY = 0.33f;
		final Color fadeColor = new Color(230, 215, 195, 100);
		final IntervalUtil TIMER = new IntervalUtil(1f, 1f);
		boolean runOnce = true;
		float duration = 0, time = 0;
		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			CombatEngineAPI engine = Global.getCombatEngine();
			target.fadeToColor(this, fadeColor, 0.5f, 0.5f, 0.75f);
			if (runOnce) {
				duration += DURATION.get(target.getHullSize()) + target.getSystem().getCooldownRemaining();
				if (sd_util.isAutomated(target))
					duration *= 1.5;
				target.getMutableStats().getAutofireAimAccuracy().modifyMult("sd_hackingsuite", AUTOFIRE_PENALTY);
				target.getFluxTracker().playOverloadSound();
				target.setShipSystemDisabled(true);
				target.getFluxTracker().showOverloadFloatyIfNeeded("System disabled for " + Math.round(duration) + " seconds!", Color.LIGHT_GRAY, 12, true);
				target.getMutableStats().getDynamic().getMod("sd_hackingsuite").modifyFlat("sd_hackingsuite", -1);
				runOnce = false;
			}
			if (engine.isPaused())
				return; // don't want the timer to progress while the engine is paused
			TIMER.advance(amount);
			if (TIMER.intervalElapsed()) {
				time += 1;
				if (time == duration / 2)
					target.getFluxTracker().showOverloadFloatyIfNeeded(Math.round(duration / 2) + " seconds until system restoration.", Color.LIGHT_GRAY, 12, true);
				if (time >= duration) {
					target.getMutableStats().getAutofireAimAccuracy().unmodifyMult("sd_hackingsuite");
					if (target.getCurrentCR() > 0)
						target.setShipSystemDisabled(false);
					target.getMutableStats().getDynamic().getMod("sd_hackingsuite").unmodify("sd_hackingsuite");
					target.getFluxTracker().showOverloadFloatyIfNeeded("System functionality restored!", Color.LIGHT_GRAY, 12, true);
					engine.removePlugin(this);
					//return;
				}
				// make arcs shoot out of the target randomly based on their flux level
				// the most challenging aspect of this is knowing what type of system it is
				// I want engines on a ship with a mobility system, weapons with an offensive, and shields with a shield system
				// but this doesn't really work because uh... there's no tagging functionality for systems
				// so, I have to cope, anyway if you find this blog and know a solution send me a DM
// 				final Color EMP_CENTER_COLOR = new Color(250, 235, 215, 205);
//				final Color EMP_EDGE_COLOR = new Color(255,120,80,105);
//					List<Object> ports = new ArrayList<>();
//					ports.addAll(ship.getEngineController().getShipEngines());
//					ports.addAll(ship.getHullSpec().getAllWeaponSlotsCopy());
//					for (Object port : ports) {
//						ShipEngineControllerAPI.ShipEngineAPI engine;
//						WeaponSlotAPI slot;
//						Vector2f portLocation = null;
//						float portAngle = 0;
//						if (port instanceof Engine) {
//							engine = (ShipEngineControllerAPI.ShipEngineAPI) port;
//							portLocation = engine.getLocation();
//							portAngle = engine.getEngineSlot().getAngle();
//						} else if (port instanceof WeaponSlotAPI) {
//							slot = (WeaponSlotAPI) port;
//							portLocation = slot.getLocation();
//							portAngle = slot.getAngle();
//						}
//						float chance = rand.nextInt(100);
//						if (chance >= ship.getFluxLevel() * 100 && portLocation != null) {
//							float radius = ship.getCollisionRadius();
//							Vector2f targetLocation = MathUtils.getPointOnCircumference(ship.getLocation(),radius * MathUtils.getRandomNumberInRange(0.4f, 0.8f),
//									MathUtils.getRandomNumberInRange(-180, 180));
//							Global.getCombatEngine().spawnEmpArcVisual(portLocation, ship,
//									targetLocation, ship, ARC_THICKNESS.get(ship.getHullSize()), EMP_EDGE_COLOR, EMP_CENTER_COLOR);
//						}
//					}
			}
		}
	}
}