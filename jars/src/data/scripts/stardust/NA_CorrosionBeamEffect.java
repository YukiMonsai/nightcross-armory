package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NA_CorrosionBeamEffect implements BeamEffectPlugin {

	public final String proj_id = "na_corrosionbeambullet_shot";
	public final String wpn_id = "na_corrosionbeambullet";

	public static final String mote_sfx = "naai_corrosion_mote";



	public Map<CombatEntityAPI, Integer> finalTarget = new HashMap<>();
	public static final float BEAM_TIME = 0.25f;

	protected float getBeamTime() {
		return  BEAM_TIME;
	}


	private static class BeamEffect {
		private IntervalUtil shockInterval = new IntervalUtil(0.1f, 0.1f);
	}

	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();


		WeaponAPI weapon = beam.getWeapon();
		if (weapon == null) return;
		ShipAPI ship = weapon.getShip();
		String key = "na_corrosionbeam" + (weapon.getSlot() != null ?
				((int) weapon.getSlot().getLocation().x) + "," + ((int) weapon.getSlot().getLocation().y)
				: weapon.getId());

		BeamEffect data = (BeamEffect) ship.getCustomData().get(key);
		if (data == null) {
			data = new BeamEffect();
			ship.setCustomData(key, data);
		}

		if (beam.getBrightness() >= 1f) {

			if (data.shockInterval.intervalElapsed() && MathUtils.getRandomNumberInRange(0, 1f) < 0.8f) {
				for (Map.Entry<CombatEntityAPI, Integer> tt : finalTarget.entrySet()) {
					int amtMissiles = tt.getValue();
					if (amtMissiles > 0) {
						releaseMissile(beam, tt.getKey());
						finalTarget.put(tt.getKey(), tt.getValue() - 1);
						break;
					}

				}
			}

			if (target != null) {
				if (target != null) {
					if (!finalTarget.containsKey(target))
						finalTarget.put(target, 0);
				}
				boolean hitShield = MathUtils.getRandomNumberInRange(0, 100) < 50 || target.getShield() != null && target.getShield().isWithinArc(beam.getTo());

				float amt = Math.max(0, 1f - data.shockInterval.getElapsed()/data.shockInterval.getMaxInterval());
				beam.setCoreColor(new Color(Math.max(amt * 0.9f, Math.max(0f, Math.min(1f, 3f - beam.getWeapon().getBurstFireTimeRemaining()))), amt * 0.8f, amt));

				if (data.shockInterval.intervalElapsed()) {
					engine.spawnEmpArcVisual(
							MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo()), null,
							MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo()), null,
							14f,
							beam.getFringeColor(),
							new Color(61, 55, 255)
					);

					data.shockInterval = new IntervalUtil(getBeamTime(), getBeamTime());

					if (target != null) {
						if (hitShield) {
							finalTarget.put(target, finalTarget.get(target) + 1);
							//createDistortionAlongBeam(beam, target);
							beam.setWidth(beam.getWidth() + 3f);
						} else
							releaseMissile(beam, target);
					}



				} else {
					data.shockInterval.advance(amount);
				}
			} else {
				float amt = 0f;
				beam.setCoreColor(new Color(Math.max(amt * 0.9f, Math.max(0f, Math.min(1f, 1f - beam.getWeapon().getBurstFireTimeRemaining()/2f))), amt * 0.8f, amt));
				if (data.shockInterval.intervalElapsed()) {

					data.shockInterval = new IntervalUtil(getBeamTime(), getBeamTime());
				} else
					data.shockInterval.advance(amount);
			}

		} else if (beam.getBrightness() < 1) {

			beam.setCoreColor(new Color(126, 35, 81));
			beam.setFringeColor(new Color(255, 25, 52));
			beam.setFringeTexture("graphics/trails/na_particlebeamtrail.png");
			beam.setCoreTexture("graphics/trails/na_plasmabeamcore.png");


			for (Map.Entry<CombatEntityAPI, Integer> tt : finalTarget.entrySet()) {
				int amtMissiles = tt.getValue();
				for (int i = 0; i < amtMissiles; i++) {
					releaseMissile(beam, tt.getKey());
				}
			}
			finalTarget = new HashMap<>();
			if (data.shockInterval.getElapsed() > 0)
				data.shockInterval = new IntervalUtil(getBeamTime(), getBeamTime());
		}
