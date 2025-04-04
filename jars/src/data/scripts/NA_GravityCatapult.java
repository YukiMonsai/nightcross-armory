package data.scripts;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Ship;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class NA_GravityCatapult extends BaseShipSystemScript {



    public static float TIME_JUMP = 0.5f; // ms for jump
    public static float TIMEFLOW_MULT = 1f;
    public static float BASE_DIST = 500; // minimum distance to jump
    public static float BASE_DIST_ADD = 50; // added to distance
    public static float BASE_DIST_PER_SIZE = 100f; // higher for bigger
    public static float FAIL_DISTANCE = 100f;
    public static float MAX_RANGE = 600f; // max distance from target
    public static float SHIP_ALPHA_MULT = 0.5f;

    private String ID = "NA_GravityCatapult";

    private static Color COLOR_AFTERIMAGE = new Color(125, 75, 255, 255);
    private static Color color = new Color(125,75,255,255);

    private static final String IMPACT_SOUND = "hit_solid";
    private static final Color EXPLOSION_COLOR = new Color(200, 23, 253, 255);
    private static final float EXPLOSION_VISUAL_RADIUS = 150;

    public static class NA_GravityCatapultData {
        IntervalUtil interval = new IntervalUtil(TIME_JUMP, TIME_JUMP);
        Vector2f initialLoc = null;
        Vector2f targetLoc = null;
        float initialFacing = 0f;
        float targetFacing = 0f;
        ShipAPI target;
        Vector2f velocity = null;
        float ID = 0;
        public void reset(float time) {
            interval = new IntervalUtil(time, time);
        }
    }
    public static class NA_GravityCatapultDataIdle {
        IntervalUtil interval = new IntervalUtil(TIME_JUMP, TIME_JUMP);
        public void reset(float time) {
            interval = new IntervalUtil(time, time);
        }
    }



    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        String shipID = id + "_" + ship.getId();

        // Temporal shenanigans, slow down time because otherwise the player's perspective would move way too quick
        if (state == State.IDLE) {
            if (stats.getEntity() == Global.getCombatEngine().getPlayerShip()) {
                if(!ship.getSystem().isOutOfAmmo()) {
                    String key = ID + "_data2_" + ship.getId();
                    NA_GravityCatapultDataIdle data = (NA_GravityCatapultDataIdle) ship.getCustomData().get(key);

                    if (data == null) {
                        data = new NA_GravityCatapultDataIdle();
                        ship.setCustomData(key, data);
                    }

                    data.interval.advance(Global.getCombatEngine().getElapsedInLastFrame());

                    if (data.interval.intervalElapsed()) {
                        ShipAPI target = ship.getShipTarget();
                        if (target != null && this.isUsable(ship.getSystem(), ship)) {

                            float jumpDist = getJumpDist(ship, target);
                            double jumpAngle = Math.atan2(target.getLocation().y - ship.getLocation().y,
                                    target.getLocation().x - ship.getLocation().x);
                            ship.addAfterimage(COLOR_AFTERIMAGE,
                                    (float) Math.cos(jumpAngle) * jumpDist,
                                    (float) Math.sin(jumpAngle) * jumpDist,
                                    0f, 0f,
                                    0.1f * effectLevel,
                                    0f, TIME_JUMP, 0.5f, true, false, false);
                        }
                        data.reset(TIME_JUMP);
                    }
                }
            }

        } else if (state == State.COOLDOWN) {
            if (stats.getEntity() == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().getTimeMult().unmodify(shipID);
            }
            String key = ID + "_data_" + ship.getId();
            NA_GravityCatapultData data = (NA_GravityCatapultData) ship.getCustomData().get(key);
            if (data != null) {
                ship.getCustomData().remove(key);
            }
        } else {
            if (stats.getEntity() == Global.getCombatEngine().getPlayerShip()
                && ((ShipAPI) stats.getEntity()).isAlive()) {
                Global.getCombatEngine().getTimeMult().modifyMult(shipID, 1f/(1f + effectLevel*TIMEFLOW_MULT));
            } else {
                Global.getCombatEngine().getTimeMult().unmodify(shipID);
            }

            // Visual
            if (stats.getEntity() instanceof ShipAPI) {
                ship.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

                if (ship.getChildModulesCopy() != null && ship.getChildModulesCopy().size() > 0) {
                    for (ShipAPI child: ship.getChildModulesCopy()) {
                        child.getEngineController().fadeToOtherColor(child, color, new Color(192, 6, 243, 255), effectLevel, 1.0f);
                        child.getEngineController().extendFlame(child, 0.1f * effectLevel, 0.1f * effectLevel, 0.5f * effectLevel);
                        child.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());
                    }
                }

                List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
                if (maneuveringThrusters != null) {
                    for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                        ship.getEngineController().fadeToOtherColor(e.getEngineSlot(), color, new Color(192, 6, 243, 255), effectLevel, 1.0f);
                        ship.getEngineController().extendFlame(e.getEngineSlot(), 0.1f * effectLevel, 0.1f * effectLevel, 0.5f * effectLevel);
                    }
                }


            }

            // set the target

            ShipAPI target = null;


            String key = ID + "_data_" + ship.getId();
            NA_GravityCatapultData data = (NA_GravityCatapultData) ship.getCustomData().get(key);
            if (data != null) target = data.target;
            else {
                if (ship.getAIFlags() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM)) {
                    ShipAPI targ = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM);
                    if (targ != null) target = targ;
                }

                if (target == null) target = findTarget(ship, false);

            }





            if (ship != null && target != null) {

                if (data == null) {

                    float jumpDist = getJumpDist(ship, target);
                    double jumpAngle = Math.atan2(target.getLocation().y - ship.getLocation().y,
                            target.getLocation().x - ship.getLocation().x);

                    data = new NA_GravityCatapultData();
                    data.target = target;
                    ship.setCustomData(key, data);
                    data.initialLoc = ship.getLocation();
                    data.ID = MagicTrailPlugin.getUniqueID();
                    data.targetLoc = new Vector2f(
                            ship.getLocation().x + (float) (Math.cos(jumpAngle) * jumpDist),
                            ship.getLocation().y + (float) (Math.sin(jumpAngle) * jumpDist)
                    );
                    if (target.getOwner() != ship.getOwner())
                        data.targetFacing = VectorUtils.getAngle(data.targetLoc, data.initialLoc);
                    else
                        data.targetFacing = VectorUtils.getAngle(data.initialLoc, data.targetLoc);
                    data.initialFacing = ship.getFacing();

                    // slow down the target as well
                    target.getVelocity().set(target.getVelocity().x*0.5f, target.getVelocity().y*0.5f);

                    ship.getVelocity().set(0, 0);

                }

                if (state != State.ACTIVE) {
                    ship.setPhased(false);
                    return;
                }
                // assess the jump


                // Do the jump

                float t1 = data.interval.getElapsed();
                data.interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
                float t2 = data.interval.getElapsed();

                if (data != null && data.targetLoc != null && data.initialLoc != null) {

                    // nyoom
                    float delta = (t2 - t1)/TIME_JUMP;
                    float ease = t2/TIME_JUMP;
                    // easing function
                    //ease = (float) (1f/(1f + Math.exp(-10f * (ease - 0.5f))));
                    if (delta > 0) {


                        // engage warp drive
                        MagicTrailPlugin.addTrailMemberAdvanced(
                                ship, /* linkedEntity */
                                data.ID, /* ID */
                                Global.getSettings().getSprite("na_trails", "na_particletrail"), /* sprite */
                                ship.getLocation(), /* position */
                                0, /* startSpeed */
                                0, /* endSpeed */
                                ship.getFacing(), /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                150f, /* startSize */
                                150f, /* endSize */
                                new Color(175, 25, 255, 255), /* startColor */
                                new Color(75, 25, 175, 255), /* endColor */
                                1f, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                2.4f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                256f, /* textureLoopLength */
                                25f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                new Vector2f(), /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );

                        // phase thru
                        ship.setPhased(true);
                        float dx = 2f*(data.targetLoc.x - data.initialLoc.x) * delta;
                        float dy = 2f*(data.targetLoc.y - data.initialLoc.y) * delta;


                        float da = MathUtils.getShortestRotation(ship.getFacing(), data.targetFacing) * delta;
                        ship.getLocation().set(new Vector2f(
                                ship.getLocation().x + dx,
                                ship.getLocation().y + dy
                        ));
                        ship.setFacing(ship.getFacing() +
                                da
                        );

                        ship.setAngularVelocity(0f);

                        // quadratic shenanigans
                        ship.setExtraAlphaMult(0.5f);
                    }
                }

            }

        }
    }

    public static float getJumpDist(ShipAPI ship, ShipAPI target) {
        float distToTarget = MathUtils.getDistance(ship.getLocation(), target.getLocation()) + target.getCollisionRadius() + ship.getCollisionRadius();
        float jumpDist = Math.max(BASE_DIST, distToTarget + BASE_DIST_ADD);
        int size_that = 0;
        if (target != null) {
            switch (target.getHullSize()) {
                case CAPITAL_SHIP: size_that = 4; break;
                case CRUISER: size_that = 3; break;
                case DESTROYER: size_that = 2; break;
                case FRIGATE: size_that = 1; break;
            }
        }
        jumpDist += size_that * BASE_DIST_PER_SIZE;

        // this bit of code borrowed from Mayorate to tell if target location is occupied
        while (true) {
            double jumpAngle = Math.atan2(target.getLocation().y - ship.getLocation().y,
                    target.getLocation().x - ship.getLocation().x);
            float endLocX = ship.getLocation().y + (float) FastTrig.sin(jumpAngle) * jumpDist;
            float endLocY = ship.getLocation().y + (float) FastTrig.cos(jumpAngle) * jumpDist;
            Vector2f endLoc = new Vector2f(endLocX, endLocY);

            boolean collides = false;
            for (CombatEntityAPI inRangeObject : CombatUtils.getEntitiesWithinRange(endLoc, ship.getCollisionRadius() + 250f)) {
                if (inRangeObject == ship || inRangeObject == target) {
                    // don't do anything if its the ship activating the system
                    continue;
                }

                if (MathUtils.isWithinRange(inRangeObject, endLoc, ship.getCollisionRadius() + 50f)) {
                    collides = true;
                    break;
                }
            }
            if (collides) {
                jumpDist += FAIL_DISTANCE;
            } else {
                break;
            }
        }
        return  jumpDist;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        String shipID = id + "_" + ship.getId();

        Global.getCombatEngine().getTimeMult().unmodify(shipID);

        String key = ID + "_data_" + ship.getId();
        NA_GravityCatapultData data = (NA_GravityCatapultData) ship.getCustomData().get(key);
        if (data != null) {
            ship.setPhased(false);
            ship.getCustomData().remove(key);
        }
        ship.setExtraAlphaMult(1f);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            if (state == State.IN) {
                return new StatusData("Locking onto gravitational signature...", false);
            } else if (state == State.OUT || state == State.ACTIVE) {
                return new StatusData("Have a nice day :)", false);
            }
        }
        return null;
    }


    public static Vector2f getJumpPoint(ShipAPI ship, ShipAPI target) {
        float jumpDist = getJumpDist(ship, target);
        double jumpAngle = Math.atan2(target.getLocation().y - ship.getLocation().y,
                target.getLocation().x - ship.getLocation().x);
        return new Vector2f(
                ship.getLocation().x + ((float) Math.cos(jumpAngle) * jumpDist),
                ship.getLocation().y + ((float) Math.sin(jumpAngle) * jumpDist)
        );
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //if (true) return true;
        ShipAPI target = null;

        if (ship.getAIFlags() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM)) {
            ShipAPI targ = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM);
            if (targ != null) target = targ;
        }

        if (target == null) target = findTarget(ship, false);

        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
            float range = getMaxRange(ship);
            if (dist > range + radSum) return false;
            if (target.isFighter()) return false;
            return target != ship;
        }
        return false;
    }


    public static float getMaxRange(ShipAPI ship) {
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_RANGE);
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;

        ShipAPI target = findTarget(ship, false);
        if (target != null && target != ship) {
            return "READY";
        }
        if ((target == null) && ship.getShipTarget() != null) {
            return "OUT OF RANGE";
        }
        return "NO TARGET";
    }

    public static ShipAPI findTarget(ShipAPI ship, boolean friendly) {
        float range = getMaxRange(ship);
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        ShipAPI target = ship.getShipTarget();
        if (ship.getAIFlags() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM)) {
            ShipAPI targ = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM);
            if (targ != null) target = targ;
        }

        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
            if (dist > range + radSum) target = null;
            else if (target.isFighter() || !target.isAlive()) target = null;
        }
        if ((!player || friendly) && (target == null
                || (friendly && target.getOwner() != ship.getOwner()))) {
            if (friendly && target != null && target.getOwner() != ship.getOwner()) target = null;
            else if (!friendly && target != null && target.getOwner() == ship.getOwner()) target = null;
            if (target == null
                    || (!friendly && target.getOwner() == ship.getOwner())
                    || (friendly && target.getOwner() != ship.getOwner())) {
                if (friendly) {
                    // find closest friend to dash through
                    List<ShipAPI> ships = NAUtils.getShipsWithinRange(ship.getLocation(), MAX_RANGE);
                    float dist = MAX_RANGE;
                    for (ShipAPI s : ships) {
                        float dd = MathUtils.getDistance(ship, s);
                        if (s.getOwner() == ship.getOwner() && s.getId() != ship.getId() && dd < dist) {
                            dist = dd;
                            target = s;
                        }
                    }
                } else if (player) {
                    target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), ShipAPI.HullSize.FRIGATE, range, true);
                } else {
                    Object test = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
                    if (test instanceof ShipAPI) {
                        target = (ShipAPI) test;
                        float dist = MathUtils.getDistance(ship, target);
                        float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
                        if (dist > range + radSum) target = null;
                        else if (target.isFighter() || !target.isAlive()) target = null;
                    }
                }
            }
            if (!friendly && target == null) {
                target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), ShipAPI.HullSize.FRIGATE, range, true);
            }
        }

        return target;
    }
}
