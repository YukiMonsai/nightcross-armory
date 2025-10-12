package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.threat.EnergyLashActivatedSystem;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.impl.combat.threat.ThreatSwarmAI;
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FindShipFilter;
import data.scripts.stardust.NA_StargazerStardust;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static data.scripts.stardust.NA_StargazerHull.STARGAZER_RED;

public class NA_StardustTransfer extends BaseShipSystemScript {
	
	public static float MAX_LASH_RANGE = 1700f;

	public static float FLUX_PER_DUST = 200;
	public static float FLUX_PER_DUST_HF_MOD = 0.5f;
	public static float MAX_DUST = 10;
	public static float MIN_ORBS = 5;


	public static class DelayedCombatActionPlugin extends BaseEveryFrameCombatPlugin {
		float elapsed = 0f;
		float delay;
		Runnable r;
		
		public DelayedCombatActionPlugin(float delay, Runnable r) {
			this.delay = delay;
			this.r = r;
		}
			
		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			if (Global.getCombatEngine().isPaused()) return;
		
			elapsed += amount;
			if (elapsed < delay) return;
			
			r.run();
	
			CombatEngineAPI engine = Global.getCombatEngine();
			engine.removePlugin(this);
		}
	}
	

	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		//boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			//player = ship == Global.getCombatEngine().getPlayerShip();
		} else {
			return;
		}


		if (state == State.IN || state == State.OUT) {
			float jitterLevel = effectLevel;

			float maxRangeBonus = 150f;
			//float jitterRangeBonus = jitterLevel * maxRangeBonus;
			float jitterRangeBonus = (1f - effectLevel * effectLevel) * maxRangeBonus;
			
			float brightness = 0f;
			float threshold = 0.1f;
			if (effectLevel < threshold) {
				brightness = effectLevel / threshold;
			} else {
				brightness = 1f - (effectLevel - threshold) / (1f - threshold);
			}
			if (brightness < 0) brightness = 0;
			if (brightness > 1) brightness = 1;
			if (state == State.OUT) {
				jitterRangeBonus = 0f;
				brightness = effectLevel * effectLevel;
			}
			Color color = STARGAZER_RED;
			//color = VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR_BRIGHT;
			//ship.setJitterUnder(this, color, jitterLevel, 21, 0f, 3f + jitterRangeBonus);
			//ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus * 0.67f);
			//ship.setJitter(this, color, jitterLevel, 1, 0f, 3f);
			ship.setJitter(this, color, jitterLevel, 5, 0f, 3f + jitterRangeBonus);
		}
		
		if (effectLevel == 1) {
			ShipAPI target = findTarget(ship);

			if (target != null) {
				CombatEngineAPI engine = Global.getCombatEngine();

				EmpArcParams params = new EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 500f;
				params.minFadeOutMult = 2f;
				params.flickerRateMult = 0.7f;

				params.flickerRateMult = 0.3f;
				Vector2f loc = MathUtils.getPointOnCircumference(ship.getLocation(),
						ship.getCollisionRadius() * 0.5f, VectorUtils.getAngle(ship.getLocation(), target.getLocation()));

				Color color = VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR;
				if (ThreatSwarmAI.isAttackSwarm(target)) {
					color = VoltaicDischargeOnFireEffect.PHASE_FRINGE_COLOR;
				}
				float emp = 0;
				float dam = 0;
				EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArcPierceShields(ship, loc, ship, target,
						DamageType.ENERGY,
						dam,
						emp, // emp
						100000f, // max range
						"energy_lash_friendly_impact",
						100f, // thickness
						//new Color(100,165,255,255),
						color,
						new Color(255,255,255,255),
						params
				);
				arc.setTargetToShipCenter(loc, target);
				arc.setCoreWidthOverride(50f);

				arc.setSingleFlickerMode(true);
				
				//params.movementDurMax = 0.1f;
//				params.movementDurMin = 0.25f;
//				params.movementDurMax = 0.25f;
				
				
				if (ship.getOwner() == target.getOwner()) {
					//params.flickerRateMult = 0.6f;

					//arc.setFadedOutAtStart(true);
					Global.getSoundPlayer().playSound("energy_lash_fire", 1f, 1f, ship.getLocation(), ship.getVelocity());
				} else {
					// skill issue
				}
				
				applyEffectToTarget(ship, target);
			}
		}
	}
	
	
	protected void hitWithFriendshipLash(ShipAPI ship, ShipAPI target) {
		NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
		NA_StargazerStardust swarm2 = NA_StargazerStardust.getSwarmFor(target);
		float orbsavailable = Math.min(MAX_DUST, swarm.getNumActiveMembers());


		if (orbsavailable > 0) {
			float orbsToUse = 0;
			float fluxAfter = target.getCurrFlux();
			float hfafter = target.getFluxTracker().getHardFlux();

			// if target has a swarm then we always send max orbs, otherwise we only consume enough to bring their flux to 0
			for (int i = 0; i < orbsavailable; i++) {
				if (i < orbsavailable && fluxAfter > 0) {
					orbsToUse++;
					float softamount = fluxAfter - hfafter;
					softamount -= FLUX_PER_DUST;
					if (softamount < 0) {
						softamount *= FLUX_PER_DUST_HF_MOD;
						fluxAfter = hfafter + softamount;
						hfafter = fluxAfter;
					} else {
						fluxAfter -= FLUX_PER_DUST;
					}
				} else break;
			}

			if (swarm2 != null) {
				orbsToUse = Math.min(MAX_DUST, swarm.getNumActiveMembers());
			}

			CombatEngineAPI engine = Global.getCombatEngine();
			EmpArcParams params = new EmpArcParams();
			params.segmentLengthMult = 8f;
			params.zigZagReductionFactor = 0.25f;
			params.fadeOutDist = 500f;
			params.minFadeOutMult = 2f;
			params.flickerRateMult = 0.45f;


			for (int i = 0; i < orbsToUse; i++) {
				NA_StargazerStardust.SwarmMember fragment = swarm.getPicker(true, true).pick();
				if (fragment != null) {

					Vector2f loc = MathUtils.getPointOnCircumference(target.getLocation(),
							target.getCollisionRadius() * 0.5f, VectorUtils.getAngle(target.getLocation(), ship.getLocation()));

					float emp = 0;
					float dam = 0;
					EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArcVisual(fragment.loc, ship, loc, target,
							8f, // thickness
							//new Color(100,165,255,255),
							STARGAZER_RED,
							new Color(242, 154, 255,255),
							params
					);
					arc.setCoreWidthOverride(6f);
					arc.setSingleFlickerMode(true);

					if (swarm2 != null) {
						swarm.removeMember(fragment);
						swarm2.addMember(fragment);
					}
				}
			}
			if (Global.getCombatEngine().isUIShowingHUD() && target.getOwner() == 0) {
				int amt = (int) Math.max(0, target.getFluxTracker().getCurrFlux() - fluxAfter);
				engine.addFloatingDamageText(target.getLocation(), amt, new Color(218, 95, 255, 255), target, ship);
			}
			target.getFluxTracker().setCurrFlux(Math.max(fluxAfter, 0));

		}
	}
	
	protected void applyEffectToTarget(ShipAPI ship, ShipAPI target) {
		if (target == null || target.getSystem() == null || target.isHulk()) return;
		if (ship == null || ship.getSystem() == null || ship.isHulk()) return;
		if (NA_StargazerStardust.getSwarmFor(ship) == null) return;
		
		if (ship.getOwner() == target.getOwner()) {
			hitWithFriendshipLash(ship, target);
		} else {
			// just a skill issue bro
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
	
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;

		NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
		if (swarm == null || swarm.getNumActiveMembers() < MIN_ORBS) return "NOT ENOUGH STARDUST";
		
		ShipAPI target = findTarget(ship);
		if (target != null && target != ship) {
			return "READY";
		}
		if ((target == null || target == ship) && ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";
	}

	public boolean isInRange(ShipAPI ship, ShipAPI target) {
		float range = getRange(ship);
		float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
		float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
		return dist <= range + radSum;
	}
	
	public boolean isValidLashTarget(ShipAPI ship, ShipAPI other) {
		if (other == null) return false;
		if (other.isHulk() || other.getOwner() == 100) return false;
		if (ship.getOwner() != other.getOwner()) return false;
		if (other.isShuttlePod()) return false;
		if (other.hasTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)) return false;

		NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
		if (swarm == null || swarm.getNumActiveMembers() < MIN_ORBS) return false;
		
		if (other.isFighter()) return false;
		if (other.getOwner() == ship.getOwner()) {
			if (other.getFluxLevel() == 0) {

				NA_StargazerStardust swarm2 = NA_StargazerStardust.getSwarmFor(other);
				if (swarm2 == null) return false;

			}
		}
		return true;
		//return !other.isFighter();
	}
	
	
	protected ShipAPI findTarget(ShipAPI ship) {
		float range = getRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();

		if (!isValidLashTarget(ship, target)) target = null;

		float extraRange = 0f;
		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.CUSTOM1)){
			target = (ShipAPI) ship.getAIFlags().getCustom(AIFlags.CUSTOM1);
			extraRange += 500f;
		}
		
		
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum + extraRange) target = null;
		} else {
			FindShipFilter filter = s -> isValidLashTarget(ship, s);
			
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipTo(ship, ship.getMouseTarget(), HullSize.FRIGATE, range, true, false, filter);
				} else {
					Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum) target = null;
					}
				}
			}
			if (target == null) {
				target = Misc.findClosestShipTo(ship, ship.getLocation(), HullSize.FRIGATE, range, true, false, filter);
			}
		}
		
		return target;
	}
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI target = findTarget(ship);
		return target != null && target != ship;
		//return super.isUsable(system, ship);
	}
	
	public static float getRange(ShipAPI ship) {
		if (ship == null) return MAX_LASH_RANGE;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_LASH_RANGE);
	}
	
}








