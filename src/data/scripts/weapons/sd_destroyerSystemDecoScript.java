package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class sd_destroyerSystemDecoScript implements EveryFrameWeaponEffectPlugin {
//    float currAngle = 0f;
//    final float rotationSpeed = 100f;
//    boolean rotatingClockwise = true;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship.getSystem().isOn()) {
            weapon.setForceFireOneFrame(true);

            // if (Global.getCombatEngine().isPaused())
            //     return;
            //
            // //I'm not commenting anything because this is secretly chatGPT code and I don't know how it works :)
            //
            // float arc = weapon.getArc();
            // float facing = weapon.getArcFacing() + (weapon.getShip() != null ? weapon.getShip().getFacing() : 0);
            //
            // if (rotatingClockwise) {
            //     currAngle += rotationSpeed * Global.getCombatEngine().getElapsedInLastFrame();
            //     if (currAngle >= arc / 2) {
            //         currAngle = arc / 2;
            //         rotatingClockwise = false;
            //     }
            // } else {
            //     currAngle -= rotationSpeed * Global.getCombatEngine().getElapsedInLastFrame();
            //     if (currAngle <= -arc / 2) {
            //         currAngle = -arc / 2;
            //         rotatingClockwise = true;
            //     }
            // }
            // float newFacing = facing + currAngle;
            // weapon.setFacing(newFacing);
        }/* else if (ship.getSystem().isChargedown()) {
            weapon.setCurrAngle(0);
        }*/
    }
}
