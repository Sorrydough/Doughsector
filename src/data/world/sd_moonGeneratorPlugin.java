package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetGenDataSpec;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;


    //very, very heavily modified version of tomatopaste's moon generator used in lights out
    //his vision of the purpose for moons was very different from my own, so I needed my own implementation

public class sd_moonGeneratorPlugin implements SectorGeneratorPlugin {
    final float MIN_RADIUS_FOR_MOON_GEN = 150; //minimum radius a planet must have before the generator tries to put moons around it
    final float DIVISOR = 100; //higher number means lower probability to generate moons
    final float SPECIAL_PROBABILITY = 10; //higher is lower chance for habitable or toxic planets
    boolean isUnknownSkies = Global.getSettings().getModManager().isModEnabled("US");
    final List<String> freezingPlanets = new ArrayList<>(); {
        freezingPlanets.add("frozen");
        freezingPlanets.add("frozen1");
        freezingPlanets.add("frozen2");
        freezingPlanets.add("frozen3");
        freezingPlanets.add("cryovolcanic");
        freezingPlanets.add("toxic_cold");
        freezingPlanets.add("rocky_ice");
        if (isUnknownSkies) {
            freezingPlanets.add("US_iceA");
            freezingPlanets.add("US_iceB");
            freezingPlanets.add("US_green");
            freezingPlanets.add("US_blue"); //cryovolcanic
            freezingPlanets.add("US_purple"); //methane
        }
    }

    final List<String> hotPlanets = new ArrayList<>(); {
        hotPlanets.add("lava");
        hotPlanets.add("lava_minor");
        hotPlanets.add("toxic");
        hotPlanets.add("irradiated");
        if (isUnknownSkies) {
            hotPlanets.add("US_lava");
            hotPlanets.add("US_volcanic");
            hotPlanets.add("US_acid");
            hotPlanets.add("US_acidRain");
            hotPlanets.add("US_acidWind");
        }
    }

    final List<String> neutralPlanets = new ArrayList<>(); {
        neutralPlanets.add("barren");
        neutralPlanets.add("barren2");
        neutralPlanets.add("barren3");
        neutralPlanets.add("barren-bombarded");
        neutralPlanets.add("barren_castiron");
        neutralPlanets.add("barren_venuslike");
        neutralPlanets.add("rocky_metallic");
        if (isUnknownSkies) {
            neutralPlanets.add("US_barrenA");
            neutralPlanets.add("US_barrenB");
            neutralPlanets.add("US_barrenC");
            neutralPlanets.add("US_barrenD");
            neutralPlanets.add("US_barrenE");
            neutralPlanets.add("US_barrenF");
            neutralPlanets.add("US_azure");
            neutralPlanets.add("US_burnt");
            neutralPlanets.add("US_dust");
        }
    }

    final List<String> warmHabitablePlanets = new ArrayList<>(); { //hab offset of -1:0 or warmer
        warmHabitablePlanets.add("desert");
        warmHabitablePlanets.add("desert1");
        if (isUnknownSkies) {
            warmHabitablePlanets.add("US_lifeless");
            warmHabitablePlanets.add("US_desertA");
            warmHabitablePlanets.add("US_desertB");
            warmHabitablePlanets.add("US_desertC");
        }
    }
    final List<String> coldHabitablePlanets = new ArrayList<>(); { //hab offset of 0:1 or colder
        coldHabitablePlanets.add("tundra");
        coldHabitablePlanets.add("barren-desert");
        if (isUnknownSkies) {
            coldHabitablePlanets.add("US_red");      //crimson
            coldHabitablePlanets.add("US_redWind");
            coldHabitablePlanets.add("US_water");
            coldHabitablePlanets.add("US_waterB");
        }
    }

