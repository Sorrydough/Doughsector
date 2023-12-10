package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class sd_motearmor extends BaseShipSystemScript {
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
    }
//    public StatusData getStatusData(int index, State state, float effectLevel) {
//        if (index == 0)
//            return new StatusData("TIMEFLOW ALTERED", false); // todo: show how much the timeflow has degraded
//        if (index == 1)
//            return new StatusData("REBALANCING ARMOR", false); // todo: figure out how to check the armor grid from here
//        return null;
//    }
//    @Override
//    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
//        ArmorGridAPI grid = ship.getArmorGrid();
//        if (sd_morphicarmor.isArmorGridDestroyed(grid))
//            return "ARMOR DESTROYED";
//        if (sd_morphicarmor.isArmorGridBalanced(grid))
//            return "ARMOR BALANCED";
//        if (system.isActive())
//            return "REBALANCING";
//        return "READY";
//    }
//    @Override
//    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
//        return !sd_morphicarmor.isArmorGridDestroyed(ship.getArmorGrid());
//    }
}
