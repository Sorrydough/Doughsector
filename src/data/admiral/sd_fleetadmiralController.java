package data.admiral;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.admiral.modules.sd_attackManager;
import data.admiral.modules.sd_formationManager;
import data.admiral.modules.sd_objectiveManager;
import org.lazywizard.console.Console;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class sd_fleetadmiralController extends BaseEveryFrameCombatPlugin {
    public final battlestateTracker battleState = new battlestateTracker();
    private final IntervalUtil interval = new IntervalUtil(0.5f, 2); // variable to approximate human reaction time
    private boolean doInit = true;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || Global.getCurrentState() != GameState.COMBAT || engine.isSimulation() || engine.isMission())
            return;
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            battleState.updateState(engine, 0, 1); // player side is 0, AI side is 1
            // ^ updating battle state is expensive so we don't do it every frame
            if (doInit) {
                Console.showMessage("Admiral Controller mounted for fleet side " + battleState.allySide);
                for (CombatFleetManagerAPI.AssignmentInfo assignment : battleState.allyTaskManager.getAllAssignments())
                    battleState.allyTaskManager.removeAssignment(assignment); // need to wipe all assignments that might've been created by alex or the player before the controller was mounted
                doInit = false;
            }

            //Console.showMessage("Allied DP: "+ battleState.deployedAllyDP +" Enemy DP: "+ battleState.deployedEnemyDP);

            if (!battleState.deployedAllyShips.isEmpty()) {
                sd_formationManager.manageFormation(battleState);
                sd_objectiveManager.manageAttackedObjectives(battleState);
                sd_attackManager.manageAttackedEnemies(battleState);
            }

            for (CombatFleetManagerAPI.AssignmentInfo assignment : battleState.allyTaskManager.getAllAssignments())
                engine.addFloatingText(assignment.getTarget().getLocation(), assignment.getType().name(), 100, Color.LIGHT_GRAY, null, 1, 5);
        }
    }
    public static class battlestateTracker { // this class doesn't do anything per se, it's just an object that holds data to be accessed by other classes
        public CombatEngineAPI engine;
        public CombatFleetManagerAPI allyFleetManager;
        public CombatFleetManagerAPI enemyFleetManager;
        public CombatTaskManagerAPI allyTaskManager;
        public CombatTaskManagerAPI enemyTaskManager;
        public final List<ShipAPI> deployedShips = new ArrayList<>();
        public final List<ShipAPI> deployedAllyShips = new ArrayList<>();
        public final List<ShipAPI> deployedEnemyShips = new ArrayList<>();
        public final HashMap<CombatFleetManagerAPI.AssignmentInfo, Object> assignmentsWithTargets = new HashMap<>();
        public final HashMap<ShipAPI, CombatFleetManagerAPI.AssignmentInfo> shipsWithTargetAssignments = new HashMap<>();
        public float deployedAllyThreat;
        public float deployedEnemyThreat;
        public int numOwnedObjectives;
        public int allySide;
        public int enemySide;
        public float averageAllySpeed;
        public float averageEnemySpeed;
        boolean runOnce = true;
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
            if (runOnce) {
                engine = combatEngine;
                allySide = allyOwner;
                enemySide = enemyOwner;
                allyFleetManager = engine.getFleetManager(allySide);
                allyTaskManager = allyFleetManager.getTaskManager(false);
                enemyFleetManager = engine.getFleetManager(enemySide);
                enemyTaskManager = enemyFleetManager.getTaskManager(false);
                runOnce = false;
            }

            for (ShipAPI ship : engine.getShips())
                if (sd_fleetadmiralUtil.isDeployedShip(ship))
                    deployedShips.add(ship);

            for (BattleObjectiveAPI objective : engine.getObjectives())
                if (objective.getOwner() == allySide)
                    numOwnedObjectives += 1;

            boolean isPlayer = allySide == 0; // todo: fix this so the admiral works when the AI reinforces a player fleet
            for (ShipAPI ship : deployedShips) {
                if (!isPlayer && ship.getOwner() == allySide) {
                    deployedAllyShips.add(ship);
                    deployedAllyThreat += sd_fleetadmiralUtil.getCombatEffectiveness(ship, 0.2f);
                } else if (isPlayer && ship.getOwner() == allySide && !ship.isAlly()) { // making sure we don't confuse reinforcing ships for the player's ships
                    deployedAllyShips.add(ship);
                    deployedAllyThreat += sd_fleetadmiralUtil.getCombatEffectiveness(ship, 0.2f);
                } else if (ship.getOwner() == enemySide) {
                    deployedEnemyShips.add(ship);
                    deployedEnemyThreat += sd_fleetadmiralUtil.getCombatEffectiveness(ship, 0.2f);
                }
            }
            sd_fleetadmiralUtil.sortByDeploymentCost(deployedAllyShips);
            sd_fleetadmiralUtil.sortByDeploymentCost(deployedEnemyShips);

            for (ShipAPI ship : deployedAllyShips)
                averageAllySpeed += ship.getMaxSpeed();
            averageAllySpeed /= Math.max(1, deployedAllyShips.size());

            for (ShipAPI ship : deployedEnemyShips)
                averageEnemySpeed += ship.getMaxSpeed();
            averageEnemySpeed /= Math.max(1, deployedEnemyShips.size());

            for (CombatFleetManagerAPI.AssignmentInfo assignment : allyTaskManager.getAllAssignments())
                assignmentsWithTargets.put(assignment, getAssignmentTarget(assignment.getTarget()));

            for (ShipAPI ship : deployedAllyShips)
                if (allyTaskManager.getAssignmentFor(ship) != null)
                    shipsWithTargetAssignments.put(ship, allyTaskManager.getAssignmentFor(ship));

            //for (battleGridSquare square : )


        }
//        static class battleGridSquare {
//            List<ShipAPI> allyShips = new ArrayList<>();
//            List<ShipAPI> enemyShips = new ArrayList<>();
//            float allyThreat = 0;
//            float enemyThreat = 0;
//
//            public void addAllyShip(ShipAPI ship) {
//                allyShips.add(ship);
//            }
//
//            public void removeShip(ShipAPI ship) {
//                enemyShips.remove(ship);
//            }
//
////        public List<String> getShips() {
////            return ships;
////        }
//
//            public List<ShipAPI> getAllyShips() {
//                return allyShips;
//            }
//            public List<ShipAPI> getEnemyShips() {
//                return enemyShips;
//            }
//            public float getEnemyThreat() {
//                return enemyThreat;
//            }
//            public float getAllyThreat() {
//                return allyThreat;
//            }
//
//
//
//        }
        public CombatFleetManagerAPI getFleetmanager(int owner) {
            if (owner == allySide)
                return allyFleetManager;
            if (owner == enemySide)
                return enemyFleetManager;
            return null;
        }
        public Object getAssignmentTarget(AssignmentTargetAPI assignment) {
            if (assignment instanceof DeployedFleetMemberAPI)
                return ((DeployedFleetMemberAPI) assignment).getShip();
            if (assignment instanceof BattleObjectiveAPI)
                return assignment;
            return null;
        }
        public List<ShipAPI> getShipsAssigned(CombatFleetManagerAPI.AssignmentInfo assignment) {
            List<ShipAPI> ships = new ArrayList<>();
            for (ShipAPI ship : deployedAllyShips)
                if (allyTaskManager.getAssignmentFor(ship) == assignment)
                    ships.add(ship);
            return ships;
        }
    }
}