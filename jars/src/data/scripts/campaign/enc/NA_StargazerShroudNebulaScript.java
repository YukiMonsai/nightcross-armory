package data.scripts.campaign.enc;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.AbyssalLightEntityPlugin;
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility;
import com.fs.starfarer.api.impl.campaign.ghosts.*;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class NA_StargazerShroudNebulaScript implements EveryFrameScript {

	protected float angryTime = 0f;
	protected float hostileTime = 0f;
	protected float moteSpawnRate = 1f;
	protected SectorEntityToken entity;
	protected IntervalUtil moteSpawn = new IntervalUtil(0.01f, 0.1f);
	protected IntervalUtil lightSpawn = new IntervalUtil(1f, 6f);
	protected Random random = null;
	protected NA_StargazerShroudGhost ghost = null;
	protected SectorEntityToken angryAt = null;


	public NA_StargazerShroudNebulaScript(SectorEntityToken entity, NA_StargazerShroudGhost ghost, float moteSpawnRate) {
		super();
		this.entity = entity;
		this.moteSpawnRate = moteSpawnRate;
		this.random = new Random();
		this.ghost = ghost;
	}


	public static float INTERDICT_TRIGGER_RANGE = 650f;
	public static float SENSOR_TRIGGER_RANGE = 2000f;
	public static float AGGRO_RANGE = 240f;


	public void advance(float amount) {
		float days = Misc.getDays(amount);
		float mult = moteSpawnRate;
		moteSpawn.advance(days * mult);
		if (moteSpawn.intervalElapsed()) {
			spawnMote(entity);
		}
		lightSpawn.advance(days * mult);
		if (lightSpawn.intervalElapsed()) {
			spawnLights(entity);
		}


		if (Global.getSector().getMemoryWithoutUpdate().getBoolean(MemFlags.GLOBAL_INTERDICTION_PULSE_JUST_USED_IN_CURRENT_LOCATION)) {
			if (entity.getContainingLocation() != null) {
				for (CampaignFleetAPI fleet : entity.getContainingLocation().getFleets()) {
					float range = Math.max(INTERDICT_TRIGGER_RANGE * 2.5f, 3f * InterdictionPulseAbility.getRange(fleet)) + fleet.getRadius() + entity.getRadius();
					float dist = Misc.getDistance(fleet.getLocation(), entity.getLocation());
					if (dist > range) continue;


					if (fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.JUST_DID_INTERDICTION_PULSE)) {
						angryTime = 15f;
						angryAt = fleet;
						if (dist < INTERDICT_TRIGGER_RANGE + fleet.getRadius() + entity.getRadius()) {
							ghost.followBehavior = null;
							ghost.clearScript();
							hostileTime = 6f;
						}
					}
				}
			}
			if (isDespawning()) return;
		}

		if (Global.getSector().getMemoryWithoutUpdate().getBoolean(MemFlags.GLOBAL_SENSOR_BURST_JUST_USED_IN_CURRENT_LOCATION)) {
			if (entity.getContainingLocation() != null) {
				for (CampaignFleetAPI fleet : entity.getContainingLocation().getFleets()) {
					float range = SENSOR_TRIGGER_RANGE + fleet.getRadius() + entity.getRadius();
					float dist = Misc.getDistance(fleet.getLocation(), entity.getLocation());
					if (dist > range) continue;


					if (fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.JUST_DID_SENSOR_BURST)) {
						if (angryTime <= 2f) {
							angryTime = 5f;
							angryAt = fleet;

							if (dist < INTERDICT_TRIGGER_RANGE + fleet.getRadius() + entity.getRadius()) {
								ghost.followBehavior = null;
								ghost.clearScript();
								hostileTime = 4f;
							} else
							if (dist < SENSOR_TRIGGER_RANGE + fleet.getRadius() + entity.getRadius()) {
								ghost.followBehavior = null;
								ghost.clearScript();
								angryTime = 15f;
							}
						}
					}
				}
			}
			if (isDespawning()) return;
		}

		if (hostileTime > 0) {
			hostileTime -= days;
			if (hostileTime < 0) hostileTime = 0;
			//System.out.println("Timeout: " + abilityResponseFlickerTimeout);
			if (angryAt != null) {
				var fleet = Global.getSector().getPlayerFleet();
				float dist = Misc.getDistance(fleet.getLocation(), entity.getLocation());

				if (angryAt == Global.getSector().getPlayerFleet() && dist < AGGRO_RANGE + fleet.getRadius() && ghost.getEntity() != null) {
 					Global.getSector().addScript(new NA_StargazerDwellerEncounterScript("NA_StargazerShroudAttackAfterAbilityUse", fleet, entity));
					ghost.clearScript();

					ghost.kill();
				} else if (ghost.followBehavior == null) {
					ghost.clearScript();
					var pf = angryAt;

					ghost.followBehavior = new GBIntercept(pf, angryTime + 3f, 4, 100f, false);

					ghost.addBehavior(ghost.followBehavior);
					ghost.addBehavior(new GBFollow(pf, 100f, 5, 30f, 200f));
				}



			}
		} else if (angryTime > 0) {
			angryTime -= days;
			if (angryTime < 0) angryTime = 0;
			//System.out.println("Timeout: " + abilityResponseFlickerTimeout);

			if (angryAt != null) {
				if (ghost.followBehavior == null) {
					ghost.clearScript();
					var pf = angryAt;


					ghost.followBehavior = new GBFollow(pf, 1000f, 12, 350f, 1000f);
					ghost.addBehavior(ghost.followBehavior);
					//addBehavior(new GBIRunEveryFrame(0f, this));
					ghost.addInterrupt(new GBITooClose(0f, pf, 100f));
					ghost.addBehavior(new GBGoAwayFrom(10f, pf, 25));
				}
			}
		} else {
			if (ghost.followBehavior != null) {


				if (Global.getSector().getPlayerFleet() != null) {
					var pf = Global.getSector().getPlayerFleet();
					ghost.clearScript();

					ghost.addBehavior(new GBFollow(pf, 1000f, 6, 1000f, 1600f));
					//addBehavior(new GBIRunEveryFrame(0f, this));
					ghost.addInterrupt(new GBITooClose(0f, pf, 100f));
					ghost.addBehavior(new GBGoAwayFrom(10f, pf, 10));
				}
			}
		}











	}



	public boolean isDespawning() {
		return entity.hasTag(Tags.FADING_OUT_AND_EXPIRING);
	}

	public static Color MOTE_COLOR = new Color(131, 3, 33,175);
	
	public static void spawnMote(SectorEntityToken from) {
		if (!from.isInCurrentLocation()) return;
		float dur = 1f + 2f * (float) Math.random();
		dur *= 2f;
		float size = 3f + (float) Math.random() * 5f;
		size *= 3f;
		Color color = MOTE_COLOR;
		
		Vector2f loc = Misc.getPointWithinRadius(from.getLocation(), from.getRadius());
		Vector2f vel = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
		vel.scale(2f + (float) Math.random() * 4f);
		vel.scale(0.25f);
		Vector2f.add(vel, from.getVelocity(), vel);
		Misc.addGlowyParticle(from.getContainingLocation(), loc, vel, size, 0.5f, dur, color);
	}

	public void spawnLights(SectorEntityToken from) {
		int num = 1 + random.nextInt(2);
		float spread = 150f + num * 10f;
		spread *= 0.5f;
		float minSize = 950f;
		for (int i = 0; i < num; i++) {
			AbyssalLightEntityPlugin.AbyssalLightParams params = new AbyssalLightEntityPlugin.AbyssalLightParams(minSize * 0.1f, minSize * 0.2f);
			params.durationDays += random.nextFloat() * 20f;
			params.frequencyChangeMult = 2f + random.nextFloat() * 2f;;
			params.frequencyMultMin *= params.frequencyChangeMult;
			params.frequencyMultMax *= params.frequencyChangeMult;
			params.color = MOTE_COLOR;
			params.detectedRange = 2000f;

			//Vector2f loc = Misc.getPointWithinRadiusUniform(point.loc, spread, data.random);
			Vector2f loc = Misc.getPointWithinRadius(from.getLocation(), spread);
			SectorEntityToken e = Global.getSector().getHyperspace().addCustomEntity(
					null, null, Entities.ABYSSAL_LIGHT, Factions.NEUTRAL, params);
			e.setLocation(loc.x, loc.y);
		}
	}

	public boolean isDone() {
		return entity.isExpired();
	}

	public boolean runWhilePaused() {
		return false;
	}
}




