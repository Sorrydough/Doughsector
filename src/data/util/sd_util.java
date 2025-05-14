package data.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import data.graphics.sd_decoSystemRangePlugin;
import data.weapons.mote.sd_moteAIScript;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.*;
import java.util.List;


public class sd_util {
    public static final Color factionColor1 = new Color (255,240,205, 55), factionColor2 = new Color (255,240,205,155),
            timeColor1 =  new Color (100,165,255,55), timeColor2 = new Color (100,165,255,155), timeColor3 = new Color (100,165,255,255),
            phaseColor1 = new Color(150,100,255, 55), phaseColor2 = new Color(150,100,255, 155), phaseColor3 = new Color(150,100,255, 255),
            damageColor1 = new Color (255,120,80,55), damageColor2 = new Color (255,120,80,155), damageColor3 = new Color (255,120,80,255),
            healColor1 = new Color (60,210,150,55), healColor2 = new Color (60,210,150,155), healColor3 = new Color (60,210,150,255),
            systemColor1 = new Color (255,250,150,55), systemColor2 = new Color (255,250,150,155), systemColor3 = new Color (255,250,150,255);

    public static boolean isNumberWithinRange(float numberA, float numberB, float deviationPercent) {
        float lowerBound = numberB - (numberB * (deviationPercent / 100));
        float upperBound = numberB + (numberB * (deviationPercent / 100));
        return numberA <= upperBound && numberA >= lowerBound;
    }
    public static boolean isCombatSituation(ShipAPI ship) {
        return Global.getCombatEngine() != null && !Global.getCombatEngine().isPaused() && ship.getOriginalOwner() != -1 && ship.getVariant() != null && ship.isAlive();
    }
    public static void blockWeaponFromFiring(WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship.getShipTarget() != null && (ship.getShipTarget().getShield() == null || ship.getShipTarget().getHullSize() == ShipAPI.HullSize.FRIGATE)) {
            weapon.setForceNoFireOneFrame(true);
            if (weapon.isInBurst() && (ship.getFluxLevel() > 0.05 && (ship.getFluxLevel() < 0.15 || !ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))))
                ship.giveCommand(ShipCommand.VENT_FLUX, null, -1);
        }
        if (!ship.getWeaponGroupFor(weapon).isAutofiring()) //need this to avoid a NPE when the weapon isn't autofiring
            return;
        ShipAPI autofireAITarget = ship.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip(); //autofire is an entirely separate AI from the main ship
        if (autofireAITarget != null && (autofireAITarget.getShipTarget().getShield() == null || autofireAITarget.getShipTarget().getHullSize() == ShipAPI.HullSize.FRIGATE)) {
            weapon.setForceNoFireOneFrame(true);
            if (weapon.isInBurst() && (ship.getFluxLevel() > 0.05 && (ship.getFluxLevel() < 0.15 || !ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))))
                ship.giveCommand(ShipCommand.VENT_FLUX, null, -1);
        }
    }
    public static boolean isAutomated(ShipAPI ship) {
        return ship.getHullSpec().getMinCrew() == 0;
        //return ship.getVariant().getHullMods().contains("automated") || ship.getHullSpec().getMinCrew() == 0
        //        || ship.getHullSpec().getTags().contains("auto_rec") || ship.getCaptain().isAICore();
    }
    public static boolean isLinked(ShipAPI ship) {
        List<String> neural = Arrays.asList("neural_interface", "neural_integrator");
        for (String hullmod : ship.getVariant().getHullMods())
            if (neural.contains(hullmod))
                return true;
        return false;
    }
    public static void modifyShieldArc(ShipAPI target, float goalShieldArc, float effectLevel) {
        // 1. If the target's shield is still unfolding, don't mess with it
        if (target.getShield() == null || target.getShield().isOff() || target.getShield().getActiveArc() < goalShieldArc)
            return;
        // 2. Calculate how quickly the target's shield should be modified
        // Let's say target arc is 90, current arc is 180
        // when effectLevel is 1, arc should be set to 90
        // when effectLevel is 0.5, arc should be set to (135 = 180-90/2)
        target.getShield().setActiveArc(Math.max(goalShieldArc, target.getShield().getActiveArc() - goalShieldArc / (1 / effectLevel)));
    }
    public static float getOptimalRange(ShipAPI ship) { // chatgpt wrote most of this
        float totalDPS = 0;
        float totalWeightedRange = 0;
        float optimalWeaponRange = 500; // default in case the ship has no weapons installed
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getType() == WeaponAPI.WeaponType.MISSILE ||
                    (weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) && !weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD_ALSO)))
                continue; // missiles are really really weird so we need to exclude them, PD can be really high DPS but short range so we exclude that too
            float weaponDPS = weapon.getSpec().getDerivedStats().getDps();
            float weaponRange = weapon.getRange();
            //adjust the weight based on DPS
            totalWeightedRange += weaponRange * weaponDPS;
            totalDPS += weaponDPS;
        }
        if (totalDPS > 0) {
            optimalWeaponRange = totalWeightedRange / totalDPS;
        }
        return optimalWeaponRange;
    }
    public static void activateSystem(ShipAPI ship, String systemID, float desirePos, float desireNeg, boolean debug) {
        float desireTotal = desirePos + desireNeg;
        if (debug)
            Console.showMessage("Ship:"+ ship.getName() +" Total: "+ Math.round(desireTotal) +" Pos: "+ Math.round(desirePos) +" Neg: "+ Math.round(desireNeg));

        if (ship.getPhaseCloak() != null && Objects.equals(ship.getPhaseCloak().getId(), systemID)) {
            if (desireTotal >= 100 && !ship.getPhaseCloak().isOn())
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, -1);
            if (desireTotal <= 0 && ship.getPhaseCloak().isOn())
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, -1);
        } else {
            if (desireTotal >= 100 && !ship.getSystem().isOn())
                ship.giveCommand(ShipCommand.USE_SYSTEM, null, -1);
            if (desireTotal <= 0 && ship.getSystem().isOn())
                ship.giveCommand(ShipCommand.USE_SYSTEM, null, -1);
        }
    }
    public static void emitMote(ShipAPI ship, Object module, boolean emitOne) {
        final Map<WeaponSize, Integer> AMOUNT = new HashMap<>(); {
            AMOUNT.put(WeaponSize.SMALL, 1);
            AMOUNT.put(WeaponSize.MEDIUM, 2);
            AMOUNT.put(WeaponSize.LARGE, 4);
        }
        final Random rand = new Random();
        if (module instanceof WeaponAPI) {
            WeaponAPI weapon = (WeaponAPI) module;
            int amount = AMOUNT.get(weapon.getSize());
            if (emitOne)
                amount = 1;
            for (int i = 0; i < amount; i++) {
                int angleOffset = rand.nextInt(181) - 90;
                float modifiedAngle = weapon.getSlot().getAngle() + angleOffset;
                MissileAPI mote = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "motelauncher", weapon.getLocation(), modifiedAngle + ship.getFacing(), ship.getVelocity());
                Global.getSoundPlayer().playSound("system_flare_launcher_active", 1.0f, 1.6f, weapon.getLocation(), ship.getVelocity());
                mote.setMissileAI(new sd_moteAIScript(mote));
                mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
                mote.setEmpResistance(10000);
                //data.motes.add(mote);
            }
        } else if (module instanceof ShipEngineAPI) {
            ShipEngineAPI vroom = (ShipEngineAPI) module;
            float size = vroom.getEngineSlot().getWidth();
            int amount = (int) Math.ceil(Math.sqrt(size));
            if (emitOne)
                amount = 1;
            for (int i = 0; i < amount; i++) {
                int angleOffset = rand.nextInt(181) - 90;
                float modifiedAngle = vroom.getEngineSlot().getAngle() + angleOffset;
                MissileAPI mote = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "motelauncher", vroom.getLocation(), modifiedAngle + ship.getFacing(), ship.getVelocity());
                Global.getSoundPlayer().playSound("system_flare_launcher_active", 1.0f, 1.6f, vroom.getLocation(), ship.getVelocity());
                mote.setMissileAI(new sd_moteAIScript(mote));
                mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
                mote.setEmpResistance(10000);
                //data.motes.add(mote);
            }
        }
    }
    public static void applySystemRangeDeco(ShipAPI ship) {
        if (!ship.getCustomData().containsKey("sd_decoSystemRange")) {
            ship.getCustomData().put("sd_decoSystemRange", -1);
            Global.getCombatEngine().addPlugin(new sd_decoSystemRangePlugin(ship));
        }
    }
    public static void sortByDistance(final ShipAPI ship, final List<ShipAPI> ships, final boolean closestFirst) {
        Collections.sort(ships, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI ship1, ShipAPI ship2) {
                float distance1 = MathUtils.getDistance(ship, ship1);
                float distance2 = MathUtils.getDistance(ship, ship2);
                if (closestFirst)
                    return Float.compare(distance1, distance2);
                return Float.compare(distance2, distance1);
            }
        });
    }
    public static boolean canUseSystemThisFrame(ShipAPI ship) { // modification of the AIUtils function, this one also works for toggle systems
        FluxTrackerAPI flux = ship.getFluxTracker();
        ShipSystemAPI system = ship.getSystem();
        return !(system == null || flux.isOverloadedOrVenting() || system.isOutOfAmmo() || ship.getOriginalOwner() == -1
                // active but can't be toggled off
                || (system.isActive() && !system.getSpecAPI().isToggle())
                // chargedown
                || (system.getState() == ShipSystemAPI.SystemState.OUT)
                // cooling down
                || !system.isActive() && system.getCooldownRemaining() > 0
                // fluxed out
                || !system.isActive() && (system.getFluxPerUse() > (flux.getMaxFlux() - flux.getCurrFlux())));
                // venting?
                //|| flux.isVenting());
    }
}