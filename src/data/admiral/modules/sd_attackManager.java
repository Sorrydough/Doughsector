package data.admiral.modules;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.*;
import com.fs.starfarer.api.combat.ShipAPI;
import data.admiral.sd_battlestateTracker;
import data.admiral.sd_fleetadmiralUtil;

import java.util.Map;

public class sd_attackManager {
    public static void manageAttackedEnemies(sd_battlestateTracker battleState) {
        // if an enemy ship is fluxed out, put an engage order on it if it doesn't already have one
        for (ShipAPI enemy : battleState.deployedEnemyShips)
            if (isTargetVulnerable(enemy)) {
                boolean isEnemyEngaged = false;
                for (Map.Entry<AssignmentInfo, Object> assignment : battleState.assignmentsWithTargets.entrySet())
                    if (enemy == assignment.getValue()) {
                        isEnemyEngaged = true;
                        break;
                    }
                if (!isEnemyEngaged)
                    sd_fleetadmiralUtil.applyAssignment(battleState.enemyFleetManager.getDeployedFleetMember(enemy), CombatAssignmentType.INTERCEPT, battleState.allySide);
            }
        for (Map.Entry<AssignmentInfo, Object> assignment : battleState.assignmentsWithTargets.entrySet())
            // check if it's time to rescind any attack orders
            if (assignment.getKey().getType() == CombatAssignmentType.INTERCEPT && assignment.getValue() instanceof ShipAPI)
                if (!isTargetVulnerable((ShipAPI) assignment.getValue()))
                    battleState.allyTaskManager.removeAssignment(assignment.getKey());
    }
    private static boolean isTargetVulnerable(ShipAPI target) {
        return target.getFluxLevel() > 0.8 || target.getHardFluxLevel() > 0.7 || target.getFluxTracker().isOverloaded() || target.getEngineController().isFlamedOut();
    }
}
