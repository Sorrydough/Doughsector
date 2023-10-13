package data.scripts.admiral;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.BattleObjectives;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.Console;

import java.awt.*;
import java.util.*;
import java.util.List;

public class sd_fleetAdmiralV1 extends BaseEveryFrameCombatPlugin {
    boolean debug = true;
    boolean doInit = true;
    boolean isAttackingObjective = false;
    boolean doHaveAllObjectives = false;
    CombatEngineAPI engine;
    CombatFleetManagerAPI fleetManager;
    CombatTaskManagerAPI taskManager;
    IntervalUtil interval = new IntervalUtil(1, 3); //runs every 1 to 3 seconds to approximate a human's variable reaction time
    int owner = 0;
    float numObjectives = 0;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (doInit) {
            if (debug)
                Console.showMessage("Player Admiral Init");
            engine = Global.getCombatEngine();
            fleetManager = engine.getFleetManager(owner);
            taskManager = fleetManager.getTaskManager(false);
            //enemyTaskManager = fleetManager.getTaskManager(false);
            //as part of init, we need to wipe all assignments that might've been created by alex's admiral
            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                taskManager.removeAssignment(assignment);
            }
            numObjectives = engine.getObjectives().size();
            doInit = false;
        }

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            //make a list of all allies and all enemies
            List<ShipAPI> allies = new ArrayList<>();
            List<ShipAPI> enemies = new ArrayList<>();
            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                if (ship.getOwner() == owner && !ship.isHulk() && !ship.isShuttlePod() && !ship.isFighter()) {
                    allies.add(ship);
                } else if (ship.getOwner() != owner && !ship.isHulk() && !ship.isShuttlePod() && !ship.isFighter()) {
                    enemies.add(ship);
                }
            }

            //sort the lists based on ship size (aka recovery cost)
            sd_fleetAdmiralUtil.sortByRecoveryCost(allies);
            sd_fleetAdmiralUtil.sortByRecoveryCost(enemies);

            //make a list of all assignments and what they're attached to
            List<sd_fleetAdmiralUtil.AssignmentInfoWithTarget> assignmentsWithTargets = new ArrayList<>();
            for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                    if (assignment.getTarget().getLocation() == ship.getLocation()) {
                        //add the 'ShipAPI ship' to a new list of ships that have an assignment, alongside its associated 'AssignmentInfo assignment'
                        assignmentsWithTargets.add(new sd_fleetAdmiralUtil.AssignmentInfoWithTarget(assignment, ship));
                    }
                }
                for (BattleObjectiveAPI objective : engine.getObjectives()) {
                    if (assignment.getTarget().getLocation() == objective.getLocation()) {
                        //same again, the goal is to keep track of what assignments are attached to what objects
                        assignmentsWithTargets.add(new sd_fleetAdmiralUtil.AssignmentInfoWithTarget(assignment, objective));
                    }
                }
            }

            //assign around and find out
            isAttackingObjective = false;
            for (sd_fleetAdmiralUtil.AssignmentInfoWithTarget assignment : assignmentsWithTargets) {
                if (assignment.getObject() instanceof ShipAPI) {
                    ShipAPI target = (ShipAPI) assignment.getObject();
                    //ensure we never have assignments on dead stuff, fighters, etc
                    if (target.isHulk() || !target.isAlive() || target.isFighter())  {
                        taskManager.removeAssignment(assignment.getAssignment());
                    }
                    //if an enemy ship had an engage order applied due to being fluxed out, check if it's time to rescind the order
                    if (assignment.getAssignment().getType() == CombatAssignmentType.INTERCEPT && (target.getFluxLevel() < 0.75 || target.getHardFluxLevel() < 0.65) && !target.getFluxTracker().isOverloaded() && !target.getEngineController().isFlamedOut()) {
                        taskManager.removeAssignment(assignment.getAssignment());
                    }
                }
                if (assignment.getObject() instanceof BattleObjectiveAPI) {
                    //once we own an objective we can clear its associated order, this is done to keep the fleet from being strung out across multiple objectives
                    //this is also beneficial because if we own all the objectives our fleet will automatically free roam
                    if (assignment.getAssignment().getType() == CombatAssignmentType.DEFEND) {
                        taskManager.removeAssignment(assignment.getAssignment());
                    }
                    //if we're attacking an objective set the flag, so we know not to attack a new objective later
                    if (assignment.getAssignment().getType() == CombatAssignmentType.ASSAULT) {
                        isAttackingObjective = true;
                    }
                }
            }

            //keep track of whether we own all the objectives
            for (BattleObjectiveAPI objective : engine.getObjectives()) {
                if (objective.getOwner() != owner) {
                    doHaveAllObjectives = false;
                    break;
                }
                doHaveAllObjectives = true;
            }

            //functions that we only do in battles with objectives (aka a fleet engagement, instead of a skirmish)
            if (numObjectives != 0) {
                //check whether the largest ally has a defend order
                boolean largestally1 = false;
                for (sd_fleetAdmiralUtil.AssignmentInfoWithTarget assignment : assignmentsWithTargets) {
                    if (assignment.getObject() instanceof ShipAPI) {
                        ShipAPI ship = (ShipAPI) assignment.getObject();
                        if (ship == allies.get(0)) {
                            largestally1 = true;
                        }
                    }
                }
                //if it doesn't, apply a defend order to it
                if (!largestally1) {
                    sd_fleetAdmiralUtil.applyAssignment(fleetManager.getDeployedFleetMember(allies.get(0)), CombatAssignmentType.DEFEND, owner);
                }

                //if we're not attacking an objective & we don't own all objectives then attack one
                if (!isAttackingObjective && !doHaveAllObjectives) {
                    attackObjective(owner);
                }
            }

            //if an enemy ship is fluxed out, put an engage order on it if it doesn't already have one
            for (ShipAPI enemy : enemies) {
                if (enemy.getFluxLevel() > 0.75 || enemy.getHardFluxLevel() > 0.65 || enemy.getFluxTracker().isOverloaded() || enemy.getEngineController().isFlamedOut()) {
                    boolean isEnemyEngaged = false;
                    for (sd_fleetAdmiralUtil.AssignmentInfoWithTarget assignment : assignmentsWithTargets) {
                        if (enemy == assignment.getObject()) {
                            isEnemyEngaged = true;
                            break;
                        }
                    }
                    if (!isEnemyEngaged) {
                        sd_fleetAdmiralUtil.applyAssignment(engine.getFleetManager(owner).getDeployedFleetMember(enemy), CombatAssignmentType.INTERCEPT, owner);
                    }
                }
            }

            //for debug. float text above all current assignments
            if (debug)
                for (CombatFleetManagerAPI.AssignmentInfo assignment : taskManager.getAllAssignments()) {
                    engine.addFloatingText(assignment.getTarget().getLocation(), assignment.getType().name(), 100, Color.LIGHT_GRAY, null, 1, 10);
                }
            assignmentsWithTargets.clear();
            allies.clear();
            enemies.clear();
        }
    }

    static void attackObjective(int owner) {
        for (BattleObjectiveAPI objective : Global.getCombatEngine().getObjectives()) {
            if (Objects.equals(objective.getType(), BattleObjectives.SENSOR_JAMMER) && objective.getOwner() != owner) {
                sd_fleetAdmiralUtil.applyAssignment(objective, CombatAssignmentType.ASSAULT, owner);
                break;
            } else if (Objects.equals(objective.getType(), BattleObjectives.NAV_BUOY) && objective.getOwner() != owner) {
                sd_fleetAdmiralUtil.applyAssignment(objective, CombatAssignmentType.ASSAULT, owner);
                break;
            } else if (Objects.equals(objective.getType(), BattleObjectives.COMM_RELAY) && objective.getOwner() != owner) {
                sd_fleetAdmiralUtil.applyAssignment(objective, CombatAssignmentType.ASSAULT, owner);
                break;
            }
        }
    }
}
