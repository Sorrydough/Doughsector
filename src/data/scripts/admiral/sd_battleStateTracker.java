package data.scripts.admiral;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class sd_battleStateTracker { // this class doesn't do anything per se, it's just an object that holds data to be accessed by other classes
    CombatFleetManagerAPI allyFleetManager;
    CombatFleetManagerAPI enemyFleetManager;
    CombatTaskManagerAPI allyTaskManager;
    CombatTaskManagerAPI enemyTaskManager;
    final List<ShipAPI> deployedShips = new ArrayList<>();
    final List<ShipAPI> deployedAllyShips = new ArrayList<>();
    final List<ShipAPI> deployedEnemyShips = new ArrayList<>();
    int deployedAllyDP;
    int deployedEnemyDP;
    int numOwnedObjectives;
    boolean doOnce = true;
    void reset() {
        deployedShips.clear();
        deployedAllyShips.clear();
        deployedEnemyShips.clear();
        deployedAllyDP = 0;
        deployedEnemyDP = 0;
        numOwnedObjectives = 0;
    }
    public void updateState(CombatEngineAPI engine, int allySide, int enemySide) {
        // need to reset all the fields that we don't want to preserve between updates
        reset();
        // updateState gets called and we populate the fields, then we can pass the object to other classes for them to also get that info
        // doing it this way so stuff doesn't have to get recalculated repeatedly and I don't have 10,000 static classes in a util the size of your mom
        if (doOnce) {
            allyFleetManager = engine.getFleetManager(allySide);
            allyTaskManager = allyFleetManager.getTaskManager(false);
            enemyFleetManager = engine.getFleetManager(enemySide);
            enemyTaskManager = enemyFleetManager.getTaskManager(false);
            doOnce = false;
        }

        for (ShipAPI ship : engine.getShips())
            if (!ship.isHulk() && !ship.isShuttlePod() && !ship.isFighter())
                deployedShips.add(ship);

        for (BattleObjectiveAPI objective : engine.getObjectives())
            if (objective.getOwner() == allySide)
                numOwnedObjectives += 1;

        for (ShipAPI ship : deployedShips) {
            if (ship.getOwner() == allySide) {
                deployedAllyShips.add(ship);
                deployedAllyDP += getDeploymentCost(ship);
            } else if (ship.getOwner() == enemySide) {
                deployedEnemyShips.add(ship);
                deployedEnemyDP += getDeploymentCost(ship);
            }
        }
        sortByDeploymentCost(deployedAllyShips);
        sortByDeploymentCost(deployedEnemyShips);






    }
    static float getDeploymentCost(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        return Math.max(stats.getSuppliesToRecover().base, stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(stats.getSuppliesToRecover().modified));
    }
    static void sortByDeploymentCost(List<ShipAPI> ships) {
        Collections.sort(ships, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI ship1, ShipAPI ship2) {
                float supplies1 = getDeploymentCost(ship1);
                float supplies2 = getDeploymentCost(ship2);
                return Float.compare(supplies2, supplies1);
            }
        });
    }
}
