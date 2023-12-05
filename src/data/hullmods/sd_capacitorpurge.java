package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_capacitorpurge extends BaseHullMod {
    final float CAPACITOR_CONVERSION = 0.05f;
    final float PPT_PENALTY = 0.9f;
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getPeakCRDuration().modifyMult(id, PPT_PENALTY);
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        // doing stupid math to determine what the actual %-wise vent bonus should be
        float bonus = stats.getFluxCapacity().getModifiedValue() * CAPACITOR_CONVERSION;
        float bonusAsPercent = Math.round((bonus / stats.getFluxDissipation().getModifiedValue()) * 100);
        stats.getVentRateMult().modifyPercent(id, bonusAsPercent / 2);
        // ^ needs to be halved to account for funky vent rate math doubling stuff such as RFC 25% bonus causing 100 * (2 * 1.25) = 250
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("While active venting, "+ Math.round(CAPACITOR_CONVERSION * 100) +"%% of the ship's flux capacity is converted into dissipation, applied after the vent bonus.", 5f,
                Misc.getHighlightColor(), Math.round(CAPACITOR_CONVERSION * 100) +"%");
        tooltip.addPara("Peak performance time reduced by "+ Math.round((1 - PPT_PENALTY) * 100) +"%%.", 5f,
                Misc.getHighlightColor(), Math.round((1 - PPT_PENALTY) * 100) +"%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}
