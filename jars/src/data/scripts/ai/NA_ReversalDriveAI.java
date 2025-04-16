package data.scripts.ai;
//////////////////////
//script partially based on code by Vayra, from Kadur
//////////////////////

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.NAUtils;
import data.scripts.NA_GravityCatapult;
import data.scripts.NA_ReversalDrive;
import data.scripts.NA_ReversalDriveSuper;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class NA_ReversalDriveAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    private IntervalUtil timer = new IntervalUtil(0.3f, 0.8f);

    public static final float DMG_WEIGHT = 0.5f;
    public static final float DEGREES = 50f;
    public static final float MIN_PARTIAL = 0.2f;
    public static final float MAX_PARTIAL = 0.4f;
    public static final float BASELINE_WEIGHT = 0.1f;

    public static final float FLUX_THRESH_PARTIAL = 0.5f;
    public static final float FLUX_THRESH_ALWAYS = 0.9f;

    private NA_ReversalDrive system = null;

    // partial reasons = add +0.2-0.4 weight
    // high reasons = add 0.7f weight
    // always reasons = add 1.0 weight
    // neg reasons = subtract 0.7 weight
    public static final ArrayList<AIFlags> PARTIAL = new ArrayList<>();
    public static final ArrayList<AIFlags> HIGH = new ArrayList<>();
    public static final ArrayList<AIFlags> ALWAYS = new ArrayList<>();
    public static final ArrayList<AIFlags> NEG = new ArrayList<>();
    static {
        NEG.add(AIFlags.PURSUING);
        PARTIAL.add(AIFlags.RUN_QUICKLY);
        PARTIAL.add(AIFlags.NEEDS_HELP);
        PARTIAL.add(AIFlags.BACK_OFF);
        PARTIAL.add(AIFlags.BACK_OFF_MIN_RANGE);
        NEG.add(AIFlags.HARASS_MOVE_IN);
        ALWAYS.add(AIFlags.IN_CRITICAL_DPS_DANGER);
        PARTIAL.add(AIFlags.BACKING_OFF);
        PARTIAL.add(AIFlags.BACK_OFF);
        PARTIAL.add(AIFlags.DO_NOT_PURSUE);
        PARTIAL.add(AIFlags.DO_NOT_BACK_OFF);
        NEG.add(AIFlags.SAFE_VENT);
        NEG.add(AIFlags.AUTO_BEAM_FIRING_AT_PHASE_SHIP);
        NEG.add(AIFlags.AUTO_FIRING_AT_PHASE_SHIP);
        NEG.add(AIFlags.MAINTAINING_STRIKE_RANGE);
        ALWAYS.add(AIFlags.HAS_INCOMING_DAMAGE);
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = (NA_ReversalDrive) system.getScript();
    }

    private float damageSinceLastTick = 0f;
    private float lastFlux = 0f;
    private float lastHull = 0f;
    // percent of flux or hull
    private final float DMG_PANIC_THRESH = 0.2f;

    public void resetTimer() {
        float len = 0.1f;
        timer = new IntervalUtil(len, len);
        if (ship != null) {
            lastFlux = ship.getFluxLevel();
            lastHull = ship.getHullLevel();
        }
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine.isPaused()) {
            return;
        }
        damageSinceLastTick = (ship.getFluxLevel() - lastFlux) - (ship.getHullLevel() - lastHull);

        float aggromod = 1f;

        if (ship.getCaptain() != null && ship.getCaptain().getPersonalityAPI() != null) {
            if (ship.getCaptain().getPersonalityAPI().equals(Personalities.RECKLESS))
                aggromod *= 1.5f;
            else if (ship.getCaptain().getPersonalityAPI().equals(Personalities.AGGRESSIVE))
                aggromod *= 1.25f;
            else if (ship.getCaptain().getPersonalityAPI().equals(Personalities.TIMID)
                || ship.getCaptain().getPersonalityAPI().equals(Personalities.CAUTIOUS))
                aggromod *= 0.75f;
        }



        if (!timer.intervalElapsed())
            timer.advance(amount);
        if (timer.intervalElapsed() || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) {

            AssignmentInfo assignment = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly()).getAssignmentFor(ship);


            if (!AIUtils.canUseSystemThisFrame(ship)) {
                return;
            }

            float weight = MathUtils.getRandomNumberInRange(0f, BASELINE_WEIGHT);
            for (AIFlags f : ALWAYS) {
                if (flags.hasFlag(f)) {
                    weight += MathUtils.getRandomNumberInRange(4f*MAX_PARTIAL, 5f*MAX_PARTIAL);
                }
            }

            Vector3f lp = system.getLastPoint();
            if (lp == null) return;
            Vector2f lastPoint = new Vector2f(lp.x, lp.y);

            float friendlyWeightHere = NAUtils.getFriendlyWeight(ship, ship.getLocation(), 1000f);
            float enemyWeightHere = NAUtils.getEnemyWeight(ship, ship.getLocation(), 1000f);
            float friendlyWeightLast = NAUtils.getFriendlyWeight(ship, lastPoint, 1000);
            float enemyWeightLast = NAUtils.getEnemyWeight(ship, lastPoint, 1000);

            weight -= friendlyWeightHere / aggromod;
            weight += friendlyWeightLast / aggromod;
            weight += enemyWeightHere / aggromod;
            weight -= enemyWeightLast / aggromod;

            if (flags.getCustom(AIFlags.MANEUVER_TARGET) != null && flags.getCustom(AIFlags.MANEUVER_TARGET) instanceof ShipAPI) {
                if (MathUtils.getDistance((ShipAPI) flags.getCustom(AIFlags.MANEUVER_TARGET), lastPoint) <= NA_ReversalDriveSuper.DMG_AREA) {
                    if (weight > -0.5 && system instanceof NA_ReversalDriveSuper) {
                        weight += DMG_WEIGHT * NAUtils.getEnemyWeight(ship, lastPoint, NA_ReversalDriveSuper.DMG_AREA);
                    } else if (weight > -0.25) {
                        weight += 0.45f;
                    }
                }
            }


            if (damageSinceLastTick > DMG_PANIC_THRESH * aggromod) {
                weight += 2f;
            } else if (flags.getCustom(AIFlags.MANEUVER_TARGET) != null && flags.getCustom(AIFlags.MANEUVER_TARGET) instanceof ShipAPI) {
                if (flags.hasFlag(AIFlags.PURSUING) || (!flags.hasFlag(AIFlags.BACK_OFF) && !flags.hasFlag(AIFlags.BACKING_OFF) && !flags.hasFlag(AIFlags.DO_NOT_PURSUE))) {
                    if (MathUtils.getDistance((ShipAPI) flags.getCustom(AIFlags.MANEUVER_TARGET), lastPoint) <=
                            MathUtils.getDistance((ShipAPI) flags.getCustom(AIFlags.MANEUVER_TARGET), ship.getLocation()))
                        weight += 2f;
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


            float friendlyWeight = NAUtils.shipSize(ship);

            if (damageSinceLastTick > DMG_PANIC_THRESH * aggromod || ship.getFluxTracker().getFluxLevel() > 0.85f ||
                    (ship.getFluxTracker().getFluxLevel() > 0.5f
                            && ship.getAIFlags() != null && (
                            ship.getAIFlags().hasFlag(AIFlags.BACKING_OFF)
                            ))) {
                panic = true;
            } else {
                List<ShipAPI> friendsNearby = NAUtils.getFriendlyShipsWithinRange(ship, ship.getLocation(), NA_GravityCatapult.MAX_RANGE, true);
                for (ShipAPI shp: friendsNearby) {
                    friendlyWeight += NAUtils.shipSize(shp);
                }
                if (ship.getAIFlags() != null && ship.getAIFlags().hasFlag(AIFlags.BACKING_OFF)) {


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
                    if (flags.getCustom(AIFlags.MANEUVER_TARGET) != null && flags.getCustom(AIFlags.MANEUVER_TARGET) instanceof ShipAPI) {
                        if (!flags.hasFlag(AIFlags.PURSUING)) {
                            if (flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)
                                    || flags.hasFlag(AIFlags.HAS_INCOMING_DAMAGE)
                                    || (MathUtils.getDistance((ShipAPI) flags.getCustom(AIFlags.MANEUVER_TARGET), lastPoint) >
                                    MathUtils.getDistance((ShipAPI) flags.getCustom(AIFlags.MANEUVER_TARGET), ship.getLocation())))
                                ship.useSystem();
                                resetTimer();
                                float len = Math.min(2.5f, Math.max(0.5f, weight));
                                timer = new IntervalUtil(len, len);
                                return;
                        }
                    }

                }
            }
        }


        if (timer.intervalElapsed()) {
            resetTimer();
        }
    }
}
