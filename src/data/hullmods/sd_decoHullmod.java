package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.console.Console;

public class sd_decoHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        String systemID = ship.getHullSpec().getShipSystemId();
        switch (systemID) {
            case "sd_auxforge":
                Console.showMessage("auxforge");
                break;
            case "sd_nullifier":
                Console.showMessage("nullifier");
                break;
            case "sd_stasisfield":
                Console.showMessage("stasisfield");
                break;
        }
    }
}
