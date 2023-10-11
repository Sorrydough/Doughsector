package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;

@SuppressWarnings("unused")
public class sd_destroyersupportglowDecoScript implements EveryFrameWeaponEffectPlugin {
    final float rotationSpeed = 180f; //get this from the weapon spec when alex adds the ability to do so
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (engine.isPaused() || ship == null)
            return;

        if (ship.getSystem().isActive()) {
            weapon.setForceFireOneFrame(true);

            float shipFacing = ship.getFacing();
            float currentAngle = weapon.getCurrAngle();
            float desiredAngle = shipFacing + normalizeAngle(currentAngle);

            desiredAngle += rotationSpeed * amount;
            weapon.setFacing(desiredAngle - shipFacing);
        }
        // TODO: ADD SOME CODE HERE TO MAKE THE WEAPON CONTINUE FIRE FOR THE DURATION OF THE PLUGIN AFTER DISENGAGING




    }

    public static float normalizeAngle(float angleDeg) {
        return (angleDeg % 360f + 360f) % 360f;
    }
}
