//By Nicke535
//Causes a decorative weapon to randomly flicker on and off with certain animations, depending on hull level.
package data.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class sd_blinkerScript implements EveryFrameWeaponEffectPlugin {

    //The minimum and maximum duration the lights are "completely off" during the animation (how long they stay at the final animation frame)
    final float MAX_OFF_DURATION = 5f;
    final float MIN_OFF_DURATION = 1f;

    //The minimum and maximum chance for a blink cycle to occur each time interval: minimum is at 100% hull level, maximum is at 0% hull level
    final float MAX_BLINK_CHANCE = 0.5f;
    final float MIN_BLINK_CHANCE = 0.05f;

    //How often the script checks to start a new blink cycle
    final IntervalUtil blinkInterval = new IntervalUtil(1f, 2f);

    //Lists all the animations of the script: one of these is chosen randomly each time the light goes off
    //Note that the last frame of the animation is the state which it will keep until being played in reverse
    //The same animation will then be played again, in reverse, once the lights go back on
    final List<AnimFrameHolder> ALL_ANIMATIONS = new ArrayList<>();
    {
        
                //Instant Turnoff
        ALL_ANIMATIONS.add(new AnimFrameHolder(
                new AnimFrame(0f, 1f),
                new AnimFrame(0.05f, 0.5f),
                new AnimFrame(0.1f, 1f),
                new AnimFrame(0.15f, 0f)
        ));
        
        //Instant Turnoff with Partial Glow
        ALL_ANIMATIONS.add(new AnimFrameHolder(
                new AnimFrame(0f, 1f),
                new AnimFrame(0.05f, 0.5f),
                new AnimFrame(0.1f, 1f),
                new AnimFrame(0.15f, 0.3f)
        ));

        //Flickering
        ALL_ANIMATIONS.add(new AnimFrameHolder(
                new AnimFrame(0f, 1f),
                new AnimFrame(0.05f, 0.5f),
                new AnimFrame(0.1f, 1f),
                new AnimFrame(0.15f, 0.5f),
                new AnimFrame(0.2f, 1f),
                new AnimFrame(0.25f, 0.5f),
                new AnimFrame(0.2f, 1f),
                new AnimFrame(0.25f, 0.5f),
                new AnimFrame(0.3f, 1f),
                new AnimFrame(0.35f, 0f)
        ));
    }

    //The color of the deco weapon, not including opacity
    final float[] COLOR = { 1f, 1f, 1f};

    //Internal script variables
    float counter = 0f;
    float currentBlinkOffDuration = 0f;
    int currentBlinkType = -1;

    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        ShipAPI ship = weapon.getShip();
        weapon.getSprite().setAdditiveBlend();
        
        if (ship == null) {
            return;
        }

        //Always point at screenspace top
        if (engine == null || !Global.getCurrentState().equals(GameState.COMBAT)) {
            //This is for refit
            weapon.setCurrAngle(0f);
        } else {
            weapon.setCurrAngle(90f);
        }

        //First: are we a hulk? If so, instantly lose all opacity: no blinkers on hulks
        float brightnessThisFrame = 0f;
        if (ship.isHulk()) {
            brightnessThisFrame = 0f;
        }

        //Otherwise, we proceed with checks
        else {
            //Are we currently in a blink cycle? If so, handle that
            if (currentBlinkType >= 0) {
                List<AnimFrame> currentFrames = ALL_ANIMATIONS.get(currentBlinkType).frames;

                //Increase our counter
                counter += amount;

                //If we're past the end of the animation, stop our animation
                float fullDuration = currentFrames.get(currentFrames.size()-1).time * 2f + currentBlinkOffDuration;
                if (counter >= fullDuration) {
                    currentBlinkType = -1;
                    brightnessThisFrame = 1f;
                    counter = 0f;
                }

                //Otherwise, calculate the opacity to use this frame
                else {
                    float counterToUse = counter;

                    //At the halfway point, we invert behaviour
                    if (counter >= fullDuration/2f) {
                        counterToUse = fullDuration - counter;
                    }

                    //Check for which frames to look at
                    for (int i = 0; i < currentFrames.size(); i++) {
                        //Last frame stuff
                        if (i == currentFrames.size()-1) {
                            if (counterToUse > currentFrames.get(i).time) {
                                brightnessThisFrame = currentFrames.get(i).opacity;
                                break;
                            }
                        }

                        //Check if we're between this frame and the next one
                        else if (currentFrames.get(i).time <= counterToUse && currentFrames.get(i+1).time > counterToUse) {
                            //Calculate how far between the frames we are, and interpolate
                            float progressBetween = (counterToUse - currentFrames.get(i).time) / (currentFrames.get(i+1).time - currentFrames.get(i).time);
                            brightnessThisFrame = (progressBetween * currentFrames.get(i+1).opacity) + ((1f-progressBetween) * currentFrames.get(i).opacity);
                            break;
                        }
                    }
                }
            }

            //Otherwise, check if we should start a blink cycle
            else {
                brightnessThisFrame = 1f;
                blinkInterval.advance(amount);
                if (blinkInterval.intervalElapsed()) {
                    if (Math.random() < ((ship.getHardFluxLevel() * MIN_BLINK_CHANCE) + ((1f- ship.getHardFluxLevel()) * MAX_BLINK_CHANCE))) {
                        counter = 0f;
                        //Chooses a random blink type for this cycle
                        currentBlinkType = MathUtils.getRandomNumberInRange(0, ALL_ANIMATIONS.size()-1);
                        brightnessThisFrame = ALL_ANIMATIONS.get(currentBlinkType).frames.get(0).opacity;

                        //Also select a random blink off duration in the process
                        currentBlinkOffDuration = MathUtils.getRandomNumberInRange(MIN_OFF_DURATION, MAX_OFF_DURATION);
                    }
                }
            }
        }

        //And finally actually apply the color
        weapon.getSprite().setColor(new Color(COLOR[0], COLOR[1], COLOR[2], brightnessThisFrame));
    }

    //Keeps track of animation frames
    static class AnimFrameHolder {
        List<AnimFrame> frames = new ArrayList<>();
        AnimFrameHolder (AnimFrame... frames) { this.frames.addAll(Arrays.asList(frames)); }
    }


    //Keeps track of animation pairs
    static class AnimFrame {
        float time;
        float opacity;
        AnimFrame (float time, float opacity) {
            this.time = time;
            this.opacity = opacity;
        }
    }
}