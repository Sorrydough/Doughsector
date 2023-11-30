package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;

import java.util.List;

public class sd_nullifierPlugin extends BaseEveryFrameCombatPlugin {
    final ShipAPI target;
    final ShipAPI ship;
    final MutableShipStatsAPI targetStats;
    DynamicStatsAPI targetDynamic;
    String id;
    public sd_nullifierPlugin(ShipAPI ship, ShipAPI target) {
        this.ship = ship;
        this.target = target;
        this.targetStats = target.getMutableStats();
        this.targetDynamic = targetStats.getDynamic();
        this.id = ship.getId();
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
    float timeMod = 0;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (target.isPhased())
            ship.getSystem().deactivate();
        targetDynamic.getMod("sd_nullifier").modifyFlat(id, 1);
        float numApplied = targetDynamic.getMod("sd_nullifier").computeEffective(0);
        float effectLevel = ship.getSystem().getEffectLevel();
        //make target's PPT and Cr degrade 25% faster
        targetStats.getCRLossPerSecondPercent().modifyMult(id, PPT_MULT * effectLevel);
        targetStats.getPeakCRDuration().modifyFlat(id, -((PPT_MULT - 1) * amount * effectLevel));
        //correct the target's timeflow to 100%,
        float targetTimeflow = targetStats.getTimeMult().getModifiedValue();
        float timeflowOver = (targetTimeflow - 1) * 100;
        if (timeflowOver != 0) {
            timeMod = timeflowOver / numApplied;
            targetStats.getTimeMult().modifyFlat(id, timeMod * effectLevel);
        }
        ship.getFluxTracker().increaseFlux(timeMod * FLUX_PER_TIMEFLOW * amount * effectLevel, true);

        if (effectLevel == 0) {
            targetDynamic.getMod("sd_nullifier").unmodifyFlat(id);
            targetStats.getCRLossPerSecondPercent().unmodifyMult(id);
            targetStats.getTimeMult().unmodifyFlat(id);
            Global.getCombatEngine().removePlugin(this);
        }
    }
}