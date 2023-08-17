package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags.*;

public class sd_siegemodeAIchatGPT implements ShipSystemAIScript {
    ShipAPI ship;
    CombatEngineAPI engine;
    ShipwideAIFlags flags;
    ShipSystemAPI system;
    float desire;
    final IntervalUtil interval = new IntervalUtil(0.5f, 1f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            CombatEntityAPI nearestEnemy = AIUtils.getNearestEnemy(ship);
            if ((nearestEnemy == null || target == null || ship.getAIFlags().hasFlag(NEEDS_HELP)) && ship.getSystem().isOn()) {
                ship.useSystem();
                return;
            }
            if (nearestEnemy == null || target == null || ship.getAIFlags().hasFlag(NEEDS_HELP) || system.getCooldownRemaining() > 0) {
                return;
            }
            float nearestEnemyDistance = MathUtils.getDistance(ship, nearestEnemy);
            float targetDistance = MathUtils.getDistance(ship, target);

            // Create a paired list of nearby enemy ships, their flux levels, their hull sizes, and whether they are getting fucked up
            List<CombatEntityAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, 1500f); // Adjust the range as needed
            List<Float> enemyFluxLevels = new ArrayList<>();
            List<Float> enemyHullSizes = new ArrayList<>();
            List<Boolean> enemiesGettingFuckedUp = new ArrayList<>();
            for (CombatEntityAPI enemy : nearbyEnemies) {
                enemyFluxLevels.add(enemy.getFluxTracker().getFluxLevel());
                enemyHullSizes.add(enemy.getHullSize().ordinal());
                // You need to implement your own logic to determine if an enemy is getting fucked up
                enemiesGettingFuckedUp.add(isEnemyGettingFuckedUp(enemy));
            }

            // Calculate desire based on various factors
            if (ship.getAIFlags().hasFlag(MAINTAINING_STRIKE_RANGE) || ship.getAIFlags().hasFlag(CAMP_LOCATION) || ship.getAIFlags().hasFlag(ESCORT_OTHER_SHIP))
                desire += 50;

            if (ship.getAIFlags().hasFlag(AVOIDING_BORDER) || ship.getAIFlags().hasFlag(HAS_POTENTIAL_MINE_TRIGGER_NEARBY) || ship.getAIFlags().hasFlag(BACKING_OFF))
                desire -= 50;

            // Calculate desire based on missiles in flight
            int missilesInFlight = ship.getMissiles().size(); // This assumes you have a method to get the list of missiles
            desire += missilesInFlight * 10; // You can adjust the weight as needed

            // Calculate desire based on optimal range
            float optimalRange = ship.getMutableStats().getEnergyWeaponRange() * 1.5f; // Adjust the range factor as needed
            if (nearestEnemyDistance < optimalRange)
                desire += 20;
            else if (nearestEnemyDistance > optimalRange * 1.25f)
                desire -= 20;

            // Calculate desire based on enemy proximity
            int numEnemies = nearbyEnemies.size();
            for (int i = 0; i < numEnemies; i++) {
                float proximityFactor = (enemyHullSizes.get(i) + 1) * (enemiesGettingFuckedUp.get(i) ? 0.5f : 1.0f);
                desire += proximityFactor * 10;
            }

            // Calculate desire based on hard flux
            desire -= ship.getFluxTracker().getHardFluxLevel() * 133;

            if (desire >= 100 && !ship.getSystem().isOn())
                ship.useSystem();
            if (desire <= -100 && ship.getSystem().isOn())
                ship.useSystem();
            desire = 0;
        }
    }

    // You need to implement this method to determine if an enemy is getting fucked up
    private boolean isEnemyGettingFuckedUp(CombatEntityAPI enemy) {
        // Implement your logic here to determine if an enemy is getting fucked up
        return false;
    }
}
