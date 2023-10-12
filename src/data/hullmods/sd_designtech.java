package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.*;

public class sd_designtech extends BaseHullMod {
    final Map<HullSize, Integer> BEAM_ITU_PERCENT = new HashMap<>(); { // free ITU bonus for beams
        BEAM_ITU_PERCENT.put(HullSize.FIGHTER, 5);
        BEAM_ITU_PERCENT.put(HullSize.FRIGATE, 10);
        BEAM_ITU_PERCENT.put(HullSize.DESTROYER, 20);
        BEAM_ITU_PERCENT.put(HullSize.CRUISER, 40);
        BEAM_ITU_PERCENT.put(HullSize.CAPITAL_SHIP, 60);
    }
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //bonus not cumulative with targeting computer modifications
        //this needs to be here instead of applyEffectsBeforeShipCreation to avoid an ordering issue
        float extra = 0;
        //need an exception for gunnery implants
        if (ship.getCaptain() != null && ship.getCaptain().getStats().hasSkill("gunnery_implants"))
            extra += 15;
        float bonusToGive = (BEAM_ITU_PERCENT.get(ship.getHullSize()) - Math.max(ship.getMutableStats().getEnergyWeaponRangeBonus().getPercentMod() - extra, -extra)) + extra; //fuck I hate math
        //make sure to check whether the bonus is positive, we don't want to accidentally subtract if the player overcomes our targeting bonus
        if (bonusToGive > 0)
            ship.getMutableStats().getBeamWeaponRangeBonus().modifyPercent(id, bonusToGive);
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Beams recieve a 10/20/40/60%% range bonus by hull size.", 5f,
                Misc.getHighlightColor(), "10/20/40/60%"); //hardcode deez nuts
        tooltip.addPara("Only the strongest bonus between this hullmod and all other percentage hullmod bonuses combined applies.", 5f,
                Misc.getDarkHighlightColor(), "Only the strongest bonus between this hullmod and all other percentage hullmod bonuses combined applies.");
        tooltip.addSectionHeading("Bonus is lost if Safety Overrides is installed.", Alignment.MID, 10f);
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}
