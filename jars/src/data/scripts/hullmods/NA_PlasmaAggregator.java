package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NA_PlasmaAggregator extends BaseHullMod {
	public static final float ROF_BOOST = 0.25f;
	public static final float FLUX_RED = 20f;

	private String ID = "NA_PlasmaAgg";

	public static final Color GLOW = new Color(10, 208, 97,155);
	private final HashMap<WeaponAPI, Boolean> fluxRefunded = new HashMap<>();
	private final List<WeaponAPI> beams = new ArrayList<>();
	private boolean inited = false;

	private IntervalUtil particleTimer = new IntervalUtil(0.2f, 0.2f);

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return Math.round(100*ROF_BOOST) + "%";
		if (index == 1) return FLUX_RED + "%";
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

		// Reduce CD of synergy
		for (WeaponAPI w: getSynergy(ship)) {
			if (w.getCooldownRemaining() > 0) {
				w.setRemainingCooldownTo(Math.max(0, w.getCooldownRemaining()-amount * ROF_BOOST));
			}
			if (w.getAmmoTracker() != null && w.getAmmoTracker().getReloadProgress() > 0) {
				w.getAmmoTracker().setReloadProgress(w.getAmmoTracker().getReloadProgress()+amount * ROF_BOOST);
			}
		}


		// Code based on Knights of Ludd, thanks selkie and co.
		float dissipationBuff = 0f;
		float flatFluxRefund = 0f;


		boolean firing = false;
		for(WeaponAPI weapon : beams){
			if(weapon.isFiring()) {
				dissipationBuff += weapon.getFluxCostToFire() * (100f - FLUX_RED)/100f;
				firing = true;
				if (particleTimer.intervalElapsed()) {
					Global.getCombatEngine().addSmoothParticle(
							weapon.getFirePoint(0),
							ship.getVelocity(),
							weapon.getSize() == WeaponAPI.WeaponSize.LARGE ? 84f :
									(weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM ? 50f :
									25f),
							0.8f, 0.5f, 1.5f,
							new Color(47, 250, 114, 150)
					);
				}
			}
		}
		for(WeaponAPI weapon : fluxRefunded.keySet()){
			if(weapon.isFiring()){
				if(!fluxRefunded.get(weapon)){
					fluxRefunded.put(weapon, true);
					firing = true;
					flatFluxRefund += weapon.getFluxCostToFire() * (100f - FLUX_RED)/100f;
					if (particleTimer.intervalElapsed()) {
						Global.getCombatEngine().addSmoothParticle(
								weapon.getFirePoint(0),
								ship.getVelocity(),
								weapon.getSize() == WeaponAPI.WeaponSize.LARGE ? 84f :
										(weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM ? 50f :
												25f),
								0.8f, 0.5f, 1.5f,
								new Color(47, 250, 114, 150)
						);
					}
				}
			} else if(!weapon.isInBurst()){
				fluxRefunded.put(weapon, false);
			}
		}
		float maxFluxRefund = ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getHardFlux();
		ship.getFluxTracker().decreaseFlux(Math.min(maxFluxRefund, flatFluxRefund));
		ship.getMutableStats().getFluxDissipation().modifyFlat(ID, dissipationBuff);

		//ship.getMutableStats().getMissileRoFMult().modifyMult(ID, ROF_PENALTY);


		if (particleTimer.intervalElapsed()) {
			if (firing)
				particleTimer = new IntervalUtil(0.2f, 0.2f);
		} else particleTimer.advance(amount);
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
					|| weapon.getType() == WeaponAPI.WeaponType.ENERGY);
	}




	private void init(ShipAPI ship){
		if (inited) return;
		for(WeaponAPI weapon : getEnergyInSynergy(ship)){
			if(!weapon.isDecorative()){
				if (weapon.isBeam() && !weapon.isBurstBeam()){
					beams.add(weapon);
				}
				else{
					fluxRefunded.put(weapon, true);
				}
			}
		}
		inited = true;
	}
}
