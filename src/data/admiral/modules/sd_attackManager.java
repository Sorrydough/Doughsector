package data.admiral.modules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.*;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.WingRole;
import com.fs.starfarer.combat.entities.Ship;
import data.admiral.sd_fleetadmiralController;
import data.admiral.sd_fleetadmiralUtil;
import data.sd_util;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import java.util.*;

public class sd_attackManager {
    public static void manageAttackedEnemies(sd_fleetadmiralUtil.battlestateTracker battleState) {
        // apply orders
        ShipAPI fighterTarget = findTargetForFighters(battleState);
//        ShipAPI primaryFleetTarget = findFleetTarget(battleState);
        for (ShipAPI enemy : battleState.deployedEnemyShips) {
//            if (enemy == primaryFleetTarget)
//                sd_fleetadmiralUtil.applyAssignment(battleState.enemyFleetManager.getDeployedFleetMember(enemy), CombatAssignmentType.ENGAGE, battleState.allySide);
            if (enemy == fighterTarget)
                sd_fleetadmiralUtil.applyAssignment(battleState.enemyFleetManager.getDeployedFleetMember(enemy), CombatAssignmentType.STRIKE, battleState.allySide);
            if (isTargetVulnerable(enemy))
                sd_fleetadmiralUtil.applyAssignment(battleState.enemyFleetManager.getDeployedFleetMember(enemy), CombatAssignmentType.INTERCEPT, battleState.allySide);
        }
        // rescind orders
        for (Map.Entry<AssignmentInfo, Object> assignment : battleState.assignmentsWithTargets.entrySet()) {
            // check if it's time to rescind any attack orders
//            if (assignment.getKey().getType() == CombatAssignmentType.ENGAGE && assignment.getValue() instanceof ShipAPI)
//                if (assignment.getValue() != primaryFleetTarget)
//                    battleState.allyTaskManager.removeAssignment(assignment.getKey());
            if (assignment.getKey().getType() == CombatAssignmentType.STRIKE && assignment.getValue() instanceof ShipAPI)
                if (assignment.getValue() != fighterTarget)
                    battleState.allyTaskManager.removeAssignment(assignment.getKey());
            if (assignment.getKey().getType() == CombatAssignmentType.INTERCEPT && assignment.getValue() instanceof ShipAPI)
                if (!isTargetVulnerable((ShipAPI) assignment.getValue()))
                    battleState.allyTaskManager.removeAssignment(assignment.getKey());
        }
    }
    private static boolean isTargetVulnerable(ShipAPI target) {
        return target.getFluxLevel() > 0.8 || target.getHardFluxLevel() > 0.7 || target.getFluxTracker().isOverloaded() || target.getEngineController().isFlamedOut();
    }

    private static ShipAPI findFleetTarget(sd_fleetadmiralUtil.battlestateTracker battleState) {
        ShipAPI fleetTarget = null;
        float speed = Float.MAX_VALUE;
        for (ShipAPI enemy : battleState.deployedEnemyShips)
            if (enemy.getMaxSpeed() < speed && !enemy.getHullSpec().isPhase()) {
                speed = enemy.getMaxSpeed();
                fleetTarget = enemy;
            }
        return fleetTarget;
    }

    private static ShipAPI findTargetForFighters(sd_fleetadmiralUtil.battlestateTracker battleState) {
        // Step 1. Create a list of all carriers that are appropriate for attacking with fighters
        HashMap<ShipAPI, Boolean> allCarriers = getCarriersList(battleState, battleState.allySide);
        List<ShipAPI> goodCarriers = new ArrayList<>();
        for (Map.Entry<ShipAPI, Boolean> entry : allCarriers.entrySet())
            if (!entry.getValue())
                goodCarriers.add(entry.getKey());

        sd_fleetadmiralUtil.sortByDeploymentCost(goodCarriers);

        // Step 2. Find the highest flux ship within 4000 range of our biggest carrier
        float fluxLevel = 0;
        ShipAPI bestTarget = null;
        Iterator<Object> iterator = Global.getCombatEngine().getAiGridShips().getCheckIterator(goodCarriers.get(0).getLocation(), 4000, 4000);
        while (iterator.hasNext()) {
            Object nextObject = iterator.next();
            if (nextObject instanceof ShipAPI) {
                ShipAPI enemy = (ShipAPI) nextObject;
                if (enemy.getOwner() == goodCarriers.get(0).getOwner())
                    continue;

                if (enemy.getFluxLevel() > fluxLevel && !enemy.getHullSpec().isPhase()) {
                    fluxLevel = enemy.getFluxLevel();
                    bestTarget = enemy;
                }
            }
        }
        return bestTarget;
    }
    private static HashMap<ShipAPI, Boolean> getCarriersList(sd_fleetadmiralUtil.battlestateTracker battleState, float owner) {
        List<ShipAPI> ships;
        if (owner == battleState.allySide)
            ships = battleState.deployedAllyShips;
        else
            ships = battleState.deployedEnemyShips;

        // boolean is to tell whether it's got bombers
        HashMap<ShipAPI, Boolean> carriers = new HashMap<>();
        for (ShipAPI ship : ships) {
            boolean isBomber = false;
            if (ship.getNumFighterBays() == 0)
                continue;
            for (FighterWingAPI wing : ship.getAllWings())
                if (wing.getRole() == WingRole.BOMBER)
                    isBomber = true;
            carriers.put(ship, isBomber);
        }
        return carriers;
    }
}
