package data.scripts.admiral.modules;

import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.admiral.sd_battleStateTracker;
import data.scripts.admiral.sd_fleetAdmiralUtil;

import java.util.Map;

public class sd_formationManager {
    public static void manageFormation(sd_battleStateTracker battleState) {
        boolean largestAllyDefended = false;
        boolean shouldBeDefensive = battleState.deployedEnemyDP > battleState.deployedAllyDP || battleState.averageAllySpeed < battleState.averageEnemySpeed;
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
        //if we need to play defensively, apply a defend order to the biggest ship
        if (!largestAllyDefended && shouldBeDefensive)
            sd_fleetAdmiralUtil.applyAssignment(battleState.allyFleetManager.getDeployedFleetMember(battleState.deployedAllyShips.get(0)), CombatAssignmentType.DEFEND, battleState.allySide);
    }
}
