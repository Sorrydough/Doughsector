package data.scripts.shaders;

import cmu.CMUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.combat.entities.Ship;
import com.fs.starfarer.combat.systems.Flare;
import com.fs.starfarer.loading.specs.EngineSlot;
import data.scripts.shaders.util.fer_FlareRenderer;
import data.scripts.shaders.util.fer_GlowRenderer;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class sd_flareTrackerPlugin extends BaseEveryFrameCombatPlugin {

    public static final String HULLSTYLE_ID = "SD_SPEC";

    private fer_FlareRenderer renderer;
    private fer_GlowRenderer glowRenderer;

    private Map<ShipAPI, List<fer_EnginePlugin.FlareData>> shipFlares = new HashMap<>();

    @Override
    public void init(CombatEngineAPI engine) {
        int textureID = Global.getSettings().getSprite("fer", "sd_blockThruster").getTextureId();
        renderer = new fer_FlareRenderer(textureID, "data/shaders/engine_tex_noflare.frag");
        glowRenderer = new fer_GlowRenderer();

        renderer.setLayer(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        glowRenderer.setLayer(CombatEngineLayers.ABOVE_SHIPS_LAYER);

        shipFlares = new HashMap<>();

        engine.addLayeredRenderingPlugin(renderer);
        engine.addLayeredRenderingPlugin(glowRenderer);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (!engine.isPaused()) {

            // logic to ensure ship set contain up date to list of existing ships

            List<ShipAPI> toRemove = new ArrayList<>();
            for (ShipAPI ship : shipFlares.keySet()) {
                if (!engine.isEntityInPlay(ship)) toRemove.add(ship);
            }
            for (ShipAPI ship : toRemove) {
                shipFlares.remove(ship);
            }

            List<ShipAPI> newShips = new ArrayList<>();
            for (ShipAPI ship : engine.getShips()) {
                if (!ship.isAlive() || ship.isHulk()) continue;

                if (ship.getHullStyleId().equals(HULLSTYLE_ID)) {
                    ship.setRenderEngines(false);

                    boolean newShip = !shipFlares.containsKey(ship);

                    if (newShip) {
                        List<fer_EnginePlugin.FlareData> flares = new ArrayList<>();
                        shipFlares.put(ship, flares);
                        newShips.add(ship);
                    }
                }
            }

            // logic to add/remove engine flares

            for (ShipAPI ship : newShips) {
                List<fer_EnginePlugin.FlareData> flares = shipFlares.get(ship);

                for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
                    fer_BaseFlare flare = new fer_BaseFlare(new Vector2f(0f, 0f), new Vector2f(0f, 0f), Color.WHITE, 0f);
                    flares.add(new fer_EnginePlugin.FlareData(eng, flare, ship, ship.getAngularVelocity()));
                }
            }

            // logic to update flares

            for (List<fer_EnginePlugin.FlareData> flares : shipFlares.values()) {
                for (fer_EnginePlugin.FlareData data : flares) {
                    fer_BaseFlare flare = data.flare;
                    float angle = data.engine.getEngineSlot().getAngle();
                    ShipAPI ship = (ShipAPI) data.entity;
                    Vector2f size = new Vector2f(
                            data.engine.getEngineSlot().getLength() * (ship.getEngineController().getExtendLengthFraction().getCurr() + 1f),
                            data.engine.getEngineSlot().getWidth() * (ship.getEngineController().getExtendWidthFraction().getCurr() + 1f)
                    );
                    Vector2f pos = new Vector2f(data.engine.getEngineSlot().computePosition(ship.getLocation(), ship.getFacing()));
                    Color color = data.engine.getEngineColor();

                    flare.setAngle(angle + ship.getFacing());
                    flare.setSize(size);
                    flare.setLocation(pos);
                    flare.setColor(color);

                    float targetL = 0.3f, targetW = 0.6f, targetG = 0.6f;

                    if (ship.getEngineController().isStrafingLeft() || ship.getEngineController().isStrafingRight()) {
                        targetW = 1.1f;
                        targetG = 1.3f;
                    }

                    if (ship.getEngineController().isAccelerating()) {
                        targetL = 0.9f;
                        targetG = 1.2f;
                        targetW = 1f;
                    } else if (ship.getEngineController().isAcceleratingBackwards()) {
                        targetL = 0.4f;
                        targetW = 1.2f;
                        targetG = 2f;
                    }

                    if (ship.getEngineController().isDecelerating()) {
                        targetW = 1.2f;
                        targetL = 0.2f;
                        targetG = 1.4f;
                    }

                    if (((EngineSlot) data.engine.getEngineSlot()).isSystemActivated()) {
                        if (ship.getSystem() != null && ship.getSystem().isOn()) {
                            flare.setDisabled(false);
                        } else {
                            targetG = 0f;
                            targetL = 0f;
                            targetW = 0f;

                            flare.setDisabled(true);
                        }
                    }

                    if (ship.getPhaseCloak() != null) {
                        flare.setDisabled(ship.isPhased());
                    }

                    if (ship.getFluxTracker().isEngineBoostActive()) {
                        targetG *= 1.3f;
                        targetL *= 1.3f;
                        targetW *= 1.3f;
                    }

                    if (data.engine.isDisabled()) {
                        targetG = 0f;
                        targetL = 0f;
                        targetW = 0f;
                    }

                    float dl = (targetL - flare.getLevelLength()) * amount * 2.25f;
                    float dw = (targetW - flare.getLevelWidth()) * amount * 1.75f;
                    flare.setLevelLength(MathUtils.clamp(dl + flare.getLevelLength(), 0.2f, 3f));
                    flare.setLevelWidth(MathUtils.clamp(dw + flare.getLevelWidth(), 0.2f, 2f));

                    float g = MathUtils.clamp(((targetG - flare.getGlowSize()) * amount) + flare.getGlowSize(), 0.8f, 1.2f);
                    flare.setGlowSize(g * 0.5f);

                    flare.setContrailSize(data.engine.getEngineSlot().getContrailWidth());
                }
            }
        }

        // set updated list of flare targets

        List<fer_EnginePlugin.FlareData> data = new ArrayList<>();
        for (List<fer_EnginePlugin.FlareData> flares : shipFlares.values()) {
            data.addAll(flares);
        }
        renderer.setDrawTargets(data);
        glowRenderer.setDrawTargets(data);
    }
}
