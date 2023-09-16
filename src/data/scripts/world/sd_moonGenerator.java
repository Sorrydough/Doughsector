package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetGenDataSpec;
import com.fs.starfarer.loading.specs.PlanetSpec;
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
    public static final float MIN_RADIUS_FOR_MOON_GEN = 150; //minimum radius a planet must have before the generator tries to put moons around it
    public static final float DIVISOR = 100; //higher number means lower probability to generate moons
    public static final float SPECIAL_PROBABILITY = 10; //higher is lower chance for habitable or toxic planets

    public static final Logger log = Global.getLogger(sd_moonGenerator.class);

    public static final List<String> ringBandTypes = new ArrayList<>();
    static {
        ringBandTypes.add("rings_dust0");
        ringBandTypes.add("rings_asteroids0");
        ringBandTypes.add("rings_ice0");
    }

     final List<String> gasGiants = new ArrayList<>(); {
        gasGiants.add("gas_giant");
        gasGiants.add("ice_giant");
        gasGiants.add("US_gas_giant");
        gasGiants.add("US_gas_giantB");
    }

    final List<String> coldPlanets = new ArrayList<>(); {
        coldPlanets.add("frozen");
        coldPlanets.add("frozen1");
        coldPlanets.add("frozen2");
        coldPlanets.add("frozen3");
        coldPlanets.add("cryovolcanic");
        coldPlanets.add("toxic_cold");
        coldPlanets.add("US_purple"); //methane
        coldPlanets.add("US_iceA");
        coldPlanets.add("US_iceB");
        coldPlanets.add("US_blue"); //cryovolcanic
    }

    final List<String> hotPlanets = new ArrayList<>(); {
        hotPlanets.add("lava");
        hotPlanets.add("lava_minor");
        hotPlanets.add("toxic");
        hotPlanets.add("irradiated");
        hotPlanets.add("US_lava");
        hotPlanets.add("US_volcanic");
        hotPlanets.add("US_green");
    }

    final List<String> neutralPlanets = new ArrayList<>(); {
        neutralPlanets.add("barren");
        neutralPlanets.add("barren2");
        neutralPlanets.add("barren3");
        neutralPlanets.add("barren-bombarded");
        neutralPlanets.add("barren_castiron");
        neutralPlanets.add("barren_venuslike");
        neutralPlanets.add("rocky_metallic");
        neutralPlanets.add("rocky_unstable");
        neutralPlanets.add("rocky_ice");
        neutralPlanets.add("US_barrenA");
        neutralPlanets.add("US_barrenB");
        neutralPlanets.add("US_barrenC");
        neutralPlanets.add("US_barrenD");
        neutralPlanets.add("US_barrenE");
        neutralPlanets.add("US_barrenF");
        neutralPlanets.add("US_azure");
        neutralPlanets.add("US_burnt");
        neutralPlanets.add("US_dust");
        neutralPlanets.add("US_acid");
        neutralPlanets.add("US_acidRain");
        neutralPlanets.add("US_acidWind");
    }

    final List<String> goldilocksPlanets = new ArrayList<>(); {
        goldilocksPlanets.add("terran");
        goldilocksPlanets.add("terran-eccentric");
        goldilocksPlanets.add("water");
        goldilocksPlanets.add("jungle");
        goldilocksPlanets.add("tundra");
        goldilocksPlanets.add("arid");
        goldilocksPlanets.add("desert");
        goldilocksPlanets.add("desert1");
        goldilocksPlanets.add("barren-desert");
        goldilocksPlanets.add("US_continent");
        goldilocksPlanets.add("US_water");
        goldilocksPlanets.add("US_waterB");
        goldilocksPlanets.add("US_alkali");
        goldilocksPlanets.add("US_jungle");
        goldilocksPlanets.add("US_auric");
        goldilocksPlanets.add("US_auricCloudy");
        goldilocksPlanets.add("US_lifelessarid");
        goldilocksPlanets.add("US_arid");
        goldilocksPlanets.add("US_crimson");
        goldilocksPlanets.add("US_desertA");
        goldilocksPlanets.add("US_desertB");
        goldilocksPlanets.add("US_desertC");
        goldilocksPlanets.add("US_red");
        goldilocksPlanets.add("US_redWind");
    }

    //Ok so here's how we need to structure it:
    //lists required: very hot, hot, neutral, cold, very cold, goldilocks
    //if we're really close to the star, we need to select from a list of "gross as fuck planets"
    //if we're really far from the star, we need to select from a list of "boring as fuck planets"
    //we need a list of neutral planets that can be selected no matter what
    //the probability of selecting a gross or boring planet needs to scale based on deviation outside the goldilocks zone
    //if we're in the goldilocks zone, we have the chance to select a habitable planet



