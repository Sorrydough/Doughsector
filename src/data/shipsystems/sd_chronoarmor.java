package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class sd_chronoarmor extends BaseShipSystemScript {
    final static boolean debug = false;
    final float TIME_MULT = 3;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        ArmorGridAPI grid = ship.getArmorGrid();

        if (sd_morphicarmor.isArmorGridDestroyed(grid))
            ship.getSystem().deactivate();

        float jitterLevel = effectLevel; // copied from alex's temporal shell code. It's a mess, but whatever.
        float jitterRangeBonus = 0;
        float maxRangeBonus = 10f;
        if (state == State.IN) {
            jitterLevel = effectLevel / (1f / ship.getSystem().getChargeUpDur());
            if (jitterLevel > 1) {
                jitterLevel = 1f;
            }
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        } else if (state == State.ACTIVE) {
            jitterLevel = 1f;
            jitterRangeBonus = maxRangeBonus;
        } else if (state == State.OUT) {
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        }
        jitterLevel = (float) Math.sqrt(jitterLevel);
        effectLevel *= effectLevel;

        ship.setJitter(this, sd_morphicarmor.JITTER_COLOR, jitterLevel, 3, 0, 0 + jitterRangeBonus);
        ship.setJitterUnder(this, sd_morphicarmor.JITTER_UNDER_COLOR, jitterLevel, 25, 0f, 7f + jitterRangeBonus);

        ship.getEngineController().fadeToOtherColor(this, sd_morphicarmor.JITTER_COLOR, new Color(0,0,0,0), effectLevel, 0.5f);
        ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);

        float averageArmorPerCell = sd_morphicarmor.getAverageArmorPerCell(grid);
        float shipTimeMult = Math.max(1, TIME_MULT * (averageArmorPerCell / grid.getMaxArmorInCell()) * effectLevel);
        stats.getTimeMult().modifyMult(id, shipTimeMult);

        if (ship == Global.getCombatEngine().getPlayerShip())
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        else
            Global.getCombatEngine().getTimeMult().unmodify(id);

        stats.getEmpDamageTakenMult().modifyMult(id, sd_morphicarmor.EMP_MULT);
        stats.getArmorBonus().modifyMult(id, 1 / sd_morphicarmor.ARMOR_MULT);
        stats.getEffectiveArmorBonus().modifyMult(id, sd_morphicarmor.ARMOR_MULT);
        stats.getMinArmorFraction().modifyMult(id, sd_morphicarmor.ARMOR_MULT);

        if (sd_morphicarmor.isArmorGridBalanced(grid))
            return;

        sd_morphicarmor.interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
        if (sd_morphicarmor.interval.intervalElapsed()) {
            //while I could rebalance the armor grid all at once, I want it to look nice and happen only one cell at a time, so that complicates everything
            //firstly we need to calculate the average hp of the grid which is done elsewhere with the getAverageArmorPerCell function
            //next we create a list of cells above average, and another list of cells below average
            java.util.List<Vector2f> cellsAboveAverage = sd_morphicarmor.getCellsAroundAverage(grid, true);
            List<Vector2f> cellsBelowAverage = sd_morphicarmor.getCellsAroundAverage(grid, false);
            //now that we have a list of cells above and below the average, we need to randomly choose one of the former and move the delta to the latter
            Vector2f cellToSubtract = cellsAboveAverage.get(new Random().nextInt(cellsAboveAverage.size()));
            Vector2f cellToAdd = cellsBelowAverage.get(new Random().nextInt(cellsBelowAverage.size()));

            //find the amount we need to subtract from the cell - this the maximum of the amount needed or the amount the cell can provide
            float amountNeededToTransfer = ship.getArmorGrid().getMaxArmorInCell() - ship.getArmorGrid().getArmorValue((int) cellToAdd.x, (int) cellToAdd.y);
            float amountAbleToTransfer = (ship.getArmorGrid().getArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y) - averageArmorPerCell);
            if (amountAbleToTransfer <= 0)
                return;

            float amountToTransfer = Math.min(amountNeededToTransfer, amountAbleToTransfer);
            //subtract the amount from the donating cell and add it to the recieving cell
            ship.getArmorGrid().setArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y, ship.getArmorGrid().getArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y) - amountToTransfer);
            ship.getArmorGrid().setArmorValue((int) cellToAdd.x, (int) cellToAdd.y, ship.getArmorGrid().getArmorValue((int) cellToAdd.x, (int) cellToAdd.y) + amountToTransfer);
            if (debug)
                Console.showMessage("Amount needed: "+ amountNeededToTransfer +" Amount Able: "+ amountAbleToTransfer +" Amount To: "+ amountToTransfer);

            //start vfx: draw an emp arc to the target cell if it's within bounds
            Vector2f toSubtractLoc = (grid.getLocation((int) cellToSubtract.x, (int) cellToSubtract.y));
            Vector2f toAddLoc = (grid.getLocation((int) cellToAdd.x, (int) cellToAdd.y));
            boolean isToSubtractInBounds = CollisionUtils.isPointWithinBounds(toSubtractLoc, ship);
            boolean isToAddInBounds = CollisionUtils.isPointWithinBounds(toAddLoc, ship);
            float intensity = sd_morphicarmor.getAverageArmorPerCell(grid) / grid.getMaxArmorInCell();
            float thickness = (2 + amountToTransfer * 2) * intensity;
            if (isToAddInBounds)
                Global.getCombatEngine().spawnEmpArcVisual(CollisionUtils.getNearestPointOnBounds(toSubtractLoc, ship),
                        ship, toAddLoc, ship, thickness, sd_morphicarmor.EMP_EDGE_COLOR, sd_morphicarmor.EMP_CENTER_COLOR);
            //draw spark effects on the cell if it's within bounds
            if (isToSubtractInBounds)
                sd_morphicarmor.drawVfx(toSubtractLoc, ship, amountToTransfer, intensity);
            if (isToAddInBounds)
                sd_morphicarmor.drawVfx(toAddLoc, ship, amountToTransfer, intensity);

            //generate flux according to amount of armor hp transferred
            float extraFlux = 0;
            if (ship.getShield() != null)
                extraFlux = (float) Math.sqrt(ship.getShield().getUpkeep() / sd_morphicarmor.FLUX_PER_ARMOR);
            ship.getFluxTracker().increaseFlux(amountToTransfer * (sd_morphicarmor.FLUX_PER_ARMOR + extraFlux), false);

            //cleanup
            ship.syncWithArmorGridState();
            ship.syncWeaponDecalsWithArmorDamage();
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodifyMult(id);
        stats.getArmorBonus().unmodifyMult(id);
        stats.getEffectiveArmorBonus().unmodifyMult(id);
        stats.getMinArmorFraction().unmodifyMult(id);
        stats.getTimeMult().unmodify(id);
    }
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0)
            return new StatusData("TIMEFLOW ALTERED", false); // todo: show how much the timeflow has degraded
        if (index == 1)
            return new StatusData("REBALANCING ARMOR", false); // todo: figure out how to check the armor grid from here
        return null;
    }
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        ArmorGridAPI grid = ship.getArmorGrid();
        if (sd_morphicarmor.isArmorGridDestroyed(grid))
            return "ARMOR DESTROYED";
        if (sd_morphicarmor.isArmorGridBalanced(grid))
            return "ARMOR BALANCED";
        if (system.isActive())
            return "REBALANCING";
        return "READY";
    }
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return !sd_morphicarmor.isArmorGridDestroyed(ship.getArmorGrid());
    }
}
