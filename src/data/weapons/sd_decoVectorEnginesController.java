package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.loading.specs.EngineSlot;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class sd_decoVectorEnginesController implements EveryFrameWeaponEffectPlugin {

    //SCRIPT BY PURRTILT

    boolean doOnce = true;
    ArrayList<decoEngine> engines = new ArrayList<>();

    final Map<ShipAPI.HullSize, Float> strafeMulti = new HashMap<>();

    {
        strafeMulti.put(ShipAPI.HullSize.FIGHTER, 1f);
        strafeMulti.put(ShipAPI.HullSize.FRIGATE, 1f);
        strafeMulti.put(ShipAPI.HullSize.DESTROYER, 0.75f);
        strafeMulti.put(ShipAPI.HullSize.CRUISER, 0.5f);
        strafeMulti.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.25f);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip().getOwner() == -1) return;
//        if (true) return;
        ShipAPI ship = weapon.getShip();
        if (doOnce) {
            //Global.getLogger(ptes_decoEnginesController.class).info(Misc.getAngleInDegrees(new Vector2f(1,0))+ "/" +Misc.getAngleInDegrees(new Vector2f(0,1))+ "/" + MathUtils.getShortestRotation(Misc.getAngleInDegrees(new Vector2f(1,0)),Misc.getAngleInDegrees(new Vector2f(0,1))));
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (slot.getId().startsWith("THR")) {
                    ShipEngineControllerAPI.ShipEngineAPI thruster = null;
                    for (ShipEngineControllerAPI.ShipEngineAPI e : ship.getEngineController().getShipEngines()) {
                        if (MathUtils.isWithinRange(e.getLocation(), slot.computePosition(ship), 4)) {
                            thruster = e;
                            break;
                        }
                    }
                    if (thruster == null)
                        return;
                    engines.add(new decoEngine(ship, thruster));
                    //engine.addFloatingText(thruster.getLocation(), tempWeapon.getSlot().getId(), 20, Color.white, ship, 0, 0);
                }
            }
            doOnce = false;
        }

        Vector2f newVector = new Vector2f();
        if (ship.getEngineController().isAccelerating()) {
            newVector.y += 1 * ship.getAcceleration();
        }
        if (ship.getEngineController().isAcceleratingBackwards()) {
            newVector.y -= 1 * ship.getDeceleration();
        }
        if (ship.getEngineController().isStrafingLeft()) {
            newVector.x -= 1 * ship.getAcceleration() * strafeMulti.get(ship.getHullSize());
        }
        if (ship.getEngineController().isStrafingRight()) {
            newVector.x += 1 * ship.getAcceleration() * strafeMulti.get(ship.getHullSize());
        }
        if (ship.getEngineController().isDecelerating()) {
            if (ship.getVelocity().lengthSquared() > 0) {
                Vector2f normalizedVel = new Vector2f(ship.getVelocity());
                normalizedVel = Misc.normalise(normalizedVel);
                normalizedVel = VectorUtils.rotate(normalizedVel, -ship.getFacing() - 90);
                newVector.x += normalizedVel.x;
                newVector.y += normalizedVel.y;
            }
        }
        newVector.scale(-1);
        Color shift = ship.getEngineController().getFlameColorShifter().getCurr();
        float ratio = shift.getAlpha() / 255f;
        float currAngle = Misc.getAngleInDegrees(newVector);

        int turn = 0;
        if (ship.getEngineController().isTurningRight()) {
            turn++;
        }
        if (ship.getEngineController().isTurningLeft()) {
            turn--;
        }

            /*
            int Red = Math.min(255, Math.round(engineColor.getRed() * (1f - ratio) + shift.getRed() * ratio));
            int Green = Math.min(255, Math.round(engineColor.getGreen() * (1f - ratio) + shift.getGreen() * ratio));
            int Blue = Math.min(255, Math.round(engineColor.getBlue() * (1f - ratio) + shift.getBlue() * ratio));

             */
        //engine.addHitParticle(weapon.getLocation(), (Vector2f) Misc.getUnitVectorAtDegreeAngle(currAngle + ship.getFacing() - 90).scale(300f), 20, 1, amount * 10, Color.red);
        for (decoEngine e : engines) {
            float thrust = 0;

            if (!VectorUtils.isZeroVector(newVector) && Math.abs(MathUtils.getShortestRotation(e.angle, currAngle)) <= 60) {
                thrust += 1;

                //engine.addHitParticle(e.weapon.getLocation(), (Vector2f) Misc.getUnitVectorAtDegreeAngle(e.angle + ship.getFacing() - 90).scale(300f), 20, 1, amount * 10, colorToUse);
            }
            if (turn != 0 && e.turn == turn) {
                thrust += 1f;
            }


            thrust(e, MathUtils.clamp(thrust, 0.4f, 1f));
        }
    }



    private void thrust(decoEngine data, float thrust) {
        ShipAPI ship = data.ship;
        Vector2f size = new Vector2f(15, 80);
        float smooth = 0.2f;
        if (data.engine.isDisabled()) thrust = 0f;
        if (ship.getEngineController().isAccelerating()) thrust = 0f;
        //Global.getLogger(ptes_decoEnginesController.class).info(weapon.getSlot().getId());


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

        data.ship.getEngineController().setFlameLevel(data.engine.getEngineSlot(), data.previousThrust);
        //data.ship.getEngineController().forceShowAccelerating();
    }

    static class decoEngine {
        public decoEngine(ShipAPI ship, ShipEngineControllerAPI.ShipEngineAPI engine) {
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
            /*
            Global.getLogger(ptes_decoEnginesController.class).info(displacementAngle + "/" + (Math.abs(displacementAngle) - 90) + "/" + tolerance + "/" + turn);
            Global.getLogger(ptes_decoEnginesController.class).info(weapon.getSlot().getAngle() + "/" + absAngle);

             */

            EngineSlot engineslot = (EngineSlot) engine.getEngineSlot();
            Global.getLogger(sd_decoVectorEnginesController.class).info(engineslot.getAccelTimeToMaxGlow());

            engineslot.setGlowParams(engineslot.getWidth(),engineslot.getLength(),10f,1);

            sizeMulti = engine.getEngineSlot().getWidth() / 3;
        }

        ShipAPI ship;
        ShipEngineControllerAPI.ShipEngineAPI engine;
        int turn;
        float angle;
        float previousThrust = 0.4f;
        float sizeMulti;
    }
}
