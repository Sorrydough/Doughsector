package data.scripts.admiral;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.admiral.modules.sd_attackManager;
import data.scripts.admiral.modules.sd_formationManager;
import data.scripts.admiral.modules.sd_objectiveManager;
import org.lazywizard.console.Console;

import java.awt.*;
import java.util.List;

public class sd_fleetAdmiralController extends BaseEveryFrameCombatPlugin {
    public final sd_battleStateTracker battleState = new sd_battleStateTracker();
    private final IntervalUtil interval = new IntervalUtil(0.5f, 2); // variable to approximate human reaction time
    private boolean doInit = true;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || Global.getCurrentState() != GameState.COMBAT)
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

            if (battleState.deployedAllyShips.size() != 0) {
                sd_formationManager.manageFormation(battleState);
                sd_objectiveManager.manageAttackedObjectives(battleState);
                sd_attackManager.manageAttackedEnemies(battleState);
            }

            for (CombatFleetManagerAPI.AssignmentInfo assignment : battleState.allyTaskManager.getAllAssignments())
                engine.addFloatingText(assignment.getTarget().getLocation(), assignment.getType().name(), 100, Color.LIGHT_GRAY, null, 1, 10);
        }
    }
}