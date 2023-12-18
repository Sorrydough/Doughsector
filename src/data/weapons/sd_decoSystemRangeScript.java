package data.weapons;

import com.fs.starfarer.api.combat.*;
import data.graphics.sd_drawSystemRadius;
import data.sd_util;
import org.lwjgl.util.Color;

public class sd_decoSystemRangeScript implements EveryFrameWeaponEffectPlugin {
    final Color colorOuter = new Color(250, 235, 215,55);
    final Color colorInner = new Color(250, 235, 215,5);
    sd_drawSystemRadius pluginOuter = null;
    sd_drawSystemRadius pluginInner = null;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (engine.isPaused() || ship == null)
            return;

        if (pluginOuter == null) {
            pluginOuter = new sd_drawSystemRadius(ship, sd_util.getOptimalRange(ship) + ship.getCollisionRadius(), 2, colorOuter, false);
            engine.addLayeredRenderingPlugin(pluginOuter);
        }
        if (pluginInner == null) {
            pluginInner = new sd_drawSystemRadius(ship, sd_util.getOptimalRange(ship) + ship.getCollisionRadius(), 2, colorInner, true);
            engine.addLayeredRenderingPlugin(pluginInner);
        }
        if (ship.getSystem().isOn()) {
            weapon.setForceFireOneFrame(true);
        }
    }
}
