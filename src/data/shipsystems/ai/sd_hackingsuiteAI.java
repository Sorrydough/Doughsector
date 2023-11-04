package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.admiral.sd_fleetAdmiralUtil;
import data.shipsystems.sd_hackingsuite;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.sd_util;

import java.util.*;
import java.util.List;

public class sd_hackingsuiteAI implements ShipSystemAIScript {
    final Map<ShipAPI.HullSize, Integer> AVG_DPCOST = new HashMap<>(); {
        AVG_DPCOST.put(ShipAPI.HullSize.FRIGATE, 5);
        AVG_DPCOST.put(ShipAPI.HullSize.DESTROYER, 10);
        AVG_DPCOST.put(ShipAPI.HullSize.CRUISER, 20);
        AVG_DPCOST.put(ShipAPI.HullSize.CAPITAL_SHIP, 40);
    }
    List<ShipAPI> targets = new ArrayList<>();
    final IntervalUtil intervalShort = new IntervalUtil(0.01f, 0.01f);
    final IntervalUtil intervalLong = new IntervalUtil(0.5f, 1f);
    final boolean debug = false;
    float systemRange = 0;
    ShipAPI ship;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }
    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (!AIUtils.canUseSystemThisFrame(ship))
            return;
        // this stuff is on a slower interval cuz it's expensive
        intervalLong.advance(amount);
        if (intervalLong.intervalElapsed()) {
            // calculate our system range, kinda important to have
            if (systemRange == 0)
                systemRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship) + ship.getCollisionRadius());
            // keep track of nearby targets
            for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, systemRange))
                if (!targets.contains(enemy) && sd_hackingsuite.isTargetValid(ship, enemy))
                    targets.add(enemy);
            if (!targets.isEmpty())
                for (ShipAPI enemy : new ArrayList<>(targets)) // doing some shenanigans to bypass a concurrent modification exception
                    if (MathUtils.getDistance(ship, enemy) > systemRange || target.getSystem().isOn())
                        targets.remove(enemy);
        }
        // no point going any further if we have no targets ))))
        if (targets.isEmpty())
            return;
        sd_fleetAdmiralUtil.sortByDeploymentCost(targets);
        intervalShort.advance(amount);
        if (intervalShort.intervalElapsed()) {
            float desirePos = 0;
            float desireNeg = 0;
            // We don't want to use the system if:
            // 1. Our flux level is too high
            desireNeg -= (ship.getHardFluxLevel() + ship.getFluxLevel()) * 100;

            // We want to use the system if:
            // 1. A valid target is within range, scaled by the target's DP cost, biggest target prioritized
            for (ShipAPI enemy : targets) {
                float enemyDeployCost = sd_fleetAdmiralUtil.getDeploymentCost(enemy);
                float desireToAttack = 150 * Math.max(2, enemyDeployCost / AVG_DPCOST.get(enemy.getHullSize()));
                if (desireToAttack + desireNeg >= 100) {
                    ship.setShipTarget(target);
                    desirePos += desireToAttack;
                    break;
                }
            }
            sd_util.activateSystem(ship, "sd_hackingsuite", desirePos, desireNeg, debug);
        }
    }
}
