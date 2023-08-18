package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags.*;

public class sd_siegemodeAI implements ShipSystemAIScript {
    ShipAPI ship;
    CombatEngineAPI engine;
    ShipwideAIFlags flags;
    ShipSystemAPI system;
    float desire;
    final IntervalUtil interval = new IntervalUtil(0.5f, 1f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;
    }

    //NOTE: CHECK WHETHER THE CRITICAL DPS DANGER FLAG IS ACTUALLY USED BY NORMAL SHIPS

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            CombatEntityAPI nearestEnemy = AIUtils.getNearestEnemy(ship);
            boolean systemNeedsOffNow = (nearestEnemy == null || target == null || ship.getAIFlags().hasFlag(NEEDS_HELP));

            //immediately shut off the system if: there's no enemies, we have no target, or we're panicking
            if (systemNeedsOffNow && ship.getSystem().isOn()) {
                ship.useSystem();
                return;
            }
            //don't bother even checking whether to turn the system on if: there's no enemies, we have no target, we're panicking, or the system is on cooldown
            if (systemNeedsOffNow || system.getCooldownRemaining() > 0) return;

            //calculate our ship's optimal range by getting the range of all our weapons, weighted by DPS
            float totalDPS = 0;
            float totalWeightedRange = 0;
            float optimalWeaponRange = 0;
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                float weaponDPS = weapon.getSpec().getDerivedStats().getDps();
                float weaponRange = weapon.getRange();

                //adjust the weight based on DPS
                totalWeightedRange += weaponRange * weaponDPS;
                totalDPS += weaponDPS;
            }
            if (totalDPS > 0) {
                optimalWeaponRange = totalWeightedRange / totalDPS;
            }

            //add desire if the enemy is within our optimal range (base weapon range +33% or -25%), otherwise subtract desire
            float targetDistance = MathUtils.getDistance(ship, target);
            //weighted by how far the deviation is from the preferred range instead of a flat "is it in or is it out?"
            float deviation = Math.abs(targetDistance - optimalWeaponRange);
            float deviationFactor = 1.0f - (deviation / optimalWeaponRange);
            if (targetDistance <= optimalWeaponRange * 1.33 || targetDistance >= optimalWeaponRange * 0.75) {
                desire += 50 * deviationFactor;
            } else if (targetDistance >= optimalWeaponRange * 1.33 || targetDistance <= optimalWeaponRange * 0.75) {
                desire -= 50 * deviationFactor;
            }

            //want system on if we have various flags
            if (ship.getAIFlags().hasFlag(MAINTAINING_STRIKE_RANGE))
                desire += 33;
            if (ship.getAIFlags().hasFlag(CAMP_LOCATION))
                desire += 33;
            if (ship.getAIFlags().hasFlag(ESCORT_OTHER_SHIP))
                desire += 33;

            //want system off if we have various flags
            if (ship.getAIFlags().hasFlag(AVOIDING_BORDER))
                desire -= 33;
            if (ship.getAIFlags().hasFlag(HAS_POTENTIAL_MINE_TRIGGER_NEARBY))
                desire -= 33;
            if (ship.getAIFlags().hasFlag(BACKING_OFF))
                desire -= 33;

            //want system on if target is getting fucked up
            if (target.getAIFlags().hasFlag(NEEDS_HELP))
                desire += 50;

            //want system on if we have missiles in flight
            boolean firedMissiles = false;
            for (MissileAPI missile : Global.getCombatEngine().getMissiles()) {
                if (missile.getSource() == ship) {
                    firedMissiles = true;
                    break;
                }
            }
            if (firedMissiles)
                desire += 50;

            //want system off if enemy is too close, more if it's a bigger ship, more if there are many, less if they are getting fucked up
            for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, optimalWeaponRange)) {
                if (enemy.getAIFlags().hasFlag(NEEDS_HELP))
                    break;
                if (enemy.getHullSize() == ShipAPI.HullSize.FRIGATE)
                    desire -= 25 / (optimalWeaponRange / MathUtils.getDistance(ship, enemy));
                if (enemy.getHullSize() == ShipAPI.HullSize.DESTROYER)
                    desire -= 50 / (optimalWeaponRange / MathUtils.getDistance(ship, enemy));
                if (enemy.getHullSize() == ShipAPI.HullSize.CRUISER)
                    desire -= 75 / (optimalWeaponRange / MathUtils.getDistance(ship, enemy));
                if (enemy.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)
                    desire -= 150 / (optimalWeaponRange / MathUtils.getDistance(ship, enemy));
            }

            //want system off if hard flux is high
            desire -= ship.getHardFluxLevel() * 133;

            if (desire >= 100 && !ship.getSystem().isOn())
                ship.useSystem();
            if (desire <= -100 && ship.getSystem().isOn())
                ship.useSystem();
            desire = 0;
        }
    }
}
