package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
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

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        //TODO: Pearlescent shields -> OpenGL bullshit? Fix agility stats. Rename all instances of sd_beamintegration to sd_beamintegration.
        //beam discount
        stats.getDynamic().getMod(Stats.SMALL_BEAM_MOD).modifyFlat(id, -1);
        stats.getDynamic().getMod(Stats.MEDIUM_BEAM_MOD).modifyFlat(id, -2);
        stats.getDynamic().getMod(Stats.LARGE_BEAM_MOD).modifyFlat(id, -4);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //give free beam ITU, bonus not cumulative with targeting computer modifications
        //this needs to be here instead of applyEffectsBeforeShipCreation to avoid an ordering issue
        float bonusToGive = BEAM_ITU_PERCENT.get(ship.getHullSize()) - Math.max( ship.getMutableStats().getEnergyWeaponRangeBonus().getPercentMod(), 0); //ie player uses overclocked targeting unit to get 10%, so we give 30%, so 40% total
        //check whether the bonus is positive, we don't want to accidentally subtract bonus instead if the player gets insane base targeting somehow
        if (bonusToGive > 0) {  ship.getMutableStats().getBeamWeaponRangeBonus().modifyPercent(id, bonusToGive); }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Beams cost 1/2/4 fewer ordnance points by weapon size.", 10f, Misc.getHighlightColor(), "1/2/4");
        tooltip.addPara("Beams recieve a 10/20/40/60%% range bonus by hull size.", 5f, Misc.getHighlightColor(), "10/20/40/60%");
        tooltip.addPara("Only the strongest bonus between this hullmod and all other percentage bonuses combined will apply.", 1f,
                Misc.getDarkHighlightColor(), "Only the strongest bonus between this hullmod and all other percentage bonuses combined will apply.");
    }

    @Override
    public boolean affectsOPCosts() { return true; }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
}