package data.admiral;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.admiral.modules.sd_attackManager;
import data.admiral.modules.sd_formationManager;
import data.admiral.modules.sd_objectiveManager;
import org.lazywizard.console.Console;

import java.awt.*;

public class sd_fleetadmiralController implements AdmiralAIPlugin {
    final int allySide;
    final int enemySide;
    public sd_fleetadmiralController(int forSide) {
        this.allySide = forSide;
        this.enemySide = calcOtherSide(forSide);
    }
    private int calcOtherSide(int forSide) {
        if (forSide == 1)
            return 0;
        if (forSide == 0)
            return 1;
        return -1;
    }
    public final sd_fleetadmiralUtil.battlestateTracker battleState = new sd_fleetadmiralUtil.battlestateTracker();
    private final IntervalUtil interval = new IntervalUtil(0.5f, 2); // variable to approximate human reaction time, they don't stare at the tactical map 24/7. I hope.
    private boolean doInit = true;
    private boolean debug = true;
    @Override
    public void preCombat() {

    }
    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || Global.getCurrentState() != GameState.COMBAT)
            return;

        if (doInit) {
            battleState.updateState(engine, allySide, enemySide); // player side is 0, AI side is 1
            if (debug)
                Console.showMessage("Admiral Controller mounted for fleet side " + battleState.allySide);
            for (CombatFleetManagerAPI.AssignmentInfo assignment : battleState.allyTaskManager.getAllAssignments())
                battleState.allyTaskManager.removeAssignment(assignment); // need to wipe all assignments that might've been created by alex or the player before the controller was mounted

            doInit = false;
        }

//        if (battleState.deployedAllyShips.get(0) != null && battleState.deployedAllyShips.get(0).getFluxLevel() < 0.5)
//            battleState.deployedAllyShips.get(0).getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF);

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            battleState.updateState(engine, allySide, enemySide);
            // ^ updating battle state is expensive so we don't do it every frame

//            for (FleetMemberAPI fleetmember : battleState.allyFleetManager.getReservesCopy()) { // todo: reverse engineer fleet deployment manager
//                if (fleetmember.canBeDeployedForCombat()) {
//                    battleState.allyFleetManager
//
//                }
//            }

            //Console.showMessage("Allied DP: "+ battleState.deployedAllyDP +" Enemy DP: "+ battleState.deployedEnemyDP);

            if (!battleState.deployedAllyShips.isEmpty()) {
                sd_formationManager.manageFormation(battleState);
                sd_objectiveManager.manageAttackedObjectives(battleState);
                //sd_attackManager.manageAttackedEnemies(battleState);
            }

            if (debug)
                for (CombatFleetManagerAPI.AssignmentInfo assignment : battleState.allyTaskManager.getAllAssignments())
                    engine.addFloatingText(assignment.getTarget().getLocation(), assignment.getType().name(), 100, Color.LIGHT_GRAY, null, 0, 0);
        }
    }
}
