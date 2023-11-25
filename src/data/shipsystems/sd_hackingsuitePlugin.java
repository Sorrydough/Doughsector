package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class sd_hackingsuitePlugin extends BaseEveryFrameCombatPlugin {
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
    final Color fadeColor = new Color(230, 215, 195,100);
    final IntervalUtil TIMER = new IntervalUtil(1f, 1f);
    boolean doOnce = true;
    float duration = 0;
    float time = 0;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        target.fadeToColor(this, fadeColor, 0.5f, 0.5f, 0.75f);
        if (doOnce) {
            duration += DURATION.get(target.getHullSize()) + target.getSystem().getCooldownRemaining();
            target.getFluxTracker().playOverloadSound();
            target.setShipSystemDisabled(true);
            target.getFluxTracker().showOverloadFloatyIfNeeded("System disabled for "+ Math.round(duration) +" seconds!", Color.LIGHT_GRAY, 5, true);
            target.getCustomData().put("sd_hackingsuite", "urmum");
            doOnce = false;
        }
        if (engine.isPaused())
            return; // don't want the timer to progress while the engine is paused
        TIMER.advance(amount);
        if (TIMER.intervalElapsed()) {
            time += 1;
            if (time >= duration) {
                target.setShipSystemDisabled(false);
                target.getCustomData().remove("sd_hackingsuite");
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
