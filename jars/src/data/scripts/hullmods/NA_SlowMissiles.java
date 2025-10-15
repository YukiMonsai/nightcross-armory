package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

public class NA_SlowMissiles extends BaseHullMod {
	public static final float ROF_PENALTY = 0.65f;

	private String ID = "NA_SlowMissiles";

	private float rof_cached = 0;

	ShipAPI thisship = null;

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "35%";
		if (index == 1) return (int) (100 * (1 - GetRofPenalty(thisship, true))) + "%";
		return null;
	}

	private float GetRofPenalty(CombatEntityAPI entity, boolean forceUpdate) {
		if (rof_cached == 0 || forceUpdate) {
			float amount = 1f;
			if (entity instanceof ShipAPI) {
				thisship = (ShipAPI) entity;
				ShipAPI ship = thisship;
				float total_synergy = 0;
				float totalslots = 0;
				float total_weapons = 0;
				float total_emptymissiles = 0;
				float total_missilesinsynergy = 0;
				float total_synergyinmissile = 0;
				if (ship.getHullSpec() != null) {
					for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
						int size = 1;
						if (slot.getSlotSize() == WeaponAPI.WeaponSize.LARGE)
							size = 4;
						else if (slot.getSlotSize() == WeaponAPI.WeaponSize.MEDIUM)
							size = 2;
						if (slot.getWeaponType() == WeaponAPI.WeaponType.SYNERGY) {
							total_synergy += size;
						}
						if (slot.getWeaponType() == WeaponAPI.WeaponType.SYNERGY
							|| slot.getWeaponType() == WeaponAPI.WeaponType.MISSILE
							|| slot.getWeaponType() == WeaponAPI.WeaponType.COMPOSITE
							|| slot.getWeaponType() == WeaponAPI.WeaponType.UNIVERSAL) {
							totalslots += size;
						}
					}
				}
				if (total_synergy > 0) {
					for (WeaponAPI weapon : ship.getAllWeapons()) {
						int size = 1;
						if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE)
							size = 4;
						else if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM)
							size = 2;
						if (weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.SYNERGY
						|| weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.MISSILE
						|| weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.COMPOSITE
						|| weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.UNIVERSAL) {
							// Roundabout way of telling if empty
							total_weapons += size;
						}

						if (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.MISSILE
								&& weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.SYNERGY) {

							total_missilesinsynergy += size;
						} else if (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
								&& weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.MISSILE) {
							total_synergyinmissile += size;
						}
					}
					total_emptymissiles = Math.max(0, totalslots - total_weapons);
					amount = 1f - (Math.max(0, total_missilesinsynergy - total_synergyinmissile - total_emptymissiles) / total_synergy);
				}
			}

			rof_cached = (ROF_PENALTY + (1f - ROF_PENALTY) * amount);
		}

		return rof_cached;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		stats.getMissileRoFMult().modifyMult(ID, GetRofPenalty(stats.getEntity(), true));
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;

		ship.getMutableStats().getMissileRoFMult().modifyMult(ID, GetRofPenalty(ship, false));
	}
}
