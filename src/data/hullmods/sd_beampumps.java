package data.hullmods;

import com.fs.starfarer.api.Global;
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
import org.lazywizard.lazylib.*;
import org.lwjgl.util.vector.Vector2f;

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
			if (isMixedBeam(weapon)) {
				weapon.ensureClonedSpec();
				weapon.getSpec().setBeamSpeed(1000000);
			}
		}
	}
	public static class BeamPumpsListener implements WeaponBaseRangeModifier {
		final ArrayList<WeaponAPI> weapons = new ArrayList<>();
		final ShipAPI ship;
		public BeamPumpsListener(ShipAPI ship) {
            this.ship = ship;
			for (WeaponAPI weapon : ship.getAllWeapons())
				if (isMixedBeam(weapon))
					weapons.add(weapon);
        }
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
	static boolean isMixedBeam(WeaponAPI weapon) {
		switch (weapon.getType()) {
			case STATION_MODULE:
			case LAUNCH_BAY:
			case DECORATIVE:
			case SYSTEM:
				return false;
		}
		return weapon.isBeam() && weapon.getSlot().getWeaponType() != WeaponType.ENERGY;
	}
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltip.addPara("Beam weapons' damage dealt and flux generated are both increased by "+ MODIFIER +"%%." , 5f,
				Misc.getHighlightColor(), MODIFIER +"%");
		tooltip.addPara("Reduces the base range of beams past "+ RANGE_THRESHOLD +" range by " + Math.round((1 - RANGE_MULT) * 100) + "%%.", 2f,
				Misc.getHighlightColor(), String.valueOf(RANGE_THRESHOLD), Math.round((1 - RANGE_MULT) * 100) + "%");
		tooltip.addPara("Beams in non-energy slots also recieve instant travel speed.", 5f);
	}
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
}