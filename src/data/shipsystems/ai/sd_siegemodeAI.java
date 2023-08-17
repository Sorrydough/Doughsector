package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags.NEEDS_HELP;

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

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            desire = 0;
            CombatEntityAPI nearestEnemy = AIUtils.getNearestEnemy(ship);
            //float nearestEnemyDistance =
            //float targetDistance =
            //float


            if (!system.isOn()) {
                //we don't want to turn the system on if:
                //we have no target
                //the system is on cooldown
                //an enemy is too close
                //

                if (target == null)
                    return;
                if (system.getCooldownRemaining() > 0)
                    return;
                if (nearestEnemy != null && MathUtils.getDistance(ship, nearestEnemy) < 1000)
                    return;

                //we want to turn the system on if:
                //1. An enemy is within 1.25x of our weapon range
                //2. within missile range
                //3. we have a missile currently in flight
                //4.


                //if (target.getdistance > ship.getlongestrangeweapon * 1.25)
                //  return;




                if (desire >= 100)
                    ship.useSystem();

            } else if (system.isOn()) {
                //we want to turn the system off if:
                //1. We have no target
                //2. The target that we do have is too far
                //3. We're under threat
                //4. Our hard flux level is too high
                //5. An enemy is so close that we don't need the system to hit them
                //6.
                if (target == null)
                    desire += 100;
                if (target != null && MathUtils.getDistance(ship, target) > 1500)
                    desire += 100;
                if (ship.getAIFlags().hasFlag(NEEDS_HELP))
                    desire += 100;
                if (ship.getHardFluxLevel() > 0.5)
                    desire += ship.getHardFluxLevel() * 100;
                if (nearestEnemy != null && MathUtils.getDistance(ship, nearestEnemy) < 500)
                    desire += 100;
                if (desire >= 100)
                    ship.useSystem();
            }
        }
    }
}
