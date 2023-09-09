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
import org.codehaus.janino.Java;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class sd_fleetAdmiralAI implements AdmiralAIPlugin {
    boolean debug = true;
    boolean doInit = true;
    CombatEngineAPI engine;
    CombatFleetManagerAPI fleetManager;
    CombatTaskManagerAPI taskManager;
    IntervalUtil interval = new IntervalUtil(2, 2);

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
            //also as part of init, we need to pick an objective and put an assault order on it
            if (engine.getObjectives().size() >= 1) { //TODO: ADJUST THIS LATER TO PRIORITIZE BY BATTLE SIZE
                for (BattleObjectiveAPI objective : engine.getObjectives()) {
                    if (Objects.equals(objective.getType(), BattleObjectives.SENSOR_JAMMER)) {
                        applyAssignment(objective.getLocation(), CombatAssignmentType.ASSAULT);
                        break;
                    } else if (Objects.equals(objective.getType(), BattleObjectives.NAV_BUOY)) {
                        applyAssignment(objective.getLocation(), CombatAssignmentType.ASSAULT);
                        break;
                    } else if (Objects.equals(objective.getType(), BattleObjectives.COMM_RELAY)) {
                        applyAssignment(objective.getLocation(), CombatAssignmentType.ASSAULT);
                        break;
                    }
                }
            }
            doInit = false;
        }

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            boolean isAttackingObjective = false;
            //if there's an assignment on something that shouldn't have it, then remove it
            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                //ensure we never have assignments on dead stuff, fighters, etc
                if (assignment.getTarget() instanceof ShipAPI) {
                    ShipAPI target = (ShipAPI) assignment.getTarget();
                    if (target.isHulk() || !target.isAlive() || target.isFighter())  {
                        taskManager.removeAssignment(assignment);
                    }
                    //if an enemy ship had an engage order applied due to being fluxed out, check if it's time to rescind the order
                    if (assignment.getType() == CombatAssignmentType.ENGAGE && target.getHardFluxLevel() < 0.75) {
                        taskManager.removeAssignment(assignment);
                    }
                }
                //once we own an objective we can clear its associated order, this is done to keep the fleet from being strung out across multiple objectives
                //this is also beneficial because if we own all the objectives our fleet will automatically free roam
                if (assignment.getTarget() instanceof BattleObjectiveAPI && assignment.getType() == CombatAssignmentType.DEFEND) {
                    taskManager.removeAssignment(assignment);
                }
                //if we're attacking an objective set the flag, so we know not to set an objective to attack later
                if (assignment.getTarget() instanceof BattleObjectiveAPI && assignment.getType() == CombatAssignmentType.DEFEND) {
                    isAttackingObjective = true;
                }
            }

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

            //for debug, show all the current assignments
            if (debug) {
//                Console.showMessage("Interval Elapsed");
//                Console.showMessage(allies.toString());
                for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                    //engine.addFloatingText(assignment.getTarget().getLocation(), assignment.getType().name(), 10, Color.LIGHT_GRAY, null, 1, 10);
                    String name = "deez";
                    if (assignment.getTarget() instanceof ShipAPI) {
                        name = ((ShipAPI) assignment.getTarget()).getName();
                    } else if (assignment.getTarget() instanceof BattleObjectiveAPI) {
                        name = ((BattleObjectiveAPI) assignment.getTarget()).getType();
                    }
                    Console.showMessage(assignment.getTarget());
                    Console.showMessage("Assignment: "+ assignment.getType() +" Target: "+ name);
                }
                Console.showMessage("-----------------Break-----------------");
            }

            //set a defend order on the two largest allies if they don't already have one
            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                if (assignment.getTarget().getLocation() == allies.get(0).getLocation())
                    break;
                applyAssignment(allies.get(0).getLocation(), CombatAssignmentType.DEFEND);
            }
            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                if (assignment.getTarget().getLocation() == allies.get(1).getLocation())
                    break;
                applyAssignment(allies.get(1).getLocation(), CombatAssignmentType.DEFEND);
            }

            //if an enemy ship is fluxed out, put an eliminate order on it if it doesn't already have one
            for (ShipAPI enemy : enemies) {
                if (enemy.getHardFluxLevel() > 0.75) {
                    for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                        if (enemy.getLocation() == assignment.getTarget().getLocation())
                            break;
                        applyAssignment(enemy.getLocation(), CombatAssignmentType.ENGAGE);
                    }
                }
            }

            //if there's no assault orders on any objectives then apply one. Very important, only have one active at a time.
            if (!isAttackingObjective) {
                if (engine.getObjectives().size() >= 1) { //TODO: ADJUST THIS LATER TO PRIORITIZE BY BATTLE SIZE
                    for (BattleObjectiveAPI objective : engine.getObjectives()) { //TODO: TURN THIS INTO A STATIC VOID CUZ I'M USING IT TWICE
                        if (Objects.equals(objective.getType(), BattleObjectives.SENSOR_JAMMER)) {
                            applyAssignment(objective.getLocation(), CombatAssignmentType.ASSAULT);
                            break;
                        } else if (Objects.equals(objective.getType(), BattleObjectives.NAV_BUOY)) {
                            applyAssignment(objective.getLocation(), CombatAssignmentType.ASSAULT);
                            break;
                        } else if (Objects.equals(objective.getType(), BattleObjectives.COMM_RELAY)) {
                            applyAssignment(objective.getLocation(), CombatAssignmentType.ASSAULT);
                            break;
                        }
                    }
                }
            }
            allies.clear();
            enemies.clear();
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

    static void applyAssignment(Vector2f target, CombatAssignmentType assignment) {
        AssignmentTargetAPI objectiveTarget = Global.getCombatEngine().getFleetManager(1).createWaypoint(target, true);
        Global.getCombatEngine().getFleetManager(1).getTaskManager(true).createAssignment(assignment, objectiveTarget, false);
    }
}
