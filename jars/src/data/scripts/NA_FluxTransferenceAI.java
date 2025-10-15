package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.stardust.NA_StargazerStardust;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;


public class NA_FluxTransferenceAI implements ShipSystemAIScript {

	protected ShipAPI ship;
	protected CombatEngineAPI engine;
	protected ShipwideAIFlags flags;
	protected ShipSystemAPI system;
	protected NA_FluxTransference script;
	
	protected IntervalUtil tracker = new IntervalUtil(0.2f, 0.4f);
	
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.flags = flags;
		this.engine = engine;
		this.system = system;
		
		script = (NA_FluxTransference)system.getScript();
	}
	
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		tracker.advance(amount);
		
		if (tracker.intervalElapsed()) {
			if (system.getCooldownRemaining() > 0) return;
			if (system.isOutOfAmmo()) return;
			if (system.isActive()) return;
			if (ship.getFluxTracker().isOverloadedOrVenting()) return;
			if (!(ship.getShipAI() != null && ship.getShipAI().getAIFlags().hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) &&
					(ship.getFluxTracker().getMaxFlux() - ship.getFluxTracker().getCurrFlux() < NA_FluxTransference.FLUX_PER_DUST)) return;
			
			ShipAPI pick = getWeightedTargets(target).getItemWithHighestWeight();
			if (pick != null) {
				ship.getAIFlags().setFlag(AIFlags.CUSTOM1, 1.5f, pick);
				ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
				//System.out.println("Lash target: " + pick);
			}
		}
	}
	
	public List<ShipAPI> getPossibleTargets() {
		List<ShipAPI> result = new ArrayList<>();
		CombatEngineAPI engine = Global.getCombatEngine();
		
		List<ShipAPI> ships = engine.getShips();
		for (ShipAPI other : ships) {
			if (other == ship) continue;
			if (!script.isValidLashTarget(ship, other)) continue;
			if (!script.isInRange(ship, other)) continue;
			result.add(other);
		}
		return result;
	}
	
	public WeightedRandomPicker<ShipAPI> getWeightedTargets(ShipAPI shipTarget) {
		WeightedRandomPicker<ShipAPI> picker = new WeightedRandomPicker<>();
		
		for (ShipAPI other : getPossibleTargets()) {
			float w = 0f;
			if (ship.getOwner() == other.getOwner()) {
				if (other.getSystem() != null && other.getSystem().getId().equals(system.getId())) continue; // dont hugbox

				if (other.getFluxLevel() < 0.33f) continue;
				w = other.getFluxLevel();
				w = other.getHardFluxLevel();

			} else {
				// skill issue
			}
			if (w > 0.15f)
				picker.add(other, w);
		}
		return picker;
	}

}






















