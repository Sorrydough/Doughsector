package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.sd_util;
import data.weapons.mote.sd_moteAIScript;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.List;

public class sd_motearmor extends BaseShipSystemScript {
    final IntervalUtil interval = new IntervalUtil(0.015f, 0.15f);
    final Map<ShipAPI.HullSize, Integer> ARMOR_PER_MOTE = new HashMap<>(); {
        ARMOR_PER_MOTE.put(ShipAPI.HullSize.FRIGATE, 25);
        ARMOR_PER_MOTE.put(ShipAPI.HullSize.DESTROYER, 50);
        ARMOR_PER_MOTE.put(ShipAPI.HullSize.CRUISER, 75);
        ARMOR_PER_MOTE.put(ShipAPI.HullSize.CAPITAL_SHIP, 150);
    }
    public static final Map<ShipAPI.HullSize, Integer> MAX_MOTES = new HashMap<>(); {
        MAX_MOTES.put(ShipAPI.HullSize.FRIGATE, 8);
        MAX_MOTES.put(ShipAPI.HullSize.DESTROYER, 13);
        MAX_MOTES.put(ShipAPI.HullSize.CRUISER, 21);
        MAX_MOTES.put(ShipAPI.HullSize.CAPITAL_SHIP, 34);
    }
    float moteProgress = 0;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (!sd_util.isCombatSituation(ship))
            return;

        ArmorGridAPI grid = ship.getArmorGrid();

        if (sd_mnemonicarmor.isArmorGridBalanced(grid)) {
            ship.setJitter(id, sd_util.factionColor, effectLevel, 2, 0, 5);
            return;
        } else {
            ship.setJitter(id, sd_util.healColor, effectLevel, 2, 0, 5);
        }

        interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
        if (interval.intervalElapsed()) {

            // keeps the list of motes attached to this ship cleaned up
            SharedMoteAIData data = getSharedData(ship);
            Iterator<MissileAPI> iter = data.motes.iterator();
            while (iter.hasNext())
                if (!Global.getCombatEngine().isMissileAlive(iter.next()))
                    iter.remove();

            // while I could rebalance the armor grid all at once, I want it to look nice and happen only one cell at a time, so that complicates everything
            // firstly we need to calculate the average hp of the grid which is done elsewhere with the getAverageArmorPerCell function
            float averageArmorPerCell = sd_mnemonicarmor.getAverageArmorPerCell(grid);
            // next we create a list of cells above average, and another list of cells below average
            List<Vector2f> cellsAboveAverage = sd_mnemonicarmor.getCellsAroundAverage(grid, true);
            List<Vector2f> cellsBelowAverage = sd_mnemonicarmor.getCellsAroundAverage(grid, false);
            // now that we have a list of cells above and below the average, we need to randomly choose one of the former and move the delta to the latter
            Vector2f cellToSubtract = cellsAboveAverage.get(new Random().nextInt(cellsAboveAverage.size()));
            Vector2f cellToAdd = cellsBelowAverage.get(new Random().nextInt(cellsBelowAverage.size()));

            // find the amount we need to subtract from the cell - this the maximum of the amount needed or the amount the cell can provide
            float amountNeededToTransfer = ship.getArmorGrid().getMaxArmorInCell() - ship.getArmorGrid().getArmorValue((int) cellToAdd.x, (int) cellToAdd.y);
            float amountAbleToTransfer = (ship.getArmorGrid().getArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y) - averageArmorPerCell);
            if (amountAbleToTransfer <= 0)
                return;

