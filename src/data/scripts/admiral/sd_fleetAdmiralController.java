package data.scripts.admiral;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.admiral.modules.sd_attackManager;
import data.scripts.admiral.modules.sd_objectiveManager;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class sd_fleetAdmiralController extends BaseEveryFrameCombatPlugin {
    public final sd_battleStateTracker battleState = new sd_battleStateTracker();
    private final IntervalUtil interval = new IntervalUtil(0.5f, 2); // variable to approximate human reaction time
    private boolean doInit = true;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null)
            return;
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            battleState.updateState(engine, 0, 1); // player side is 0, AI side is 1
            // ^ updating battle state is expensive so we don't do it every frame
            if (doInit) {
                for (CombatFleetManagerAPI.AssignmentInfo assignment : battleState.allyTaskManager.getAllAssignments())
                    battleState.allyTaskManager.removeAssignment(assignment); // need to wipe all assignments that might've been created by alex or the player before the controller was mounted
                doInit = false;
            }

            for (CombatFleetManagerAPI.AssignmentInfo assignment : battleState.allyTaskManager.getAllAssignments())
                engine.addFloatingText(assignment.getTarget().getLocation(), assignment.getType().name(), 100, Color.LIGHT_GRAY, null, 1, 10);

            sd_attackManager.manageAttackedEnemies(battleState);
            sd_objectiveManager.manageAttackedObjectives(battleState);



        }
    }
}