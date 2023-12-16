package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_wetmounts extends BaseHullMod {
    final float RANGE_BONUS = 100;
    final float RECOIL_MULT = 0.5f;
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // copied effects from armored mounts
        stats.getMaxRecoilMult().modifyMult(id, RECOIL_MULT);
        stats.getRecoilPerShotMult().modifyMult(id, RECOIL_MULT);
        stats.getRecoilDecayMult().modifyMult(id, RECOIL_MULT);
    }
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyFlat(id, RANGE_BONUS);
        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyFlat(id, RANGE_BONUS);
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Recoil is reduced by "+ Math.round(RECOIL_MULT * 100) +"%% and weapon range is extended by a flat "+ Math.round(RANGE_BONUS) +" units." , 5f,
                Misc.getHighlightColor(), Math.round(RECOIL_MULT * 100) +"%", Math.round(RANGE_BONUS) + "");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}