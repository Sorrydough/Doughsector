package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class sd_missileboosters extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new sd_missileboostersPlugin(ship));
    }

    static class sd_missileboostersPlugin extends BaseEveryFrameCombatPlugin {
        final ShipAPI ship;
        CombatEngineAPI engine;
        public sd_missileboostersPlugin(ShipAPI ship) {
            this.ship = ship;
            this.engine = Global.getCombatEngine();
        }
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused())
                return;

            for (MissileAPI missile : Global.getCombatEngine().getMissiles())
                if (MathUtils.getDistance(missile, ship) < 50 && !missile.getCustomData().containsKey("sd_booster")) {
                    Vector2f vel = missile.getVelocity(); //get the velocity
                    Vector2f normalised = vel.normalise(new Vector2f()); //normalise it so that you can scale it easily
                    Vector2f goal = (Vector2f) normalised.scale(missile.getMaxSpeed()); //scale the normalised vector to w/e the correct top speed should be
                    missile.getVelocity().set(goal.x, goal.y);
                    missile.getCustomData().put("sd_booster", -1);
                }
        }
    }
}
