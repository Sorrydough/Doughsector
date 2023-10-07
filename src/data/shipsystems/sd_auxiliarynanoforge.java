package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class sd_auxiliarynanoforge extends BaseShipSystemScript  {
    final float BAY_REPLENISHMENT_AMOUNT = 0.10f;
    final float MISSILE_COOLDOWN_PENALTY = 5;
    static final float MISSILE_RELOAD_AMOUNT = 2;
    WeaponAPI emptiestMissile = null;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        WeaponSlotAPI slot = null;

        emptiestMissile = getEmptiestMissile(ship);
        float emptiestMissileRatio = getAmmoRatio(emptiestMissile);

        if (effectLevel == 1) {
            // find out which missile has the least ammo and its ratio
            // if fighters are at a lower "ammo" than the lowest missile, then restore fighters - there's an inspection bug with getEmptiestMissile, suppress the error for my own sanity
            if (emptiestMissile == null || ship.getSharedFighterReplacementRate() < emptiestMissileRatio) {
                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    if (bay.getWing() == null)
                        continue;
                    // replenish the stat
                    bay.setCurrRate(Math.min(bay.getCurrRate() + BAY_REPLENISHMENT_AMOUNT, 1));
                    // instantly relaunch the fighters we just built (if any)
                    int maxWingSize = bay.getWing().getSpec().getNumFighters();
                    int fightersToAdd = Math.round(maxWingSize * BAY_REPLENISHMENT_AMOUNT);
                    bay.makeCurrentIntervalFast();
                    if (fightersToAdd > 0 && bay.getWing().getWingMembers().size() < maxWingSize)
                        bay.setFastReplacements(fightersToAdd);
                    slot = bay.getWeaponSlot();
                }
            } else { // otherwise, restore ammo to the missile
                int maxAmmo = emptiestMissile.getMaxAmmo();
                int ammoAfterReload = Math.min(emptiestMissile.getAmmo() + (int) Math.ceil(MISSILE_RELOAD_AMOUNT / getReloadCost(emptiestMissile)), maxAmmo);
                emptiestMissile.setAmmo(ammoAfterReload);
                emptiestMissile.setRemainingCooldownTo(MISSILE_COOLDOWN_PENALTY + emptiestMissile.getCooldown() + emptiestMissile.getCooldownRemaining());
                slot = emptiestMissile.getSlot();
            }
        }
        if (slot != null) {
            //TODO: fix vfx being drawn during chargedown only
            doVFX(ship, slot, Global.getCombatEngine());
        }
    }
    void doVFX(ShipAPI ship, WeaponSlotAPI slot, CombatEngineAPI engine) {
        Vector2f location = slot.computePosition(ship);
        engine.addFloatingText(location, "YOUR MOM!",
                25, Color.LIGHT_GRAY, ship, 1, 10);
        for (int a = 0; a < 10; a++) {
            engine.addSmoothParticle(location, ship.getVelocity(), MathUtils.getRandomNumberInRange(25f, 35f), 1, 0.1f, new Color(255, 120, 80, 1));
            Vector2f fastParticleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(80f, 250f), slot.computeMidArcAngle(ship) + MathUtils.getRandomNumberInRange(-17f, 17f));
            float randomSize01 = MathUtils.getRandomNumberInRange(3f, 5f);
            engine.addSmoothParticle(MathUtils.getRandomPointOnCircumference(location, 4f), fastParticleVel, randomSize01, 1, MathUtils.getRandomNumberInRange(0.2f, 0.25f), new Color(255, 120, 80, 1));
            for (int b = 0; b < 3; b++) {
                Vector2f particleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(35f, 125f), slot.computeMidArcAngle(ship) + MathUtils.getRandomNumberInRange(-20f, 20f));
                float randomSize1 = MathUtils.getRandomNumberInRange(3f, 5f);
                engine.addSmoothParticle(MathUtils.getRandomPointOnCircumference(location, 4f), particleVel, randomSize1, 1, MathUtils.getRandomNumberInRange(0.35f, 0.5f), new Color(255, 120, 80, 1));
            }
        }
    }
    static WeaponAPI getEmptiestMissile(ShipAPI ship) {
        WeaponAPI emptiestMissile = null;
        float emptiestMissileRatio = 1;
        // find out which missile has the least ammo
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getType() != WeaponAPI.WeaponType.MISSILE || !weapon.usesAmmo())
                continue;
            if (getAmmoRatio(weapon) < emptiestMissileRatio) {
                emptiestMissileRatio = getAmmoRatio(weapon);
                emptiestMissile = weapon;
            }
        }
        return emptiestMissile;
    }
    static float getAmmoRatio(WeaponAPI weapon) {
        if (weapon == null)
            return 1;
        return (float) weapon.getAmmo() / weapon.getMaxAmmo();
    }
    static boolean canRestoreAmmo(WeaponAPI missile) {
        if (missile == null)
            return false;
        return missile.getAmmo() + (int) Math.ceil(MISSILE_RELOAD_AMOUNT / getReloadCost(missile)) <= missile.getMaxAmmo();
    }
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo() || system.isActive() || system.getState() == ShipSystemAPI.SystemState.COOLDOWN)
            return "FABRICATING";
        WeaponAPI emptiestMissile = getEmptiestMissile(ship);
        float ammoRatio = getAmmoRatio(emptiestMissile);
        float replacementRate = ship.getSharedFighterReplacementRate();
        if (canRestoreAmmo(emptiestMissile) && ammoRatio <= replacementRate)
            return "RESTORE MISSILES";
        if (ship.getSharedFighterReplacementRate() < 0.9 && replacementRate < ammoRatio)
            return "RESTORE FIGHTERS";
        return "STANDBY";
    }
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return canRestoreAmmo(getEmptiestMissile(ship)) || ship.getSharedFighterReplacementRate() <= 0.9;
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
