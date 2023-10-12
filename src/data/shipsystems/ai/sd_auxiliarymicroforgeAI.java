package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.sd_auxiliarymicroforge;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class sd_auxiliarymicroforgeAI implements ShipSystemAIScript {
    final IntervalUtil interval = new IntervalUtil(0.5f, 1f);
    final boolean debug = false;
    ShipAPI ship;
    ShipSystemAPI system;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }
    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            if (!AIUtils.canUseSystemThisFrame(ship))
                return;
            float desirePos = 0;
            float desireNeg = 0;
            // We want to use the system if:
            // 1. We have a lot of charges saved up
            desirePos += ((float) system.getAmmo() / system.getMaxAmmo()) * 100;
            // 2. Our missiles are depleted
            boolean willRestoreFighters = sd_auxiliarymicroforge.willRestoreFighters(ship);
            WeaponAPI missile = sd_auxiliarymicroforge.getEmptiestMissile(ship);
            if (sd_auxiliarymicroforge.canReloadMissile(missile) && !willRestoreFighters)
                desirePos += (1 - (float) missile.getAmmo() / missile.getMaxAmmo()) * 100;
            // 3. Our fighters are depleted
            float replacement = ship.getSharedFighterReplacementRate();
            if (replacement < 0.9 && willRestoreFighters)
                desirePos += (1 - (replacement - 0.3)) * 100;

            // We don't want to use the system if:
            // 1. Our flux level is too high
            desireNeg -= (ship.getHardFluxLevel() + ship.getFluxLevel()) * 100;
            // 2. We're under attack and flux is escalating
            if (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))
                desireNeg -= ship.getHardFluxLevel() * 50;

            float desireTotal = desirePos + desireNeg;
            if (debug)
                Console.showMessage("Desire Total: "+ desireTotal +" Desire Pos: "+ desirePos +" Desire Neg: "+ desireNeg);
            if (desireTotal > 100)
                ship.useSystem();
        }
    }
}
