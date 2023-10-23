package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class sd_stasisfieldPlugin extends BaseEveryFrameCombatPlugin {
    final ShipAPI target;
    final ShipAPI ship;
    public sd_stasisfieldPlugin(ShipAPI ship, ShipAPI target) {
        this.ship = ship;
        this.target = target;
    }
    public final Color fadeColor = new Color(150,100,255, 50);
    final IntervalUtil TIMER = new IntervalUtil(0.5f, 0.5f);
    final float STASIS_MULT_MIN = 0.1f;
    final float STASIS_MULT_MAX = 1.0f;
    boolean doOnce = true;
    float time = 0;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipSystemAPI system = ship.getSystem();
        MutableShipStatsAPI targetStats = target.getMutableStats();
        String id = this.toString();

        float DURATION = system.getChargeUpDur() + system.getChargeActiveDur() + system.getChargeDownDur();
        float effectLevel = system.getEffectLevel();
        target.fadeToColor(id, sd_stasisfield.jitterColor, 0.25f, 0.25f, 0.75f * effectLevel);
        target.setJitterUnder(id, sd_stasisfield.jitterUnderColor, effectLevel, 10, 0, 10);
        target.setJitter(id, sd_stasisfield.jitterColor, effectLevel, 1, 0, 5);

        if (target.getShield() != null)
            target.getShield().toggleOff();
        float STASIS_MULT = STASIS_MULT_MIN + (STASIS_MULT_MAX - STASIS_MULT_MIN) * (1.0f - effectLevel); // chatgpt wrote this
        targetStats.getTimeMult().modifyMult(id, STASIS_MULT);
        targetStats.getHullDamageTakenMult().modifyMult(id, STASIS_MULT);
        targetStats.getArmorDamageTakenMult().modifyMult(id, STASIS_MULT);

        if (Global.getCombatEngine().isPaused())
            return;
        TIMER.advance(amount);
        if (TIMER.intervalElapsed()) {
            float jitterExtra = effectLevel *= time;
            target.setJitterUnder(id, sd_stasisfield.jitterUnderColor, effectLevel, 15, 0, 15 * jitterExtra);
            target.setJitter(id, sd_stasisfield.jitterColor, effectLevel, 5, 0, 5 * jitterExtra);
            time += 0.5;
            if (time >= DURATION || !ship.getSystem().isActive()) { // checking for the system being off because it can be turned off arbitrarily
                targetStats.getTimeMult().unmodifyMult(id);
                targetStats.getHullDamageTakenMult().unmodifyMult(id);
                targetStats.getArmorDamageTakenMult().unmodifyMult(id);
                targetStats.getShieldDamageTakenMult().unmodifyMult(id);
                engine.removePlugin(this);
            }
        }
    }
}
