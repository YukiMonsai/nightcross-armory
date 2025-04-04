package data.scripts.ai;
//////////////////////
//script partially based on code by Vayra, from Kadur
//////////////////////

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.NAUtils;
import data.scripts.NA_GravityCatapult;
import data.scripts.NA_RelativityDrive;
import data.scripts.util.NAUtil;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class NA_GRAVITYCATAPULTAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    private IntervalUtil timer = new IntervalUtil(0.3f, 0.8f);

    public static final float DEGREES = 50f;
    public static final float MIN_PARTIAL = 0.2f;
    public static final float MAX_PARTIAL = 0.4f;
    public static final float BASELINE_WEIGHT = 0.1f;

    public static final float FLUX_THRESH_PARTIAL = 0.5f;
    public static final float FLUX_THRESH_ALWAYS = 0.9f;

    // partial reasons = add +0.2-0.4 weight
    // high reasons = add 0.7f weight
    // always reasons = add 1.0 weight
    // neg reasons = subtract 0.7 weight
    public static final ArrayList<AIFlags> PARTIAL = new ArrayList<>();
    public static final ArrayList<AIFlags> HIGH = new ArrayList<>();
    public static final ArrayList<AIFlags> ALWAYS = new ArrayList<>();
    public static final ArrayList<AIFlags> NEG = new ArrayList<>();
    static {
        HIGH.add(AIFlags.PURSUING);
        NEG.add(AIFlags.RUN_QUICKLY);
        PARTIAL.add(AIFlags.NEEDS_HELP);
        NEG.add(AIFlags.BACK_OFF);
        NEG.add(AIFlags.BACK_OFF_MIN_RANGE);
        NEG.add(AIFlags.ESCORT_OTHER_SHIP);
        NEG.add(AIFlags.DO_NOT_USE_FLUX);
        NEG.add(AIFlags.HARASS_MOVE_IN);
        NEG.add(AIFlags.CAMP_LOCATION);
        HIGH.add(AIFlags.IN_CRITICAL_DPS_DANGER);
        NEG.add(AIFlags.BACKING_OFF);
        NEG.add(AIFlags.BACK_OFF);
        NEG.add(AIFlags.DO_NOT_PURSUE);
        PARTIAL.add(AIFlags.DO_NOT_BACK_OFF);
        NEG.add(AIFlags.SAFE_VENT);
        NEG.add(AIFlags.AUTO_BEAM_FIRING_AT_PHASE_SHIP);
        NEG.add(AIFlags.AUTO_FIRING_AT_PHASE_SHIP);
        NEG.add(AIFlags.MAINTAINING_STRIKE_RANGE);
        HIGH.add(AIFlags.HAS_INCOMING_DAMAGE);
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
    }

    // method to check if we're facing within X degrees of target
    private boolean rightDirection(ShipAPI ship, Vector2f targetLocation) {
        Vector2f curr = ship.getLocation();
        float angleToTarget = VectorUtils.getAngle(curr, targetLocation);
        return (Math.abs(MathUtils.getShortestRotation(angleToTarget, ship.getFacing())) <= DEGREES);
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine.isPaused()) {
            return;
        }
        if (!timer.intervalElapsed())
            timer.advance(amount);
        if (timer.intervalElapsed() || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) {
            if (!AIUtils.canUseSystemThisFrame(ship)) {
                return;
            }
            float weight = MathUtils.getRandomNumberInRange(0f, BASELINE_WEIGHT);
            for (AIFlags f : ALWAYS) {
                if (flags.hasFlag(f)) {
                    weight += MathUtils.getRandomNumberInRange(4f*MAX_PARTIAL, 5f*MAX_PARTIAL);
                }
            }



            if (target != null && target.getOwner() != ship.getOwner()) {
                // HOSTILE DETECTED
                // check if target is bigger. if so we probably want to flank
                int size_this = 1;
                int size_that = 1;
                switch (ship.getHullSize()) {
                    case CAPITAL_SHIP: size_this = 4; break;
                    case CRUISER: size_this = 3; break;
                    case DESTROYER: size_this = 2; break;
                }
                switch (target.getHullSize()) {
                    case CAPITAL_SHIP: size_that = 4; break;
                    case CRUISER: size_that = 3; break;
                    case DESTROYER: size_that = 2; break;
                }
                if (size_that > size_this) {
                    // check if we need to get behind their shield -- if so, increase weight by 0.5 if target is bigger than us
                    if (target.getShield() != null && target.getShield().isOn()
                        && target.getShield().isWithinArc(ship.getLocation())) {
                        weight += 0.7f * Math.max(0, 1f - Math.abs(MathUtils.getShortestRotation(target.getFacing(),
                                VectorUtils.getAngle(target.getLocation(), ship.getLocation())))/60f);
                    } else if (target.getShield() != null && target.getShield().isOn()) {
                        // stay behind!
                        weight -= 3.0f * Math.max(0, 1f - Math.abs(MathUtils.getShortestRotation(target.getFacing() + 180f,
                                VectorUtils.getAngle(target.getLocation(), ship.getLocation())))/90f);
                    }
                }
            }

            for (AIFlags f : NEG) {
                if (flags.hasFlag(f)) {
                    weight -= 0.7;
                }
            }

            // Make it less likely to activate the system (which would disable firing) if the weapons are firing
            int count = ship.getAllWeapons().size();
            if (count > 0) {
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (w.isFiring())
                    {
                        weight -= 1.5f / ((float)count);
                    } else if (!w.isDisabled() && w.getCooldownRemaining() > 0) {
                        weight -= 0.4f / ((float)count);
                    }
                }
            }


            for (AIFlags f : PARTIAL) {
                if (flags.hasFlag(f)) {
                    weight += MathUtils.getRandomNumberInRange(MIN_PARTIAL, MAX_PARTIAL);
                }
            }
            for (AIFlags f : HIGH) {
                if (flags.hasFlag(f)) {
                    weight += MathUtils.getRandomNumberInRange(2f*MAX_PARTIAL, 3f*MAX_PARTIAL);
                }
            }

            boolean panic = false;
            List<ShipAPI> enemiesNearby = NAUtils.getEnemyShipsWithinRange(ship, ship.getLocation(), NA_GravityCatapult.MAX_RANGE, true);

            float enemyWeight = 0;
            for (ShipAPI shp: enemiesNearby) {
                if (!shp.getFluxTracker().isOverloadedOrVenting())
                    enemyWeight += NAUtils.shipSize(shp);
            }

            if (ship.getFluxTracker().getFluxLevel() > 0.85f ||
                    (ship.getFluxTracker().getFluxLevel() > 0.5f
                            && ship.getAIFlags() != null && (
                            ship.getAIFlags().hasFlag(AIFlags.BACKING_OFF)
                            ))) {
                panic = true;
            } else {
                if (ship.getAIFlags() != null && ship.getAIFlags().hasFlag(AIFlags.BACKING_OFF)) {
                    List<ShipAPI> friendsNearby = NAUtils.getFriendlyShipsWithinRange(ship, ship.getLocation(), NA_GravityCatapult.MAX_RANGE, true);

                    float friendlyWeight = NAUtils.shipSize(ship);
                    for (ShipAPI shp: friendsNearby) {
                        friendlyWeight += NAUtils.shipSize(shp);
                    }

                    if (enemyWeight > friendlyWeight) {
                        panic = true;
                    }
                }
            }





            if (weight >= 1.0f || panic) {
                // dont dive into a group of enemies lol
                if (target != null && !panic) {
                    float dist = MathUtils.getDistance(ship.getLocation(), target.getLocation());
                    float angle = (float) Math.atan2(target.getLocation().y - ship.getLocation().y,
                            target.getLocation().x - ship.getLocation().x);
                    Vector2f targetloc = new Vector2f(
                            target.getLocation().x + (float) (Math.cos(angle)*(dist + NA_GravityCatapult.BASE_DIST)),
                            target.getLocation().y + (float) (Math.sin(angle)*(dist + NA_GravityCatapult.BASE_DIST))
                    );
                    List<ShipAPI> shipsOnOtherSide = NAUtils.getShipsWithinRange(targetloc, NA_GravityCatapult.MAX_RANGE+400f);
                    ArrayList<ShipAPI> filtered = new ArrayList<ShipAPI>();
                    for (ShipAPI s : shipsOnOtherSide) {
                        if (s.isAlive()
                                && s.getOwner() != ship.getOwner()
                                && s.getFluxTracker().getFluxLevel() < 0.9f
                                && !s.getFluxTracker().isOverloadedOrVenting()) {
                            filtered.add(s);
                        }
                    }
                    if (filtered.size() > 1) weight -= filtered.size() * 1.5f;
                }

                if (weight >= 1.3f || panic) {
                    if (panic) {
                        // we DONT want to use it, but a lot of negative reasons means we want to retreat
                        ShipAPI friend = NA_GravityCatapult.findTarget(ship, true);
                        if (friend != null) {
                            // use on friend preferentially
                            float targetWeight = 0f;
                            Vector2f targetPoint = NA_GravityCatapult.getJumpPoint(ship, friend);

                            List<ShipAPI> trgNearby = NAUtils.getEnemyShipsWithinRange(ship, targetPoint, NA_GravityCatapult.MAX_RANGE, true);

                            for (ShipAPI shp: trgNearby) {
                                if (!shp.getFluxTracker().isOverloadedOrVenting())
                                    targetWeight += NAUtils.shipSize(shp);
                            }

                            if (targetWeight < enemyWeight) {
                                ShipAPI oldTarget = ship.getShipTarget();
                                ship.getAIFlags().setFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM, 1f, friend);
                                ship.setShipTarget(friend);
                                ship.useSystem();
                                ship.setShipTarget(oldTarget);
                                return;
                            } else {
                                float len = 0.25f;
                                timer = new IntervalUtil(len, len);
                                return;
                            }


                        } else {
                            // find a target in range with the least enemyweight of the point around
                            ShipAPI bestTarget = null;
                            float bestTargetWeight = enemyWeight;

                            for (ShipAPI tmp: enemiesNearby) {
                                if (tmp.isFighter()) continue;
                                float targetWeight = 0f;
                                Vector2f targetPoint = NA_GravityCatapult.getJumpPoint(ship, tmp);

                                List<ShipAPI> trgNearby = NAUtils.getEnemyShipsWithinRange(ship, targetPoint, NA_GravityCatapult.MAX_RANGE, true);

                                for (ShipAPI shp: trgNearby) {
                                    if (!shp.getFluxTracker().isOverloadedOrVenting())
                                        targetWeight += NAUtils.shipSize(shp);
                                }

                                if (targetWeight < bestTargetWeight) {
                                    bestTargetWeight = targetWeight;
                                    bestTarget = tmp;
                                }


                            }

                            if (bestTarget != null) {

                                ShipAPI oldTarget = ship.getShipTarget();
                                ship.setShipTarget(bestTarget);
                                ship.getAIFlags().setFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM, 1f, bestTarget);
                                ship.useSystem();
                                ship.setShipTarget(oldTarget);
                                return;
                            } else {
                                float len = 0.25f;
                                timer = new IntervalUtil(len, len);
                                return;
                            }
                        }
                    } else {
                        ship.useSystem();
                        float len = Math.min(2.5f, Math.max(0.5f, weight));
                        timer = new IntervalUtil(len, len);
                        return;
                    }
                }
            } else if (weight < -3.0f) {
                // we DONT want to use it, but a lot of negative reasons means we want to retreat
                ShipAPI friend = NA_GravityCatapult.findTarget(ship, true);
                if (friend != null) {
                    ShipAPI oldTarget = ship.getShipTarget();
                    ship.setShipTarget(friend);
                    ship.getAIFlags().setFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM, 1f, friend);
                    ship.useSystem();
                    ship.setShipTarget(oldTarget);
                    return;
                } else {
                    float len = 0.25f;
                    timer = new IntervalUtil(len, len);
                    return;
                }
            }
        }
    }
}
