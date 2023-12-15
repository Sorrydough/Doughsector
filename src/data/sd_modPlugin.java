package data;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import java.util.ArrayList;
import java.util.List;

import data.world.sd_moonGenerator;

@SuppressWarnings("unused")
public class sd_modPlugin extends BaseModPlugin {
    SettingsAPI settings = Global.getSettings();
    @Override
    public void onApplicationLoad() {
        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            ShaderLib.init();
            TextureData.readTextureDataCSV("data/lights/sd_texture_data.csv");
            LightData.readLightDataCSV("data/lights/sd_light_data.csv");
        }
        List <String> sd_weaponsList = new ArrayList<>(); {
            //projectiles
            sd_weaponsList.add("lightmg");
            sd_weaponsList.add("lightdualmg");
            sd_weaponsList.add("lightneedler");
            sd_weaponsList.add("amblaster");
            //beams
            sd_weaponsList.add("ionbeam");
            sd_weaponsList.add("gravitonbeam");
            sd_weaponsList.add("tachyonlance");
            sd_weaponsList.add("hil");
            //point defense
            sd_weaponsList.add("pdburst");
            sd_weaponsList.add("heavyburst");
            sd_weaponsList.add("guardian");
            //missiles
            sd_weaponsList.add("heatseeker");
            sd_weaponsList.add("salamanderpod");
            sd_weaponsList.add("breach");
            sd_weaponsList.add("breachpod");
            sd_weaponsList.add("gazer");
            sd_weaponsList.add("gazerpod");
            sd_weaponsList.add("dragon");
            sd_weaponsList.add("dragonpod");
            sd_weaponsList.add("pilum");
            sd_weaponsList.add("pilum_large");
            sd_weaponsList.add("squall");
            //self-insert donut steel weapons
            sd_weaponsList.add("sd_gravlance");
            sd_weaponsList.add("sd_dragon_single");
            sd_weaponsList.add("sd_antimatterlauncher");
            sd_weaponsList.add("sd_antimatterpod");
        }
        for (String weapon : sd_weaponsList) {
            try {
                WeaponSpecAPI spec = settings.getWeaponSpec(weapon);
                spec.addTag("sd_arsenal_package");
            } catch (Exception ignored) {}
        }
    }
    public void onNewGameAfterProcGen() {
        new sd_moonGenerator().generate(Global.getSector());
    }
    public void onGameLoad(boolean newGame) {
        final FactionAPI playerFaction = Global.getSector().getPlayerPerson().getFaction();
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