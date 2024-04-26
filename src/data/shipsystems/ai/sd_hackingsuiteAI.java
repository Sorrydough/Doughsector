package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.admiral.sd_fleetadmiralUtil;
import data.shipsystems.sd_hackingsuite;

import org.lazywizard.console.Console;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import data.sd_util;

import java.util.*;
import java.util.List;

import static data.admiral.sd_fleetadmiralUtil.getDeploymentCost;

public class sd_hackingsuiteAI implements ShipSystemAIScript {
    final Map<ShipAPI.HullSize, Integer> AVG_DPCOST = new HashMap<>(); {
        AVG_DPCOST.put(ShipAPI.HullSize.FRIGATE, 5);
        AVG_DPCOST.put(ShipAPI.HullSize.DESTROYER, 10);
        AVG_DPCOST.put(ShipAPI.HullSize.CRUISER, 20);
        AVG_DPCOST.put(ShipAPI.HullSize.CAPITAL_SHIP, 40);
    }
    List<ShipAPI> targets = new ArrayList<>();
    final IntervalUtil intervalShort = new IntervalUtil(0.01f, 0.01f), intervalLong = new IntervalUtil(0.5f, 1f);
    float systemRange = 0;
    boolean isLinkedEnemy = false;
    boolean isAutomatedEnemy = false;
    ShipAPI ship;
    ShipSystemAPI system;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }
    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (!sd_util.isCombatSituation(ship) || !sd_util.canUseSystemThisFrame(ship))
            return;
        // this stuff is on a slower interval cuz it's expensive
        intervalLong.advance(amount);
        if (intervalLong.intervalElapsed()) {
            // check if the enemy has anything we might want to save charges for
            List<ShipAPI> deployedEnemyShips = new ArrayList<>();
            for (ShipAPI other : Global.getCombatEngine().getShips())
                if (sd_fleetadmiralUtil.isDeployedShip(other) && other.getOwner() != ship.getOwner())
                    deployedEnemyShips.add(other);

            for (ShipAPI enemy : deployedEnemyShips) {
                boolean foundLinked = false;
                boolean foundAutomated = false;
                if (sd_util.isLinked(enemy)) {
                    isLinkedEnemy = true;
                    foundLinked = true;
                }
                if (sd_util.isAutomated(enemy)) {
                    isAutomatedEnemy = true;
                    foundAutomated = true;
                }
                if (!foundLinked)
                    isLinkedEnemy = false;
                if (!foundAutomated)
                    isAutomatedEnemy = false;
            }

            // calculate our system range, kinda important to have
            if (systemRange == 0)
                systemRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship) + ship.getCollisionRadius());
            // keep track of nearby targets
            for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, systemRange))
                if (!targets.contains(enemy) && sd_hackingsuite.isTargetValid(ship, enemy))
                    targets.add(enemy);
            if (!targets.isEmpty())
                for (ShipAPI enemy : new ArrayList<>(targets)) // doing some shenanigans to bypass a concurrent modification exception
                    if (!sd_hackingsuite.isTargetValid(ship, enemy)) // somehow system becomes null when a ship dies fun fact
                        targets.remove(enemy);
        }
        // no point going any further if we have no targets ))))
        if (targets.isEmpty())
            return;

        // neural linked ships are top priority, then automated ships, then finally crewed ships
        sortByPriority(targets);

        intervalShort.advance(amount);
        if (intervalShort.intervalElapsed()) {
            float desirePos = 0;
            float desireNeg = 0;
            // We don't want to use the system if:
            // Our flux level is too high
            desireNeg -= (ship.getHardFluxLevel() + ship.getFluxLevel()) * 100;

            // We want to use the system if:
            // A valid target is within range, scaled by the target's DP cost, biggest target prioritized, automated or neural linked ships extra preferred
            for (ShipAPI enemy : targets) {
                if (!sd_hackingsuite.isTargetValid(ship, target)) // doing this again even though we do it earlier because of the slow interval, need to make sure the target isn't dead or it npes
                    continue;
                float enemyDeployCost = getDeploymentCost(enemy);
                float baseDesire = 150;
                if (isLinkedEnemy || isAutomatedEnemy)
                    baseDesire = 100; // if there's a high priority target on the field, reduce our willingness to use the system on normal ships
                float desireToAttack = baseDesire * Math.max(2, enemyDeployCost / AVG_DPCOST.get(enemy.getHullSize()));
                // modulate attack desire based on number of charges
                desireToAttack *= (float) system.getAmmo() / system.getMaxAmmo();
                // bonus desire if the target is high priority, these stack so an automated linked ship will get spammed
                if (sd_util.isLinked(target))
                    desireToAttack *= 1.25;
                if (sd_util.isAutomated(target))
                    desireToAttack *= 1.25;

                if (desireToAttack + desireNeg >= 100) {
                    ship.setShipTarget(target);
                    desirePos += desireToAttack;
                    break; // break when a target has been selected, the list of potential targets is sorted by highest DP first so we know we're always selecting the best target
                }
            }
            sd_util.activateSystem(ship, "sd_hackingsuite", desirePos, desireNeg, false);
        }
    }

    public static void sortByPriority(final List<ShipAPI> ships) { // chatgpt wrote this, it only had one minor bug where it inverted some true/false checks
        ships.sort((ship1, ship2) -> {
            // Check if ships are neural-linked and crewed
            boolean isNeuralLinked1 = sd_util.isLinked(ship1);
            boolean isNeuralLinked2 = sd_util.isLinked(ship2);
            boolean isCrewed1 = !sd_util.isAutomated(ship1);
            boolean isCrewed2 = !sd_util.isAutomated(ship2);

            // Priority order: Neural-linked Automated, Neural-linked Crewed, Automated, Crewed
            if (isNeuralLinked1 && isCrewed1) {
                if (isNeuralLinked2 && isCrewed2) {
                    // Both neural-linked crewed ships, compare by deployment cost
                    return Float.compare(getDeploymentCost(ship2), getDeploymentCost(ship1));
                } else if (isNeuralLinked2) {
                    // Only ship1 is neural-linked crewed, but ship2 is neural-linked automated
                    return 1;
                } else {
                    // Only ship1 is neural-linked crewed, and ship2 is not neural-linked
                    return -1;
                }
            } else if (isNeuralLinked2 && isCrewed2) {
                // Only ship2 is neural-linked crewed
                return 1;
            } else if (isNeuralLinked1) {
                // Only ship1 is neural-linked automated
                return -1;
            } else if (isNeuralLinked2) {
                // Only ship2 is neural-linked automated
                return 1;
            } else if (!isCrewed1) {
                // Only ship1 is automated
                return -1;
            } else if (!isCrewed2) {
                // Only ship2 is automated
                return 1;
            } else {
                // Neither ships are automated, compare by deployment cost
                return Float.compare(getDeploymentCost(ship2), getDeploymentCost(ship1));
            }
        });
    }
}
