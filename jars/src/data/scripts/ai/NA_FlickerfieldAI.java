package data.scripts.ai;
//////////////////////
//script partially based on code by Vayra, from Kadur
//////////////////////

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NA_Flickerfield;
import data.scripts.NA_GravityCatapult;
import data.scripts.NA_ReversalDrive;
import data.scripts.NA_ReversalDriveSuper;
import data.scripts.campaign.plugins.NAUtils;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class NA_FlickerfieldAI implements ShipSystemAIScript {

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

    private NA_Flickerfield system = null;

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
        PARTIAL.add(AIFlags.PHASE_BRAWLER_DUMPING_FLUX);
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
        this.system = (NA_Flickerfield) system.getScript();
    }

    private float damageSinceLastTick = 0f;
    private float lastFlux = 0f;
    private float lastHull = 0f;
    // percent of flux or hull
    private final float DMG_PANIC_THRESH = 0.2f;

    public void resetTimer() {
        float len = 0.05f;
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
                    weight += MathUtils.getRandomNumberInRange(9f*MAX_PARTIAL, 15f*MAX_PARTIAL);
                }
            }


            List<MissileAPI> missiles = NAUtils.getMissilesWithinRange(ship.getLocation(), 500f);
            for (MissileAPI b : missiles) {
                if (b.getSource() == ship) continue;
                if (!(b.getOwner() == ship.getOwner() && b.getCollisionClass() != CollisionClass.MISSILE_FF)) continue;
                if (ship.isPointInBounds(MathUtils.getPointOnCircumference(b.getLocation(), 450, b.getFacing()))
                        || ship.isPointInBounds(MathUtils.getPointOnCircumference(b.getLocation(), 300, b.getFacing()))
                        || ship.isPointInBounds(MathUtils.getPointOnCircumference(b.getLocation(), 150, b.getFacing()))
                        || ship.isPointInBounds(MathUtils.getPointOnCircumference(b.getLocation(), 50, b.getFacing()))) {
                    weight += b.getDamageAmount() * 0.005f;
                }
            }
            List<DamagingProjectileAPI> bullets = NAUtils.getProjectilesWithinRange(ship.getLocation(), 500f, false);
            for (DamagingProjectileAPI b : bullets) {
                if (b.getSource() == ship) continue;
                if (ship.isPointInBounds(MathUtils.getPointOnCircumference(b.getLocation(), 450, b.getFacing()))
                        || ship.isPointInBounds(MathUtils.getPointOnCircumference(b.getLocation(), 300, b.getFacing()))
                        || ship.isPointInBounds(MathUtils.getPointOnCircumference(b.getLocation(), 150, b.getFacing()))
                        || ship.isPointInBounds(MathUtils.getPointOnCircumference(b.getLocation(), 50, b.getFacing()))) {
                    weight += b.getDamageAmount() * 0.003f;
                }
            }


            List<BeamAPI> beans = Global.getCombatEngine().getBeams();
            for (BeamAPI b : beans) {
                if (b.getDamageTarget() == ship) continue;
                weight += b.getDamage().getDamage() * 0.003f;
            }


            if (damageSinceLastTick > DMG_PANIC_THRESH * aggromod) {
                weight += 2f;
            } else if (flags.getCustom(AIFlags.MANEUVER_TARGET) != null && flags.getCustom(AIFlags.MANEUVER_TARGET) instanceof ShipAPI) {
                if (flags.hasFlag(AIFlags.PURSUING) || (!flags.hasFlag(AIFlags.BACK_OFF) && !flags.hasFlag(AIFlags.BACKING_OFF) && !flags.hasFlag(AIFlags.DO_NOT_PURSUE))) {
                    weight += 2f;
                }
            }

            for (AIFlags f : NEG) {
                if (flags.hasFlag(f)) {
                    weight -= 0.7;
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

            float friendlyWeight = NAUtils.shipSize(ship);

            if (damageSinceLastTick > DMG_PANIC_THRESH * aggromod || ship.getFluxTracker().getFluxLevel() > 0.85f ||
                    (ship.getFluxTracker().getFluxLevel() > 0.5f
                            && ship.getAIFlags() != null && (
                            ship.getAIFlags().hasFlag(AIFlags.BACKING_OFF)
                            ))) {
                panic = true;
            }





            if (weight >= 1.0f || panic) {

                if (weight >= 1.3f || panic) {
                    if (flags.getCustom(AIFlags.MANEUVER_TARGET) != null && flags.getCustom(AIFlags.MANEUVER_TARGET) instanceof ShipAPI) {
                        boolean further = true;
                        if (weight > 2f || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)
                                || ((further && !flags.hasFlag(AIFlags.PURSUING) && flags.hasFlag(AIFlags.BACKING_OFF))
                                || (!further && flags.hasFlag(AIFlags.PURSUING)))) {
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
