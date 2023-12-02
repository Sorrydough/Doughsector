package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.Console;

import java.util.List;

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

    //1. CR degrades 25% faster. DONE
    //2. PPT degrades 25% faster. DONE

    //3. Track how many instances of this system are applied to the target. DONE

    //4. Calculate the target's total timeflow factor. DONE
    //5. Reduce the target's timeflow down to 100%. DONE??

    //6. Generate flux on the ship using this system according to how much it's correcting the target's timeflow. DONE
    //7. Add some baseline flux generation to the CSV for the PPT effect or whatever

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
        targetDynamic.getMod("sd_nullifier").modifyFlat(id, 1);
        float numApplied = targetDynamic.getMod("sd_nullifier").computeEffective(0);

        // 2. Apply our timeflow change
        targetStats.getTimeMult().unmodify("sd_nullifier");
        float baseTimeflow = target.getMutableStats().getTimeMult().getModifiedValue();
        target.getMutableStats().getTimeMult().modifyMult("sd_nullifier", 1 / baseTimeflow); // todo: make this depend on effectlevel
//        if (target == engine.getPlayerShip()) {
//            engine.getTimeMult().unmodifyFlat("sd_nullifier");
//            float engineBaseTimeflow = engine.getTimeMult().getModifiedValue();
//            engine.getTimeMult().modifyMult("sd_nullifier", 1 / engineBaseTimeflow);
//        }

        // 3. Generate flux
        float difference = Math.abs(1 - baseTimeflow);
        ship.getFluxTracker().increaseFlux(difference * FLUX_PER_TIMEFLOW * amount * effectLevel * 100, true);

        // debug
        interval.advance(amount);
        if (interval.intervalElapsed())
            Console.showMessage("Difference: "+ difference +" Target Timeflow: "+ targetStats.getTimeMult().getModifiedValue());

        if (effectLevel == 0) { // cleanup
            targetDynamic.getMod("sd_nullifier").unmodifyFlat(id);
            targetStats.getCRLossPerSecondPercent().unmodifyMult(id);
            targetStats.getTimeMult().unmodifyFlat("sd_nullifier");
            engine.getTimeMult().unmodifyFlat("sd_nullifier");
            Global.getCombatEngine().removePlugin(this);
        }
    }
//    public static float getNullifierBaseTimeflow(ShipAPI target) {
//
//
//        return 0;
//    }
}