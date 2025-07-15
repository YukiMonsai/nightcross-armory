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

public class NA_Helical extends BaseHullMod {

	private String ID = "NA_Helical";

	private boolean inited = false;


	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}


	public String getSModDescriptionParam(int index, HullSize hullSize) {
		//if (index == 0) return "" + (int) SMOD_AMMO_BONUS + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {


	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		ShipAPI player = Global.getCombatEngine().getPlayerShip();

		if (!ship.isAlive()) return;
		init(ship);

		if (ship.getSystem() != null && !ship.getSystem().isActive() && !ship.getSystem().isCoolingDown()) {
			ship.getMutableStats().getZeroFluxMinimumFluxLevel().modifyFlat(ID, 100f);
		} else {
			ship.getMutableStats().getZeroFluxMinimumFluxLevel().unmodify(ID);
		}

	}





	private void init(ShipAPI ship){
		if (inited) return;
		inited = true;
	}
}
