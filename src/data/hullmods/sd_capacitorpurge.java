package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class sd_capacitorpurge extends BaseHullMod {
    static final float CAPACITOR_CONVERSION = 0.1f;
    static final float MALFUNCTION_CHANCE = 0.05f;
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new sd_capacitorpurge.sd_capacitorpurgeListener(ship));
    }
    public static class sd_capacitorpurgeListener implements AdvanceableListener {
        protected ShipAPI ship;
        protected MutableShipStatsAPI stats;
        public sd_capacitorpurgeListener(ShipAPI ship) {
            this.ship = ship;
            this.stats = ship.getMutableStats();
        }
        protected String id = "sd_capacitorpurge";
        protected IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);
        float duration = 0;
        @Override
        public void advance(float amount) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                if (!ship.getFluxTracker().isVenting()) {
                    duration = 0;
                    stats.getWeaponMalfunctionChance().unmodifyFlat(id);
                    stats.getEngineMalfunctionChance().unmodifyFlat(id);
                    float bonus = stats.getFluxCapacity().getModifiedValue() * CAPACITOR_CONVERSION;
                    float bonusAsPercent = Math.round((bonus / stats.getFluxDissipation().getModifiedValue()) * 100);
                    stats.getVentRateMult().modifyPercent(id, bonusAsPercent / 2);
                } else {
                    duration += 0.1;
                    stats.getWeaponMalfunctionChance().modifyFlat(id, MALFUNCTION_CHANCE * duration);
                    stats.getEngineMalfunctionChance().modifyFlat(id, MALFUNCTION_CHANCE * duration);
                }
            }
        }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("While active venting, "+ Math.round(CAPACITOR_CONVERSION * 100) +"%% of the ship's flux capacity is converted into dissipation, applied after the vent bonus.", 5f,
                Misc.getHighlightColor(), Math.round(CAPACITOR_CONVERSION * 100) +"%");
        tooltip.addPara("Malfunctions may occur while venting. Longer vents experience more severe malfunctions.", 5f);
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}