//			Global.getSoundPlayer().playLoop("system_emp_emitter_loop",
//											 beam.getDamageTarget(), 1.5f, beam.getBrightness() * 0.5f,
//											 beam.getTo(), new Vector2f());
	}

	public void releaseMissile(BeamAPI beam, CombatEntityAPI target) {
		if (target instanceof ShipAPI && beam.getSource() != null && beam.getSource().isAlive()) {
			if (Global.getCombatEngine().isEntityInPlay(target)) {
				float dd = MathUtils.getDistance(beam.getFrom(), beam.getTo());
				float ang = VectorUtils.getAngle(beam.getFrom(), beam.getTo());
				Vector2f pp = MathUtils.getPoint(beam.getFrom(), Math.min(dd,
								Math.max(100, MathUtils.getRandomNumberInRange(dd * 0.1f, dd*0.4f))),
						ang
				);
				CombatEntityAPI proj = Global.getCombatEngine().spawnProjectile(beam.getSource(), null,
						wpn_id,
						pp,
						ang + Math.signum(MathUtils.getRandomNumberInRange(-1, 1)) * 15f,
						beam.getSource().getVelocity());
				if (proj instanceof MissileAPI) ((MissileAPI) proj).setEmpResistance(4);
				Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(beam.getSource(),
						WeaponAPI.WeaponType.MISSILE, false, ((DamagingProjectileAPI) proj).getDamage());

				makeDistortion(pp);

				Global.getSoundPlayer().playSound(
						mote_sfx, 1.0f, 1.0f, pp, Misc.ZERO);

				// stargazer hullmod
				NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(beam.getSource());
				int active = swarm == null ? 0 : swarm.getNumActiveMembers();
				int required = 1;
				if (active >= required && beam.getDamageTarget() != null) {
					CombatEngineAPI engine = Global.getCombatEngine();
					NA_StargazerStardust.SwarmMember fragment = pickPrimaryFragment(swarm, beam);
					if (fragment == null) {
						return;
					}

					proj = Global.getCombatEngine().spawnProjectile(beam.getSource(), null,
							wpn_id,
							pp,
							ang + Math.signum(MathUtils.getRandomNumberInRange(-1, 1)) * 15f,
							beam.getSource().getVelocity());
					if (proj instanceof MissileAPI) ((MissileAPI) proj).setEmpResistance(4);
					Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(beam.getSource(),
							WeaponAPI.WeaponType.MISSILE, false, ((DamagingProjectileAPI) proj).getDamage());

					makeDistortion(pp);


					if (proj instanceof MissileAPI) {
						MissileAPI missile = (MissileAPI) proj;
						if (missile.getWeapon() == null || !missile.getWeapon().hasAIHint(WeaponAPI.AIHints.RANGE_FROM_SHIP_RADIUS)) {
							missile.setStart(new Vector2f(missile.getLocation()));
						}
						missile.getLocation().set(fragment.loc);

						swarm.removeMember(fragment);



						WeaponAPI weapon = beam.getWeapon();


						Vector2f from = weapon.getFirePoint(0);

						EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
						params.segmentLengthMult = 4f;

						params.glowSizeMult = 0.5f;
						params.brightSpotFadeFraction = 0.33f;
						params.brightSpotFullFraction = 0.5f;
						params.movementDurMax = 0.2f;
						params.flickerRateMult = 0.5f;

						float dist = Misc.getDistance(from, missile.getLocation());
						float minBright = 100f;
						if (dist * params.brightSpotFullFraction < minBright) {
							params.brightSpotFullFraction = minBright / Math.max(minBright, dist);
						}

						float thickness = 20f;

						EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, weapon.getShip(),
								missile.getLocation(),
								missile,
								thickness, // thickness
								new Color(85, 0, 0),
								new Color(255, 25, 52),
								params
						);
						//arc.setCoreWidthOverride(thickness * coreWidthMult);
						arc.setSingleFlickerMode(true);
						arc.setUpdateFromOffsetEveryFrame(true);
						//arc.setRenderGlowAtStart(false);
						//arc.setFadedOutAtStart(true);


					}

				}
			}
		}
	}


	public void createDistortionAlongBeam(BeamAPI beam, CombatEntityAPI target) {
		if (target instanceof ShipAPI && beam.getSource() != null && beam.getSource().isAlive()) {
			if (Global.getCombatEngine().isEntityInPlay(target)) {
				float dd = MathUtils.getDistance(beam.getFrom(), beam.getTo());
				float ang = VectorUtils.getAngle(beam.getFrom(), beam.getTo());
				Vector2f pp = MathUtils.getPoint(beam.getFrom(), Math.min(dd,
								Math.max(100, MathUtils.getRandomNumberInRange(dd * 0.1f, dd*0.4f))),
						ang
				);

				makeDistortion(pp);
			}
		}
	}

	public void makeDistortion(Vector2f pp) {
		RippleDistortion ripple = new RippleDistortion(pp, Misc.ZERO);
		ripple.setSize(16f);
		ripple.setIntensity(10.0F +  MathUtils.getRandomNumberInRange(0, 20f));
		ripple.setFrameRate(10 + MathUtils.getRandomNumberInRange(0, 5));
		ripple.setCurrentFrame(MathUtils.getRandomNumberInRange(0, 10));
		ripple.fadeInIntensity(.15F + MathUtils.getRandomNumberInRange(0, 0.25f));
		DistortionShader.addDistortion(ripple);
	}




	protected NA_StargazerStardust.SwarmMember pickPrimaryFragment(NA_StargazerStardust sourceSwarm, BeamAPI beam) {
		return pickOuterFragmentWithinRangeClosestTo(sourceSwarm, beam.getLength()*2, beam.getDamageTarget().getLocation());
	}


	protected NA_StargazerStardust.SwarmMember pickOuterFragmentWithinRangeClosestTo(NA_StargazerStardust sourceSwarm, float range, Vector2f otherLoc) {
		NA_StargazerStardust.SwarmMember best = null;
		float minDist = Float.MAX_VALUE;
		WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker = sourceSwarm.getPicker(true, true);
		while (!picker.isEmpty()) {
			NA_StargazerStardust.SwarmMember p = picker.pickAndRemove();
			float dist = Misc.getDistance(p.loc, sourceSwarm.getAttachedTo().getLocation());
			if (sourceSwarm.params.generateOffsetAroundAttachedEntityOval) {
				dist -= Misc.getTargetingRadius(p.loc, sourceSwarm.attachedTo, false) + sourceSwarm.params.maxOffset - range * 0.5f;
			}
			if (dist > range) continue;
			dist = Misc.getDistance(p.loc, otherLoc);
			if (dist < minDist) {
				best = p;
				minDist = dist;
			}
		}
		return best;
	}
}
