package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

public class sd_nullifier extends BaseShipSystemScript {
    public static final Color jitterUnderColor = new Color(150,100,255, 150);
    public static final Color jitterColor = new Color(150,100,255, 50);
    boolean doOnce = true;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.setJitter(id, jitterColor, effectLevel, 1, 0, 5);
        ship.setJitterUnder(id, jitterUnderColor, effectLevel, 5, 0, 10);

        if (doOnce) { // apply our effect plugin to the target
            Global.getCombatEngine().addPlugin(new sd_nullifierPlugin(ship, ship.getShipTarget()));
            doOnce = false;
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        doOnce = true;
    }
}
