package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_mixedintegration extends BaseHullMod {
    static final int bonus = 100;
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new MixedIntegrationRangeMod());
    }
    public static class MixedIntegrationRangeMod implements WeaponRangeModifier {
        public MixedIntegrationRangeMod() {}
        @Override
        public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }
        @Override
        public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1;
        }
        @Override
        public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.getType() == WeaponType.ENERGY && isSlotMixedEnergy(weapon.getSlot()))
                return bonus;
            return 0;
        }
    }
    public String getUnapplicableReason(ShipAPI ship) {
        boolean applicable = false;
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
            if (isSlotMixedEnergy(slot)) {
                applicable = true;
                break;
            }
        }
        if (!applicable)
            return "Ship must have a synergy or universal slot.";
        return null;
    }
    static boolean isSlotMixedEnergy(WeaponSlotAPI slot) {
        return slot.getWeaponType() == WeaponType.SYNERGY || slot.getWeaponType() == WeaponType.UNIVERSAL;
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("The range of energy weapons in synergy or universal slots is extended by "+ bonus +" units.", 5f,
                Misc.getHighlightColor(), String.valueOf(bonus));
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}
