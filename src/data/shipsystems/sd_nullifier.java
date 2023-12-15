package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.sd_util;
import org.lazywizard.lazylib.MathUtils;

public class sd_nullifier extends BaseShipSystemScript {
    boolean doOnce = true;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.setJitter(id, sd_util.timeColor, effectLevel, 1, 0, 5);
        ship.setJitterUnder(id, sd_util.timeUnderColor, effectLevel, 5, 0, 10);

        if (doOnce) { // apply our effect plugin to the target
            Global.getCombatEngine().addPlugin(new sd_nullifierPlugin(ship, ship.getShipTarget()));
            doOnce = false;
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        doOnce = true;
    }
    public static boolean isTargetValid(ShipAPI ship, ShipAPI target) { // checks whether the target is in range, blah blah blah
        if (target == null)												// needs to take target as an input to work in the AI script
            return false;
        float targetDistance = MathUtils.getDistance(ship, target);
        float systemRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship) + ship.getCollisionRadius());
        return !target.isFighter() && target != ship && !(targetDistance > systemRange) && !target.isPhased();
    }
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return isTargetValid(ship, ship.getShipTarget()) && sd_util.canUseSystemThisFrame(ship);
    }
}