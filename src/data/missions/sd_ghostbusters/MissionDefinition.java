package data.missions.sd_ghostbusters;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.Global;
import java.util.List;

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

        api.addBriefingItem("Find out what sort of foe we're up against by rout or annihilation");
        api.addBriefingItem("Reinforcing from the core would take time that we don't have: the TTS Wraith Caller must survive");

//        api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
//
//        api.addToFleet(FleetSide.PLAYER, "doom_Attack", FleetMemberType.SHIP, "TTS Wraith Caller", true);
//        api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, "TTS Raging Storm", false);
//        api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, "TTS Eclipsed Justice", false);
//        api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Pandemic Echo", false);
//        api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Archon's Wrath", false);
//        api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, "TTS Soulweaver", false);
//
//        api.defeatOnShipLoss("TTS Wraith Caller");
//
//        api.addToFleet(FleetSide.ENEMY, "sd_cruiserskirm_Buster", FleetMemberType.SHIP, "ʌЇҜↁϻ₸♆ʟ₮-Φ", true);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigatelarge_Buster", FleetMemberType.SHIP, "乙๏₣ǶΔӘřŦ-7", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigatelarge_Buster", FleetMemberType.SHIP, "Ҥ₳ƱΩμΞѬ-4", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigatelarge_Buster", FleetMemberType.SHIP, "Ψl҉Ψl҉ⱠՃႸяΦЖ-9", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigatelarge_Buster", FleetMemberType.SHIP, "₴ł₦₭₮Ɽ∆ß-12", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigateskirm_Standard", FleetMemberType.SHIP, "๏ԾⱠ⌘ᑕʞ⌂Σ-ψ", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigateskirm_Standard", FleetMemberType.SHIP, "₣ㄖ尺ʞȻ₮Σຮ-Ψ", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Buster", FleetMemberType.SHIP, "ΣɆ⋏ζ₭ⱠЪⱥ₮ξ-Y", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Buster", FleetMemberType.SHIP, "ἷЯĶ₲₥ዞ⏣⇀-Z", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Buster", FleetMemberType.SHIP, "௮乇ҜΔ⌘Æⱡⓔ-X", false);
//        api.addToFleet(FleetSide.ENEMY, "sd_frigatelight_Buster", FleetMemberType.SHIP, "₡ℓⱥᖇҜ₦ӨɄ⇌Ҭ-Я", false);

        api.getDefaultCommander(FleetSide.ENEMY).getStats().setSkillLevel(Skills.PHASE_CORPS, 1);

        api.addToFleet(FleetSide.ENEMY, "doom_Attack", FleetMemberType.SHIP, "TTS Wraith Caller", true);
        api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, "TTS Raging Storm", false);
        api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, "TTS Eclipsed Justice", false);
        api.addToFleet(FleetSide.ENEMY, "afflictor_Strike", FleetMemberType.SHIP, "TTS Pandemic Echo", false);
        api.addToFleet(FleetSide.ENEMY, "afflictor_Strike", FleetMemberType.SHIP, "TTS Archon's Wrath", false);
        api.addToFleet(FleetSide.ENEMY, "afflictor_Strike", FleetMemberType.SHIP, "TTS Soulweaver", false);

//        api.defeatOnShipLoss("TTS Wraith Caller");

        api.addToFleet(FleetSide.PLAYER, "sd_cruiserheavy_Standard", FleetMemberType.SHIP, "ʌЇҜↁϻ₸♆ʟ₮-Φ", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "sd_cruiserskirm_Buster", FleetMemberType.SHIP, "ʌЇҜↁϻ₸♆ʟ₮-Φ", true).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "sd_destroyercarrier_Standard", FleetMemberType.SHIP, "ʌЇҜↁϻ₸♆ʟ₮-Φ", false);
        api.addToFleet(FleetSide.PLAYER, "sd_frigatelarge_Buster", FleetMemberType.SHIP, "Ҥ₳ƱΩμΞѬ-4", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "sd_frigatelarge_Buster", FleetMemberType.SHIP, "Ψl҉Ψl҉ⱠՃႸяΦЖ-9", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "sd_frigateskirm_Standard", FleetMemberType.SHIP, "๏ԾⱠ⌘ᑕʞ⌂Σ-ψ", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "sd_frigateskirm_Standard", FleetMemberType.SHIP, "₣ㄖ尺ʞȻ₮Σຮ-Ψ", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "sd_frigatelight_Buster", FleetMemberType.SHIP, "ἷЯĶ₲₥ዞ⏣⇀-Z", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "sd_frigatelight_Buster", FleetMemberType.SHIP, "௮乇ҜΔ⌘Æⱡⓔ-X", false).getCaptain().setPersonality("aggressive");
        api.addToFleet(FleetSide.PLAYER, "sd_frigatelight_Buster", FleetMemberType.SHIP, "₡ℓⱥᖇҜ₦ӨɄ⇌Ҭ-Я", false).getCaptain().setPersonality("aggressive");

//        api.getDefaultCommander(FleetSide.ENEMY).getStats().setSkillLevel(Skills.);

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
