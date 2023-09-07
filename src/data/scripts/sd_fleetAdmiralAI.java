package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.console.Console;

public class sd_fleetAdmiralAI implements AdmiralAIPlugin {

    boolean doInit = true;

    @Override
    public void preCombat() {



    }

    @Override
    public void advance(float amount) {

        //CombatEngineAPI engine = Global.getCombatEngine();
        if (doInit) {
            Console.showMessage("ur mum");
//            for (ShipAPI ship : engine.getShips()) {
//
//
//
//
//
//            }
//
//
//
//            for (BattleObjectiveAPI objective : engine.getObjectives()) {
//                if (objective.getType() == )
//                CombatAssignmentType.CAPTURE
//
//
//
//            }
            doInit = false;
        }
    }
}
