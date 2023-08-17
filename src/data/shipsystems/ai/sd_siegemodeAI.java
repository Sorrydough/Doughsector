package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
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
            //immediately shut off the system if: there's no enemies, we have no target, or we're panicking
            CombatEntityAPI nearestEnemy = AIUtils.getNearestEnemy(ship);
            if (nearestEnemy == null || target == null || ship.getAIFlags().hasFlag(NEEDS_HELP) && ship.getSystem().isOn())
                ship.useSystem();
            //don't bother even calculating whether to turn the system on if: there's no enemies, we have no target, we're panicking, or the system is on cooldown
            if (nearestEnemy == null || target == null || ship.getAIFlags().hasFlag(NEEDS_HELP) || system.getCooldownRemaining() > 0)
                return;
            float nearestEnemyDistance = MathUtils.getDistance(ship, nearestEnemy);
            float targetDistance = MathUtils.getDistance(ship, target);
            //create a paired list of nearby enemy ships, their flux levels, and their hull sizes

            if (!system.isOn()) {
                //we don't want to turn the system on if:
                //desire -= enemy is too close or too far - find the deviation between the target and our optimal range and subtract desire based on that
                //

                if (nearestEnemyDistance < 1000)
                    return;

                //4.


                //if (targetDistance > ship.getbestweapon'srange * 1.25)
                //I need to


                //desire += 100 if the enemy ship is within optimal range +25%
                //desire += 100 if we've fired missiles and those missiles have a chance of reaching the target

            } else if (system.isOn()) {



                //don't want to turn system off if we have missiles in flight that might hit their target
                //desire -=
                //want to turn system off if target is deviating significantly outside our optimal range
                //desire +=
                //want to turn system off if enemy is nearby, more if it's a bigger ship, more if there are many, less if they are getting fucked up
                //desire +=
                //want to turn system off if hard flux is high
                desire += ship.getHardFluxLevel() * 133;
            }
            if (desire >= 100)
                ship.useSystem();
            desire = 0;
        }
    }
}
