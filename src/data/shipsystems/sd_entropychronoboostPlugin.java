package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class sd_entropychronoboostPlugin extends BaseEveryFrameCombatPlugin {
    final ShipAPI target;
    final ShipAPI ship;
    public sd_entropychronoboostPlugin(ShipAPI ship, ShipAPI target) {
        this.ship = ship;
        this.target = target;
    }
    final Map<ShipAPI.HullSize, Integer> AVERAGE_PPT = new HashMap<>(); {
        AVERAGE_PPT.put(ShipAPI.HullSize.FRIGATE, 240);
        AVERAGE_PPT.put(ShipAPI.HullSize.DESTROYER, 360);
        AVERAGE_PPT.put(ShipAPI.HullSize.CRUISER, 480);
        AVERAGE_PPT.put(ShipAPI.HullSize.CAPITAL_SHIP, 600);
    }
    final IntervalUtil interval = new IntervalUtil(1, 1);
    final float CR_DEGRADE_PERCENT = 25;
    final float DURATION = 5;
    boolean doOnce = true;
    float time = 0;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        String id = this.toString();
        


        if (doOnce) {
            target.getMutableStats().getCRLossPerSecondPercent().modifyPercent(id, CR_DEGRADE_PERCENT);
            target.setTimeDeployed(target.getFullTimeDeployed() + 0.5f);


            doOnce = false;
        }







        if (Global.getCombatEngine().isPaused())
            return;
        interval.advance(amount);
        if (interval.intervalElapsed()) {



            time += 1;
            if (time >= DURATION) {
                target.getMutableStats().getCRLossPerSecondPercent().unmodifyPercent(id);
                engine.removePlugin(this);
            }
        }
    }
}
