package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.graphics.sd_decoSystemRangePlugin;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class sd_utilityhullmod extends BaseHullMod {
    List<String> decoSystemRange = Arrays.asList("sd_hackingsuite", "sd_nullifier");
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        Global.getCombatEngine().addPlugin(new sd_utilityhullmodPlugin(ship));
        if (decoSystemRange.contains(ship.getSystem().getId()))
            Global.getCombatEngine().addPlugin(new sd_decoSystemRangePlugin(ship));
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Utility hullmod to make ships look and behave correctly. You shouldn't be able to see this.", 5f,
                Misc.getHighlightColor(), "You shouldn't be able to see this.");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
    static class sd_utilityhullmodPlugin extends BaseEveryFrameCombatPlugin {
        final ShipAPI ship;
        final CombatEngineAPI engine;
        public sd_utilityhullmodPlugin(ShipAPI ship) {
            this.ship = ship;
            this.engine = Global.getCombatEngine();
        }
        final IntervalUtil timer = new IntervalUtil (0.5f, 1.5f);
        final boolean applyAI = false;
        boolean runOnce = true;
        float maxRange = 0;
        String personality = Personalities.AGGRESSIVE;
        //    AdmiralAIPlugin admiral = new sd_fleetAdmiralAI();
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused() || ship.getShipAI() == null)
                return;
            if (runOnce) {
//                if (applyAI && Global.getCombatEngine().isMission() && Global.getCombatEngine().getFleetManager(ship.getOwner()).getAdmiralAI() != admiral) {
//                    Global.getCombatEngine().getFleetManager(ship.getOwner()).setAdmiralAI(admiral);
//                }
                if (Global.getCombatEngine().isSimulation() && ship.getHullSize() != ShipAPI.HullSize.CAPITAL_SHIP) {
                    ship.getCaptain().setPersonality(personality);
                    Console.showMessage("Personality for "+ ship.getName() +" overriden to "+ personality);
                }
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
                // apply alex's AI behavior overrides
                if (ship.getHullSize().ordinal() > 3) {
                    ship.getShipAI().getConfig().alwaysStrafeOffensively = true;
                } else {
                    ship.getShipAI().getConfig().alwaysStrafeOffensively = true;
                    ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = false;
                }
                runOnce = false;
                timer.randomize();
            }

            ////////////////////////////////////////////////////////
            //INCREDIBLY SIMPLE VENTING BEHAVIOR TO KEEP FLUX DOWN//
            //////////////////////////////////////////////////////// TODO: IMPROVE THIS BEHAVIOR TO ONLY HAPPEN WHEN THE TARGET IS TOO FAR AWAY TO BE THREATENING
