package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;

@SuppressWarnings("unused")
public class sd_antimatterarray implements OnFireEffectPlugin {
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		//makes it influenced by systems expertise range mult, randomize the velocity a bit as well, so it looks nicer
		float rangeBonus = weapon.getShip().getMutableStats().getSystemRangeBonus().computeEffective(weapon.getRange()) / weapon.getRange();
		float speedMult = (0.25f + 0.75f * (float) Math.random()) * rangeBonus;
		projectile.getVelocity().scale(speedMult);
		
		float angVel = (float) (Math.signum((float) Math.random() - 0.5f) * (0.5f + Math.random()) * 720f);
		projectile.setAngularVelocity(angVel);

		float delay = 0.25f + 0.75f * (float) Math.random();
		weapon.setRefireDelay(delay);

		//loc, vel, size,
		//brightness, duration, color
		Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(24f, 48f),
				MathUtils.getRandomNumberInRange(0.33f, 1.33f), MathUtils.getRandomNumberInRange(0.33f, 1.33f), new Color(150,100,255, 150));
		Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(24f, 48f),
				MathUtils.getRandomNumberInRange(0.33f, 1.33f), MathUtils.getRandomNumberInRange(0.33f, 1.33f), new Color(155,100,255,255));
	}
}