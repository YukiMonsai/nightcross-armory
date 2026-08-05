package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.campaign.plugins.NAUtils;
import data.scripts.weapons.NA_AriaHit;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NA_GravityDrag extends BaseHullMod {
	public static final float RANGE = 1500f;
	public static final float TIMEFLOW = 10f;
	public static final float BEAMRESIST = 50f;


	private String ID = "NA_GravityDrag";


	private IntervalUtil applyTimer = new IntervalUtil(0.1f, 0.1f);

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return Math.round(RANGE) + "";
		if (index == 1) return TIMEFLOW + "%";
		if (index == 2) return BEAMRESIST + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;

		String key2 = NA_TidalGrid.ID + "2_" + ship.getId();

		NA_TidalGrid.NightcrossTargetingLevelData effectlevel = (NA_TidalGrid.NightcrossTargetingLevelData) ship.getCustomData().get(key2);

		if (effectlevel != null && effectlevel.level == 0) {
			ship.getMutableStats().getBeamDamageTakenMult().modifyPercent(ID, BEAMRESIST);
		} else {
			ship.getMutableStats().getBeamDamageTakenMult().unmodify(ID);
		}


		if (applyTimer.intervalElapsed()) {
			applyTimer.randomize();
			apply(ship, RANGE, ship.isPhased() ? TIMEFLOW * 2 : TIMEFLOW, ship.getSystem() != null && ship.getSystem().getId() == "na_massinflation" && ship.getSystem().isOn());
		} else applyTimer.advance(amount);
	}


	private static final float SCRAMBLE_DURATION = 0.15f;
	public static Color JITTER_UNDER_COLOR = new Color(46, 0, 250,155);
	public static Color TEXT_COLOR = new Color(146, 103, 255,255);


	public static class TargetData {
		public ShipAPI target;
		public EveryFrameCombatPlugin targetEffectPlugin;
		public float timeflowmult = TIMEFLOW;
		public float speedmult = 1.0f;
		public Object KEY_TARGET = new Object();
		public IntervalUtil elapsed = new IntervalUtil(SCRAMBLE_DURATION, SCRAMBLE_DURATION);
		public TargetData(ShipAPI ship, ShipAPI target) {
			this.target = target;
		}
	}
	
	public void apply(ShipAPI ship, float radius, float strength, boolean systemOn) {
		List<ShipAPI> enemies = NAUtils.getEnemyShipsWithinRange(ship, ship.getLocation(), radius, true);

		for (ShipAPI target : enemies) {
			float dist = MathUtils.getDistance(ship, target);
			float mult = Math.min(1, Math.max(0, 1f - dist/RANGE));

			if (mult < 0) continue;


			final String targetDataKey = target.getId() + "_gravitydrag_target_data";

			Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
			if (targetDataObj == null) {
				Global.getCombatEngine().getCustomData().put(targetDataKey, new TargetData(ship, target));
				if (target != null) {
					targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
					((TargetData) targetDataObj).target = target;
					((TargetData) targetDataObj).timeflowmult = strength * mult;

					/*if (target.getFluxTracker().showFloaty() ||
							ship == Global.getCombatEngine().getPlayerShip() ||
							target == Global.getCombatEngine().getPlayerShip()) {
						target.getFluxTracker().showOverloadFloatyIfNeeded("Gravity disrupted!", TEXT_COLOR, 4f, true);
					}*/
				}
			} else {
				((TargetData) targetDataObj).target = target;
				((TargetData) targetDataObj).elapsed.setElapsed(0f); // reset
				((TargetData) targetDataObj).timeflowmult = strength * mult;
			}

			if (targetDataObj == null || ((TargetData) targetDataObj).target == null) return;

			final TargetData targetData = (TargetData) targetDataObj;

			if (targetData.targetEffectPlugin == null) {
				targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
					@Override
					public void advance(float amount, List<InputEventAPI> events) {
						if (Global.getCombatEngine().isPaused()) return;
						if (targetData.target == Global.getCombatEngine().getPlayerShip()) {
							Global.getCombatEngine().maintainStatusForPlayerShip(targetData.KEY_TARGET,
									"graphics/icons/na_gravity.png",
									"Gravitational Drag",
									"-" + (int)(targetData.timeflowmult) + "% timeflow", true);
						}

						if (targetData.elapsed.intervalElapsed() || !targetData.target.isAlive()) {
							targetData.target.getMutableStats().getTimeMult().unmodify(targetDataKey);
							Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
							Global.getCombatEngine().getCustomData().remove(targetDataKey);
						} else {
							targetData.elapsed.advance(amount);
							targetData.target.getMutableStats().getTimeMult().modifyMult(targetDataKey, 1f-targetData.timeflowmult/100);

							if (targetData.target.getHullSize() != HullSize.FIGHTER) {
								targetData.target.setJitterShields(false);
								targetData.target.setJitterUnder(this, JITTER_UNDER_COLOR,
										0.25f * targetData.timeflowmult / TIMEFLOW * targetData.timeflowmult / TIMEFLOW,
										3, 5f, 10f);
							}
						}
					}
				};
				Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
			}
			
		}
	}
}
