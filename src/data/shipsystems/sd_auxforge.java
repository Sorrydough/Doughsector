package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.sd_util;

public class sd_auxforge extends BaseShipSystemScript  {
    static final float MISSILE_RELOAD_AMOUNT = 2;
    final float MISSILE_COOLDOWN_PENALTY = 5, BAY_REPLENISHMENT_AMOUNT = 0.10f;
    boolean willRestoreFighters = true;
    boolean doOnce = true;
    WeaponAPI missile;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;
        ShipAPI ship = (ShipAPI) stats.getEntity();

        //need a doOnce here so the weapon glow doesn't shift when the system goes into chargedown
        if (doOnce && ship.getSystem().isChargeup()) {
            missile = getEmptiestMissile(ship);
            willRestoreFighters = willRestoreFighters(ship);
            doOnce = false;
        }

        //if we're going to restore missiles, then start glowing the missile
        // holy shit this vfx was a pain in the ass and I had to refactor the entire script architecture to get it to work seamlessly
        // I've deleted over 150 lines of code from previous vfx implementations, be happy that this is only one line now (although there's a few more in the hangar deco script)
        if (!willRestoreFighters)
            missile.setGlowAmount(Math.min(effectLevel + 0.5f, 1), sd_util.damageUnderColor);

        // the actual restoration effects occur here
        if (effectLevel == 1) { // need to check effectLevel to prevent the restoration from occurring multiple times
            if (willRestoreFighters) { // restore fighters if they're at a lower "ammo" than the lowest missile, or there's no missile
                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    if (bay.getWing() == null)
                        continue;
                    bay.setCurrRate(Math.min(bay.getCurrRate() + BAY_REPLENISHMENT_AMOUNT, 1));
                    // instantly relaunch the fighters we just built (if any)
                    int maxWingSize = bay.getWing().getSpec().getNumFighters();
                    int fightersToAdd = Math.round(maxWingSize * BAY_REPLENISHMENT_AMOUNT);
                    bay.makeCurrentIntervalFast();
                    if (fightersToAdd > 0 && bay.getWing().getWingMembers().size() < maxWingSize)
                        bay.setFastReplacements(fightersToAdd);
                }
            } else { // otherwise, restore ammo to the missile
                int maxAmmo = missile.getMaxAmmo();
                int ammoAfterReload = Math.min(missile.getAmmo() + (int) Math.ceil(getReloadCost(missile) / MISSILE_RELOAD_AMOUNT), maxAmmo);
                missile.setAmmo(ammoAfterReload);
                missile.setRemainingCooldownTo(MISSILE_COOLDOWN_PENALTY + missile.getCooldown() + missile.getCooldownRemaining());
            }
            doOnce = true;
        }
    }
    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (missile != null) //glow sticks around after the system deactivates otherwise LMAO
            missile.setGlowAmount(0, sd_util.damageUnderColor);
    }
    public static boolean willRestoreFighters(ShipAPI ship) {
        WeaponAPI missile = getEmptiestMissile(ship);
        if (missile == null)
            return true;
        float ratio = (float) missile.getAmmo() / missile.getMaxAmmo();
        return ship.getSharedFighterReplacementRate() <= ratio;
    }
    public static WeaponAPI getEmptiestMissile(ShipAPI ship) {
        WeaponAPI emptiestMissile = null; // returns null if there's no missile to restore ammo for
        float emptiestMissileRatio = 1;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getType() != WeaponAPI.WeaponType.MISSILE || !weapon.usesAmmo())
                continue;
            float ratio = (float) weapon.getAmmo() / weapon.getMaxAmmo();
            if (ratio < emptiestMissileRatio) {
                emptiestMissileRatio = ratio;
                emptiestMissile = weapon;
            }
        }
        return emptiestMissile;
    }
    public static boolean canReloadMissile(WeaponAPI weapon) {
        return weapon != null && weapon.getAmmo() + (int) Math.ceil(MISSILE_RELOAD_AMOUNT / getReloadCost(weapon)) <= weapon.getMaxAmmo();
    }
    public static boolean canBeUsed(ShipAPI ship) {
        return sd_util.canUseSystemThisFrame(ship) && (ship.getSharedFighterReplacementRate() < 0.9 || canReloadMissile(getEmptiestMissile(ship)));
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
        if (canReloadMissile(getEmptiestMissile(ship)))
            return "RESTORE MISSILES";
        return "STANDBY";
    }
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return canBeUsed(ship);
    }
    // literally just copied alex's map from the missile autoloader because he didn't make it an api call and I don't want to import
    static float getReloadCost(WeaponAPI weapon) {
        if (weapon.getSpec().hasTag(Tags.RELOAD_1PT)) return 1f;
        if (weapon.getSpec().hasTag(Tags.RELOAD_1_AND_A_HALF_PT)) return 1.5f;
        if (weapon.getSpec().hasTag(Tags.RELOAD_2PT)) return 2f;
        if (weapon.getSpec().hasTag(Tags.RELOAD_3PT)) return 3f;
        if (weapon.getSpec().hasTag(Tags.RELOAD_4PT)) return 4f;
        if (weapon.getSpec().hasTag(Tags.RELOAD_5PT)) return 5f;
        if (weapon.getSpec().hasTag(Tags.RELOAD_6PT)) return 6f;

        int op = Math.round(weapon.getSpec().getOrdnancePointCost(null, null));
        if (op == 1) return 1f;
        if (op == 2 || op == 3) return 2f;
        if (op == 4) return 3f;
        if (op == 5 || op == 6) return 4f;
        return 6f;
    }
}
