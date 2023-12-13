package data.shipsystems;

import java.awt.Color;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.dark.shaders.distortion.*;
import data.sd_util;

public class sd_morphicarmor extends BaseShipSystemScript {
	public static final float FLUX_PER_ARMOR = 3, DESTROYED_THRESHOLD = 0.1f;
	final IntervalUtil interval = new IntervalUtil(0.015f, 0.15f);
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		ArmorGridAPI grid = ship.getArmorGrid();

		if (isArmorGridBalanced(grid)) {
			ship.setJitter(id, sd_util.factionColor, effectLevel, 2, 0, 5);
			//ship.setJitterUnder(id, sd_util.factionUnderColor, effectLevel, 10, 0, 5);
			return;
		} else {
			ship.setJitter(id, sd_util.healColor, effectLevel, 2, 0, 5);
			//ship.setJitterUnder(id, sd_util.healUnderColor, effectLevel, 10, 0, 5);
		}

		interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
		if (interval.intervalElapsed()) {
			//while I could rebalance the armor grid all at once, I want it to look nice and happen only one cell at a time, so that complicates everything
			//firstly we need to calculate the average hp of the grid which is done elsewhere with the getAverageArmorPerCell function
			float averageArmorPerCell = getAverageArmorPerCell(grid);
			//next we create a list of cells above average, and another list of cells below average
			List<Vector2f> cellsAboveAverage = getCellsAroundAverage(grid, true);
			List<Vector2f> cellsBelowAverage = getCellsAroundAverage(grid, false);
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
			//Console.showMessage("Amount needed: "+ amountNeededToTransfer +" Amount Able: "+ amountAbleToTransfer +" Amount To: "+ amountToTransfer);

			//start vfx: draw an emp arc to the target cell if it's within bounds
			Vector2f toSubtractLoc = (grid.getLocation((int) cellToSubtract.x, (int) cellToSubtract.y));
			Vector2f toAddLoc = (grid.getLocation((int) cellToAdd.x, (int) cellToAdd.y));
			boolean isToSubtractInBounds = CollisionUtils.isPointWithinBounds(toSubtractLoc, ship);
			boolean isToAddInBounds = CollisionUtils.isPointWithinBounds(toAddLoc, ship);
			float intensity = getAverageArmorPerCell(grid) / grid.getMaxArmorInCell();
			float thickness = (2 + amountToTransfer * 2) * intensity;
			if (isToAddInBounds)
				Global.getCombatEngine().spawnEmpArcVisual(CollisionUtils.getNearestPointOnBounds(toSubtractLoc, ship), ship, toAddLoc, ship,
						thickness, sd_util.healColor, sd_util.damageUnderColor);
			//draw spark effects on the cell if it's within bounds
			if (isToSubtractInBounds)
				drawVfx(toSubtractLoc, ship, amountToTransfer, intensity);
			if (isToAddInBounds)
				drawVfx(toAddLoc, ship, amountToTransfer, intensity);

			//generate flux according to amount of armor hp transferred
			ship.getFluxTracker().increaseFlux(amountToTransfer * FLUX_PER_ARMOR, false);

			//cleanup
			ship.syncWithArmorGridState();
			ship.syncWeaponDecalsWithArmorDamage();
		}
	}

	public static List<Vector2f> getCellsAroundAverage(ArmorGridAPI grid, boolean above) {
		List<Vector2f> cells = new ArrayList<>();
		float average = getAverageArmorPerCell(grid);
		for (int ix = 0; ix < grid.getGrid().length; ix++) {
			for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
				float currentArmor = grid.getArmorValue(ix, iy);
				boolean isAboveAverage = currentArmor > average;
				boolean isWithinRange = sd_util.isNumberWithinRange(currentArmor, average, 1);
				if ((above && isAboveAverage && !isWithinRange) || (!above && !isAboveAverage && !isWithinRange)) {
					cells.add(new Vector2f(ix, iy));
				}
			}
		}
		return cells;
	}
	public static float getAverageArmorPerCell(ArmorGridAPI grid) { // chatgpt wrote this
		float armor = 0f;
		for (int ix = 0; ix < grid.getGrid().length; ix++) {
			for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
				armor += grid.getArmorValue(ix, iy);
			}
		}
		return armor / (grid.getGrid().length * grid.getGrid()[0].length);
	}
	public static boolean isArmorGridBalanced(ArmorGridAPI grid) {
		boolean balanced = false;
		List<Vector2f> cellsAboveAverage = getCellsAroundAverage(grid, true);
		List<Vector2f> cellsBelowAverage = getCellsAroundAverage(grid, false);
		if (cellsAboveAverage.isEmpty() || cellsBelowAverage.isEmpty())
			balanced = true;
		return balanced;
	}
	public static boolean isArmorGridDestroyed(ArmorGridAPI grid) {
		return getAverageArmorPerCell(grid) < grid.getMaxArmorInCell() * DESTROYED_THRESHOLD;
	}
	public static void drawVfx(Vector2f loc, ShipAPI ship, float size, float intensity) {
		float sizeSqrt = (float) Math.sqrt(size);
		Color particleColor = new Color(255,120,80, (int) Math.min(205 + (ship.getFluxLevel() * 50), 255));
		for (int i = 0; i < (2 + Math.round(sizeSqrt * 3)); i++) {
			//sparks
			float particleSize = 0.5f + MathUtils.getRandomNumberInRange(sizeSqrt * 2, sizeSqrt * 4);
			float particleDuration = MathUtils.getRandomNumberInRange(1, 2);
			Vector2f particleLoc = MathUtils.getRandomPointOnCircumference(loc, sizeSqrt);
			Vector2f particleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), 0.5f + MathUtils.getRandomNumberInRange(sizeSqrt, size), MathUtils.getRandomNumberInRange(-180f, 180f));
			//Console.showMessage("Transferred Sqrt: "+ sizeSqrt +" Particle Size: "+ particleSize +" Particle Duration: "+ particleDuration);
			Global.getCombatEngine().addSmoothParticle(particleLoc, particleVel, particleSize, intensity, particleDuration, particleColor);
		}
		//draw a distortion wave
		RippleDistortion ripple = new RippleDistortion();
		ripple.setLocation(loc);
		ripple.setSize((50 + size) * intensity);
		ripple.setVelocity(ship.getVelocity());
		ripple.setLifetime(intensity);
		ripple.setIntensity(intensity);
		DistortionShader.addDistortion(ripple);
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
		if (isArmorGridDestroyed(grid))
			return "ARMOR DESTROYED";
		if (isArmorGridBalanced(grid))
			return "ARMOR BALANCED";
		if (system.isActive())
			return "REBALANCING";
		return "READY";
	}
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return !isArmorGridDestroyed(ship.getArmorGrid()) && sd_util.canUseSystemThisFrame(ship);
	}
}



