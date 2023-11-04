package data.scripts;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class sd_util {
    public static boolean isNumberWithinRange(float numberA, float numberB, float deviationPercent) {
        float lowerBound = numberB - (numberB * (deviationPercent / 100));
        float upperBound = numberB + (numberB * (deviationPercent / 100));
        return numberA <= upperBound && numberA >= lowerBound;
    }

    public static float getOptimalRange(ShipAPI ship) { // chatgpt wrote most of this
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
    public static void activateSystem(ShipAPI ship, String systemID, int desirePos, int desireNeg, boolean debug) {
        int desireTotal = desirePos + desireNeg;
        if (debug)
            Console.showMessage("Ship:"+ ship.getName() +" Total: "+ desireTotal +" Pos: "+ desirePos +" Neg: "+ desireNeg);

        if (ship.getPhaseCloak() != null && Objects.equals(ship.getPhaseCloak().getId(), systemID)) {
            if (desireTotal >= 100 && !ship.getPhaseCloak().isOn())
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, -1);
            if (desireTotal <= 0 && ship.getPhaseCloak().isOn())
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, -1);
        } else {
            if (desireTotal >= 100 && !ship.getSystem().isOn())
                ship.useSystem();
            if (desireTotal <= 0 && ship.getSystem().isOn())
                ship.useSystem();
        }
    }
    public static void sortByDistance(final ShipAPI ship, final List<ShipAPI> ships, final boolean closestFirst) {
        Collections.sort(ships, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI ship1, ShipAPI ship2) {
                float distance1 = MathUtils.getDistance(ship, ship1);
                float distance2 = MathUtils.getDistance(ship, ship2);
                if (closestFirst)
                    return Float.compare(distance1, distance2);
                return Float.compare(distance2, distance1);
            }
        });
    }
}