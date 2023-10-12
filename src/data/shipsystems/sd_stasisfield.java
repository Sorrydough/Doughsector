package data.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.combat.AIUtils;

public class sd_stasisfield extends BaseShipSystemScript {
    public static final Color jitterUnderColor = new Color(150,100,255, 150);
    public static final Color jitterColor = new Color(150,100,255, 50);
    boolean doOnce = true;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.setJitterUnder(id, jitterUnderColor, effectLevel, 10, 0, 10);
        ship.setJitter(id, jitterColor, effectLevel, 1, 0, 1);

        if (doOnce) { // apply our effect plugin to the target
            Global.getCombatEngine().addPlugin(new sd_stasisfieldPlugin(ship, ship.getShipTarget()));
            doOnce = false;
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        doOnce = true;
    }
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return ship.getShipTarget() != null && ship.getShipTarget().getHullSize() != ShipAPI.HullSize.FIGHTER;
    }
}