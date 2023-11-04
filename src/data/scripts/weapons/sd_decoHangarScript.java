package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import data.shipsystems.sd_auxforge;


public class sd_decoHangarScript implements EveryFrameWeaponEffectPlugin {
    boolean willRestoreFighters = true;
    boolean doOnce = true;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        ShipSystemAPI system = ship.getSystem();
        if (doOnce && system.isChargeup()) { // when the system turns on, track whether we're going to glow the hangar
            willRestoreFighters = sd_auxforge.willRestoreFighters(ship);
            doOnce = false;
        }
        if (system.isActive() && willRestoreFighters) // if the system is on & we're going to glow the hangar, then do so
            weapon.setForceFireOneFrame(true);
        if (!system.isActive() && !doOnce) { // if the system turns off, reset the tracker for the next activation
            doOnce = true;
        }
    }
}
