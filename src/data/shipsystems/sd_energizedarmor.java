package data.shipsystems;

import java.awt.Color;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

public class sd_energizedarmor extends BaseShipSystemScript {
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		ship.setJitter(id, new Color(250, 235, 215, 50), effectLevel, 2, 0, 5);
		ship.setJitterUnder(id, new Color(250, 235, 215, 150), effectLevel, 25, 0, 7);

		stats.getEmpDamageTakenMult().modifyMult(id, 0.33f * effectLevel);
		stats.getArmorDamageTakenMult().modifyMult(id,0.33f * effectLevel);
		stats.getMaxArmorDamageReduction().modifyFlat(id, 15 * effectLevel);

		//while I could rebalance the armor grid all at once, I want it to look nice and happen only one cell per frame, so that complicates everything
		//firstly we need to calculate the average hp of the grid which is done elsewhere with the getAverageArmor function
		List<Vector2f> cellsAboveAverage = new ArrayList<>();
		List<Vector2f> cellsBelowAverage = new ArrayList<>();
		//next we create a list of cells above average, and another list of cells below average
		for (int ix = 0; ix < ship.getArmorGrid().getGrid().length; ix++) {
			for (int iy = 0; iy < ship.getArmorGrid().getGrid()[0].length; iy++) {
				float currentArmor = ship.getArmorGrid().getArmorValue(ix, iy);
				if (currentArmor > getAverageArmorPerCell(ship)) {
					cellsAboveAverage.add(new Vector2f(ix, iy));
				} else if (currentArmor < getAverageArmorPerCell(ship)) {
					cellsBelowAverage.add(new Vector2f(ix, iy));
				}
			}
		}
		//now that we have a list of cells above and below the average, we need to randomly choose one of the former and move the delta to the latter
		Vector2f cellToSubtract = cellsAboveAverage.get(new Random().nextInt(cellsAboveAverage.size()));
		Vector2f cellToAdd = cellsBelowAverage.get(new Random().nextInt(cellsBelowAverage.size()));

		//find the amount we need to subtract from the cell - this is a function of the amount needed vs. the amount the cell can provide
		float amountNeededToTransfer = ship.getArmorGrid().getMaxArmorInCell() - ship.getArmorGrid().getArmorValue((int)cellToAdd.x, (int)cellToAdd.y);
		//float amountAbleToTransfer =
		//ship.getArmorGrid().setArmorValue((int)cellToAdd.x, (int)cellToAdd.y, );




	}

	//calculates total armor of the ship
	private float getAverageArmorPerCell(ShipAPI ship) {
		float armor = 0f;
		for (int ix = 0; ix < ship.getArmorGrid().getGrid().length; ix++) {
			for (int iy = 0; iy < ship.getArmorGrid().getGrid()[0].length; iy++) {
				armor += ship.getArmorGrid().getArmorValue(ix, iy);
			}
		}
		return armor / (ship.getArmorGrid().getGrid().length * ship.getArmorGrid().getGrid()[0].length);
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEffectiveArmorBonus().unmodify(id);
		stats.getMaxArmorDamageReduction().unmodify(id);
		stats.getEmpDamageTakenMult().unmodify(id);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("15%% increased maximum damage reduced by armor", false);
		}
		if (index == 1) {
			return new StatusData("Armor hardness and emp resistance tripled", false);
		}
		if (index == 2) {
			return new StatusData("Rebalancing armor", false);
		}
		return null;
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return getAverageArmorPerCell(ship) > ship.getArmorGrid().getMaxArmorInCell() / 10;
	}
}



