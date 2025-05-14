package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.util.sd_util;
import data.shipsystems.sd_nullifier;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static data.admiral.sd_fleetadmiralUtil.getDeploymentCost;

public class sd_nullifierAI implements ShipSystemAIScript {
    List<ShipAPI> targets = new ArrayList<>();
    final IntervalUtil intervalShort = new IntervalUtil(0.01f, 0.01f), intervalLong = new IntervalUtil(0.5f, 1f);
    float systemRange = 0;
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
            // calculate our system range, kinda important to have
            if (systemRange == 0)
                systemRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship) + ship.getCollisionRadius());
            // keep track of nearby targets
            for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, systemRange))
                if (!targets.contains(enemy) && sd_nullifier.isTargetValid(ship, enemy))
                    targets.add(enemy);
            // somehow system becomes null when a ship dies fun fact
            if (!targets.isEmpty())
                targets.removeIf(enemy -> !sd_nullifier.isTargetValid(ship, enemy));
        }
        // no point going any further if we have no targets ))))
        if (targets.isEmpty())
            return;

        // First, sort by timeflow. Highest timeflow goes at the top.
        // Second, sort by how much PPT has degraded. Lowest PPT remaining goes at the top when comparing two ships with same timeflow.
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
                if (!sd_nullifier.isTargetValid(ship, target)) // doing this again even though we do it earlier because of the slow interval, need to make sure the target isn't dead or it npes
                    continue;
                float enemyDeployCost = getDeploymentCost(enemy);
                float baseDesire = 150;

                float desireToAttack = baseDesire * 1.5f;

                if (desireToAttack + desireNeg >= 100) {
                    ship.setShipTarget(target);
                    desirePos += desireToAttack;
                    break; // break when a target has been selected, the list of potential targets is sorted so we know we're always selecting the best target
                }
            }
            sd_util.activateSystem(ship, "sd_nullifier", desirePos, desireNeg, false);
        }
    }

    public static void sortByPriority(final List<ShipAPI> ships) {
        ships.sort((ship1, ship2) -> {
            // Check if ship's PPT ratio and timeflow
            float timeflow1 = getTargetTimeflow(ship1);
            float timeflow2 = getTargetTimeflow(ship2);
            float ppt1 = getRemainingPPT(ship1);
            float ppt2 = getRemainingPPT(ship2);

            // Priority order: Highest timeflow first, lowest PPT second
            if (timeflow1 == timeflow2)
                return Float.compare(ppt2, ppt1);
            else
                return Float.compare(timeflow2, timeflow1);
        });
    }

    static float getTargetTimeflow(ShipAPI ship) {
        float baseTimeFlow = ship.getMutableStats().getDynamic().getMod("sd_baseTimeMult").flatBonus;
        if (baseTimeFlow == 0)
            baseTimeFlow = ship.getMutableStats().getTimeMult().modified;
        return baseTimeFlow;
    }
    static float getRemainingPPT(ShipAPI ship) {
        return ship.getPeakTimeRemaining() / ship.getMutableStats().getPeakCRDuration().computeEffective(ship.getHullSpec().getNoCRLossTime());
    }
}
    // top priority: any ship that's got improved timeflow
    // we want to target the ship with the highest baseline timeflow that's available
    // second priority: the ship with the lowest remaining PPT as a percentage of its total
    // third prioity: the ship with the lowest remaining CR
    // final priority: choose the highest DP ship out of the above
    // additional criteria: if one of our ships is already targeting something, we also want to target it too so we can pile on the effect
