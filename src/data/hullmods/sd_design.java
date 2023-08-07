package data.hullmods;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.*;

public class sd_design extends BaseHullMod {

    final Map<HullSize, Integer> BEAM_ITU_PERCENT = new HashMap<>();
    {	//free ITU bonus for beams
        BEAM_ITU_PERCENT.put(HullSize.FIGHTER, 0);
        BEAM_ITU_PERCENT.put(HullSize.FRIGATE, 10);
        BEAM_ITU_PERCENT.put(HullSize.DESTROYER, 20);
        BEAM_ITU_PERCENT.put(HullSize.CRUISER, 40);
        BEAM_ITU_PERCENT.put(HullSize.CAPITAL_SHIP, 60);
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        //TODO: Pearlescent shields -> OpenGL bullshit? Fix turnrate. Sort out carrier frigate turret looking goofy. Figure out how to convert beams into lances with HSA.
        //TODO: Implement active flares to counter doom mines. Find a counter to UAF nukes.  Implement fighters. Rework this hullmod description.
        //TODO: Set up ship system variants. Implement cargo bonuses. Civilian refits - phaeton tanker, kite liner. Finish the missile destroyer. Maybe ECM builtin for light frigate.
        //TODO:
        //beam discount
        stats.getDynamic().getMod(Stats.SMALL_BEAM_MOD).modifyFlat(id, -1);
        stats.getDynamic().getMod(Stats.MEDIUM_BEAM_MOD).modifyFlat(id, -2);
        stats.getDynamic().getMod(Stats.LARGE_BEAM_MOD).modifyFlat(id, -4);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.removeListenerOfClass(sd_designListener.class);
        ship.addListener(new sd_designListener(ship));

        //give free beam ITU, bonus not cumulative with targeting computer modifications
        //this needs to be here instead of applyEffectsBeforeShipCreation to avoid an ordering issue
        float bonusToGive = BEAM_ITU_PERCENT.get(ship.getHullSize()) - Math.max( ship.getMutableStats().getEnergyWeaponRangeBonus().getPercentMod(), 0); //ie player uses overclocked targeting unit to get 10%, so we give 30%, so 40% total
        //check whether the bonus is positive, we don't want to accidentally subtract bonus instead if the player gets insane base targeting somehow
        if (bonusToGive > 0) {  ship.getMutableStats().getBeamWeaponRangeBonus().modifyPercent(id, bonusToGive); }
    }

