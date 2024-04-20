package data.admiral.modules;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.admiral.sd_fleetadmiralController;
import data.admiral.sd_fleetadmiralUtil;

import java.util.Map;

public class sd_formationManager {
    public static void manageFormation(sd_fleetadmiralUtil.battlestateTracker battleState) {
        boolean largestAllyDefended = false;
        boolean shouldBeDefensive = battleState.averageAllySpeed < battleState.averageEnemySpeed; //battleState.deployedEnemyThreat > battleState.deployedAllyThreat ||
        for (Map.Entry<CombatFleetManagerAPI.AssignmentInfo, Object> assignment : battleState.assignmentsWithTargets.entrySet()) {
            if (assignment.getKey().getType() == CombatAssignmentType.DEFEND && assignment.getValue() instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) assignment.getValue();
                // don't apply a defend order to a ship that already has one
                if (ship == battleState.deployedAllyShips.get(0) && shouldBeDefensive)
                    largestAllyDefended = true;
                // remove defend order if the battle state shifts in our favor
                else if (ship == battleState.deployedAllyShips.get(0) && !shouldBeDefensive)
                    battleState.allyTaskManager.removeAssignment(assignment.getKey());
            }
        }
        // if we need to play defensively, apply a defend order to the biggest ship
        if (!largestAllyDefended && shouldBeDefensive)
            sd_fleetadmiralUtil.applyAssignment(battleState.allyFleetManager.getDeployedFleetMember(battleState.deployedAllyShips.get(0)), CombatAssignmentType.DEFEND, battleState.allySide);
    }
        // todo: check for the largest ship that isn't out of position, instead of the largest ship generally
}       // todo: fix bug where if a small ship gets a defend order, it becomes stuck there after a bigger ship deploys
        // todo: criteria to go on the offense (use engage orders on weak enemy pockets)
        // todo: slice the battlespace up into a grid and track the state of each square