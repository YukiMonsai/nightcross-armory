package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.NAUtils;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NA_OverloadWeapons extends BaseHullMod {
	private String ID = "NA_OverloadWeapons";
	private static float ARC_PERIOD = 0.25f;


	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
	}

	public static class NA_OverloadWeaponsArcTimer {
		IntervalUtil interval = new IntervalUtil(ARC_PERIOD, ARC_PERIOD*2f);
		int remainingCount = 10;
		public void reset() {
			interval = new IntervalUtil(ARC_PERIOD, ARC_PERIOD*2f);
		}
		public void resetCount() {
			remainingCount = 2 + (int) (Math.random() *3);
		}
	}



	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;

		CombatEngineAPI engine = Global.getCombatEngine();

		String key = ID + "_" + ship.getId();

		ship.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

		if (ship.getParentStation() != null && (
				ship.getParentStation().getEngineController().isAccelerating()
				|| ship.getParentStation().getEngineController().isAcceleratingBackwards()
				)) {
			List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
			if (maneuveringThrusters != null) {
				for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
					if (Math.abs(Math.sin(Math.toRadians(e.getEngineSlot().getAngle()))) > 0.1) {
						// Nothing!!
					} else {
						if (ship.getParentStation().getEngineController().isAccelerating()) {
							ship.getEngineController().extendFlame(e.getEngineSlot(), 1.0f, 1.0f, 0.0f);
						} else {
							ship.getEngineController().extendFlame(e.getEngineSlot(), 0.0f, 0.0f, 1.0f);
						}

					}
				}
			}
		}
		if (ship.getParentStation() != null && (
				ship.getParentStation().getFluxTracker().isOverloadedOrVenting()
				)) {
			// EMP weapons



			NA_OverloadWeaponsArcTimer arctimer = (NA_OverloadWeaponsArcTimer) ship.getCustomData().get(key);
			if (arctimer == null) {
				arctimer = new NA_OverloadWeaponsArcTimer();
				ship.setCustomData(key, arctimer);
			}
			if (arctimer.remainingCount > 0) {
				arctimer.interval.advance(amount);
			}
			if (arctimer.interval.intervalElapsed()) {
				arctimer.reset();
				for (WeaponAPI w: ship.getUsableWeapons()) {
					w.setForceNoFireOneFrame(true);
					engine.spawnEmpArc(ship,
							w.getLocation(),
							ship,
							ship,
							DamageType.ENERGY,
							0,
							0, // emp
							1000f, // max range
							null, //"tachyon_lance_emp_impact",
							20f, // thickness
							ship.getOverloadColor(),
							new Color(255, 255, 255, 255)
					);
				}

			}
		} else if (ship.getCustomData().get(key) != null) {
			ship.removeCustomData(key);
		}
	}
}
