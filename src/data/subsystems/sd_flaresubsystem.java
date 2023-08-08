package data.subsystems;

import activators.CombatActivator;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.Random;


public class sd_flaresubsystem extends CombatActivator
{
    final int BASEACTIVEDURATION = 1, COOLDOWNDURATION = 10, FLARESTOFIRE = 3;
    final IntervalUtil FLAREINTERVAL = new IntervalUtil(0.1f, 0.1f), AITIMER = new IntervalUtil(0.25f, 1f);
    boolean flareDoOnce = true;
    int firedFlares = 0;
    @Override
    public void advance(float amount)
    {
        if (!isActive())
            return;
        FLAREINTERVAL.advance(amount);
        if (firedFlares < FLARESTOFIRE && FLAREINTERVAL.intervalElapsed()) {
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (slot.isSystemSlot()) {
                    int angleOffset = new Random().nextInt(51) - 25;  //generate a random number between -25 and 25
                    float modifiedAngle = slot.getAngle() + angleOffset;
                    Global.getCombatEngine().spawnProjectile(ship, null, "sd_flarelauncher", slot.computePosition(ship), modifiedAngle + ship.getFacing(), ship.getVelocity());
                    Global.getSoundPlayer().playSound("launch_flare_1", 1.0f, 1.6f, slot.computePosition(ship), ship.getVelocity());
                }
            }
            firedFlares++;
        }
    }

    @Override
    public void onFinished() {
        firedFlares = 0;
        flareDoOnce = false;
    }

    public sd_flaresubsystem(ShipAPI ship) { super(ship); }

    @Override
    public float getBaseActiveDuration() { return BASEACTIVEDURATION; }

    @Override
    public float getBaseCooldownDuration() { return COOLDOWNDURATION; }

    @Override
    public String getDisplayText()
    {
        return "Active Flare Launcher";
    }

    @Override
    public String getStateText()
    {
        if (isOn()) return "Launching";
        else if (isCooldown()) return "Fabricating";
        else if (isOff()) return "Ready";
        else return "";
    }

    @Override
    public boolean shouldActivateAI(float amount)
    {
        AITIMER.advance(amount);
        if (ship == null || !ship.isAlive() || !state.equals(State.READY))
            return false;
        if (!AITIMER.intervalElapsed())
            return false;
        return AIUtils.getNearbyEnemyMissiles(ship, 300).size() > 2;
    }
}
