package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.ai.AssignmentModulePlugin;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Objectives;
import com.fs.starfarer.campaign.ai.AssignmentModule;
import com.fs.starfarer.combat.CombatFleetManager;
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
            Console.showMessage("Admiral Init");
//            AssignmentModulePlugin


//            for (ShipAPI ship : engine.getShips()) {
//                if (ship.getAIFlags().hasFlag(AIF))
//
//
//
//
//            }
//
//
//
//            for (BattleObjectiveAPI objective : engine.getObjectives()) {
//
//                CombatAssignmentType.DEFEND
//
//
//
//            }
            doInit = false;
        }
    }
}
