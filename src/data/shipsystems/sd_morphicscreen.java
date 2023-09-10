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

public class sd_morphicscreen extends BaseShipSystemScript { //TODO: DEBUG THIS

	final IntervalUtil interval = new IntervalUtil(0.01f, 0.1f);

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		ship.setJitter(id, new Color(250, 235, 215, 50), effectLevel, 2, 0, 5);
		ship.setJitterUnder(id, new Color(250, 235, 215, 150), effectLevel, 25, 0, 7);

		stats.getEmpDamageTakenMult().modifyMult(id, 0.5f * effectLevel);
		stats.getArmorDamageTakenMult().modifyMult(id,0.5f * effectLevel);

		interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
		if (interval.intervalElapsed()) {
			//while I could rebalance the armor grid all at once, I want it to look nice and happen only one cell per frame, so that complicates everything
			//firstly we need to calculate the average hp of the grid which is done elsewhere with the getAverageArmor function
			List<Vector2f> cellsAboveAverage = new ArrayList<>();
			List<Vector2f> cellsBelowAverage = new ArrayList<>();
			ArmorGridAPI grid = ship.getArmorGrid();
			//next we create a list of cells above average, and another list of cells below average
			for (int ix = 0; ix < grid.getGrid().length; ix++) {
				for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
					float currentArmor = grid.getArmorValue(ix, iy);
					if (currentArmor > getAverageArmorPerCell(grid)) {
						cellsAboveAverage.add(new Vector2f(ix, iy));
					} else if (currentArmor < getAverageArmorPerCell(grid)) {
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
			float amountAbleToTransfer = (ship.getArmorGrid().getArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y) - getAverageArmorPerCell(grid));
			if (amountAbleToTransfer < 1)
				amountAbleToTransfer = 0;
			if (amountAbleToTransfer == 0)
				return;
			float amountToTransfer = Math.min(amountNeededToTransfer, amountAbleToTransfer);
			//subtract the amount from the donating cell and add it to the recieving cell
			ship.getArmorGrid().setArmorValue((int) cellToAdd.x, (int) cellToAdd.y, ship.getArmorGrid().getArmorValue((int) cellToAdd.x, (int) cellToAdd.y) + amountToTransfer);
			ship.getArmorGrid().setArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y, ship.getArmorGrid().getArmorValue((int) cellToSubtract.x, (int) cellToSubtract.y) - amountToTransfer);

//			Console.showMessage("Amount needed: "+ amountNeededToTransfer +" Amount Able: "+ amountAbleToTransfer +" Amount To: "+ amountToTransfer);

			//make a smoke poof
			List<Vector2f> cells = new ArrayList<>();
			cells.add(cellToSubtract);
			cells.add(cellToAdd);
			for (Vector2f cell : cells) {
				Vector2f cellLocOnModel = grid.getLocation((int) cell.x, (int) cell.y);
				if (CollisionUtils.isPointWithinBounds(cellLocOnModel, ship)) {
					//engine.addFloatingText(cellLocOnModel, valueOf(totalHullDamage), 15, new Color(100f / 255f, 110f / 255f, 100f / 255f, 0.25f), ship, 10, 15); //debug text
					for (int i = 0; i < 25; i++) {
						//smoke
						Vector2f nebVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(10f, 40f), MathUtils.getRandomNumberInRange(-180f, 180f));
						float randomSize2 = MathUtils.getRandomNumberInRange(20f, 32f);
						Color steamColor = new Color(100f / 255f, 110f / 255f, 100f / 255f, 0.25f);
						Global.getCombatEngine().addNebulaParticle(MathUtils.getRandomPointOnCircumference(cellLocOnModel, 5f), nebVel, randomSize2, 1.8f, 0.6f, 0.7f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), steamColor);
						//sparks
						Global.getCombatEngine().addSmoothParticle(cellLocOnModel, ship.getVelocity(), MathUtils.getRandomNumberInRange(25f, 35f), 0.8f, 0.1f, new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
						Vector2f fastParticleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(80f, 250f), MathUtils.getRandomNumberInRange(-180f, 180f));
						float randomSize01 = MathUtils.getRandomNumberInRange(3f, 5f);
						Global.getCombatEngine().addSmoothParticle(MathUtils.getRandomPointOnCircumference(cellLocOnModel, 4f), fastParticleVel, randomSize01, 0.8f, MathUtils.getRandomNumberInRange(0.2f, 0.25f), new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
						Vector2f particleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(35f, 125f), MathUtils.getRandomNumberInRange(-120f, 120f));
						float randomSize1 = MathUtils.getRandomNumberInRange(3f, 5f);
						Global.getCombatEngine().addSmoothParticle(MathUtils.getRandomPointOnCircumference(cellLocOnModel, 4f), particleVel, randomSize1, 0.8f, MathUtils.getRandomNumberInRange(0.35f, 0.5f), new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
					}
				}
			}
			cells.clear();
			cellsAboveAverage.clear();
			cellsBelowAverage.clear();
		}
	}

	//calculates total armor of the ship
	public static float getAverageArmorPerCell(ArmorGridAPI grid) {
		float armor = 0f;
		for (int ix = 0; ix < grid.getGrid().length; ix++) {
			for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
				armor += grid.getArmorValue(ix, iy);
			}
		}
		return armor / (grid.getGrid().length * grid.getGrid()[0].length);
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEffectiveArmorBonus().unmodify(id);
//		stats.getMaxArmorDamageReduction().unmodify(id);
		stats.getEmpDamageTakenMult().unmodify(id);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Armor hardness and emp resistance doubled", false);
		}
		if (index == 1) {
			return new StatusData("Rebalancing armor", false);
		}
		return null;
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return getAverageArmorPerCell(ship.getArmorGrid()) > ship.getArmorGrid().getMaxArmorInCell() / 10;
	}
}



