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
    float timeMod = 0;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused())
            return;
        if (!sd_nullifier.isTargetValid(ship, target))
            ship.getSystem().deactivate();

        float effectLevel = ship.getSystem().getEffectLevel();

        // make target's PPT and Cr degrade 25% faster
        targetStats.getCRLossPerSecondPercent().modifyMult(id, PPT_MULT * effectLevel);
        targetStats.getPeakCRDuration().modifyFlat(id, -((PPT_MULT - 1) * amount * effectLevel));

        // initialize some info for having multiple instances of this system work together
        float totalModification = targetDynamic.getMod("sd_nullifier").computeEffective(0); // calculates how much the timeflow is being modified by any instances of our ships
        float numApplied = targetDynamic.getMod("sd_nullifier").getFlatBonuses().size(); // number of ships attempting to modify the target's timeflow
        if (!targetDynamic.getMod("sd_nullifier").getFlatBonuses().containsKey(id))
            numApplied += 1; // this may start out at 0 when we haven't applied our effect yet, so we need to add one to it to prevent a divide by 0

        // correct the target's timeflow to 100%, accounting for how many other ships with this system are also trying to help

        // ok so let's say enemy has 1.2 timeflow
        // it's not modified yet, so timeflowBeforeMod is 1.2
        // therefore necessary mod is 0.2

        // let's say it's 1.2 with 3 ships working on it
        // therefore necessary mod is 0.2 / 3 = 0.0666

        // let's say it's 0.8 with 2 ships working on it
        // necessary mod = (1 - 0.8) / 2 = 0.1

        // we have two ships each reducing timeflow by flat 0.2
        // so it's -0.4 in the modifier, down to 1.0
        // this therefore has to be 1.0 + -1 * -0.4 = 1.4

        float timeflowBeforeMod = targetStats.getTimeMult().getModifiedValue() - totalModification;
        // timeflow before mod: 2.5. after mod: 1. In totalModification: -1.5.
        // timeflow before mod: 0.8. after mod: 1. In totalModification: 0.2.


        if (timeflowBeforeMod != 1) {
            float necessaryMod = (1 - timeflowBeforeMod) / numApplied;
//            if (timeflowBeforeMod > 1)  // if target's timeflow is above 1, then we're going to subtract when we modify it
//                necessaryMod *= -1;

            timeMod = necessaryMod;
            targetDynamic.getMod("sd_nullifier").modifyFlat(id, timeMod);
            targetStats.getTimeMult().modifyFlat(id, timeMod);
            if (targetStats.getTimeMult().getModifiedValue() > 2) {
                float stat = targetStats.getTimeMult().getMultStatMod("sd_nullifier").getValue();
                float timeflow = targetStats.getTimeMult().getModifiedValue();
                Console.showMessage("Time Mod: " + timeMod + " Target Timeflow: " + timeflow);
            }
            if (target == engine.getPlayerShip())
                engine.getTimeMult().modifyFlat(id, timeMod);
        }

        interval.advance(amount);
        if (interval.intervalElapsed())
            Console.showMessage("Time Mod: "+ timeMod +" Target Timeflow: "+ targetStats.getTimeMult().getModifiedValue());

        ship.getFluxTracker().increaseFlux(timeMod * FLUX_PER_TIMEFLOW * amount * effectLevel * 100, true);

        if (effectLevel == 0) { // cleanup
            targetDynamic.getMod("sd_nullifier").unmodifyFlat(id);
            targetStats.getCRLossPerSecondPercent().unmodifyMult(id);
            targetStats.getTimeMult().unmodifyFlat(id);
            engine.getTimeMult().unmodifyFlat(id);
            Global.getCombatEngine().removePlugin(this);
        }
    }
}