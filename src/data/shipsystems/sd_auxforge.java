package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.sd_util;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;

public class sd_auxforge extends BaseShipSystemScript  {
    final float MISSILE_COOLDOWN_PENALTY = 10, BAY_REPLENISHMENT_AMOUNT = 0.10f;
    boolean willRestoreFighters = true;
    boolean runOnce = true;
    List<WeaponAPI> missiles = new ArrayList<>();
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (!sd_util.isCombatSituation(ship))
            return;

        //need a runOnce here so the weapon glow doesn't shift when the system goes into chargedown
        if (runOnce && ship.getSystem().isChargeup()) {
            missiles = getReloadableMissiles(ship);
            willRestoreFighters = willRestoreFighters(ship);
            runOnce = false;
        }

        //if we're going to restore missiles, then start glowing the missile
        // holy shit this vfx was a pain in the ass and I had to refactor the entire script architecture to get it to work seamlessly
        // I've deleted over 150 lines of code from previous vfx implementations, be happy that this is only one line now (although there's a few more in the hangar deco script)
        // Ok update: I had to refactor it again cuz scope changed, so all that complaining was about code that no longer exists. :)))))))
        if (!willRestoreFighters)
            for (WeaponAPI missile : missiles)
                missile.setGlowAmount(1, sd_util.healUnderColor);

        // the actual restoration effects occur here
        if (effectLevel == 1) { // need to check effectLevel to prevent the restoration from occurring multiple times
            if (willRestoreFighters) { // restore fighters if they're at a lower "ammo" than the lowest missile, or there's no missile
                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    if (bay.getWing() == null)
                        continue;
                    bay.setCurrRate(Math.min(bay.getCurrRate() + BAY_REPLENISHMENT_AMOUNT, 1));
                    // instantly relaunch the fighters we just built (if any)
                    int maxWingSize = bay.getWing().getSpec().getNumFighters();
                    int fightersToAdd = (int) Math.ceil(maxWingSize * BAY_REPLENISHMENT_AMOUNT);
                    bay.makeCurrentIntervalFast();
                    if (fightersToAdd > 0 && bay.getWing().getWingMembers().size() < maxWingSize)
                        bay.setFastReplacements(fightersToAdd);
                }
            } else { // otherwise, restore ammo to the missile
                for (WeaponAPI missile : missiles) {
                    missile.setAmmo(Math.min(missile.getAmmo() + missile.getSpec().getMaxAmmo(), missile.getMaxAmmo()));
                    missile.setRemainingCooldownTo(MISSILE_COOLDOWN_PENALTY + missile.getCooldown() + missile.getCooldownRemaining());
                }
            }
            runOnce = true;
        }
    }
    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (!missiles.isEmpty()) // glow sticks around after the system deactivates otherwise LMAO
            for (WeaponAPI missile : missiles)
                missile.setGlowAmount(0, sd_util.factionColor);
    }
    public static float getAverageMissileFullness(List<WeaponAPI> missiles) {
        List<Float> fullnesses = new ArrayList<>();
        for (WeaponAPI missile : missiles)
            fullnesses.add((float) (missile.getAmmo() / missile.getMaxAmmo()));

        float fullness = 0; // returns 0 if there's nothing that can be restored
        for (float ratio : fullnesses)
            fullness += ratio;

        if (!fullnesses.isEmpty())
            fullness /= fullnesses.size();

        return fullness;
    }
    public static boolean willMissilesOverfill(List<WeaponAPI> missiles) {
        if (missiles.isEmpty()) // need this because of how I set the code up
            return true;

        boolean willOverflow = false;
        for (WeaponAPI missile : missiles)
            // Normally we want to restore the base max ammo, but missile ammo reductions might exist, so we do math.min
            if (missile.getAmmo() + Math.min(missile.getMaxAmmo(), missile.getSpec().getMaxAmmo()) > missile.getMaxAmmo())
                willOverflow = true;

        return willOverflow;
    }
    public static List<WeaponAPI> getReloadableMissiles(ShipAPI ship) {
        List<WeaponAPI> missiles = new ArrayList<>();
        for (WeaponAPI weapon : ship.getAllWeapons())
            if (weapon.getSize() == WeaponAPI.WeaponSize.SMALL && weapon.getType() == WeaponAPI.WeaponType.MISSILE && weapon.usesAmmo()) {
                missiles.add(weapon);
            }

        return missiles;
    }
    public static boolean willRestoreFighters(ShipAPI ship) {
        List<WeaponAPI> missiles = getReloadableMissiles(ship);
        if (willMissilesOverfill(missiles))
            return true;
        return ship.getSharedFighterReplacementRate() <= getAverageMissileFullness(missiles);
    }
    public static boolean canBeUsed(ShipAPI ship) {
        return sd_util.canUseSystemThisFrame(ship) && (ship.getSharedFighterReplacementRate() < 0.9 || !willMissilesOverfill(getReloadableMissiles(ship)));
    }
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return canBeUsed(ship);
    }
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isCoolingDown() || system.isOutOfAmmo())
            return "COOLDOWN";
        if (ship.getFluxTracker().isOverloadedOrVenting() || system.getFluxPerUse() >= (ship.getFluxTracker().getMaxFlux() - ship.getFluxTracker().getCurrFlux()))
            return "STANDBY";
        if (system.isActive())
            return "FABRICATING";
        if (willRestoreFighters(ship) && ship.getSharedFighterReplacementRate() < 0.9)
            return "RESTORE FIGHTERS";
        if (!willMissilesOverfill(getReloadableMissiles(ship)))
            return "RESTORE MISSILES";
        return "STANDBY";
    }
}
