package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicMisc;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NA_CorrosionBeamEffect implements BeamEffectPlugin {

	public final float BEAM_TIME = 0.25f;
	private IntervalUtil shockInterval = new IntervalUtil(BEAM_TIME, BEAM_TIME);
	public final String proj_id = "na_corrosionbeambullet_shot";
	public final String wpn_id = "na_corrosionbeambullet";



	public Map<CombatEntityAPI, Integer> finalTarget = new HashMap<>();


	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		if (beam.getBrightness() >= 1f) {

			if (shockInterval.intervalElapsed() && MathUtils.getRandomNumberInRange(0, 1f) < 0.8f) {
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

				float amt = Math.max(0, 1f - shockInterval.getElapsed()/shockInterval.getMaxInterval());
				beam.setCoreColor(new Color(Math.max(amt * 0.9f, Math.max(0f, Math.min(1f, 3f - beam.getWeapon().getBurstFireTimeRemaining()))), amt * 0.8f, amt));

				if (shockInterval.intervalElapsed()) {
					engine.spawnEmpArcVisual(
							MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo()), null,
							MathUtils.getRandomPointOnLine(beam.getFrom(), beam.getTo()), null,
							14f,
							beam.getFringeColor(),
							new Color(61, 55, 255)
					);

					shockInterval = new IntervalUtil(BEAM_TIME, BEAM_TIME);

					if (target != null) {
						if (hitShield) {
							finalTarget.put(target, finalTarget.get(target) + 1);
							//createDistortionAlongBeam(beam, target);
							beam.setWidth(beam.getWidth() + 3f);
						} else
							releaseMissile(beam, target);
					}



				} else {
					shockInterval.advance(amount);
				}
			} else {
				float amt = 0f;
				beam.setCoreColor(new Color(Math.max(amt * 0.9f, Math.max(0f, Math.min(1f, 1f - beam.getWeapon().getBurstFireTimeRemaining()/2f))), amt * 0.8f, amt));
				if (shockInterval.intervalElapsed()) {

					shockInterval = new IntervalUtil(BEAM_TIME, BEAM_TIME);
				} else
					shockInterval.advance(amount);
			}

		} else if (beam.getBrightness() < 1) {

			beam.setCoreColor(new Color(5, 5, 5));
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
			if (shockInterval.getElapsed() > 0)
				shockInterval = new IntervalUtil(BEAM_TIME, BEAM_TIME);
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
				Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(beam.getSource(),
						WeaponAPI.WeaponType.MISSILE, false, ((DamagingProjectileAPI) proj).getDamage());

				makeDistortion(pp);
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
}
