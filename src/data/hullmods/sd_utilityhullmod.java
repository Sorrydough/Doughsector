package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.graphics.sd_decoSystemRangePlugin;

import java.util.Arrays;
import java.util.List;

public class sd_utilityhullmod extends BaseHullMod {
    List<String> decoSystemRange = Arrays.asList("sd_motearmor", "sd_hackingsuite", "sd_nullifier", "sd_stasisfield");
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        Global.getCombatEngine().addPlugin(new sd_utilityhullmodPlugin(ship));
        if (decoSystemRange.contains(ship.getSystem().getId()))
            Global.getCombatEngine().addPlugin(new sd_decoSystemRangePlugin(ship));
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Utility hullmod to make ships look and behave correctly. You shouldn't be able to see this.", 5f,
                Misc.getHighlightColor(), "You shouldn't be able to see this.");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}
