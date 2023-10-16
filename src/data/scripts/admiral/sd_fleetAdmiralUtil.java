package data.scripts.admiral;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AssignmentTargetAPI;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class sd_fleetAdmiralUtil {
    // need a custom class to keep track of what each assignment is actually attached to
    public static class AssignmentInfoWithTarget { // TODO: STOP USING THIS GOOFY THING AND USE A MAP INSTEAD
        private final CombatFleetManagerAPI.AssignmentInfo assignment;
        private final Object object; // You can use 'Object' to represent various types of targets
        public AssignmentInfoWithTarget(CombatFleetManagerAPI.AssignmentInfo assignment, Object object) {
            this.assignment = assignment;
            this.object = object;
        }
        public CombatFleetManagerAPI.AssignmentInfo getAssignment() { return assignment; }
        public Object getObject() { return object; }
    }

    public static void applyAssignment(AssignmentTargetAPI target, CombatAssignmentType assignment, int owner) {
        Global.getCombatEngine().getFleetManager(owner).getTaskManager(false).createAssignment(assignment, target, false);
    }

    public static void sortByRecoveryCost(List<ShipAPI> ships) {
        Collections.sort(ships, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI ship1, ShipAPI ship2) {
                float supplies1 = ship1.getHullSpec().getSuppliesToRecover();
                float supplies2 = ship2.getHullSpec().getSuppliesToRecover();
                return Float.compare(supplies2, supplies1);
            }
        });
    }
}
