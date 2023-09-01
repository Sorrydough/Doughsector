package data.shipsystems;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class sd_damperfield extends BaseShipSystemScript {
	final Map<HullSize, Float> DAMPER_MULT = new HashMap<>();
	{	//incoming damage is reduced to this %
		DAMPER_MULT.put(HullSize.FIGHTER, 0.33f);
		DAMPER_MULT.put(HullSize.FRIGATE, 0.33f);
		DAMPER_MULT.put(HullSize.DESTROYER, 0.33f);
		DAMPER_MULT.put(HullSize.CRUISER, 0.5f);
		DAMPER_MULT.put(HullSize.CAPITAL_SHIP, 0.5f);
	}
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
			return;

		ShipAPI ship = (ShipAPI) stats.getEntity();
		ship.setJitter(id, new Color(250, 235, 215, 50), effectLevel, 2, 0, 5);
		ship.setJitterUnder(id, new Color(250, 235, 215, 150), effectLevel, 25, 0, 7);

		float damperMult = DAMPER_MULT.get(stats.getVariant().getHullSize());
		stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);
		stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);
		stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);

		if (ship == Global.getCombatEngine().getPlayerShip()) {
			float percent = (1f - damperMult) * effectLevel * 100;
			Global.getCombatEngine().maintainStatusForPlayerShip(id, "graphics/icons/hullsys/damper_field.png", "Damper Field", Math.round(percent) + "% less damage taken", false);
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getHullDamageTakenMult().unmodify(id);
		stats.getArmorDamageTakenMult().unmodify(id);
		stats.getEmpDamageTakenMult().unmodify(id);
	}
}



