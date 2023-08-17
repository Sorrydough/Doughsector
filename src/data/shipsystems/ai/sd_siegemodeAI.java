package data.shipsystems.ai;

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
            //immediately shut off the system if: there's no enemies, we have no target, or we're panicking
            CombatEntityAPI nearestEnemy = AIUtils.getNearestEnemy(ship);
            if ((nearestEnemy == null || target == null || ship.getAIFlags().hasFlag(NEEDS_HELP)) && ship.getSystem().isOn()) { ship.useSystem(); return; }
            //don't bother even checking whether to turn the system on if: there's no enemies, we have no target, we're panicking, or the system is on cooldown
            if (nearestEnemy == null || target == null || ship.getAIFlags().hasFlag(NEEDS_HELP) || system.getCooldownRemaining() > 0) return;
            float nearestEnemyDistance = MathUtils.getDistance(ship, nearestEnemy);
            float targetDistance = MathUtils.getDistance(ship, target);
            //need to create a paired list of nearby enemy ships, their flux levels, their hull sizes, and whether they are getting fucked up


            //want system on if we have various flags
            if (ship.getAIFlags().hasFlag(MAINTAINING_STRIKE_RANGE) || ship.getAIFlags().hasFlag(CAMP_LOCATION) || ship.getAIFlags().hasFlag(ESCORT_OTHER_SHIP))
                desire += 50;
            //want system off if we have various flags
            if (ship.getAIFlags().hasFlag(AVOIDING_BORDER) || ship.getAIFlags().hasFlag(HAS_POTENTIAL_MINE_TRIGGER_NEARBY) || ship.getAIFlags().hasFlag(BACKING_OFF))
                desire -= 50;

            //want system on if we have missiles in flight that might hit their target
            //desire +=

            //want system on if the enemy is within our optimal range (base weapon range +50% or -25%)
            //desire +=
            //want system off if target is deviating significantly outside our optimal range
            //desire -=
            //want system off if enemy is nearby, more if it's a bigger ship, more if there are many, less if they are getting fucked up
            //desire +=

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
