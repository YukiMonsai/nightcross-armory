package data.scripts.ai;
//////////////////////
//script partially based on code by Vayra, from Kadur
//////////////////////

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.NA_GravityCatapult;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;

public class NA_TIDALDISRUPTIONAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    private final IntervalUtil timer = new IntervalUtil(0.3f, 0.8f);

    public static final float DEGREES = 50f;
    public static final float MIN_PARTIAL = 0.2f;
    public static final float MAX_PARTIAL = 0.4f;
    public static final float BASELINE_WEIGHT = 0.1f;

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
        HIGH.add(AIFlags.HARASS_MOVE_IN);
        NEG.add(AIFlags.RUN_QUICKLY);
        NEG.add(AIFlags.TURN_QUICKLY);
        PARTIAL.add(AIFlags.MAINTAINING_STRIKE_RANGE);
        NEG.add(AIFlags.IN_CRITICAL_DPS_DANGER);
        NEG.add(AIFlags.BACKING_OFF);
        NEG.add(AIFlags.BACK_OFF);
        NEG.add(AIFlags.DO_NOT_PURSUE);
        PARTIAL.add(AIFlags.DELAY_STRIKE_FIRE);
        PARTIAL.add(AIFlags.DO_NOT_BACK_OFF);
        NEG.add(AIFlags.SAFE_VENT);
        NEG.add(AIFlags.DO_NOT_PURSUE);
        HIGH.add(AIFlags.ESCORT_OTHER_SHIP);
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


            CombatFleetManagerAPI.AssignmentInfo assignment = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly()).getAssignmentFor(ship);
            // First priority: dont use if retreating
            if (assignment != null && assignment.getType() == CombatAssignmentType.RETREAT) {
                 return;
            }

            // dont use on fighters
            if (target != null && target.getHullSize() == ShipAPI.HullSize.FIGHTER) return;

            // always use it if a target is high on flux
            float flux = target != null ? target.getFluxLevel() : 0f;
            if (flux >= FLUX_THRESH_ALWAYS) {
                ship.useSystem();
                return;
            }

            for (AIFlags f : ALWAYS) {
                if (flags.hasFlag(f)) {
                    ship.useSystem();
                    return;
                }
            }

            float weight = MathUtils.getRandomNumberInRange(0f, BASELINE_WEIGHT);


            if (target != null && target.getOwner() != ship.getOwner()) {
                // HOSTILE DETECTED
                // check if we are behind the shield or its off -- if so, increase weight because our weapons can hit
                if (target.getShield() == null || !target.getShield().isOn()
                    || !target.getShield().isWithinArc(ship.getLocation())) {
                    weight += 0.4f;
                }
            }

            for (AIFlags f : NEG) {
                if (flags.hasFlag(f)) {
                    weight -= 0.5;
                }
            }

            // Make it more likely to activate the system if the weapons are firing, to help salvo land
            int count = ship.getAllWeapons().size();
            if (count > 0) {
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (w.isFiring())
                    {
                        weight += 0.7f / ((float)count);
                    } else if (!w.isDisabled() && w.getCooldownRemaining() > 0) {
                        weight -= 0.2f / ((float)count);
                    }
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
