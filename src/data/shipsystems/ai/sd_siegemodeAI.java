package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.Console;
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
    final IntervalUtil interval = new IntervalUtil(1.5f, 3f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;
    }

    //Things to debug: 1. Whether critical DPS danger flag is used by non-phase ships
    //2. Whether the ship is targeting anything beyond 1300 range
    //3. Whether the optimal DPS distance calculation is working properly

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

            //add desire if the enemy is within our optimal range, otherwise subtract desire
            float targetDistance = MathUtils.getDistance(ship, target);
            //weighted by how far the deviation is from the preferred range instead of a flat "is it in or is it out?"
            float deviation = Math.abs(targetDistance - optimalWeaponRange);
            float deviationFactor = 1 - (deviation / optimalWeaponRange);
            if (targetDistance <= optimalWeaponRange * 1.33 && targetDistance >= optimalWeaponRange * 0.8) {
                desire += 100 * deviationFactor;
            } else if (targetDistance > optimalWeaponRange * 1.33 || targetDistance < optimalWeaponRange * 0.8) {
                desire -= 50 * deviationFactor;
            }

            float targetSpeed = target.getHullSpec().getEngineSpec().getMaxSpeed();

            float ourSpeedSystemOff = ship.getMaxSpeed();
            if (ship.getSystem().isOn())
                ourSpeedSystemOff *= 1.25f;

            float ourSpeedSystemOn = ship.getMaxSpeed();
            if (!ship.getSystem().isOn())
                ourSpeedSystemOn *= 0.8;

            //want system off if the target is going to just run away when it's on, and we could otherwise catch it
            if (targetSpeed > ourSpeedSystemOn)
                desire -= 50;
            //cancel out a bit of that earlier desire reduction if they're going to kite us even if the system is off, so we can zone them out instead
            if (targetSpeed > ourSpeedSystemOff)
                desire += 25;
            //want system on if we can kite the target when it's active
            if (ourSpeedSystemOn > targetSpeed)
                desire += 25;

            //want system on if we have various flags
            if (ship.getAIFlags().hasFlag(MAINTAINING_STRIKE_RANGE))
                desire += 33;
            if (ship.getAIFlags().hasFlag(ESCORT_OTHER_SHIP))
                desire += 33;
            if (ship.getAIFlags().hasFlag(CAMP_LOCATION))
                desire += 33;

            //want system off if we have various flags
            if (ship.getAIFlags().hasFlag(BACKING_OFF))
                desire -= 33;
            if (ship.getAIFlags().hasFlag(AVOIDING_BORDER))
                desire -= 33;
            if (ship.getAIFlags().hasFlag(HAS_POTENTIAL_MINE_TRIGGER_NEARBY))
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
            for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, optimalWeaponRange * 0.8f)) {
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

            Console.showMessage("Optimal Range: "+ Math.round(optimalWeaponRange) +", Target Distance: "+ Math.round(targetDistance) +", Desire: "+ Math.round(desire));

            if (desire >= 100 && !ship.getSystem().isOn())
                ship.useSystem();
            if (desire <= -100 && ship.getSystem().isOn())
                ship.useSystem();
            desire = 0;
        }
    }
}
