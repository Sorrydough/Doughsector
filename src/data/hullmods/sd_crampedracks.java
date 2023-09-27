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
        tooltip.addPara("This ship's mixed hardpoints are strapped for space, making them unable to recieve ammo capacity bonuses.", 5f, Misc.getNegativeHighlightColor(), "unable to recieve ammo capacity");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
}
