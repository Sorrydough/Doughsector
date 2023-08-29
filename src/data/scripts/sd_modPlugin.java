package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class sd_modPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            ShaderLib.init();
            TextureData.readTextureDataCSV("data/lights/sd_texture_data.csv");
        }

        List <String> sd_weaponsList = new ArrayList<>();
        {
            //projectiles
            sd_weaponsList.add("amblaster");
            sd_weaponsList.add("plasma");

            //beams
            sd_weaponsList.add("taclaser");
            sd_weaponsList.add("ionbeam");
            sd_weaponsList.add("gravitonbeam");
            sd_weaponsList.add("phasebeam");
            sd_weaponsList.add("tachyonlance");
            sd_weaponsList.add("hil");

            //point defense
            sd_weaponsList.add("lrpdlaser");
            sd_weaponsList.add("pdburst");
            sd_weaponsList.add("heavyburst");
            sd_weaponsList.add("guardian");

            //missiles
            sd_weaponsList.add("heatseeker");
            sd_weaponsList.add("salamanderpod");
            sd_weaponsList.add("gorgon");
            sd_weaponsList.add("gorgonpod");
            sd_weaponsList.add("hydra");
            sd_weaponsList.add("gazer");
            sd_weaponsList.add("gazerpod");
            sd_weaponsList.add("sabot_single");
            sd_weaponsList.add("sabotpod");
            sd_weaponsList.add("breach");
            sd_weaponsList.add("breachpod");
            sd_weaponsList.add("phasecl");
            sd_weaponsList.add("squall");
        }

        for (String weapon : sd_weaponsList) {
            Global.getSettings().getWeaponSpec(weapon).addTag("sd_arsenal_package");
        }
    }
}