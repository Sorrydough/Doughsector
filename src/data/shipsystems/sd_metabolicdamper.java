package data.shipsystems;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.*;

public class sd_metabolicdamper extends BaseShipSystemScript {
	public static Map<HullSize, Float> DAMPER_MULT = new HashMap<>();
	static {	//incoming damage is reduced to this %
		DAMPER_MULT.put(HullSize.FIGHTER, 0.33f);
		DAMPER_MULT.put(HullSize.FRIGATE, 0.33f);
		DAMPER_MULT.put(HullSize.DESTROYER, 0.33f);
		DAMPER_MULT.put(HullSize.CRUISER, 0.5f);
		DAMPER_MULT.put(HullSize.CAPITAL_SHIP, 0.5f);
	}

	public static Map<HullSize, Float> METABOLIZE_MULT = new HashMap<>();
	static {	//multiplier on the efficiency of turning hull damage into armor, lower number is more efficient
		METABOLIZE_MULT.put(HullSize.FIGHTER, 2f);
		METABOLIZE_MULT.put(HullSize.FRIGATE, 4f);
		METABOLIZE_MULT.put(HullSize.DESTROYER, 5f);
		METABOLIZE_MULT.put(HullSize.CRUISER, 6f);
		METABOLIZE_MULT.put(HullSize.CAPITAL_SHIP, 8f);
	}
	Object KEY_SHIP = new Object();
	protected Object STATUSKEY1 = new Object();
	final IntervalUtil flareInterval = new IntervalUtil(0.1f, 0.1f); //timing on firing flares, should be set to whatever is in the .wpn file
	final Random rand = new Random();
	final IntervalUtil interval = new IntervalUtil(0.033f, 0.033f);
	boolean applyListener = true, flareDoOnce = true;
	float damageTaken, firedFlares;
	private ShipAPI ship;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		if (Global.getCombatEngine() == null)
			return;
		ship = (ShipAPI) stats.getEntity();
		float timer = Global.getCombatEngine().getElapsedInLastFrame();
		if (ship.getSystem().isActive()) {
			if (applyListener) { //apply a listener to check for how much damage got succed
				ship.addListener(new DamageListener() {
					@Override
					public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
						if (target instanceof ShipAPI && ship.getSystem().isActive()) {
							damageTaken += result.getDamageToHull();
							//damageTaken += result.getDamageToPrimaryArmorCell()/50; //had to comment this out, seems like it was a bit overpowered
						}
					}
				});
				applyListener = false;
			}

