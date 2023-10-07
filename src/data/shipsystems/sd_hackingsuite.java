package data.shipsystems;

import java.awt.Color;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.sun.xml.internal.ws.api.pipe.Engine;
import data.scripts.sd_util;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class sd_hackingsuite extends BaseShipSystemScript {
	final Map<ShipAPI.HullSize, Integer> FONT_SIZE = new HashMap<>(); {
		FONT_SIZE.put(ShipAPI.HullSize.FRIGATE, 30);
		FONT_SIZE.put(ShipAPI.HullSize.DESTROYER, 40);
		FONT_SIZE.put(ShipAPI.HullSize.CRUISER, 50);
		FONT_SIZE.put(ShipAPI.HullSize.CAPITAL_SHIP, 60);
	}
	final Map<ShipAPI.HullSize, Integer> ARC_THICKNESS = new HashMap<>(); {
		ARC_THICKNESS.put(ShipAPI.HullSize.FRIGATE, 5);
		ARC_THICKNESS.put(ShipAPI.HullSize.DESTROYER, 8);
		ARC_THICKNESS.put(ShipAPI.HullSize.CRUISER, 12);
		ARC_THICKNESS.put(ShipAPI.HullSize.CAPITAL_SHIP, 15);
	}
	final Color Color1 = new Color(250, 235, 215,75);
	final Color Color2 = new Color(250, 235, 215,155);
	final float DISRUPTION_DURATION = 5;
	final Random rand = new Random();
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;
		// TODO: DEBUG THIS VS. MONITOR, HAVE SOUND AND VFX PLAY WITH A LISTENER PLUGIN STOLEN FROM ALEX DISRUPTOR CODE, DRAW A RING INDICATING SYSTEM RANGE
		// TODO: MAKE THE REMAINDER COOLDOWN SCALE OFF THE TARGET'S HULL SIZE,
		// set jitter effects for ourselves
		ShipAPI ship = (ShipAPI) stats.getEntity();
		float jitterLevel = effectLevel;
		if (state == State.OUT)
			jitterLevel *= jitterLevel;
		float jitterExtra = jitterLevel * 50;
		ship.setJitter(this, Color1, jitterLevel, 4, 0f, 0 + jitterExtra);
		ship.setJitterUnder(this, Color2, jitterLevel, 20, 0f, 3f + jitterExtra);

		// force the target's system on cooldown for its base cooldown time plus an amount of seconds, note we check effectLevel and not for active state because our system doesn't have an active duration
		if (ship.getSystem().getEffectLevel() == 1) {
			applyEffectToTarget(ship.getShipTarget());
		}
	}
	void applyEffectToTarget(final ShipAPI target) {
		final CombatEngineAPI engine = Global.getCombatEngine();
		if (target.getSystem().isOn())
			target.getSystem().deactivate();
		final float duration = target.getSystem().getCooldownRemaining() + DISRUPTION_DURATION;
		target.getSystem().setCooldown(duration);
		engine.addFloatingText(target.getLocation(), "System disabled for "+ Math.round(duration) +" seconds!",
				FONT_SIZE.get(target.getHullSize()), Color.LIGHT_GRAY, target, 1, 10);
		engine.addPlugin(new BaseEveryFrameCombatPlugin() { // TODO: MOVE THIS INTO ITS OWN CLASS, SIMILAR TO THE PPT CHRONOBOOST
			float time = 0;
			boolean doOnce = true;
			final IntervalUtil interval = new IntervalUtil(1f, 1f);
			@Override
			public void advance(float amount, List<InputEventAPI> events) {
//				final Color EMP_CENTER_COLOR = new Color(250, 235, 215, 205);
//				final Color EMP_EDGE_COLOR = new Color(255,120,80,105);
				target.fadeToColor(this, Color1, 0.5f, 0.5f, 0.75f);
				if (doOnce) {
					target.getFluxTracker().playOverloadSound();
					target.setShipSystemDisabled(true);
					doOnce = false;
				}
				if (Global.getCombatEngine().isPaused())
					return;
				interval.advance(amount);
				if (interval.intervalElapsed()) {
					time += 1;
					if (time >= duration) {
						target.setShipSystemDisabled(false);
						engine.removePlugin(this);
						//return;
					}
					// make arcs shoot out of the target randomly based on their flux level
					// the most challenging aspect of this is knowing what type of system it is
					// I want engines on a ship with a mobility system, weapons with an offensive, and shields with a shield system
					// but this doesn't really work because uh... there's no tagging functionality for systems
					// so, I have to cope, anyway if you find this blog and know a solution send me a DM
//					List<Object> ports = new ArrayList<>();
//					ports.addAll(target.getEngineController().getShipEngines());
//					ports.addAll(target.getHullSpec().getAllWeaponSlotsCopy());
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
//						if (chance >= target.getFluxLevel() * 100 && portLocation != null) {
//							float radius = target.getCollisionRadius();
//							Vector2f targetLocation = MathUtils.getPointOnCircumference(target.getLocation(),radius * MathUtils.getRandomNumberInRange(0.4f, 0.8f),
//									MathUtils.getRandomNumberInRange(-180, 180));
//							Global.getCombatEngine().spawnEmpArcVisual(portLocation, target,
//									targetLocation, target, ARC_THICKNESS.get(target.getHullSize()), EMP_EDGE_COLOR, EMP_CENTER_COLOR);
//						}
//					}
				}
			}
		});
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