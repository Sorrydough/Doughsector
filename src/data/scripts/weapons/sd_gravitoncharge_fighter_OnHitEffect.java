package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;

public class sd_gravitoncharge_fighter_OnHitEffect implements OnHitEffectPlugin {

    final HashSet<CombatEntityAPI> hitTargets = new HashSet<>();
    final float ratio = 3;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI))
            return;
        if (hitTargets.contains(target))
            return;
        else
            hitTargets.add(target);
        if (shieldHit) {
            float targetRemainingCapacity = (1 - ((ShipAPI) target).getFluxLevel()) * ((ShipAPI) target).getMutableStats().getFluxCapacity().getModifiedValue();
            ((ShipAPI) target).getFluxTracker().increaseFlux(Math.min(projectile.getDamageAmount() * ratio, targetRemainingCapacity), true);
        }
    }
}
