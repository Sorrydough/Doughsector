package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class sd_chronoboostPlugin extends BaseEveryFrameCombatPlugin {
    final ShipAPI target;
    final ShipAPI ship;
    final MutableShipStatsAPI targetStats;
    public sd_chronoboostPlugin(ShipAPI ship, ShipAPI target) {
        this.ship = ship;
        this.target = target;
        this.targetStats = target.getMutableStats();
    }
    final Map<ShipAPI.HullSize, Float> PPT_DRAIN = new HashMap<>(); {
        PPT_DRAIN.put(ShipAPI.HullSize.FRIGATE, 0.5f);
        PPT_DRAIN.put(ShipAPI.HullSize.DESTROYER, 1f);
        PPT_DRAIN.put(ShipAPI.HullSize.CRUISER, 1.5f);
        PPT_DRAIN.put(ShipAPI.HullSize.CAPITAL_SHIP, 2f);
    }
    final IntervalUtil TIMER = new IntervalUtil(1, 1);
    final String id = this.toString();
    final float CR_DEGRADE_PERCENT = 25;
    final float DURATION = 10;
    float totalPeakTimeLost = 0;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        float effectLevel = ship.getSystem().getEffectLevel();

        targetStats.getCRLossPerSecondPercent().modifyPercent(id, CR_DEGRADE_PERCENT * effectLevel);

        if (Global.getCombatEngine().isPaused())
            return;
        TIMER.advance(amount);
        if (TIMER.intervalElapsed()) {
            if (target.getPeakTimeRemaining() > PPT_DRAIN.get(target.getHullSize())) {
                totalPeakTimeLost += PPT_DRAIN.get(target.getHullSize()) * effectLevel;
                targetStats.getPeakCRDuration().modifyFlat(id, -totalPeakTimeLost);
            }
            if (effectLevel == 0) {
                target.getMutableStats().getCRLossPerSecondPercent().unmodifyPercent(id);
                Global.getCombatEngine().removePlugin(this);
            }
        }
    }
}
