package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.sd_util;
import data.weapons.mote.sd_moteAIScript;
import data.shipsystems.sd_mnemonicarmor;
import data.shipsystems.sd_motearmor;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

// todo: change the advance functionality to be related to min CR

// todo needs to go into here:
// spawn motes when we start a fight, scales off field strength - DONE
// spawn reaper motes when we get armorbroken, scales off field strength - DONE
// give an ECM bonus depending on the number of alive motes, scales off field strength - DONE
// set an attractor lock on the nearest enemy when we get overloaded or armorbroken, clear attractor lock when we're not overloading - DONE

// queso how does the cloak functionality work?
// all ships of the same hull size or smaller have their sensor profile reduced by 50% OR reduced to this ship's sensor profile, whichever is the worse bonus
// can also reduce the sensor profile of a single ship that's larger than itself, the strength of which is dependent on this ship's sensor stat

// Reduces the sensor signature of 1 ship larger than itself and 3 ships at most the same size by 50% OR to this ship's sensor signature, whichever is the weaker bonus.
// Adding additional ships of this type doesn't improve the sensor signature reduction, only the largest ship of the type can provide sensor sig reduction.
// Stops functioning if the ship's CR goes below 0.1


public class sd_motefield extends BaseHullMod implements HullModFleetEffect {
    final static float DEPLETED_CR = 0.1f;
    final static float FULL_CR = 0.5f;
    final float PERSONAL_CLOAK_MULT = 0.75f;
    final String MOD_KEY = "sd_motefield";
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSensorProfile().modifyMult(id, PERSONAL_CLOAK_MULT);
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        Global.getCombatEngine().addPlugin(new sd_motefieldPlugin(ship));
    }
    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {
        String key = "$updatedMotefieldModifier";
        if (fleet.isPlayerFleet() && fleet.getMemoryWithoutUpdate() != null && !fleet.getMemoryWithoutUpdate().getBoolean(key) && fleet.getMemoryWithoutUpdate().getBoolean("$justToggledTransponder")) {
            this.onFleetSync(fleet);
            fleet.getMemoryWithoutUpdate().set(key, true, 0.1f);
        }
    }
    @Override
    public void onFleetSync(CampaignFleetAPI fleet) { // todo: this
        //float mult = getPhaseFieldMultBaseProfileAndTotal(fleet);
        float mult = 1;

        if (fleet.isTransponderOn())
            mult = 1;

        if (mult <= 0)
            fleet.getDetectedRangeMod().unmodifyMult(MOD_KEY);
        else
            fleet.getDetectedRangeMod().modifyMult(MOD_KEY, mult, "Phase ships in fleet");
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("This ship's system is tied into a unique armor technology. While its principles are kept jealously classified, their effects can be plainly observed:", 5f);
        tooltip.addPara("Upon arrival in combat the ship is accompanied by a number of motes. When the system rebalances its armor, these motes regenerate.",5f);
        tooltip.addPara("The fleet recieves a 0.5%% improvement to their ECM rating per mote present on the battlefield.", 2f, Misc.getHighlightColor(), "0.5%");
        tooltip.addPara("Out of combat, an obscuring effect is also observed:", 5f);
        tooltip.addPara("This ship reduces the sensor signature of 1 ship larger than itself and 3 ships of at most the same size by 50% OR to this ship's sensor signature, whichever is the weaker bonus.", 2f);
        tooltip.addPara("All aforementioned effects degrade as more ships of the type become present in the fleet and as this ship's combat readiness slides into malfunction range.", 5f);
    }
    @Override
    public boolean withAdvanceInCampaign() {
        return true;
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    static class sd_motefieldPlugin extends BaseEveryFrameCombatPlugin {
        final ShipAPI ship;
        final CombatEngineAPI engine;
        final sd_motearmor.SharedMoteAIData data;
        final String id;
        final float ECM_PER_MOTE = 0.5f;
        final float motesToSpawn;
        final float motefieldStrengthFactor;
        public sd_motefieldPlugin(ShipAPI ship) {
            this.ship = ship;
            this.id = ship.getId() + "_motefield";
            this.engine = Global.getCombatEngine();
            this.motefieldStrengthFactor = getMotefieldStrengthFactor(ship);
            this.motesToSpawn = sd_motearmor.MAX_MOTES.get(ship.getHullSize()) * motefieldStrengthFactor;
            data = sd_motearmor.getSharedData(ship);
        }
        final IntervalUtil interval = new IntervalUtil(0.1f, 0.5f);
        boolean wantYolo = false;
        float spawnedStarterMotes = 0;
        float spawnedReaperMotes = 0;
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (!sd_util.isCombatSituation(ship))
                return;

            interval.advance(amount);
            if (interval.intervalElapsed()) {
                ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, ECM_PER_MOTE * data.motes.size() * motefieldStrengthFactor);

//                ShipAPI target = null; // todo: revisit this and write a util that makes use of target for ship system
//                if (ship.getAI() != null && ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM) instanceof ShipAPI)
//                    target = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM);
//                if (ship.getShipTarget() != null)
//                    target = ship.getShipTarget();

                if (wantYolo || ship.getFluxTracker().isOverloaded())
                    data.attractorTarget = ship.getShipTarget();
                else
                    data.attractorTarget = null;

                if (spawnedStarterMotes > motesToSpawn && spawnedReaperMotes > motesToSpawn)
                    return;

                Vector2f randomPoint = getRandomPointOnShip(ship);
                if (sd_mnemonicarmor.isArmorGridDestroyed(ship.getArmorGrid()))
                    wantYolo = true;
                if (spawnedStarterMotes < motesToSpawn) { // todo: make it look like these come from offscreen
                    sd_motearmor.emitMote(ship, randomPoint, false);
                    spawnedStarterMotes ++;
                }
                if (wantYolo && spawnedReaperMotes < motesToSpawn) {
                    // spawn reaper motes somewhere
                    int angleOffset = new Random().nextInt(361) - 180;
                    MissileAPI mote = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "sd_motesuperlauncher", randomPoint, angleOffset + ship.getFacing(), ship.getVelocity());
                    Global.getSoundPlayer().playSound("system_flare_launcher_active", 1.0f, 1.6f, randomPoint, ship.getVelocity());
                    mote.setMissileAI(new sd_moteAIScript(mote));
                    mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
                    mote.setEmpResistance(10000);
                    sd_motearmor.getSharedData(ship).motes.add(mote);
                    spawnedReaperMotes ++;
                }
            }
        }
    }
    // todo: integrate this logic to account for location giving the cell's corner
