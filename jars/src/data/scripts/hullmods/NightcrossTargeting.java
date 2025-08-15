package data.scripts.hullmods;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import java.awt.Color;
import data.scripts.NAUtils;

public class NightcrossTargeting extends BaseHullMod {

	private static Map mag = new HashMap();
	static {
		mag.put(HullSize.FIGHTER, 0.51f);
		mag.put(HullSize.FRIGATE, 0.75f);
		mag.put(HullSize.DESTROYER, 1.1f);
		mag.put(HullSize.CRUISER, 2f);
		mag.put(HullSize.CAPITAL_SHIP, 3f);
	}
	private static Map mag2 = new HashMap();
	static {
		mag2.put(HullSize.FIGHTER, 50f);
		mag2.put(HullSize.FRIGATE, 25f);
		mag2.put(HullSize.DESTROYER, 20f);
		mag2.put(HullSize.CRUISER, 15f);
		mag2.put(HullSize.CAPITAL_SHIP, 10f);
	}

	public static final float FLUX_RED = 33f;
	public static final float RPM_INCREASE = 33f;
	public static final float TIME_SECONDS = 1.5f;
	public static final float PARTICLE_PERIOD = 0.08f;
	public static final float ARC_PERIOD = 0.08f;
	public static final float PARTICLE_DURATION = 0.15f;
	public static final float PARTICLE_RADIUS = 30f;
	public static final float PARTICLE_VELOCITY = 5f;
	public static final float ARC_CHANCE_VISUAL = 0.12f;
	public static final float ARC_CHANCE_VISUAL_REPEAT = 0.03f;
	public static final float SHIELD_RATE = 50f;

	public static final Color PARTICLE_CHARGE_COLOR_SG = new Color(138, 0, 23, 90);
	public static final Color PARTICLE_CHARGE_COLOR = new Color(0, 75, 175, 80);
	public static final String ACTIVATE_SOUND = "system_ammo_feeder";
	public static final String CHARGE_SOUND = "na_chargeup";





	private String ID = "NightcrossTargeting";

