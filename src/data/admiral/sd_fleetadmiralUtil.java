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
    public static class battlestateTracker { // this class doesn't do anything per se, it's just an object that holds data to be accessed by other classes
        public CombatEngineAPI engine;
        public CombatFleetManagerAPI allyFleetManager;
        public CombatFleetManagerAPI enemyFleetManager;
        public CombatTaskManagerAPI allyTaskManager;
        public CombatTaskManagerAPI enemyTaskManager;
        public final java.util.List<ShipAPI> deployedShips = new ArrayList<>();
        public final java.util.List<ShipAPI> deployedAllyShips = new ArrayList<>();
        public final java.util.List<ShipAPI> deployedEnemyShips = new ArrayList<>();
        public final HashMap<CombatFleetManagerAPI.AssignmentInfo, Object> assignmentsWithTargets = new HashMap<>();
        public final HashMap<ShipAPI, CombatFleetManagerAPI.AssignmentInfo> shipsWithTargetAssignments = new HashMap<>();
        public float deployedAllyThreat;
        public float deployedEnemyThreat;
        public int numOwnedObjectives;
        public int allySide;
        public int enemySide;
        public float averageAllySpeed;
        public float averageEnemySpeed;
        boolean runOnce = true;
        void reset() {
            assignmentsWithTargets.clear();
            shipsWithTargetAssignments.clear();
            deployedShips.clear();
            deployedAllyShips.clear();
            deployedEnemyShips.clear();
            deployedAllyThreat = 0;
            deployedEnemyThreat = 0;
            numOwnedObjectives = 0;
            averageAllySpeed = 0;
            averageEnemySpeed = 0;
        }
        public void updateState(CombatEngineAPI combatEngine, int allyOwner, int enemyOwner) {
            // need to reset all the fields that we don't want to preserve between updates
            reset();
            // updateState gets called and we populate the fields, then we can pass the object to other classes for them to also get that info
            // doing it this way so stuff doesn't have to get recalculated repeatedly and I don't have 10,000 static classes in a util the size of your mom
            if (runOnce) {
                engine = combatEngine;
                allySide = allyOwner;
                enemySide = enemyOwner;
                allyFleetManager = engine.getFleetManager(allySide);
                allyTaskManager = allyFleetManager.getTaskManager(false);
                enemyFleetManager = engine.getFleetManager(enemySide);
                enemyTaskManager = enemyFleetManager.getTaskManager(false);
                runOnce = false;
            }

            for (ShipAPI ship : engine.getShips())
                if (sd_fleetadmiralUtil.isActualShip(ship))
                    deployedShips.add(ship);

            for (BattleObjectiveAPI objective : engine.getObjectives())
                if (objective.getOwner() == allySide)
                    numOwnedObjectives += 1;

            boolean isPlayer = allySide == 0; // todo: fix this so the admiral works when the AI reinforces a player fleet
            for (ShipAPI ship : deployedShips) {
                if (!isPlayer && ship.getOwner() == allySide) {
                    deployedAllyShips.add(ship);
                    deployedAllyThreat += sd_fleetadmiralUtil.getCombatEffectiveness(ship, 0.2f);
                } else if (isPlayer && ship.getOwner() == allySide && !ship.isAlly()) { // making sure we don't confuse reinforcing ships for the player's ships
                    deployedAllyShips.add(ship);
                    deployedAllyThreat += sd_fleetadmiralUtil.getCombatEffectiveness(ship, 0.2f);
                } else if (ship.getOwner() == enemySide) {
                    deployedEnemyShips.add(ship);
                    deployedEnemyThreat += sd_fleetadmiralUtil.getCombatEffectiveness(ship, 0.2f);
                }
            }
            sd_fleetadmiralUtil.sortByDeploymentCost(deployedAllyShips);
            sd_fleetadmiralUtil.sortByDeploymentCost(deployedEnemyShips);

            for (ShipAPI ship : deployedAllyShips)
                averageAllySpeed += ship.getMaxSpeed();
            averageAllySpeed /= Math.max(1, deployedAllyShips.size());

            for (ShipAPI ship : deployedEnemyShips)
                averageEnemySpeed += ship.getMaxSpeed();
            averageEnemySpeed /= Math.max(1, deployedEnemyShips.size());

            for (CombatFleetManagerAPI.AssignmentInfo assignment : allyTaskManager.getAllAssignments())
                assignmentsWithTargets.put(assignment, getAssignmentTarget(assignment.getTarget()));

            for (ShipAPI ship : deployedAllyShips)
                if (allyTaskManager.getAssignmentFor(ship) != null)
                    shipsWithTargetAssignments.put(ship, allyTaskManager.getAssignmentFor(ship));

            //for (battleGridSquare square : )


        }
        //        static class battleGridSquare {
//            List<ShipAPI> allyShips = new ArrayList<>();
//            List<ShipAPI> enemyShips = new ArrayList<>();
//            float allyThreat = 0;
//            float enemyThreat = 0;
//
//            public void addAllyShip(ShipAPI ship) {
//                allyShips.add(ship);
//            }
//
//            public void removeShip(ShipAPI ship) {
//                enemyShips.remove(ship);
//            }
//
////        public List<String> getShips() {
////            return ships;
////        }
//
//            public List<ShipAPI> getAllyShips() {
//                return allyShips;
//            }
//            public List<ShipAPI> getEnemyShips() {
//                return enemyShips;
//            }
//            public float getEnemyThreat() {
//                return enemyThreat;
//            }
//            public float getAllyThreat() {
//                return allyThreat;
//            }
//
//
//
//        }
        public CombatFleetManagerAPI getFleetmanager(int owner) {
            if (owner == allySide)
                return allyFleetManager;
            if (owner == enemySide)
                return enemyFleetManager;
            return null;
        }
        public Object getAssignmentTarget(AssignmentTargetAPI assignment) {
            if (assignment instanceof DeployedFleetMemberAPI)
                return ((DeployedFleetMemberAPI) assignment).getShip();
            if (assignment instanceof BattleObjectiveAPI)
                return assignment;
            return null;
        }
        public java.util.List<ShipAPI> getShipsAssigned(CombatFleetManagerAPI.AssignmentInfo assignment) {
            List<ShipAPI> ships = new ArrayList<>();
            for (ShipAPI ship : deployedAllyShips)
                if (allyTaskManager.getAssignmentFor(ship) == assignment)
                    ships.add(ship);
            return ships;
        }
    }
    public static void applyAssignment(AssignmentTargetAPI target, CombatAssignmentType assignment, int owner) {
        Global.getCombatEngine().getFleetManager(owner).getTaskManager(false).createAssignment(assignment, target, false);
    }
    public static float getDeploymentCost(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        return Math.max(stats.getSuppliesToRecover().base, stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).computeEffective(stats.getSuppliesToRecover().modified));
    }

    public static boolean isActualShip(ShipAPI ship) {
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
            for (String whyisthisastring : variant.getHullMods()) {
                // adjust by dmods that actually reduce combat performance
                if (Global.getSettings().getHullModSpec(whyisthisastring).hasTag("dmod") && !dModsExcluded.contains(whyisthisastring)) {
                    modifier -= dmodMod;
                    continue;
                }
                // adjust threat by certain hullmods that overperform
                if (overpoweredHullmods.containsKey(whyisthisastring))
                    modifier += overpoweredHullmods.get(whyisthisastring);
            }
            // and adjust by non-logistic smods
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
        if (ship.getFleetMember().getFleetCommander() != null)
            for (SkillLevelAPI skill : ship.getFleetMember().getFleetCommander().getStats().getSkillsCopy()) {
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
        if (!ship.getAllWeapons().isEmpty()) {
            float totalDPS = 0;
            float workingDPS = 0;
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                totalDPS += weapon.getSpec().getDerivedStats().getDps();
                if (!weapon.isDisabled())
                    workingDPS += weapon.getSpec().getDerivedStats().getDps();
            }
            modifier *= Math.max(0.5, workingDPS / totalDPS);
        }

        // factor disabled engines
        if (!ship.getEngineController().getShipEngines().isEmpty()) {
            float totalThrust = 0;
            float workingThrust = 0;
            for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
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
    public static float getShipStrengthAssigned(CombatFleetManagerAPI.AssignmentInfo assignment, battlestateTracker battleState) {
        float strength = 0;
        for (ShipAPI ship : battleState.deployedAllyShips)
            if (battleState.allyTaskManager.getAssignmentFor(ship) == assignment)
                strength += getCombatEffectiveness(ship, 0.2f);
        return strength;
    }
//    public static float calculateThreatLevel(CombatFleetManagerAPI.AssignmentInfo assignment, float radius, sd_battlestateTracker battleState) {a
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
