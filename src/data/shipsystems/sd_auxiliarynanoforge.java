package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class sd_auxiliarynanoforge extends BaseShipSystemScript  {
    final float BAY_REPLENISHMENT_AMOUNT = 0.10f;
    final float MISSILE_COOLDOWN_PENALTY = 5;
    static final float MISSILE_RELOAD_AMOUNT = 2;
    boolean doOnce = true;
    boolean restoreFighters = true;
    List<WeaponSlotAPI> slots = new ArrayList<>();
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        // find out which missile has the least ammo and its ratio
        WeaponAPI weapon = getEmptiestMissile(ship);
        float emptiestMissileRatio = getAmmoRatio(weapon);
        if (doOnce && ship.getSystem().isChargeup()) { // need a doOnce here to prevent the vfx location from "shifting" after restoration has occurred
            slots.clear(); //clear any remembered slots from the previous use of the system
            // if there's no weapon to restore ammo to, make a list of our fighter bays to do vfx on
            if (weapon == null || ship.getSharedFighterReplacementRate() < emptiestMissileRatio) {
                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    if (bay.getWing() == null)
                        continue;
                    slots.add(bay.getWeaponSlot());
                }
            } else { //if we're going to restore ammo then set the flag
                slots.add(weapon.getSlot());
                restoreFighters = false;
            }
            doOnce = false;
        }
        //do vfx, holy shit this was a pain in the ass and I had to refactor the entire script architecture to get it to work seamlessly
        if (slots.size() > 0) { // TODO: CUT ALL THIS BULLSHIT, SET WEAPON TO GLOW, SET HANGAR DECO WEAPON TO HAVE A GLOW SPRITE
            for (WeaponSlotAPI slot : slots) {
                CombatEngineAPI engine = Global.getCombatEngine();
                Vector2f location = slot.computePosition(ship);
                Color particleColor = new Color(255, 120, 80, Math.round(105 * effectLevel));
                engine.addSmoothParticle(location, ship.getVelocity(), MathUtils.getRandomNumberInRange(25f, 35f), effectLevel, 0.1f, particleColor);
                Vector2f fastParticleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(80f, 250f),
                        slot.computeMidArcAngle(ship) + MathUtils.getRandomNumberInRange(-17f, 17f));
                float randomSize01 = MathUtils.getRandomNumberInRange(3f, 5f);
                engine.addSmoothParticle(MathUtils.getRandomPointOnCircumference(location, 10f), fastParticleVel, randomSize01, effectLevel,
                        MathUtils.getRandomNumberInRange(0.2f, 0.25f), new Color(255,120,80,105));
                for (int b = 0; b < 2; b++) {
                    Vector2f particleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(35f, 125f),
                            slot.computeMidArcAngle(ship) + MathUtils.getRandomNumberInRange(-20f, 20f));
                    float randomSize1 = MathUtils.getRandomNumberInRange(3f, 5f);
                    engine.addSmoothParticle(MathUtils.getRandomPointOnCircumference(location, 10f), particleVel, randomSize1, effectLevel,
                            MathUtils.getRandomNumberInRange(0.35f, 0.5f), particleColor);
                }
            }
        }
        // the actual restoration effects occur here
        if (effectLevel == 1) { // need to check effectLevel to prevent the restoration from occurring multiple times
            if (restoreFighters) { // if fighters are at a lower "ammo" than the lowest missile, then restore fighters
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
                }
            } else { // otherwise, restore ammo to the missile
                assert weapon != null;
                int maxAmmo = weapon.getMaxAmmo();
                int ammoAfterReload = Math.min(weapon.getAmmo() + (int) Math.ceil(MISSILE_RELOAD_AMOUNT / getReloadCost(weapon)), maxAmmo);
                weapon.setAmmo(ammoAfterReload);
                weapon.setRemainingCooldownTo(MISSILE_COOLDOWN_PENALTY + weapon.getCooldown() + weapon.getCooldownRemaining());
            }
            restoreFighters = true;
            doOnce = true;
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
        if (!AIUtils.canUseSystemThisFrame(ship))
            return "STANDBY";
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
        return AIUtils.canUseSystemThisFrame(ship) && (canRestoreAmmo(getEmptiestMissile(ship)) || ship.getSharedFighterReplacementRate() <= 0.9);
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
