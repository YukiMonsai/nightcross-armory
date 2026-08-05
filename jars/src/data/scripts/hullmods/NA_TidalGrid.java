package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NA_Flickerfield;
import data.scripts.campaign.plugins.NAUtils;
import data.scripts.stardust.NA_StargazerStars;
import data.scripts.util.NAUtil;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NA_TidalGrid extends BaseHullMod {

	private static Map mag = new HashMap();
	static {
		mag.put(HullSize.FIGHTER, 1.0f);
		mag.put(HullSize.FRIGATE, 3f);
		mag.put(HullSize.DESTROYER, 2.6f);
		mag.put(HullSize.CRUISER, 2.3f);
		mag.put(HullSize.CAPITAL_SHIP, 2f);
	}
	public static Color WEAPON_GLOW = new Color(20, 159, 245,155);

	public static final float FLUX_RED = 50f;
	public static final float FLUX_RED_ENG = 33f;
	public static final float RPM_INCREASE = 50f;
	public static final float DMG_INCREASE = 33f;
	public static final float MISSILE_DMG = 33f;
	public static final float MISSILE_HP = 50f;

	public static final float TIME_SECONDS = 1.5f;

	public static final float DURATION_FACTOR = 1.2f; // Each second in phase
	public static final float DURATION_MAX = 15.0f; // Max duration
	public static final float TIMEFLOW_PEN = 0.7f; // Max duration

	public static final float PARTICLE_PERIOD = 0.2f;
	public static final float ARC_PERIOD = 0.08f;
	public static final float PARTICLE_DURATION = 0.15f;
	public static final float PARTICLE_RADIUS = 30f;
	public static final float PARTICLE_VELOCITY = 5f;
	public static final float ARC_CHANCE_VISUAL = 0.12f;
	public static final float ARC_CHANCE_VISUAL_REPEAT = 0.03f;

	public static final Color PARTICLE_CHARGE_COLOR_SG = new Color(138, 0, 23, 150);
	public static final Color PARTICLE_CHARGE_COLOR = new Color(0, 75, 175, 150);
	public static final String ACTIVATE_SOUND = "system_ammo_feeder";
	public static final String CHARGE_SOUND = "na_chargeup";



	public static float FLUX_THRESHOLD_INCREASE_PERCENT = 300f;



	public static String ID = "NightcrossTidalGrid";

	private static class NightcrossTargetingData {
		IntervalUtil interval = new IntervalUtil(DURATION_MAX, DURATION_MAX);
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
	public static class NightcrossTargetingLevelData {
		float level = 1.0f;
		float duration = 0.0f;
	}

	private static Map mag2 = new HashMap();
	static {
		mag2.put(HullSize.FIGHTER, 50f);
		mag2.put(HullSize.FRIGATE, 20f);
		mag2.put(HullSize.DESTROYER, 20f);
		mag2.put(HullSize.CRUISER, 20f);
		mag2.put(HullSize.CAPITAL_SHIP, 20f);
	}


	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) FLUX_RED + "%";
		if (index == 1) return "" + (int) RPM_INCREASE + "%";
		if (index == 2) return "" + (int) DMG_INCREASE + "%";
		if (index == 3) return "" + Math.round((Float) mag.get(HullSize.FRIGATE)) + "";
		if (index == 4) return "" + Math.round((Float) mag.get(HullSize.DESTROYER)) + "";
		if (index == 5) return "" + Math.round((Float) mag.get(HullSize.CRUISER)) + "";
		if (index == 6) return "" + Math.round((Float) mag.get(HullSize.CAPITAL_SHIP)) + " seconds";
		if (index == 7) return "" + (int) DURATION_FACTOR + "%";
		if (index == 8) return "" + (int) 30 + "%";

		if (index == 9) return "" + Math.round((Float) mag2.get(HullSize.FRIGATE)) + "";
		return "err";
	}


	public String getSModDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + Math.round((Float) mag2.get(hullSize)) + "%";
		return null;
	}

	public static Color TIDAL_BLUE = new Color(53, 53, 253);

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color t = Misc.getTextColor();
		Color g = Misc.getGrayColor();

		tooltip.addPara("\"What your children achieved with freedom from gravity, we will have the power to determine on our own.", TIDAL_BLUE, opad);

		tooltip.addSectionHeading("Tidal Burst", Alignment.MID, opad);

		tooltip.addPara("The ship's flux systems are designed to handle an arcane device called a \"Tidal Reactor,\" producing relatively shallow phase dives and keeping the ship's flux venting components and weapon systems partially in phase after exiting a dive. For a duration after exiting phase, the resulting Tidal Burst grants:\n" +
						" - -%s ballistic weapon flux cost\n - +%s ballistic fire rate\n - -%s energy weapon flux cost\n - +%s energy weapon damage\n - +%s missile damage\n - +%s missile hitpoints\n",
				opad, h,
				"" + (int) FLUX_RED + "%",
				"" + (int) RPM_INCREASE + "%",
				"" + (int) FLUX_RED_ENG + "%",
				"" + (int) DMG_INCREASE + "%",
				"" + (int) MISSILE_DMG + "%",
				"" + (int) MISSILE_HP + "%"
		);


		tooltip.addSectionHeading("Duration", Alignment.MID, opad);

		tooltip.addPara("The duration of this effect is equal to %s seconds per second spent in phase, up to a maximum of %s seconds. The system is disrupted and duration reset at the start of a phase dive, and is re-enabled after remaining in phase space for %s seconds.",
				opad, h,
				"" + Math.round((Float) mag.get(hullSize)) + "",
				"" + (int) DURATION_FACTOR + "",
				"" + (int) DURATION_MAX + ""
		);


		tooltip.addSectionHeading("Other Effects", Alignment.MID, opad);

		tooltip.addPara("The phase coils aretuned to a much shallower dive, resulting in %s reduced timeflow increase while phased compared to standard phase coils, but reducing phase coil stress significantly and almost completely eliminating the speed penalty from hard flux. Incompatible with Adaptive Phase Coil.",
				opad, h,
				"" + 30 + "%");



	}

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
	public float getTooltipWidth() {
		return super.getTooltipWidth();
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(
				Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT);
		stats.getDynamic().getMod(
				Stats.PHASE_TIME_BONUS_MULT).modifyPercent(id, TIMEFLOW_PEN);

		if (isSMod(stats))
			stats.getVentRateMult().modifyPercent(id, (float) mag2.get(hullSize));
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

			if (!ship.getPhaseCloak().isOn()) {
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

		if (ship.getPhaseCloak().isOn() && effectlevel.level == 0) {
			effectlevel.duration = Math.min(effectlevel.duration + amount * DURATION_FACTOR, DURATION_MAX);
		}

		if (effectlevel.level > 0) {
			if (ship.getPhaseCloak().isOn()) {
				data.intervalOff.advance(amount);
			} else {
				data.resetOff((Float) mag.get(ship.getHullSize()));
			}

			ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify(ID);
			ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(ID);
			ship.getMutableStats().getBallisticRoFMult().unmodify(ID);
			ship.getMutableStats().getEnergyWeaponDamageMult().unmodify(ID);
			ship.getMutableStats().getMissileHealthBonus().unmodify(ID);
			ship.getMutableStats().getMissileWeaponDamageMult().unmodify(ID);


			if (data.intervalOff.intervalElapsed()) {
				effectlevel.level = 0f;
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
								new Color(194, 210, 248,255)
						);
						break;
					}
				}

			} else if (ship == player) {
				if (ship.getPhaseCloak().isOn() && data.intervalOff.getIntervalDuration() > 0) {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							"na_tidalcloak",
							"graphics/icons/hullsys/high_energy_focus.png",
							"Tidal Grid",
							"Tidal Burst charging " + ((int) (100f * data.intervalOff.getElapsed() / data.intervalOff.getIntervalDuration())) + "%",
							true);
				} else {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							"na_tidalcloak",
							"graphics/icons/hullsys/high_energy_focus.png",
							"Tidal Grid",
							"Tidal Burst inactive. enter phase to prime.",
							true);
				}

			}
		} else {
			if (!ship.getPhaseCloak().isOn()) {

				if (effectlevel.duration > 0) {

					data.reset(Math.min(effectlevel.duration, DURATION_MAX));
					effectlevel.duration = 0;
				}

				ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyPercent(ID, -FLUX_RED);
				ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent(ID, -FLUX_RED_ENG);
				ship.getMutableStats().getBallisticRoFMult().modifyPercent(ID, RPM_INCREASE);
				ship.getMutableStats().getEnergyWeaponDamageMult().modifyPercent(ID, DMG_INCREASE);
				ship.getMutableStats().getMissileHealthBonus().modifyPercent(ID, MISSILE_HP);
				ship.getMutableStats().getMissileWeaponDamageMult().modifyPercent(ID, MISSILE_DMG);

				//ship.setJitter(ship, TIDAL_BLUE, 0.5f, 3, 20f);

				for (WeaponAPI w : ship.getAllWeapons()) {
					if (!w.isDecorative()) {
						w.setGlowAmount(0.5f, WEAPON_GLOW);
					}
				}

				if (ship == player) {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							"na_tidalcloak",
							"graphics/icons/hullsys/high_energy_focus.png",
							"Tidal Grid",
							"Increased fire rate and damage for " + ((int) (data.interval.getIntervalDuration() - data.interval.getElapsed())) + " seconds",
							false);
				}



			} else {

				// AI fix for the ister chaos
				if (ship.getAIFlags() != null && ship.getShipTarget() != null && (ship != Global.getCombatEngine().getPlayerShip()
						|| (Global.getCombatEngine().getCombatUI() != null && Global.getCombatEngine().getCombatUI().isAutopilotOn()))
					&& NAUtils.shipSize(ship) + 1 >= NAUtils.shipSize(ship.getShipTarget())) {
					if (!ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER)) {
						if (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN_IN_GOOD_SPOT)) {
							ship.getPhaseCloak().deactivate();
						}
					}
				}

				if (ship == player) {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							"na_tidalcloak",
							"graphics/icons/hullsys/high_energy_focus.png",
							"Tidal Grid",
							"Tidal Burst duration: " + ((int) (effectlevel.duration)) + " seconds",
							true);
				}

				ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify(ID);
				ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(ID);
				ship.getMutableStats().getBallisticRoFMult().unmodify(ID);
				ship.getMutableStats().getEnergyWeaponDamageMult().unmodify(ID);
				ship.getMutableStats().getMissileHealthBonus().unmodify(ID);
				ship.getMutableStats().getMissileWeaponDamageMult().unmodify(ID);
			}


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

						Global.getCombatEngine().addNegativeSwirlyNebulaParticle(
								pos, new Vector2f(ship.getVelocity().x*0.5f, ship.getVelocity().y*0.5f),
								sz, 3f, 0.5f, 0.5f,
								0.8f,
								new Color(94, 92, 0, 65)
						);

					}
				}
			} else {
				particletimer.interval.advance(amount);
			}

			if ((ship.getPhaseCloak().isOn() || (ship.getFluxTracker() != null && ship.getFluxTracker().isOverloadedOrVenting())) && !data.intervalOff.intervalElapsed()) {
				data.resetOff((Float) mag.get(ship.getHullSize()));
				effectlevel.level = 1f;

				Global.getSoundPlayer().playSound(ACTIVATE_SOUND, 1f, 1f, ship.getLocation(), ship.getVelocity());
				if (chargesound.sound != null) {
					chargesound.sound.stop();
					chargesound.sound = null;
				}
			} else {
				if (!ship.getPhaseCloak().isOn()) {
					data.resetOff((Float) mag.get(ship.getHullSize()));
				}
				if (data.interval.intervalElapsed()) {
					if (chargesound.sound == null && !ship.getPhaseCloak().isOn()) {
						chargesound.sound = Global.getSoundPlayer().playSound("na_steamoff", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
					if (!ship.getPhaseCloak().isOn()) {
						effectlevel.level = 1f;
					}
				} else {
					if (chargesound.sound != null) {
						chargesound.sound.stop();
						chargesound.sound = null;
					}
					if (!(ship.getMutableStats().getDynamic().getStat(NA_Flickerfield.TIDAL_PAUSE).getModifiedValue() > 1.01))
						data.interval.advance(amount);
				}
			}
		}
	}
}
