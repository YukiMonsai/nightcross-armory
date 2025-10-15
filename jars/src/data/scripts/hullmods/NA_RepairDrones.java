package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.campaign.plugins.NAUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.magiclib.util.MagicFakeBeam;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NA_RepairDrones extends BaseHullMod {

	private String ID = "NA_RepairDrones";

	public String state = "idle";

	public final float MAX_REPAIR = 0.25f;
	public final float REPAIR_RATE = 100f; // per second
	public final float REPAIR_RANGE = 200f;



	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return Math.round(MAX_REPAIR * 100) + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getMissileRoFMult().modifyMult(ID, ROF_PENALTY);
	}

	public static List<ShipAPI> getFighters(ShipAPI carrier) {
		List<ShipAPI> result = new ArrayList<>();

		for (ShipAPI ship : Global.getCombatEngine().getShips()) {
			if (!ship.isFighter() || !ship.isAlive()) {
				continue;
			}
			if (ship.getWing() == null) {
				continue;
			}
			if (ship.getWing().getSourceShip() == carrier) {
				result.add(ship);
			}
		}

		return result;
	}

	class RepairDronesData {
		String id;
		float repaired = 0;
		IntervalUtil repairBeam = new IntervalUtil(0.25f, 0.5f);
		RepairDronesData(ShipAPI ship) {
			id = ID + ship.getId();
		}
	}

	public RepairDronesData getData(ShipAPI ship) {
		if (Global.getCombatEngine() != null) {
			if (!Global.getCombatEngine().getCustomData().containsKey(ID + ship.getId())) {
				return (RepairDronesData) Global.getCombatEngine().getCustomData().put(ID + ship.getId(),
						new RepairDronesData(ship));
			}
			return (RepairDronesData) Global.getCombatEngine().getCustomData().get(ID + ship.getId());
		}
		return null;
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;
		if (ship.getFluxTracker().isOverloadedOrVenting()) {
			if (Global.getCombatEngine().getPlayerShip().getId().equals(ship.getId())) {
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"na_repairdrones",
						"graphics/icons/hullsys/high_energy_focus.png",
						"Repair Coordinator",
						"ERROR: Please contact NCA customer support for assistance.",
						false);
			}
			return;
		}

		List<ShipAPI> fighters = getFighters(ship);
		int count = fighters.size();
		if (count > 0 && ship.getAllWings() != null && ship.getAllWings().size() > 0 && ship != Global.getCombatEngine().getPlayerShip()) {
			FighterWingAPI baseFighter = ship.getAllWings().get(0);
			float range = baseFighter.getRange();

			// Ally scan
			List<ShipAPI> allies = NAUtils.getShipsWithinRange(ship.getLocation(), range);
			ShipAPI closest = null;
			float closestdist = range * 2f;
			for (ShipAPI s : allies) {
				if (s != ship && s.isAlive() && s.getOwner() == ship.getOwner() && !s.isPhased() && s.getHullSize() != HullSize.FIGHTER) {
					float maxrepair = s.getMaxHitpoints() * MAX_REPAIR;
					RepairDronesData data = getData(s);
					if (data != null) {
						float repaired = data.repaired;
						if (s.getHitpoints() < s.getMaxHitpoints() && repaired < maxrepair) {
							float dist = MathUtils.getDistance(ship.getLocation(), s.getLocation());
							if (dist < closestdist) {
								closest = s;
								closestdist = dist;
							}
						}
					}

				}
			}
			// go to ships under fire preemptively
			if (closest == null) {
				for (ShipAPI s : allies) {
					if (s != ship && s.isAlive() && s.getOwner() == ship.getOwner() && !s.isPhased() && s.getHullSize() != HullSize.FIGHTER
						&& !s.getFluxTracker().isVenting()) {
						if (s.getFluxTracker().getHardFlux() > s.getFluxTracker().getMaxFlux()*0.1f) {
							float dist = MathUtils.getDistance(ship.getLocation(), s.getLocation());
							if (dist < closestdist) {
								closest = s;
								closestdist = dist;
							}
						}
					}
				}
			}

			// get closest
			if (closest != null && ship.getAIFlags() != null) {
				ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET, 10f, closest);

				// do the repair
				doRepairs(amount, ship, closest, fighters);
			}
		}

		if (Global.getCombatEngine().getPlayerShip().getId().equals(ship.getId())) {
			Global.getCombatEngine().maintainStatusForPlayerShip(
					"na_repairdrones",
					"graphics/icons/hullsys/high_energy_focus.png",
					"Repair Coordinator: " + state.toUpperCase(),
					count + " drones active",
					false);
		}

	}

	public void fighterRepairTarget(float amount, float rate, ShipAPI fighter, ShipAPI target) {
		WeaponAPI w = fighter.getAllWeapons().get(0);
		RepairDronesData data = getData(target);
		if (data != null && MathUtils.getDistance(fighter, target) < REPAIR_RANGE) {
			float amountToRepair = Math.min(rate * amount, Math.max(0,
					Math.min(target.getMaxHitpoints() - target.getHitpoints(),
							target.getMaxHitpoints() * MAX_REPAIR - data.repaired)
					));
			if (amountToRepair > 0) {
				target.setHitpoints(target.getHitpoints() + amountToRepair);
				if (data.repairBeam.intervalElapsed()) {
					MagicFakeBeam.spawnAdvancedFakeBeam(
							Global.getCombatEngine(),
							w.getFirePoint(0),
							REPAIR_RANGE*2, VectorUtils.getAngle(w.getFirePoint(0),
									target.getLocation()),
							4f, 10f, 0.1f,
							"base_trail_rough",
							"base_trail_aura",
							256f, /* textureLoopLength */
							16f, /* textureScrollSpeed */
							0.05f, 0.2f, 0.1f, 0.15f, 25f,
							new Color(75, 255, 125),
							new Color(14, 99, 186),
							0, DamageType.ENERGY, 0, fighter
					);
					data.repairBeam.randomize();
				} else {
					data.repairBeam.advance(amount);
				}

			}
		}

	}

	public void doRepairs(float amount, ShipAPI ship, ShipAPI target, List<ShipAPI> fighters) {
		List<ShipAPI> eligibleFighters = new ArrayList<>();
		for (ShipAPI f : fighters) {
			if (f.isAlive() && f.getAllWeapons().size() > 0 && MathUtils.getDistance(f, target) < REPAIR_RANGE) {
				WeaponAPI w = f.getAllWeapons().get(0);
				if (!w.isDisabled() && !w.isFiring() && !(w.getCooldownRemaining() > 0)) {
					eligibleFighters.add(f);
				}
			}
		}
		if (eligibleFighters.size() > 0) {
			for (ShipAPI f : fighters) {
				fighterRepairTarget(amount, REPAIR_RATE, f, target);
			}
		}
	}
}