	private static class NightcrossTargetingData {
		IntervalUtil interval = new IntervalUtil(TIME_SECONDS, TIME_SECONDS);
		public void reset(float time) {
			interval = new IntervalUtil(time, time);
		}
	}
	public static class NightcrossTargetingAIData {
		boolean holdShieldsOff = false;
	}
	private static class NightcrossTargetingEffectData {
		IntervalUtil interval = new IntervalUtil(PARTICLE_PERIOD, PARTICLE_PERIOD*2f);
		public void reset() {
			interval = new IntervalUtil(PARTICLE_PERIOD, PARTICLE_PERIOD*2f);
		}
	}
	private static class NightcrossTargetingArcData {
		IntervalUtil interval = new IntervalUtil(ARC_PERIOD, ARC_PERIOD*2f);
		int remainingCount = 0;
		public void reset() {
			interval = new IntervalUtil(ARC_PERIOD, ARC_PERIOD*2f);
		}
		public void resetCount() {
			remainingCount = 3;
		}
	}
	private static class NightcrossTargetingChargeData {
		SoundAPI sound = null;
	}
	private static class NightcrossTargetingLevelData {
		float level = 1.0f;
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) FLUX_RED + "%";
		//if (index == 1) return "" + (int) RPM_INCREASE + "%";
		if (index == 1) return "" + Math.round((Float) mag.get(HullSize.FRIGATE)) + "";
		if (index == 2) return "" + Math.round((Float) mag.get(HullSize.DESTROYER)) + "";
		if (index == 3) return "" + Math.round((Float) mag.get(HullSize.CRUISER)) + "";
		if (index == 4) return "" + Math.round((Float) mag.get(HullSize.CAPITAL_SHIP)) + " seconds";
		if (index == 5) return Math.round(SHIELD_RATE) + "%";
		if (index == 6) return "" + Math.round((Float) mag2.get(HullSize.FRIGATE)) + "";
		if (index == 7) return "" + Math.round((Float) mag2.get(HullSize.DESTROYER)) + "";
		if (index == 8) return "" + Math.round((Float) mag2.get(HullSize.CRUISER)) + "";
		if (index == 9) return "" + Math.round((Float) mag2.get(HullSize.CAPITAL_SHIP)) + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_RATE);
		stats.getVentRateMult().modifyPercent(id, (float) mag2.get(hullSize));
	}



	private StandardLight light;
	private WaveDistortion wave;
	private Vector2f pos = new Vector2f();
	private Vector2f vel = new Vector2f();
	private Vector2f zero = new Vector2f();



	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;

		CombatEngineAPI engine = Global.getCombatEngine();

		String key = ID + "_" + ship.getId();
		String key2 = ID + "2_" + ship.getId();
		String key3 = ID + "3_" + ship.getId();
		String key4  = ID + "4_" + ship.getId();
		String key5  = ID + "5_" + ship.getId();
		String key6  = ID + "6_" + ship.getId();
		NightcrossTargetingData data = (NightcrossTargetingData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NightcrossTargetingData();
			ship.setCustomData(key, data);
		}
		NightcrossTargetingLevelData effectlevel = (NightcrossTargetingLevelData) ship.getCustomData().get(key2);
		if (effectlevel == null) {
			effectlevel = new NightcrossTargetingLevelData();
			ship.setCustomData(key2, effectlevel);
		}
		NightcrossTargetingEffectData particletimer = (NightcrossTargetingEffectData) ship.getCustomData().get(key3);
		if (particletimer == null) {
			particletimer = new NightcrossTargetingEffectData();
			ship.setCustomData(key3, particletimer);
		}
		NightcrossTargetingArcData arctimer = (NightcrossTargetingArcData) ship.getCustomData().get(key4);
		if (arctimer == null) {
			arctimer = new NightcrossTargetingArcData();
			ship.setCustomData(key4, arctimer);
		}
		if (arctimer.remainingCount > 0) {
			arctimer.interval.advance(amount);
		}
		NightcrossTargetingChargeData chargesound = (NightcrossTargetingChargeData) ship.getCustomData().get(key5);
		if (chargesound == null) {
			chargesound = new NightcrossTargetingChargeData();
			ship.setCustomData(key5, chargesound);
		}
		NightcrossTargetingAIData shieldAI = (NightcrossTargetingAIData) ship.getCustomData().get(key6);
		if (shieldAI == null) {
			shieldAI = new NightcrossTargetingAIData();
			ship.setCustomData(key6, shieldAI);
		}

		if (arctimer.remainingCount > 0 && arctimer.interval.intervalElapsed()) {
			arctimer.reset();
			arctimer.remainingCount -= 1;

			if ((ship.getShield() == null || ship.getShield().isOff()) && !((ship.getFluxTracker() != null && ship.getFluxTracker().isOverloadedOrVenting()))) {
				arctimer.remainingCount = 0;
			} else {
				float chance = 1f * (ARC_CHANCE_VISUAL_REPEAT);
				for (WeaponAPI weapon : ship.getAllWeapons()) {
					if (Math.random() < chance && (weapon.getType() == WeaponAPI.WeaponType.BALLISTIC || weapon.getType() == WeaponAPI.WeaponType.ENERGY)) {
						pos = weapon.getLocation();
						vel = Vector2f.add(ship.getVelocity(),
								NAUtils.lengthdir(PARTICLE_VELOCITY, (float) (Math.random() * 2f * Math.PI)),
								null);

						engine.spawnEmpArc(ship,
								pos,
								ship,
								ship,
								DamageType.ENERGY,
								0,
								0, // emp
								100000f, // max range
								null, //"tachyon_lance_emp_impact",
								20f, // thickness
								NAUtils.isStargazerRed(ship) ? PARTICLE_CHARGE_COLOR_SG : PARTICLE_CHARGE_COLOR,
								new Color(255, 255, 255, 255)
						);

					}
				}
			}
		}

		ship.getMutableStats().getShieldUnfoldRateMult().modifyPercent(ID, SHIELD_RATE);
		ship.getMutableStats().getVentRateMult().modifyPercent(ID, (float) mag2.get(ship.getHullSize()));

		ShipwideAIFlags ai = ship.getAIFlags();
		if (effectlevel.level > 0.99) {
			if (ai.hasFlag(ShipwideAIFlags.AIFlags.SAFE_FROM_DANGER_TIME))
				ai.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS);
			else ai.removeFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS);
			ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyPercent(ID, -FLUX_RED);
			ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent(ID, -FLUX_RED);
			ship.getMutableStats().getBallisticRoFMult().modifyPercent(ID, RPM_INCREASE);
			ship.getMutableStats().getEnergyRoFMult().modifyPercent(ID, RPM_INCREASE);

			// Iterate over all the weapons on this ship and
			if (particletimer.interval.intervalElapsed()) {
				particletimer.reset();
				for (WeaponAPI weapon : ship.getAllWeapons()) {
					if (weapon.getType() == WeaponAPI.WeaponType.BALLISTIC
							|| weapon.getType() == WeaponAPI.WeaponType.ENERGY
							|| weapon.getType() == WeaponAPI.WeaponType.HYBRID
							|| weapon.getType() == WeaponAPI.WeaponType.SYNERGY
							|| weapon.getType() == WeaponAPI.WeaponType.COMPOSITE) {
						pos = weapon.getLocation();

						float sz = 15f;
						if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) sz = 25;
						else
						if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) sz = 35f;

						Global.getCombatEngine().addSwirlyNebulaParticle(
								pos, new Vector2f(ship.getVelocity().x*0.5f, ship.getVelocity().y*0.5f),
								sz, 3f, 0.3f, 0.3f,
								0.8f,

								NAUtils.isStargazerRed(ship) ? PARTICLE_CHARGE_COLOR_SG : new Color(51, 151, 223, 150),
								true
						);

					}
				}
			} else {
				particletimer.interval.advance(amount);
			}

			if ((ship.getShield() != null && ship.getShield().isOn()) || (ship.getFluxTracker() != null && ship.getFluxTracker().isOverloadedOrVenting())) {
				effectlevel.level = 0.5f;
				data.reset((Float) mag.get(ship.getHullSize()));
				float chance = 1f * (ARC_CHANCE_VISUAL * ship.getAllWeapons().size());
				arctimer.reset();
				arctimer.resetCount();
				for (WeaponAPI weapon : ship.getAllWeapons()) {
					if (Math.random() < chance &&(weapon.getType() == WeaponAPI.WeaponType.BALLISTIC || weapon.getType() == WeaponAPI.WeaponType.ENERGY)) {
						pos = weapon.getLocation();
						vel = Vector2f.add(ship.getVelocity(),
								NAUtils.lengthdir(PARTICLE_VELOCITY, (float) (Math.random() * 2f * Math.PI)),
								null);

						engine.spawnEmpArc(ship,
								pos,
								ship,
								ship,
								DamageType.ENERGY,
								0,
								0, // emp
								100000f, // max range
								"tachyon_lance_emp_impact", //"tachyon_lance_emp_impact",
								20f, // thickness
								NAUtils.isStargazerRed(ship) ? PARTICLE_CHARGE_COLOR_SG : PARTICLE_CHARGE_COLOR,
								new Color(65,125,255,255)
						);
						break;
					}
				}




			}
			if (ship == player) {
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"nightcrosstargeting",
						"graphics/icons/hullsys/high_energy_focus.png",
						"Dynamic Grid",
						"+" + Math.round(RPM_INCREASE) + "% RoF, -" + Math.round(FLUX_RED) + "% weapon flux cost",
						false);
			}
		} else {


			ai.removeFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS);
			ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify(ID);
			ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(ID);
			ship.getMutableStats().getBallisticRoFMult().unmodify(ID);
			ship.getMutableStats().getEnergyRoFMult().unmodify(ID);



			if (effectlevel.level > 0.01) {
				if ((ship.getShield() == null || ship.getShield().isOff()) && !((ship.getFluxTracker() != null && ship.getFluxTracker().isOverloadedOrVenting()))) {
					data.interval.advance(amount);
				} else {
					data.reset((Float) mag.get(ship.getHullSize()));
					effectlevel.level = 0f;

					if (chargesound.sound != null) {
						chargesound.sound.stop();
						chargesound.sound = null;
					}
				}
				if (data.interval.intervalElapsed()) {
					effectlevel.level = 1f;
					Global.getSoundPlayer().playSound(ACTIVATE_SOUND, 1f, 1f, ship.getLocation(), ship.getVelocity());
					if (chargesound.sound != null) {
						chargesound.sound.stop();
						chargesound.sound = null;
					}
					if (ship != player && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS) && shieldAI.holdShieldsOff) {
						ship.getAIFlags().unsetFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS);
					}
				} else {

					if (particletimer.interval.intervalElapsed()) {
						pos = ship.getLocation();
						vel = Vector2f.add(ship.getVelocity(),
								NAUtils.lengthdir(PARTICLE_VELOCITY, (float) (Math.random() * 2f * Math.PI)),
								null);

						light = new StandardLight(pos, zero, zero, null);
						light.setIntensity(0.1f + 0.25f * data.interval.getElapsed() / data.interval.getIntervalDuration());
						light.setVelocity(vel);
						light.setSize(ship.getCollisionRadius());
						light.setColor(NAUtils.isStargazerRed(ship) ? PARTICLE_CHARGE_COLOR_SG : PARTICLE_CHARGE_COLOR);
						light.fadeIn(0.05f);
						light.setLifetime(PARTICLE_DURATION);
						light.setAutoFadeOutTime(0.17f);
						light.setSize(25f);
						LightShader.addLight(light);
					} else {
						particletimer.interval.advance(amount);
					}


					if (chargesound.sound == null && ((ship.getShield() == null || ship.getShield().isOff()) && !((ship.getFluxTracker() != null && ship.getFluxTracker().isOverloadedOrVenting())))) {
						chargesound.sound = Global.getSoundPlayer().playSound(CHARGE_SOUND, 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
				}

				if (ship == player) {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							"nightcrosstargeting",
							"graphics/icons/hullsys/high_energy_focus.png",
							"Nightcross Systems Integration",
							"Rerouting power... " + ((int) (100f * data.interval.getElapsed()/Math.max(0.5f, data.interval.getIntervalDuration()))) + "%",
							true);


				} else if (!ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS) && !shieldAI.holdShieldsOff
				&& !(ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HARASS_MOVE_IN) ||
						ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON) ||
						ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP) ||
						ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE) ||
						ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER) ||
						ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))) {
					// Temporary AI modification to get the AI not to use shields while it's charging.

					ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS, (Float) mag.get(ship.getHullSize()));
					shieldAI.holdShieldsOff = true;
				}
				//"system_entropy"


			} else {
				if ((ship.getShield() == null || ship.getShield().isOff()) && !((ship.getFluxTracker() != null && ship.getFluxTracker().isOverloadedOrVenting()))) {
					effectlevel.level = 0.5f;
				}
				if (ship == player) {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							"nightcrosstargeting",
							"graphics/icons/hullsys/quantum_disruptor.png",
							"Nightcross Systems Integration",
							"Power diverted to shields.",
							true);
				}
			}
		}
	}
}
