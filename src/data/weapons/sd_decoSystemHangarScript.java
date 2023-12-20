package data.weapons;

import com.fs.starfarer.api.combat.*;
import data.shipsystems.sd_auxforge;


public class sd_decoSystemHangarScript implements EveryFrameWeaponEffectPlugin {
    boolean willRestoreFighters = true;
    boolean runOnce = true;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        ShipSystemAPI system = ship.getSystem();
        if (runOnce && system.isChargeup()) { // when the system turns on, track whether we're going to glow the hangar
            willRestoreFighters = sd_auxforge.willRestoreFighters(ship);
            runOnce = false;
        }
        if (system.isActive() && willRestoreFighters) // if the system is on & we're going to glow the hangar, then do so
            weapon.setForceFireOneFrame(true);
        if (!system.isActive() && !runOnce) { // if the system turns off, reset the tracker for the next activation
            runOnce = true;
        }
    }
}
