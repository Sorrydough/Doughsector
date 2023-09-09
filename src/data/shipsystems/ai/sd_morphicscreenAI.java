package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags.*;

public class sd_morphicscreenAI implements ShipSystemAIScript {

    ShipAPI ship;
    CombatEngineAPI engine;
    ShipwideAIFlags flags;
    ShipSystemAPI system;
    float desire;
    final IntervalUtil interval = new IntervalUtil(0.5f, 1f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            //Utilities we need:
            //1. Calculate the maximum hit that the ship's armor can reduce

            //2. We need to know whether damage is incoming from too many directions




            //We want the system on if:
            //1. We can flicker the incoming damage

            //2. Incoming damage from multiple directions





            //3. Mines are buttfucking us
            if (ship.getAIFlags().hasFlag(HAS_INCOMING_DAMAGE)) {
                for (MissileAPI missile : AIUtils.getNearbyEnemyMissiles(ship, 500)) {
                    if (!ship.getShield().isWithinArc(missile.getLocation()) && missile.isMine()) {
                        desire += 100;
                    }
                }
            }

            //4. Our armor grid is dramatically imbalanced



            //Temp stopgap: If we need help, activate the system
            if (ship.getAIFlags().hasFlag(NEEDS_HELP)) {
                desire += 100;
            }


            if (desire >= 100 && !ship.getSystem().isOn())
                ship.useSystem();
            if (desire <= 0 && ship.getSystem().isOn())
                ship.useSystem();
            desire = 0;
        }
    }
}
