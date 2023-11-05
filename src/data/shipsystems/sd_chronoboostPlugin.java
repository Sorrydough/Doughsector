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
    final String id = this.toString();
    final float TIME_PERCENT = 50;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        float effectLevel = ship.getSystem().getEffectLevel();

        targetStats.getTimeMult().modifyPercent(id, TIME_PERCENT * effectLevel);
        targetStats.getFluxDissipation().modifyMult(id, 1 - effectLevel);

        if (effectLevel == 0) {
            targetStats.getTimeMult().unmodifyPercent(id);
            targetStats.getFluxDissipation().unmodifyMult(id);
            Global.getCombatEngine().removePlugin(this);
        }
    }
}
