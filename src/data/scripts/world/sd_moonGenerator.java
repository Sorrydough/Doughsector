package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * code by tomatopaste. adds random moons to specific objects upon generation. majority of code is to assign appropriate conditions
 */
public class sd_moonGenerator implements SectorGeneratorPlugin {
    public static final float MIN_RADIUS_FOR_MOON_GEN = 150;
    public static final float DIVISOR = 105;
    public static final float HABITABLE_PROBABILITY = 8;

    public static final Logger log = Global.getLogger(sd_moonGenerator.class);

    public static final List<String> ringBandTypes = new ArrayList<>();
    static {
        ringBandTypes.add("rings_dust0");
        ringBandTypes.add("rings_asteroids0");
        ringBandTypes.add("rings_ice0");
    }

    public static final List<String> hotPlanetTypes = new ArrayList<>();
    static {
        hotPlanetTypes.add("barren_venuslike");
        hotPlanetTypes.add("lava_minor");
        hotPlanetTypes.add("toxic");
        hotPlanetTypes.add("US_lava");
        hotPlanetTypes.add("US_volcanic");
        hotPlanetTypes.add("US_green");
        hotPlanetTypes.add("US_acid");
        hotPlanetTypes.add("US_acidRain");
        hotPlanetTypes.add("US_acidWind");
    }

    public static final List<String> neutralPlanetTypes = new ArrayList<>();
    static {
        neutralPlanetTypes.add("barren");
        neutralPlanetTypes.add("barren_castiron");
        neutralPlanetTypes.add("barren2");
        neutralPlanetTypes.add("barren3");
        neutralPlanetTypes.add("rocky_metallic");
        neutralPlanetTypes.add("rocky_unstable");
        neutralPlanetTypes.add("barren-bombarded");
        neutralPlanetTypes.add("US_azur");
        neutralPlanetTypes.add("US_burnt");
        neutralPlanetTypes.add("US_dust");
        neutralPlanetTypes.add("US_barrenA");
        neutralPlanetTypes.add("US_barrenB");
        neutralPlanetTypes.add("US_barrenC");
        neutralPlanetTypes.add("US_barrenD");
        neutralPlanetTypes.add("US_barrenE");
        neutralPlanetTypes.add("US_barrenF");
        neutralPlanetTypes.add("US_artificial");
    }

    public static final List<String> habitablePlanetTypes = new ArrayList<>();
    static {
        habitablePlanetTypes.add("jungle");
        habitablePlanetTypes.add("desert1");
        habitablePlanetTypes.add("desert");
        habitablePlanetTypes.add("terran-eccentric");
        habitablePlanetTypes.add("water");
        habitablePlanetTypes.add("barren-desert");
        habitablePlanetTypes.add("US_continent");
        habitablePlanetTypes.add("US_water");
        habitablePlanetTypes.add("US_waterB");
        habitablePlanetTypes.add("US_alkali");
        habitablePlanetTypes.add("US_jungle");
        habitablePlanetTypes.add("US_auric");
        habitablePlanetTypes.add("US_auricCloudy");
        habitablePlanetTypes.add("US_magnetic");
        habitablePlanetTypes.add("US_storm");
        habitablePlanetTypes.add("US_lifelessarid");
        habitablePlanetTypes.add("US_arid");
        habitablePlanetTypes.add("US_crimson");
        habitablePlanetTypes.add("US_desertA");
        habitablePlanetTypes.add("US_desertB");
        habitablePlanetTypes.add("US_desertC");
        habitablePlanetTypes.add("US_red");
        habitablePlanetTypes.add("US_redWind");
    }

    public static final List<String> coldPlanetTypes = new ArrayList<>();
    static {
        coldPlanetTypes.add("rocky_ice");
        coldPlanetTypes.add("cryovolcanic");
        coldPlanetTypes.add("frozen");
        coldPlanetTypes.add("frozen1");
        coldPlanetTypes.add("frozen2");
        coldPlanetTypes.add("frozen3");
        coldPlanetTypes.add("toxic_cold");
        coldPlanetTypes.add("US_blue");
        coldPlanetTypes.add("US_purple");
        coldPlanetTypes.add("US_iceA");
        coldPlanetTypes.add("US_iceB");
    }

    public static final List<String> hotConditions = new ArrayList<>();
    static {
        hotConditions.add(Conditions.HOT);
        hotConditions.add(Conditions.LOW_GRAVITY);
    }

