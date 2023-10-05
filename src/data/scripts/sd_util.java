package data.scripts;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class sd_util {
    public static boolean isNumberWithinRange(float numberA, float numberB, float deviationPercent) {
        float lowerBound = numberB - (numberB * (deviationPercent / 100));
        float upperBound = numberB + (numberB * (deviationPercent / 100));
        return numberA <= upperBound && numberA >= lowerBound;
    }
    public static float getOptimalRange(ShipAPI ship) {
        float totalDPS = 0;
        float totalWeightedRange = 0;
        float optimalWeaponRange = 0;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            float weaponDPS = weapon.getSpec().getDerivedStats().getDps();
            float weaponRange = weapon.getRange();

            //adjust the weight based on DPS
            totalWeightedRange += weaponRange * weaponDPS;
            totalDPS += weaponDPS;
        }
        if (totalDPS > 0) {
            optimalWeaponRange = totalWeightedRange / totalDPS;
        }
        return optimalWeaponRange;
    }
}
