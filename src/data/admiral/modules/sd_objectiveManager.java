package data.admiral.modules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.BattleObjectives;
import data.admiral.sd_battlestateTracker;
import data.admiral.sd_fleetadmiralUtil;

import java.util.Map;
import java.util.Objects;

public class sd_objectiveManager {
    public static void manageAttackedObjectives(sd_battlestateTracker battleState) {
        boolean isAttackingObjective = false;
        boolean doHaveAllObjectives = false;
        for (Map.Entry<CombatFleetManagerAPI.AssignmentInfo, Object> assignment : battleState.assignmentsWithTargets.entrySet()) {
            if (assignment.getValue() instanceof BattleObjectiveAPI) {
                //once we own an objective we can clear its associated order, this is done to keep the fleet from being strung out across multiple objectives
                //this is also beneficial because if we own all the objectives our fleet will automatically free roam
                if (assignment.getKey().getType() == CombatAssignmentType.DEFEND)
                    battleState.allyTaskManager.removeAssignment(assignment.getKey());
                //if we're attacking an objective set the flag, so we know not to attack a new objective later
                if (assignment.getKey().getType() == CombatAssignmentType.ASSAULT)
                    isAttackingObjective = true;
            }
        }

        //keep track of whether we own all the objectives
        int numObjectives = battleState.engine.getObjectives().size();
        if (battleState.numOwnedObjectives == numObjectives)
            doHaveAllObjectives = true;
        //if we're not attacking an objective & we don't own all objectives then attack one
        if (!doHaveAllObjectives && !isAttackingObjective)
            attackObjective(battleState.allySide);
    }
    private static void attackObjective(int owner) {
        for (BattleObjectiveAPI objective : Global.getCombatEngine().getObjectives()) {
            if (Objects.equals(objective.getType(), BattleObjectives.SENSOR_JAMMER) && objective.getOwner() != owner) {
                sd_fleetadmiralUtil.applyAssignment(objective, CombatAssignmentType.ASSAULT, owner);
                break;
            } else if (Objects.equals(objective.getType(), BattleObjectives.NAV_BUOY) && objective.getOwner() != owner) {
                sd_fleetadmiralUtil.applyAssignment(objective, CombatAssignmentType.ASSAULT, owner);
                break;
            } else if (Objects.equals(objective.getType(), BattleObjectives.COMM_RELAY) && objective.getOwner() != owner) {
                sd_fleetadmiralUtil.applyAssignment(objective, CombatAssignmentType.ASSAULT, owner);
                break;
            }
        }
    }
}