    public static final List<String> randomHotAttributes = new ArrayList<>();
    static {
        randomHotAttributes.add(Conditions.METEOR_IMPACTS);
        randomHotAttributes.add(Conditions.TECTONIC_ACTIVITY);
        hotConditions.add(Conditions.TOXIC_ATMOSPHERE);
    }

    public static final List<String> barrenConditions = new ArrayList<>();
    static {
        barrenConditions.add(Conditions.NO_ATMOSPHERE);
        barrenConditions.add(Conditions.LOW_GRAVITY);
    }

    public static final List<String> randomBarrenAttributes = new ArrayList<>();
    static {
        randomBarrenAttributes.add(Conditions.METEOR_IMPACTS);
        randomBarrenAttributes.add(Conditions.TECTONIC_ACTIVITY);
    }

    public static final List<String> frozenConditions = new ArrayList<>();
    static {
        frozenConditions.add(Conditions.LOW_GRAVITY);
        frozenConditions.add(Conditions.COLD);
    }

    public static final List<String> randomFrozenAttributes = new ArrayList<>();
    static {
        randomFrozenAttributes.add(Conditions.METEOR_IMPACTS);
        randomFrozenAttributes.add(Conditions.TECTONIC_ACTIVITY);
    }

    public static final List<String> habitableConditions = new ArrayList<>();
    static {
        habitableConditions.add(Conditions.HABITABLE);
        habitableConditions.add(Conditions.LOW_GRAVITY);
    }

    public static final List<String> randomHabitableAttributes = new ArrayList<>();
    static {
        randomHabitableAttributes.add(Conditions.INIMICAL_BIOSPHERE);
        randomHabitableAttributes.add(Conditions.TECTONIC_ACTIVITY);
        randomHabitableAttributes.add(Conditions.MILD_CLIMATE);
        randomHabitableAttributes.add(Conditions.EXTREME_WEATHER);
    }

    public static final List<String> randomDerelictThemeAttributes = new ArrayList<>();
    static {
        randomDerelictThemeAttributes.add(Conditions.POLLUTION);
        randomDerelictThemeAttributes.add(Conditions.DECIVILIZED_SUBPOP);
    }

    public static final List<String> ruins = new ArrayList<>();
    static {
        ruins.add(Conditions.RUINS_SCATTERED);
        ruins.add(Conditions.RUINS_WIDESPREAD);
        ruins.add(Conditions.RUINS_EXTENSIVE);
        ruins.add(Conditions.RUINS_VAST);
    }

    public static final List<String> volatiles = new ArrayList<>();
    static {
        volatiles.add(Conditions.VOLATILES_TRACE);
        volatiles.add(Conditions.VOLATILES_DIFFUSE);
        volatiles.add(Conditions.VOLATILES_ABUNDANT);
        volatiles.add(Conditions.VOLATILES_PLENTIFUL);
    }

    public static final List<String> ore = new ArrayList<>();
    static {
        ore.add(Conditions.ORE_SPARSE);
        ore.add(Conditions.ORE_MODERATE);
        ore.add(Conditions.ORE_ABUNDANT);
        ore.add(Conditions.ORE_RICH);
        ore.add(Conditions.ORE_ULTRARICH);
    }

    public static final List<String> rareOre = new ArrayList<>();
    static {
        rareOre.add(Conditions.RARE_ORE_SPARSE);
        rareOre.add(Conditions.RARE_ORE_MODERATE);
        rareOre.add(Conditions.RARE_ORE_ABUNDANT);
        rareOre.add(Conditions.RARE_ORE_RICH);
        rareOre.add(Conditions.RARE_ORE_ULTRARICH);
    }

    public static final List<String> organics = new ArrayList<>();
    static {
        organics.add(Conditions.ORGANICS_TRACE);
        organics.add(Conditions.ORGANICS_COMMON);
        organics.add(Conditions.ORGANICS_ABUNDANT);
        organics.add(Conditions.ORGANICS_PLENTIFUL);
    }

    public static final List<String> uninhabitableConditionsWithAtmosphere = new ArrayList<>();
    static {
        uninhabitableConditionsWithAtmosphere.add(Conditions.TOXIC_ATMOSPHERE);
        uninhabitableConditionsWithAtmosphere.add(Conditions.EXTREME_WEATHER);
    }

    public static final List<String> farmland = new ArrayList<>();
    static {
        farmland.add(Conditions.FARMLAND_POOR);
        farmland.add(Conditions.FARMLAND_ADEQUATE);
        farmland.add(Conditions.FARMLAND_RICH);
        farmland.add(Conditions.FARMLAND_BOUNTIFUL);
    }

