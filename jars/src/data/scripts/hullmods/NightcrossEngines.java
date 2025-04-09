package data.scripts.hullmods;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;

public class NightcrossEngines extends BaseHullMod {

	public static final float DMG_MULT = 50f;
	public static final float ENG_REPAIR = -20f;
	public static final float SHIELD_RATE_MULT = 40f;
	public static final float ZFLUX_BOOST = 10f;
	public static final float HULL_THRESH = 50f;
	private String ID = "NightcrossEngines";
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEngineDamageTakenMult().modifyPercent(id, DMG_MULT);
		stats.getCombatEngineRepairTimeMult().modifyPercent(id, ENG_REPAIR);
		stats.getZeroFluxSpeedBoost().modifyFlat(id, ZFLUX_BOOST);

	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) ZFLUX_BOOST + "%";
		if (index == 1) return "" + (int) DMG_MULT + "%";
		if (index == 2) return "" + (int) HULL_THRESH + "%";
		if (index == 3) return "" + (int) SHIELD_RATE_MULT + "%";
		return null;
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		//ship.getFluxTracker().setHardFlux(ship.getFluxTracker().getCurrFlux());
//		if (ship.getEngineController().isAccelerating() ||
//				ship.getEngineController().isAcceleratingBackwards() ||
//				ship.getEngineController().isDecelerating() ||
//				ship.getEngineController().isTurningLeft() ||
//				ship.getEngineController().isTurningRight() ||
//				ship.getEngineController().isStrafingLeft() ||
//				ship.getEngineController().isStrafingRight()) {
//		ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
//		ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
//		}
		ShipAPI player = Global.getCombatEngine().getPlayerShip();
		float effectLevel = Math.max(0f, Math.min(1f, (100f/HULL_THRESH)*(1f-ship.getHullLevel())));
		if (effectLevel > 0.001) {
			ship.getMutableStats().getZeroFluxSpeedBoost().modifyPercent(ID, -100f*effectLevel);
			if(ship == player){
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"nightcrossengines",
						"graphics/icons/hullsys/infernium_injector.png",
						"Precision Drive Systems",
						"Zero-flux engine boost decreased by " + Math.round(100f * effectLevel)+ "% due to damage.",
						true);
			}
		} else {
			ship.getMutableStats().getZeroFluxSpeedBoost().unmodify(ID);
			if(ship == player){
				Global.getCombatEngine().maintainStatusForPlayerShip(
						"nightcrossengines",
						"graphics/icons/hullsys/infernium_injector.png",
						"Precision Drive Systems",
						"Engines operating at full capacity.",
						false);
			}
		}



	}



}
