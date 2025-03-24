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

import java.util.ArrayList;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class NA_RELATIVITYDRIVEAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    private final IntervalUtil timer = new IntervalUtil(0.3f, 0.8f);

    public static final float DEGREES = 50f;
    public static final float MIN_PARTIAL = 0.2f;
    public static final float MAX_PARTIAL = 0.4f;
    public static final float BASELINE_WEIGHT = 0.1f;

    public static final float FLUX_THRESH_PARTIAL = 0.5f;
    public static final float FLUX_THRESH_ALWAYS = 0.9f;

    // partial reasons = add +0.2-0.4 weight
    // high reasons = add 0.7f weight
    // always reasons = add 1.0 weight
    // neg reasons = subtract 0.25 weight
    public static final ArrayList<AIFlags> PARTIAL = new ArrayList<>();
    public static final ArrayList<AIFlags> HIGH = new ArrayList<>();
    public static final ArrayList<AIFlags> ALWAYS = new ArrayList<>();
    public static final ArrayList<AIFlags> NEG = new ArrayList<>();
    static {
        HIGH.add(AIFlags.PURSUING);
        HIGH.add(AIFlags.HARASS_MOVE_IN);
        ALWAYS.add(AIFlags.RUN_QUICKLY);
        PARTIAL.add(AIFlags.TURN_QUICKLY);
        PARTIAL.add(AIFlags.NEEDS_HELP);
        PARTIAL.add(AIFlags.BACK_OFF);
        PARTIAL.add(AIFlags.BACK_OFF_MIN_RANGE);
        ALWAYS.add(AIFlags.IN_CRITICAL_DPS_DANGER);
        PARTIAL.add(AIFlags.BACKING_OFF);
        NEG.add(AIFlags.DO_NOT_PURSUE);
        NEG.add(AIFlags.DELAY_STRIKE_FIRE);
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
        timer.advance(amount);
        if (timer.intervalElapsed() || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) {
            if (!AIUtils.canUseSystemThisFrame(ship)) {
                return;
            }

            boolean useMe = false;
            Vector2f targetLocation = null;
            AssignmentInfo assignment = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly()).getAssignmentFor(ship);

            for (AIFlags f : ALWAYS) {
                if (flags.hasFlag(f)) {
                    ship.useSystem();
                    return;
                }
            }

            float flux = ship.getFluxLevel();
            if (flux >= FLUX_THRESH_ALWAYS) {
                ship.useSystem();
                return;
            }

            // First priority: use to retreat if ordered to retreat. Overrides/ignores the "useMe" system and AI flag checks.
            if (assignment != null && assignment.getType() == CombatAssignmentType.RETREAT) {
                if (ship.getOwner() == 1 || (ship.getOwner() == 0 && engine.getFleetManager(FleetSide.PLAYER).getGoal() == FleetGoal.ESCAPE)) {
                    targetLocation = new Vector2f(ship.getLocation().x, ship.getLocation().y + 800f); // if ship is enemy OR in "escape" type battle, target loc is UP
                } else {
                    targetLocation = new Vector2f(ship.getLocation().x, ship.getLocation().y - 800f); // if ship is player's, target loc is DOWN
                }
                if (rightDirection(ship, targetLocation)) {
                    ship.useSystem();
                }

                return;  // prevents the AI from activating the ship's system while retreating and facing the wrong direction
                // thanks, Starsector forums user Morathar
            }

            float weight = MathUtils.getRandomNumberInRange(0f, BASELINE_WEIGHT);

            if (flux >= FLUX_THRESH_PARTIAL) {
                weight += MathUtils.getRandomNumberInRange(MIN_PARTIAL, MAX_PARTIAL) * (1f + flux);
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
                        weight += 0.5f;
                    }
                }
            }

            for (AIFlags f : NEG) {
                if (flags.hasFlag(f)) {
                    weight -= 0.25;
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
