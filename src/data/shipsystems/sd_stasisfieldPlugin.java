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
    final MutableShipStatsAPI targetStats;
    public sd_stasisfieldPlugin(ShipAPI ship, ShipAPI target) {
        this.ship = ship;
        this.target = target;
        this.targetStats = target.getMutableStats();
    }
    public final Color fadeColor = new Color(150,100,255, 50);
    final IntervalUtil TIMER = new IntervalUtil(0.5f, 0.5f);
    final float STASIS_MULT_MIN = 0.1f;
    final float STASIS_MULT_MAX = 1.0f;
    final String id = this.toString();
    final CombatEngineAPI engine = Global.getCombatEngine();
    float time = 0;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        float effectLevel = ship.getSystem().getEffectLevel();
        float STASIS_MULT = STASIS_MULT_MIN + (STASIS_MULT_MAX - STASIS_MULT_MIN) * (1f - effectLevel); // chatgpt wrote this

        target.fadeToColor(id, sd_stasisfield.jitterColor, 0.25f, 0.25f, 0.75f * effectLevel);
        target.setJitterUnder(id, sd_stasisfield.jitterUnderColor, effectLevel, 10, 0, 10);
        target.setJitter(id, sd_stasisfield.jitterColor, effectLevel, 1, 0, 5);

        targetStats.getTimeMult().modifyMult(id, STASIS_MULT);
        targetStats.getHullDamageTakenMult().modifyMult(id, STASIS_MULT);
        targetStats.getArmorDamageTakenMult().modifyMult(id, STASIS_MULT);
        if (target.getShield() != null) {
            targetStats.getShieldDamageTakenMult().modifyMult(id, STASIS_MULT);
            targetStats.getShieldUnfoldRateMult().modifyMult(id, 1 / STASIS_MULT); // allows the target to deploy and rotate its shields at full speed regardless of the time dilation
            targetStats.getShieldTurnRateMult().modifyMult(id, 1 / STASIS_MULT);
            targetStats.getShieldArcBonus().modifyMult(id, STASIS_MULT);
        }

        if (!ship.getSystem().isActive()) { // system can be turned off at will
            targetStats.getTimeMult().unmodifyMult(id);
            targetStats.getHullDamageTakenMult().unmodifyMult(id);
            targetStats.getArmorDamageTakenMult().unmodifyMult(id);
            if (target.getShield() != null) {
                targetStats.getShieldDamageTakenMult().unmodifyMult(id);
                targetStats.getShieldUnfoldRateMult().unmodifyMult(id);
                targetStats.getShieldTurnRateMult().unmodifyMult(id);
                targetStats.getShieldArcBonus().unmodifyMult(id);
            }
            engine.removePlugin(this);
        }

        if (Global.getCombatEngine().isPaused())
            return;
        TIMER.advance(amount);
        if (TIMER.intervalElapsed()) { // using a timer to "pulse" the enhanced jitter
            time += 0.5f;
            float jitterExtra = effectLevel *= time;
            target.setJitterUnder(id, sd_stasisfield.jitterUnderColor, effectLevel, 15, 0, 15 * jitterExtra);
            target.setJitter(id, sd_stasisfield.jitterColor, effectLevel, 5, 0, 5 * jitterExtra);
        }
    }
}
