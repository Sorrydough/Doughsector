package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_antispall extends BaseHullMod {
    final float SPALL_BONUS = 0.9f;
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFragmentationDamageTakenMult().modifyMult(id, SPALL_BONUS);
        stats.getKineticDamageTakenMult().modifyMult(id, SPALL_BONUS);
        stats.getCrewLossMult().modifyMult(id, SPALL_BONUS);
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Reduces damage from kinetic and fragmentation hits, as well as crew casualties in combat by "+ Math.round((1 - SPALL_BONUS) * 100) +"%%.", 5f,
                Misc.getHighlightColor(), Math.round((1 - SPALL_BONUS) * 100) +"%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}