//     final List<String> habitableStars = new ArrayList<>(); {
//        habitableStars.add(StarTypes.WHITE_DWARF);
//        habitableStars.add(StarTypes.BROWN_DWARF);
//        habitableStars.add(StarTypes.RED_DWARF);
//        habitableStars.add(StarTypes.YELLOW);
//        habitableStars.add(StarTypes.ORANGE);
//        habitableStars.add(StarTypes.ORANGE_GIANT);
//    }

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

                for (SectorEntityToken token : entityTokens) {
                    if (token instanceof PlanetAPI) {
                        PlanetAPI planet = (PlanetAPI) token;

                        if (planet.getId().startsWith("sd_moon")) {
                            continue;
                        }
                        float planetRadius = planet.getRadius();
                        if (planet.isStar()) {
                            continue;
                        } else if (planetRadius < MIN_RADIUS_FOR_MOON_GEN) {
                            continue;
                        }

                        int numMoons;
                        numMoons = random.nextInt (1 + (int) (planetRadius / DIVISOR));
                        log.info("Adding " + numMoons + " moons to " + planet.getTypeId());


                        boolean isVeryHot = planet.getMarket().hasCondition(Conditions.VERY_HOT);
                        boolean isHot = planet.getMarket().hasCondition(Conditions.HOT) || isVeryHot;
                        boolean isVeryCold = planet.getMarket().hasCondition(Conditions.VERY_COLD);
                        boolean isCold = planet.getMarket().hasCondition(Conditions.COLD) || isVeryCold;

                        float orbitRadius = planetRadius * 1.5f;
                        for (int i = 0; i < numMoons; i++) {
                            String id = "sd_moon_" + i + "_" + planet.hashCode();
                            int radius = random.nextInt((int)(planet.getRadius() * 0.75)) + 25;
                            orbitRadius += random.nextInt(50) + radius + 75;

                            int orbitDays = random.nextInt(20) + 20;

                            boolean spawnHabitable = (1 == random.nextInt((int)SPECIAL_PROBABILITY));

                            String type;
                            if (spawnHabitable && !isVeryHot && !isVeryCold) {
                                int index = random.nextInt(goldilocksPlanets.size());
                                type = goldilocksPlanets.get(index);
                            }  else if (isHot) {
                                int index = random.nextInt(hotPlanets.size());
                                type = hotPlanets.get(index);
                            } else if (isCold) {
                                int index = random.nextInt(coldPlanets.size());
                                type = coldPlanets.get(index);
                            } else {
                                int index = random.nextInt(neutralPlanets.size());
                                type = neutralPlanets.get(index);
                            }

                            PlanetAPI moon = system.addPlanet(id, planet, planet.getFullName() + " M-" + toRoman(i + 1), type, planet.getSpec().getPitch(), radius, orbitRadius, orbitDays);
                            PlanetConditionGenerator.generateConditionsForPlanet(moon, system.getAge());

                            StringBuilder logs = new StringBuilder("Added moon to: " + planet.getTypeId() + " with orbit radius " + orbitRadius + ", with conditions :");
                            for (MarketConditionAPI c : moon.getMarket().getConditions()) {
                                logs.append(", ").append(c.getId());
                            }
                            log.info(logs);
                        }
                    }
                }
            }
        }
    }
}
