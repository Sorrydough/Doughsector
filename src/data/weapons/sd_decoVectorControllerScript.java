package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.loading.specs.EngineSlot;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class sd_decoVectorControllerScript implements EveryFrameWeaponEffectPlugin {
    //SCRIPT BY PURRTILT
    boolean runOnce = true;
    decoEngine target = null;
    final Map<ShipAPI.HullSize, Float> strafeMulti = new HashMap<>(); {
        strafeMulti.put(ShipAPI.HullSize.FIGHTER, 1f);
        strafeMulti.put(ShipAPI.HullSize.FRIGATE, 1f);
        strafeMulti.put(ShipAPI.HullSize.DESTROYER, 0.75f);
        strafeMulti.put(ShipAPI.HullSize.CRUISER, 0.5f);
        strafeMulti.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.25f);
    }
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip().getOwner() == -1)
            return;

        ShipAPI ship = weapon.getShip();
        ShipEngineControllerAPI controller = ship.getEngineController();

        if (runOnce) {
            if (weapon.getSlot().getId().startsWith("THR")) {
                ShipEngineAPI thruster = null;
                for (ShipEngineAPI e : controller.getShipEngines()) {
                    if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 2)) {
                        thruster = e;
                        break;
                    }
                }
                if (thruster != null)
                    target = new decoEngine(ship, thruster);
            }
            runOnce = false;
        }

        if (target == null)
            return;

        Vector2f newVector = new Vector2f();
        if (controller.isAccelerating())
            newVector.y += 1 * ship.getAcceleration();
        if (controller.isAcceleratingBackwards())
            newVector.y -= 1 * ship.getDeceleration();
        if (controller.isStrafingLeft())
            newVector.x -= 1 * ship.getAcceleration() * strafeMulti.get(ship.getHullSize());
        if (controller.isStrafingRight())
            newVector.x += 1 * ship.getAcceleration() * strafeMulti.get(ship.getHullSize());
        if (controller.isDecelerating())
            if (ship.getVelocity().lengthSquared() > 0) {
                Vector2f normalizedVel = new Vector2f(ship.getVelocity());
                normalizedVel = Misc.normalise(normalizedVel);
                normalizedVel = VectorUtils.rotate(normalizedVel, -ship.getFacing() - 90);
                newVector.x += normalizedVel.x;
                newVector.y += normalizedVel.y;
            }
        newVector.scale(-1);
        Color shift = controller.getFlameColorShifter().getCurr();
        float ratio = shift.getAlpha() / 255f;
        float currAngle = Misc.getAngleInDegrees(newVector);

        int turn = 0;
        if (controller.isTurningRight())
            turn++;
        if (controller.isTurningLeft())
            turn--;

        float thrust = 0;
        if (!VectorUtils.isZeroVector(newVector) && Math.abs(MathUtils.getShortestRotation(target.angle, currAngle)) <= 60)
            thrust += 1;
        if (turn != 0 && target.turn == turn)
            thrust += 1f;
        thrust(target, MathUtils.clamp(thrust, 0.4f, 1f));
    }

    private void thrust(decoEngine data, float thrust) {
        ShipAPI ship = data.ship;
        Vector2f size = new Vector2f(15, 80);
        float smooth = 0.2f;
        if (data.engine.isDisabled())
            thrust = 0f;
        if (ship.getEngineController().isAccelerating())
            thrust = 0f;
        //target angle
        float length = thrust;
        float amount = Global.getCombatEngine().getElapsedInLastFrame();

        //thrust is reduced while the engine isn't facing the target angle, then smoothed
        /*
        length -= data.previousThrust;
        length *= smooth;
        length += data.previousThrust;
        data.previousThrust = length;*/
        EngineSlot engineslot = (EngineSlot) data.engine.getEngineSlot();

        if (data.previousThrust < thrust) {
            data.previousThrust += engineslot.getAccelTimeToMaxGlow() * amount;
            data.previousThrust = Math.min(thrust , data.previousThrust);
        } else if (data.previousThrust > thrust){
            data.previousThrust -= engineslot.getAccelTimeToMaxGlow() * amount;
            data.previousThrust = Math.max(thrust , data.previousThrust);
        }
        //data.ship.getEngineController().forceShowAccelerating();
        data.ship.getEngineController().setFlameLevel(data.engine.getEngineSlot(), data.previousThrust);
    }

    static class decoEngine {
        public decoEngine(ShipAPI ship, ShipEngineAPI engine) {
            this.ship = ship;
            this.engine = engine;
            angle = engine.getEngineSlot().getAngle() + 90;
            Vector2f loc = engine.getEngineSlot().computePosition(new Vector2f(), 0);
            float leverRatio = loc.length() / ship.getCollisionRadius();
            float absAngle = engine.getEngineSlot().getAngle();
            VectorUtils.getAngle(new Vector2f(1,0),new Vector2f(0,1));
            Vector2f pushDirection = Misc.getUnitVectorAtDegreeAngle(absAngle);
            float displacementAngle = MathUtils.getShortestRotation(Misc.getAngleInDegrees(loc), Misc.getAngleInDegrees(pushDirection));

            float tolerance = 25 * leverRatio;
            if (leverRatio > 0.25){
                if (displacementAngle > 0) {
                    if (Math.abs(displacementAngle - 90) < tolerance) {
                        turn = 1;
                    }
                } else {
                    if (Math.abs(Math.abs(displacementAngle) - 90) < tolerance) {
                        turn = -1;
                    }
                }
            }
            EngineSlot engineslot = (EngineSlot) engine.getEngineSlot();
            engineslot.setGlowParams(engineslot.getWidth(),engineslot.getLength(),10f,1);
            sizeMulti = engine.getEngineSlot().getWidth() / 3;
        }

        ShipAPI ship;
        ShipEngineAPI engine;
        int turn;
        float angle;
        float previousThrust = 0.4f;
        float sizeMulti;
    }
}
