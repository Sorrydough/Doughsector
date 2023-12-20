package data.weapons;

import com.fs.starfarer.api.combat.*;

public class sd_decoSystemWeaponScript implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getShip().getSystem().isOn())
            weapon.setForceFireOneFrame(true);
    }
}
