package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.ai.AssignmentModulePlugin;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.BattleObjectives;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Objectives;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.campaign.ai.AssignmentModule;
import com.fs.starfarer.combat.CombatFleetManager;
import com.fs.starfarer.combat.entities.BattleObjective;
import javafx.util.Pair;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class sd_fleetAdmiralAI implements AdmiralAIPlugin {
    boolean debug = true;
    boolean doInit = true;
    boolean isAttackingObjective = false;
    boolean doHaveAllObjectives = false;
    CombatEngineAPI engine;
    CombatFleetManagerAPI fleetManager;
    CombatTaskManagerAPI taskManager;
    IntervalUtil interval = new IntervalUtil(1, 3); //runs every 1 to 3 seconds to approximate a human's variable reaction time

    @Override
    public void preCombat() {
    }

    @Override
    public void advance(float amount) {
        if (doInit) {
            Console.showMessage("Admiral Init");
            engine = Global.getCombatEngine();
            fleetManager = engine.getFleetManager(1);
            taskManager = fleetManager.getTaskManager(true);
            //as part of init, we need to wipe all assignments that might've been created by alex's admiral
            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                taskManager.removeAssignment(assignment);
            }
            doInit = false;
        }

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            //make a list of all allies and all enemies
            List<ShipAPI> allies = new ArrayList<>();
            List<ShipAPI> enemies = new ArrayList<>();
            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                if (ship.getOwner() == 1 && !ship.isHulk() && !ship.isShuttlePod() && !ship.isFighter()) {
                    allies.add(ship);
                } else if (ship.getOwner() == 0 && !ship.isHulk() && !ship.isShuttlePod() && !ship.isFighter()) {
                    enemies.add(ship);
                }
            }

            //sort the lists based on ship size (aka recovery cost)
            sortByRecoveryCost(allies);
            sortByRecoveryCost(enemies);

            //make a list of all assignments and what they're attached to
            List<AssignmentInfoWithTarget> assignmentsWithTargets = new ArrayList<>();
            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                    if (assignment.getTarget().getLocation() == ship.getLocation()) {
                        //add the 'ShipAPI ship' to a new list of ships that have an assignment, alongside its associated 'AssignmentInfo assignment'
                        assignmentsWithTargets.add(new AssignmentInfoWithTarget(assignment, ship));
                    }
                }
                for (BattleObjectiveAPI objective : engine.getObjectives()) {
                    if (assignment.getTarget().getLocation() == objective.getLocation()) {
                        //same again, the goal is to keep track of what assignments are attached to what objects
                        assignmentsWithTargets.add(new AssignmentInfoWithTarget(assignment, objective));
                    }
                }
            }

            //if there's an assignment on something that shouldn't have it, then remove it
            for (AssignmentInfoWithTarget assignment : assignmentsWithTargets) {
                if (assignment.getObject() instanceof ShipAPI) {
                    ShipAPI target = (ShipAPI) assignment.getObject();
                    //ensure we never have assignments on dead stuff, fighters, etc
                    if (target.isHulk() || !target.isAlive() || target.isFighter())  {
                        taskManager.removeAssignment(assignment.getAssignment());
                    }
                    //if an enemy ship had an engage order applied due to being fluxed out, check if it's time to rescind the order
                    if (assignment.getAssignment().getType() == CombatAssignmentType.ENGAGE && target.getHardFluxLevel() < 0.75) {
                        taskManager.removeAssignment(assignment.getAssignment());
                    }
                }
                if (assignment.getObject() instanceof BattleObjectiveAPI) {
                    //once we own an objective we can clear its associated order, this is done to keep the fleet from being strung out across multiple objectives
                    //this is also beneficial because if we own all the objectives our fleet will automatically free roam
                    if (assignment.getAssignment().getType() == CombatAssignmentType.DEFEND) {
                        taskManager.removeAssignment(assignment.getAssignment());
                    }
                    //if we're attacking an objective set the flag, so we know not to set an objective to attack later
                    if (assignment.getAssignment().getType() == CombatAssignmentType.ASSAULT) {
                        isAttackingObjective = true;
                    }
                }
            }

            //keep track of whether we own all the objectives
            for (BattleObjectiveAPI objective : engine.getObjectives()) {
                if (objective.getOwner() != 1) {
                    doHaveAllObjectives = false;
                    break;
                }
                doHaveAllObjectives = true;
            }

            //if we're not attacking an objective, and we don't own all objectives then attack one
            if (engine.getObjectives().size() >= 1 && !isAttackingObjective && !doHaveAllObjectives) {
                attackObjective();
                isAttackingObjective = true;
            }

            //set a defend order on the two largest allies if they don't already have one
