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

public class sd_highscatteramp extends BaseHullMod {

	final float RANGE_THRESHOLD = 500;
	final float RANGE_MULT = 0.50f;
	final float DAMAGE_BONUS_PERCENT = 50;
	final float FLUX_GENERATED_PERCENT = 25;
	final float SMOD_MODIFIER = 15;

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().getHullMods().contains(HullMods.ADVANCEDOPTICS)) { return "Incompatible with Advanced Optics"; }
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) { return !ship.getVariant().getHullMods().contains(HullMods.ADVANCEDOPTICS); }

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		boolean sMod = isSMod(stats);
		stats.getBeamWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS_PERCENT + (sMod ? SMOD_MODIFIER : 0));
		stats.getBeamWeaponFluxCostMult().modifyPercent(id, FLUX_GENERATED_PERCENT);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new HighScatterAmpRangeMod());
	}

	public String getSModDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
		if (index == 0) return Math.round(SMOD_MODIFIER) + "%";
		return null;
	}
	public class HighScatterAmpRangeMod implements WeaponBaseRangeModifier {
		public HighScatterAmpRangeMod() {}
		public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) { return 0; }
		public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) { return 1f; }
		public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
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
		tooltip.addPara("Beams deal "+ (int)DAMAGE_BONUS_PERCENT + "%% more damage.", 10f, Misc.getHighlightColor(), (int)DAMAGE_BONUS_PERCENT + "%");
		tooltip.addPara("Beams generate "+ (int)FLUX_GENERATED_PERCENT + "%% more flux.", 10f, Misc.getNegativeHighlightColor(), (int)FLUX_GENERATED_PERCENT + "%");
		tooltip.addPara("Reduces the portion of the range of beams that is above "+ (int)RANGE_THRESHOLD +" range by " + Math.round((1f - RANGE_MULT) * 100f) + "%%. The base range is affected.", 10f, Misc.getNegativeHighlightColor(), String.valueOf((int) RANGE_THRESHOLD), Math.round((1f - RANGE_MULT) * 100f) + "%");
		tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, 10f);
		tooltip.addPara("The base range is reduced, thus percentage and multiplicative modifiers apply to the reduced base value.", 10f);
	}
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }

}