//            if (ship.getHardFluxLevel() > 0.1 && ship.getHardFluxLevel() < 0.2 && !ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE) && !ship.getSystem().isActive()) {
//                for (WeaponAPI weapon : ship.getAllWeapons()) {
//                    if (weapon.isInBurst())
//                        break;
//                    ship.giveCommand(ShipCommand.VENT_FLUX, null, -1);
//                }
//            }

            ////////////////////////////
            //IMPROVES SQUALL BEHAVIOR//
            ////////////////////////////
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                switch (weapon.getType()) { //NPEs can eat my ass
                    case STATION_MODULE:
                    case LAUNCH_BAY:
                    case DECORATIVE:
                    case SYSTEM:
                        continue;
                }

                if (Objects.equals(weapon.getSpec().getWeaponId(), "squall")) {
                    if (ship.getShipTarget() != null && (ship.getShipTarget().getShield() == null || ship.getShipTarget().getHullSize() == ShipAPI.HullSize.FRIGATE)) {
                        weapon.setForceNoFireOneFrame(true);
                        if (weapon.isInBurst() && (ship.getFluxLevel() > 0.05 && (ship.getFluxLevel() < 0.15 || !ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))))
                            ship.giveCommand(ShipCommand.VENT_FLUX, null, -1);
                    }
                    if (!ship.getWeaponGroupFor(weapon).isAutofiring()) //need this to avoid a NPE when the weapon isn't autofiring
                        continue;
                    ShipAPI autofireAITarget = ship.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip(); //autofire is an entirely separate AI from the main ship
                    if (autofireAITarget != null && (autofireAITarget.getShipTarget().getShield() == null || autofireAITarget.getShipTarget().getHullSize() == ShipAPI.HullSize.FRIGATE)) {
                        weapon.setForceNoFireOneFrame(true);
                        if (weapon.isInBurst() && (ship.getFluxLevel() > 0.05 && (ship.getFluxLevel() < 0.15 || !ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))))
                            ship.giveCommand(ShipCommand.VENT_FLUX, null, -1);
                    }
                }
            }

            ////////////////////////////////////////////////////////////
            //TELLS THE SHIP TO BACK OFF IF IT CAN'T SHOOT ITS WEAPONS//
            ////////////////////////////////////////////////////////////
            float biggestFluxCost = 0;
            for (WeaponAPI weapon : ship.getAllWeapons())
                if (weapon.getFluxCostToFire() > biggestFluxCost)
                    biggestFluxCost = weapon.getFluxCostToFire();
            if (biggestFluxCost + ship.getFluxTracker().getCurrFlux() > ship.getFluxTracker().getMaxFlux() * 0.95) {
                ship.getAIFlags().unsetFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF);
            }

            ////////////////////////////////////////////////////////////////
            //ALLEVIATES SMALL SHIPS FLUXING THEMSELVES WITH BURST WEAPONS// todo: improve this behavior to calculate the burst potential of its opponent and keep its flux below that level
            ////////////////////////////////////////////////////////////////
            if (ship.getHullSize() == ShipAPI.HullSize.FRIGATE || ship.getHullSize() == ShipAPI.HullSize.DESTROYER)
                if (ship.getShipTarget() != null && ship.getFluxLevel() > 0.25 && ship.getShipTarget().getHullLevel() > 0.25)
                    for (WeaponAPI weapon : ship.getAllWeapons())
                        if (weapon.getFluxCostToFire() + ship.getFluxTracker().getCurrFlux() > ship.getFluxTracker().getMaxFlux() * 0.85)
                            weapon.setForceNoFireOneFrame(true);

            ///////////////////////////////////////////////////////////
            //FIXES CARRIERS SENDING STRIKE BOMBERS AGAINST DUMB SHIT//
            /////////////////////////////////////////////////////////// TODO: THIS



            // FIXES SHIPS BACKING OFF WHILE VENTING EVEN THOUGH THEY'RE IN NO DANGER
            // TODO: THIS




            ////////////////////////////
            //FIXES SUICIDING FIGHTERS// IF OUR REPLACEMENT RATE SUCKS THEN PRESERVING IT SHOULD BE OUR TOP PRIORITY
            //////////////////////////// TODO: FIGURE OUT WHY THIS JUST DOESN'T WORK, AND FIGURE OUT WHY IT DOESN'T NPE
            if (ship.hasLaunchBays() && ship.getSharedFighterReplacementRate() < 0.85) {
                ship.setPullBackFighters(true);
                ship.giveCommand(ShipCommand.PULL_BACK_FIGHTERS, null, -1);
            }

            /////////////////////////////////////
            //FIXES BATTLECARRIERS RUNNING AWAY// THERE'S AN AI BUG, AND WE NEED TO FIX IT BY TELLING THE CARRIER OT RECALL ITS FIGHTERS IN SPECIFIC CIRCUMSTANCE TO FIX IT
            ///////////////////////////////////// TODO: DEBUG WHY THIS DOESN'T WORK, ALSO ALEX FIXED IT NEXT UPDATE SO I CAN REMOVE IT LATER
//            if (ship.hasLaunchBays() && ship.getVariant().getHints().contains(ShipHullSpecAPI.ShipTypeHints.COMBAT))
//                if (ship.getSharedFighterReplacementRate() > 0.85 && !ship.isPullBackFighters())
//                    ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE);

            /////////////////////////////////////////////////
            //FIXES SHOOTING STRIKE WEAPONS AT PHASED SHIPS// THIS TOOK 5 HOURS IN TOTAL FOR ME TO MAKE THROUGH VARIOUS ITERATIONS AND DEBUGGING BTW
            /////////////////////////////////////////////////
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
                    if (!ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))
                        ship.getShield().toggleOff(); //Combines with later behavior to cause ships to preserve their 0-flux boost when chasing phased ships
                }

                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    switch (weapon.getType()) { //NPEs can eat my ass
                        case STATION_MODULE:
                        case LAUNCH_BAY:
                        case DECORATIVE:
                        case SYSTEM:
                            continue;
                    }

                    if (weapon.getCooldown() > 1 || ship.getFluxLevel() < 0.1) { //if the weapon is low rof or our flux is empty, don't shoot at phase ships
                        //TODO: ^ weapon.usesAmmo() || figure out how to incorporate this but still allow PD to engage missiles
                        if (ship.getShipTarget() != null && ship.getShipTarget().isPhased() && ship.getShipTarget().getFluxLevel() < 0.95)
                            weapon.setForceNoFireOneFrame(true);
                        if (!ship.getWeaponGroupFor(weapon).isAutofiring()) //need this to avoid a NPE when the weapon isn't autofiring
                            continue;
                        ShipAPI autofireAITarget = ship.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip(); //autofire is an entirely separate AI from the main ship
                        if (autofireAITarget != null && autofireAITarget.isPhased() && autofireAITarget.getFluxLevel() < 0.95)
                            weapon.setForceNoFireOneFrame(true);
                    }
                }
            }

            ////////////////////////////////////////////////////
            //SCY'S VENTING CODE, PRACTICALLY UNCUSTOMIZED YET//
            ////////////////////////////////////////////////////
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
}
