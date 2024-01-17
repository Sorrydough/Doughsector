package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.sd_util;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
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
        final List<MissileAPI> modifiedMissiles = new ArrayList<>();
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (!sd_util.isCombatSituation(ship))
                return;

            for (WeaponAPI weapon : ship.getAllWeapons())
                if (weapon.getCooldownRemaining() > 0 && weapon.getType() == WeaponAPI.WeaponType.MISSILE)
                    for (MissileAPI missile : Global.getCombatEngine().getMissiles())
                        if (missile.getSource() == ship && !modifiedMissiles.contains(missile)) { // arthr did this vector math, turns out he's good at that
                            Vector2f newVel = Misc.rotateAroundOrigin(new Vector2f(missile.getMaxSpeed(),0),missile.getFacing());
                            missile.getVelocity().set(newVel.x + missile.getVelocity().x,newVel.y+ missile.getVelocity().y);
                            modifiedMissiles.add(missile);
                        }

            for (MissileAPI missile : new ArrayList<>(modifiedMissiles))
                if (!Global.getCombatEngine().getMissiles().contains(missile))
                    modifiedMissiles.remove(missile);
        }
    }
}
