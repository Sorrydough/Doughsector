package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_capacitorpurge extends BaseHullMod {
    static final int CAPACITOR_CONVERSION = 2;
    final int PPT_PENALTY = 10;
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) { stats.getPeakCRDuration().modifyMult(id, 1 - (float) PPT_PENALTY / 100); }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) { ship.addListener(new CapacitorPurgeMod(ship)); }
    public static class CapacitorPurgeMod implements AdvanceableListener {
        ShipAPI ship;
        public CapacitorPurgeMod(ShipAPI ship) { this.ship = ship; }
        @Override
        public void advance(float amount) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            if (ship.getFluxTracker().isVenting()) {
                stats.getFluxDissipation().modifyFlat(this.toString(), ship.getFluxTracker().getMaxFlux() * CAPACITOR_CONVERSION / 100);
            } else {
                stats.getFluxDissipation().unmodify(this.toString());
            }
        }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("While active venting, "+ CAPACITOR_CONVERSION +"%% of the ship's flux capacity is converted into base dissipation.", 5f, Misc.getHighlightColor(), CAPACITOR_CONVERSION +"%");
        tooltip.addPara("Peak performance time reduced by "+ PPT_PENALTY +"%%.", 5f, Misc.getHighlightColor(), PPT_PENALTY +"%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) { return ship.getHullSpec().getManufacturer().equals("???"); }
}
