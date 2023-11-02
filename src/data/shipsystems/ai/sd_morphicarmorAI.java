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
            // god fucking damn that's a lotta prereqs
            if (!AIUtils.canUseSystemThisFrame(ship) || (!ship.getSystem().isOn() && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)) ||
                    sd_morphicarmor.getAverageArmorPerCell(grid) <= grid.getMaxArmorInCell() * sd_morphicarmor.DESTROYED_THRESHOLD || ship.getFluxLevel() >= sd_morphicarmor.HIGH_FLUX ||
                    sd_morphicarmor.isArmorGridBalanced(grid) || sd_morphicarmor.getCellsAroundAverage(grid, true).size() == 0)
                return;
            float desirePos = 0;
            float desireNeg = 0;
            // We want the system on if:
            // 1. Our armor grid isn't balanced
            desirePos += 150;
            // We want the system off if:
            // 1. Our flux level is too high
            desireNeg -= (ship.getHardFluxLevel() + ship.getFluxLevel()) * 100;
            // the system automatically shuts off when it does nothing or when the ship is about to flux itself out, so we don't need to write code for those situations
            int desireTotal = (int) (desirePos + desireNeg);
            if (debug)
                Console.showMessage("Desire Total: "+ desireTotal +" Desire Pos: "+ desirePos +" Desire Neg: "+ desireNeg);

            sd_util.activateSystem(ship, "sd_morphicarmor", desireTotal);
        }
    }
}
