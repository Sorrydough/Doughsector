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

public class sd_morphicarmor extends BaseShipSystemScript {

	final IntervalUtil interval = new IntervalUtil(0.01f, 0.1f);
	final boolean debug = false;
	static final float DEVIATION_PERCENT = 5;
	final float FLUX_GEN_DIVISOR = 10;
	final Color Color1 = new Color(250, 235, 215, 15);
	final Color Color2 = new Color(255,120,80,255);
	final Color Color3 = new Color(255,120,80,50);
	final Color Color4 = new Color(255,120,80,200);

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		ship.setJitter(id, Color3, effectLevel, 1, 0, 5);
		ship.setJitterUnder(id, Color4, effectLevel, 10, 0, 7);

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
					if (currentArmor > averageArmorPerCell && !isNumberWithinRange(currentArmor, averageArmorPerCell, DEVIATION_PERCENT)) {
						cellsAboveAverage.add(new Vector2f(ix, iy));
					} else if (currentArmor < averageArmorPerCell && !isNumberWithinRange(currentArmor, averageArmorPerCell, DEVIATION_PERCENT)) {
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
			if (debug)
				Console.showMessage("Amount needed: "+ amountNeededToTransfer +" Amount Able: "+ amountAbleToTransfer +" Amount To: "+ amountToTransfer);

			//start vfx: draw an emp arc to the target cell if it's within bounds
			Vector2f toSubtractLoc = (grid.getLocation((int) cellToSubtract.x, (int) cellToSubtract.y));
			Vector2f toAddLoc = (grid.getLocation((int) cellToAdd.x, (int) cellToAdd.y));
			boolean isToSubtractInBounds = CollisionUtils.isPointWithinBounds(toSubtractLoc, ship);
			boolean isToAddInBounds = CollisionUtils.isPointWithinBounds(toAddLoc, ship);
			if (isToAddInBounds)
				Global.getCombatEngine().spawnEmpArcVisual(CollisionUtils.getNearestPointOnBounds(toSubtractLoc, ship), ship, toAddLoc, ship, 8, Color1, Color2);
			//draw spark effects on the cell if it's within bounds
			if (isToSubtractInBounds)
				drawParticles(toSubtractLoc, ship, amountToTransfer);
			if (isToAddInBounds)
				drawParticles(toAddLoc, ship, amountToTransfer);
			//generate flux according to shield upkeep and amount of armor hp transferred
			if (ship.getShield() != null)
				ship.getFluxTracker().increaseFlux(amountToTransfer * ship.getShield().getUpkeep() / FLUX_GEN_DIVISOR, true);
			//cleanup
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
		float numImbalanced = 0;
		boolean balanced = true;
		Outer:
		for (int ix = 0; ix < grid.getGrid().length; ix++) {
			for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
				if (!isNumberWithinRange(grid.getArmorValue(ix, iy), averageArmor, DEVIATION_PERCENT)) {
					numImbalanced += 1; //need to have this for the special case where we have the entire armor grid balanced except for a couple cells and they become unrestoreable
					if (numImbalanced > 2) {
						balanced = false;
						break Outer;
					}
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

	public static void drawParticles(Vector2f loc, ShipAPI ship, float size) {
		float sizeSqrt = (float) Math.sqrt(size);
		float particleIntensity = 0.7f + (ship.getFluxLevel() * 0.3f);
		Color particleColor = new Color(255,120,80, (int) Math.min(155 + (ship.getFluxLevel() * 100), 255));
		for (int i = 0; i < Math.round(sizeSqrt * 2); i++) {
			//sparks
			float particleSize = MathUtils.getRandomNumberInRange(sizeSqrt, sizeSqrt * 2);
			float particleDuration = MathUtils.getRandomNumberInRange(sizeSqrt / 100, sizeSqrt / 50);
			Vector2f particleLoc = MathUtils.getRandomPointOnCircumference(loc, sizeSqrt);
			Vector2f particleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(sizeSqrt, size), MathUtils.getRandomNumberInRange(-180f, 180f));
			Global.getCombatEngine().addSmoothParticle(particleLoc, particleVel, particleSize, particleIntensity, particleDuration, particleColor);
		}
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Rebalancing armor", false);
		}
		return null;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo() || system.getState() != ShipSystemAPI.SystemState.IDLE)
			return null;
		if (isArmorGridBalanced(ship.getArmorGrid()))
			return "ARMOR BALANCED";
		return "READY";
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return getAverageArmorPerCell(ship.getArmorGrid()) > ship.getArmorGrid().getMaxArmorInCell() / 10 && !isArmorGridBalanced(ship.getArmorGrid());
	}
}



