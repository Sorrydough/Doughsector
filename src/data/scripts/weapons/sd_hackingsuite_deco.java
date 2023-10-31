package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.graphics.sd_drawSystemRadius;
import data.scripts.sd_util;
import org.lwjgl.util.Color;

public class sd_hackingsuite_deco implements EveryFrameWeaponEffectPlugin {
    final Color colorOuter = new Color(250, 235, 215,105);
    final Color colorInner = new Color(250, 235, 215,15);
    sd_drawSystemRadius pluginOuter = null;
    sd_drawSystemRadius pluginInner = null;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (pluginOuter == null) {
            pluginOuter = new sd_drawSystemRadius(ship, sd_util.getOptimalRange(ship) + ship.getCollisionRadius(), 2, colorOuter, false);
            engine.addLayeredRenderingPlugin(pluginOuter);
        }
        if (pluginInner == null) {
            pluginInner = new sd_drawSystemRadius(ship, sd_util.getOptimalRange(ship) + ship.getCollisionRadius(), 2, colorInner, true);
            engine.addLayeredRenderingPlugin(pluginInner);
        }
    }
}