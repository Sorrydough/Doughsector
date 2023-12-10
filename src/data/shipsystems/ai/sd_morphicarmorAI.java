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
            if (!sd_util.canUseSystemThisFrame(ship) || sd_morphicarmor.isArmorGridDestroyed(grid))
                return;

            float desirePos = 0;
            float desireNeg = 0;

            // We want the system on if our armor grid isn't balanced, otherwise just turn it off immediately cuz it's doing nothing for ya tbqh
            if (!sd_morphicarmor.isArmorGridBalanced(grid)) {
                desirePos += 150;
                // Calculate how much flux we're going to generate by using our system and scale negative desire accordingly
                float fluxToRebalance = getFluxToRebalance(grid);
                desireNeg -= ((fluxToRebalance / ship.getMaxFlux()) * ship.getFluxLevel()) * 100;
                // We want the system off if:
                // 1. Our flux level is too high
                desireNeg -= ship.getFluxLevel() * 100;
                // 2. We could dissipate hardflux
                if (ship.getShield() != null && ship.getShield().isOff() && sd_util.isNumberWithinRange(ship.getHardFluxLevel(), ship.getFluxLevel(), 1))
                    desireNeg -= ship.getHardFluxLevel() * 150;
            }
            else desireNeg -= 50;

            sd_util.activateSystem(ship, "sd_morphicarmor", desirePos, desireNeg, debug);
        }
    }
    static float getFluxToRebalance(ArmorGridAPI grid) {
        float flux = 0;
        for (int ix = 0; ix < grid.getGrid().length; ix++) {
            for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
                flux += Math.abs(grid.getArmorValue(ix, iy) - sd_morphicarmor.getAverageArmorPerCell(grid));
            }
        }
        return flux * sd_morphicarmor.FLUX_PER_ARMOR;
    }
}
