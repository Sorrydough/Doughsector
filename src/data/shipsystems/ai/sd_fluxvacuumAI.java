package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags.*;
import static com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags.NEEDS_HELP;

public class sd_fluxvacuumAI implements ShipSystemAIScript {

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
            CombatEntityAPI nearestEnemy = AIUtils.getNearestEnemy(ship);
            boolean systemNeedsOffNow = (nearestEnemy == null || target == null || ship.getAIFlags().hasFlag(NEEDS_HELP));

            //immediately shut off the system if: there's no enemies, we have no target, or we're panicking
            if (systemNeedsOffNow && ship.getSystem().isOn()) {
                ship.useSystem();
                return;
            }
            //don't bother even checking whether to turn the system on if: there's no enemies, we have no target, we're panicking, or the system is on cooldown
            if (systemNeedsOffNow || system.getCooldownRemaining() > 0) return;




            //want system on if we have various flags
            if (ship.getAIFlags().hasFlag(CAMP_LOCATION))
                desire += 33;
            if (ship.getAIFlags().hasFlag(ESCORT_OTHER_SHIP))
                desire += 33;

            //want system off if we have various flags
//            if (ship.getAIFlags().hasFlag(AVOIDING_BORDER))
//                desire -= 33;
//            if (ship.getAIFlags().hasFlag(HAS_POTENTIAL_MINE_TRIGGER_NEARBY))
//                desire -= 33;
//            if (ship.getAIFlags().hasFlag(BACKING_OFF))
//                desire -= 33;



            //want system off if enemy is too close, more if it's a bigger ship, more if there are many, less if they are getting fucked up
//            for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, optimalWeaponRange)) {
//                if (enemy.getAIFlags().hasFlag(NEEDS_HELP))
//                    break;
//                if (enemy.getHullSize() == ShipAPI.HullSize.FRIGATE)
//                    desire -= 25 / (optimalWeaponRange / MathUtils.getDistance(ship, enemy));
//                if (enemy.getHullSize() == ShipAPI.HullSize.DESTROYER)
//                    desire -= 50 / (optimalWeaponRange / MathUtils.getDistance(ship, enemy));
//                if (enemy.getHullSize() == ShipAPI.HullSize.CRUISER)
//                    desire -= 75 / (optimalWeaponRange / MathUtils.getDistance(ship, enemy));
//                if (enemy.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)
//                    desire -= 150 / (optimalWeaponRange / MathUtils.getDistance(ship, enemy));
//            }

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
