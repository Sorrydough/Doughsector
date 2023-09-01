package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;

@SuppressWarnings("unused")
public class sd_destroyersupportglowDecoScript implements EveryFrameWeaponEffectPlugin {
    final float rotationSpeed = 180f; //get this from the weapon spec when alex adds the ability to do so
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        if (ship.getSystem().isActive()) {
            weapon.setForceFireOneFrame(true);

            float shipFacing = ship.getFacing();
            float currentAngle = weapon.getCurrAngle();
            float desiredAngle = shipFacing + normalizeAngle(currentAngle);

            desiredAngle += rotationSpeed * amount;
            weapon.setFacing(desiredAngle - shipFacing);
        }
    }

    public static float normalizeAngle(float angleDeg) {
        return (angleDeg % 360f + 360f) % 360f;
    }
}
