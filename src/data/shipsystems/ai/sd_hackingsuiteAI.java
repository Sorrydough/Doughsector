package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.sd_hackingsuite;

import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.sd_util;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class sd_hackingsuiteAI implements ShipSystemAIScript {
    final List<String> uselessSystems = new ArrayList<>(); {
        uselessSystems.add("flarelauncher");
    }
    List<ShipAPI> targets = new ArrayList<>();
    final IntervalUtil intervalShort = new IntervalUtil(0.01f, 0.01f);
    final IntervalUtil intervalLong = new IntervalUtil(0.5f, 1f);
    final boolean debug = false;
    float systemRange = 0;
    ShipAPI ship;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }
    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (!AIUtils.canUseSystemThisFrame(ship))
            return;
        // this stuff is on a slower interval cuz it's expensive
        intervalLong.advance(amount);
        if (intervalLong.intervalElapsed()) {
            // calculate our system range, kinda important to have
            if (systemRange == 0)
                systemRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship) + ship.getCollisionRadius());
            // keep track of nearby targets
            for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, systemRange))
                if (!targets.contains(enemy) && sd_hackingsuite.isTargetValid(ship, enemy))
                    targets.add(enemy);
            if (!targets.isEmpty())
                for (ShipAPI enemy : new ArrayList<>(targets)) // doing some shenanigans to bypass a concurrent modification exception
                    if (MathUtils.getDistance(ship, enemy) > systemRange)
                        targets.remove(enemy);
        }
        // no point going any further if we have no targets ))))
        if (targets.isEmpty())
            return;
        intervalShort.advance(amount);
        if (intervalShort.intervalElapsed()) {
            float desirePos = 0;
            float desireNeg = 0;
            // We want to use the system if:
            // 1. A ship within range is using its system
            for (ShipAPI enemy : targets) {
                if (enemy.getSystem().isOn()) {
                    ship.setShipTarget(enemy);
                    desirePos += 150;
                }
            }
            // We don't want to use our system if:
            // 1. Our flux level is too high
            desireNeg -= (ship.getFluxLevel() * 100) * (0.5 + ship.getSystem().getFluxPerUse() / ship.getMaxFlux()); // this math is more fragile than you'd think
            // 2. The enemy's system isn't worthwhile disabling
            for (String system : uselessSystems) {
                if (Objects.equals(target.getSystem().getId(), system))
                    desireNeg -= 100;
            }
            float desireTotal = desirePos + desireNeg;
            if (debug)
                Global.getCombatEngine().addFloatingText(ship.getLocation(), "Desire Total: "+ desireTotal +" Desire Pos: "+ desirePos +" Desire Neg: "+ desireNeg, 20, Color.CYAN, ship, 5, 5);
            if (desireTotal >= 100)
                ship.useSystem();
        }
    }
}
