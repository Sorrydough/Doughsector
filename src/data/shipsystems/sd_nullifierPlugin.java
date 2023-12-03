package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Map;

public class sd_nullifierPlugin extends BaseEveryFrameCombatPlugin {
    final ShipAPI target;
    final ShipAPI ship;
    final MutableShipStatsAPI targetStats;
    DynamicStatsAPI targetDynamic;
    String id;
    CombatEngineAPI engine;
    public sd_nullifierPlugin(ShipAPI ship, ShipAPI target) {
        this.ship = ship;
        this.target = target;
        this.targetStats = target.getMutableStats();
        this.targetDynamic = targetStats.getDynamic();
        this.id = ship.getId();
        this.engine = Global.getCombatEngine();
    }
    final float PPT_MULT = 1.25f;
    final float FLUX_PER_TIMEFLOW = 2;

    IntervalUtil interval = new IntervalUtil(1, 1);
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

        // 1. Figure out what the biggest nullification bonus is that's being applied to the target
        float nullification = 0;
        for (Map.Entry<String, MutableStat.StatMod> nullifier : targetDynamic.getMod("sd_nullifier").getFlatBonuses().entrySet()) {
            if (nullifier.getValue().getValue() > nullification)
                nullification = nullifier.getValue().getValue();
        }
        if (nullification == 0)
            return;

        // 2. Apply our timeflow change
        targetStats.getTimeMult().unmodify("sd_nullifier");
        float baseTimeflow = target.getMutableStats().getTimeMult().getModifiedValue();
        targetDynamic.getMod("sd_baseTimeMult").modifyFlat("sd_nullifier", baseTimeflow); // storing this on the target so I can access it in the AI script
        float modificationMult = 1 / (baseTimeflow * nullification);
        targetStats.getTimeMult().modifyMult("sd_nullifier", modificationMult); // todo: make a util for checking whether target is valid
        if (target == engine.getPlayerShip())
            engine.getTimeMult().modifyMult("sd_nullifier", modificationMult);

        // 3. Generate flux
        float modificationPercent = Math.abs(1 - baseTimeflow) * 100;
        ship.getFluxTracker().increaseFlux(modificationPercent * FLUX_PER_TIMEFLOW * nullification * amount, true);

        // debug
        interval.advance(amount);
        if (interval.intervalElapsed())
            Console.showMessage("Difference: "+ modificationPercent +" Target Timeflow: "+ targetStats.getTimeMult().getModifiedValue());

        if (effectLevel == 0) { // cleanup
            targetDynamic.getMod("sd_nullifier").unmodifyFlat(id);
            targetStats.getCRLossPerSecondPercent().unmodifyMult(id);
            if (targetDynamic.getMod("sd_nullifier").getFlatBonuses().isEmpty()) {
                targetDynamic.getMod("sd_baseTimeMult").unmodify("sd_nullifier");
                targetStats.getTimeMult().unmodifyFlat("sd_nullifier");
                engine.getTimeMult().unmodifyFlat("sd_nullifier");
            }
            Global.getCombatEngine().removePlugin(this);
        }
    }
//    public static float getNullifierBaseTimeflow(MutableShipStatsAPI targetStats, float nullification) {
//        targetStats.getTimeMult().unmodifyMult("sd_nullifier");
//        float baseTimeflow = targetStats.getTimeMult().getModifiedValue();
//        targetStats.getTimeMult().modifyMult("sd_nullifier", nullification / baseTimeflow);
//        return baseTimeflow;
//    }
}