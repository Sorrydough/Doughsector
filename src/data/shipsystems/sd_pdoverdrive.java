package data.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class sd_pdoverdrive extends BaseShipSystemScript {
    boolean doOnce = true;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.setJitter(id, new Color(150,100,255, 50), effectLevel, 1, 0, 5);
        ship.setJitterUnder(id, new Color(150,100,255, 150), effectLevel, 10, 0f, 10f);

        if (doOnce) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getType() != WeaponAPI.WeaponType.MISSILE && weapon.getAmmo() < weapon.getMaxAmmo())
                    weapon.setAmmo(weapon.getMaxAmmo());
            }
            doOnce = false;
        }
        stats.getBeamWeaponFluxCostMult().modifyMult(id, 2 * effectLevel);
        stats.getBeamWeaponDamageMult().modifyMult(id, 2 * effectLevel);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, 100 * effectLevel);
        stats.getEnergyAmmoRegenMult().modifyMult(id, 3 * effectLevel);
        stats.getEnergyRoFMult().modifyMult(id, 3 * effectLevel);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        doOnce = true;
        stats.getBeamWeaponFluxCostMult().unmodifyMult(id);
        stats.getBeamWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponRangeBonus().unmodifyPercent(id);
        stats.getEnergyAmmoRegenMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
    }
}