    public static final List<String> weakStars = new ArrayList<>();
    static {
        weakStars.add(StarTypes.BROWN_DWARF);
        weakStars.add(StarTypes.RED_DWARF);
        weakStars.add(StarTypes.WHITE_DWARF);
        weakStars.add(StarTypes.YELLOW);
    }

    public static final List<String> habitableSystems = new ArrayList<>();
    static {
        habitableSystems.addAll(weakStars);
        habitableSystems.add(StarTypes.ORANGE);
        habitableSystems.add(StarTypes.ORANGE_GIANT);
    }

    static Random random = new Random();

    private final static TreeMap<Integer, String> map = new TreeMap<Integer, String>();
    static {
        map.put(1000, "M");
        map.put(900, "CM");
        map.put(500, "D");
        map.put(400, "CD");
        map.put(100, "C");
        map.put(90, "XC");
        map.put(50, "L");
        map.put(40, "XL");
        map.put(10, "X");
        map.put(9, "IX");
        map.put(5, "V");
        map.put(4, "IV");
        map.put(1, "I");
    }

    public static String toRoman(int number) {
        int l = map.floorKey(number);
        if (number == l) return map.get(number);
        return map.get(l) + toRoman(number - l);
    }

    @Override
    public void generate(SectorAPI sector) {
        log.info("Adding moons to in-system entities...");
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (system.isProcgen()) {
                CopyOnWriteArrayList<SectorEntityToken> entityTokens = new CopyOnWriteArrayList<>(system.getAllEntities());

                boolean hasRuins = false;
                OUTER:
                for (PlanetAPI planet : system.getPlanets()) {
                    for (String string : ruins) {
                        if (planet.getMarket() != null && planet.getMarket().hasCondition(string)) {
                            hasRuins = true;
                            break OUTER;
                        }
                    }
                }

                for (SectorEntityToken token : entityTokens) {
                    if (token instanceof PlanetAPI) {
                        PlanetAPI planet = (PlanetAPI) token;

                        if (planet.getId().startsWith("sstc_moon")) {
                            continue;
                        }
                        float planetRadius = planet.getRadius();
                        if (planet.isStar()) {
                            continue;
                        } else if (planetRadius < MIN_RADIUS_FOR_MOON_GEN) {
                            continue;
                        }

                        int numMoonsMin = 0;


                        int numMoons;
                        numMoons = random.nextInt (1 + (int) (planetRadius / DIVISOR)) + numMoonsMin;
                        log.info("Adding " + numMoons + " moons to " + planet.getTypeId());

                        boolean isHot = false;
                        boolean isCold = false;
                        if (planet.getMarket().hasCondition(Conditions.HOT) || planet.getMarket().hasCondition(Conditions.VERY_HOT)) {
                            isHot = true;
                        } else if (planet.getMarket().hasCondition(Conditions.COLD) || planet.getMarket().hasCondition(Conditions.VERY_COLD)) {
                            isCold = true;
                        }

                        float orbitRadius = planetRadius * 1.5f;
                        for (int i = 0; i < numMoons; i++) {
                            String id = "sstc_moon_" + i + "_" + planet.hashCode();
                            int radius = random.nextInt(25) + 25;
                            orbitRadius += random.nextInt(50) + radius + 75;

                            int orbitDays = random.nextInt(20) + 20;

                            boolean spawnHabitable = (1 == random.nextInt((int) HABITABLE_PROBABILITY) && habitableSystems.contains(system.getStar().getTypeId()));

                            String type;
                            if (spawnHabitable) {
                                int index = random.nextInt(habitablePlanetTypes.size());
                                type = habitablePlanetTypes.get(index);
                            } else if (isHot) {
                                int index = random.nextInt(hotPlanetTypes.size());
                                type = hotPlanetTypes.get(index);
                            } else if (isCold) {
                                int index = random.nextInt(coldPlanetTypes.size());
                                type = coldPlanetTypes.get(index);
                            } else {
                                int index = random.nextInt(neutralPlanetTypes.size());
                                type = neutralPlanetTypes.get(index);
                            }

                            PlanetAPI moon = system.addPlanet(id, planet, planet.getFullName() + " M-" + toRoman(i + 1), type, planet.getSpec().getPitch(), radius, orbitRadius, orbitDays);

                            MarketAPI market = moon.getMarket();
                            String oreType;
                            String rareOreType;
                            String volatileType;
                            String organicsType;
                            String farmlandType;
                            switch (moon.getTypeId()) {
                                //HABITABLE PLANETS
                                case "jungle":
                                    for (String string : habitableConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomHabitableAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomHabitableAttributes.get(e));
                                        }
                                    }

                                    oreType = getOreType(1, 0, 1);
                                    rareOreType = getRareOreType(1, 0, 0);
                                    organicsType = getOrganicsType(0, 1, 1);
                                    farmlandType = getFarmlandType(0, 1, 1);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }
                                    if (organicsType != null) {
                                        market.addCondition(organicsType);
                                    }
                                    if (farmlandType != null) {
                                        market.addCondition(farmlandType);
                                    }

                                    break;
                                case "desert1":
                                case "desert":
                                    for (String string : habitableConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomHabitableAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomHabitableAttributes.get(e));
                                        }
                                    }
                                    market.addCondition(Conditions.HOT);

                                    oreType = getOreType(1, 0, 0);
                                    rareOreType = getRareOreType(1, 0, 0);
                                    organicsType = getOrganicsType(0, 0, 0);
                                    farmlandType = getFarmlandType(0, 0, 0);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }
                                    if (organicsType != null) {
                                        market.addCondition(organicsType);
                                    }
                                    if (farmlandType != null) {
                                        market.addCondition(farmlandType);
                                    }

