package data.scripts.graphics;

import com.fs.starfarer.api.combat.*;
import data.scripts.sd_util;
import org.lazywizard.lazylib.opengl.DrawUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

public class drawSystemRadius extends BaseCombatLayeredRenderingPlugin {
    ShipAPI ship;
    float radius;
    float width;
    Color color;
    public drawSystemRadius(ShipAPI ship, float radius, float width, Color color) {
        this.ship = ship;
        this.radius = radius;
        this.width = width;
        this.color = color;
    }
    public void render(CombatEngineLayers layer, ViewportAPI viewport) { // TODO: COPY SOTF CODE TO MAKE THIS STOP BEING BUGGY AF
        if (ship.getSystem().getState().equals(ShipSystemAPI.SystemState.IDLE)) {
            GL11.glLineWidth(3);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            DrawUtils.drawCircle(ship.getLocation().x, ship.getLocation().y, radius, 180, false);
        }
    }
}