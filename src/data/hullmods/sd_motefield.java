package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.PhaseField;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.graphics.sd_decoSystemRangePlugin;
import data.sd_util;
import data.shipsystems.mote.sd_moteAIScript;
import data.shipsystems.sd_mnemonicarmor;
import data.shipsystems.sd_motearmor;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

// todo needs to go into here:
// spawn motes when we start a fight
// set an attractor lock on the nearest enemy when we get overloaded, clear attractor lock when we're not overloading
// give an ECM bonus depending on the number of alive motes

// queso how does the cloak functionality work?
// all ships of the same hull size or smaller have their sensor profile reduced by 50% OR reduced to this ship's sensor profile, whichever is the worse bonus
// can also reduce the sensor profile of a single ship that's larger than itself, the strength of which is dependent on this ship's sensor stat

public class sd_motefield extends BaseHullMod implements HullModFleetEffect {
    final float MIN_CR = 0.1f;
    final float PERSONAL_CLOAK_MULT = 0.75f;
    final String ID = "sd_motefield";
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSensorProfile().modifyMult(id, PERSONAL_CLOAK_MULT);
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        Global.getCombatEngine().addPlugin(new sd_motefieldPlugin(ship));
    }
    @Override
    public void advanceInCampaign(CampaignFleetAPI campaignFleetAPI) {

    }

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return false;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI campaignFleetAPI) {

    }

    static class sd_motefieldPlugin extends BaseEveryFrameCombatPlugin {
        final ShipAPI ship;
        final CombatEngineAPI engine;
        final sd_motearmor.SharedMoteAIData data;
        final String id;
        final float ECM_PER_MOTE = 0.5f;
        public sd_motefieldPlugin(ShipAPI ship) {
            this.ship = ship;
            this.id = ship.getId() + "_motefield";
            this.engine = Global.getCombatEngine();
            data = sd_motearmor.getSharedData(ship);
        }
        final IntervalUtil interval = new IntervalUtil(0.1f, 0.5f);
        boolean wantYolo = false;
        float spawnedStarterMotes = 0;
        float spawnedReaperMotes = 0;
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                ship.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, ECM_PER_MOTE * data.motes.size());

                if (ship.getFluxTracker().isOverloaded())
                    data.attractorTarget = ship.getShipTarget();
                else
                    data.attractorTarget = null;
                if (sd_mnemonicarmor.isArmorGridDestroyed(ship.getArmorGrid())) {
                    data.attractorTarget = ship.getShipTarget();
                    wantYolo = true;
                }

                float motesToSpawn = sd_motearmor.STARTING_MOTES.get(ship.getHullSize());
                if (spawnedStarterMotes < motesToSpawn) {
                    sd_motearmor.emitMote(ship, ship.getLocation(), false);
                    spawnedStarterMotes ++;
                }
                if (wantYolo && spawnedReaperMotes < motesToSpawn) {
                    // spawn reaper motes somewhere
                    int angleOffset = new Random().nextInt(361) - 180;
                    MissileAPI mote = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "sd_motelauncher", ship.getLocation(), angleOffset + ship.getFacing(), ship.getVelocity());
                    Global.getSoundPlayer().playSound("system_flare_launcher_active", 1.0f, 1.6f, ship.getLocation(), ship.getVelocity());
                    mote.setMissileAI(new sd_moteAIScript(mote));
                    mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
                    mote.setEmpResistance(10000);
                    sd_motearmor.getSharedData(ship).motes.add(mote);
                    spawnedReaperMotes ++;
                }
            }
        }
    }
//    static Vector2f getRandomPointOnShip(ShipAPI ship) {
//        Vector2f pointOnGrid = new Vector2f(new Random().nextInt(ship.getArmorGrid().getGrid().length), new Random().nextInt(ship.getArmorGrid().getGrid()[0].length));
//        boolean isToSubtractInBounds = CollisionUtils.isPointWithinBounds(toSubtractLoc, ship);
//
//        return
//    }
}
