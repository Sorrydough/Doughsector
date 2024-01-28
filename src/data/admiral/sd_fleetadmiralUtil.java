package data.admiral;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import java.util.*;


public class sd_fleetadmiralUtil {
    public static void applyAssignment(AssignmentTargetAPI target, CombatAssignmentType assignment, int owner) {
        Global.getCombatEngine().getFleetManager(owner).getTaskManager(false).createAssignment(assignment, target, false);
    }
    public static float getDeploymentCost(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        return Math.max(stats.getSuppliesToRecover().base, stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(stats.getSuppliesToRecover().modified));
    }

    public static boolean isDeployedShip(ShipAPI ship) {
        return !ship.isStationModule() && !ship.isHulk() && !ship.isShuttlePod() && !ship.isFighter();
    }

    public static float getCombatEffectiveness(ShipAPI ship, float minFraction) {
        final Map<String, Integer> overpoweredHullmods = new HashMap<>(); {
            overpoweredHullmods.put("safetyoverrides", 20);
        }
        final List<String> sModsExcluded = new ArrayList<>(); {
            for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs())
                if (hullmod.hasUITag("Logistics"))
                    sModsExcluded.add(hullmod.getId());
        }
        final List<String> dModsExcluded = new ArrayList<>(); { // todo: check vayra dmods
            dModsExcluded.add("degraded_drive_field");
            dModsExcluded.add("malfunctioning_comms");
            dModsExcluded.add("fragile_subsystems");
            dModsExcluded.add("comp_storage");
            dModsExcluded.add("increased_maintenance");
            dModsExcluded.add("erratic_injector");
            dModsExcluded.add("faulty_auto");
            dModsExcluded.add("damaged_mounts");
            dModsExcluded.add("degraded_life_support");
        }

        final int dmodMod = 5;
        final int smodMod = 15;
        final int officerSkillMod = 15;
        final int officerEliteSkillMod = 10;
        float modifier = 100;

        ShipVariantAPI variant = ship.getMutableStats().getVariant();
        if (variant != null) {
            // adjust threat by certain hullmods that overperform
            for (Map.Entry<String, Integer> hullmod : overpoweredHullmods.entrySet())
                if (variant.hasHullMod(hullmod.getKey()))
                    modifier += hullmod.getValue();
            // adjust by dmods that actually reduce combat performance
            for (String whyisthisastring : variant.getHullMods())
                if (Global.getSettings().getHullModSpec(whyisthisastring).hasTag("dmod") && !dModsExcluded.contains(whyisthisastring))
                    modifier -= dmodMod;
            // and adjust by non-logistic hullmods
            for (String whyisthisastring : variant.getSMods())
                if (!sModsExcluded.contains(whyisthisastring))
                    modifier += smodMod;
            // adjust by the captain's skills
            List<SkillLevelAPI> skills = ship.getCaptain().getStats().getSkillsCopy();
            if (ship.isStationModule())
                skills = ship.getParentStation().getCaptain().getStats().getSkillsCopy();
            for (SkillLevelAPI skill : skills) {
                if (skill.getLevel() == 1)
                    modifier += officerSkillMod;
                else if (skill.getLevel() == 2)
                    modifier += officerSkillMod + officerEliteSkillMod;
            }
        }
        // adjust for fleet admiral's skills
        PersonAPI commander = ship.getFleetMember().getFleetCommander();
        if (commander != null)
            for (SkillLevelAPI skill : commander.getStats().getSkillsCopy()) {
                String skillID = skill.getSkill().getId();
                if (Objects.equals(skillID, Skills.TACTICAL_DRILLS)) {
                    modifier += 5;
                    continue;
                }
                if (ship.getCaptain() != null) {
                    if (Objects.equals(skillID, Skills.WOLFPACK_TACTICS)) {
                        switch (ship.getHullSize()) {
                            case FRIGATE:
                                modifier += 20;
                                break;
                            case DESTROYER:
                                modifier += 10;
                                break;
                        }
                        continue;
                    }
                    if (Objects.equals(skillID, Skills.COORDINATED_MANEUVERS))
                        modifier += 10;
                }
            }
        // modify by CR
        if (ship.losesCRDuringCombat()) {
            float CRmod = 0;
            float CR = ship.getCurrentCR();
            if (CR >= 0.7f) { // chatgpt wrote this :)
                CRmod = (CR - 0.7f) / 0.3f * 10;
            } else if (CR <= 0.5) {
                CRmod -= (0.5f - CR) / 0.5f * 10;
            }
            if (CR < 0.4f) // malfunctions start below this point so we multiply the size of the penalty
                CRmod -= CRmod * CRmod;
            modifier += CRmod;
        }

