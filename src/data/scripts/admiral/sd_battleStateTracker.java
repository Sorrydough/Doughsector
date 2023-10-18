package data.scripts.admiral;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.*;

import data.scripts.admiral.sd_fleetAdmiralUtil.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.*;
import org.lwjgl.util.vector.Vector2f;


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
    public int deployedAllyDP;
    public int deployedEnemyDP;
    public int numOwnedObjectives;
    public int allySide;
    public int enemySide;
    boolean doOnce = true;
    void reset() {
        assignmentsWithTargets.clear();
        deployedShips.clear();
        deployedAllyShips.clear();
        deployedEnemyShips.clear();
        deployedAllyDP = 0;
        deployedEnemyDP = 0;
        numOwnedObjectives = 0;
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
            if (!ship.isHulk() && !ship.isShuttlePod() && !ship.isFighter())
                deployedShips.add(ship);

        for (BattleObjectiveAPI objective : engine.getObjectives())
            if (objective.getOwner() == allySide)
                numOwnedObjectives += 1;

        for (ShipAPI ship : deployedShips) {
            if (ship.getOwner() == allySide) {
                deployedAllyShips.add(ship);
                deployedAllyDP += getDeploymentCost(ship);
            } else if (ship.getOwner() == enemySide) {
                deployedEnemyShips.add(ship);
                deployedEnemyDP += getDeploymentCost(ship);
            }
        }
        sortByDeploymentCost(deployedAllyShips);
        sortByDeploymentCost(deployedEnemyShips);

        for (AssignmentInfo assignment : allyTaskManager.getAllAssignments())
            assignmentsWithTargets.put(assignment, getObjectAtLocation(assignment.getTarget().getLocation()));
    }
    private static Object getObjectAtLocation(Vector2f location) {
        CombatEngineAPI engine = Global.getCombatEngine();
        for (ShipAPI ship : engine.getShips())
            if (location == ship.getLocation())
                return ship;
        for (BattleObjectiveAPI objective : engine.getObjectives())
            if (location == objective.getLocation())
                return objective;
        return null;
    }
    private static float getDeploymentCost(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        return Math.max(stats.getSuppliesToRecover().base, stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(stats.getSuppliesToRecover().modified));
    }
    private static void sortByDeploymentCost(List<ShipAPI> ships) {
        Collections.sort(ships, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI ship1, ShipAPI ship2) {
                float supplies1 = getDeploymentCost(ship1);
                float supplies2 = getDeploymentCost(ship2);
                return Float.compare(supplies2, supplies1);
            }
        });
    }
}

// TODO: CHECK IF IT'S POSSIBLE FOR ASSIGNMENTS TO BE ON DEAD STUFF
//        for (Map.Entry<CombatFleetManagerAPI.AssignmentInfo, Object> assignment : assignmentsWithTargets.entrySet()) {
//            if (assignment.getValue() instanceof ShipAPI) {
//                ShipAPI target = (ShipAPI) assignment.getValue();
//                if (target.isHulk() || !target.isAlive() || target.isFighter())
//                    allyTaskManager.removeAssignment(assignment.getKey());
//            }
//        }