package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class sd_auxiliarynanoforge extends BaseShipSystemScript  {
    final float BAY_REPLENISHMENT_AMOUNT = 0.10f;
    final float MISSILE_COOLDOWN_PENALTY = 3;
    final float MISSILE_RELOAD_AMOUNT = 2;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (Global.getCombatEngine() == null || stats.getEntity().getOwner() == -1 || stats.getVariant() == null)
            return;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        WeaponAPI emptiestMissile = null;
        float emptiestMissileRatio = 1;
        //find out which missile has the least ammo
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getType() != WeaponAPI.WeaponType.MISSILE || !weapon.usesAmmo())
                continue;

            float weaponAmmoRatio = (float) weapon.getAmmo() / weapon.getMaxAmmo();
            if (weaponAmmoRatio < emptiestMissileRatio) {
                emptiestMissileRatio = weaponAmmoRatio;
                emptiestMissile = weapon;
            }
        }
        //if fighters are at a lower "ammo" than the lowest missile, then restore fighters
        if (emptiestMissile == null || ship.getSharedFighterReplacementRate() < emptiestMissileRatio) {
            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing() == null)
                    continue;

                //replenish the stat
                bay.setCurrRate(Math.min(bay.getCurrRate() + BAY_REPLENISHMENT_AMOUNT, 1));

                //instantly relaunch the fighters we just built (if any)
                int maxWingSize = bay.getWing().getSpec().getNumFighters();
                int fightersToAdd = Math.round(maxWingSize * BAY_REPLENISHMENT_AMOUNT);
                bay.makeCurrentIntervalFast();
                if (fightersToAdd > 0 && bay.getWing().getWingMembers().size() < maxWingSize)
                    bay.setFastReplacements(fightersToAdd);

                //do vfx
                //TODO: this
            }
        } else { //otherwise, restore ammo to the missile
            emptiestMissile.setAmmo(emptiestMissile.getAmmo() + (int) Math.ceil(MISSILE_RELOAD_AMOUNT / getReloadCost(emptiestMissile)));
            emptiestMissile.setRemainingCooldownTo(MISSILE_COOLDOWN_PENALTY + emptiestMissile.getCooldown());

            //do vfx
            //TODO: this
        }
    }

    //TODO: RETURN UNUSABLE IF ALL MISSILES ARE RESTORED AND FIGHTERS ARE HEALTHY
//    @Override
//    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
//        if (system.isOutOfAmmo() || system.getState() != ShipSystemAPI.SystemState.IDLE)
//            return null;
//        if (getAverageArmorPerCell(ship.getArmorGrid()) <= ship.getArmorGrid().getMaxArmorInCell() / 10)
//            return "ARMOR DESTROYED";
//        if (isArmorGridBalanced(ship.getArmorGrid()))
//            return "ARMOR BALANCED";
//        if (system.isActive())
//            return "REBALANCING";
//        return "READY";
//    }

    //literally just copied alex's map from the missile autoloader because he didn't make it an api call and I don't want to import
    public static float getReloadCost(WeaponAPI weapon) {
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
