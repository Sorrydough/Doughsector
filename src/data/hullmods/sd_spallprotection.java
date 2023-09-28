package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_spallprotection extends BaseHullMod {
    final int SPALL_BONUS = 10;
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFragmentationDamageTakenMult().modifyMult(id, 1f - SPALL_BONUS * 0.01f);
        stats.getKineticDamageTakenMult().modifyMult(id, 1f - SPALL_BONUS * 0.01f);
        stats.getCrewLossMult().modifyMult(id, 1f - SPALL_BONUS * 0.01f);
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Reduces incoming kinetic damage, fragmentation damage, and crew casualties in combat by "+ SPALL_BONUS +"%%.", 5f, Misc.getHighlightColor(), SPALL_BONUS +"%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) { return ship.getHullSpec().getManufacturer().equals("???"); }
}