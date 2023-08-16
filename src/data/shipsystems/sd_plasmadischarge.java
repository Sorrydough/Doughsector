package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;

public class sd_plasmadischarge implements OnFireEffectPlugin {
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		//make this influenced by systems expertise range mult
		//verify that energy weapon mastery boosts damage
		float speedMult = 0.25f + 0.75f * (float) Math.random();
		projectile.getVelocity().scale(speedMult);
		
		float angVel = (float) (Math.signum((float) Math.random() - 0.5f) * (0.5f + Math.random()) * 720f);
		projectile.setAngularVelocity(angVel);

		float delay = 0.25f + 0.75f * (float) Math.random();
		weapon.setRefireDelay(delay);

		//loc, vel, size,
		//brightness, duration, color
		Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(24f, 48f),
				MathUtils.getRandomNumberInRange(0.33f, 1.33f), MathUtils.getRandomNumberInRange(0.33f, 1.33f), new Color(100,100,255,150));
		Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(24f, 48f),
				MathUtils.getRandomNumberInRange(0.33f, 1.33f), MathUtils.getRandomNumberInRange(0.33f, 1.33f), new Color(255,100,100,150));
	}
}