            float amountToTransfer = Math.min(amountNeededToTransfer, amountAbleToTransfer);
            // subtract the amount from the donating cell and add it to the recieving cell
            ship.getArmorGrid().setArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y, ship.getArmorGrid().getArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y) - amountToTransfer);
            ship.getArmorGrid().setArmorValue((int) cellToAdd.x, (int) cellToAdd.y, ship.getArmorGrid().getArmorValue((int) cellToAdd.x, (int) cellToAdd.y) + amountToTransfer);
            //Console.showMessage("Amount needed: "+ amountNeededToTransfer +" Amount Able: "+ amountAbleToTransfer +" Amount To: "+ amountToTransfer);

            // start vfx: draw an emp arc to the target cell if it's within bounds
            Vector2f toSubtractLoc = (grid.getLocation((int) cellToSubtract.x, (int) cellToSubtract.y));
            Vector2f toAddLoc = (grid.getLocation((int) cellToAdd.x, (int) cellToAdd.y));
            boolean isToSubtractInBounds = CollisionUtils.isPointWithinBounds(toSubtractLoc, ship);
            boolean isToAddInBounds = CollisionUtils.isPointWithinBounds(toAddLoc, ship);
            float intensity = sd_mnemonicarmor.getAverageArmorPerCell(grid) / grid.getMaxArmorInCell();
            float thickness = (2 + amountToTransfer * 2) * intensity;
            if (isToAddInBounds)
                Global.getCombatEngine().spawnEmpArcVisual(CollisionUtils.getNearestPointOnBounds(toSubtractLoc, ship), ship, toAddLoc, ship,
                        thickness, sd_util.healColor, sd_util.damageUnderColor);
            // draw spark effects on the cell if it's within bounds
            if (isToSubtractInBounds)
                sd_mnemonicarmor.drawVfx(toSubtractLoc, ship, amountToTransfer, intensity);
            if (isToAddInBounds)
                sd_mnemonicarmor.drawVfx(toAddLoc, ship, amountToTransfer, intensity);

            // generate flux according to amount of armor hp transferred
            ship.getFluxTracker().increaseFlux(amountToTransfer * sd_mnemonicarmor.FLUX_PER_ARMOR, false);

            // cleanup
            ship.syncWithArmorGridState();
            ship.syncWeaponDecalsWithArmorDamage();

            // some mote stuff
            moteProgress += amountToTransfer;
            if (moteProgress >= ARMOR_PER_MOTE.get(ship.getHullSize()) && data.motes.size() < MAX_MOTES.get(ship.getHullSize())) {
                emitMote(ship, CollisionUtils.getNearestPointOnBounds(toAddLoc, ship), true);
                moteProgress -= ARMOR_PER_MOTE.get(ship.getHullSize());
            }
        }
    }
    public static class SharedMoteAIData {
        public float elapsed = 0f;
        public List<MissileAPI> motes = new ArrayList<>();
        public ShipAPI attractorTarget = null;
    }
    public static SharedMoteAIData getSharedData(ShipAPI source) {
        String key = source + "_mote_AI_shared";
        SharedMoteAIData data = (SharedMoteAIData) Global.getCombatEngine().getCustomData().get(key);
        if (data == null) {
            data = new SharedMoteAIData();
            Global.getCombatEngine().getCustomData().put(key, data);
        }
        return data;
    }
    public static void emitMote(ShipAPI ship, Vector2f loc, boolean sound) {
        int angleOffset = new Random().nextInt(361) - 180;
        MissileAPI mote = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "sd_motelauncher", loc, angleOffset + ship.getFacing(), ship.getVelocity());
        if (sound)
            Global.getSoundPlayer().playSound("system_flare_launcher_active", 1.0f, 1.6f, loc, ship.getVelocity());
        mote.setMissileAI(new sd_moteAIScript(mote));
        mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
        mote.setEmpResistance(10000);
        getSharedData(ship).motes.add(mote);
    }
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0)
            return new StatusData("REBALANCING ARMOR", false); // todo: figure out how to check the armor grid from here
        return null;
    }
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        ArmorGridAPI grid = ship.getArmorGrid();
        if (!sd_util.canUseSystemThisFrame(ship))
            return "STANDBY";
        if (sd_mnemonicarmor.isArmorGridDestroyed(grid))
            return "ARMOR DESTROYED";
        if (sd_mnemonicarmor.isArmorGridBalanced(grid))
            return "ARMOR BALANCED";
        if (system.isActive())
            return "REBALANCING";
        return "READY";
    }
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return !sd_mnemonicarmor.isArmorGridDestroyed(ship.getArmorGrid()) && sd_util.canUseSystemThisFrame(ship);
    }
}
