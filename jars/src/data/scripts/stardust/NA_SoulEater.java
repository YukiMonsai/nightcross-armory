package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Ship;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;

public class NA_SoulEater implements BeamEffectPlugin {

	public static final int COLOR_R = 225;
	public static final int COLOR_G = 75;
	public static final int COLOR_B = 15;
	private boolean wasZero = true;

	public static final float DRILLTIME = 0.2f;
	public static final float MIN_DMG_PER_CREW = 15f;

	private static class BeamEffect {
		private IntervalUtil shockInterval = new IntervalUtil(0.1f, 0.2f);
		private IntervalUtil drillInterval = new IntervalUtil(DRILLTIME, DRILLTIME);
		public float dmgStored = 0;
		public float hash = (float) (Math.random());
	}

	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();

		WeaponAPI weapon = beam.getWeapon();
		if (weapon == null) return;
		ShipAPI ship = weapon.getShip();

		float sizemult = 0.6f;
		if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) sizemult = 2.0f;
		else
		if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) sizemult = 1.5f;

		String key = "naai_hil_" + (weapon.getSlot() != null ?
				((int) weapon.getSlot().getLocation().x) + "," + ((int) weapon.getSlot().getLocation().y)
				: weapon.getId());

		BeamEffect data = (BeamEffect) ship.getCustomData().get(key);
		if (data == null) {
			data = new BeamEffect();
			ship.setCustomData(key, data);
		}

		if (beam.getBrightness() >= 1f) {
			//beam.setCoreColor(new Color(COLOR_R, COLOR_G, COLOR_B));


			if (amount > 0 && target != null) {
				if (target instanceof ShipAPI && ((ShipAPI) target).isAlive() && (((ShipAPI) target).getShield() == null || ((ShipAPI) target).getShield().isOff() || !((ShipAPI) target).getShield().isWithinArc(beam.getTo()))) {
					NA_StargazerStardust sourceSwarm = NA_StargazerStardust.getSwarmFor(beam.getSource());
					if (sourceSwarm != null) {
						// check if armor is pierced

						ArmorGridAPI grid = ((ShipAPI) target).getArmorGrid();
						int[] cell = grid.getCellAtLocation(beam.getTo());
						if (cell == null) return;

						int gridWidth = grid.getGrid().length;
						int gridHeight = grid.getGrid()[0].length;

						float armorTotal = 0f;
						for (int i = -2; i <= 2; i++) {
							for (int j = -2; j <= 2; j++) {
								if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

								int cx = cell[0] + i;
								int cy = cell[1] + j;

								if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

								float damMult = 1/30f;
								if (i == 0 && j == 0) {
									damMult = 1/15f;
								} else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
									damMult = 1/15f;
								} else { // T hits
									damMult = 1/30f;
								}

								float armorInCell = grid.getArmorValue(cx, cy);
								armorTotal += armorInCell;
							}
						}

						if (armorTotal < ((ShipAPI) target).getArmorGrid().getArmorRating() * 0.33) {
							if (target instanceof ShipAPI && ((ShipAPI) target).getFleetMember().getMaxCrew() > 0) {
								float dur = beam.getDamage().getDpsDuration();
								// needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
								if (!wasZero) dur = 0;
								wasZero = beam.getDamage().getDpsDuration() <= 0;

								data.dmgStored += beam.getDamage().computeDamageDealt(dur);
								float crew = 0.5f * (((ShipAPI) target).getFleetMember().getMinCrew() + ((ShipAPI) target).getFleetMember().getMaxCrew());
								if (((ShipAPI) target).getFleetMember().getCrewFraction() < 0.99f) {
									crew = ((ShipAPI) target).getFleetMember().getMinCrew() * ((ShipAPI) target).getFleetMember().getCrewFraction();
								}
								float dmgNeededForSoul = target.getMaxHitpoints() / (1 + crew);

								if (data.dmgStored > Math.max(MIN_DMG_PER_CREW, dmgNeededForSoul)) {
									data.dmgStored = Math.max(0, data.dmgStored * 0.7f - Math.max(MIN_DMG_PER_CREW, dmgNeededForSoul));
									NA_StargazerStardust.SwarmMember p = sourceSwarm.addMember();
									p.loc.set(beam.getTo());
									p.fader.setDurationIn(0.3f);
								}
							}
						}



					}
				}


				Global.getCombatEngine().addNegativeSwirlyNebulaParticle(
						beam.getTo(), Misc.ZERO, 55f, 1.5f, amount, amount*2f, amount*4f,
						new Color(85, 71, 1)
				);
			}


		}

		if (beam.getBrightness() > 0) {
			//beam.setCoreColor(new Color(COLOR_R, COLOR_G, COLOR_B, (int) (100 * beam.getBrightness())));

			data.drillInterval.advance(amount);
			if (data.drillInterval.intervalElapsed()) {
				data.drillInterval = new IntervalUtil(DRILLTIME, DRILLTIME);
				//data.hash = (float) Math.random();
			}
			float prog = data.drillInterval.getElapsed()/DRILLTIME;

			// draw the drill
			MagicTrailPlugin.addTrailMemberAdvanced(
					ship, /* linkedEntity */
					data.hash , /* ID */
					Global.getSettings().getSprite("na_trails", "na_particletrailcore"), /* sprite */
					MathUtils.getPointOnCircumference(weapon.getLocation(), 20f + 5f * ((float)Math.sin(prog*Math.PI)), weapon.getCurrAngle() + 360f * prog), /* position */
					2f, /* startSpeed */
					0f, /* endSpeed */
					weapon.getCurrAngle(), /* angle */
					0f, /* startAngularVelocity */
					0f, /* endAngularVelocity */
					65f, /* startSize */
					20f, /* endSize */
					new Color(255, 111, 209, 255),//new Color((200), (int) (50 + 50f * (0.5f-0.5f*(float)Math.cos(-.3+prog*6.1f))), (int) (5 + 125 * (0.5f-0.5f*(float)Math.cos(-.3+prog*6.1f)))), /* startColor */
					new Color(103, 34, 241, 255),
					//new Color((int) (103 + 90f * (0.5f-0.5f*(float)Math.sin(prog*2f))), (int) (103 + 90f * (0.5f-0.5f*(float)Math.sin(prog*2f))), (int) (103f + 90f * (0.5f-0.5f*(float)Math.sin(prog*2f)))), /* endColor */
					1f, /* opacity */
					0.1f, /* inDuration */
					0.4f, /* mainDuration */
					0.5f, /* outDuration */
					GL11.GL_SRC_ALPHA, /* blendModeSRC */
					GL11.GL_ONE, /* blendModeDEST */
					256f, /* textureLoopLength */
					56f, /* textureScrollSpeed */
					-1, /* textureOffset */
					MathUtils.getPointOnCircumference(ship.getVelocity(), 140f, weapon.getCurrAngle() + (float) (Math.random() * 2f) + 1f - 4f * ((float) Math.sin(prog*2f*Math.PI))), /* offsetVelocity */
					null, /* advancedOptions */
					CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
					1f /* frameOffsetMult */
			);
			// draw the drill
			MagicTrailPlugin.addTrailMemberAdvanced(
					ship, /* linkedEntity */
					data.hash + 1f , /* ID */
					Global.getSettings().getSprite("na_trails", "na_particletrail"), /* sprite */
					MathUtils.getPointOnCircumference(weapon.getLocation(), 20f + 5f * ((float)Math.sin(prog*Math.PI)), weapon.getCurrAngle() + 360f * prog), /* position */
					2f, /* startSpeed */
					0f, /* endSpeed */
					weapon.getCurrAngle(), /* angle */
					0f, /* startAngularVelocity */
					0f, /* endAngularVelocity */
					65f, /* startSize */
					20f, /* endSize */
					new Color(244, 29, 68, 255),
					new Color(232, 137, 60, 40),
					//new Color((int) (103 + 90f * (0.5f-0.5f*(float)Math.sin(prog*2f))), (int) (103 + 90f * (0.5f-0.5f*(float)Math.sin(prog*2f))), (int) (103f + 90f * (0.5f-0.5f*(float)Math.sin(prog*2f)))), /* endColor */
					1f, /* opacity */
					0.1f, /* inDuration */
					0.4f, /* mainDuration */
					0.5f, /* outDuration */
					GL11.GL_BLEND_DST, /* blendModeSRC */
					GL11.GL_ONE, /* blendModeDEST */
					256f, /* textureLoopLength */
					-256f, /* textureScrollSpeed */
					0f, /* textureOffset */
					MathUtils.getPointOnCircumference(ship.getVelocity(), 100f, weapon.getCurrAngle() + (float) (Math.random() * 2f) + 1f - 4f * ((float) Math.sin(prog*2f*Math.PI))), /* offsetVelocity */
					null, /* advancedOptions */
					CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
					1f /* frameOffsetMult */
			);

			// vfx
			if (data.shockInterval.intervalElapsed()) {
				data.shockInterval.randomize();

				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
				params.maxZigZagMult = 0.25f;
				params.flickerRateMult = 0.25f;
				params.glowSizeMult *= 0.5f*sizemult;

				if (weapon.getCooldownRemaining() == 0 && beam.getBrightness() < 1f) {
					EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
							MathUtils.getRandomPointInCone(beam.getFrom(),
									250f, weapon.getCurrAngle()-45f,
									weapon.getCurrAngle()+45f),
							null,
							beam.getFrom(), beam.getSource(),
							beam.getWidth()*0.5f * sizemult,
							new Color(251, 38, 121),
							beam.getCoreColor(), params
					);
					arc.setSingleFlickerMode(true);
					arc.setRenderGlowAtStart(false);
				} else {
					/*EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
							beam.getFrom(), null,
							MathUtils.getRandomPointInCone(beam.getFrom(),
									80f,
									weapon.getCurrAngle()-150f,
									weapon.getCurrAngle()+150f),
							null,
							beam.getWidth()*0.5f * sizemult,
							new Color(159, 5, 33),
							beam.getCoreColor(), params
					);
					arc.setSingleFlickerMode(true);
					arc.setRenderGlowAtEnd(false);*/
				}
			} else {
				data.shockInterval.advance(amount);
			}
		}
	}



}