    final List<String> neutralHabitablePlanets = new ArrayList<>(); {   //hab offset not fitting the other two categories, ie -1:1 or -2:1
        //neutralHabitablePlanets.addAll(coldHabitablePlanets);           //planets requiring a hab offset of 0:0 are too strict for my simple moon generator, they can be added later
        //neutralHabitablePlanets.addAll(warmHabitablePlanets);           //TODO: when I figure out how to check a planet's current hab offset then I can more effectively choose what to put on it
        neutralHabitablePlanets.add("arid");
        neutralHabitablePlanets.add("water");
        neutralHabitablePlanets.add("jungle");
        if (isUnknownSkies) {
            neutralHabitablePlanets.add("US_alkali");
            neutralHabitablePlanets.add("US_jungle");
            neutralHabitablePlanets.add("US_auric");
            neutralHabitablePlanets.add("US_auricCloudy");
            neutralHabitablePlanets.add("US_lifelessArid");
            neutralHabitablePlanets.add("US_arid");
            neutralHabitablePlanets.add("US_crimson"); //bombarded lifeless
        }
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

    final Random random = new Random();
    //shamelessly stolen from tomato
    final static TreeMap<Integer, String> map = new TreeMap<Integer, String>(); static {
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
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (system.isProcgen() && system.getAge() != null) {
                CopyOnWriteArrayList<SectorEntityToken> entityTokens = new CopyOnWriteArrayList<>(system.getAllEntities());

                for (SectorEntityToken token : entityTokens) {
                    if (token instanceof PlanetAPI) {
                        PlanetAPI planet = (PlanetAPI) token;
                        if (planet.isStar() || planet.getRadius() < MIN_RADIUS_FOR_MOON_GEN || planet.getId().startsWith("sd_moon"))
                            continue;

                        int numMoons;
                        numMoons = random.nextInt (1 + (int) (planet.getRadius() / DIVISOR));

                        boolean isVeryHot = planet.getMarket().hasCondition(Conditions.VERY_HOT);
                        boolean isHot = planet.getMarket().hasCondition(Conditions.HOT) || isVeryHot;
                        boolean isVeryCold = planet.getMarket().hasCondition(Conditions.VERY_COLD);
                        boolean isCold = planet.getMarket().hasCondition(Conditions.COLD) || isVeryCold;

                        float orbitRadius = planet.getRadius() * 1.5f;
                        for (int i = 0; i < numMoons; i++) {
                            int radius = random.nextInt((int)(planet.getRadius() * 0.2)) + 25;
                            orbitRadius += random.nextInt(50) + radius + 75;

                            int orbitDays = random.nextInt(20) + 20;

                            boolean spawnHabitable = (1 == random.nextInt((int)SPECIAL_PROBABILITY));

                            String type;
                            if (spawnHabitable && !isVeryHot && !isVeryCold) {
                                int index = random.nextInt(neutralHabitablePlanets.size());
                                type = neutralHabitablePlanets.get(index);
                            }  else if (isHot) {
                                int index = random.nextInt(hotPlanets.size());
                                type = hotPlanets.get(index);
                            } else if (isCold) {
                                int index = random.nextInt(freezingPlanets.size());
                                type = freezingPlanets.get(index);
                            } else {
                                int index = random.nextInt(neutralPlanets.size());
                                type = neutralPlanets.get(index);
                            }

                            PlanetAPI moon = system.addPlanet("sd_moon_" + i + "_" + planet.hashCode(), planet, planet.getFullName() + " M-" + toRoman(i + 1), type, planet.getSpec().getPitch(), radius, orbitRadius, orbitDays);
                            PlanetConditionGenerator.generateConditionsForPlanet(moon, system.getAge());
                            //PlanetGenDataSpec spec = (PlanetGenDataSpec) Global.getSettings().getSpec(PlanetGenDataSpec.class, moon.getSpec().getPlanetType(), false);
                            //StarGenDataSpec starData = (StarGenDataSpec)Global.getSettings().getSpec(StarGenDataSpec.class, star.getSpec().getPlanetType(), false);
                            // todo: make this work
                        }
                    }
                }
            }
        }
    }
}
