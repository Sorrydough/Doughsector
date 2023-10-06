package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.HashMap;
import java.util.Map;

public class sd_externalhangar extends BaseHullMod {
    final int NUMBER_BAYS = 1;
    final int CREW_PENALTY = 20;
    final int WING_RANGE_MULT = 0;
    final int BOMBER_PENALTY = 10000;
    final Map<HullSize, Float> REFIT_MOD = new HashMap<>(); {
        REFIT_MOD.put(HullSize.FRIGATE, 5f);
        REFIT_MOD.put(HullSize.DESTROYER, 2f);
        REFIT_MOD.put(HullSize.CRUISER, 1f);
        REFIT_MOD.put(HullSize.CAPITAL_SHIP, 0.75f);
    }
    final Map<HullSize, Integer> HULL_PENALTY = new HashMap<>(); {
        HULL_PENALTY.put(HullSize.FRIGATE, 500);
        HULL_PENALTY.put(HullSize.DESTROYER, 1000);
        HULL_PENALTY.put(HullSize.CRUISER, 2000);
        HULL_PENALTY.put(HullSize.CAPITAL_SHIP, 3000);
    }	//hull reduced by this flat amount, applied after all other bonuses
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinCrewMod().modifyFlat(id, CREW_PENALTY);
        stats.getNumFighterBays().modifyFlat(id, NUMBER_BAYS);
        stats.getFighterWingRange().modifyMult(id, WING_RANGE_MULT);
        stats.getFighterRefitTimeMult().modifyMult(id, REFIT_MOD.get(hullSize));
        stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyFlat(id, BOMBER_PENALTY);
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getMutableStats().getHullBonus().modifyFlat(id, -HULL_PENALTY.get(ship.getHullSize()));
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Installs a hangar on the ship with 20/50/100/125%% rebuild speed by hullsize.", 5,
                Misc.getHighlightColor(), "20/50/100/125%"); // I hate this so much, literally not even worth my mindspace to make it inherit the modifier dynamically
        tooltip.addPara("Fighters are unable to leave the vicinity of their ship.", 5);
        tooltip.addPara("Minimum crew is increased by "+ CREW_PENALTY +".", 2,
                Misc.getHighlightColor(), String.valueOf(CREW_PENALTY));
        tooltip.addPara("Bombers cannot be installed.", 2);
    }
    @Override
    public boolean affectsOPCosts() {
        return true;
    }
    public String getUnapplicableReason(ShipAPI ship) {
        return "Ship already has fighter bays.";
    }
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && ship.getHullSpec().getFighterBays() < 1;
    }
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return ship.getHullSpec().getManufacturer().equals("???");
    }
}
