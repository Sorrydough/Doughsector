package data.shipsystems.mote;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class sd_moteControlScript extends BaseShipSystemScript {
	protected static float MAX_ATTRACTOR_RANGE = 3000f;
	public static float MAX_DIST_FROM_SOURCE_TO_ENGAGE_AS_PD = 2000f;
	public static float MAX_DIST_FROM_ATTRACTOR_TO_ENGAGE_AS_PD = 1000f;
	public static int MAX_MOTES = 30;
	public static float ANTI_FIGHTER_DAMAGE = 200;
	public static float ANTI_FIGHTER_DAMAGE_HF = 1000;
	public static float ATTRACTOR_DURATION_LOCK = 20f;
	public static float ATTRACTOR_DURATION = 10f;
	public static class MoteData {
		public Color jitterColor;
		public Color empColor;
		public int maxMotes;
		public float antiFighterDamage;
		public String impactSound;
		public String loopSound;
	}
	
	public static Map<String, MoteData> MOTE_DATA = new HashMap<>();
	public static String MOTELAUNCHER = "motelauncher";
	static {
		MoteData normal = new MoteData();
		normal.jitterColor = new Color(100,165,255,175);
		normal.empColor = new Color(100,165,255,255);
		normal.maxMotes = MAX_MOTES;
		normal.antiFighterDamage = ANTI_FIGHTER_DAMAGE;
		normal.impactSound = "mote_attractor_impact_normal";
		normal.loopSound = "mote_attractor_loop";
		
		MOTE_DATA.put(MOTELAUNCHER, normal);
	}

	public static String getWeaponId(ShipAPI ship) {
		return MOTELAUNCHER;
	}
	public static float getAntiFighterDamage(ShipAPI ship) {
		return MOTE_DATA.get(getWeaponId(ship)).antiFighterDamage;
	}
	public static String getImpactSoundId(ShipAPI ship) {
		return MOTE_DATA.get(getWeaponId(ship)).impactSound;
	}
	public static Color getJitterColor(ShipAPI ship) {
		return MOTE_DATA.get(getWeaponId(ship)).jitterColor;
	}
	public static Color getEMPColor(ShipAPI ship) {
		return MOTE_DATA.get(getWeaponId(ship)).empColor;
	}
	public static int getMaxMotes(ShipAPI ship) {
		return MOTE_DATA.get(getWeaponId(ship)).maxMotes;
	}
	public static String getLoopSound(ShipAPI ship) {
		return MOTE_DATA.get(getWeaponId(ship)).loopSound;
	}
	
	public static class SharedMoteAIData {
		public float elapsed = 0f;
		public List<MissileAPI> motes = new ArrayList<>();
		public float attractorRemaining = 0f;
		public Vector2f attractorTarget = null;
		public ShipAPI attractorLock = null;
	}
	
	public static SharedMoteAIData getSharedData(ShipAPI source) {
		String key = source + "_mote_AI_shared";
		SharedMoteAIData data = (SharedMoteAIData) Global.getCombatEngine().getCustomData().get(key);
		if (data == null) {
			data = new SharedMoteAIData();
			Global.getCombatEngine().getCustomData().put(key, data);
		}
		return data;
	}

	protected IntervalUtil launchInterval = new IntervalUtil(0.75f, 1.25f);
	protected WeightedRandomPicker<WeaponSlotAPI> launchSlots = new WeightedRandomPicker<>();
	protected WeaponSlotAPI attractor = null;
	protected boolean findNewTargetOnUse = true;
	
	protected void findSlots(ShipAPI ship) {
		if (attractor != null) return;
		for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
			if (slot.isSystemSlot()) {
				if (slot.getSlotSize() == WeaponSize.SMALL) {
					launchSlots.add(slot);
				}
				if (slot.getSlotSize() == WeaponSize.MEDIUM) {
					attractor = slot;
				}
			}
		}
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		
		float amount = Global.getCombatEngine().getElapsedInLastFrame();
		
		SharedMoteAIData data = getSharedData(ship);
		data.elapsed += amount;
		
		if (data.attractorRemaining > 0) {
			data.attractorRemaining -= amount;
			if (data.attractorRemaining <= 0 || 
					(data.attractorLock != null && !data.attractorLock.isAlive()) ||
					data.motes.isEmpty()) {
				data.attractorTarget = null;
				data.attractorLock = null;
				data.attractorRemaining = 0;
			}
		}
		if (effectLevel <= 0) {
			findNewTargetOnUse = true;
		}
		
		CombatEngineAPI engine = Global.getCombatEngine();
		
		launchInterval.advance(amount * 5f);
		if (launchInterval.intervalElapsed()) {
			Iterator<MissileAPI> iter = data.motes.iterator();
			while (iter.hasNext()) {
				if (!engine.isMissileAlive(iter.next())) {
					iter.remove();
				}
			}
			
			if (ship.isHulk()) {
				for (MissileAPI mote : data.motes) {
					mote.flameOut();
				}
				data.motes.clear();
				return;
			}
			
			int maxMotes = getMaxMotes(ship);
			if (data.motes.size() < maxMotes && data.attractorLock == null &&// false && 
					!ship.getFluxTracker().isOverloadedOrVenting()) {
				findSlots(ship);
				
				WeaponSlotAPI slot = launchSlots.pick();
				
				Vector2f loc = slot.computePosition(ship);
				float dir = slot.computeMidArcAngle(ship);
				float arc = slot.getArc();
				dir += arc * (float) Math.random() - arc /2f;
				
				String weaponId = getWeaponId(ship);
				MissileAPI mote = (MissileAPI) engine.spawnProjectile(ship, null, 
						  weaponId, 
						  loc, dir, null);
				mote.setWeaponSpec(weaponId);
				mote.setMissileAI(new sd_moteAIScript(mote));
				mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
				mote.setEmpResistance(10000);
				data.motes.add(mote);
				
				engine.spawnMuzzleFlashOrSmoke(ship, slot, mote.getWeaponSpec(), 0, dir);
				
				Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 0.25f, loc, new Vector2f());
			}
		}
		
		float maxMotes = getMaxMotes(ship);
		float fraction = data.motes.size() / (Math.max(1f, maxMotes));
		float volume = fraction * 3f;
		if (volume > 1f) volume = 1f;
		if (data.motes.size() > 3) {
			Vector2f com = new Vector2f();
			for (MissileAPI mote : data.motes) {
				Vector2f.add(com, mote.getLocation(), com);
			}
			com.scale(1f / data.motes.size());
			//Global.getSoundPlayer().playLoop("mote_attractor_loop", ship, 1f, volume, com, new Vector2f());
			Global.getSoundPlayer().playLoop(getLoopSound(ship), ship, 1f, volume, com, new Vector2f());
		}
		
		
		if (effectLevel > 0 && findNewTargetOnUse) {
			calculateTargetData(ship);
			findNewTargetOnUse = false;
		}
		
		if (effectLevel == 1) {
			// possible if system is reused immediately w/ no time to cool down, I think
			if (data.attractorTarget == null) {
				calculateTargetData(ship);
			}
			findSlots(ship);
			
			Vector2f slotLoc = attractor.computePosition(ship);
			
			CombatEntityAPI asteroid = engine.spawnAsteroid(0, data.attractorTarget.x, data.attractorTarget.y, 0, 0);
			asteroid.setCollisionClass(CollisionClass.NONE);
			CombatEntityAPI target = asteroid;
			if (data.attractorLock != null) {
				target = data.attractorLock;
			}
			
			float emp = 0;
			float dam = 0;
			EmpArcEntityAPI arc = engine.spawnEmpArc(ship, slotLoc, ship, target, DamageType.ENERGY, dam, emp,100000f,"mote_attractor_targeted_ship",40f,
							   							sd_moteControlScript.getEMPColor(ship),
														new Color(255,255,255,255));
			if (data.attractorLock != null) {
				arc.setTargetToShipCenter(slotLoc, data.attractorLock);
			}
			arc.setCoreWidthOverride(30f);
			
			if (data.attractorLock == null) {
				Global.getSoundPlayer().playSound("mote_attractor_targeted_empty_space", 1f, 1f, data.attractorTarget, new Vector2f());
			}
			engine.removeEntity(asteroid);
		}
	}
	
	public void calculateTargetData(ShipAPI ship) {
		SharedMoteAIData data = getSharedData(ship);
		Vector2f targetLoc = getTargetLoc(ship);
		//System.out.println(getTargetedLocation(ship));
		data.attractorLock = getLockTarget(ship, targetLoc);
		
		data.attractorRemaining = ATTRACTOR_DURATION;
		if (data.attractorLock != null) {
			targetLoc = new Vector2f(data.attractorLock.getLocation());
			data.attractorRemaining = ATTRACTOR_DURATION_LOCK;
		}
		data.attractorTarget = targetLoc;
		
		if (data.attractorLock != null) {
			// need to do this in a script because when the ship is phased, the charge-in time of the system (0.1s)
			// is not enough for the jitter to come to full effect (which requires 0.1s "normal" time)
			Global.getCombatEngine().addPlugin(createTargetJitterPlugin(data.attractorLock,
					ship.getSystem().getChargeUpDur(), ship.getSystem().getChargeDownDur(),
					sd_moteControlScript.getJitterColor(ship)));
		}
	}

	public Vector2f getTargetedLocation(ShipAPI from) {
		Vector2f loc = from.getSystem().getTargetLoc();
		if (loc == null) {
			loc = new Vector2f(from.getMouseTarget());
		}
		return loc;
	}

	public Vector2f getTargetLoc(ShipAPI from) {
		findSlots(from);
		
		Vector2f slotLoc = attractor.computePosition(from);
		Vector2f targetLoc = new Vector2f(getTargetedLocation(from));
		float dist = Misc.getDistance(slotLoc, targetLoc);
		if (dist > getRange(from)) {
			targetLoc = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(slotLoc, targetLoc));
			targetLoc.scale(getRange(from));
			Vector2f.add(targetLoc, slotLoc, targetLoc);
		}
		return targetLoc;
	}

	public ShipAPI getLockTarget(ShipAPI from, Vector2f loc) {
		Vector2f slotLoc = attractor.computePosition(from);
		for (ShipAPI other : Global.getCombatEngine().getShips()) {
			if (other.isFighter()) continue;
			if (other.getOwner() == from.getOwner()) continue;
			if (other.isHulk()) continue;
			if (!other.isTargetable()) continue;
			
			float dist = Misc.getDistance(slotLoc, other.getLocation());
			if (dist > getRange(from)) continue;
			
			dist = Misc.getDistance(loc, other.getLocation());
			if (dist < other.getCollisionRadius() + 50f) {
				return other;
			}
		}
		return null;
	}
	
	public static float getRange(ShipAPI ship) {
		if (ship == null) return MAX_ATTRACTOR_RANGE;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_ATTRACTOR_RANGE);
	}
	
	protected EveryFrameCombatPlugin createTargetJitterPlugin(final ShipAPI target, final float in, final float out, final Color jitterColor) {
		return new BaseEveryFrameCombatPlugin() {
			float elapsed = 0f;
			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (Global.getCombatEngine().isPaused()) return;
				
				elapsed += amount;

				float level = 0f;
				if (elapsed < in) {
					level = elapsed / in;
				} else if (elapsed < in + out) {
					level = 1f - (elapsed - in) / out;
					level *= level;
				} else {
					Global.getCombatEngine().removePlugin(this);
					return;
				}

				if (level > 0) {
                    float maxRangeBonus = 50f;
					float jitterRangeBonus = level * maxRangeBonus;
					target.setJitterUnder(this, jitterColor, level, 10, 0f, jitterRangeBonus);
					target.setJitter(this, jitterColor, level, 4, 0f, 0 + jitterRangeBonus);
				}
			}
		};
	}
}








