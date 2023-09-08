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
import org.lazywizard.console.Console;

import java.util.HashMap;
import java.util.Map;

public class sd_highscatteramp extends BaseHullMod {

	final float RANGE_THRESHOLD = 500;
	final float RANGE_MULT = 0.50f;
	final float DAMAGE_BONUS_PERCENT = 50;
	final float SMOD_MODIFIER = 15;

	final Map<WeaponAPI.WeaponSize, Float> BONUS_MULT = new HashMap<>();
	{	//damage bonus for beams based on weapon size
		BONUS_MULT.put(WeaponAPI.WeaponSize.SMALL, 2f);
		BONUS_MULT.put(WeaponAPI.WeaponSize.MEDIUM, 1.5f);
		BONUS_MULT.put(WeaponAPI.WeaponSize.LARGE, 1.25f);
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().getHullMods().contains(HullMods.ADVANCEDOPTICS)) { return "Incompatible with Advanced Optics"; }
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) { return !ship.getVariant().getHullMods().contains(HullMods.ADVANCEDOPTICS); }

//	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
//		boolean sMod = isSMod(stats);
//		stats.getBeamWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS_PERCENT + (sMod ? SMOD_MODIFIER : 0));
//	}



	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new HighScatterAmpRangeMod());
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.isBeam() && !weapon.isDecorative()) {
				weapon.getDamage().setDamage(weapon.getDamage().getDamage() * BONUS_MULT.get(weapon.getSize()));
				Console.showMessage("Weapon Modified: "+ weapon.getDisplayName() +" On Ship: "+ ship.getHullSpec().getHullName());
			}
		}
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
		tooltip.addPara("Beams deal 100/50/25%% more damage by size.", 10f, Misc.getHighlightColor(), "100/50/25%");
		tooltip.addPara("Reduces the portion of the range of beam weapons that is above "+ (int)RANGE_THRESHOLD +" range by " + Math.round((1f - RANGE_MULT) * 100f) + "%%. The base range is affected.", 10f, Misc.getNegativeHighlightColor(), String.valueOf((int) RANGE_THRESHOLD), Math.round((1f - RANGE_MULT) * 100f) + "%");
		tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, 10f);
		tooltip.addPara("The base range is reduced, thus percentage and multiplicative modifiers apply to the reduced base value.", 10f);
	}
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }

}