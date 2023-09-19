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
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class sd_morphicarmor extends BaseShipSystemScript { //TODO: IMPROVE VISUALS WITH RANDOM VELOCITY ON PARTICLES, AND ADD EMP ARCS WHEN APPROPRIATE. MAYBE ALSO GENERATE SOFT FLUX PER CELL REBALANCED

	final IntervalUtil interval = new IntervalUtil(0.01f, 0.1f);
	static final float DEVIATION_PERCENT = 10;
	final Color Color1 = new Color(250, 235, 215, 50);
	final Color Color2 = new Color(250, 235, 215, 150);
	final Color Color3 = new Color(245,85,55,225);

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		ship.setJitter(id, Color1, effectLevel, 1, 0, 5);
		ship.setJitterUnder(id, Color2, effectLevel, 10, 0, 7);

		interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
		if (interval.intervalElapsed()) {
			//while I could rebalance the armor grid all at once, I want it to look nice and happen only one cell at a time, so that complicates everything
			//firstly we need to calculate the average hp of the grid which is done elsewhere with the getAverageArmorPerCell function
			List<Vector2f> cellsAboveAverage = new ArrayList<>();
			List<Vector2f> cellsBelowAverage = new ArrayList<>();
			ArmorGridAPI grid = ship.getArmorGrid();
			if (isArmorGridBalanced(grid))
				ship.getSystem().deactivate();
			float averageArmorPerCell = getAverageArmorPerCell(grid);
			//next we create a list of cells above average, and another list of cells below average
			for (int ix = 0; ix < grid.getGrid().length; ix++) {
				for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
					float currentArmor = grid.getArmorValue(ix, iy);
					if (currentArmor > averageArmorPerCell) {
						cellsAboveAverage.add(new Vector2f(ix, iy));
					} else if (currentArmor < averageArmorPerCell) {
						cellsBelowAverage.add(new Vector2f(ix, iy));
					}
				}
			}

			if (cellsAboveAverage.size() == 0 || cellsBelowAverage.size() == 0)
				return;
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
//			Console.showMessage("Amount needed: "+ amountNeededToTransfer +" Amount Able: "+ amountAbleToTransfer +" Amount To: "+ amountToTransfer);

			//TODO: MODIFY THE VISUALS TO VARY BY THE AMOUNT OF ARMOR BEING TRANSFERRED
			//vfx
			//start with an emp arc from location a to location b if they're both in bounds
			Vector2f toSubtractLoc = (grid.getLocation((int) cellToSubtract.x, (int) cellToSubtract.y));
			Vector2f toAddLoc = (grid.getLocation((int) cellToAdd.x, (int) cellToAdd.y));
			boolean isToSubtractInBounds = CollisionUtils.isPointWithinBounds(toSubtractLoc, ship);
			boolean isToAddInBounds = CollisionUtils.isPointWithinBounds(toAddLoc, ship);
			if (isToAddInBounds)
				Global.getCombatEngine().spawnEmpArcVisual(CollisionUtils.getNearestPointOnBounds(toSubtractLoc, ship), ship, toAddLoc, ship, 8, Color1, Color2);
			//next draw smoke effects on the cell if it's within bounds
			if (isToSubtractInBounds)
				drawSmokeParticles(toSubtractLoc, ship, amountToTransfer);
			if (isToAddInBounds)
				drawSmokeParticles(toAddLoc, ship, amountToTransfer);
			ship.syncWithArmorGridState();
			ship.syncWeaponDecalsWithArmorDamage();
		}
	}

	public static float getAverageArmorPerCell(ArmorGridAPI grid) {
		float armor = 0f;
		for (int ix = 0; ix < grid.getGrid().length; ix++) {
			for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
				armor += grid.getArmorValue(ix, iy);
			}
		}
		return armor / (grid.getGrid().length * grid.getGrid()[0].length);
	}

	public static boolean isArmorGridBalanced(ArmorGridAPI grid) {
		float averageArmor = getAverageArmorPerCell(grid);
		boolean balanced = true;
		Outer:
		for (int ix = 0; ix < grid.getGrid().length; ix++) {
			for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
				if (!isNumberWithinRange(grid.getArmorValue(ix, iy), averageArmor, DEVIATION_PERCENT)) {
					balanced = false;
					break Outer;
				}
			}
		}
		return balanced;
	}

	public static boolean isNumberWithinRange(float numberA, float numberB, float deviation) {
		float lowerBound = numberB - (numberB * (deviation / 100));
		float upperBound = numberB + (numberB * (deviation / 100));
		return numberA <= upperBound && numberA >= lowerBound;
	}

	public static void drawSmokeParticles(Vector2f loc, ShipAPI ship, float size) {
		for (int i = 0; i < 25; i++) {
			//smoke
			Vector2f nebVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(10f, 40f), MathUtils.getRandomNumberInRange(-180f, 180f));
			float randomSize2 = MathUtils.getRandomNumberInRange(20f, 32f);
			Color steamColor = new Color(100f / 255f, 110f / 255f, 100f / 255f, 0.25f);
			Global.getCombatEngine().addNebulaParticle(MathUtils.getRandomPointOnCircumference(loc, 5f), nebVel, randomSize2, 1.8f, 0.6f, 0.7f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), steamColor);
			//sparks
			Global.getCombatEngine().addSmoothParticle(loc, ship.getVelocity(), MathUtils.getRandomNumberInRange(25f, 35f), 0.8f, 0.1f, new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
			Vector2f fastParticleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(80f, 250f), MathUtils.getRandomNumberInRange(-180f, 180f));
			float randomSize01 = MathUtils.getRandomNumberInRange(3f, 5f);
			Global.getCombatEngine().addSmoothParticle(MathUtils.getRandomPointOnCircumference(loc, 4f), fastParticleVel, randomSize01, 0.8f, MathUtils.getRandomNumberInRange(0.2f, 0.25f), new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
			Vector2f particleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(35f, 125f), MathUtils.getRandomNumberInRange(-120f, 120f));
			float randomSize1 = MathUtils.getRandomNumberInRange(3f, 5f);
			Global.getCombatEngine().addSmoothParticle(MathUtils.getRandomPointOnCircumference(loc, 4f), particleVel, randomSize1, 0.8f, MathUtils.getRandomNumberInRange(0.35f, 0.5f), new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
		}
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Rebalancing armor", false); //TODO: CUSTOM READINESS/STATUS INDICATORS
		}
		return null;
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return getAverageArmorPerCell(ship.getArmorGrid()) > ship.getArmorGrid().getMaxArmorInCell() / 10 && !isArmorGridBalanced(ship.getArmorGrid());
	}
}



