package data.shipsystems;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

public class sd_quantumdisruptor extends BaseShipSystemScript {
	final float DISRUPTION_DUR = 1f;
	final float DISRUPTION_RANGE = 1000f;
	final Color OVERLOAD_COLOR = new Color(250, 235, 215,255);
	final Color JITTER_COLOR = new Color(250, 235, 215,75);
	final Color JITTER_UNDER_COLOR = new Color(250, 235, 215,155);
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		
		float jitterLevel = effectLevel;
		if (state == State.OUT) {
			jitterLevel *= jitterLevel;
		}
		float jitterRangeBonus = jitterLevel * 50;
		
		ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 20, 0f, 3f + jitterRangeBonus);
		ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);
		
		String targetKey = ship.getId() + "_acausal_target";
		Object foundTarget = Global.getCombatEngine().getCustomData().get(targetKey); 
		if (state == State.IN) {
			if (foundTarget == null) {
				ShipAPI target = findTarget(ship);
				if (target != null) {
					Global.getCombatEngine().getCustomData().put(targetKey, target);
				}
			}
		} else if (effectLevel >= 1) {
			if (foundTarget instanceof ShipAPI) {
				ShipAPI target = (ShipAPI) foundTarget;
				if (target.getFluxTracker().isOverloadedOrVenting()) target = ship;
				applyEffectToTarget(ship, target);
			}
		} else if (state == State.OUT && foundTarget != null) {
			Global.getCombatEngine().getCustomData().remove(targetKey);
		}
	}

	protected ShipAPI findTarget(ShipAPI ship) {
		float range = ship.getMutableStats().getSystemRangeBonus().computeEffective(DISRUPTION_RANGE);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();
		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM)){
			target = (ShipAPI) ship.getAIFlags().getCustom(AIFlags.TARGET_FOR_SHIP_SYSTEM);
		}
		
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum) target = null;
		} else {
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FIGHTER, range, true);
				} else {
					Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum) target = null;
					}
				}
			}
			if (target == null) {
				target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FIGHTER, range, true);
			}
		}
		if (target == null || target.getFluxTracker().isOverloadedOrVenting()) target = ship;
		
		return target;
	}

	protected void applyEffectToTarget(final ShipAPI ship, final ShipAPI target) {
		if (target.getFluxTracker().isOverloadedOrVenting()) {
			return;
		}
		if (target == ship) return;
		
		target.setOverloadColor(OVERLOAD_COLOR);
		target.getFluxTracker().beginOverloadWithTotalBaseDuration(DISRUPTION_DUR);
		//target.getEngineController().forceFlameout(true);
		
		if (target.getFluxTracker().showFloaty() || 
				ship == Global.getCombatEngine().getPlayerShip() ||
				target == Global.getCombatEngine().getPlayerShip()) {
			target.getFluxTracker().playOverloadSound();
			target.getFluxTracker().showOverloadFloatyIfNeeded("System Disruption!", OVERLOAD_COLOR, 4f, true);
		}
		
		Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (!target.getFluxTracker().isOverloadedOrVenting()) {
					target.resetOverloadColor();
					Global.getCombatEngine().removePlugin(this);
				}
			}
		});
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo() || system.getState() != SystemState.IDLE)
			return null;
		ShipAPI target = findTarget(ship);
		if (target == null || target.isFighter() || target == ship || target.getFluxTracker().isOverloadedOrVenting())
			return "NO TARGET";
		if (MathUtils.getDistance(ship, target) > ship.getMutableStats().getSystemRangeBonus().computeEffective(DISRUPTION_RANGE))
			return "OUT OF RANGE";
		return "READY";
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI target = findTarget(ship);
		return target != null && !target.isFighter() && target != ship && !target.getFluxTracker().isOverloadedOrVenting() && !(MathUtils.getDistance(ship, target) > ship.getMutableStats().getSystemRangeBonus().computeEffective(DISRUPTION_RANGE));
	}
}








