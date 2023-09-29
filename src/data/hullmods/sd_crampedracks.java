package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_crampedracks extends BaseHullMod
{
    final float EXTRA_AMMO_MULT = 0.5f;
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getType().equals(WeaponAPI.WeaponType.MISSILE) && !weapon.getSlot().getId().startsWith("MSL")) {
                int defaultAmmo = weapon.getSpec().getMaxAmmo();
                int ammoOver = weapon.getAmmo() - defaultAmmo;
                if (ammoOver > 0) {
                    int newAmmo = (int) Math.ceil(defaultAmmo + ammoOver * EXTRA_AMMO_MULT);
                    weapon.setAmmo(newAmmo);
                    weapon.setMaxAmmo(newAmmo);
                }
            }
        }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Missile ammo bonuses for this ship's mixed hardpoints are reduced by "+ Math.round(EXTRA_AMMO_MULT * 100) +"%%.", 5f, Misc.getHighlightColor(), Math.round(EXTRA_AMMO_MULT * 100) +"%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) { return ship.getHullSpec().getManufacturer().equals("???"); }
}
