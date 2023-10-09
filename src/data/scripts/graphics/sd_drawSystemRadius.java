package data.scripts.graphics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.sd_util;
import org.lazywizard.lazylib.opengl.DrawUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class sd_drawSystemRadius extends BaseCombatLayeredRenderingPlugin {
    final Map<ShipSystemAPI.SystemState, Float> ALPHA_MULT = new HashMap<>(); {
        ALPHA_MULT.put(ShipSystemAPI.SystemState.IN, 0.5f);
        ALPHA_MULT.put(ShipSystemAPI.SystemState.ACTIVE, 0.25f);
        ALPHA_MULT.put(ShipSystemAPI.SystemState.OUT, 0.5f);
        ALPHA_MULT.put(ShipSystemAPI.SystemState.COOLDOWN, 0.25f);
        ALPHA_MULT.put(ShipSystemAPI.SystemState.IDLE, 1f);
    }
    ShipAPI ship;
    final float radius;
    final float width;
    Color color;
    boolean drawFilled;
    final IntervalUtil interval = new IntervalUtil(0.25f, 0.25f);
    public sd_drawSystemRadius(ShipAPI ship, float radius, float width, Color color, boolean drawFilled) {
        this.ship = ship;
        this.radius = radius;
        this.width = width;
        this.color = color;
        this.drawFilled = drawFilled;
    }
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
    }
    public float getRenderRadius() {
        return radius + 500f;
    }
    float timer = 1;
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
            ShipSystemAPI system = ship.getSystem();
            interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (interval.intervalElapsed()) { // using some timer shenanigans to scale opacity based on system state
                if (system.getState().equals(ShipSystemAPI.SystemState.COOLDOWN) && timer > 0.25) {
                    timer -= 0.05;
                    if (timer < 0.25)
                        timer = 0.2f;
                }
                if (system.getState().equals(ShipSystemAPI.SystemState.IDLE) && timer < 1) {
                    timer += 0.1;
                    if (timer > 1)
                        timer = 1;
                }
            }
            float alpha = color.getAlpha() * timer;
            GL11.glLineWidth(3);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha / 255f);
            DrawUtils.drawCircle(ship.getLocation().x, ship.getLocation().y, radius, 180, drawFilled);
        }
    }
}