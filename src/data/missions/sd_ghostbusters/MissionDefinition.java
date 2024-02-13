package data.missions.sd_ghostbusters;

import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.Global;

import java.util.HashMap;
import java.util.List;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import org.magiclib.campaign.MagicCaptainBuilder;

@SuppressWarnings("unused")
public class MissionDefinition implements MissionDefinitionPlugin {
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.PLAYER, "TTS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "???", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "Tri-Tachyon Phase Ambush");
        api.setFleetTagline(FleetSide.ENEMY, "Unknown Fleet");

        api.addBriefingItem("The best scouting is done with your guns. Force them to fight and test their mettle.");
        api.addBriefingItem("Some of their ships are tailored to our fleet's weaknesses. There won't be room for mistakes.");

        api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
        FleetMemberAPI BLUFLAGSHIP = api.addToFleet(FleetSide.PLAYER, "doom_Attack", FleetMemberType.SHIP, "TTS Wraithcaller", true);
        api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, "TTS Raging Storm", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, "TTS Eclipsed Justice", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Soulweaver", false);
        api.addToFleet(FleetSide.PLAYER, "tempest_Attack", FleetMemberType.SHIP, "TTS Stormbringer", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.PLAYER, "omen_PD", FleetMemberType.SHIP, "TTS Archon's Wrath", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.PLAYER, "omen_PD", FleetMemberType.SHIP, "TTS Shadow Sentinel", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);

        api.defeatOnShipLoss("TTS Wraithcaller");

        HashMap<String, Integer> BLUADMIRALSKILLS = new HashMap<>();
        BLUADMIRALSKILLS.put(Skills.HELMSMANSHIP, 2);
        BLUADMIRALSKILLS.put(Skills.FIELD_MODULATION, 2);
        BLUADMIRALSKILLS.put(Skills.POLARIZED_ARMOR, 1);
        BLUADMIRALSKILLS.put(Skills.IMPACT_MITIGATION, 1);
        BLUADMIRALSKILLS.put(Skills.SYSTEMS_EXPERTISE, 1);

        PersonAPI BLUADMIRAL = new MagicCaptainBuilder(Factions.NEUTRAL)
                .setLevel(5)
                .setGender(FullName.Gender.MALE)
                .setPersonality(Personalities.AGGRESSIVE)
                .setPortraitId("graphics/portraits/portrait_corporate10.png")
                .setSkillLevels(BLUADMIRALSKILLS)
                .create();
        BLUFLAGSHIP.setCaptain(BLUADMIRAL);

        api.getDefaultCommander(FleetSide.ENEMY).getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
        FleetMemberAPI REDFLAGSHIP = api.addToFleet(FleetSide.ENEMY, "sd_cruiserheavy_Specialist", FleetMemberType.SHIP, "Radial Impulse", true);
        api.addToFleet(FleetSide.ENEMY, "sd_cruiser_Attack", FleetMemberType.SHIP, "Involuted Mountain", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.ENEMY, "sd_destroyercarrier_Command", FleetMemberType.SHIP, "Galactic Cloud", false);
        api.addToFleet(FleetSide.ENEMY, "sd_destroyercarrier_Command", FleetMemberType.SHIP, "Bombarded Signal", false);
        api.addToFleet(FleetSide.ENEMY, "sd_destroyerlight_Attack", FleetMemberType.SHIP, "Whispering Light", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.ENEMY, "sd_destroyerlight_Attack", FleetMemberType.SHIP, "Lightwave Instrument", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Strike", FleetMemberType.SHIP, "Reconstructed Mind", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Strike", FleetMemberType.SHIP, "Last Heartbeat", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Strike", FleetMemberType.SHIP, "Direct Path", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Strike", FleetMemberType.SHIP, "Chilled Nova", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Strike", FleetMemberType.SHIP, "Deep Blue", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);
        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Strike", FleetMemberType.SHIP, "Even Dark", false).getCaptain().setPersonality(Personalities.AGGRESSIVE);

        HashMap<String, Integer> REDADMIRALSKILLS = new HashMap<>();
        REDADMIRALSKILLS.put(Skills.POINT_DEFENSE, 2);
        REDADMIRALSKILLS.put(Skills.HELMSMANSHIP, 2);
        REDADMIRALSKILLS.put(Skills.ENERGY_WEAPON_MASTERY, 2);
        REDADMIRALSKILLS.put(Skills.GUNNERY_IMPLANTS, 1);

        PersonAPI REDADMIRAL = new MagicCaptainBuilder(Factions.NEUTRAL)
                .setLevel(4)
                .setGender(FullName.Gender.MALE)
                .setPersonality(Personalities.AGGRESSIVE)
                .setPortraitId("graphics/portraits/sd_app_087.png")
                .setSkillLevels(REDADMIRALSKILLS)
                .create();
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
        boolean runOnce = true;
        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (runOnce && !Global.getCombatEngine().isPaused()) {
                Global.getSoundPlayer().playCustomMusic(3, 10, "sd_ghostbusters");
                runOnce = false;
            }
        }
    }
}
