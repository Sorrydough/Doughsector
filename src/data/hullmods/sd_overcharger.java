package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.Misc;

import java.util.HashMap;
import java.util.Map;
public class sd_overcharger extends BaseHullMod {

	public static float MIN_RANGE = 400, MAX_RANGE = 1000;

	public static Map<WeaponAPI.WeaponSize, Float> DAMAGE_BONUS_MULT = new HashMap<>();
	static {    //outgoing damage boosted by this mult, has math done to it and becomes a % later
		DAMAGE_BONUS_MULT.put(WeaponAPI.WeaponSize.SMALL, 1f);
		DAMAGE_BONUS_MULT.put(WeaponAPI.WeaponSize.MEDIUM, 0.5f);
		DAMAGE_BONUS_MULT.put(WeaponAPI.WeaponSize.LARGE, 0.25f);
	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.removeListenerOfClass(BeamAmpDamageDealtMod.class);
		ship.addListener(new BeamAmpDamageDealtMod(ship));
	}

	public static class BeamAmpDamageDealtMod implements DamageDealtModifier, AdvanceableListener {
		protected ShipAPI ship;

		public BeamAmpDamageDealtMod(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {

		}

		public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {

			Vector2f from = null;
			WeaponAPI weapon = null;
			if (param instanceof BeamAPI) {
				from = ((BeamAPI)param).getFrom();
				weapon = ((BeamAPI)param).getWeapon();
			} else {
				return null;
			}

			if (weapon == null || ship == null) return null;

			float damageMult = DAMAGE_BONUS_MULT.get(weapon.getSize());

			float dist = Misc.getDistance(from, point);
			float f = 1f;
			if (dist > MAX_RANGE) {
				f = 0f;
			} else if (dist > MIN_RANGE) {
				f = 1f - (dist - MIN_RANGE) / (MAX_RANGE - MIN_RANGE);
			}
			if (f < 0) f = 0;
			if (f > 1) f = 1;

			String id = "beamoc_dam_mod";
			damage.getModifier().modifyPercent(id, (damageMult * f) * 100f);
			return id;
		}
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	//float RANGE_THRESHOLD = 1, SMOD_FLUX_PENALTY = 1, RANGE_MULT = 1;

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltip.addPara("Beam weapons do an additional 100/50/25%% damage by weapon size at "+ (int)MIN_RANGE +" range, diminishing to no bonus at "+ (int)MAX_RANGE +" range.", 10f, Misc.getHighlightColor(), "100/50/25%", String.valueOf((int)MIN_RANGE), String.valueOf((int)MAX_RANGE));
//		if (isForModSpec) {
//			tooltip.addSectionHeading("S-mod bonus", Misc.getGrayColor(), Misc.setAlpha(Misc.scaleColorOnly(Misc.getGrayColor(), 0.4f), 175), Alignment.MID, 10f);
//			tooltip.addPara("Beams generate "+ (int)SMOD_FLUX_PENALTY +"%% more flux.", 10f, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), (int)SMOD_FLUX_PENALTY +"%");
//		} else if (ship.getVariant().getSMods().contains("high_scatter_amp")) {
//			tooltip.addSectionHeading("S-mod bonus", Misc.getNegativeHighlightColor(),  Misc.setAlpha(Misc.scaleColorOnly(Misc.getNegativeHighlightColor(), 0.4f), 175), Alignment.MID, 10f);
//			tooltip.addPara("Beams generate "+ (int)SMOD_FLUX_PENALTY +"%% more flux.", 10f, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), (int)SMOD_FLUX_PENALTY +"%");
//		} else if (Global.getSettings().getBoolean("BuiltInSMod") && ship.getHullSpec().isBuiltInMod("high_scatter_amp")) {
//			tooltip.addSectionHeading("Built-in bonus", Misc.getNegativeHighlightColor(),  Misc.setAlpha(Misc.scaleColorOnly(Misc.getNegativeHighlightColor(), 0.4f), 175), Alignment.MID, 10f);
//			tooltip.addPara("Beams generate "+ (int)SMOD_FLUX_PENALTY +"%% more flux.", 10f, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), (int)SMOD_FLUX_PENALTY +"%");
//		} else {
//			tooltip.addSectionHeading("S-mod bonus", Misc.getGrayColor(), Misc.setAlpha(Misc.scaleColorOnly(Misc.getGrayColor(), 0.4f), 175), Alignment.MID, 10f);
//			tooltip.addPara("Beams generate "+ (int)SMOD_FLUX_PENALTY +"%% more flux.", 10f, Misc.getGrayColor(), Misc.getHighlightColor(), (int)SMOD_FLUX_PENALTY +"%");
//		}
	}

//	@Override
//	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
//		return false;
//	}
//
//	@Override
//	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
//		tooltip.addPara("Beam weapons deal hard flux damage to shields.", 10f, Misc.getHighlightColor(), "hard flux");
//		if (isForModSpec) {
//			tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, 10f);
//			tooltip.addPara("The base range is reduced, thus percentage and multiplicative modifiers - such as from Integrated Targeting Unit, " + "skills, or similar sources - apply to the reduced base value.", 10f);
//			tooltip.addPara("Reduces the portion of the range of beam weapons that is above "+ (int)RANGE_THRESHOLD +" units by "+ Math.round((1f - RANGE_MULT) * 100f) +"%%. The base range is affected.", 10f, Misc.getHighlightColor(), String.valueOf((int) RANGE_THRESHOLD), Math.round((1f - RANGE_MULT) * 100f) + "%");
//			tooltip.addSectionHeading("S-mod penalty", Misc.getGrayColor(), Misc.setAlpha(Misc.scaleColorOnly(Misc.getGrayColor(), 0.4f), 175), Alignment.MID, 10f);
//			tooltip.addPara("Beams generate "+ (int)SMOD_FLUX_PENALTY +"%% more flux.", 10f, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), (int)SMOD_FLUX_PENALTY +"%");
//		} else if (ship.getVariant().getSMods().contains("high_scatter_amp")) {
//			tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, 10f);
//			tooltip.addPara("The base range is reduced, thus percentage and multiplicative modifiers - such as from Integrated Targeting Unit, " + "skills, or similar sources - apply to the reduced base value.", 10f);
//			tooltip.addPara("Reduces the portion of the range of beam weapons that is above "+ (int)RANGE_THRESHOLD +" units by "+ Math.round((1f - RANGE_MULT) * 100f) +"%%. The base range is affected.", 10f, Misc.getHighlightColor(), String.valueOf((int) RANGE_THRESHOLD), Math.round((1f - RANGE_MULT) * 100f) + "%");
//			tooltip.addSectionHeading("S-mod penalty", Misc.getNegativeHighlightColor(),  Misc.setAlpha(Misc.scaleColorOnly(Misc.getNegativeHighlightColor(), 0.4f), 175), Alignment.MID, 10f);
//			tooltip.addPara("Beams generate "+ (int)SMOD_FLUX_PENALTY +"%% more flux.", 10f, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), (int)SMOD_FLUX_PENALTY +"%");
//		} else if (Global.getSettings().getBoolean("BuiltInSMod") && ship.getHullSpec().isBuiltInMod("high_scatter_amp")) {
//			tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, 10f);
//			tooltip.addPara("The base range is reduced, thus percentage and multiplicative modifiers - such as from Integrated Targeting Unit, " + "skills, or similar sources - apply to the reduced base value.", 10f);
//			tooltip.addPara("Reduces the portion of the range of beam weapons that is above "+ (int)RANGE_THRESHOLD +" units by "+ Math.round((1f - RANGE_MULT) * 100f) +"%%. The base range is affected.", 10f, Misc.getHighlightColor(), String.valueOf((int) RANGE_THRESHOLD), Math.round((1f - RANGE_MULT) * 100f) + "%");
//			tooltip.addSectionHeading("Built-in penalty", Misc.getNegativeHighlightColor(),  Misc.setAlpha(Misc.scaleColorOnly(Misc.getNegativeHighlightColor(), 0.4f), 175), Alignment.MID, 10f);
//			tooltip.addPara("Beams generate "+ (int)SMOD_FLUX_PENALTY +"%% more flux.", 10f, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), (int)SMOD_FLUX_PENALTY +"%");
//		} else {
//			tooltip.addPara("Reduces the portion of the range of beam weapons that is above "+ (int)RANGE_THRESHOLD +" units by "+ Math.round((1f - RANGE_MULT) * 100f) +"%%. The base range is affected.", 10f, Misc.getHighlightColor(), String.valueOf((int) RANGE_THRESHOLD), Math.round((1f - RANGE_MULT) * 100f) + "%");
//			tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, 10f);
//			tooltip.addPara("The base range is reduced, thus percentage and multiplicative modifiers - such as from Integrated Targeting Unit, " + "skills, or similar sources - apply to the reduced base value.", 10f);
//			tooltip.addSectionHeading("S-mod penalty", Misc.getGrayColor(), Misc.setAlpha(Misc.scaleColorOnly(Misc.getGrayColor(), 0.4f), 175), Alignment.MID, 10f);
//			tooltip.addPara("Beams generate "+ (int)SMOD_FLUX_PENALTY +"%% more flux.", 10f, Misc.getGrayColor(), Misc.getHighlightColor(), (int)SMOD_FLUX_PENALTY +"%");
//		}
//	}
}