                                    break;
                                case "terran-eccentric":
                                    for (String string : habitableConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomHabitableAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomHabitableAttributes.get(e));
                                        }
                                    }
                                    if (random.nextBoolean()) {
                                        market.addCondition(Conditions.COLD);
                                    } else {
                                        market.addCondition(Conditions.HOT);
                                    }
                                    if (weakStars.contains(system.getStar().getTypeId())) {
                                        market.addCondition(Conditions.POOR_LIGHT);
                                    }

                                    oreType = getOreType(1, 1, 0);
                                    rareOreType = getRareOreType(0, 0, 0);
                                    organicsType = getOrganicsType(0, 1, 1);
                                    farmlandType = getFarmlandType(0, 1, 1);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }
                                    if (organicsType != null) {
                                        market.addCondition(organicsType);
                                    }
                                    if (farmlandType != null) {
                                        market.addCondition(farmlandType);
                                    }

                                    break;
                                case "water":
                                    for (String string : habitableConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomHabitableAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomHabitableAttributes.get(e));
                                        }
                                    }
                                    market.addCondition(Conditions.WATER_SURFACE);

                                    oreType = getOreType(1, 1, 0);
                                    rareOreType = getRareOreType(0, -1, 0);
                                    organicsType = getOrganicsType(0, 1, 1);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }
                                    if (organicsType != null) {
                                        market.addCondition(organicsType);
                                    }

                                    break;
                                case "barren-desert":
                                    for (String string : habitableConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomHabitableAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomHabitableAttributes.get(e));
                                        }
                                    }
                                    market.addCondition(Conditions.HOT);

                                    oreType = getOreType(1, 0, 0);
                                    rareOreType = getRareOreType(1, -1, 0);
                                    organicsType = getOrganicsType(1, 1, 0);
                                    farmlandType = getFarmlandType(1, -2, 0);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }
                                    if (organicsType != null) {
                                        market.addCondition(organicsType);
                                    }
                                    if (farmlandType != null) {
                                        market.addCondition(farmlandType);
                                    }

                                    break;
                                //NEUTRAL PLANETS
                                case "barren":
                                case "barren2":
                                case "barren3":
                                    for (String string : barrenConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomBarrenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomBarrenAttributes.get(e));
                                        }
                                    }

                                    oreType = getOreType(2, -1, 0);
                                    rareOreType = getRareOreType(2, -2, 0);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }

                                    break;
                                case "barren_castiron":
                                case "rocky_metallic":
                                    for (String string : barrenConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomBarrenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomBarrenAttributes.get(e));
                                        }
                                    }

                                    oreType = getOreType(0, 1, 1);
                                    rareOreType = getRareOreType(0, 0, 1);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }

                                    break;
                                case "rocky_unstable":
                                    for (String string : barrenConditions) {
                                        market.addCondition(string);
                                    }
                                    market.addCondition(Conditions.TECTONIC_ACTIVITY);

                                    for (int e = 0; e < randomBarrenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomBarrenAttributes.get(e));
                                        }
                                    }

                                    oreType = getOreType(2, 0, 0);
                                    rareOreType = getRareOreType(2, 0, 0);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }

                                    break;
                                case "barren-bombarded":
                                    for (String string : barrenConditions) {
                                        market.addCondition(string);
                                    }
                                    market.addCondition(Conditions.METEOR_IMPACTS);

                                    for (int e = 0; e < randomBarrenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomBarrenAttributes.get(e));
                                        }
                                    }

                                    oreType = getOreType(2, -1, 0);
                                    rareOreType = getRareOreType(3, 1, 0);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }

                                    break;

                                //COLD PLANETS
                                case "rocky_ice":
                                    for (String string : frozenConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomFrozenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomFrozenAttributes.get(e));
                                        }
                                    }

                                    oreType = getOreType(2, 0, 0);
                                    volatileType = getVolatileType(0, 0, 1);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (volatileType != null) {
                                        market.addCondition(volatileType);
                                    }

                                    break;
                                case "frozen":
                                case "frozen1":
                                case "frozen2":
                                case "frozen3":
                                    for (String string : frozenConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomFrozenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomFrozenAttributes.get(e));
                                        }
                                    }

                                    oreType = getOreType(2, 0, 0);
                                    volatileType = getVolatileType(0, 0, 0);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (volatileType != null) {
                                        market.addCondition(volatileType);
                                    }

                                    break;
                                case "cryovolcanic":
                                    for (String string : frozenConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomFrozenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomFrozenAttributes.get(e));
                                        }
                                    }

                                    oreType = getOreType(0, 0, 1);
                                    rareOreType = getRareOreType(1, 1, 0);
                                    volatileType = getVolatileType(0, 1, 1);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }
                                    if (volatileType != null) {
                                        market.addCondition(volatileType);
                                    }

                                    break;
                                case "toxic_cold":
                                    for (String string : frozenConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomFrozenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomFrozenAttributes.get(e));
                                        }
                                    }

                                    market.addCondition(Conditions.TOXIC_ATMOSPHERE);
                                    market.removeCondition(Conditions.THIN_ATMOSPHERE);
                                    market.addCondition(Conditions.DENSE_ATMOSPHERE);

                                    oreType = getOreType(1, 0, 0);
                                    rareOreType = getRareOreType(1, 0, 0);
                                    volatileType = getVolatileType(1, 1, 0);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }
                                    if (volatileType != null) {
                                        market.addCondition(volatileType);
                                    }

                                    break;

                                //HOT PLANETS
                                case "toxic":
                                    for (String string : hotConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomHotAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomHotAttributes.get(e));
                                        }
                                    }

                                    market.addCondition(Conditions.TOXIC_ATMOSPHERE);
                                    market.addCondition(Conditions.DENSE_ATMOSPHERE);

                                    oreType = getOreType(1, 0, 0);
                                    rareOreType = getRareOreType(1, 0, 0);
                                    volatileType = getVolatileType(1, 0, 0);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }
                                    if (volatileType != null) {
                                        market.addCondition(volatileType);
                                    }

                                    break;
                                case "lava_minor":
                                    for (String string : hotConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomHotAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomHotAttributes.get(e));
                                        }
                                    }
                                    market.removeCondition(Conditions.TECTONIC_ACTIVITY);
                                    market.addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY);

                                    if (hasSufficientAtmosphere(market)) {
                                        market.addCondition(Conditions.TOXIC_ATMOSPHERE);
                                    }

                                    oreType = getOreType(1, 1, 1);
                                    rareOreType = getRareOreType(1, 1, 1);
                                    if (oreType != null) {
                                        market.addCondition(oreType);
                                    }
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }

                                    break;
                                case "barren_venuslike":
                                    for (String string : hotConditions) {
                                        market.addCondition(string);
                                    }

                                    for (int e = 0; e < randomBarrenAttributes.size(); e++) {
                                        if (random.nextInt(e + 1) == 1) {
                                            market.addCondition(randomBarrenAttributes.get(e));
                                        }
                                    }

                                    rareOreType = getRareOreType(1, 0, 0);
                                    if (rareOreType != null) {
                                        market.addCondition(rareOreType);
                                    }

                                    break;
                            }

                            if (spawnHabitable) {
                                if (market.hasCondition(Conditions.EXTREME_WEATHER)) {
                                    market.removeCondition(Conditions.MILD_CLIMATE);
                                }
                            } else if (hasSufficientAtmosphere(market)) {
                                for (int e = 0; e < uninhabitableConditionsWithAtmosphere.size(); e++) {
                                    if (random.nextInt(e + 1) == 1) {
                                        market.addCondition(uninhabitableConditionsWithAtmosphere.get(e));
                                    }
                                }
                            }

                            if (planet.getMarket().hasCondition(Conditions.POOR_LIGHT)) {
                                market.addCondition(Conditions.POOR_LIGHT);
                            }

                            if (system.isNebula() || system.getStar().getTypeId().equals(StarTypes.BLACK_HOLE)) {
                                market.removeCondition(Conditions.COLD);
                                market.addCondition(Conditions.VERY_COLD);
                                market.removeCondition(Conditions.POOR_LIGHT);
                                market.addCondition(Conditions.DARK);
                            } else if (system.getStar().getTypeId().equals(StarTypes.NEUTRON_STAR)) {
                                market.addCondition(Conditions.IRRADIATED);
                            }

                            if (planet.getMarket().hasCondition(Conditions.VERY_HOT) && !spawnHabitable) {
                                market.removeCondition(Conditions.HOT);
                                market.addCondition(Conditions.VERY_HOT);
                            }

                            int weight;
                            int shift;
                            if (spawnHabitable) {
                                shift = 2;
                                weight = 0;
                            } else {
                                shift = 0;
                                weight = 1;
                            }
                            if (hasRuins && random.nextBoolean()) {
                                String ruinType = getRuinType(weight, shift);
                                if (ruinType != null) {
                                    market.addCondition(ruinType);
                                }
                            } else if (planet.isGasGiant() && random.nextBoolean()) {
                                String ruinType = getRuinType(weight, shift);
                                if (ruinType != null) {
                                    market.addCondition(ruinType);
                                }
                            }

                            StringBuilder logs = new StringBuilder("Added moon to: " + planet.getTypeId() + " with orbit radius " + orbitRadius + ", with conditions :");
                            for (MarketConditionAPI c : market.getConditions()) {
                                logs.append(", ").append(c.getId());
                            }
                            log.info(logs);
                        }
                    }
                }
            }
        }
    }

    //probabilities are 0.25 + 0.125 + 0.0625 + 0.03125 etc. which sum an infinity value of 0.5 overall chance of getting a resource
    private String getRuinType(int weight, int shift) {
        for (int i = 0; i < ruins.size(); i++) {
            if (random.nextInt(4 * (i + 1 + weight) + 1) == 1) {
                return ruins.get(MathUtils.clamp((i + shift), 0, ruins.size() - 1));
            }
        }
        return null;
    }

    private String getFarmlandType(int weight, int shift, int guarantee) {
        for (int i = 0; i < farmland.size(); i++) {
            if (random.nextInt(4 * (i + 1 + weight) + 1) == 1) {
                return farmland.get(MathUtils.clamp((i + shift), guarantee, farmland.size() - 1));
            }
        }
        return null;
    }

    private String getOrganicsType(int weight, int shift, int guarantee) {
        for (int i = 0; i < organics.size(); i++) {
            if (random.nextInt(4 * (i + 1 + weight) + 1) == 1) {
                return organics.get(MathUtils.clamp((i + shift), guarantee, organics.size() - 1));
            }
        }
        return null;
    }

    private String getVolatileType(int weight, int shift, int guarantee) {
        for (int i = 0; i < volatiles.size(); i++) {
            if (random.nextInt(4 * (i + 1 + weight) + 1) == 1) {
                return volatiles.get(MathUtils.clamp((i + shift), guarantee, volatiles.size() - 1));
            }
        }
        return null;
    }

    private String getOreType(int weight, int shift, int guarantee) {
        for (int i = 0; i < ore.size(); i++) {
            if (random.nextInt(4 * (i + 1 + weight) + 1) == 1) {
                return ore.get(MathUtils.clamp((i + shift), guarantee, ore.size() - 1));
            }
        }
        return null;
    }

    private String getRareOreType(int weight, int shift, int guarantee) {
        for (int i = 0; i < rareOre.size(); i++) {
            if (random.nextInt(4 * (i + 1 + weight) + 1) == 1) {
                return rareOre.get(MathUtils.clamp((i + shift), guarantee, rareOre.size() - 1));
            }
        }
        return null;
    }

    private boolean hasSufficientAtmosphere(MarketAPI market) {
        return !market.hasCondition(Conditions.NO_ATMOSPHERE);
    }
}
