package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.graphics.drawSystemRadius;
import data.scripts.sd_util;
import org.lazywizard.lazylib.opengl.DrawUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

public class sd_destroyerinterferenceDecoScript implements EveryFrameWeaponEffectPlugin {
    final Color color = new Color(255, 0, 0, 255);
    drawSystemRadius plugin = null;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (plugin == null) {
            plugin = new drawSystemRadius(ship, sd_util.getOptimalRange(ship), 2, color);
            Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
        }
        if (ship.getSystem().isOn()) {
            weapon.setForceFireOneFrame(true);
        }
    }
}
