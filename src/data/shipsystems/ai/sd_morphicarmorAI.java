package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScriptAdvanced;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.sd_morphicarmor;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.sd_util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class sd_morphicarmorAI implements ShipSystemAIScript {
    final IntervalUtil interval = new IntervalUtil(0.5f, 1f);
    final boolean debug = false;
    List<sd_util.FutureHit> predictedWeaponHits = new ArrayList<>();
    List<sd_util.FutureHit> incomingProjectiles = new ArrayList<>();
    ShipAPI ship;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) { this.ship = ship; }
    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            ArmorGridAPI grid = ship.getArmorGrid();
            // if any of these is the case then the system is definitely off and we don't want to turn it on, so we can return to save cpu time
            if (!AIUtils.canUseSystemThisFrame(ship) || sd_morphicarmor.getAverageArmorPerCell(grid) <= grid.getMaxArmorInCell() * sd_morphicarmor.DESTROYED_THRESHOLD)
                return;

            float desirePos = 0; // todo: add AI behavior to use the system to block emp damage
            float desireNeg = 0; // todo: add a way to check whether incoming damage is gonna fuck us up (aka damper field logic)

            // calculate all potential incoming damage
//            float unfoldTime = 0;
//            if (ship.getShield() != null)
//                unfoldTime = ship.getShield().getUnfoldTime() * 1.1f;
//            predictedWeaponHits = sd_util.generatePredictedWeaponHits(ship, ship.getLocation(), unfoldTime);
//            incomingProjectiles = sd_util.incomingProjectileHits(ship, ship.getLocation());
//            List<sd_util.FutureHit> combinedHits = new ArrayList<>();
//            combinedHits.addAll(predictedWeaponHits);
//            combinedHits.addAll(incomingProjectiles);

            // We want the system on if our armor grid isn't balanced, otherwise just turn it off immediately cuz it's doing nothing for ya tbqh
            if (ship.getFluxLevel() < 0.95f && !sd_morphicarmor.isArmorGridBalanced(grid)) {
                desirePos += 150;
                // We want the system off if:
                // 1. Our flux level is too high
                desireNeg -= (ship.getHardFluxLevel() + ship.getFluxLevel()) * 100;
                // 2. We could dissipate hardflux
                if (ship.getShield() != null && ship.getShield().isOff() && sd_util.isNumberWithinRange(ship.getHardFluxLevel(), ship.getFluxLevel(), 1))
                    desireNeg -= ship.getHardFluxLevel() * 100;
            }
            else desireNeg -= 50;

            sd_util.activateSystem(ship, "sd_morphicarmor", desirePos, desireNeg, debug);
        }
    }
}
