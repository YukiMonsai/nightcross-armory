package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.NAUtils;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NA_TidalGrid extends BaseHullMod {

	private static Map mag = new HashMap();
	static {
		mag.put(HullSize.FIGHTER, 1.0f);
		mag.put(HullSize.FRIGATE, 1.5f);
		mag.put(HullSize.DESTROYER, 2.0f);
		mag.put(HullSize.CRUISER, 2.5f);
		mag.put(HullSize.CAPITAL_SHIP, 3.0f);
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

	public static final Color PARTICLE_COLOR = new Color(80, 210, 240);
	public static final Color PARTICLE_CHARGE_COLOR = new Color(0, 75, 175);
	public static final String ACTIVATE_SOUND = "system_ammo_feeder";
	public static final String CHARGE_SOUND = "na_chargeup";



	public static float FLUX_THRESHOLD_INCREASE_PERCENT = 75f;



	private String ID = "NightcrossTidalGrid";

	private static class NightcrossTargetingData {
		IntervalUtil interval = new IntervalUtil(TIME_SECONDS, TIME_SECONDS);
		IntervalUtil intervalOff = new IntervalUtil(TIME_SECONDS, TIME_SECONDS);
		public void reset(float time) {
			interval = new IntervalUtil(time, time);
		}
		public void resetOff(float time) {
			intervalOff = new IntervalUtil(time, time);
		}
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
		if (index == 1) return "" + (int) RPM_INCREASE + "%";
		if (index == 2) return "" + Math.round((Float) mag.get(HullSize.FRIGATE)) + "";
		if (index == 3) return "" + Math.round((Float) mag.get(HullSize.DESTROYER)) + "";
		if (index == 4) return "" + Math.round((Float) mag.get(HullSize.CRUISER)) + "";
		if (index == 5) return "" + Math.round((Float) mag.get(HullSize.CAPITAL_SHIP)) + " seconds";
		if (index == 6) return "" + (int) + FLUX_THRESHOLD_INCREASE_PERCENT + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(
				Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT);

	}



	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id){
		if (ship.getVariant().getHullMods().contains("adaptive_coils")) {
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "adaptive_coils", "na_tidalgrid");
		}
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

		if (arctimer.remainingCount > 0 && arctimer.interval.intervalElapsed()) {
			arctimer.reset();
			arctimer.remainingCount -= 1;

			if (ship.getPhaseCloak().isOn()) {
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
								PARTICLE_CHARGE_COLOR,
								new Color(255, 255, 255, 255)
						);

					}
				}
			}
		}

		if (effectlevel.level > 0) {
			if (!ship.getPhaseCloak().isOn()) {
				data.intervalOff.advance(amount);
			}

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
						vel = Vector2f.add(ship.getVelocity(),
								NAUtils.lengthdir(PARTICLE_VELOCITY, (float) (Math.random() * 2f * Math.PI)),
								null);

						light = new StandardLight(pos, zero, zero, null);
						light.setIntensity(0.15f);
						light.setVelocity(vel);
						light.setSize(PARTICLE_RADIUS);
						light.setColor(PARTICLE_COLOR);
						light.fadeIn(0.05f);
						light.setLifetime(PARTICLE_DURATION);
						light.setAutoFadeOutTime(0.17f);
						light.setSize(30f);
						LightShader.addLight(light);

					}
				}
			} else {
				particletimer.interval.advance(amount);
			}

			if (data.intervalOff.intervalElapsed()) {
				effectlevel.level = 0f;
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
								PARTICLE_CHARGE_COLOR,
								new Color(65,125,255,255)
						);
						break;
					}
				}
			}

			if (ship == player) {
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"na_tidalcloak",
						"graphics/icons/hullsys/high_energy_focus.png",
						"Tidal Cloak",
						"Tidal Cloak ACTIVE",
						true);


			}
		} else {
			if (ship.getPhaseCloak().isOn()) {

				ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyPercent(ID, -FLUX_RED);
				ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent(ID, -FLUX_RED);
				ship.getMutableStats().getBallisticRoFMult().modifyPercent(ID, RPM_INCREASE);
				ship.getMutableStats().getEnergyRoFMult().modifyPercent(ID, RPM_INCREASE);
			} else {

				ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify(ID);
				ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(ID);
				ship.getMutableStats().getBallisticRoFMult().unmodify(ID);
				ship.getMutableStats().getEnergyRoFMult().unmodify(ID);
			}


			if (!ship.getPhaseCloak().isOn() && !data.intervalOff.intervalElapsed()) {
				data.resetOff((Float) mag.get(ship.getHullSize()));
				effectlevel.level = 1f;

				Global.getSoundPlayer().playSound(ACTIVATE_SOUND, 1f, 1f, ship.getLocation(), ship.getVelocity());
				if (chargesound.sound != null) {
					chargesound.sound.stop();
					chargesound.sound = null;
				}
			} else {
				if (ship.getPhaseCloak().isOn())
					data.resetOff((Float) mag.get(ship.getHullSize()));
				if (data.interval.intervalElapsed()) {
					if (chargesound.sound == null && ship.getPhaseCloak().isOn()) {
						chargesound.sound = Global.getSoundPlayer().playSound(CHARGE_SOUND, 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
				} else {
					if (chargesound.sound != null) {
						chargesound.sound.stop();
						chargesound.sound = null;
					}
					data.interval.advance(amount);
				}
			}
		}
	}
}
