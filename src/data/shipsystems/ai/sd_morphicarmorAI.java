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
            ArmorGridAPI grid = ship.getArmorGrid();
            // god fucking damn that's a lotta prereqs
            if (!AIUtils.canUseSystemThisFrame(ship) || (!ship.getSystem().isOn() && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)) ||
                    sd_morphicarmor.getAverageArmorPerCell(grid) <= grid.getMaxArmorInCell() * sd_morphicarmor.DESTROYED_THRESHOLD || ship.getFluxLevel() >= sd_morphicarmor.HIGH_FLUX ||
                    sd_morphicarmor.isArmorGridBalanced(grid) || sd_morphicarmor.getCellsAroundAverage(grid, true).size() == 0)
                return; // the system automatically shuts off when it does nothing, so we can just return in that case
            float desirePos = 0;
            float desireNeg = 0;
            // We want the system on if:
            // 1. Our armor grid isn't balanced
            desirePos += 150;
            // We want the system off if:
            // 1. Our flux level is too high
            desireNeg -= (ship.getHardFluxLevel() + ship.getFluxLevel()) * 100;
            // 2. We could dissipate hardflux
            if (sd_util.isNumberWithinRange(ship.getHardFluxLevel(), ship.getFluxLevel(), 1)) {
                if (ship.getShield() == null || ship.getShield().isOff())
                    desireNeg -= ship.getHardFluxLevel() * 100;
            }
            // system has a failsafe to shut itself off if the ship's about to flux itself out with it, so we don't need to write AI for that case
            float desireTotal = desirePos + desireNeg;
            if (debug)
                Console.showMessage("Desire Total: "+ desireTotal +" Desire Pos: "+ desirePos +" Desire Neg: "+ desireNeg);

            if (ship.getSystem() instanceof sd_morphicarmor) {
                if (desireTotal >= 100 && !ship.getSystem().isOn())
                    ship.useSystem();
                if (desireTotal <= 0 && ship.getSystem().isOn())
                    ship.useSystem();
            } else if (ship.getPhaseCloak() != null) { // for some reason cloak instanceof morphicarmor doesn't work, and also I get an NPE if I don't do a check for cloak being present
                if (desireTotal >= 100 && !ship.getPhaseCloak().isOn())
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, -1);
                if (desireTotal <= 0 && ship.getPhaseCloak().isOn())
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, -1);
            }
        }
    }
}
