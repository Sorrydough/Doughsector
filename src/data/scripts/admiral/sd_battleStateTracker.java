package data.scripts.admiral;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.*;

import java.util.*;

public class sd_battleStateTracker { // this class doesn't do anything per se, it's just an object that holds data to be accessed by other classes
    public CombatEngineAPI engine;
    public CombatFleetManagerAPI allyFleetManager;
    public CombatFleetManagerAPI enemyFleetManager;
    public CombatTaskManagerAPI allyTaskManager;
    public CombatTaskManagerAPI enemyTaskManager;
    public final List<ShipAPI> deployedShips = new ArrayList<>();
    public final List<ShipAPI> deployedAllyShips = new ArrayList<>();
    public final List<ShipAPI> deployedEnemyShips = new ArrayList<>();
    public final HashMap<AssignmentInfo, Object> assignmentsWithTargets = new HashMap<>();
    public final HashMap<ShipAPI, AssignmentInfo> shipsWithTargetAssignments = new HashMap<>();
    public float deployedAllyThreat;
    public float deployedEnemyThreat;
    public int numOwnedObjectives;
    public int allySide;
    public int enemySide;
    public float averageAllySpeed;
    public float averageEnemySpeed;
    boolean doOnce = true;
    void reset() {
        assignmentsWithTargets.clear();
        shipsWithTargetAssignments.clear();
        deployedShips.clear();
        deployedAllyShips.clear();
        deployedEnemyShips.clear();
        deployedAllyThreat = 0;
        deployedEnemyThreat = 0;
        numOwnedObjectives = 0;
        averageAllySpeed = 0;
        averageEnemySpeed = 0;
    }
    public void updateState(CombatEngineAPI combatEngine, int allyOwner, int enemyOwner) {
        // need to reset all the fields that we don't want to preserve between updates
        reset();
        // updateState gets called and we populate the fields, then we can pass the object to other classes for them to also get that info
        // doing it this way so stuff doesn't have to get recalculated repeatedly and I don't have 10,000 static classes in a util the size of your mom
        if (doOnce) {
            engine = combatEngine;
            allySide = allyOwner;
            enemySide = enemyOwner;
            allyFleetManager = engine.getFleetManager(allySide);
            allyTaskManager = allyFleetManager.getTaskManager(false);
            enemyFleetManager = engine.getFleetManager(enemySide);
            enemyTaskManager = enemyFleetManager.getTaskManager(false);
            doOnce = false;
        }

        for (ShipAPI ship : engine.getShips())
            if (ship.getFleetMember() != null && !ship.isHulk() && !ship.isShuttlePod() && !ship.isFighter()) // need to nullcheck fleetmember to prevent a crash with stations
                deployedShips.add(ship);

        for (BattleObjectiveAPI objective : engine.getObjectives())
            if (objective.getOwner() == allySide)
                numOwnedObjectives += 1;

        boolean isPlayer = allySide == 0;
        for (ShipAPI ship : deployedShips) {
            if (!isPlayer && ship.getOwner() == allySide) {
                deployedAllyShips.add(ship);
                deployedAllyThreat += sd_fleetAdmiralUtil.getCombatEffectiveness(ship);
            } else if (isPlayer && ship.getOwner() == allySide && !ship.isAlly()) { // making sure we don't confuse reinforcing ships for the player's ships
                deployedAllyShips.add(ship);
                deployedAllyThreat += sd_fleetAdmiralUtil.getCombatEffectiveness(ship);
            } else if (ship.getOwner() == enemySide) {
                deployedEnemyShips.add(ship);
                deployedEnemyThreat += sd_fleetAdmiralUtil.getCombatEffectiveness(ship);
            }
        }
        sd_fleetAdmiralUtil.sortByDeploymentCost(deployedAllyShips);
        sd_fleetAdmiralUtil.sortByDeploymentCost(deployedEnemyShips);

        for (ShipAPI ship : deployedAllyShips)
            averageAllySpeed += ship.getMaxSpeed();
        averageAllySpeed /= Math.max(1, deployedAllyShips.size());

        for (ShipAPI ship : deployedEnemyShips)
            averageEnemySpeed += ship.getMaxSpeed();
        averageEnemySpeed /= Math.max(1, deployedEnemyShips.size());

        for (AssignmentInfo assignment : allyTaskManager.getAllAssignments())
            assignmentsWithTargets.put(assignment, getAssignmentTarget(assignment.getTarget()));

        for (ShipAPI ship : deployedAllyShips)
            if (allyTaskManager.getAssignmentFor(ship) != null)
                shipsWithTargetAssignments.put(ship, allyTaskManager.getAssignmentFor(ship));
    }
    public CombatFleetManagerAPI getFleetmanager(int owner) {
        if (owner == allySide)
            return allyFleetManager;
        if (owner == enemySide)
            return enemyFleetManager;
        return null;
    }
    public static Object getAssignmentTarget(AssignmentTargetAPI assignment) {
        if (assignment instanceof DeployedFleetMemberAPI)
            return ((DeployedFleetMemberAPI) assignment).getShip();
        if (assignment instanceof BattleObjectiveAPI)
            return assignment;
        return null;
    }
}