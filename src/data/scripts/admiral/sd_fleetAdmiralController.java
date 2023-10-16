package data.scripts.admiral;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class sd_fleetAdmiralController extends BaseEveryFrameCombatPlugin {
    private final BattleState battleState = new BattleState();
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        battleState.updateState(engine, 0, 1); // player side is 0, AI side is 1





    }

    public static class BattleState {
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
        public void updateState(CombatEngineAPI engine, int allySide, int enemySide) {
            deployedShips.clear();
            deployedAllyShips.clear();
            deployedEnemyShips.clear();
            deployedAllyDP = 0;
            deployedEnemyDP = 0;
            numOwnedObjectives = 0;
            // updateState gets called and we fill out all the fields, then we can pass the object to other classes for them to also get that info
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


        public CombatFleetManagerAPI getAllyFleetManager() {
            return allyFleetManager;
        }
        public CombatFleetManagerAPI getEnemyFleetManager() {
            return enemyFleetManager;
        }
        public CombatTaskManagerAPI getAllyTaskManager() {
            return allyTaskManager;
        }
        public CombatTaskManagerAPI getEnemyTaskManager() {
            return enemyTaskManager;
        }
        public List<ShipAPI> getDeployedShips() {
            return deployedShips;
        }
        public List<ShipAPI> getDeployedAllyShips() {
            return deployedAllyShips;
        }
        public List<ShipAPI> getDeployedEnemyShips() {
            return deployedEnemyShips;
        }
        public int getDeployedAllyDP() {
            return deployedAllyDP;
        }
        public int getDeployedEnemyDP() {
            return deployedEnemyDP;
        }
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
    static float getDeploymentCost(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        return Math.max(stats.getSuppliesToRecover().base, stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(stats.getSuppliesToRecover().modified));

    }
}