        // factor disabled weapons
        List<WeaponAPI> weapons = ship.getAllWeapons();
        if (!weapons.isEmpty()) {
            float totalDPS = 0;
            float workingDPS = 0;
            for (WeaponAPI weapon : weapons) {
                totalDPS += weapon.getSpec().getDerivedStats().getDps();
                if (!weapon.isDisabled())
                    workingDPS += weapon.getSpec().getDerivedStats().getDps();
            }
            modifier *= Math.max(0.5, workingDPS / totalDPS);
        }

        // factor disabled engines
        List<ShipEngineAPI> engines = ship.getEngineController().getShipEngines();
        if (!engines.isEmpty()) {
            float totalThrust = 0;
            float workingThrust = 0;
            for (ShipEngineAPI engine : engines) {
                totalThrust += engine.getContribution();
                if (!engine.isDisabled())
                    workingThrust += engine.getContribution();
            }
            modifier *= Math.max(0.5, workingThrust / totalThrust);
        }

        // factor flux level
        modifier *= Math.min(1, 1 - (ship.getFluxLevel() + 0.5));

        // factor carrier replenishment
        if (ship.hasLaunchBays())
            modifier *= Math.min(1, 0.45 + ship.getSharedFighterReplacementRate());

        // special code for stations and modules, note that this function calls itself recursively when dealing with them so this part is going to look real goofy
        float deploymentCost = 0;
        if (ship.isShipWithModules()) {
            for (ShipAPI module : ship.getChildModulesCopy())
                deploymentCost += getCombatEffectiveness(module, 0.2f);
        } else if (ship.isStationModule()) {
            ShipAPI parent = ship.getParentStation();
            deploymentCost = getDeploymentCost(parent) / parent.getChildModulesCopy().size();
        } else {
            deploymentCost = getDeploymentCost(ship);
        }
        if (ship.getEngineController().getShipEngines().isEmpty()) // stuff that can't move is undercosted so we have to bump its value up a bit
            deploymentCost *= 1.5f;
        // math.max because a ship's combat effectiveness rating should never go below a specified portion of its DP. We can modify this for armor tanks etc contextually when we call the function
        return deploymentCost * Math.max(minFraction, modifier / 100);
    }
    public static void sortByDeploymentCost(final List<ShipAPI> ships) {
        Collections.sort(ships, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI ship1, ShipAPI ship2) {
                float supplies1 = getDeploymentCost(ship1);
                float supplies2 = getDeploymentCost(ship2);
                return Float.compare(supplies2, supplies1);
            }
        });
    }
    public static float getShipStrengthAssigned(CombatFleetManagerAPI.AssignmentInfo assignment, sd_fleetadmiralController.battlestateTracker battleState) {
        float strength = 0;
        for (ShipAPI ship : battleState.deployedAllyShips)
            if (battleState.allyTaskManager.getAssignmentFor(ship) == assignment)
                strength += getCombatEffectiveness(ship, 0.2f);
        return strength;
    }
//    public static float calculateThreatLevel(CombatFleetManagerAPI.AssignmentInfo assignment, float radius, sd_battlestateTracker battleState) {
//        float threat = 0;
//        List<ShipAPI> assignedToTarget = new ArrayList<>();
//        for (Map.Entry<ShipAPI, CombatFleetManagerAPI.AssignmentInfo> ship : battleState.shipsWithTargetAssignments.entrySet()) {
//            if (ship.getValue() == assignment) {
//                assignedToTarget.add(ship.getKey());
//            }
//        }
//
//
//
//
//
//        for (ShipAPI enemy : battleState.deployedEnemyShips) {
//
//
//        }
//
//
//
//
//
//        return threat; // ur mum
//    }
}
