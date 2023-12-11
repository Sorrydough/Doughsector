package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.sd_util;

import java.util.HashMap;

public class sd_capacitorpurge extends BaseHullMod {
    static final float CAPACITOR_CONVERSION = 0.05f;
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
        protected HashMap<ShipEngineAPI, Boolean> engines = new HashMap<>();
        protected HashMap<WeaponAPI, Boolean> weapons = new HashMap<>();
        boolean doOnce = true;
        boolean vented = false;
        float duration = 0;
        @Override
        public void advance(float amount) {
            // create a list of all weapon and engine slots as well as whether they've been disabled for this repair cycle
            // if a slot wasn't disabled before and it is now, flag it as disabled and play our effect
            // if a slot isn't disabled and it was flagged as disabled, unflag it as disabled
            if (doOnce) { // this tracks whether a mote has been emitted by this module yet, it does NOT track whether the module is disabled (although these two things are related)
                for (ShipEngineAPI vroom : ship.getEngineController().getShipEngines())
                    engines.put(vroom, false);
                for (WeaponAPI weapon : ship.getAllWeapons())
                    weapons.put(weapon, false);
                doOnce = false;
            }
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                if (!ship.getFluxTracker().isVenting()) {
                    duration = 0;
                    stats.getWeaponMalfunctionChance().unmodifyFlat(id);
                    stats.getEngineMalfunctionChance().unmodifyFlat(id);
                    float bonus = stats.getFluxCapacity().getModifiedValue() * CAPACITOR_CONVERSION;
                    float bonusAsPercent = Math.round((bonus / stats.getFluxDissipation().getModifiedValue()) * 100);
                    stats.getVentRateMult().modifyPercent(id, bonusAsPercent / 2);
                    if (vented) {
                        doOnce = true;
                        vented = false;
                    }
                } else {
                    duration += 0.1;
                    stats.getWeaponMalfunctionChance().modifyFlat(id, MALFUNCTION_CHANCE * duration);
                    stats.getEngineMalfunctionChance().modifyFlat(id, MALFUNCTION_CHANCE * duration);
                    for (ShipEngineAPI vroom : ship.getEngineController().getShipEngines()) {
                        if (vroom.isDisabled() && !engines.get(vroom)) { // aka if the engine is disabled and it wasn't disabled last time we checked
                            sd_util.emitMote(ship, vroom);
                            engines.put(vroom, true);
                        } else if (!vroom.isDisabled() && engines.get(vroom)) // if the engine isn't disabled and it was disabled last time we checked, update its state
                            engines.put(vroom, false);
                    }
                    for (WeaponAPI weapon : ship.getAllWeapons()) {
                        if (weapon.isDisabled() && !weapons.get(weapon)) {
                            sd_util.emitMote(ship, weapon);
                            weapons.put(weapon, true);
                        } else if (!weapon.isDisabled() && weapons.get(weapon))
                            weapons.put(weapon, false);
                    }
                    vented = true;
                }
            }
        }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("While active venting, "+ Math.round(CAPACITOR_CONVERSION * 100) +"%% of the ship's flux capacity is converted into dissipation, applied after the vent bonus.", 5f,
                Misc.getHighlightColor(), Math.round(CAPACITOR_CONVERSION * 100) +"%");
        tooltip.addPara("Malfunctions may occur while venting and have a unique side effect. Longer vents experience more severe malfunctions.", 5f);
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}
