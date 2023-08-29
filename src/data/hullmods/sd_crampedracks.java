package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_crampedracks extends BaseHullMod
{
    public static final float PENALTY = 0.67f;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getType().equals(WeaponAPI.WeaponType.MISSILE) && !weapon.getSlot().getId().startsWith("MSL")) {
                weapon.setAmmo((int) Math.ceil(weapon.getAmmo() * PENALTY));
                weapon.setMaxAmmo((int) Math.ceil(weapon.getMaxAmmo() * PENALTY));
            }
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("This ship's mixed hardpoints are strapped for space, reducing their missile ammunition capacity by 33%% rounded up.", 5f, Misc.getNegativeHighlightColor(), "33%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}
