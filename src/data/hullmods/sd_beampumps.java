package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.sd_util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class sd_beampumps extends BaseHullMod {
	static final int RANGE_THRESHOLD = 500;
	static final float RANGE_MULT = 0.5f;
	final int MODIFIER = 50;
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamWeaponDamageMult().modifyPercent(id, MODIFIER);
		stats.getBeamWeaponFluxCostMult().modifyPercent(id, MODIFIER);
	}
	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new BeamPumpsListener(ship));
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			switch (weapon.getType()) {
				case STATION_MODULE:
				case LAUNCH_BAY:
				case DECORATIVE:
				case BUILT_IN:
				case SYSTEM:
				case ENERGY:
					continue;
			}
			if (isMixedBeam(weapon)) {
				weapon.ensureClonedSpec();
				weapon.getSpec().setBeamSpeed(1000000);
			}
		}
	}
	public static class BeamPumpsListener implements AdvanceableListener, WeaponBaseRangeModifier {
		final ArrayList<WeaponAPI> weapons = new ArrayList<>();
		final ShipAPI ship;
		public BeamPumpsListener(ShipAPI ship) {
            this.ship = ship;
			for (WeaponAPI weapon : ship.getAllWeapons())
				if (isMixedBeam(weapon))
					weapons.add(weapon);
        }
		final HashMap<BeamAPI, Float> beamsWithTime = new HashMap<>();
		final Map<WeaponAPI.WeaponSize, Integer> REQUIRED_TIME = new HashMap<>(); {
			REQUIRED_TIME.put(WeaponAPI.WeaponSize.SMALL, 8);
			REQUIRED_TIME.put(WeaponAPI.WeaponSize.MEDIUM, 6);
			REQUIRED_TIME.put(WeaponAPI.WeaponSize.LARGE, 2);
		}
        @Override
		public void advance(float amount) {
			for (WeaponAPI weapon : weapons) {
				for (BeamAPI beam : weapon.getBeams()) {
					if (beam.didDamageThisFrame())
						beamsWithTime.put(beam, amount + beamsWithTime.get(beam));
					if (beamsWithTime.get(beam) >= REQUIRED_TIME.get(beam.getWeapon().getSize())) {
						sd_util.emitMote(ship, beam.getWeapon(), true);
						beamsWithTime.remove(beam);
					}
				}
			}
		}
		public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			return 0;
		}
		public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}
		public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) { // copied alex's HSA code lol
			if (weapon.isBeam() && weapon.getSlot().getWeaponType() == WeaponType.ENERGY) {
				float range = weapon.getSpec().getMaxRange();
				if (range < RANGE_THRESHOLD) return 0;

				float past = range - RANGE_THRESHOLD;
				float penalty = past * (1f - RANGE_MULT);
				return -penalty;
			}
			return 0f;
		}
	}
	static boolean isMixedBeam(WeaponAPI weapon) {
		return weapon.isBeam() && weapon.getSlot().getWeaponType() != WeaponType.ENERGY;
	}
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltip.addPara("Beam weapons' damage dealt and flux generated are both increased by "+ MODIFIER +"%%." , 5f,
				Misc.getHighlightColor(), MODIFIER +"%");
		tooltip.addPara("Reduces the base range of beams past "+ RANGE_THRESHOLD +" range by " + Math.round((1 - RANGE_MULT) * 100) + "%%.", 2f,
				Misc.getHighlightColor(), String.valueOf(RANGE_THRESHOLD), Math.round((1 - RANGE_MULT) * 100) + "%");
		tooltip.addPara("Beams in non-energy slots also recieve instant travel speed and a special effect.", 5f,
				Misc.getHighlightColor(), "special effect");
	}
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
}