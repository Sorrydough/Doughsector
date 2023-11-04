package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.sd_morphicarmor;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.sd_util;

import java.util.Objects;

public class sd_morphicarmorAI implements ShipSystemAIScript {
    final IntervalUtil interval = new IntervalUtil(0.5f, 1f);
    final boolean debug = false;
    ShipAPI ship;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) { this.ship = ship; }
    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            ArmorGridAPI grid = ship.getArmorGrid();
            // if any of these is the case then the system is definitely off and we don't want to turn it on, so we can return to save cpu time
            if (!AIUtils.canUseSystemThisFrame(ship) || (!ship.getSystem().isOn() && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)) // do not pursue thing to prevent fluxlocking
                    || sd_morphicarmor.getAverageArmorPerCell(grid) <= grid.getMaxArmorInCell() * sd_morphicarmor.DESTROYED_THRESHOLD)
                return;

            float desirePos = 0;
            float desireNeg = 0; // todo: add a way to check whether incoming damage is gonna fuck us up (aka damper field logic)
            // We want the system on if our armor grid isn't balanced, otherwise just turn it off immediately cuz it's doing nothing for ya tbqh
            if (ship.getFluxLevel() < 0.95f && !sd_morphicarmor.isArmorGridBalanced(grid)) {
                desirePos += 150;
                // We want the system off if:
                // 1. Our flux level is too high
                desireNeg -= (ship.getHardFluxLevel() + ship.getFluxLevel()) * 100;
                // 2. We could dissipate hardflux
                if (ship.getShield() != null && ship.getShield().isOff() && sd_util.isNumberWithinRange(ship.getHardFluxLevel(), ship.getFluxLevel(), 1))
                    desireNeg -= ship.getHardFluxLevel() * 100;
            }
            else desireNeg -= 50;

            sd_util.activateSystem(ship, "sd_morphicarmor", desirePos, desireNeg, debug);
        }
    }
}