class NA_StargazerDwellerEncounterScript implements EveryFrameScript {
	protected float elapsed = 0f;
	protected String trigger;
	protected boolean done = false;
	protected float triggerDelay;
	protected SectorEntityToken light;
	protected CampaignFleetAPI fleet;
	public NA_StargazerDwellerEncounterScript(String trigger, CampaignFleetAPI fleet, SectorEntityToken light) {
		this.trigger = trigger;
		this.fleet = fleet;
		this.light = light;
		triggerDelay = 1f + (float) Math.random() * 1f;

	}
	public boolean isDone() {
		return done;
	}
	public boolean runWhilePaused() {
		return false;
	}
	public void advance(float amount) {
		if (done) return;

		if (Global.getSector().isPaused() || Global.getSector().getCampaignUI().isShowingDialog()) {
			return;
		}

		elapsed += amount;

		if (fleet == null || elapsed > 3f || !light.isAlive() ||
				fleet.getContainingLocation() != light.getContainingLocation()) {
			done = true;
			return;
		}

		float dist = Misc.getDistance(fleet, light) - fleet.getRadius() - light.getRadius();
		if (dist > NA_StargazerShroudNebulaScript.AGGRO_RANGE) {
			done = true;
			return;
		}


		if (elapsed > triggerDelay) {
			if (fleet.isPlayerFleet()) {
				Misc.showRuleDialog(light, trigger);

				if (light != null) {
					light.addTag(Tags.NON_CLICKABLE);
					light.addTag(Tags.FADING_OUT_AND_EXPIRING);
					light.setExpired(true);
				}
			} else {
				fleet.despawn(CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE, light);

				if (light != null) {
					light.addTag(Tags.NON_CLICKABLE);
					light.addTag(Tags.FADING_OUT_AND_EXPIRING);
					light.setExpired(true);
				}
			}
			done = true;
			return;
		}
	}
}







