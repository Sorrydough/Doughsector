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
	final int FLUX_GENERATED_PENALTY = 25;
	final int RANGE_THRESHOLD = 500;
	final int DAMAGE_BONUS = 50;
	final float ROF_PENALTY = 0.75f;
	final float RANGE_MULT = 0.5f;
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS);
		stats.getBeamWeaponFluxCostMult().modifyPercent(id, FLUX_GENERATED_PENALTY);
		stats.getEnergyRoFMult().modifyMult(id, ROF_PENALTY);
	}
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltip.addPara("Beams deal "+ DAMAGE_BONUS + "%% more damage.", 5f,
				Misc.getHighlightColor(), DAMAGE_BONUS + "%");
		tooltip.addPara("Beams generate "+ FLUX_GENERATED_PENALTY + "%% more flux.", 5f,
				Misc.getHighlightColor(), FLUX_GENERATED_PENALTY + "%");
		tooltip.addPara("Reduces the portion of the range of beams that is above "+ RANGE_THRESHOLD +" range by " + Math.round((1 - RANGE_MULT) * 100) + "%%.", 2f,
				Misc.getHighlightColor(), String.valueOf(RANGE_THRESHOLD), Math.round((1 - RANGE_MULT) * 100) + "%");
		tooltip.addPara("Reduces energy weapons' rate of fire by "+ Math.round((1 - ROF_PENALTY) * 100) +"%%.", 2f,
				Misc.getHighlightColor(), Math.round((1 - ROF_PENALTY) * 100) +"%");
		tooltip.addSectionHeading("Modifiers will apply to the reduced base range.", Alignment.MID, 10f);
	}
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
}