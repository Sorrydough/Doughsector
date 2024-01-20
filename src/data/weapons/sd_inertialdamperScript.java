package data.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class sd_inertialdamperScript implements OnFireEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        MissileAPI missile = (MissileAPI) projectile;
        Vector2f newVel = Misc.rotateAroundOrigin(new Vector2f(missile.getMaxSpeed(),0),missile.getFacing());
        missile.getVelocity().set(newVel.x,newVel.y);
    }
}
