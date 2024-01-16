package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import data.sd_util;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class sd_missileboosters extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        Global.getCombatEngine().addPlugin(new sd_missileboostersPlugin(ship));
    }
    static class sd_missileboostersPlugin extends BaseEveryFrameCombatPlugin {
        final ShipAPI ship;
        public sd_missileboostersPlugin(ShipAPI ship) {
            this.ship = ship;
        }
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (!sd_util.isCombatSituation(ship))
                return;

            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getCooldownRemaining() > 0 && weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                    for (MissileAPI missile : Global.getCombatEngine().getMissiles())
                        if (missile.getSource() == ship && !missile.getCustomData().containsKey("sd_booster")) { // ruddygreat did this math stuff
                            Vector2f vel = missile.getVelocity(); // get the velocity
                            Vector2f normalised = vel.normalise(new Vector2f()); // normalise it so that you can scale it easily
                            Vector2f goal = (Vector2f) normalised.scale(missile.getMaxSpeed()); // scale the normalised vector to w/e the correct top speed should be
                            missile.getVelocity().set(goal.x, goal.y);
                            missile.getCustomData().put("sd_booster", -1);
                        }
                }
            }
        }
    }
}
