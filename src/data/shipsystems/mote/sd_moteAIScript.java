package data.shipsystems.mote;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import data.shipsystems.sd_motearmor;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class sd_moteAIScript implements MissileAIPlugin {
	public static float MAX_HARD_AVOID_RANGE = 250;
	public static float AVOID_RANGE = 50;
	public static float COHESION_RANGE = 100;
	public static float ATTRACTOR_LOCK_STOP_FLOCKING_ADD = 250;
	protected MissileAPI missile;
	protected IntervalUtil tracker = new IntervalUtil(0.05f, 0.1f);
	protected IntervalUtil updateListTracker = new IntervalUtil(0.05f, 0.1f);
	protected List<MissileAPI> missileList = new ArrayList<>();
	protected List<CombatEntityAPI> hardAvoidList = new ArrayList<>();
	protected float random;
	protected CombatEntityAPI target;
	protected sd_motearmor.SharedMoteAIData data;
	
	public sd_moteAIScript(MissileAPI missile) {
		this.missile = missile;
		data = sd_motearmor.getSharedData(missile.getSource());
		random = (float) Math.random();
		elapsed = -(float) Math.random() * 0.5f;
		updateHardAvoidList();
	}

	protected IntervalUtil flutterCheck = new IntervalUtil(2f, 4f);
	protected float flutterRemaining = 0f;
	protected float elapsed;
	public void advance(float amount) {
		if (missile.isFizzling() || missile.getSource() ==  null)
			return;

		elapsed += amount;

		updateListTracker.advance(amount);
		if (updateListTracker.intervalElapsed())
			updateHardAvoidList();

		if (flutterRemaining <= 0) {
			flutterCheck.advance(amount);
			if (flutterCheck.intervalElapsed() && (Math.random() > 0.9f || (data.attractorLock != null && Math.random() > 0.5f)))
				flutterRemaining = 2f + (float) Math.random() * 2f;
		}

		if (elapsed >= 0.5f) {
			boolean wantToFlock = !isTargetValid();
			if (data.attractorLock != null) {
				float dist = Misc.getDistance(missile.getLocation(), data.attractorLock.getLocation());
				if (dist > data.attractorLock.getCollisionRadius() + ATTRACTOR_LOCK_STOP_FLOCKING_ADD)
					wantToFlock = true;
			}

			if (wantToFlock) {
				doFlocking();
			} else {
				CombatEngineAPI engine = Global.getCombatEngine();
				Vector2f targetLoc = engine.getAimPointWithLeadForAutofire(missile, 1.5f, target, 50);
				engine.headInDirectionWithoutTurning(missile, Misc.getAngleInDegrees(missile.getLocation(), targetLoc),10000);
				if (random > 0.5f) {
					missile.giveCommand(ShipCommand.TURN_LEFT);
				} else {
					missile.giveCommand(ShipCommand.TURN_RIGHT);
				}
				missile.getEngineController().forceShowAccelerating();
			}
		}

		tracker.advance(amount);
		if (tracker.intervalElapsed())
			if (elapsed >= 0.5f)
				acquireNewTargetIfNeeded();
	}


	boolean isTargetValid() {
		if (target instanceof ShipAPI) {
			ShipAPI thing = (ShipAPI) target;
			if (thing.isPhased() || thing.isHulk())
				return false;
		}
		if (target instanceof MissileAPI) {
			MissileAPI thing = (MissileAPI) target;
			if (!Global.getCombatEngine().isInPlay(thing) || thing.isFizzling())
				return false;
		}
		return target != null && target.getOwner() != missile.getOwner();
	}

	// put ships and asteroids into a list of shit to avoid, uses an AI grid iterator because alex thinks it should
	void updateHardAvoidList() {
		hardAvoidList.clear();
		Iterator<Object> iter = Global.getCombatEngine().getAiGridShips().getCheckIterator(missile.getLocation(),MAX_HARD_AVOID_RANGE * 2f,MAX_HARD_AVOID_RANGE * 2f);
		while (iter.hasNext()) {
			Object nextObject = iter.next();
			if (nextObject instanceof ShipAPI)
				hardAvoidList.add((ShipAPI) nextObject);
		}
		iter = Global.getCombatEngine().getAiGridAsteroids().getCheckIterator(missile.getLocation(),MAX_HARD_AVOID_RANGE * 2f,MAX_HARD_AVOID_RANGE * 2f);
		while (iter.hasNext()) {
			Object nextObject = iter.next();
			if (nextObject instanceof CombatEntityAPI)
				hardAvoidList.add((CombatEntityAPI) nextObject);
		}
	}

	protected void acquireNewTargetIfNeeded() {
		if (data.attractorLock != null) {
			target = data.attractorLock;
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();

		// want to: target nearest missile that is not targeted by another two motes already
		int owner = missile.getOwner();

		int maxMotesPerTarget = 1;
		ShipAPI source = missile.getSource();
		float hardfluxCollision = source.getCollisionRadius() * source.getHardFluxLevel();
		// distance from source ship to seek targets
		float maxDistFromSourceShip = source.getCollisionRadius() + hardfluxCollision;
		// distance from attractor target to seek targets
		float maxDistFromAttractor = source.getCollisionRadius() / 2 + hardfluxCollision;

		float minDist = Float.MAX_VALUE;
		CombatEntityAPI closest = null;
		for (MissileAPI other : engine.getMissiles()) {
			if (other.getOwner() == owner || other.getOwner() == 100)
				continue;

			float distToTarget = Misc.getDistance(missile.getLocation(), other.getLocation());
			if (distToTarget > minDist || distToTarget > 3000 && !engine.isAwareOf(owner, other))
				continue;

			float distFromAttractor = Float.MAX_VALUE;
			if (data.attractorTarget != null)
				distFromAttractor = Misc.getDistance(other.getLocation(), data.attractorTarget);

			float distFromSource = Misc.getDistance(other.getLocation(), missile.getSource().getLocation());
			if (distFromSource > maxDistFromSourceShip && distFromAttractor > maxDistFromAttractor)
				continue;

			if (getNumMotesTargeting(other) >= maxMotesPerTarget)
				continue;
			if (distToTarget < minDist) {
				closest = other;
				minDist = distToTarget;
			}
		}

		for (ShipAPI other : engine.getShips()) {
			float distToTarget = Misc.getDistance(missile.getLocation(), other.getLocation());
			if (other.getOwner() == owner || other.getOwner() == 100 || distToTarget > minDist || !engine.isAwareOf(owner, other))
				continue;

			float distFromAttractor = Float.MAX_VALUE;
			if (data.attractorTarget != null)
				distFromAttractor = Misc.getDistance(other.getLocation(), data.attractorTarget);

			float distFromSource = Misc.getDistance(other.getLocation(), missile.getSource().getLocation());
			if (distFromSource > maxDistFromSourceShip && distFromAttractor > maxDistFromAttractor)
				continue;

			if (getNumMotesTargeting(other) >= maxMotesPerTarget)
				continue;
			if (distToTarget < minDist) {
				closest = other;
				minDist = distToTarget;
			}
		}
		target = closest;
	}
	
	public void doFlocking() {
		if (missile.getSource() == null) return;
		
		ShipAPI source = missile.getSource();
		
		float avoidRange = AVOID_RANGE;
		float cohesionRange = COHESION_RANGE;

		// todo: improve mote orbit behavior, look closer at code down below since alex is adding flat values strangely
		float hardfluxCollision = source.getCollisionRadius() * source.getHardFluxLevel();
		// distance at which the motes try to rejoin with the source
		float sourceRejoin = source.getCollisionRadius() + hardfluxCollision;
		// distance at which the source repels the motes
		float sourceRepel = (source.getCollisionRadius() / 2)  + hardfluxCollision;
		// distance at which the motes try to align themselves with the source's speed
		float sourceCohesion = source.getCollisionRadius() + (hardfluxCollision * 2f);
		
		float sin = (float) Math.sin(data.elapsed);
		float mult = 1f + sin * 0.25f;
		avoidRange *= mult;
		
		Vector2f total = new Vector2f();
		Vector2f attractor = getAttractorLoc();
		
		if (attractor != null) {
			float dist = Misc.getDistance(missile.getLocation(), attractor);
			Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), attractor));
			float f = dist / 200f;
			if (f > 1f) f = 1f;
			dir.scale(f * 3f);
			Vector2f.add(total, dir, total);
			avoidRange *= 3f;
		}
		
		boolean hardAvoiding = false;
		for (CombatEntityAPI other : hardAvoidList) {
			float dist = Misc.getDistance(missile.getLocation(), other.getLocation());
			float hardAvoidRange = other.getCollisionRadius() + avoidRange + 50f;
			if (dist < hardAvoidRange) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(other.getLocation(), missile.getLocation()));
				float f = 1f - dist / (hardAvoidRange);
				dir.scale(f * 5f);
				Vector2f.add(total, dir, total);
				hardAvoiding = f > 0.5f;
			}
		}

		for (MissileAPI otherMissile : data.motes) {
			if (otherMissile == missile) continue;

			float dist = Misc.getDistance(missile.getLocation(), otherMissile.getLocation());
            if (dist < avoidRange && !hardAvoiding) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(otherMissile.getLocation(), missile.getLocation()));
				Vector2f.add(total, dir, total);
			}
			if (dist < cohesionRange) {
				Vector2f dir = new Vector2f(otherMissile.getVelocity());
				Misc.normalise(dir);
				Vector2f.add(total, dir, total);
			}
		}
		
		if (missile.getSource() != null) {
			float dist = Misc.getDistance(missile.getLocation(), source.getLocation());
			if (dist > sourceRejoin) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), source.getLocation()));
				float f = dist / (sourceRejoin  + 400f) - 1f;
				dir.scale(f * 0.5f);
				Vector2f.add(total, dir, total);
			}
			if (dist < sourceRepel) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.getLocation(), missile.getLocation()));
				float f = 1f - dist / sourceRepel;
				dir.scale(f * 5f);
				Vector2f.add(total, dir, total);
			}
			if (dist < sourceCohesion && source.getVelocity().length() > 20f) {
				Vector2f dir = new Vector2f(source.getVelocity());
				Misc.normalise(dir);
				float f = 1f - dist / sourceCohesion;
				dir.scale(f);
				Vector2f.add(total, dir, total);
			}
			// if not strongly going anywhere, circle the source ship; only kicks in for lone motes
			if (total.length() <= 0.05f) {
				float offset = random > 0.5f ? 90f : -90f;
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), source.getLocation()) + offset);
				float f = 1f;
				dir.scale(f);
				Vector2f.add(total, dir, total);
			}
		}
		
		if (total.length() > 0) {
			float dir = Misc.getAngleInDegrees(total);
			Global.getCombatEngine().headInDirectionWithoutTurning(missile, dir, 10000);
			
			if (random > 0.5f) {
				missile.giveCommand(ShipCommand.TURN_LEFT);
			} else {
				missile.giveCommand(ShipCommand.TURN_RIGHT);
			}
			missile.getEngineController().forceShowAccelerating();
		}
	}
	protected int getNumMotesTargeting(CombatEntityAPI other) {
		int count = 0;
		for (MissileAPI mote : data.motes) {
			if (mote == missile) continue;
			if (mote.getUnwrappedMissileAI() instanceof sd_moteAIScript) {
				sd_moteAIScript ai = (sd_moteAIScript) mote.getUnwrappedMissileAI();
				if (ai.getTarget() == other) {
					count++;
				}
			}
		}
		return count;
	}
	public Vector2f getAttractorLoc() {
		Vector2f attractor = null;
		if (data.attractorTarget != null) {
			attractor = data.attractorTarget;
			if (data.attractorLock != null) {
				attractor = data.attractorLock.getLocation();
			}
		}
		return attractor;
	}
	public CombatEntityAPI getTarget() {
		return target;
	}
	public void setTarget(CombatEntityAPI target) {
		this.target = target;
	}
}
