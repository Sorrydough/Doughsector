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
//        ship.fadeToColor(id, new Color(75, 75, 75, 255), 0.1f, 0.1f, effectLevel);
//        ship.getEngineController().fadeToOtherColor(id, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), effectLevel, 0.75f * effectLevel);
        ship.setJitterUnder(id, new Color(150,100,255, 150), effectLevel, 15, 0f, 20f);

        if (doOnce) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getType() != WeaponAPI.WeaponType.MISSILE && weapon.getAmmo() < weapon.getMaxAmmo())
                    weapon.setAmmo(weapon.getMaxAmmo());
            }
            doOnce = false;
        }
        stats.getBeamWeaponDamageMult().modifyMult(id, 2);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, 100);
        stats.getEnergyAmmoRegenMult().modifyMult(id, 3);
        stats.getEnergyRoFMult().modifyMult(id, 3);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        doOnce = true;
        stats.getBeamWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponRangeBonus().unmodifyPercent(id);
        stats.getEnergyAmmoRegenMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
    }
}



