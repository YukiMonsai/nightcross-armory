import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.lang.reflect.*;
import java.util.*;

/**
 * Runs the actual installed/source-compiled RKKV AIs against proxies of real game API
 * interfaces. No replacement engine/library source is used. Rendering is not invoked.
 * The angular integration below models the installed engine's processCommands bytecode;
 * it is not a full combat simulation or a performance benchmark.
 */
public final class RkkvGuidanceRegression {
    private static int passed, failed;
    private static final float[] STEPS = {.016f, .033f, .067f, .1f, .2f};

    public static void main(String[] args) throws Exception {
        for (String suffix : new String[]{"", "_HE"}) {
            Class<?> type = Class.forName("data.scripts.weapons.ai.NA_RKKVAI" + suffix);
            testType(type, !suffix.isEmpty());
        }
        System.out.println("RKKV guidance: " + passed + " passed, " + failed + " failed");
        if (failed != 0) System.exit(1);
    }

    private static void testType(Class<?> type, boolean he) throws Exception {
        String name = type.getSimpleName() + ": ";
        for (float[] point : new float[][]{{3000, 4000}, {0, -4000}, {-3000, -4000}}) {
            run(name + "Euclidean lead " + Arrays.toString(point), () -> {
                Fixture f = new Fixture(type);
                Method m = type.getDeclaredMethod("leadPoint", Vector2f.class,
                        Vector2f.class, Vector2f.class, float.class);
                m.setAccessible(true);
                Vector2f lead = (Vector2f) m.invoke(f.ai, new Vector2f(point[0], point[1]),
                        new Vector2f(10, 20), new Vector2f(), 1000f);
                float seconds = (float)Math.hypot(point[0], point[1]) / 1000f;
                near(lead.x, point[0] + 10 * seconds, .01f, "lead x");
                near(lead.y, point[1] + 20 * seconds, .01f, "lead y");
            });
        }
        for (float[] point : new float[][]{{8000, 6000}, {-8000, 6000},
                {8000, -6000}, {-8000, -6000}}) {
            run(name + "stationary world target " + Arrays.toString(point), () -> {
                Fixture f = new Fixture(type);
                f.target.location.set(point[0], point[1]);
                f.missile.velocity.set(point[0] / 10, point[1] / 10);
                f.missile.facing = bearing(f.target.location);
                f.start(1);
                f.advance(.016f);
                Vector2f lead = (Vector2f) field(f.ai, "lead");
                near(lead.x, point[0], .01f, "stationary lead x");
                near(lead.y, point[1], .01f, "stationary lead y");
            });
        }
        for (float speed : new float[]{0f, .01f, 1000f}) {
            run(name + "moving world target at launch speed " + speed, () -> {
                Fixture f = new Fixture(type);
                f.target.location.set(8000, 6000);
                f.target.velocity.set(40, -60);
                f.missile.velocity.set(speed, 0);
                f.start(1);
                f.advance(.016f);
                Vector2f lead = (Vector2f) field(f.ai, "lead");
                float time = 10000f / (f.missile.maxSpeed * (he ? .7f : .5f));
                near(lead.x, 8000 + .75f * 40 * time, .02f, "moving lead x");
                near(lead.y, 6000 - .75f * 60 * time, .02f, "moving lead y");
            });
        }
        for (String invalid : new String[]{"dead", "removed", "noncolliding"}) {
            run(name + "discard retained " + invalid, () -> {
                Fixture f = new Fixture(type);
                f.start(1);
                f.invalidate(invalid);
                f.advance(.016f);
                require(f.guided.getTarget() == null, "invalid target was retained");
            });
            run(name + "reject launcher " + invalid, () -> {
                Fixture f = new Fixture(type);
                f.invalidate(invalid);
                f.source.shipTarget = f.target.ship;
                f.missile.source = f.source.ship;
                setField(f.ai, "stage", 0);
                f.advance(.016f);
                require(f.guided.getTarget() == null, "invalid launcher target was inherited");
            });
            run(name + "reject maneuver flag " + invalid, () -> {
                Fixture f = new Fixture(type);
                f.invalidate(invalid);
                f.source.flags.setFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET,
                        10f, f.target.ship);
                f.missile.source = f.source.ship;
                f.advance(.016f);
                require(f.guided.getTarget() == null, "invalid maneuver target was inherited");
            });
            run(name + "fallback reacquires after " + invalid, () -> {
                Fixture f = new Fixture(type);
                f.start(0);
                f.invalidate(invalid);
                f.source.shipTarget = f.target.ship;
                f.missile.source = f.source.ship;
                Entity replacement = new Entity(false);
                replacement.location.set(1000, 0);
                f.ships.add(replacement.ship);
                timer(f.ai, "targetTimer").forceIntervalElapsed();
                f.advance(.016f);
                require(f.guided.getTarget() == replacement.ship, "fallback did not pick valid ship");
            });
        }
        run(name + "valid launcher target inherited", () -> {
            Fixture f = new Fixture(type);
            f.source.shipTarget = f.target.ship;
            f.missile.source = f.source.ship;
            f.advance(.016f);
            require(f.guided.getTarget() == f.target.ship, "valid target not inherited");
        });
        run(name + "flare cannot steal valid target", () -> {
            Fixture f = new Fixture(type);
            f.start(1);
            f.guided.setTarget(new Entity(true).missile);
            require(f.guided.getTarget() == f.target.ship, "flare changed target");
        });
        run(name + "fizzling clears target without commands", () -> {
            Fixture f = new Fixture(type);
            f.start(1);
            f.missile.fizzling = true;
            f.advance(.2f);
            require(f.guided.getTarget() == null, "fizzling retained target");
            require(f.missile.commands.isEmpty(), "fizzling issued command");
        });
        run(name + "paused leaves guidance unchanged", () -> {
            Fixture f = new Fixture(type);
            f.start(1);
            f.paused = true;
            f.advance(.2f);
            require(f.guided.getTarget() == f.target.ship, "pause changed target");
            require(f.missile.commands.isEmpty(), "paused issued command");
        });
        for (int stage : new int[]{0, 1, 2}) {
            for (float dt : STEPS) {
                run(name + "aligned stage " + stage + " dt=" + dt, () -> {
                    Fixture f = new Fixture(type);
                    f.target.location.set(stage == 0 ? -4000 : 8000, 0);
                    f.missile.velocity.set(100, 0);
                    f.start(stage);
                    timer(f.ai, "launchTimer").forceIntervalElapsed();
                    timer(f.ai, "launchTimer").advance(0f);
                    f.advance(dt);
                    require(f.missile.commands.contains(ShipCommand.ACCELERATE),
                            "aligned missile did not accelerate");
                    require(!f.missile.commands.contains(ShipCommand.TURN_LEFT)
                            && !f.missile.commands.contains(ShipCommand.TURN_RIGHT),
                            "aligned missile issued a turn");
                    f.missile.integrateAngular(dt);
                    near(f.missile.angularVelocity, 0, .0001f, "aligned angular drift");
                });
                for (float angle : new float[]{-1, 1, -10, 10}) {
                    for (float omega : new float[]{-90, 0, 90}) {
                        run(name + "bounded steering stage=" + stage + " dt=" + dt
                                + " angle=" + angle + " omega=" + omega, () -> {
                            Fixture f = new Fixture(type);
                            f.target.location.set(stage == 0 ? -4000 : 8000, 0);
                            f.missile.velocity.set(100, 0);
                            f.missile.facing = -angle;
                            f.missile.angularVelocity = omega;
                            f.start(stage);
                            f.advance(dt);
                            f.missile.integrateAngular(dt);
                            float error = MathUtils.getShortestRotation(f.missile.facing, 0);
                            // Opposite initial angular momentum can turn away initially;
                            // it must not cross past the requested heading this frame.
                            require(error * angle >= -.0005f,
                                    "overshoot: remaining=" + error + " initial=" + angle);
                            require(Float.isFinite(error), "nonfinite heading");
                            require(Math.abs(f.missile.angularVelocity) <= f.missile.maxTurn + .001f,
                                    "post-engine angular rate exceeds nominal maximum");
                        });
                    }
                }
            }
        }
        for (int stage : new int[]{0, 1, 2}) {
            for (float dt : STEPS) {
                for (float angle : new float[]{-120, 120}) {
                    run(name + "converges stage=" + stage + " dt=" + dt + " angle=" + angle, () -> {
                        Fixture f = new Fixture(type);
                        f.target.location.set(stage == 0 ? -4000 : 8000, 0);
                        f.missile.velocity.set(100, 0);
                        f.missile.facing = -angle;
                        f.start(stage);
                        // Fixed positions isolate steering; no collision, acceleration,
                        // rendering or full translational combat integration is modeled.
                        for (int i = 0; i < Math.ceil(6f / dt); i++) {
                            f.advance(dt);
                            f.missile.integrateAngular(dt);
                            require(f.guided.getTarget() == f.target.ship, "lost valid target");
                        }
                        near(MathUtils.getShortestRotation(f.missile.facing, 0), 0, .03f,
                                "steady heading did not converge");
                        near(f.missile.angularVelocity, 0, .1f, "steady angular rate");
                    });
                }
            }
        }
    }

    private static final class Fixture {
        final Entity missile = new Entity(true), target = new Entity(false), source = new Entity(false);
        final List<ShipAPI> ships = new ArrayList<>();
        final Set<Object> removed = Collections.newSetFromMap(new IdentityHashMap<>());
        final Object ai;
        final GuidedMissileAI guided;
        boolean paused;
        Fixture(Class<?> type) throws Exception {
            // Actual Nightcross 2.1.1 projectile engineSpec values (unmodified).
            boolean he = type.getSimpleName().endsWith("_HE");
            missile.maxTurn = he ? 1250f : 1150f;
            missile.turnAcceleration = he ? 2850f : 2950f;
            missile.turnDeceleration = missile.turnAcceleration * .5f;
            source.owner = 0;
            missile.owner = 0;
            target.location.set(8000, 0);
            CombatEngineAPI engine = proxy(CombatEngineAPI.class, (p, m, a) -> {
                switch (m.getName()) {
                    case "isPaused": return paused;
                    case "isEntityInPlay": return a[0] != null && !removed.contains(a[0]);
                    case "getShips": return ships;
                    case "getPlayerShip": return source.ship;
                    case "getFogOfWar": return proxy(FogOfWarAPI.class,
                            (p2, m2, a2) -> m2.getName().equals("isVisible")
                                    ? true : defaultValue(p2, m2, a2));
                    case "getProjectiles": case "getAsteroids": case "getMissiles":
                        return Collections.emptyList();
                    case "isUIShowingHUD": return false;
                    default: return defaultValue(p, m, a);
                }
            });
            Global.setCombatEngine(engine);
            ai = type.getConstructor(MissileAPI.class, ShipAPI.class).newInstance(missile.missile, null);
            guided = (GuidedMissileAI)ai;
            // Avoid rendering and random timers while exercising actual guidance.
            timer(ai, "beamTimer").forceCurrInterval(10000);
            timer(ai, "beamTimer").setElapsed(0);
            timer(ai, "targetTimer").setElapsed(0);
        }
        void start(int stage) throws Exception {
            setField(ai, "stage", stage);
            guided.setTarget(target.ship);
        }
        void invalidate(String kind) {
            if (kind.equals("dead")) target.alive = false;
            if (kind.equals("removed")) removed.add(target.ship);
            if (kind.equals("noncolliding")) target.collision = CollisionClass.NONE;
        }
        void advance(float dt) {
            missile.commands.clear();
            ((MissileAIPlugin)ai).advance(dt);
        }
    }

    private static final class Entity implements InvocationHandler {
        final Vector2f location = new Vector2f(), velocity = new Vector2f();
        final EnumSet<ShipCommand> commands = EnumSet.noneOf(ShipCommand.class);
        final ShipwideAIFlags flags = new ShipwideAIFlags();
        final MissileAPI missile;
        final ShipAPI ship;
        float facing, angularVelocity, maxSpeed = 2000, maxTurn = 180,
                turnAcceleration = 360, turnDeceleration = 180;
        int owner = 1;
        boolean alive = true, fizzling;
        CollisionClass collision = CollisionClass.SHIP;
        ShipAPI source, shipTarget;
        Entity(boolean isMissile) {
            missile = isMissile ? proxy(MissileAPI.class, this) : null;
            ship = isMissile ? null : proxy(ShipAPI.class, this);
        }
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "getLocation": return location;
                case "getMouseTarget": return location;
                case "getVelocity": return velocity;
                case "getFacing": return facing;
                case "getAngularVelocity": return angularVelocity;
                case "setAngularVelocity": angularVelocity = (Float)a[0]; return null;
                case "getMaxSpeed": return maxSpeed;
                case "getMaxRange": return 20000f;
                case "getMaxTurnRate": return maxTurn;
                case "getTurnAcceleration": return turnAcceleration;
                case "getEngineController": return proxy(ShipEngineControllerAPI.class,
                        (p2, m2, a2) -> m2.getName().equals("getTurnDeceleration")
                                ? turnDeceleration : defaultValue(p2, m2, a2));
                case "getEngineStats": return proxy(MutableShipStatsAPI.class,
                        (p2, m2, a2) -> m2.getName().equals("getMissileMaxSpeedBonus")
                                ? new StatBonus() : defaultValue(p2, m2, a2));
                case "getSource": return source;
                case "getShipTarget": return shipTarget;
                case "getAIFlags": return flags;
                case "isAlive": return alive;
                case "isFizzling": return fizzling;
                case "getCollisionClass": return collision;
                case "getOwner": return owner;
                case "getHullSize": return ShipAPI.HullSize.CRUISER;
                case "giveCommand": commands.add((ShipCommand)a[0]); return null;
                default: return defaultValue(p, m, a);
            }
        }

        void integrateAngular(float dt) {
            // Installed com.fs.starfarer.combat.entities.ship.null.processCommands:
            // turn command applies +/- acceleration then the max-turn limit;
            // no turn command brakes toward zero at getTurnDeceleration().
            int sign = commands.contains(ShipCommand.TURN_LEFT) ? 1
                    : commands.contains(ShipCommand.TURN_RIGHT) ? -1 : 0;
            if (sign == 0) {
                angularVelocity -= Math.signum(angularVelocity)
                        * Math.min(Math.abs(angularVelocity), turnDeceleration * dt);
            } else if (Math.abs(angularVelocity) > maxTurn) {
                angularVelocity -= Math.signum(angularVelocity)
                        * Math.min(Math.abs(angularVelocity), 2 * turnAcceleration * dt);
            } else {
                float next = angularVelocity + sign * turnAcceleration * dt;
                angularVelocity = Math.abs(next) > maxTurn ? sign * maxTurn : next;
            }
            facing = (facing + angularVelocity * dt) % 360f;
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler h) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, h));
    }
    private static Object defaultValue(Object p, Method m, Object[] a) {
        if (m.getName().equals("equals")) return p == a[0];
        if (m.getName().equals("hashCode")) return System.identityHashCode(p);
        if (m.getName().equals("toString")) return "TestProxy";
        Class<?> r = m.getReturnType();
        if (r == boolean.class) return false;
        if (r == float.class) return 0f;
        if (r == double.class) return 0d;
        if (r == int.class) return 0;
        if (r == long.class) return 0L;
        return null;
    }
    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
    private static void setField(Object o, String name, Object value) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, value);
    }
    private static IntervalUtil timer(Object o, String name) throws Exception {
        return (IntervalUtil)field(o, name);
    }
    private static float bearing(Vector2f v) {
        return (float)Math.toDegrees(Math.atan2(v.y, v.x));
    }
    private static void near(float actual, float expected, float tolerance, String detail) {
        require(Float.isFinite(actual) && Math.abs(actual - expected) <= tolerance,
                detail + ": expected " + expected + ", got " + actual);
    }
    private static void require(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
    private interface Checked { void run() throws Exception; }
    private static void run(String name, Checked body) {
        try {
            body.run();
            passed++;
        } catch (Throwable failure) {
            failed++;
            if (failed <= 25) {
                System.out.println("FAIL " + name + " -- " + failure);
                if (!(failure instanceof AssertionError)) failure.printStackTrace(System.out);
            }
        }
    }
}
