package data.scripts.admiral;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class sd_fleetAdmiralController extends BaseEveryFrameCombatPlugin {
    private final sd_battleStateTracker battleState = new sd_battleStateTracker();
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



        }
    }
}