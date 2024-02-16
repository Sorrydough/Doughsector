package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import data.sd_util;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import java.util.List;
import java.util.Map;

public class sd_nullifier extends BaseShipSystemScript {
    boolean runOnce = true;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (!sd_util.isCombatSituation(ship))
            return;

        ship.setJitter(id, sd_util.timeColor1, effectLevel, 1, 0, 5);
        ship.setJitterUnder(id, sd_util.timeColor2, effectLevel, 5, 0, 10);

        if (runOnce) { // apply our effect plugin to the target
            Global.getCombatEngine().addPlugin(new sd_nullifierPlugin(ship, ship.getShipTarget()));
            runOnce = false;
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        runOnce = true;
    }
    public static boolean isTargetValid(ShipAPI ship, ShipAPI target) { // checks whether the target is in range, blah blah blah, needs to take target as an input to work in the AI script
        if (target == null)
            return false;
        float targetDistance = MathUtils.getDistance(ship, target);
        float systemRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(sd_util.getOptimalRange(ship) + ship.getCollisionRadius());
        return !target.isFighter() && target != ship && !(targetDistance > systemRange) && !target.isPhased();
    }
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (system.isActive())
            return true;
        return isTargetValid(ship, ship.getShipTarget()) && sd_util.canUseSystemThisFrame(ship);
    }
    static class sd_nullifierPlugin extends BaseEveryFrameCombatPlugin { // todo: make a util for checking whether target is valid
        final ShipAPI target, ship;
        final MutableShipStatsAPI targetStats;
        final DynamicStatsAPI targetDynamic;
        final CombatEngineAPI engine;
        final String id;
        public sd_nullifierPlugin(ShipAPI ship, ShipAPI target) {
            this.ship = ship;
            this.target = target;
            this.targetStats = target.getMutableStats();
            this.targetDynamic = targetStats.getDynamic();
            this.id = ship.getId();
            this.engine = Global.getCombatEngine();
        }
        final float FLUX_PER_TIMEFLOW = 2, PPT_MULT = 1.5f;
        boolean isDeactivating = false;
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused())
                return;
            // thanks combustiblelemon for helping me fix a bug here where the system couldn't be turned off because you were unable to use the system
            if (!sd_nullifier.isTargetValid(ship, target) && !isDeactivating) {
                isDeactivating = true;
                ship.useSystem();
            }

            float effectLevel = ship.getSystem().getEffectLevel();

            // make target's PPT and CR degrade faster
            targetStats.getCRLossPerSecondPercent().modifyMult(id, PPT_MULT * effectLevel);
            targetStats.getPeakCRDuration().modifyFlat(id, -((PPT_MULT - 1) * amount * effectLevel));

            // 1. Track how many instances of the system are being applied to the target
            targetDynamic.getMod("sd_nullifier").modifyFlat(id, effectLevel);
            float numApplied = targetDynamic.getMod("sd_nullifier").getFlatBonuses().size();

            // 2. Figure out the biggest effectLevel that's being applied to the target
            float nullificationLevel = 0;
            for (Map.Entry<String, MutableStat.StatMod> nullifier : targetDynamic.getMod("sd_nullifier").getFlatBonuses().entrySet())
                if (nullifier.getValue().getValue() > nullificationLevel)
                    nullificationLevel = nullifier.getValue().getValue();

            target.fadeToColor("sd_nullifier", sd_util.timeColor2, 0.25f, 0.25f, 0.66f * nullificationLevel);
            target.setJitterUnder("sd_nullifier", sd_util.timeColor2, nullificationLevel, 10, 0, 10);
            target.setJitter("sd_nullifier", sd_util.timeColor1, nullificationLevel, 1, 0, 5);

            // 3. Apply our timeflow change according to the biggest effectLevel
            targetStats.getTimeMult().unmodify("sd_nullifier");
            float baseTimeflow = target.getMutableStats().getTimeMult().getModifiedValue();
            targetDynamic.getMod("sd_baseTimeMult").modifyFlat("sd_nullifier", baseTimeflow); // storing this on the target so I can access it in the AI script
            float modificationMult = (1 + ((baseTimeflow - 1) * (1 - nullificationLevel))) / baseTimeflow; // puretilt wrote this math for me
            targetStats.getTimeMult().modifyMult("sd_nullifier", modificationMult);
            if (target == engine.getPlayerShip())
                engine.getTimeMult().modifyMult("sd_nullifier", modificationMult);

            // 5. Generate flux
            float modificationPercent = Math.abs(1 - baseTimeflow) * 100;
            ship.getFluxTracker().increaseFlux((modificationPercent * FLUX_PER_TIMEFLOW * effectLevel * amount) / numApplied, true); // divide by numApplied to share flux load

            if (effectLevel == 0) { // cleanup
                Console.showMessage("Doing nullifier cleanup for target "+ target.getName());
                targetDynamic.getMod("sd_nullifier").unmodifyFlat(id);
                targetStats.getCRLossPerSecondPercent().unmodifyMult(id);
                if (targetDynamic.getMod("sd_nullifier").getFlatBonuses().isEmpty()) {
                    targetDynamic.getMod("sd_baseTimeMult").unmodify("sd_nullifier");
                    targetStats.getTimeMult().unmodifyFlat("sd_nullifier");
                    engine.getTimeMult().unmodifyFlat("sd_nullifier");
                }
                engine.removePlugin(this);
            }
        }
    }
}