package data.shipsystems;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class sd_shuntoverride extends BaseShipSystemScript {

	Object KEY_SHIP = new Object();
	final IntervalUtil arcInterval = new IntervalUtil(0.25f, 0.5f); //timing on visual arcs
	final IntervalUtil flareInterval = new IntervalUtil(0.1f, 0.1f); //timing on firing flares, should be set to whatever is in the .wpn file
	final Random rand = new Random();
	final IntervalUtil interval = new IntervalUtil(0.033f, 0.033f);
	boolean applyListener = true, flareDoOnce = true;
	float damageTaken, firedFlares;
	private ShipAPI ship;
	public Map<ShipAPI.HullSize, Float> METABOLIZE_MULT = new HashMap<>();
	{	//multiplier on the efficiency of turning hull damage into armor, lower number is more efficient
		METABOLIZE_MULT.put(ShipAPI.HullSize.FIGHTER, 2f);
		METABOLIZE_MULT.put(ShipAPI.HullSize.FRIGATE, 5f);
		METABOLIZE_MULT.put(ShipAPI.HullSize.DESTROYER, 6f);
		METABOLIZE_MULT.put(ShipAPI.HullSize.CRUISER, 8f);
		METABOLIZE_MULT.put(ShipAPI.HullSize.CAPITAL_SHIP, 9f);
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
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

			ShieldAPI shield = ship.getShield();
			float radius = shield.getRadius();
			float arc = shield.getActiveArc();
			// force shield on
			shield.toggleOn();
			ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
			if (ship.getSystem().isChargeup()) {
				shield.setArc(Math.max(360 * effectLevel, arc));
			}

			arcInterval.advance(timer * effectLevel);
			if (arcInterval.intervalElapsed()) {
				//continuously update the shield arc for visual effects
				arc = shield.getActiveArc();
				for (int i=0; i < 3; i++) {
					float angle1 = (float) ((Math.random() * arc - (arc * 0.5f)));
					float randDist1 = radius * MathUtils.getRandomNumberInRange(0.96f, 1.04f);
					Vector2f loc1 = MathUtils.getPointOnCircumference(ship.getLocation(), randDist1, angle1);

					float angle2 = (float) ((Math.random() * arc - (arc * 0.5f)));
					float randDist2 = radius * MathUtils.getRandomNumberInRange(0.96f, 1.04f);
					Vector2f loc2 = MathUtils.getPointOnCircumference(ship.getLocation(), randDist2, angle2);

					Global.getCombatEngine().spawnEmpArcVisual(loc1, ship, loc2, ship, 5f, ship.getHullSpec().getShieldSpec().getInnerColor(), ship.getHullSpec().getShieldSpec().getRingColor());
				}
			}
			ship.getEngineController().fadeToOtherColor(KEY_SHIP, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), effectLevel, 0.75f * effectLevel);
			ship.setJitterUnder(KEY_SHIP, ship.getHullSpec().getShieldSpec().getInnerColor(), effectLevel, 10, 0f, 10f);

		} else {
			//this is all stuff that I want to run while the script would normally be off, but we need to run it while idle so all the unapply stuff goes into here
			firedFlares = 0;
			flareDoOnce = true;
			//reset the shield arc
			if (ship.getSystem().isChargedown()) {
				ship.getShield().setArc(Math.max(360 * effectLevel, ship.getShield().getActiveArc()));
			}

			//put the armor repair functionality here
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
//						float metabolizeMult = METABOLIZE_MULT.get(ShipAPI.HullSize.FRIGATE);
//						if (stats.getVariant() != null) {
//							metabolizeMult = METABOLIZE_MULT.get(stats.getVariant().getHullSize());
//						}
//						//modify by the grid repair mult to account for the armor grid value not actually being even remotely similar to the ship's listed armor value
//						damageTaken -= (toRepair * gridRepairMult) * metabolizeMult; //then modify by the hull size efficiency modifier
//						if (damageTaken < 1) damageTaken = 0; //just in case some weird negative or decimal stuff happens
//						//puffs at the location of the repaired armor cell if it's within the bounds of the ship
//						if (CollisionUtils.isPointWithinBounds(cellLocOnModel, ship)) {
//							//engine.addFloatingText(cellLocOnModel, valueOf(totalHullDamage), 15, new Color(100f / 255f, 110f / 255f, 100f / 255f, 0.25f), ship, 10, 15); //debug text
//							//get the nearest system hardpoint, create smoke at it, and create an arc from the hardpoint to the cell being repaired
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
	}
}



