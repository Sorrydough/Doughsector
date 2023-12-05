package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_beampumps extends BaseHullMod {
	static final int RANGE_THRESHOLD = 500;
	static final float RANGE_MULT = 0.5f;
	final int FLUX_PENALTY = 25;
	final int DAMAGE_BONUS = 50;
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS);
		stats.getBeamWeaponFluxCostMult().modifyPercent(id, FLUX_PENALTY);
	}
	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new BeamPumpsRangeMod());
	}
	public static class BeamPumpsRangeMod implements WeaponBaseRangeModifier {
		public BeamPumpsRangeMod() {}
		public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			return 0;
		}
		public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}
		public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) { // copied alex's HSA code lol
			if (weapon.isBeam()) {
				float range = weapon.getSpec().getMaxRange();
				if (range < RANGE_THRESHOLD) return 0;

				float past = range - RANGE_THRESHOLD;
				float penalty = past * (1f - RANGE_MULT);
				return -penalty;
			}
			return 0f;
		}
	}
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltip.addPara("Beam weapons deal "+ DAMAGE_BONUS +"%% more damage and are "+ FLUX_PENALTY +"%% more flux intensive to run." , 5f,
				Misc.getHighlightColor(), DAMAGE_BONUS +"%", FLUX_PENALTY +"%");
		tooltip.addPara("Reduces the base range of beams past "+ RANGE_THRESHOLD +" range by " + Math.round((1 - RANGE_MULT) * 100) + "%%.", 2f,
				Misc.getHighlightColor(), String.valueOf(RANGE_THRESHOLD), Math.round((1 - RANGE_MULT) * 100) + "%");
	}
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
}