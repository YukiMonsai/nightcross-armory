package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NA_HardlightMatrix extends BaseHullMod {
	private static Map mag = new HashMap();
	static {
		mag.put(HullSize.FIGHTER, 50f);
		mag.put(HullSize.FRIGATE, 100f);
		mag.put(HullSize.DESTROYER, 150f);
		mag.put(HullSize.CRUISER, 250f);
		mag.put(HullSize.CAPITAL_SHIP, 350f);
	}

	private String ID = "NA_HardlightMatrix";

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + ((Float) mag.get(HullSize.FRIGATE));
		if (index == 1) return "" + ((Float) mag.get(HullSize.DESTROYER));
		if (index == 2) return "" + ((Float) mag.get(HullSize.CRUISER));
		if (index == 3) return "" + ((Float) mag.get(HullSize.CAPITAL_SHIP));
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
		if (ship.getFluxTracker().isOverloadedOrVenting()) return;

		float armorBonus = 0;
		float bonuses = 0;

		List<ShipAPI> modules = ship.getChildModulesCopy();
		for (ShipAPI module : modules) {
			if (module.isAlive()) {
				if (module.getHullSpec().hasTag("hardlight_generator")) {
					List<WeaponAPI> weapons = module.getAllWeapons();
					if (weapons.size() == 0 || !weapons.get(0).isDisabled()) {
						armorBonus += (float) mag.get(ship.getHullSize());
						bonuses += 1;
						if (weapons.size() > 0) {
							weapons.get(0).setForceFireOneFrame(true);
						}
					}

				}
			}
		}

		if (armorBonus > 0) {
			ship.getMutableStats().getEffectiveArmorBonus().modifyFlat(ID, armorBonus);

			if (Global.getCombatEngine().getPlayerShip().getId().equals(ship.getId())) {
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"na_hardlightmatrix",
						"graphics/icons/hullsys/high_energy_focus.png",
						"Hardlight Matrix",
						"Current effective armor boost: " + ((int) (armorBonus)),
						false);
			}

			ship.setJitterUnder(
					ship, new Color(140, 175, 255), bonuses > 1 ? 0.8f : 0.4f, 3, 15f
			);

		} else {

			if (Global.getCombatEngine().getPlayerShip().getId().equals(ship.getId())) {
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"na_hardlightmatrix",
						"graphics/icons/hullsys/high_energy_focus.png",
						"Hardlight Matrix",
						"Matrix offline",
						true);
			}

			ship.getMutableStats().getEffectiveArmorBonus().unmodify(ID);
		}

	}
}
