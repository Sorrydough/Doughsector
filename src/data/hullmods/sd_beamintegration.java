package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.*;

public class sd_beamintegration extends BaseHullMod {
    final Map<HullSize, Integer> BEAM_ITU_PERCENT = new HashMap<>();
    {	//free ITU bonus for beams
        BEAM_ITU_PERCENT.put(HullSize.FIGHTER, 0);
        BEAM_ITU_PERCENT.put(HullSize.FRIGATE, 10);
        BEAM_ITU_PERCENT.put(HullSize.DESTROYER, 20);
        BEAM_ITU_PERCENT.put(HullSize.CRUISER, 40);
        BEAM_ITU_PERCENT.put(HullSize.CAPITAL_SHIP, 60);
    }
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //give free beam ITU, bonus not cumulative with targeting computer modifications
        //this needs to be here instead of applyEffectsBeforeShipCreation to avoid an ordering issue
        float bonusToGive = BEAM_ITU_PERCENT.get(ship.getHullSize()) - Math.max( ship.getMutableStats().getEnergyWeaponRangeBonus().getPercentMod(), 0);
        //also check whether the bonus is positive, we don't want to accidentally subtract bonus instead if the player overcomes our targeting bonus somehow
        if (bonusToGive > 0) {  ship.getMutableStats().getBeamWeaponRangeBonus().modifyPercent(id, bonusToGive); }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Beams recieve a 10/20/40/60%% range bonus by hull size.", 5f, Misc.getHighlightColor(), "10/20/40/60%");
        tooltip.addPara("Only the strongest bonus between this hullmod and all other percentage bonuses combined will apply.", 5f,
                Misc.getDarkHighlightColor(), "Only the strongest bonus between this hullmod and all other percentage bonuses combined will apply.");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
}