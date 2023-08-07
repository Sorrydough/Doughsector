package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
public class sd_spallprotection extends BaseHullMod {

    final float SPALL_BONUS = 25;

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFragmentationDamageTakenMult().modifyMult(id, 1f - SPALL_BONUS * 0.01f);
        stats.getCrewLossMult().modifyMult(id, 1f - SPALL_BONUS * 0.01f);
    }
}