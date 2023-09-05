 
package data.hullmods;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.*;

public class sd_customai extends BaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.removeListenerOfClass(sd_aiListener.class);
        ship.addListener(new sd_aiListener(ship));
    }

    boolean runOnce = false;
    float maxRange= 0;
    final IntervalUtil timer = new IntervalUtil (0.5f, 1.5f);

    public class sd_aiListener implements AdvanceableListener { //SCY's venting code
        protected ShipAPI ship;
        public sd_aiListener(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {

            if (Global.getCombatEngine().isPaused() || ship.getShipAI() == null)
                return;

            if (!runOnce) {
                runOnce = true;
                List<WeaponAPI> loadout = ship.getAllWeapons();
                if (loadout != null) {
                    for (WeaponAPI w : loadout) {
                        if (w.getType() != WeaponAPI.WeaponType.MISSILE) {
                            if (w.getRange() > maxRange) {
                                maxRange = w.getRange();
                            }
                        }
                    }
                }
                timer.randomize();
            }

            //TODO: ADD A FUNCTION THAT MAKES SHIPS SCARED OF ENEMIES WITH UNFIRED STRIKE WEAPONS
            //TODO: MAKE BATTLECARRIERS STOP GETTING SLAPPED WITH A DO NOT PURSUE FLAG
            //TODO: CUSTOM REAPER TORPEDO AI

            //////////////////////////////////////////////////////////
            //ALLEVIATES SHIPS FLUXING THEMSELVES WITH BURST WEAPONS//
            //////////////////////////////////////////////////////////
            if (ship.getShipTarget() != null && ship.getFluxLevel() > 0.25 && ship.getShipTarget().getHullLevel() > 0.25)
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (weapon.getFluxCostToFire() + ship.getFluxTracker().getCurrFlux() > ship.getFluxTracker().getMaxFlux() * 0.85)
                        weapon.setForceNoFireOneFrame(true);
                }

            ///////////////////////////////////////////////
            //FIXES SUICIDING FIGHTERS AND TIMID BEHAVIOR//
            ///////////////////////////////////////////////
            //ok so if our fighters aren't all healthy and our replacement rate isn't high, then we regroup until they are, unless the target is getting super fucked
            //if (ship.getWingLeader() != null && !ship.getWingLeader().getShipTarget().getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP))
//                } else if (pullback && ship.getSharedFighterReplacementRate() == 1) {
//                    ship.setPullBackFighters(false);
//                    pullback = false;
//                }
            if (ship.getSharedFighterReplacementRate() < 0.85) {
                ship.setPullBackFighters(true);
                ship.giveCommand(ShipCommand.PULL_BACK_FIGHTERS, null, -1);
            }

            /////////////////////////////////////
            //FIXES BATTLECARRIERS RUNNING AWAY// IF OUR FIGHTERS ARE BEING AGGRESSIVE THEN WE WANT TO BE AGGRESSIVE
            ///////////////////////////////////// TODO: SWAP CAPTAIN TO AGGRESSIVE AND CHECK IF THIS CODE IS STILL NECESSARY
            if (ship.hasLaunchBays() && ship.getVariant().getHints().contains(ShipHullSpecAPI.ShipTypeHints.COMBAT))
                if (ship.getSharedFighterReplacementRate() > 0.85 && !ship.isPullBackFighters())
                    ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE);

            /////////////////////////////////////////
            //FIXES SHOOTING STRIKE AT PHASED SHIPS// THIS TOOK 5 HOURS IN TOTAL FOR ME TO MAKE THROUGH VARIOUS ITERATIONS AND DEBUGGING BTW
            ///////////////////////////////////////// TODO: DEBUG THIS FOR MODDED PHASE SYSTEMS LIKE SIERRA
            boolean isPhaseEnemy = false;
            for (ShipAPI enemy : AIUtils.getEnemiesOnMap(ship)) {
                if (enemy.isPhased()) { //first, check if the enemy even has phase ships, so we don't run code frivilously
                    isPhaseEnemy = true;
                    break;
                }
            }

            if (isPhaseEnemy) {
                if (ship.getShipTarget() != null && ship.getShipTarget().isPhased() && ship.getShipTarget().getFluxLevel() > 0.85 && ship.getFluxLevel() < 0.75) {
                    ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF);  //REALLY horny to get overextended phase ships
                    ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.PURSUING);
                }

                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    switch (weapon.getType()) { //NPEs can eat my ass
                        case STATION_MODULE:
                        case LAUNCH_BAY:
                        case DECORATIVE:
                        case SYSTEM:
                            continue;
                    }

                    if (weapon.getCooldown() > 1) { //if the weapon is ammo-limited or refires slowly, don't shoot it at phased ships
                        // ^ weapon.usesAmmo() || figure out how to incorporate this but still allow PD to engage missiles
                        if (ship.getShipTarget() != null && ship.getShipTarget().isPhased() && ship.getShipTarget().getFluxLevel() < 0.9)
                            weapon.setForceNoFireOneFrame(true);
                        if (!ship.getWeaponGroupFor(weapon).isAutofiring()) //need this to avoid a NPE when the weapon isn't autofiring
                            continue;
                        ShipAPI autofireAITarget = ship.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip(); //autofire is an entirely separate AI from the main ship
                        if (autofireAITarget != null && autofireAITarget.isPhased() && autofireAITarget.getFluxLevel() < 0.9)
                            weapon.setForceNoFireOneFrame(true);
                    }
                }
            }

            timer.advance(amount);
            if (timer.intervalElapsed()) {
                if (ship.getFluxTracker().isOverloadedOrVenting() || ship.getFluxTracker().getFluxLevel() < 0.2 || ship.getSystem().isActive())
                    return;

                MissileAPI closest = AIUtils.getNearestEnemyMissile(ship);
                if (closest != null && MathUtils.isWithinRange(ship, closest,500))
                    return;

                for (WeaponAPI wep : ship.getAllWeapons()) {
                    if (wep.isFiring() && wep.isInBurst())
                        return;
                }

//                if ( ship.getFluxTracker().getFluxLevel() < 0.5 && AIUtils.getNearbyEnemies(ship, maxRange).size() > 0) {
//                    return;
//                }

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

    @Override
    public boolean affectsOPCosts() { return true; }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) { return false; }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("This ship has improved AI. Also you shouldn't see this.", 10f, Misc.getHighlightColor(), "you shouldn't see this");
    }
}
