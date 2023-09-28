package data.hullmods;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class sd_loadline extends BaseHullMod {
    final float PERFORMANCE_PENALTY = 10;
    final Color color = new Color(255,255,255,255);
    String urmum = "urmum";
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) { //TODO: REWORK THIS ENTIRE HULLMOD, USE ADVANCEABLE LISTENER
        stats.getPeakCRDuration().modifyFlat(id, 240f);
        stats.getCRLossPerSecondPercent().modifyMult(id, 0.75f);
        stats.getOverloadTimeMod().modifyMult(id, 0.75f);
        stats.getVentRateMult().modifyPercent(id, 33f);
        stats.getWeaponHealthBonus().modifyMult(id, 1.25f);
        stats.getEngineHealthBonus().modifyMult(id, 1.25f);
        stats.getWeaponMalfunctionChance().modifyMult(id, 0.5f);
        stats.getEngineMalfunctionChance().modifyMult(id, 0.5f);
        stats.getCriticalMalfunctionChance().modifyMult(id, 0.5f);
    }
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;
        MutableShipStatsAPI stats = ship.getMutableStats();
        ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
        if (ship.areSignificantEnemiesInRange()) {
            ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);
            float baseDiss = stats.getFluxDissipation().getBaseValue();
            float totalDiss = stats.getFluxDissipation().getModifiedValue();
            float penaltyMult = 0.67f;
            float newDiss = ((totalDiss - baseDiss) * penaltyMult) + baseDiss;
            float multiplier = newDiss / totalDiss;
            stats.getFluxDissipation().modifyMult(urmum, multiplier);
            stats.getMaxSpeed().modifyMult(urmum, 1f - PERFORMANCE_PENALTY * 0.01f);
            stats.getAcceleration().modifyMult(urmum, 1f - PERFORMANCE_PENALTY * 0.01f);
            stats.getDeceleration().modifyMult(urmum, 1f - PERFORMANCE_PENALTY * 0.01f);
            stats.getTurnAcceleration().modifyMult(urmum, 1f - PERFORMANCE_PENALTY * 0.01f);
            stats.getMaxTurnRate().modifyMult(urmum, 1f - PERFORMANCE_PENALTY * 0.01f);
            stats.getFighterRefitTimeMult().modifyMult(urmum, 1f - PERFORMANCE_PENALTY * 0.01f);
            stats.getSystemRegenBonus().modifyMult(urmum, 1f - PERFORMANCE_PENALTY * 0.01f);
		} else {
            stats.getFluxDissipation().unmodifyMult(urmum);
            stats.getMaxSpeed().unmodifyMult(urmum);
            stats.getAcceleration().unmodifyMult(urmum);
            stats.getDeceleration().unmodifyMult(urmum);
            stats.getTurnAcceleration().unmodifyMult(urmum);
            stats.getMaxTurnRate().unmodifyMult(urmum);
            stats.getFighterRefitTimeMult().unmodifyMult(urmum);
            stats.getSystemRegenBonus().unmodifyMult(urmum);
        }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Systems are restricted to operate below their usual wartime output, dramatically improving the ship’s endurance.", 5);
        tooltip.addSectionHeading("Details", Alignment.MID, 10);
        tooltip.addPara("Increases peak operating time by 240 seconds.", 5, Misc.getHighlightColor(), "240 seconds");
        tooltip.addPara("Reduces CR degradation rate by 25%%.", 5, Misc.getHighlightColor(), "25%");
        tooltip.addPara("Reduces overload duration by 25%%.", 5, Misc.getHighlightColor(), "25%");
        tooltip.addPara("Improves module durability by 25%%.", 5, Misc.getHighlightColor(), "25%");
        tooltip.addPara("Reduces the risk of malfunctions by 50%%.", 5, Misc.getHighlightColor(), "50%");
        tooltip.addPara("Improves active venting by 33%%.", 5, Misc.getHighlightColor(), "33%");

        tooltip.addPara("When performance is not steady:", 10, Misc.getNegativeHighlightColor(), "steady");
        tooltip.addPara("- Weapon rate of fire, system regeneration rate, top speed, maneuverability, shield unfold rate, and fighter replacement rate are all reduced by 10%%.", 5, Misc.getNegativeHighlightColor(), "10%");
        tooltip.addPara("- Flux dissipation above the hull's baseline spec is clamped to 67%% effectiveness.", 5, Misc.getNegativeHighlightColor(), "clamped to 67%");
        tooltip.addPara("This hullmod cannot be built in.", 10);

        //tooltip.addSectionHeading("Incompatibilities", Alignment.MID, pad);
        //TooltipMakerAPI text = tooltip.beginImageWithText("graphics/dough/icons/tooltip/hullmod_incompatible.png", 40);
        //text.addPara("This modification is incompatible with the following hullmods:", pad);
        //text.addPara("- Hardened Subsystems", Misc.getNegativeHighlightColor(), padS);
        //text.addPara("- Safety Overrides", Misc.getNegativeHighlightColor(), padS);
//        if (Global.getSettings().getModManager().isModEnabled("apex_design")) {
//            text.addPara("- Nanolaminate Plating", Misc.getNegativeHighlightColor(), 0f);
//            text.addPara("- Cryocooled Armor Lattice", Misc.getNegativeHighlightColor(), 0f);
//        }
        //tooltip.addImageWithText(pad);
    }
    //Shamelessly "borrowed" dme's hullmod blocking code because it does the exact thing that I need
    final Set<String> BLOCKED_HULLMODS = new HashSet<>(2);
    {
        BLOCKED_HULLMODS.add("safetyoverrides");
        BLOCKED_HULLMODS.add("hardenedsubsystems");
    }
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        for (String tmp : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(tmp))
            {
                ship.getVariant().removeMod(tmp);
            }
        }
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }
    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) { return ship.getHullSpec().getManufacturer().equals("???"); }
}
