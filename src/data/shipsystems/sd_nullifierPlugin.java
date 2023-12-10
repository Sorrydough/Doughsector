package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import data.scripts.sd_util;

import java.util.List;
import java.util.Map;

public class sd_nullifierPlugin extends BaseEveryFrameCombatPlugin { // todo: make a util for checking whether target is valid
    final ShipAPI target, ship;
    final MutableShipStatsAPI targetStats;
    final DynamicStatsAPI targetDynamic;
    final CombatEngineAPI engine;
    final String id;
    public sd_nullifierPlugin(ShipAPI ship, ShipAPI target) {
        this.ship = ship;
        this.target = target;
        this.targetStats = target.getMutableStats();
        this.targetDynamic = targetStats.getDynamic();
        this.id = ship.getId();
        this.engine = Global.getCombatEngine();
    }
    final float FLUX_PER_TIMEFLOW = 2, PPT_MULT = 1.25f;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused())
            return;
        if (!sd_nullifier.isTargetValid(ship, target))
            ship.getSystem().deactivate();

        float effectLevel = ship.getSystem().getEffectLevel();

        // make target's PPT and CR degrade 25% faster
        targetStats.getCRLossPerSecondPercent().modifyMult(id, PPT_MULT * effectLevel);
        targetStats.getPeakCRDuration().modifyFlat(id, -((PPT_MULT - 1) * amount * effectLevel));

        // 1. Track how many instances of the system are being applied to the target
        targetDynamic.getMod("sd_nullifier").modifyFlat(id, effectLevel);
        float numApplied = targetDynamic.getMod("sd_nullifier").getFlatBonuses().size();

        // 2. Figure out the biggest effectLevel that's being applied to the target
        float nullificationLevel = 0;
        for (Map.Entry<String, MutableStat.StatMod> nullifier : targetDynamic.getMod("sd_nullifier").getFlatBonuses().entrySet())
            if (nullifier.getValue().getValue() > nullificationLevel)
                nullificationLevel = nullifier.getValue().getValue();

        // 3. Apply our timeflow change according to the biggest effectLevel
        targetStats.getTimeMult().unmodify("sd_nullifier");
        float baseTimeflow = target.getMutableStats().getTimeMult().getModifiedValue();
        targetDynamic.getMod("sd_baseTimeMult").modifyFlat("sd_nullifier", baseTimeflow); // storing this on the target so I can access it in the AI script
        float modificationMult = (1 + ((baseTimeflow - 1) * (1 - nullificationLevel))) / baseTimeflow; // puretilt wrote this math for me
        targetStats.getTimeMult().modifyMult("sd_nullifier", modificationMult);
        if (target == engine.getPlayerShip())
            engine.getTimeMult().modifyMult("sd_nullifier", modificationMult);

        // 4. Slightly reduce target shield arc
//        targetStats.getShieldArcBonus().modifyMult("sd_nullifier", (PPT_MULT - 1) * nullificationLevel);
//        sd_util.modifyShieldArc(target, Math.max(45, targetStats.getShieldArcBonus().computeEffective(target.getHullSpec().getShieldSpec().getArc())), effectLevel);

        // 5. Generate flux
        float modificationPercent = Math.abs(1 - baseTimeflow) * 100;
        ship.getFluxTracker().increaseFlux((modificationPercent * FLUX_PER_TIMEFLOW * effectLevel * amount) / numApplied, true); // divide by numApplied to share flux load

        if (effectLevel == 0) { // cleanup
            targetDynamic.getMod("sd_nullifier").unmodifyFlat(id);
            targetStats.getCRLossPerSecondPercent().unmodifyMult(id);
            if (targetDynamic.getMod("sd_nullifier").getFlatBonuses().isEmpty()) {
                targetDynamic.getMod("sd_baseTimeMult").unmodify("sd_nullifier");
//                targetStats.getShieldArcBonus().unmodifyMult("sd_nullifier");
                targetStats.getTimeMult().unmodifyFlat("sd_nullifier");
                engine.getTimeMult().unmodifyFlat("sd_nullifier");
            }
            Global.getCombatEngine().removePlugin(this);
        }
    }
}