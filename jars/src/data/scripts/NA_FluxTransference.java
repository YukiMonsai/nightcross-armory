package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.impl.combat.threat.ThreatSwarmAI;
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FindShipFilter;
import data.scripts.stardust.NA_StargazerStardust;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

import static data.scripts.stardust.NA_StargazerHull.STARGAZER_RED;

public class NA_FluxTransference extends BaseShipSystemScript {
	
	public static float MAX_LASH_RANGE = 1550f;

	public static float FLUX_PER_DUST = 2000;
	public static float FLUX_PER_DUST_HF_MOD = 0.5f;
	public static float SELF_TRANSFER = 0.5f;
	public static float ARC_2_DELAY = 0.2f;

	public static Color COLOR_ARC_1 = new Color(125, 175, 255, 150);
	public static Color COLOR_ARC_2 = new Color(249, 169, 255, 226);

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
			//color = VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR_BRIGHT;
			//ship.setJitterUnder(this, color, jitterLevel, 21, 0f, 3f + jitterRangeBonus);
			//ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus * 0.67f);
			//ship.setJitter(this, color, jitterLevel, 1, 0f, 3f);
			ship.setJitter(this, COLOR_ARC_1, jitterLevel, 5, 0f, 3f + jitterRangeBonus);
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
				Vector2f tloc = MathUtils.getPointOnCircumference(target.getLocation(),
						target.getCollisionRadius() * 0.5f, VectorUtils.getAngle(target.getLocation(), ship.getLocation()));


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
						COLOR_ARC_1,
						new Color(255,255,255,255),
						params
				);
				arc.setTargetToShipCenter(loc, target);
				arc.setCoreWidthOverride(30f);

				arc.setSingleFlickerMode(true);

				final ShipAPI ship_final = ship;
				final ShipAPI target_final = ship;

				Global.getCombatEngine().addPlugin(new DelayedCombatActionPlugin(ARC_2_DELAY, new Runnable() {
					@Override
					public void run() {
						EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArcPierceShields(target_final, tloc, ship_final, ship_final,
								DamageType.ENERGY,
								dam,
								emp, // emp
								100000f, // max range
								"energy_lash_friendly_impact",
								100f, // thickness
								//new Color(100,165,255,255),
								COLOR_ARC_2,
								new Color(255,255,255,255),
								params
						);
						arc.setTargetToShipCenter(tloc, ship_final);
						arc.setCoreWidthOverride(50f);

						arc.setSingleFlickerMode(true);
					}
				}));
				
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
		float fluxAfter = target.getCurrFlux();
		float hfafter = target.getFluxTracker().getHardFlux();

		float softamount = fluxAfter - hfafter;
		softamount -= FLUX_PER_DUST;
		if (softamount < 0) {
			softamount *= FLUX_PER_DUST_HF_MOD;
			fluxAfter = hfafter + softamount;
			hfafter = fluxAfter;
		} else {
			fluxAfter -= FLUX_PER_DUST;
		}

		CombatEngineAPI engine = Global.getCombatEngine();

		int amt = (int) Math.max(0, target.getFluxTracker().getCurrFlux() - fluxAfter);
		if (engine.isUIShowingHUD() && target.getOwner() == 0) {
			engine.addFloatingDamageText(target.getLocation(), amt, new Color(218, 95, 255, 255), target, ship);
		}
		target.getFluxTracker().setCurrFlux(Math.max(fluxAfter, 0));
		target.getFluxTracker().setHardFlux(Math.max(hfafter, 0));

		if ((ship.getFluxTracker().getCurrFlux() + FLUX_PER_DUST) > ship.getMaxFlux()) {
			ship.getFluxTracker().setCurrFlux(ship.getMaxFlux());
			ship.getFluxTracker().beginOverloadWithTotalBaseDuration(2.5f);
		} else {
			ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() + FLUX_PER_DUST);
		}

	}
	
	protected void applyEffectToTarget(ShipAPI ship, ShipAPI target) {
		if (target == null || target.getSystem() == null || target.isHulk()) return;
		if (ship == null || ship.getSystem() == null || ship.isHulk()) return;
		
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
		if (other.isPhased()) return false;
		if (other.hasTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)) return false;

		
		if (other.isFighter()) return false;
		if (other.getOwner() == ship.getOwner()) {
			if (other.getFluxLevel() == 0) {

				return false;

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








