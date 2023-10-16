package data.scripts.admiral.modules;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.admiral.sd_fleetAdmiralUtil;

import java.util.List;

public class sd_admiralExecutionManager {
//    public static void attackTarget(sd_FleetManager manager) {
//        //if an enemy ship is fluxed out, put an engage order on it if it doesn't already have one
//        for (ShipAPI enemy : enemies) {
//            if (enemy.getFluxLevel() > 0.75 || enemy.getHardFluxLevel() > 0.65 || enemy.getFluxTracker().isOverloaded() || enemy.getEngineController().isFlamedOut()) {
//                boolean isEnemyEngaged = false;
//                for (sd_fleetAdmiralUtil.AssignmentInfoWithTarget assignment : assignmentsWithTargets) {
//                    if (enemy == assignment.getObject()) {
//                        isEnemyEngaged = true;
//                        break;
//                    }
//                }
//                if (!isEnemyEngaged) {
//                    sd_fleetAdmiralUtil.applyAssignment(engine.getFleetManager(enemySide).getDeployedFleetMember(enemy), CombatAssignmentType.INTERCEPT, allySide);
//                }
//            }
//        }
//    }
}
