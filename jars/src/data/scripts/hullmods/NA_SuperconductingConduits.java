package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NA_SuperconductingConduits extends BaseHullMod {
	public static final float ROF_BOOST = 0.2f;
	public static final float RECHARGE_BOOST = 0.25f;
	public static final float SMOD_BONUS = 100f;

	private String ID = "NA_SuperconductingConduits";

	public static final Color GLOW = new Color(10, 208, 97,155);
	private boolean inited = false;


	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return Math.round(100*ROF_BOOST) + "%";
		if (index == 1) return Math.round(100*RECHARGE_BOOST) + "%";
		return null;
	}


	public String getSModDescriptionParam(int index, HullSize hullSize) {
		//if (index == 0) return "" + (int) SMOD_AMMO_BONUS + "%";
		if (index == 0) return "" + (int) SMOD_BONUS + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getMissileRoFMult().modifyMult(ID, ROF_PENALTY);


	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;
		init(ship);

		boolean sMod = isSMod(ship.getMutableStats());
		// Reduce CD of synergy
		for (WeaponAPI w: getSynergy(ship)) {


			if (w.getCooldownRemaining() > 0) {
				w.setRemainingCooldownTo(Math.max(0, w.getCooldownRemaining()-amount * ROF_BOOST));
			}
			if (w.getAmmoTracker() != null && w.getAmmoTracker().getReloadProgress() > 0) {
				float mult = (sMod && w.getSlot() != null && w.getSlot().getWeaponType() != WeaponAPI.WeaponType.ENERGY) ? 2f : 1f;
				w.getAmmoTracker().setReloadProgress(w.getAmmoTracker().getReloadProgress()+amount * RECHARGE_BOOST * mult);
			}
		}




	}


	public static List<WeaponAPI> getSynergy(ShipAPI carrier) {
		List<WeaponAPI> result = new ArrayList<WeaponAPI>();

		for (WeaponAPI weapon : carrier.getAllWeapons()) {
			if (
					weaponIsSynergy(weapon)
			) {
				result.add(weapon);
			}
		}

		return result;
	}

	public static List<WeaponAPI> getEnergyInSynergy(ShipAPI carrier) {
		List<WeaponAPI> result = new ArrayList<WeaponAPI>();

		for (WeaponAPI weapon : carrier.getAllWeapons()) {
			if (
					weaponIsEnergyInSynergy(weapon)
			) {
				result.add(weapon);
			}
		}

		return result;
	}

	public static boolean weaponIsSynergy(WeaponAPI weapon) {
		return weapon != null
				&& (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
					|| weapon.getSpec().getType() == WeaponAPI.WeaponType.SYNERGY
					|| weapon.getType() == WeaponAPI.WeaponType.SYNERGY);
	}
	public static boolean weaponIsEnergyInSynergy(WeaponAPI weapon) {
		return weapon != null
				&& weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.SYNERGY
				&& (weapon.getSpec().getType() == WeaponAPI.WeaponType.ENERGY
					|| weapon.getType() == WeaponAPI.WeaponType.ENERGY)
				&& !(weapon.getSpec().getType() == WeaponAPI.WeaponType.SYNERGY
					|| weapon.getType() == WeaponAPI.WeaponType.SYNERGY
					|| weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY);
	}




	private void init(ShipAPI ship){
		if (inited) return;
		inited = true;
	}
}
