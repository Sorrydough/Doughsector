package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_capacitorpurge extends BaseHullMod {
    final float CAPACITOR_CONVERSION = 0.02f;
    final float PPT_PENALTY = 0.9f;
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getPeakCRDuration().modifyMult(id, PPT_PENALTY);
        // doing a fuckton of stupid math to determine what the actual %-wise vent bonus should be
        float capacity = stats.getFluxCapacity().getModifiedValue();
        float bonus = capacity * CAPACITOR_CONVERSION;
        float bonusAsPercent = Math.round((bonus / stats.getFluxDissipation().getModifiedValue()) * 100);
        stats.getVentRateMult().modifyPercent(id, bonusAsPercent);
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("While active venting, "+ Math.round(CAPACITOR_CONVERSION * 100) +"%% of the ship's flux capacity is converted into base dissipation.", 5f, Misc.getHighlightColor(), Math.round(CAPACITOR_CONVERSION * 100) +"%");
        tooltip.addPara("Peak performance time reduced by "+ Math.round(1 - PPT_PENALTY * 100) +"%%.", 5f, Misc.getHighlightColor(), Math.round(1 - PPT_PENALTY * 100) +"%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return ship.getHullSpec().getManufacturer().equals("???");
    }
}
