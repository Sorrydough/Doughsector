package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_driveinterference extends BaseHullMod {
    final int BURN_PENALTY = 2;
    final int MIN_BURN = 6;
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        float capitalsInFleet = 0;
        if (ship.getFleetMember() != null) {
            for (FleetMemberAPI ally : ship.getFleetMember().getFleetData().getMembersListCopy()) {
                if (ally.getVariant().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)
                    capitalsInFleet += 1;
            }
        }
        float burnLevel = stats.getMaxBurnLevel().getModifiedValue();
        float targetBurnLevel = Math.max(MIN_BURN, burnLevel - ((capitalsInFleet - 1) * BURN_PENALTY)); // subtract 1 to account for itself
        float amountToSubtract = burnLevel - targetBurnLevel;
        if (amountToSubtract > 0)
            stats.getMaxBurnLevel().modifyFlat(id, -amountToSubtract);
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Ship loses "+ BURN_PENALTY +" burn level per additional capital sized ship in its fleet, down to a minimum of "+ MIN_BURN +".", 5,
                Misc.getHighlightColor(), String.valueOf(BURN_PENALTY), String.valueOf(MIN_BURN));
    }
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}