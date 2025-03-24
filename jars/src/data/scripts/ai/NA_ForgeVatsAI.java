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
import data.scripts.NA_ForgeVats;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class NA_ForgeVatsAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    private final IntervalUtil timer = new IntervalUtil(0.3f, 0.8f);

    public static final float DEGREES = 50f;
    public static final float MIN_PARTIAL = 0.2f;
    public static final float MAX_PARTIAL = 0.4f;
    public static final float BASELINE_WEIGHT = 0.1f;

    public static final float FLUX_THRESH_PARTIAL = 0.5f;

    // partial reasons = add +0.2-0.4 weight
    // high reasons = add 0.7f weight
    // always reasons = add 1.0 weight
    // neg reasons = subtract 0.25 weight
    public static final ArrayList<AIFlags> PARTIAL = new ArrayList<>();
    public static final ArrayList<AIFlags> HIGH = new ArrayList<>();
    public static final ArrayList<AIFlags> NEG = new ArrayList<>();
    static {
        NEG.add(AIFlags.PURSUING);
        NEG.add(AIFlags.HARASS_MOVE_IN);
        NEG.add(AIFlags.RUN_QUICKLY);
        NEG.add(AIFlags.NEEDS_HELP);
        NEG.add(AIFlags.BACK_OFF);
        NEG.add(AIFlags.IN_CRITICAL_DPS_DANGER);
        PARTIAL.add(AIFlags.DO_NOT_PURSUE);
        NEG.add(AIFlags.DELAY_STRIKE_FIRE);
        HIGH.add(AIFlags.SAFE_VENT);
        NEG.add(AIFlags.AUTO_BEAM_FIRING_AT_PHASE_SHIP);
        NEG.add(AIFlags.AUTO_FIRING_AT_PHASE_SHIP);
        NEG.add(AIFlags.MAINTAINING_STRIKE_RANGE);
        NEG.add(AIFlags.HAS_INCOMING_DAMAGE);
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
        timer.advance(amount);
        if (timer.intervalElapsed() || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) {
            if (!AIUtils.canUseSystemThisFrame(ship)) {
                return;
            }

            AssignmentInfo assignment = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly()).getAssignmentFor(ship);

            float flux = ship.getFluxLevel();
            if (flux >= FLUX_THRESH_PARTIAL) {
                return;
            }

            if (assignment != null && assignment.getType() == CombatAssignmentType.RETREAT) {
                return;  // prevents the AI from activating the ship's system while retreating
                // thanks, Starsector forums user Morathar
            }


            List<WeaponAPI> wpns = NA_ForgeVats.getWeapons(ship);

            if (wpns.size() == 0) {
                return; // dont use if we have no relevant stuff
            }
            float weight = MathUtils.getRandomNumberInRange(0f, BASELINE_WEIGHT);

            for (WeaponAPI wpn : wpns) {
                if (wpn.getAmmoTracker().getAmmo() < 0.5f * wpn.getAmmoTracker().getMaxAmmo()) {
                    if (wpn.getSpec().getSize() == WeaponAPI.WeaponSize.LARGE)
                        weight += 2.5 / wpns.size();
                    else if (wpn.getSpec().getSize() == WeaponAPI.WeaponSize.LARGE)
                        weight += 1.2 / wpns.size();
                    else if (wpn.getSpec().getSize() == WeaponAPI.WeaponSize.LARGE)
                        weight += 0.7 / wpns.size();
                }
            }

            if (weight < 0.5f) return; // dont reload if we dont have eligible weapons

            if (target != null && target.getOwner() != ship.getOwner()) {
                // HOSTILE DETECTED
                weight -= 0.25; // we dont want to use in combat too much
            }

            if (flux <= 0.1) {
                weight += 0.35;
            }

            for (AIFlags f : NEG) {
                if (flags.hasFlag(f)) {
                    weight -= 0.25;
                }
            }

            for (AIFlags f : PARTIAL) {
                if (flags.hasFlag(f)) {
                    weight += MathUtils.getRandomNumberInRange(MIN_PARTIAL, MAX_PARTIAL);
                    if (weight >= 1.0f) {
                        ship.useSystem();
                        return;
                    }
                }
            }
            for (AIFlags f : HIGH) {
                if (flags.hasFlag(f)) {
                    weight += MathUtils.getRandomNumberInRange(2f*MAX_PARTIAL, 3f*MAX_PARTIAL);
                    if (weight >= 1.0f) {
                        ship.useSystem();
                        return;
                    }
                }
            }


            if (weight >= 1.0f) {
                ship.useSystem();
            }
        }
    }
}
