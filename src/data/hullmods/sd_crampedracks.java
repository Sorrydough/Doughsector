package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_crampedracks extends BaseHullMod
{
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getType().equals(WeaponAPI.WeaponType.MISSILE) && !weapon.getSlot().getId().startsWith("MSL")) {
                int maxAmmo = weapon.getSpec().getMaxAmmo();
                weapon.setAmmo(maxAmmo);
                weapon.setMaxAmmo(maxAmmo);
            }
        }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("This ship's mixed hardpoints are cramped, making them unable to recieve ammo capacity bonuses.", 5f);
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) { return ship.getHullSpec().getManufacturer().equals("???"); }
}