//            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
//                if (assignment.getTarget().getLocation() == allies.get(0).getLocation())
//                    break;
//                applyAssignment(fleetManager.getDeployedFleetMember(allies.get(0)), CombatAssignmentType.DEFEND);
//            }
//            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
//                if (assignment.getTarget().getLocation() == allies.get(1).getLocation())
//                    break;
//                applyAssignment(fleetManager.getDeployedFleetMember(allies.get(1)), CombatAssignmentType.DEFEND);
//            }

            //if an enemy ship is fluxed out, put an eliminate order on it if it doesn't already have one
//            for (ShipAPI enemy : enemies) {
//                if (enemy.getHardFluxLevel() > 0.75) {
//                    for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
//                        if (enemy.getLocation() == assignment.getTarget().getLocation())
//                            break;
//                        applyAssignment(fleetManager.getDeployedFleetMember(enemy), CombatAssignmentType.ENGAGE);
//                    }
//                }
//            }

            //for debug. show all the current assignments
            if (debug) {
//                Console.showMessage("Interval Elapsed");
//                Console.showMessage(allies.toString());
                for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                    //engine.addFloatingText(assignment.getTarget().getLocation(), assignment.getType().name(), 10, Color.LIGHT_GRAY, null, 1, 10);
                    Console.showMessage("Assignment Type: "+ assignment.getType());
                    Console.showMessage("Assignment Location: "+ assignment.getTarget().getLocation());
                    for (BattleObjectiveAPI objective : engine.getObjectives()) {
                        Console.showMessage("Objective Location: "+ objective.getLocation());
                    }
//                    for (ShipAPI ally : allies) {
//                        Console.showMessage("Ally Location: "+ ally.getLocation());
//                    }


//                    String name = "deez";
//                    if (assignment.getTarget() instanceof ShipAPI) {
//                        name = ((ShipAPI) assignment.getTarget()).getName();
//                    } else if (assignment.getTarget() instanceof BattleObjectiveAPI) {
//                        name = ((BattleObjectiveAPI) assignment.getTarget()).getType();
//                    }
//                    Console.showMessage("Assignment: "+ assignment.getType() +" Target: "+ name);
                }
                Console.showMessage("-----------------Break-----------------");
            }
            assignmentsWithTargets.clear();
            allies.clear();
            enemies.clear();
        }
    }

    // Create a class to represent an assignment and its associated target
    static class AssignmentInfoWithTarget {
        private final CombatFleetManagerAPI.AssignmentInfo assignment;
        private final Object object; // You can use 'Object' to represent various types of targets
        public AssignmentInfoWithTarget(CombatFleetManagerAPI.AssignmentInfo assignment, Object object) {
            this.assignment = assignment;
            this.object = object;
        }
        public CombatFleetManagerAPI.AssignmentInfo getAssignment() {
            return assignment;
        }
        public Object getObject() {
            return object;
        }
    }

    static void sortByRecoveryCost(List<ShipAPI> ships) {
        Collections.sort(ships, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI ship1, ShipAPI ship2) {
                float supplies1 = ship1.getHullSpec().getSuppliesToRecover();
                float supplies2 = ship2.getHullSpec().getSuppliesToRecover();
                return Float.compare(supplies2, supplies1);
            }
        });
    }

    static void applyAssignment(AssignmentTargetAPI target, CombatAssignmentType assignment) {
        Global.getCombatEngine().getFleetManager(1).getTaskManager(true).createAssignment(assignment, target, false);
    }

    static void attackObjective() {
        for (BattleObjectiveAPI objective : Global.getCombatEngine().getObjectives()) {
            if (Objects.equals(objective.getType(), BattleObjectives.SENSOR_JAMMER)) {
                applyAssignment(objective, CombatAssignmentType.ASSAULT);
                break;
            } else if (Objects.equals(objective.getType(), BattleObjectives.NAV_BUOY)) {
                applyAssignment(objective, CombatAssignmentType.ASSAULT);
                break;
            } else if (Objects.equals(objective.getType(), BattleObjectives.COMM_RELAY)) {
                applyAssignment(objective, CombatAssignmentType.ASSAULT);
                break;
            }
        }
    }
}
