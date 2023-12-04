package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.loading.specs.HullVariantSpec;

import java.util.List;
import java.util.Map;

public class sd_stasisfieldPlugin extends BaseEveryFrameCombatPlugin {
    final ShipAPI target;
    final ShipAPI ship;
    final MutableShipStatsAPI targetStats;
    final DynamicStatsAPI targetDynamic;
    final CombatEngineAPI engine;
    final String id;
    public sd_stasisfieldPlugin(ShipAPI ship, ShipAPI target) {
        this.ship = ship;
        this.target = target;
        this.targetStats = target.getMutableStats();
        this.targetDynamic = targetStats.getDynamic();
        this.id = ship.getId();
        this.engine = Global.getCombatEngine();
    }
    final float STASIS_MULT_MIN = 0.25f;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused())
            return;
        if (!sd_stasisfield.isTargetValid(ship, target))
            ship.getSystem().deactivate();

        float effectLevel = ship.getSystem().getEffectLevel();

        // look at the nullifier plugin script for a play by play breakdown of how this code for target sharing works, it's mostly copied from there
        targetDynamic.getMod("sd_stasisfield").modifyFlat(id, effectLevel);
        float numApplied = targetDynamic.getMod("sd_stasisfield").getFlatBonuses().size();

        float stasisLevel = 0;
        for (Map.Entry<String, MutableStat.StatMod> stasisification : targetDynamic.getMod("sd_stasisfield").getFlatBonuses().entrySet())
            if (stasisification.getValue().getValue() > stasisLevel)
                stasisLevel = stasisification.getValue().getValue();

        float stasis_mult = STASIS_MULT_MIN + (1f - STASIS_MULT_MIN) * (1f - stasisLevel); // chatgpt wrote this

        target.fadeToColor("sd_stasisfield", sd_stasisfield.jitterUnderColor, 0.25f, 0.25f, 0.66f * stasisLevel);
        target.setJitterUnder("sd_stasisfield", sd_stasisfield.jitterUnderColor, stasisLevel, 10, 0, 10);
        target.setJitter("sd_stasisfield", sd_stasisfield.jitterColor, stasisLevel, 1, 0, 5);

        targetStats.getTimeMult().modifyMult("sd_stasisfield", stasis_mult);
        targetStats.getEmpDamageTakenMult().modifyMult("sd_stasisfield", stasis_mult);
        targetStats.getHullDamageTakenMult().modifyMult("sd_stasisfield", stasis_mult);
        targetStats.getArmorDamageTakenMult().modifyMult("sd_stasisfield", stasis_mult);
        targetStats.getShieldDamageTakenMult().modifyMult("sd_stasisfield", stasis_mult);
        targetStats.getShieldUnfoldRateMult().modifyMult("sd_stasisfield", 1 / stasis_mult); // allows the target to deploy and rotate its shields at full speed relative to global space
        targetStats.getShieldTurnRateMult().modifyMult("sd_stasisfield", 1 / stasis_mult); // ^
        targetStats.getShieldArcBonus().modifyMult("sd_stasisfield", stasis_mult); // todo: handle flux generation here and make it depend on target hullsize.

//        if (targetDynamic.getMod("sd_baseMass").getFlatBonuses().isEmpty())
//            targetDynamic.getMod("sd_baseMass").modifyFlat(id, target.getMass());
//
//        float goalMass = targetDynamic.getMod("sd_baseMass").computeEffective(0) / stasisLevel;
//        if (target.getMass() != goalMass)
//            target.setMass(goalMass);

        modifyShieldArc(target, Math.max(35, targetStats.getShieldArcBonus().computeEffective(target.getHullSpec().getShieldSpec().getArc())), stasisLevel);

        if (effectLevel == 0) { // system can be turned off at will so we have to check for effect level when unapplying
            targetDynamic.getMod("sd_stasisfield").unmodifyFlat(id);
            if (targetDynamic.getMod("sd_stasisfield").getFlatBonuses().isEmpty()) {
                targetStats.getTimeMult().unmodifyMult("sd_stasisfield");
                targetStats.getEmpDamageTakenMult().unmodifyMult("sd_stasisfield");
                targetStats.getHullDamageTakenMult().unmodifyMult("sd_stasisfield");
                targetStats.getArmorDamageTakenMult().unmodifyMult("sd_stasisfield");
                targetStats.getShieldDamageTakenMult().unmodifyMult("sd_stasisfield");
                targetStats.getShieldUnfoldRateMult().unmodifyMult("sd_stasisfield");
                targetStats.getShieldTurnRateMult().unmodifyMult("sd_stasisfield");
                targetStats.getShieldArcBonus().unmodifyMult("sd_stasisfield");
//                target.setMass(targetDynamic.getMod("sd_baseMass").computeEffective(0));
//                targetDynamic.getMod("sd_baseMass").unmodifyFlat(id);
            }
            engine.removePlugin(this);
        }
    }
    public static void modifyShieldArc(ShipAPI target, float goalShieldArc, float effectLevel) {
        // 1. If the target's shield is still unfolding, don't mess with it
        if (target.getShield() == null || target.getShield().isOff() || target.getShield().getActiveArc() < goalShieldArc)
            return;
        // 2. Calculate how quickly the target's shield should be modified
        // Let's say target arc is 90, current arc is 180
        // when effectLevel is 1, arc should be set to 90
        // when effectLevel is 0.5, arc should be set to (135 = 180-90/2)
        target.getShield().setActiveArc(Math.max(goalShieldArc, target.getShield().getActiveArc() - goalShieldArc / (1 / effectLevel)));
    }
}
