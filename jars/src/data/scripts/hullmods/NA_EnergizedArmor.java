package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.NA_FastCaps;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NA_EnergizedArmor extends BaseHullMod {
	public static final float FLUX_RED = 5f;
	public static final float DMG_RATIO = 0.7f;
	public static final float DMG_RED = 50f;

	private String ID = "NA_EnergizedArmor";

	private static Map maxenergy = new HashMap();
	static {
		maxenergy.put(HullSize.FIGHTER, 50f);
		maxenergy.put(HullSize.FRIGATE, 1000f);
		maxenergy.put(HullSize.DESTROYER, 2500f);
		maxenergy.put(HullSize.CRUISER, 5000f);
		maxenergy.put(HullSize.CAPITAL_SHIP, 10000f);
	}
	private static Map maxarmor = new HashMap();
	static {
		maxarmor.put(HullSize.FIGHTER, 200f);
		maxarmor.put(HullSize.FRIGATE, 300f);
		maxarmor.put(HullSize.DESTROYER, 450f);
		maxarmor.put(HullSize.CRUISER, 600f);
		maxarmor.put(HullSize.CAPITAL_SHIP, 800f);
	}

	public static final Color GLOW = new Color(195, 237, 246,155);

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return ((int) FLUX_RED) + "%";
		if (index == 1) return ((int) (100f * DMG_RATIO)) + "%";
		if (index == 2) return "" + Math.round((Float) maxenergy.get(HullSize.FRIGATE))
				+ "/" + Math.round((Float) maxenergy.get(HullSize.DESTROYER))
				+ "/" + Math.round((Float) maxenergy.get(HullSize.CRUISER))
				+ "/" + Math.round((Float) maxenergy.get(HullSize.CAPITAL_SHIP)) + "";
		if (index == 3) return "" + Math.round((Float) maxarmor.get(HullSize.FRIGATE))
				+ "/" + Math.round((Float) maxarmor.get(HullSize.DESTROYER))
				+ "/" + Math.round((Float) maxarmor.get(HullSize.CRUISER))
				+ "/" + Math.round((Float) maxarmor.get(HullSize.CAPITAL_SHIP)) + "";
		if (index == 4) return ((int) (DMG_RED)) + "%";
		return null;
	}



	private static class NAEnergizedArmorData {

		private NA_EnergizedArmorDamageTakenListener listener;
		private float lastFrameHardFlux = 0;
		private float energyTotal = 0;
		private boolean inited = false;

		private IntervalUtil soundTimer = new IntervalUtil(1.5f, 2.5f);
		private IntervalUtil glowtimer = new IntervalUtil(0.1f, 0.15f);
	}


	public boolean isApplicableToShip(ShipAPI ship) {
		return ship.getPhaseCloak() != null || ship.getShield() != null;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		return "Can only be installed on ships with shields or phase cloaks.";
	}

	public String getSModDescriptionParam(int index, HullSize hullSize) {
		//if (index == 0) return "" + (int) SMOD_AMMO_BONUS + "%";
		return null;
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id){
		if (ship.getVariant().getHullMods().contains("shield_shunt")) {
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "shield_shunt", "na_energizedarmor");
		}
	}
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getMissileRoFMult().modifyMult(ID, ROF_PENALTY);

		if (!(stats.getEntity() instanceof ShipAPI ship)) return;

        String key = ID + "_" + ship.getId();
		NAEnergizedArmorData data = (NAEnergizedArmorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAEnergizedArmorData();
			ship.setCustomData(key, data);
		}

		if (data.listener != null) {


			data.inited = false;
			unapply(data.listener.ship);
		}


		boolean sMod = isSMod(stats);
		if (sMod) {
			//stats.getFluxDissipation().modifyPercent(ID, -FLUX_RED/2);
		} else {
			stats.getFluxDissipation().modifyPercent(ID, -FLUX_RED);
		}
	}


	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();


		if (!ship.isAlive()) return;

		String key = ID + "_" + ship.getId();
		NAEnergizedArmorData data = (NAEnergizedArmorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAEnergizedArmorData();
			ship.setCustomData(key, data);
		}


		if (!data.soundTimer.intervalElapsed()) {
			data.soundTimer.advance(amount);
		}
		if (!data.glowtimer.intervalElapsed()) {
			data.glowtimer.advance(amount);
		}
		init(ship);

		// generate energy
		float maxEnergy = Math.max(100f, (float) maxenergy.get(ship.getHullSize()));
		float currFlux = ship.getFluxTracker().getHardFlux();
		if (currFlux > data.lastFrameHardFlux) {
			boolean loud = data.energyTotal < (maxEnergy - 5f);
			float delt = currFlux - data.lastFrameHardFlux;

			data.energyTotal += delt;
			if (data.soundTimer.intervalElapsed() && (loud || delt > 5)) {
				if (data.energyTotal >= maxEnergy && loud) {
					for (int i = 0; i < 3; i++) {
						Global.getCombatEngine().spawnEmpArcPierceShields(ship,
								ship.getLocation(),
								ship,
								ship,
								DamageType.ENERGY,
								0,
								0, // emp
								100000f, // max range
								"", //"tachyon_lance_emp_impact",
								8f, // thickness
								new Color(0, 155, 175),
								new Color(65,125,255,255)
						);
					}
					data.soundTimer.randomize();

					if (data.glowtimer.intervalElapsed()) {
						data.glowtimer = new IntervalUtil(0.5f, 0.5f);
						ship.setJitterShields(false);
						ship.setJitter(
								ship, GLOW, 0.5f - 0.5f*(data.glowtimer.getElapsed()/data.glowtimer.getIntervalDuration()), 3, 5f
						);
					}

					Global.getSoundPlayer().playSound("na_ionmatrix", 1f, 1.5f, ship.getLocation(), ship.getVelocity());
				} else {

					if (Math.random() < 0.2f)
						Global.getCombatEngine().spawnEmpArcPierceShields(ship,
								ship.getLocation(),
								ship,
								ship,
								DamageType.ENERGY,
								0,
								0, // emps
								100000f, // max range
								"", //"tachyon_lance_emp_impact",
								4f, // thickness
								new Color(0, 155, 175, 110),
								new Color(65,125,255,255)
						);

					Global.getSoundPlayer().playSound("na_ionmatrix", 1f, 0.05f + MathUtils.getRandomNumberInRange(0f, 0.25f), ship.getLocation(), ship.getVelocity());

					data.soundTimer.randomize();

				}
			}
		}
		data.lastFrameHardFlux = currFlux;

		// clamp

		data.energyTotal = Math.max(0, Math.min(data.energyTotal, maxEnergy));

		float currEnergy = data.energyTotal;
		float effArmor = getEffArmor(ship);
		float dmgRed = getEffDmgRed(ship);


		MutableShipStatsAPI stats = ship.getMutableStats();
		if (stats != null) {
			if (effArmor > 0) {
				stats.getEffectiveArmorBonus().modifyFlat(ID, effArmor);
				stats.getHullDamageTakenMult().modifyPercent(ID, dmgRed);
				stats.getWeaponDamageTakenMult().modifyPercent(ID, dmgRed);
				stats.getEngineDamageTakenMult().modifyPercent(ID, dmgRed);
				ship.setJitterShields(false);
				ship.setJitter(
						ship, GLOW, 0.3f * currEnergy / maxEnergy, 3, 25f
				);
			} else {
				stats.getEffectiveArmorBonus().unmodify(ID);
				stats.getHullDamageTakenMult().unmodify(ID);
				stats.getWeaponDamageTakenMult().unmodify(ID);
				stats.getEngineDamageTakenMult().unmodify(ID);
			}
			if (!data.glowtimer.intervalElapsed()) {
				ship.setJitterShields(false);
				ship.setJitter(
						ship, GLOW, 1f - (data.glowtimer.getElapsed()/data.glowtimer.getIntervalDuration()), 1, 15f
				);
			}
		}

		if (amount > 0 && !ship.isPhased() && (ship.getShield() == null || !ship.getShield().isOn())) {
			// slow decay
			float decay = 10;
			data.energyTotal = Math.max(0, data.energyTotal - amount * decay);
		}

		if (ship == player) {
			Global.getCombatEngine().maintainStatusForPlayerShip(
					ID,
					"graphics/icons/hullsys/high_energy_focus.png",
					"Armor Energizer Matrix",
					((int) currEnergy) + " energy => +" + ((int) effArmor) + " effective armor, " + ((int) dmgRed) + "% hull damage taken",
					false);
		}

	}

	private void unapply(ShipAPI ship) {

		String key = ID + "_" + ship.getId();
		NAEnergizedArmorData data = (NAEnergizedArmorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAEnergizedArmorData();
			ship.setCustomData(key, data);
		}

		if (data.listener != null && ship.hasListener(data.listener)) {
			ship.removeListener(data.listener);
		}
		data.listener = null;

		MutableShipStatsAPI stats = ship.getMutableStats();
		if (stats != null) {
			stats.getEffectiveArmorBonus().unmodify(ID);
			stats.getHullDamageTakenMult().unmodify(ID);
			stats.getWeaponDamageTakenMult().unmodify(ID);
			stats.getEngineDamageTakenMult().unmodify(ID);
		}
	}

	private void init(ShipAPI ship){
		String key = ID + "_" + ship.getId();
		NAEnergizedArmorData data = (NAEnergizedArmorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAEnergizedArmorData();
			ship.setCustomData(key, data);
		}
		if (data.inited) return;
		if (data.listener == null) {
			data.listener = new NA_EnergizedArmorDamageTakenListener(ship, this);
			ship.addListener(data.listener);
		}
		data.energyTotal = 0;
		data.lastFrameHardFlux = 0;
		data.inited = true;
	}



	public static class NA_EnergizedArmorDamageTakenListener implements DamageTakenModifier {
		private final ShipAPI ship;
		private final NA_EnergizedArmor hullmod;

		public NA_EnergizedArmorDamageTakenListener(ShipAPI ship, NA_EnergizedArmor hullmod) {
			this.ship = ship;
			this.hullmod = hullmod;
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (!shieldHit && hullmod != null && target instanceof ShipAPI target_ship
					&& ship != null && ship.isAlive() && target_ship.isAlive() && target_ship == ship) {


				String key = hullmod.ID + "_" + ship.getId();
				NAEnergizedArmorData data = (NAEnergizedArmorData) ship.getCustomData().get(key);
				if (data == null) {
					data = new NAEnergizedArmorData();
					ship.setCustomData(key, data);
				}

				if (data.energyTotal > 0) {
					// Reduce energy
					float reduct = damage.getDamage();
					if (param instanceof BeamAPI beam) {
						reduct *= damage.getDpsDuration();
					}

					if (reduct > 0) {
						data.energyTotal = Math.max(0, data.energyTotal - DMG_RATIO * Math.max(0, reduct));

						if (data.energyTotal == 0 && data.soundTimer.intervalElapsed()) {
							data.soundTimer.randomize();

							RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
							ripple.setSize(ship.getCollisionRadius());
							ripple.setIntensity(65.0F);
							ripple.setFrameRate(30);
							ripple.setCurrentFrame(45);
							ripple.fadeOutIntensity(0.75f);
							DistortionShader.addDistortion(ripple);

							Global.getSoundPlayer().playSound("na_superblaster_impact", 0.8f, 1f, ship.getLocation(), ship.getVelocity());

							for (int i = 0; i < 7; i++) {
								Global.getCombatEngine().spawnEmpArcPierceShields(ship,
										point,
										ship,
										ship,
										DamageType.ENERGY,
										0,
										0, // emp
										100000f, // max range
										"tachyon_lance_emp_impact",
										8f, // thickness
										new Color(64, 0, 175),
										new Color(255, 65, 100,255)
								);
							}
						}
						if (MathUtils.getRandomNumberInRange(0f, 100f + damage.getDamage()) > 100f) {
							Global.getCombatEngine().spawnEmpArcPierceShields(ship,
									point,
									ship,
									ship,
									DamageType.ENERGY,
									0,
									0, // emp
									250f, // max range
									"", //"tachyon_lance_emp_impact",
									8f, // thickness
									new Color(64, 0, 175),
									new Color(255, 65, 100,255)
							);
						}
						data.glowtimer = new IntervalUtil(0.25f, 0.25f);

					}

				}
			}
			return "";
		}
	}

	private float getEffArmor(ShipAPI ship) {
		float maxEnergy = Math.max(100f, (float) maxenergy.get(ship.getHullSize()));
		float maxArmor = (float) maxarmor.get(ship.getHullSize());

		String key = ID + "_" + ship.getId();
		NAEnergizedArmorData data = (NAEnergizedArmorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAEnergizedArmorData();
			ship.setCustomData(key, data);
		}

		data.energyTotal = Math.max(0, Math.min(data.energyTotal, maxEnergy));

		float currEnergy = data.energyTotal;
		return maxArmor * (float) (Math.sqrt(currEnergy / maxEnergy));
	}
	private float getEffDmgRed(ShipAPI ship) {
		float maxEnergy = Math.max(100f, (float) maxenergy.get(ship.getHullSize()));

		String key = ID + "_" + ship.getId();
		NAEnergizedArmorData data = (NAEnergizedArmorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAEnergizedArmorData();
			ship.setCustomData(key, data);
		}

		data.energyTotal = Math.max(0, Math.min(data.energyTotal, maxEnergy));

		float currEnergy = data.energyTotal;
		return -DMG_RED * (float) (Math.sqrt(currEnergy / maxEnergy));
	}
}
