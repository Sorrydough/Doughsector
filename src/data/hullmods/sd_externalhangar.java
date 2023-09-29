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
    final Map<HullSize, Integer> REFIT_MOD = new HashMap<>();
    {	//fighter refit time modifier
        REFIT_MOD.put(HullSize.FRIGATE, 5);
        REFIT_MOD.put(HullSize.DESTROYER, 50);
        REFIT_MOD.put(HullSize.CRUISER, 100);
        REFIT_MOD.put(HullSize.CAPITAL_SHIP, 150);
    }
    final Map<HullSize, Integer> HULL_PENALTY = new HashMap<>();
    {	//hull reduced by this flat amount, applied after all other bonuses
        HULL_PENALTY.put(HullSize.FRIGATE, 500);
        HULL_PENALTY.put(HullSize.DESTROYER, 1000);
        HULL_PENALTY.put(HullSize.CRUISER, 2000);
        HULL_PENALTY.put(HullSize.CAPITAL_SHIP, 3000);
    }
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinCrewMod().modifyFlat(id, CREW_PENALTY);
        stats.getNumFighterBays().modifyFlat(id, NUMBER_BAYS);
        stats.getFighterWingRange().modifyMult(id, WING_RANGE_MULT);
        stats.getFighterRefitTimeMult().modifyMult(id, (float) REFIT_MOD.get(hullSize) / 100); //TODO: FIX THIS BEING RETARDED
        stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyFlat(id, BOMBER_PENALTY);
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getMutableStats().getHullBonus().modifyFlat(id, -HULL_PENALTY.get(ship.getHullSize()));
//        for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) { //TODO: transform spec into support fighter, https://discord.com/channels/187635036525166592/824910699415207937/1156506838897004585
//            FighterWingAPI wing = bay.getWing();
//            if (wing == null)
//                continue;
//            wing.getSpec().setRole(WingRole.SUPPORT);
//        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Installs a hangar on the ship with 5/50/100/150%% refit speed by hullsize.", 5, Misc.getHighlightColor(), "5/50/100/150%");
        tooltip.addPara("Fighters are unable to leave the vicinity of their ship.", 5);
        tooltip.addPara("Minimum crew is increased by "+ CREW_PENALTY +".", 2, Misc.getHighlightColor(), CREW_PENALTY + "");
        tooltip.addPara("Bombers cannot be installed.", 2);
    }

    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) { return ship.getHullSpec().getManufacturer().equals("???"); }
    public boolean isApplicableToShip(ShipAPI ship) { return ship != null && ship.getHullSpec().getFighterBays() < 1; }
    public String getUnapplicableReason(ShipAPI ship) { return "Ship already has fighter bays."; }
}
