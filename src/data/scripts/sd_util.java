package data.scripts;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.CollisionUtils;

public class sd_util {
    public static float returnArmorCellFullDamageRating(ShipAPI ship, int i, int j, boolean excludeOOB) {
        //we calculate the full reduction rating by adding the armorvalue of the cell itself, the 8 adjacent cells, and half the value of the 12 cells adjacent to those
        float totalArmorValue = ship.getArmorGrid().getArmorValue(i, j);

        //if exclude out of bounds is true and the location is out of bounds then we reject the location and move onto the next
        if (excludeOOB && !CollisionUtils.isPointWithinBounds(ship.getArmorGrid().getLocation(i, j), ship)) {
            return 0f;
        }

        for (int x = i - 1; x <= i + 1; x++) {
            for (int y = j - 1; y <= j + 1; y++) {
                if (x >= 0 && x < ship.getArmorGrid().getGrid().length && y >= 0 && y < ship.getArmorGrid().getGrid()[0].length) {
                    totalArmorValue += ship.getArmorGrid().getArmorValue(x, y);
                }
            }
        }

        for (int x = i - 2; x <= i + 2; x++) {
            for (int y = j - 2; y <= j + 2; y++) {
                if (x >= 0 && x < ship.getArmorGrid().getGrid().length && y >= 0 && y < ship.getArmorGrid().getGrid()[0].length) {
                    if (Math.abs(x - i) == 2 || Math.abs(y - j) == 2) {
                        totalArmorValue += 0.5f * ship.getArmorGrid().getArmorValue(x, y);
                    }
                }
            }
        }
        return totalArmorValue;
    }
}
