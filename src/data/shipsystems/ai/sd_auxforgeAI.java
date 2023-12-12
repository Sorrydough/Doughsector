package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.sd_util;
import data.shipsystems.sd_auxforge;
import org.lwjgl.util.vector.Vector2f;

import static data.shipsystems.sd_auxforge.*;

public class sd_auxforgeAI implements ShipSystemAIScript {
    final IntervalUtil interval = new IntervalUtil(0.5f, 1f);
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
            if (!canBeUsed(ship))
                return;
            float desirePos = 0;
            float desireNeg = 0;
            // We want to use the system if:
            // 1. We have a lot of charges saved up
            desirePos += (Float.class.cast(system.getAmmo() / system.getMaxAmmo())) * 100;
            // 2. Our missiles are depleted
            boolean willRestoreFighters = sd_auxforge.willRestoreFighters(ship);
            WeaponAPI missile = getEmptiestMissile(ship);
            if (canReloadMissile(missile) && !willRestoreFighters)
                desirePos += (1 - Float.class.cast(missile.getAmmo() / missile.getMaxAmmo())) * 100;
            // 3. Our fighters are depleted
            float replacement = ship.getSharedFighterReplacementRate();
            if (replacement < 0.9 && willRestoreFighters)
                desirePos += Float.class.cast((1 - (replacement - 0.3)) * 100);

            // We don't want to use the system if:
            // 1. Our flux level is too high
            desireNeg -= (ship.getHardFluxLevel() + ship.getFluxLevel()) * 100;
            // 2. We're under attack and flux is escalating
            if (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))
                desireNeg -= ship.getFluxLevel() * 50;

            sd_util.activateSystem(ship, "sd_auxforge", desirePos, desireNeg, false);
        }
    }
}
