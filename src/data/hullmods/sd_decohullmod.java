package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.graphics.sd_decoSystemRangePlugin;

import java.util.Arrays;
import java.util.List;

public class sd_decohullmod extends BaseHullMod {
    List<String> decoSystemRange = Arrays.asList("sd_motearmor", "sd_hackingsuite", "sd_nullifier", "sd_stasisfield");
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (decoSystemRange.contains(ship.getSystem().getId()))
            Global.getCombatEngine().addPlugin(new sd_decoSystemRangePlugin(ship));
    }
}
