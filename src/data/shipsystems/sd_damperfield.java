package data.shipsystems;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class sd_damperfield extends BaseShipSystemScript {
	public static Map<HullSize, Float> DAMPER_MULT = new HashMap<>();
	static {	//incoming damage is reduced to this %
		DAMPER_MULT.put(HullSize.FIGHTER, 0.33f);
		DAMPER_MULT.put(HullSize.FRIGATE, 0.33f);
		DAMPER_MULT.put(HullSize.DESTROYER, 0.33f);
		DAMPER_MULT.put(HullSize.CRUISER, 0.5f);
		DAMPER_MULT.put(HullSize.CAPITAL_SHIP, 0.5f);
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship.getSystem().isActive()) {
			ship.fadeToColor(id, new Color(75, 75, 75, 255), 0.1f, 0.1f, effectLevel);
			ship.getEngineController().fadeToOtherColor(id, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), effectLevel, 0.75f * effectLevel);
			ship.setJitterUnder(id, new Color(250,235,215, 255), effectLevel, 10, 0f, 15f);

			float damperMult = DAMPER_MULT.get(HullSize.FRIGATE);
			if (stats.getVariant() != null) {
				damperMult = DAMPER_MULT.get(stats.getVariant().getHullSize());
			}
			stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);
			stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);
			stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);

			if (ship == Global.getCombatEngine().getPlayerShip()) {
				ShipSystemAPI system = getDamper(ship);
				if (system != null) {
					float percent = (1f - damperMult) * effectLevel * 100;
					Global.getCombatEngine().maintainStatusForPlayerShip(id, system.getSpecAPI().getIconSpriteName(), system.getDisplayName(), Math.round(percent) + "% less damage taken", false);
				}
			}

		} else {
			stats.getHullDamageTakenMult().unmodify(id);
			stats.getArmorDamageTakenMult().unmodify(id);
			stats.getEmpDamageTakenMult().unmodify(id);
		}
	}

	public ShipSystemAPI getDamper(ShipAPI ship) {
		ShipSystemAPI system = ship.getPhaseCloak();
		if (system != null && system.getId().equals("damper")) return system;
		if (system != null && system.getId().equals("damper_omega")) return system;
		if (system != null && system.getId().equals("sd_damperfield")) return system;
		if (system != null && system.getSpecAPI() != null && system.getSpecAPI().hasTag(Tags.SYSTEM_USES_DAMPER_FIELD_AI)) return system;
		return ship.getSystem();
	}
}