//    Vector2f cellOffset = new Vector2f(ship.getArmorGrid().getCellSize() / 2f, -ship.getArmorGrid().getCellSize() / 2f);
//    cellOffset = VectorUtils.rotate(cellOffset, ship.getFacing());
    static Vector2f getRandomPointOnShip(ShipAPI ship) {
        List <Vector2f> pointsWithinBounds = new ArrayList<>();
        ArmorGridAPI grid = ship.getArmorGrid();
        for (int ix = 0; ix < grid.getGrid().length; ix++) {
            for (int iy = 0; iy < grid.getGrid()[0].length; iy++) {
                Vector2f location = grid.getLocation(ix, iy);
                if (CollisionUtils.isPointWithinBounds(location, ship))
                    pointsWithinBounds.add(location);
            }
        }
        return pointsWithinBounds.get(new Random().nextInt(pointsWithinBounds.size()));
    }
    // returns a multiplier dependent on the number of other fleetships interfering with this ship's motefield strength, as well as any potential CR penalties
    public static float getMotefieldStrengthFactor(ShipAPI ship) {
        List<FleetMemberAPI> interferingMotefields = new ArrayList<>();
        if (ship.getFleetMember() != null) {
            for (FleetMemberAPI fleetMember : ship.getFleetMember().getFleetData().getMembersListCopy()) {
                if (fleetMember.getVariant().getHullMods().contains("sd_motefield") && !Objects.equals(fleetMember.getId(), ship.getFleetMember().getId())
                        && fleetMember.getHullSpec().getHullSize().ordinal() >= ship.getHullSize().ordinal()) {
                    interferingMotefields.add(fleetMember);
                }
            }
        }
        // if CR is 0.1, then readinessStrengthFactor is 0
        // if CR is 0.5, then readinessStrengthFactor is 1
        // Chatgpt wrote this CR math and I didn't doublecheck it, if it doesn't work then we know who to blame
        float readinessStrengthFactor = Math.max(0, (ship.getCurrentCR() - DEPLETED_CR) / (FULL_CR - DEPLETED_CR));
        float interferenceStrengthFactor = (float) 1 / Math.max(1, interferingMotefields.size());

        return (1 * interferenceStrengthFactor * readinessStrengthFactor);
    }
}