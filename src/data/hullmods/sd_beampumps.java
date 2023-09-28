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
	final int RANGE_THRESHOLD = 500;
	final float RANGE_MULT = 0.50f;
	final int DAMAGE_BONUS = 50;
	final int FLUX_GENERATED_PENALTY = 25;
	final int BURST_BEAM_PENALTY = 25;
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS);
		stats.getBeamWeaponFluxCostMult().modifyPercent(id, FLUX_GENERATED_PENALTY);
	}
	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new BeamPumpsRangeMod());
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.isBurstBeam())
				weapon.setRefireDelay(weapon.getRefireDelay() * (1 + (float) BURST_BEAM_PENALTY / 100));
		}
	}
	public class BeamPumpsRangeMod implements WeaponBaseRangeModifier {
		public BeamPumpsRangeMod() {}
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
		tooltip.addPara("Beams deal "+ DAMAGE_BONUS + "%% more damage.", 5f, Misc.getHighlightColor(), DAMAGE_BONUS + "%");
		tooltip.addPara("Beams generate "+ FLUX_GENERATED_PENALTY + "%% more flux.", 5f, Misc.getHighlightColor(), (int)FLUX_GENERATED_PENALTY + "%");
		tooltip.addPara("Reduces the portion of the range of beams that is above "+ RANGE_THRESHOLD +" range by " + Math.round((1f - RANGE_MULT) * 100f) + "%%. The base range is affected.", 2f, Misc.getHighlightColor(), String.valueOf(RANGE_THRESHOLD), Math.round((1f - RANGE_MULT) * 100f) + "%");
		tooltip.addPara("Increases the refire delay of burst beams by "+ BURST_BEAM_PENALTY +"%%.",2f, Misc.getHighlightColor(), BURST_BEAM_PENALTY +"%");
		tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, 10f);
		tooltip.addPara("The base range is reduced, therefore modifiers will apply to the reduced base value.", 2f);
	}
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
	@Override
	public boolean showInRefitScreenModPickerFor(ShipAPI ship) { return ship.getHullSpec().getManufacturer().equals("???"); }
}