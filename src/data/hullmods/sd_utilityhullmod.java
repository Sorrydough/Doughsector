package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.loading.specs.EngineSlot;
import data.util.sd_util;
import data.shipsystems.sd_mnemonicarmor;
import lunalib.lunaSettings.LunaSettings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.*;
import java.util.List;

import static com.fs.starfarer.api.combat.ShipEngineControllerAPI.*;
import static data.util.StarficzAIUtils.*;

public class sd_utilityhullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new sd_utilityhullmodListener(ship));
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
    static class sd_utilityhullmodListener implements AdvanceableListener {
        private final ShieldAPI shield;
        final ShipAPI ship;
        final CombatEngineAPI engine;
        boolean enabled = true;
        public sd_utilityhullmodListener(ShipAPI ship) {
            this.ship = ship;
            this.engine = Global.getCombatEngine();
            this.shield = ship.getShield();
            if (Global.getSettings().getModManager().isModEnabled("lunalib"))
                this.enabled = Boolean.parseBoolean(LunaSettings.getString("sd_doughsector", "sd_enableAITweaks"));
        }
        boolean runOnce = true;
        boolean fixEngines = true;
        String personality = Personalities.AGGRESSIVE;

        IntervalUtil debugInterval = new IntervalUtil(1.5f, 1.5f);
        @Override
        public void advance(float amount) {

            if (fixEngines && sd_util.isCombatSituation(ship)) {
                for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                    EngineSlot slot = (EngineSlot) engine.getEngineSlot();
                    slot.setSystemActivated(false);
                }
                fixEngines = false;
            }

            if (!sd_util.isCombatSituation(ship) || !enabled || ship.getShipAI() == null)
                return;

            if (runOnce) {
                if (Global.getCombatEngine().isSimulation() && ship.getHullSize() != ShipAPI.HullSize.CAPITAL_SHIP) {
                //if (Global.getCombatEngine().isSimulation()) {
                        ship.getCaptain().setPersonality(personality);
                        Console.showMessage("Personality for "+ ship.getName() +" overriden to "+ personality);
                }
                // apply alex's AI behavior overrides
                ship.getShipAI().getConfig().alwaysStrafeOffensively = true;
                ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = false;

                runOnce = false;
            }

//            debugInterval.advance(amount);
//            if (debugInterval.intervalElapsed())
//                if (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_POTENTIAL_MINE_TRIGGER_NEARBY))
//                    engine.addFloatingText(ship.getLocation(), "Mine trigger!", 50, Color.WHITE, ship, 0, 0);


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

            ////////////////////////////
            //FIXES SUICIDING FIGHTERS// IF OUR REPLACEMENT RATE SUCKS THEN PRESERVING IT SHOULD BE OUR TOP PRIORITY
            //////////////////////////// ALSO RECALL OUR FIGHTERS IF AI JANK IS PUTTING US OUT OF POSITION
            if (ship.hasLaunchBays() && (ship.getSharedFighterReplacementRate() < 0.85 || (ship.getFluxLevel() < 0.2 && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF))))
                ship.setForceCarrierPullBackTime(1);

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
                    ShipAPI target = ship.getShipTarget();
                    if (ship.getWeaponGroupFor(weapon).isAutofiring())
                        target = ship.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip();

                    if (target != null && (target.getShield() == null || target.getHullSize() == ShipAPI.HullSize.FRIGATE)) {
                        weapon.setForceNoFireOneFrame(true);
                        if (weapon.isInBurst() && (ship.getFluxLevel() > 0.05 && (ship.getFluxLevel() < 0.15 || !ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))))
                            ship.giveCommand(ShipCommand.VENT_FLUX, null, -1);
                    }
                }
            }

            /////////////////////////////////////////////////
            //FIXES SHOOTING STRIKE WEAPONS AT PHASED SHIPS// THIS TOOK 8 HOURS IN TOTAL BTW
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
                    if (!ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE) && ship.getShield() != null)
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

                    // if the weapon is low rof or our flux is empty, don't shoot at phased ships
                    if ((weapon.getCooldown() > 1 || weapon.usesAmmo() || ship.getFluxLevel() < 0.1) && !weapon.hasAIHint(WeaponAPI.AIHints.PD)) {
                        ShipAPI target = ship.getShipTarget();

                        if (ship.getWeaponGroupFor(weapon).isAutofiring())
                            target = ship.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip();

                        if (target != null && target.isPhased() && target.getFluxLevel() < 0.95)
                            weapon.setForceNoFireOneFrame(true); // fun fact, this doesn't work if you put it into an EFS. Only works in an advanceable listener. No idea why.
                    }
                }
            }

            ////////////////////////////////////////////////////////
            // CUSTOM VENT AND ARMORTANKING CODE BEYOND THIS PART //
            // USES STARFICZ'S AI UTILS ////////////////////////////
            ////////////////////////////////////////////////////////
            float unfoldTime = 0;
            if (shield != null)
                unfoldTime = shield.getUnfoldTime();
            incomingHitsTracker.advance(amount);
            if (incomingHitsTracker.intervalElapsed()) {
                lastUpdatedTime = engine.getTotalElapsedTime(false);
                potentialHitsForVenting = generatePredictedWeaponHits(ship, ship.getLocation(), unfoldTime + ship.getFluxTracker().getTimeToVent());
                potentialHitsForVenting.addAll(incomingProjectileHits(ship, ship.getLocation()));
                if (shield != null) {
                    potentialHitsForShield = new ArrayList<>();
                    for (FutureHit hit : potentialHitsForVenting)
                        if (hit.timeToHit <= ship.getShield().getUnfoldTime() + (shield.getUnfoldTime() / 3)) // slight buffer factor so the ship isn't forced to be as risky as physically possible with shield use
                            potentialHitsForShield.add(hit);
                }
            }

            boolean isWeaponFiring = false;
            for (WeaponAPI weapon : ship.getAllWeapons()) { // wep.isinburst doesn't work for tachyon lance
                if (weapon.isFiring() && (weapon.isBurstBeam() || weapon.isInBurst())) {
                    isWeaponFiring = true;
                    break;
                }
            }
            if (!isWeaponFiring && isDamageAcceptable(ship, potentialHitsForVenting)) {
                if (ship.getFluxLevel() > 0.2)
                    ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }

            if (shield != null) {
                if (isDamageAcceptable(ship, potentialHitsForShield)) {
                    if (shield.isOn())
                        ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    if (shield.isOff())
                        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                }
            }
        }
        private final IntervalUtil incomingHitsTracker = new IntervalUtil(0.05f, 0.05f);
        public float lastUpdatedTime = 0;
        public float lastShieldOnTime = 0;
        public List<FutureHit> potentialHitsForVenting = new ArrayList<>();
        public List<FutureHit> potentialHitsForShield = new ArrayList<>();

        public boolean isDamageAcceptable(ShipAPI ship, List<FutureHit> potentialHits) {
            if (isScriptedNonsenseNearby(ship))
                return false;
            // calculate how much damage the ship would take if shields went down
            float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
            float timeElapsed = currentTime - lastUpdatedTime;
            float armor = getCurrentArmorRating(ship);

            float unfoldTime = shield.getUnfoldTime();
            float bufferTime = unfoldTime / 3;
            if (shield.isOn())
                lastShieldOnTime = currentTime;
            float delayTime = Math.max(0.5f - (currentTime - lastShieldOnTime), 0f);

            float armorAfterIncoming = armor;
            float incomingHullDamage = 0;
            float incomingEMPDamage = 0;

            for (FutureHit hit : potentialHits) {
                float timeToBlock = unfoldTime + delayTime;
                float timeToHit = (hit.timeToHit - timeElapsed);
                if (timeToHit < -0.1f)
                    continue; // skip hits that have already happened
                if (timeToHit <= (timeToBlock + bufferTime)) {
                    Pair<Float, Float> trueDamage = damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armorAfterIncoming, ship);
                    armorAfterIncoming = Math.max(armorAfterIncoming - trueDamage.one, 0);
                    incomingHullDamage += trueDamage.two;
                    incomingEMPDamage += hit.empDamage;
                }
            }

            boolean isArmorDamageAcceptable = (armorAfterIncoming > armor * 0.9) && !sd_mnemonicarmor.isArmorGridDestroyed(ship.getArmorGrid());
            boolean isHullDamageAcceptable = incomingHullDamage < ship.getHitpoints() * 0.05;

            boolean isEMPDamageAcceptable = true;
            if (!ship.getEngineController().getShipEngines().isEmpty()) {
                List<ShipEngineAPI> thrusters = ship.getEngineController().getShipEngines();
                sortByContribution(thrusters);
                int i = 0;
                for (ShipEngineAPI thruster : thrusters) {
                    if (i < Math.ceil((double) thrusters.size() / 2)) {
                        if (thruster.getHitpoints() < incomingEMPDamage) {
                            isEMPDamageAcceptable = false;
                            break;
                        }
                        i++;
                    }
                }
            }

            return isHullDamageAcceptable && isArmorDamageAcceptable && isEMPDamageAcceptable;
        }
        public static void sortByContribution(final List<ShipEngineAPI> thrusters) {
            thrusters.sort((thruster1, thruster2) -> {
                float contribution1 = thruster1.getContribution();
                float contribution2 = thruster2.getContribution();
                return Float.compare(contribution2, contribution1);
            });
        }
        public boolean isScriptedNonsenseNearby(ShipAPI ship) {
            // check for scripted ship systems
            Iterator<Object> iterator = engine.getAiGridShips().getCheckIterator(ship.getLocation(), 2500, 2500);
            while (iterator.hasNext()) {
                Object nextObject = iterator.next();
                if (nextObject instanceof ShipAPI) {
                    ShipAPI thing = (ShipAPI) nextObject;
                    if (thing.getOwner() == ship.getOwner() || thing.getSystem() == null || systemsExcluded.contains(thing.getSystem().getId()))
                        continue;

                    if (thing.getSystem().getSpecAPI().getThreatAmount() > 0 && MathUtils.getDistance(ship, thing) < thing.getSystem().getSpecAPI().getThreatRange(thing.getMutableStats()) * 1.2)
                        return true;
                }
            }
            //check for mines
            iterator = engine.getAiGridMissiles().getCheckIterator(ship.getLocation(), 2500, 2500);
            while (iterator.hasNext()) {
                Object nextObject = iterator.next();
                if (nextObject instanceof MissileAPI) {
                    MissileAPI thing = (MissileAPI) nextObject;
                    if (thing.getOwner() == ship.getOwner() || !thing.isMine())
                        continue;

                    if (MathUtils.getDistance(ship, thing) < thing.getMineExplosionRange() * 1.2)
                        return true;
                }
            }
            return false;
        }
        final List<String> systemsExcluded = new ArrayList<>(); { // todo: check vayra dmods
            systemsExcluded.add("mine_strike");
            systemsExcluded.add("drone_strike");
        }
    }
}
