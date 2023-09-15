package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import java.util.ArrayList;
import java.util.List;

import data.scripts.world.sd_moonGenerator;

@SuppressWarnings("unused")
public class sd_modPlugin extends BaseModPlugin {
//    public void
//    when the player loads their save, if their phase ships doctrine is set to 0, it removes the gremlin blueprint
    // maybe all the other bps too?

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
            sd_weaponsList.add("gigacannon");

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
            sd_weaponsList.add("reaper");
            sd_weaponsList.add("typhoon");
            sd_weaponsList.add("cyclone");
            sd_weaponsList.add("gazer");
            sd_weaponsList.add("gazerpod");
            sd_weaponsList.add("sabot_single");
            sd_weaponsList.add("sabotpod");
            sd_weaponsList.add("squall");
        }

        for (String weapon : sd_weaponsList) {
            Global.getSettings().getWeaponSpec(weapon).addTag("sd_arsenal_package");
        }
    }

    @Override
    public void onNewGameAfterProcGen() {
        new sd_moonGenerator().generate(Global.getSector());
    }
}