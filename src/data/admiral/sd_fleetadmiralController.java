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
import java.util.List;

public class sd_fleetadmiralController extends BaseEveryFrameCombatPlugin {
    public final sd_battlestateTracker battleState = new sd_battlestateTracker();
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
}