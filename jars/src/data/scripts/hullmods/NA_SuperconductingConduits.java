package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NA_SuperconductingConduits extends BaseHullMod {
	public static final float ROF_BOOST = 0.6f;
	public static final float MAX_PEN = 2f;
	public static final float MAX_PEN_MAX_FACTOR = 2f;

	private String ID = "NA_SuperconductingConduits";

	public static final Color GLOW = new Color(10, 208, 97,155);
	private boolean inited = false;


	ShipAPI thisship = null;


	private static Map maxop = new HashMap();
	static {
		maxop.put(HullSize.FIGHTER, 4f);
		maxop.put(HullSize.FRIGATE, 10f);
		maxop.put(HullSize.DESTROYER, 20f);
		maxop.put(HullSize.CRUISER, 40f);
		maxop.put(HullSize.CAPITAL_SHIP, 80f);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return Math.round(100*ROF_BOOST) + "%";
		if (index == 1) return (int)Math.round((float) maxop.get(HullSize.FRIGATE)) + "";
		if (index == 2) return (int)Math.round((float) maxop.get(HullSize.DESTROYER)) + "";
		if (index == 3) return (int)Math.round((float) maxop.get(HullSize.CRUISER)) + "";
		if (index == 4) return (int)Math.round((float) maxop.get(HullSize.CAPITAL_SHIP)) + "";


		float rof = GetRof(thisship, false);
		if (rof == 0) return "";
		if (index == 5) return "\n\nCurrent Bonus: " + (int) (100 * (GetRof(thisship, true))) + "%";
		return null;
	}

	private static class RofCacheData {
		public float rof_cached = 0;
	}

	private float GetRof(CombatEntityAPI entity, boolean forceUpdate) {

		if (entity == null && !forceUpdate) return 0;
		RofCacheData data = null;
		if (entity != null) {
			data = entity.getCustomData().containsKey(ID + "data") ? (RofCacheData) entity.getCustomData().get(ID + "data") : null;
			if (data == null) {
				entity.setCustomData(ID + "data", new RofCacheData());
				data = (RofCacheData) entity.getCustomData().get(ID + "data");
			}
		}
		if (data == null) data = new RofCacheData();
		if ((data != null && data.rof_cached == 0) || (forceUpdate && entity != null)) {
			float penalty = 0f;
			if (entity instanceof ShipAPI) {
				thisship = (ShipAPI) entity;
				ShipAPI ship = thisship;
				boolean sMod = isSMod(thisship.getMutableStats());
				float totalOP = 0;
				float maxOP = (float) maxop.get((thisship).getHullSpec().getHullSize());
				if (sMod) maxOP *= 2;

				for (WeaponAPI weapon : ship.getAllWeapons()) {
					if (weaponIsMissile(weapon) || weaponIsSynergy(weapon)) {
						totalOP += weapon.getSpec().getOrdnancePointCost(ship.getCaptain() != null ? ship.getCaptain().getStats() : null, ship.getMutableStats());
					}
				}
				totalOP -= maxOP;
				if (totalOP > 0) {
					penalty = MAX_PEN * Math.min(1f, totalOP/(maxOP * (MAX_PEN_MAX_FACTOR)));
				}
			}

			data.rof_cached = ROF_BOOST / (1 + penalty);
		}

		return data.rof_cached;
	}


	public String getSModDescriptionParam(int index, HullSize hullSize) {
		//if (index == 0) return "" + (int) SMOD_AMMO_BONUS + "%";
		if (index == 0) return "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, (Float) mag.get(hullSize));
		//stats.getMissileRoFMult().modifyMult(ID, ROF_PENALTY);
		GetRof(stats.getEntity(), true);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		super.applyEffectsAfterShipCreation(ship, id);
		GetRof(ship.getMutableStats().getEntity(), true);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;
		init(ship);

		boolean sMod = isSMod(ship.getMutableStats());
		float rof = GetRof(ship, sMod);
		// Reduce CD of synergy
		for (WeaponAPI w: getSynergy(ship)) {


			if (!w.isBeam() || w.isBurstBeam()) {
				if (w.getCooldownRemaining() > 0 && !w.isInBurst()) {
					w.setRemainingCooldownTo(Math.max(0.00000001f, w.getCooldownRemaining() - amount * rof));
				}
			}
			if (w.getAmmoTracker() != null && w.getAmmoTracker().getReloadProgress() > 0) {
				float ProgressPerSecond = w.getAmmoTracker().getAmmoPerSecond()/w.getAmmoTracker().getReloadSize();
				if (ProgressPerSecond > 0)
					w.getAmmoTracker().setReloadProgress(w.getAmmoTracker().getReloadProgress()+amount*ProgressPerSecond * rof);
			}
		}




	}


	public static List<WeaponAPI> getSynergy(ShipAPI carrier) {
		List<WeaponAPI> result = new ArrayList<WeaponAPI>();

		for (WeaponAPI weapon : carrier.getAllWeapons()) {
			if (
					weaponIsSynergy(weapon)
					|| weaponIsMissile(weapon)
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
	public static boolean weaponIsMissile(WeaponAPI weapon) {
		return weapon != null
				&& (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.MISSILE
				|| weapon.getSpec().getType() == WeaponAPI.WeaponType.MISSILE
				|| weapon.getType() == WeaponAPI.WeaponType.MISSILE);
	}




	private void init(ShipAPI ship){
		if (inited) return;
		inited = true;
	}
}
