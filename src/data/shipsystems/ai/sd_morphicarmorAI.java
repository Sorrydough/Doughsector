package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.sd_morphicarmor;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.sd_util;

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
            // the system automatically shuts off when it does nothing, so we can just return in that case
            if (!AIUtils.canUseSystemThisFrame(ship) || (!ship.getSystem().isOn() && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)) ||
                    sd_morphicarmor.isArmorGridBalanced(ship.getArmorGrid()) || sd_morphicarmor.getCellsAroundAverage(ship.getArmorGrid(), true).size() == 0)
                return;
            float desirePos = 0;
            float desireNeg = 0;
            // We want the system on if:
            // 1. Our armor grid isn't balanced
            desirePos += 150;
            // We want the system off if:
            // 1. Our flux level is too high
            desireNeg -= (ship.getHardFluxLevel() * 100 + ship.getFluxLevel() * 100);
            // 2. We're at risk of overloading ourselves (or getting overloaded) // TODO: CHECK IF THIS CAN BE REMOVED NOW THAT I FOUND THE DO NOT PURSUE BUG
            if (ship.getFluxLevel() >= 0.9) //&& ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)
                desireNeg -= 50;
            // 3. We could dissipate hardflux
            if (sd_util.isNumberWithinRange(ship.getHardFluxLevel(), ship.getFluxLevel(), 1)) {
                if (ship.getShield() == null || ship.getShield().isOff()) // TODO: CHECK THE SHIELD SHUNT BEHAVIOR AND SEE IF THAT NEEDS TO BE CHANGED AT ALL
                    desireNeg -= ship.getHardFluxLevel() * 100;
            }
            float desireTotal = desirePos + desireNeg;
            if (debug)
                Console.showMessage("Desire Total: "+ desireTotal +" Desire Pos: "+ desirePos +" Desire Neg: "+ desireNeg);
            if (desireTotal >= 100 && !ship.getSystem().isOn())
                ship.useSystem();
            if (desireTotal <= 0 && ship.getSystem().isOn())
                ship.useSystem();
        }
    }
}
