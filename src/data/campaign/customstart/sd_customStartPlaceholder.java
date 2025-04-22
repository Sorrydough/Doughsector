package data.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.NGCAddStandardStartingScript;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;
import exerelin.utilities.StringHelper;
import org.magiclib.util.MagicCampaign;
import org.magiclib.util.MagicVariables;

import java.util.*;

//Placeholder start for mod testing. Start with a small fleet, a blueprint package, and near a random gate in the sector.
@SuppressWarnings("unused")
public class sd_customStartPlaceholder extends CustomStart {
    @Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);

        data.addScriptBeforeTimePass(
                new Script() {
                    @Override
                    public void run() {
                        List<String> notThemes = Arrays.asList(
                                MagicVariables.AVOID_COLONIZED_SYSTEM,
                                MagicVariables.AVOID_BLACKHOLE_PULSAR,
                                "theme_hidden"
                        );

                        List<String> entities = Collections.singletonList(Tags.GATE);

                        List<String> remnantThemes = Arrays.asList(
                                Tags.THEME_REMNANT_SUPPRESSED,
                                Tags.THEME_REMNANT_NO_FLEETS,
                                Tags.THEME_REMNANT_DESTROYED,
                                Tags.THEME_REMNANT_SECONDARY,
                                Tags.THEME_REMNANT_RESURGENT
                        );

                        List<String> derelictThemes = Arrays.asList(
                                Tags.THEME_DERELICT,
                                Tags.THEME_DERELICT_MOTHERSHIP,
                                Tags.THEME_DERELICT_CRYOSLEEPER,
                                Tags.THEME_DERELICT_SURVEY_SHIP,
                                Tags.THEME_DERELICT_PROBES
                        );

                        SectorEntityToken location = MagicCampaign.findSuitableTarget(
                                null,
                                null,
                                "CLOSE",
                                remnantThemes,
                                notThemes,
                                entities,
                                false,
                                true,
                                false
                        );

                        if (location == null) {
                            location = MagicCampaign.findSuitableTarget(
                                    null,
                                    null,
                                    "CLOSE",
                                    derelictThemes,
                                    notThemes,
                                    entities,
                                    false,
                                    true,
                                    false
                            );
                        }

                        if (location == null) {
                            String msg = "Critical failure: expected to find an inactive gate in a derelict or remnant system.\n"
                                    + "This is most likely caused by another mod that removes or alters these objects.\n"
                                    + "Please report this issue to Sorrydough on Discord with how to reproduce.\n"
                                    + "If you can't reproduce it, you probably just got astronomically unlucky and can ignore this.";
                            throw new RuntimeException(msg);
                        }

                        Global.getSector().getMemoryWithoutUpdate().set("$nex_startLocation", location.getId());
                    }
                }
        );


        CampaignFleetAPI tempFleet = FleetFactoryV3.createEmptyFleet(PlayerFactionStore.getPlayerFactionIdNGC(), FleetTypes.PATROL_SMALL, null);

        addFleetMember("sd_retrofitstarliner_Surplus", dialog, data, tempFleet, "none");
        addFleetMember("sd_frigate_Surplus", dialog, data, tempFleet, "flagship");
        addFleetMember("sd_frigatelight_Surplus", dialog, data, tempFleet, "none");
        addFleetMember("sd_frigatelight_Surplus", dialog, data, tempFleet, "none");
        addFleetMember("sd_retrofitkite_Surplus", dialog, data, tempFleet, "none");


        data.getStartingCargo().getCredits().add(10000);
        AddRemoveCommodity.addCreditsGainText(10000, dialog.getTextPanel());
        MutableCharacterStatsAPI stats = data.getPerson().getStats();
        stats.addPoints(1);

        tempFleet.getFleetData().setSyncNeeded();
        tempFleet.getFleetData().syncIfNeeded();
        tempFleet.forceSync();

        int crew = 0;
        int fuel = 0;
        int supplies = 0;
        int machinery = 0;
        for (FleetMemberAPI member : tempFleet.getFleetData().getMembersListCopy()) {
            crew += (int) member.getMinCrew() * 1.5;
            fuel += (int) member.getFuelCapacity() * 0.75;
            supplies += (int) member.getBaseDeploymentCostSupplies() * 3;
            machinery += (int) member.getBaseDeploymentCostSupplies() * 2;
        }
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.CREW, crew);
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.FUEL, fuel);
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.SUPPLIES, supplies);
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.HEAVY_MACHINERY, machinery);

        AddRemoveCommodity.addCommodityGainText(Commodities.CREW, crew, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.FUEL, fuel, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.SUPPLIES, supplies, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.HEAVY_MACHINERY, machinery, dialog.getTextPanel());

        data.getStartingCargo().addSpecial(new SpecialItemData("sd_arsenal_package", null), 1);

        // enforce corvus mode
        data.setDifficulty("normal");
        ExerelinSetupData.getInstance().easyMode = false;
        ExerelinSetupData.getInstance().hardMode = true;
        ExerelinSetupData.getInstance().freeStart = true;
        ExerelinSetupData.getInstance().randomStartLocation = true;
        ExerelinSetupData.getInstance().dModLevel = 1;

        data.addScript(new Script() {
            public void run() {
                CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();

                NGCAddStandardStartingScript.adjustStartingHulls(fleet);

                fleet.getFleetData().ensureHasFlagship();

                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    float max = member.getRepairTracker().getMaxCR();
                    member.getRepairTracker().setCR(max);
                }
                fleet.getFleetData().setSyncNeeded();
            }
        });

        dialog.getVisualPanel().showFleetInfo(StringHelper.getString("exerelin_ngc", "playerFleet", true),
                tempFleet, null, null);

        dialog.getOptionPanel().addOption(StringHelper.getString("done", true), "nex_NGCDone");
        dialog.getOptionPanel().addOption(StringHelper.getString("back", true), "nex_NGCStartBack");
    }

    public void addFleetMember(String vid, InteractionDialogAPI dialog, CharacterCreationData data, CampaignFleetAPI fleet, String special) {
        data.addStartingFleetMember(vid, FleetMemberType.SHIP);
        FleetMemberAPI temp = Global.getFactory().createFleetMember(FleetMemberType.SHIP, vid);
        fleet.getFleetData().addFleetMember(temp);
        temp.getRepairTracker().setCR(0.5f);

        if (special.equals("flagship")) {
            fleet.getFleetData().setFlagship(temp);
            temp.setCaptain(data.getPerson());
        }
        AddRemoveCommodity.addFleetMemberGainText(temp.getVariant(), dialog.getTextPanel());
    }

}
