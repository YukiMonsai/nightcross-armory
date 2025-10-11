package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.combat.C;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.stardust.NA_StargazerStardust;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class NA_HardlightMatrix2 extends BaseHullMod {


	public static float AMT_SHIELD = 0.40f;
	protected String ID = "NA_HardlightMatrix2";

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int)(100f * AMT_SHIELD) + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getMissileRoFMult().modifyMult(ID, ROF_PENALTY);

		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship instanceof ShipAPI) {

			String key = ID + "_" + ship.getId();
			NAAEGISDeflectorData data = (NAAEGISDeflectorData) ship.getCustomData().get(key);
			if (data == null) {
				data = new NAAEGISDeflectorData();
				ship.setCustomData(key, data);
			}

			if (data.listener != null) {


				data.inited = false;
				unapply(data.listener.ship);
			}
		}

	}




	public static class NAAEGISDeflectorDamageTakenListener implements DamageTakenModifier {
		private final ShipAPI ship;
		private final NA_HardlightMatrix2 hullmod;

		public NAAEGISDeflectorDamageTakenListener(ShipAPI ship, NA_HardlightMatrix2 hullmod) {
			this.ship = ship;
			this.hullmod = hullmod;
		}

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (shieldHit && hullmod != null && target instanceof ShipAPI target_ship
					&& ship != null && ship.isAlive() && target_ship.isAlive() && target_ship == ship) {


				String key = hullmod.ID + "_" + ship.getId();
				NAAEGISDeflectorData data = (NAAEGISDeflectorData) ship.getCustomData().get(key);
				if (data == null) {
					data = new NAAEGISDeflectorData();
					ship.setCustomData(key, data);
				}

				if (damage.getDamage() > 0 && data.HitTimer.intervalElapsed()) {

					List<ShipAPI> modules = ship.getChildModulesCopy();
					EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
					params.maxZigZagMult = 0.35f;
					params.flickerRateMult = 0.45f;
					params.glowSizeMult *= 0.8f;

					for (ShipAPI module : modules) {
						if (module.isAlive()) {
							if (module.getHullSpec().hasTag("hardlight_generator")) {
								List<WeaponAPI> weapons = module.getAllWeapons();
								if (weapons.size() == 0 || !weapons.get(0).isDisabled()) {
									if (ship.getShield() != null && ship.getShield().isOn()) {
										EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
												point, ship, weapons.get(0).getLocation(), module, 5f,
												new Color(166, 71, 255, 50),
												new Color(255, 205, 224, 150), params
										);
										//arc.setSingleFlickerMode(true);
										arc.setRenderGlowAtEnd(false);
										data.HitTimer.setElapsed(0);
									}

								}

							}
						}
					}
				}
			}
			return "";
		}
	}


	private static class NAAEGISDeflectorData {

		private NAAEGISDeflectorDamageTakenListener listener;
		public boolean inited = false;

		private IntervalUtil ArcTimer = new IntervalUtil(0.07f, 0.15f);
		private IntervalUtil HitTimer = new IntervalUtil(0.05f, 0.1f);
	}



	private void unapply(ShipAPI ship) {

		String key = ID + "_" + ship.getId();
		NAAEGISDeflectorData data = (NAAEGISDeflectorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAAEGISDeflectorData();
			ship.setCustomData(key, data);
		}

		if (data.listener != null && ship.hasListener(data.listener)) {
			ship.removeListener(data.listener);
		}
		data.listener = null;

	}


	private void init(ShipAPI ship){
		String key = ID + "_" + ship.getId();
		NAAEGISDeflectorData data = (NAAEGISDeflectorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAAEGISDeflectorData();
			ship.setCustomData(key, data);
		}
		if (data.inited) return;
		if (data.listener == null) {
			data.listener = new NAAEGISDeflectorDamageTakenListener(ship, this);
			ship.addListener(data.listener);
		}
		data.inited = true;
	}


	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;

		float armorBonus = 0;
		float bonuses = 0;

		List<ShipAPI> modules = ship.getChildModulesCopy();


		init(ship);

		String key = ID + "_" + ship.getId();
		NAAEGISDeflectorData data = (NAAEGISDeflectorData) ship.getCustomData().get(key);
		if (data == null) {
			data = new NAAEGISDeflectorData();
			ship.setCustomData(key, data);
		}


		if (data != null) {
			data.HitTimer.advance(amount);
			data.ArcTimer.advance(amount);
		}

		
		if (!ship.getFluxTracker().isOverloadedOrVenting()) {
			EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
			params.maxZigZagMult = 1.6f;
			params.flickerRateMult = 0.45f;
			params.glowSizeMult *= 0.4f;
			params.glowColorOverride = new Color(108, 25, 250);

			int ii = 0;
			for (ShipAPI module : modules) {
				if (module.isAlive()) {
					if (module.getHullSpec().hasTag("hardlight_generator")) {
						List<WeaponAPI> weapons = module.getAllWeapons();
						if (weapons.size() == 0 || !weapons.get(0).isDisabled()) {
							if (ship.getShield() != null && ship.getShield().isOn()) {
								if (amount > 0 && Math.random() < 0.75f && data.ArcTimer.intervalElapsed()) {

									EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
											weapons.get(0).getLocation(), module, MathUtils.getPointOnCircumference(ship.getShieldCenterEvenIfNoShield(),
													ship.getShieldRadiusEvenIfNoShield(),
													MathUtils.getRandomNumberInRange(
															ship.getShield().getFacing() - (ii) * ship.getShield().getActiveArc()*0.5f,
															ship.getShield().getFacing() + (1-ii) * ship.getShield().getActiveArc()*0.5f)
													), ship, 15f,
											ship.getShield().getInnerColor(),
											ship.getShield().getRingColor(), params
									);
									arc.setSingleFlickerMode(false);
									arc.setRenderGlowAtEnd(true);
									data.ArcTimer.setElapsed(0);
								}
							}



							bonuses += 1;
							if (weapons.size() > 0) {
								weapons.get(0).setForceFireOneFrame(true);
							}
						}
						ii++;

					}
				}
			}
		}


		if (bonuses > 0) {
			ship.getMutableStats().getShieldAbsorptionMult().modifyMult(ID, 1f - AMT_SHIELD*bonuses);
			if (bonuses > 0)
				ship.getMutableStats().getHardFluxDissipationFraction().modifyMult(ID, 0f);

			if (Global.getCombatEngine().getPlayerShip().getId().equals(ship.getId())) {
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"na_hardlightmatrix2",
						"graphics/icons/hullsys/high_energy_focus.png",
						"AEGIS Deflectors",
						"-" + ((int) (AMT_SHIELD * 100f * bonuses)) + "% shield damage taken",
						false);
			}


		} else {

			if (Global.getCombatEngine().getPlayerShip().getId().equals(ship.getId())) {
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"na_hardlightmatrix2",
						"graphics/icons/hullsys/high_energy_focus.png",
						"AEGIS Deflectors",
						"Deflectors offline.",
						true);
			}

			ship.getMutableStats().getShieldAbsorptionMult().unmodify(ID);
			ship.getMutableStats().getHardFluxDissipationFraction().unmodify(ID);
		}

	}
}
