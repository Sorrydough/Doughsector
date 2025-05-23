package data;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.ArrayList;
import java.util.List;

import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Level;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import data.world.sd_moonGeneratorPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("unused")
public class sd_modPlugin extends BaseModPlugin {
    SettingsAPI settings = Global.getSettings();
    List <String> sd_weaponsList = new ArrayList<>(); {
        //projectiles
        sd_weaponsList.add("amblaster");
        sd_weaponsList.add("sd_gravlance");
        sd_weaponsList.add("sd_amhowitzer");
        sd_weaponsList.add("railgun");
        sd_weaponsList.add("lightmg");
        sd_weaponsList.add("lightdualmg");
        //beams
        sd_weaponsList.add("ionbeam");
        sd_weaponsList.add("irautolance");
        sd_weaponsList.add("gravitonbeam");
        sd_weaponsList.add("tachyonlance");
        sd_weaponsList.add("hil");
        sd_weaponsList.add("pdburst");
        sd_weaponsList.add("heavyburst");
        sd_weaponsList.add("guardian");
        //missiles
        sd_weaponsList.add("sd_grapeshotrack");
        sd_weaponsList.add("heatseeker");
        sd_weaponsList.add("salamanderpod");
        sd_weaponsList.add("breach");
        sd_weaponsList.add("breachpod");
        sd_weaponsList.add("pilum");
        sd_weaponsList.add("pilum_large");
        sd_weaponsList.add("dragon");
        sd_weaponsList.add("dragonpod");
        sd_weaponsList.add("squall");
        sd_weaponsList.add("locust");
    }
    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        AutofireAIPlugin plugin = null;
        if (weapon.getShip().getVariant().getHullMods().contains("sd_utilityhullmod") && weapon.getType() != WeaponAPI.WeaponType.MISSILE)
            plugin = new data.autofire.features.autofire.AutofireAI(weapon);
        return new PluginPick<>(plugin, CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    @Override
    public void onApplicationLoad() {
        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            ShaderLib.init();
            TextureData.readTextureDataCSV("data/lights/sd_texture_data.csv");
            LightData.readLightDataCSV("data/lights/sd_light_data.csv");
        }
        for (String weapon : sd_weaponsList) {
            try {
                WeaponSpecAPI spec = settings.getWeaponSpec(weapon);
                spec.addTag("sd_arsenal_package");
            } catch (Exception ignored) {}
        }
    }
    public void onGameLoad(boolean newGame) {
        final boolean hasLunaLib = Global.getSettings().getModManager().isModEnabled("lunalib");
        final FactionAPI playerFaction = Global.getSector().getPlayerPerson().getFaction();
        // generate moons if someone's loading a save that hasn't had them generated yet
        // we don't need to use an onNewGameAfterProcGen because onGameLoad runs in that case anyway
        boolean wantMoons = true;
        if (hasLunaLib)
            wantMoons = Boolean.parseBoolean(LunaSettings.getString("sd_doughsector", "sd_generateMoons"));
        if (wantMoons && !Global.getSector().getMemoryWithoutUpdate().contains("$sd_moons")) {
            Global.getSector().getMemoryWithoutUpdate().set("$sd_moons", true);
            new sd_moonGeneratorPlugin().generate(Global.getSector());
        }
        // remove baseBP
        boolean wantRemoveBaseBP = false;
        if (hasLunaLib)
            wantRemoveBaseBP = Boolean.parseBoolean(LunaSettings.getString("sd_doughsector", "sd_removeBaseBP"));
        if (wantRemoveBaseBP) {
            for (String weapon : new ArrayList<>(playerFaction.getKnownWeapons()))
                if (settings.getWeaponSpec(weapon).hasTag("base_bp"))
                    playerFaction.removeKnownWeapon(weapon);
            for (String ship : new ArrayList<>(playerFaction.getKnownShips()))
                if (settings.getHullSpec(ship).hasTag("base_bp"))
                    playerFaction.removeKnownShip(ship);
            for (String fighter : new ArrayList<>(playerFaction.getKnownFighters()))
                if (settings.getFighterWingSpec(fighter).hasTag("base_bp"))
                    playerFaction.removeKnownFighter(fighter);
        }
    }
}