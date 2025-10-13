package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.AfterburnerStats;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.FastTrig;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class NA_EngineRCS extends BaseHullMod {
	private String ID = "NA_EngineRCS";
	private static float RATE = 0.8f;
	private static float ANG_RATE = 0.5f;

	private final String SHIP_ECHO = "na_echo";
	private final String SHIP_ECHOL = "na_echo_leftmodule";
	private final String SHIP_ECHOR = "na_echo_rightmodule";
	private final String DECO_ECHOL = "na_echo_leftwing";
	private final String DECO_ECHOR = "na_echo_rightwing";


	public static Vector2f ZERO = new Vector2f(0f, 0f);

	protected class NA_AfterburnerStatsInfo {
		float LastAngVel = 0f;
		float Value_Smooth = 0f;
		NA_AfterburnerStatsInfo (float f) {
			LastAngVel = f;
			Value_Smooth = f;
		}
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
	}

	public static class NA_OverloadWeaponsArcTimer {

	}

	private boolean loaded = false;
	public static final String INNER_LARGE = "graphics/fx/na_shields.png";
	public static final String OUTER_LARGE = "graphics/fx/na_shields_ring.png";

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);



		if (!ship.isAlive()) return;
		if (ship.getShield() != null) {
			if (!loaded) {
				loaded = true;
				// borrowed from LOST_SECTOR
				try {
					Global.getSettings().loadTexture(INNER_LARGE);
				} catch (IOException ex) {
					throw new RuntimeException("Failed to load sprite '" + INNER_LARGE + "'!", ex);
				}
				try {
					Global.getSettings().loadTexture(OUTER_LARGE);
				} catch (IOException ex) {
					throw new RuntimeException("Failed to load sprite '" + OUTER_LARGE + "'!", ex);
				}
			}
			ship.getShield().setRadius(ship.getShieldRadiusEvenIfNoShield(), INNER_LARGE, OUTER_LARGE);
		}

		if (ship instanceof ShipAPI) {

			ShipHullSpecAPI spec = ship.getHullSpec().getBaseHull();
			if (spec == null) spec = ship.getHullSpec();

			switch (spec.getHullId()) {
				case SHIP_ECHO:
					// check if modules exist
					boolean removeLeft = true;
					boolean removeRight = true;
					List<ShipAPI> modules = ship.getChildModulesCopy();
					for (ShipAPI module : modules) {
						ShipHullSpecAPI specm = module.getHullSpec().getBaseHull();
						if (specm == null) specm = module.getHullSpec();
						if (module.isAlive()) {
							switch (specm.getHullId()) {
								case SHIP_ECHOL:
									removeLeft = false;
									break;
								case SHIP_ECHOR:
									removeRight = false;
									break;
							}
						}
					}

					for (WeaponAPI wpn : ship.getAllWeapons()) {
						if (wpn.getSpec().getWeaponId().contentEquals(DECO_ECHOL)) {
							if (removeLeft) wpn.getAnimation().setFrame(1);
							else wpn.getAnimation().setFrame(0);
						} else if (wpn.getSpec().getWeaponId().contentEquals(DECO_ECHOR)) {
							if (removeRight) wpn.getAnimation().setFrame(1);
							else wpn.getAnimation().setFrame(0);
						}
					}

					break;
			}

			ship.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

			List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
			if (maneuveringThrusters != null && !ship.getEngineController().isFlamedOut() && !ship.getEngineController().isFlamingOut()) {
				float angVel = ship.getAngularVelocity();
				float lastAngVel = angVel;
				if (ship.getCustomData().get("NA_AfterburnerStatsInfo") != null && ship.getCustomData().get("NA_AfterburnerStatsInfo") instanceof NA_AfterburnerStatsInfo) {
					lastAngVel = ((NA_AfterburnerStatsInfo) ship.getCustomData().get("NA_AfterburnerStatsInfo")).LastAngVel;
					((NA_AfterburnerStatsInfo) ship.getCustomData().get("NA_AfterburnerStatsInfo")).LastAngVel = angVel;
				} else {
					ship.setCustomData("NA_AfterburnerStatsInfo", new NA_AfterburnerStatsInfo(angVel));
				}
				float smol = 1.8f* ship.getMutableStats().getTurnAcceleration().getModifiedValue() / (1f + ship.getMutableStats().getMaxTurnRate().getModifiedValue());
				float smold = 2.2f* ship.getMutableStats().getTurnAcceleration().getModifiedValue() / (1f + ship.getMutableStats().getMaxTurnRate().getModifiedValue());
				float Agility_scaling = Math.max(1f, (80f * (float)Math.sqrt(ship.getMutableStats().getMaxTurnRate().getModifiedValue())) / Math.max(1f, ship.getMutableStats().getTurnAcceleration().getModifiedValue()));
				float changeInAngVel = 2.5f* Agility_scaling* Agility_scaling * ANG_RATE*(angVel - lastAngVel);
				float changeInAngVelOrig = changeInAngVel;
				changeInAngVel += Math.signum(angVel) * Math.min(24f * smold, Math.max(0f, 50f * smold - 10f* Math.abs(angVel)));

				if (changeInAngVel <= 0 && angVel < 1f) changeInAngVel = Math.min(angVel, changeInAngVel);
				else if (changeInAngVel >= 0 && angVel > smol) changeInAngVel = Math.max(angVel, changeInAngVel);
				else if (changeInAngVel <= 0 && angVel > changeInAngVel) changeInAngVel = Math.min(angVel, changeInAngVel);
				else if (changeInAngVel >= 0 && angVel < changeInAngVel) changeInAngVel = Math.max(angVel, changeInAngVel);
				else if (Math.abs(changeInAngVel) < smol*20f) changeInAngVel = Math.abs(angVel) > smol ? -angVel : 0;

 				float angVel_smooth = ((NA_AfterburnerStatsInfo) ship.getCustomData().get("NA_AfterburnerStatsInfo")).Value_Smooth;

				/*if (Math.abs(changeInAngVel - angVel_smooth) < 0.7f*RATE * amount) angVel_smooth = 0;
				else if (changeInAngVel > 0 || (changeInAngVel == 0 && angVel == 0 && angVel_smooth < 0))
					angVel_smooth += ((angVel_smooth < 0 || Math.abs(changeInAngVel) < 0.01 || changeInAngVelOrig > 2f / Agility_scaling) ? 3f * RATE * amount : 0.7f * RATE * amount);
				else if (changeInAngVel < 0 || (changeInAngVel == 0 && angVel == 0 && angVel_smooth > 0))
					angVel_smooth -= ((angVel_smooth > 0 || Math.abs(changeInAngVel) < 0.01 || changeInAngVelOrig < -2f / Agility_scaling) ? 3f * RATE * amount : 0.7f * RATE * amount);*/
				if ((Math.abs(changeInAngVelOrig) < smol + 1f))
					angVel_smooth = 2f/16f * (15f * angVel_smooth/2f + Math.signum(changeInAngVel) * Math.min(1f, Math.max(Math.min(105f, Math.abs(changeInAngVel))/100f - 0.4f, 0f)));
				else if ((angVel_smooth < .8 && changeInAngVel > smol) || (angVel_smooth > -.8 && changeInAngVel < -smol))
					angVel_smooth = 2f/10f * (9f * angVel_smooth/2f + Math.signum(changeInAngVel) * Math.min(1f, Math.max(Math.min(150f, Math.abs(changeInAngVel))/100f - 0.4f, 0f)));
				else
					angVel_smooth = 2f/21f * (20f * angVel_smooth/2f + Math.signum(changeInAngVel) * Math.min(1f, Math.max(Math.min(105f, Math.abs(changeInAngVel))/100f - 0.4f, 0f)));


				float maxx = (0.8f + 0.4f * Math.min(1f, Math.abs(angVel)/ship.getMutableStats().getMaxTurnRate().getModifiedValue()));
				if (angVel_smooth > 2f) angVel_smooth = 2f;
				else if (angVel_smooth < -2f) angVel_smooth = -2f;
				((NA_AfterburnerStatsInfo) ship.getCustomData().get("NA_AfterburnerStatsInfo")).Value_Smooth = angVel_smooth;

				for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
					if ((Math.abs(Math.sin(Math.toRadians(e.getEngineSlot().getAngle()))) > 0.1
						&& e.getEngineSlot().getLength() < 48)
						|| Math.cos(Math.toRadians(e.getEngineSlot().getAngle())) > 0) {
						float cross = VectorUtils.getCrossProduct(
								new Vector2f((float) Math.cos(Math.toRadians(e.getEngineSlot().getAngle())),
										(float) Math.sin(Math.toRadians(e.getEngineSlot().getAngle()))),
								e.getEngineSlot().computePosition(Misc.ZERO, 0f)
						);

						ship.getEngineController().setFlameLevel(e.getEngineSlot(), maxx
								*(float) (Math.max(0f, Math.min(1f, Math.signum(cross) *
								Math.signum(angVel_smooth) * Math.max(0, 1.2f * Math.abs(angVel_smooth)- 0.27f)))));
					}
				}
			}
		}
	}
}
