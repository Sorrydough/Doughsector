package data.scripts.admiral;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;


public class sd_fleetAdmiralUtil {
    public static void applyAssignment(AssignmentTargetAPI target, CombatAssignmentType assignment, int owner) {
        Global.getCombatEngine().getFleetManager(owner).getTaskManager(false).createAssignment(assignment, target, false);
    }
    public static float getDeploymentCost(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        return Math.max(stats.getSuppliesToRecover().base, stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(stats.getSuppliesToRecover().modified));
    }
    public static float calculateCombatEffectiveness(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        float CombatEffectiveness = getDeploymentCost(ship);
        if (stats.getVariant().hasHullMod("safetyoverrides")) // change this to use a map
            CombatEffectiveness *= 1.25;
        return CombatEffectiveness;
    }
    public static Object getObjectAtLocation(Vector2f location) {
        CombatEngineAPI engine = Global.getCombatEngine();
        for (ShipAPI ship : engine.getShips())
            if (location == ship.getLocation())
                return ship;
        for (BattleObjectiveAPI objective : engine.getObjectives())
            if (location == objective.getLocation())
                return objective;
        return null;
    }
    public static void sortByDeploymentCost(List<ShipAPI> ships) {
        Collections.sort(ships, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI ship1, ShipAPI ship2) {
                float supplies1 = getDeploymentCost(ship1);
                float supplies2 = getDeploymentCost(ship2);
                return Float.compare(supplies2, supplies1);
            }
        });
    }
    public static float getShipStrengthAssigned(CombatFleetManagerAPI.AssignmentInfo assignment, sd_battleStateTracker battleState) {
        List<ShipAPI> assignedToTarget = new ArrayList<>();
        for (Map.Entry<ShipAPI, CombatFleetManagerAPI.AssignmentInfo> ship : battleState.shipsWithTargetAssignments.entrySet()) {
            if (ship.getValue() == assignment) {
                assignedToTarget.add(ship.getKey());
            }
        }
        float strength = 0;
        for (ShipAPI ship : assignedToTarget) {
            strength += calculateCombatEffectiveness(ship);
        }
        return strength;
    }
    public static float calculateThreatLevel(CombatFleetManagerAPI.AssignmentInfo assignment, float radius, sd_battleStateTracker battleState) {
        float threat = 0;
        List<ShipAPI> assignedToTarget = new ArrayList<>();
        for (Map.Entry<ShipAPI, CombatFleetManagerAPI.AssignmentInfo> ship : battleState.shipsWithTargetAssignments.entrySet()) {
            if (ship.getValue() == assignment) {
                assignedToTarget.add(ship.getKey());
            }
        }





        for (ShipAPI enemy : battleState.deployedEnemyShips) {


        }





        return threat; // ur mum
    }

}
