package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.sd_util;
import org.lazywizard.lazylib.MathUtils;

public class sd_stasisfield extends BaseShipSystemScript {
    boolean runOnce = true;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.setJitter(id, sd_util.timeColor, effectLevel, 1, 0, 1);
        ship.setJitterUnder(id, sd_util.timeUnderColor, effectLevel, 10, 0, 10);

        if (runOnce) { // apply our effect plugin to the target
            Global.getCombatEngine().addPlugin(new sd_stasisfieldPlugin(ship, ship.getShipTarget()));
            runOnce = false;
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        runOnce = true;
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