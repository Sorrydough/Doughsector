package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.sd_morphicarmor;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class sd_morphicarmorAI implements ShipSystemAIScript {

    ShipAPI ship;
    CombatEngineAPI engine;
    ShipwideAIFlags flags;
    ShipSystemAPI system;
    float desire;
    final IntervalUtil interval = new IntervalUtil(0.5f, 1f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    boolean debug = false;

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            //the system automatically shuts off when it does nothing, so we can just return in that case
            if (sd_morphicarmor.isArmorGridBalanced(ship.getArmorGrid()) || ship.getFluxTracker().isOverloadedOrVenting()
                    || ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE) || sd_morphicarmor.getCellsAroundAverage(ship.getArmorGrid(), true).size() == 0)
                return;
            //We want the system on if:
            //1. Our armor grid isn't balanced
            desire += 150;

            //We want the system off if:
            //1. Our flux level is too high
            desire -= (ship.getFluxLevel() * 100 + ship.getHardFluxLevel() * 100);
            //2. We're at risk of overloading ourselves (or getting overloaded)
            if (ship.getFluxLevel() >= 0.85)
                desire -= 50;

            if (debug)
                Console.showMessage("Desire: "+ desire);

            if (desire >= 100 && !ship.getSystem().isOn())
                ship.useSystem();
            if (desire <= 0 && ship.getSystem().isOn())
                ship.useSystem();
            desire = 0;
        }
    }
}
