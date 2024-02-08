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
	final int DAMAGE_BONUS = 50;
	final int FLUX_PENALTY = 50;
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS);
		stats.getBeamWeaponFluxCostMult().modifyPercent(id, FLUX_PENALTY);
	}
	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (WeaponAPI weapon : ship.getAllWeapons())
			if (isMixedBeam(weapon)) {
				weapon.ensureClonedSpec();
				weapon.getSpec().setBeamSpeed(1000000);
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
		tooltip.addPara("For beam weapons, flux generated is increased by "+ FLUX_PENALTY +"%% and damage dealt is increased by "+ DAMAGE_BONUS +"%%." , 5f,
				Misc.getHighlightColor(), FLUX_PENALTY +"%", DAMAGE_BONUS +"%");
		tooltip.addPara("When mounted in non-energy slots, they also recieve instant travel speed.", 5f);
	}
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
}