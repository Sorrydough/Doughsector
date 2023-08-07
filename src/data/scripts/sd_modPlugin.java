package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

public class sd_modPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() throws Exception {
        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            ShaderLib.init();
            TextureData.readTextureDataCSV("data/lights/sd_texture_data.csv");
        }
    }
}