			flareInterval.advance(timer);
			if (firedFlares < 3 && flareInterval.intervalElapsed()) {
				for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
					if (slot.isSystemSlot()) {
						int angleOffset = rand.nextInt(51) - 25;  //generate a random number between -25 and 25
						float modifiedAngle = slot.getAngle() + angleOffset;
						Global.getCombatEngine().spawnProjectile(ship, null, "sd_flarelauncher", slot.computePosition(ship), modifiedAngle + ship.getFacing(), ship.getVelocity());
						Global.getSoundPlayer().playSound("launch_flare_1", 1.0f, 1.6f, slot.computePosition(ship), ship.getVelocity());
					}
				}
				firedFlares++;
			}
			if (ship.getSystem().isChargedown() && flareDoOnce) {
				firedFlares = 0;
				flareDoOnce = false;
			}

			ship.fadeToColor(KEY_SHIP, new Color(75, 75, 75, 255), 0.1f, 0.1f, effectLevel);
			ship.getEngineController().fadeToOtherColor(KEY_SHIP, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), effectLevel, 0.75f * effectLevel);
			//ship.setJitterUnder(KEY_SHIP, new Color(200, 200, 200, 255), effectLevel, 15, 0f, 15f);
			ship.setJitterUnder(KEY_SHIP, new Color(250,235,215, 255), effectLevel, 10, 0f, 15f);

			float damperMult = DAMPER_MULT.get(HullSize.FRIGATE);
			if (stats.getVariant() != null) {
				damperMult = DAMPER_MULT.get(stats.getVariant().getHullSize());
			}
			stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);
			stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);
			stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - damperMult) * effectLevel);

			if (player) {
				ShipSystemAPI system = getDamper(ship);
				if (system != null) {
					float percent = (1f - damperMult) * effectLevel * 100;
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1, system.getSpecAPI().getIconSpriteName(), system.getDisplayName(), Math.round(percent) + "% less damage taken", false);
				}
			}

		} else {
			//this is all stuff that I want to run while the script would normally be off, but we need to run it while idle so all the unapply stuff goes into here
			stats.getHullDamageTakenMult().unmodify(id);
			stats.getArmorDamageTakenMult().unmodify(id);
			stats.getEmpDamageTakenMult().unmodify(id);
			firedFlares = 0;
			flareDoOnce = true;

			//if the damper isn't active, we try to consume totalHullDamage to repair the armor
//			if (damageTaken > 0) {
//				CombatEngineAPI engine = Global.getCombatEngine();
//				//don't run code when we shouldn't dumb human
//				if (engine.isPaused() || !ship.isAlive() || ship.isPiece() || engine.isCombatOver()) return;
//				interval.advance(engine.getElapsedInLastFrame() * stats.getTimeMult().getModifiedValue());
//				if (interval.intervalElapsed()) {
//					ArmorGridAPI armorGrid = ship.getArmorGrid();
//					///thanks ruddygreat for fixing my illegible code
//					float maxRepPerTick = armorGrid.getMaxArmorInCell() * 0.25f; //25% max rep per cell per tick
//					//get a random grid square, then find its location on the physical ship itself
//					Vector2f cellLocOnGrid = new Vector2f(rand.nextInt(armorGrid.getGrid().length), rand.nextInt(armorGrid.getGrid()[0].length));
//					Vector2f cellLocOnModel = armorGrid.getLocation((int) cellLocOnGrid.x, (int) cellLocOnGrid.y);
//					//use the size of the grid and the maximum armor per cell to calculate the ship's ACTUAL amount of armor
//					float actualArmorTotalMax = ((armorGrid.getGrid().length * armorGrid.getGrid()[0].length) * armorGrid.getMaxArmorInCell());
//					//now that we know how much armor the ship actually has, we need to generate a number that we can use later to convert hull damage into repaired armor at the intended ratio
//					float gridRepairMult = ship.getArmorGrid().getArmorRating()/actualArmorTotalMax;
//					//find the max armor of the cell, then find out how much is missing, then replenish the missing amount
//					float armorAtLoc = armorGrid.getArmorValue((int) cellLocOnGrid.x, (int) cellLocOnGrid.y);
//					if (armorAtLoc < armorGrid.getMaxArmorInCell()) {
//						float toRepair = armorGrid.getMaxArmorInCell() - armorAtLoc;
//						//it's better to have a bunch of partially repaired cells than to have 1 fully repaired cell and many empty ones
//						if (toRepair > maxRepPerTick) toRepair = maxRepPerTick;
//						armorGrid.setArmorValue((int) cellLocOnGrid.x, (int) cellLocOnGrid.y, armorAtLoc + toRepair);
//						//subtract the amount we repaired from the available pool, with an efficiency dependent on hull size
//						float metabolizeMult = METABOLIZE_MULT.get(HullSize.FRIGATE);
//						if (stats.getVariant() != null) {
//							metabolizeMult = METABOLIZE_MULT.get(stats.getVariant().getHullSize());
//						}
//						//modify by the grid repair mult to account for the armor grid value not actually being even remotely similar to the ship's listed armor value
//						damageTaken -= (toRepair * gridRepairMult) * metabolizeMult; //then modify by the hull size efficiency modifier
//						if (damageTaken < 1) damageTaken = 0; //just in case some weird negative or decimal stuff happens
//						//puffs at the location of the repaired armor cell if it's within the bounds of the ship
//						if (CollisionUtils.isPointWithinBounds(cellLocOnModel, ship)) {
//							//engine.addFloatingText(cellLocOnModel, valueOf(totalHullDamage), 15, new Color(100f / 255f, 110f / 255f, 100f / 255f, 0.25f), ship, 10, 15); //debug text
//							for (int i = 0; i < 25; i++) {
//								//smoke
//								Vector2f nebVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(10f, 40f), MathUtils.getRandomNumberInRange(-180f, 180f));
//								float randomSize2 = MathUtils.getRandomNumberInRange(20f, 32f);
//								Color steamColor = new Color(100f / 255f, 110f / 255f, 100f / 255f, 0.25f);
//								engine.addNebulaParticle(MathUtils.getRandomPointOnCircumference(cellLocOnModel, 5f), nebVel, randomSize2, 1.8f, 0.6f, 0.7f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), steamColor);
//								//sparks
//								engine.addSmoothParticle(cellLocOnModel, ship.getVelocity(), MathUtils.getRandomNumberInRange(25f, 35f), 0.8f, 0.1f, new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
//								Vector2f fastParticleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(80f, 250f), MathUtils.getRandomNumberInRange(-180f, 180f));
//								float randomSize01 = MathUtils.getRandomNumberInRange(3f, 5f);
//								engine.addSmoothParticle(MathUtils.getRandomPointOnCircumference(cellLocOnModel, 4f), fastParticleVel, randomSize01, 0.8f, MathUtils.getRandomNumberInRange(0.2f, 0.25f), new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
//								Vector2f particleVel = MathUtils.getPointOnCircumference(ship.getVelocity(), MathUtils.getRandomNumberInRange(35f, 125f), MathUtils.getRandomNumberInRange(-120f, 120f));
//								float randomSize1 = MathUtils.getRandomNumberInRange(3f, 5f);
//								engine.addSmoothParticle(MathUtils.getRandomPointOnCircumference(cellLocOnModel, 4f), particleVel, randomSize1, 0.8f, MathUtils.getRandomNumberInRange(0.35f, 0.5f), new Color(1f, 120f / 255f, 80f / 255f, 0.25f));
//							}
//						}
//						ship.syncWithArmorGridState();
//						ship.syncWeaponDecalsWithArmorDamage();
//					}
//				}
//			}
		}
//		if (player) {
//			ShipSystemAPI system = getDamper(ship);
//			if (system != null) {
//				Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1, system.getSpecAPI().getIconSpriteName(), system.getDisplayName(), valueOf(totalHullDamage), false);
//			}
//		}
	}

	public ShipSystemAPI getDamper(ShipAPI ship) {
		ShipSystemAPI system = ship.getPhaseCloak();
		if (system != null && system.getId().equals("damper")) return system;
		if (system != null && system.getId().equals("damper_omega")) return system;
		if (system != null && system.getId().equals("sd_metabolicdamper")) return system;
		if (system != null && system.getSpecAPI() != null && system.getSpecAPI().hasTag(Tags.SYSTEM_USES_DAMPER_FIELD_AI)) return system;
		return ship.getSystem();
	}
}