    @Override
    public boolean affectsOPCosts() { return true; }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Beams cost 1/2/4 fewer ordnance points by weapon size.", 10f, Misc.getHighlightColor(), "1/2/4");
        tooltip.addPara("Beams recieve a 10/20/40/60%% range bonus by hull size.", 5f, Misc.getHighlightColor(), "10/20/40/60%");
        tooltip.addPara("Only the strongest bonus between this hullmod and all other percentage bonuses combined will apply.", 1f,
                Misc.getDarkHighlightColor(), "Only the strongest bonus between this hullmod and all other percentage bonuses combined will apply.");
    }

    boolean runOnce = false;
    float maxRange= 0;
    final IntervalUtil timer = new IntervalUtil (0.5f, 1.5f);

    public class sd_designListener implements AdvanceableListener { //SCY's venting code
        protected ShipAPI ship;
        public sd_designListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (!runOnce){
                runOnce=true;
                List<WeaponAPI> loadout = ship.getAllWeapons();
                if (loadout!=null){
                    for (WeaponAPI w : loadout){
                        if (w.getType()!=WeaponAPI.WeaponType.MISSILE){
                            if (w.getRange()>maxRange){
                                maxRange=w.getRange();
                            }
                        }
                    }
                }
                timer.randomize();
            }

            if (Global.getCombatEngine().isPaused() || ship.getShipAI() == null) {
                return;
            }
            timer.advance(amount);
            if (timer.intervalElapsed()) {
                if (ship.getFluxTracker().isOverloadedOrVenting()) {
                    return;
                }
                MissileAPI closest= AIUtils.getNearestEnemyMissile(ship);
                if (closest!=null && MathUtils.isWithinRange(ship, closest,500)){
                    return;
                }

                if ( ship.getFluxTracker().getFluxLevel() < 0.5 && AIUtils.getNearbyEnemies(ship, maxRange) != null) {
                    return;
                }

                //venting need
                float ventingNeed;
                switch (ship.getHullSize()) {
                    case CAPITAL_SHIP:
                        ventingNeed = 2*(float) Math.pow(ship.getFluxTracker().getFluxLevel(),5f);
                        break;
                    case CRUISER:
                        ventingNeed = 1.5f*(float) Math.pow(ship.getFluxTracker().getFluxLevel(),4f);
                        break;
                    case DESTROYER:
                        ventingNeed = (float) Math.pow(ship.getFluxTracker().getFluxLevel(),3f);
                        break;
                    default:
                        ventingNeed = (float) Math.pow(ship.getFluxTracker().getFluxLevel(),2f);
                        break;
                }

                float hullFactor;
                switch (ship.getHullSize()) {
                    case CAPITAL_SHIP:
                        hullFactor=(float) Math.pow(ship.getHullLevel(),0.4f);
                        break;
                    case CRUISER:
                        hullFactor=(float) Math.pow(ship.getHullLevel(),0.6f);
                        break;
                    case DESTROYER:
                        hullFactor=ship.getHullLevel();
                        break;
                    default:
                        hullFactor=(float) Math.pow(ship.getHullLevel(),2f);
                        break;
                }

                //situational danger
                float dangerFactor = 0;
                List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, 2000f);
                for (ShipAPI enemy : nearbyEnemies) {
                    //reset often with timid or cautious personalities
                    FleetSide side = FleetSide.PLAYER;
                    if (ship.getOriginalOwner() > 0){
                        side = FleetSide.ENEMY;
                    }
                    if(Global.getCombatEngine().getFleetManager(side).getDeployedFleetMember(ship)!=null){
                        PersonalityAPI personality = (Global.getCombatEngine().getFleetManager(side).getDeployedFleetMember(ship)).getMember().getCaptain().getPersonalityAPI();
                        if(personality.getId().equals("timid") || personality.getId().equals("cautious")){
                            if (enemy.getFluxTracker().isOverloaded() && enemy.getFluxTracker().getOverloadTimeRemaining() > ship.getFluxTracker().getTimeToVent()) {
                                continue;
                            }
                            if (enemy.getFluxTracker().isVenting() && enemy.getFluxTracker().getTimeToVent() > ship.getFluxTracker().getTimeToVent()) {
                                continue;
                            }
                        }
                    }

                    switch (enemy.getHullSize()) {
                        case CAPITAL_SHIP:
                            dangerFactor+= Math.max(0, 3f -(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                            break;
                        case CRUISER:
                            dangerFactor+= Math.max(0, 2.25f -(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                            break;
                        case DESTROYER:
                            dangerFactor+= Math.max(0, 1.5f -(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                            break;
                        case FRIGATE:
                            dangerFactor+= Math.max(0, 1f -(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                            break;
                        default:
                            dangerFactor+= Math.max(0, 0.5f -(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/640000));
                            break;
                    }
                }

//                //situational help
//                float helpFactor = 0;
//                List<ShipAPI> nearbyAllies = AIUtils.getNearbyAllies(ship, 2000f);
//                for (ShipAPI ally : nearbyAllies) {
//                    if (ally.getHullSize()==ShipAPI.HullSize.CAPITAL_SHIP){
//                        helpFactor+= Math.max(0,2-(MathUtils.getDistanceSquared(ally.getLocation(), ship.getLocation())/1000000));
//                    } else if (ally.getHullSize()==ShipAPI.HullSize.CRUISER){
//                        helpFactor+= Math.max(0,2-(MathUtils.getDistanceSquared(ally.getLocation(), ship.getLocation())/800000));
//                    } else if (ally.getHullSize()==ShipAPI.HullSize.DESTROYER){
//                        helpFactor+= Math.max(0,2-(MathUtils.getDistanceSquared(ally.getLocation(), ship.getLocation())/600000));
//                    } else if (ally.getHullSize()==ShipAPI.HullSize.FRIGATE){
//                        helpFactor+= Math.max(0,2-(MathUtils.getDistanceSquared(ally.getLocation(), ship.getLocation())/400000));
//                    }
//                }
//
//                float decisionLevel= ventingNeed * hullFactor * ((helpFactor+1)/(dangerFactor+1));
                float decisionLevel = (ventingNeed * hullFactor + 1) / (dangerFactor + 1);

                if (decisionLevel >= 1.5f || (ship.getFluxTracker().getFluxLevel() > 0.1f && dangerFactor == 0)) {
                    ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                }
            }
        }
    }
}