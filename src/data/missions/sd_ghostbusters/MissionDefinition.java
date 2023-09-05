package data.missions.sd_ghostbusters;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.Global;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

@SuppressWarnings("unused")
public class MissionDefinition implements MissionDefinitionPlugin {
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.PLAYER, "TTS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "???", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "Tri-Tachyon Phase Ambush");
        api.setFleetTagline(FleetSide.ENEMY, "Unknown Fleet");

        api.addBriefingItem("The best scouting is done with your guns: Force them to fight and test their mettle.");
        api.addBriefingItem("Losing a phase cruiser would be an embarrassment. The TTS Wraith Caller must survive.");

        api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
        FleetMemberAPI BLUFLAGSHIP = api.addToFleet(FleetSide.PLAYER, "doom_Attack", FleetMemberType.SHIP, "TTS Wraith Caller", true);
        api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, "TTS Raging Storm", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, "TTS Eclipsed Justice", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Pandemic Echo", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Archon's Wrath", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Soulweaver", false).getCaptain().setPersonality("aggressive");
        api.defeatOnShipLoss("TTS Wraith Caller");

        PersonAPI BLUADMIRAL = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.TRITACHYON), 0, FleetFactoryV3.getSkillPrefForShip(BLUFLAGSHIP), true, null, false, false, 0, new Random());
        BLUADMIRAL.getStats().setLevel(3);
        BLUADMIRAL.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
        BLUADMIRAL.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
        BLUADMIRAL.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1);
        BLUADMIRAL.setPersonality("aggressive");
        BLUFLAGSHIP.setCaptain(BLUADMIRAL);
        //TODO: MIRROR PARHELION, UPDATE VARIANTS, NAME THE SHIPS, AND WE'RE GOOD TO GO

        api.getDefaultCommander(FleetSide.ENEMY).getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
        FleetMemberAPI REDFLAGSHIP = api.addToFleet(FleetSide.ENEMY, "sd_cruiserheavy_Standard", FleetMemberType.SHIP, "ʌЇҜↁϻ₸♆ʟ₮-Φ", true);
        api.addToFleet(FleetSide.ENEMY, "sd_cruiserskirm_Buster", FleetMemberType.SHIP, "ʌЇҜↁϻ₸♆ʟ₮-Φ", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.ENEMY, "sd_destroyercarrier_Standard", FleetMemberType.SHIP, "ʌЇҜↁϻ₸♆ʟ₮-Φ", false);
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelarge_Buster", FleetMemberType.SHIP, "Ҥ₳ƱΩμΞѬ-4", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelarge_Buster", FleetMemberType.SHIP, "Ψl҉Ψl҉ⱠՃႸяΦЖ-9", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.ENEMY, "sd_frigateskirm_Standard", FleetMemberType.SHIP, "๏ԾⱠ⌘ᑕʞ⌂Σ-ψ", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.ENEMY, "sd_frigateskirm_Standard", FleetMemberType.SHIP, "₣ㄖ尺ʞȻ₮Σຮ-Ψ", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Buster", FleetMemberType.SHIP, "ἷЯĶ₲₥ዞ⏣⇀-Z", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Buster", FleetMemberType.SHIP, "௮乇ҜΔ⌘Æⱡⓔ-X", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Buster", FleetMemberType.SHIP, "₡ℓⱥᖇҜ₦ӨɄ⇌Ҭ-Я", false).getCaptain().setPersonality("aggressive");

        PersonAPI REDADMIRAL = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.INDEPENDENT), 0, FleetFactoryV3.getSkillPrefForShip(BLUFLAGSHIP), true, null, false, false, 0, new Random());
        REDADMIRAL.getStats().setLevel(3);
        REDADMIRAL.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        REDADMIRAL.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
        REDADMIRAL.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1);
        REDADMIRAL.setPersonality("aggressive");
        REDFLAGSHIP.setCaptain(REDADMIRAL);


        float width = 14000f;
        float height = 16000f;
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

        float minX = -width / 2;
        float minY = -height / 2;
        api.addObjective(minX + width * 0.5f, minY + height * 0.5f, "sensor_array");

        api.setHyperspaceMode(true);

        api.addPlugin(new ghostbusters_Plugin());
    }

    private static class ghostbusters_Plugin extends BaseEveryFrameCombatPlugin {
        boolean doOnce = true;
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (doOnce && !Global.getCombatEngine().isPaused()) {
                Global.getSoundPlayer().playCustomMusic(3, 10, "sd_ghostbusters");
                doOnce = false;
            }
//            if (Global.getCombatEngine().isPaused()) {
//                Global.getSoundPlayer().pauseCustomMusic();
//                doOnce = true;
//            }
        }
    }
}
