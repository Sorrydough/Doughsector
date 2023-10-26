package data.scripts.admiral.modules;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.*;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.admiral.sd_battleStateTracker;
import data.scripts.admiral.sd_fleetAdmiralUtil;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Map;

public class sd_attackManager {
    public static void manageAttackedEnemies(sd_battleStateTracker battleState) {
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
                    sd_fleetAdmiralUtil.applyAssignment(battleState.enemyFleetManager.getDeployedFleetMember(enemy), CombatAssignmentType.INTERCEPT, battleState.allySide);
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
