package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.campaign.plugins.NAUtils;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.List;

public class NA_ShieldStabilizer extends BaseHullMod {

	private String ID = "NA_ShieldStabilizer";

	public final float AMT_PER_SHOCK = 100f;
	public final float TIME_PER_SHOCK = 0.75f;
	public final float RANGE_PER_SHOCK = 350f;

	public String getDescriptionParam(int index, HullSize hullSize) {
		//if (index == 0) return "" + ((Float) mag.get(hullSize));
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getMissileRoFMult().modifyMult(ID, ROF_PENALTY);
	}

	class ShieldStabilizerData {
		String id;
		IntervalUtil beamTime = new IntervalUtil(TIME_PER_SHOCK, TIME_PER_SHOCK);
		ShieldStabilizerData(ShipAPI ship) {
			id = ID + ship.getId();
		}
	}

	public ShieldStabilizerData getData(ShipAPI ship) {
		if (Global.getCombatEngine() != null) {
			if (!Global.getCombatEngine().getCustomData().containsKey(ID + ship.getId())) {
				return (ShieldStabilizerData) Global.getCombatEngine().getCustomData().put(ID + ship.getId(),
						new ShieldStabilizerData(ship));
			}
			return (ShieldStabilizerData) Global.getCombatEngine().getCustomData().get(ID + ship.getId());
		}
		return null;
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;
		if (ship.getFluxTracker().isOverloadedOrVenting()) {
			return;
		}

		ShieldStabilizerData data = getData(ship);
		if (data == null) return;
		if (data.beamTime.intervalElapsed()) {
			data.beamTime.randomize();

			if (ship.getFluxTracker().getMaxFlux() - ship.getFluxTracker().getCurrFlux() > AMT_PER_SHOCK) {
				List<ShipAPI> allies = NAUtils.getShipsWithinRange(ship.getLocation(), RANGE_PER_SHOCK);

				ShipAPI closest = null;
				float weight = -1000000000f;
				for (ShipAPI s : allies) {
					if (s.isAlive() && s.getOwner() == ship.getOwner() && !s.isPhased() && s.getHullSize() != HullSize.FIGHTER
							&& s.getFluxTracker().getHardFlux() > 0) {

						float dist = MathUtils.getDistance(ship.getLocation(), s.getLocation());
						float w = s.getMaxHitpoints()*s.getFluxTracker().getHardFlux()/s.getFluxTracker().getMaxFlux() - dist;
						if (w > weight) {
							closest = s;
							weight = w;
						}
					}
				}

				if (closest != null) {
					float dist = MathUtils.getDistance(ship, closest);
					if (dist < RANGE_PER_SHOCK) {
						// zap
						float amount_to_zap = Math.min(AMT_PER_SHOCK, closest.getFluxTracker().getHardFlux());
						closest.getFluxTracker().setHardFlux(
								Math.max(0, closest.getFluxTracker().getHardFlux() - amount_to_zap)
						);
						closest.getFluxTracker().setCurrFlux(
								Math.min(closest.getFluxTracker().getMaxFlux(), closest.getFluxTracker().getCurrFlux() + amount_to_zap)
						);
						ship.getFluxTracker().setHardFlux(
								Math.min(ship.getFluxTracker().getMaxFlux(), ship.getFluxTracker().getHardFlux() + amount_to_zap)
						);
						Global.getCombatEngine().spawnEmpArc(ship,
								MathUtils.getRandomPointInCircle(closest.getLocation(), closest.getCollisionRadius()*0.5f),
								ship,
								ship,
								DamageType.ENERGY,
								0,
								0, // emp
								RANGE_PER_SHOCK*2, // max range
								"tachyon_lance_emp_impact",
								20f,
								new Color(80, 208, 131, 255),
								new Color(133, 238, 255, 255)
						);

						Global.getSoundPlayer().playSound("na_ionmatrix", 0.8f, 1.5f, closest.getLocation(), closest.getVelocity());
					}
				}
			}
		} else data.beamTime.advance(amount);




	}

}
