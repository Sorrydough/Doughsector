package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.sd_util;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class sd_missileboosters extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new sd_missileboostersPlugin(ship));
    }
    static class sd_missileboostersPlugin extends BaseEveryFrameCombatPlugin {
        final ShipAPI ship;
        final IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);
        public sd_missileboostersPlugin(ShipAPI ship) {
            this.ship = ship;
        }
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (!sd_util.isCombatSituation(ship))
                return;

            interval.advance(amount);
            if (interval.intervalElapsed())
                for (MissileAPI missile : Global.getCombatEngine().getMissiles())
                    if (missile.getSource() == ship && !missile.getCustomData().containsKey("sd_booster")) {
                        Vector2f vel = missile.getVelocity(); // get the velocity
                        Vector2f normalised = vel.normalise(new Vector2f()); // normalise it so that you can scale it easily
                        Vector2f goal = (Vector2f) normalised.scale(missile.getMaxSpeed()); // scale the normalised vector to w/e the correct top speed should be
                        missile.getVelocity().set(goal.x, goal.y);
                        missile.getCustomData().put("sd_booster", -1);
                    }
        }
    }
}
