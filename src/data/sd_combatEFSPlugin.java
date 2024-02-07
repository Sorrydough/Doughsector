package data;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.admiral.sd_fleetadmiralController;
import lunalib.lunaSettings.LunaSettings;
import org.lazywizard.console.Console;

import java.util.List;

public class sd_combatEFSPlugin extends BaseEveryFrameCombatPlugin {
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || Global.getCurrentState() != GameState.COMBAT)
            return;

        boolean enabledNPC = false;
        if (Global.getSettings().getModManager().isModEnabled("lunalib"))
            enabledNPC = Boolean.parseBoolean(LunaSettings.getString("sd_doughsector", "sd_enableAIAdmiral"));
        if (enabledNPC && !(engine.getFleetManager(1).getAdmiralAI() instanceof sd_fleetadmiralController) && engine.getFleetManager(1).getReservesCopy().isEmpty())
            engine.getFleetManager(1).setAdmiralAI(new sd_fleetadmiralController(1));  // todo: reverse engineer the deployment manager

        boolean enabledPlayer = false;
        if (Global.getSettings().getModManager().isModEnabled("lunalib"))
            enabledPlayer = Boolean.parseBoolean(LunaSettings.getString("sd_doughsector", "sd_enablePlayerAdmiral"));
        if (enabledPlayer && !(engine.getFleetManager(0).getAdmiralAI() instanceof sd_fleetadmiralController))
            engine.getFleetManager(0).setAdmiralAI(new sd_fleetadmiralController(0));
    }
}



