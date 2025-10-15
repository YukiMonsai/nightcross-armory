package data.scripts;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.plugins.NAUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NA_ReversalDrive extends BaseShipSystemScript {

    protected static final Color HARDLIGHT_TRAIL_COLOR_START = new Color(226, 255, 251);
    protected static final Color HARDLIGHT_TRAIL_COLOR_END = new Color(83, 78, 238);

    protected static float DMG_AREA = 300f;
    protected static float DMG_AMT = 800f;
    protected static DamageType DMG_TYPE = DamageType.ENERGY;

    protected String ID = "NA_ReversalDrive";

    protected static Color COLOR_AFTERIMAGE = new Color(125, 75, 255, 255);
    protected static Color color = new Color(125,75,255,255);


    protected boolean activated = false;

    protected float systemID = 0;


    public static Color JITTER_COLOR = new Color(50,50,255,75);
    public static Color JITTER_UNDER_COLOR = new Color(100,100,255,155);

    public ShipAPI ship = null;

    public CollisionClass OriginalClass = null;

    public Vector3f getLastPoint() {
        if (ship == null) return null;
        String key = ID + "_data_" + ship.getId();
        NA_ReversalDriveData data = (NA_ReversalDriveData) ship.getCustomData().get(key);
        if (data == null) {
            data = new NA_ReversalDriveData();
            ship.setCustomData(key, data);
        }

        Vector3f lastPoint = null;
        if (data.positions.size() > 1) {
            lastPoint = data.positions.get(data.positions.size()-1);
        }
        return lastPoint;
    }

    private static float TIME_AFTERIMAGE = 0.1f;
    private static float REVERT_TIME = 2f;
    private static float TIME_STEPS_MAX = 20; // 3 seconds
    protected static class NA_ReversalDriveData {
        IntervalUtil interval = new IntervalUtil(TIME_AFTERIMAGE, TIME_AFTERIMAGE);
        List<Vector3f> positions = new ArrayList<>();

        // Add a point
        public void add(Vector3f point) {
            positions.add(0, point);
            if (positions.size() > TIME_STEPS_MAX) {
                positions.remove((int) (TIME_STEPS_MAX));
            }
        }
        public void use(float time) {
            interval = new IntervalUtil(time, time);
            positions = new ArrayList<>();
        }

        public void reset(float time) {
            interval = new IntervalUtil(time, time);
        }

    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship != null) {
            String key = ID + "_data_" + ship.getId();
            NA_ReversalDriveData data = (NA_ReversalDriveData) ship.getCustomData().get(key);
            if (data == null) {
                data = new NA_ReversalDriveData();
                ship.setCustomData(key, data);
            }

            if (data.positions.size() > 1) {
                Vector3f lp = getLastPoint();
                return lp == null || NAUtils.getShipsWithinRange(new Vector2f(lp.x, lp.y), 5f).size() == 0;
            }
        }
        return false;
    }


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        if (this.ship == null) this.ship = ship;
        if (OriginalClass == null) OriginalClass = ship.getCollisionClass();

        String shipID = id + "_" + ship.getId();


        if (systemID == 0f)
            systemID = MagicTrailPlugin.getUniqueID();

        if (stats.getEntity() instanceof ShipAPI) {
            if (ship != null) {
                String key = ID + "_data_" + ship.getId();
                NA_ReversalDriveData data = (NA_ReversalDriveData) ship.getCustomData().get(key);
                if (data == null) {
                    data = new NA_ReversalDriveData();
                    ship.setCustomData(key, data);
                }

                Vector3f lastPoint = getLastPoint();

                if (state == State.COOLDOWN || state == State.IDLE) {
                    if (activated) {
                        activated = false;
                        if (!ship.isPhased()) {
                            ship.setCollisionClass(OriginalClass);
                        }
                    }

                    if (ship.getOwner() == 0)
                        MagicTrailPlugin.addTrailMemberAdvanced(
                            ship, /* linkedEntity */
                                systemID, /* ID */
                            Global.getSettings().getSprite("na_trails", "na_hardlighttrail"), /* sprite */
                            ship.getLocation(), /* position */
                            0f, /* startSpeed */
                            0f, /* endSpeed */
                            ship.getFacing(), /* angle */
                            0f, /* startAngularVelocity */
                            0f, /* endAngularVelocity */
                            ship.getCollisionRadius()*1.0f, /* startSize */
                            25f, /* endSize */
                            HARDLIGHT_TRAIL_COLOR_START, /* startColor */
                            HARDLIGHT_TRAIL_COLOR_END, /* endColor */
                            0.3f, /* opacity */
                            0.25f, /* inDuration */
                                0.25f, /* mainDuration */
                                1.25f, /* outDuration */
                            GL11.GL_SRC_ALPHA, /* blendModeSRC */
                            GL11.GL_ONE_MINUS_SRC_ALPHA, /* blendModeDEST */
                            256f, /* textureLoopLength */
                            16f, /* textureScrollSpeed */
                            -1, /* textureOffset */
                            Misc.ZERO, /* offsetVelocity */
                            null, /* advancedOptions */
                            CombatEngineLayers.BELOW_SHIPS_LAYER, /* layerToRenderOn */
                            1f /* frameOffsetMult */
                        );

                    // TRACK
                    data.interval.advance(Global.getCombatEngine().getElapsedInLastFrame()
                        / Math.max(0.01f, ship.getMutableStats().getTimeMult().getModifiedValue()));
                    if (data.interval.intervalElapsed()) {
                        data.add(new Vector3f(
                                ship.getLocation().x,
                                ship.getLocation().y,
                                ship.getFacing()
                        ));
                        if (lastPoint != null && state == State.IDLE) {
                            if (Global.getCombatEngine().getPlayerShip() != null
                                    && ship.getId() == Global.getCombatEngine().getPlayerShip().getId())
                                MagicRender.battlespace(
                                        Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                                    new Vector2f(lastPoint.x, lastPoint.y),
                                    Misc.ZERO,
                                    new Vector2f(ship.getSpriteAPI().getHeight(), ship.getSpriteAPI().getWidth()),
                                    Misc.ZERO,
                                    lastPoint.z - 90,
                                    0, COLOR_AFTERIMAGE,
                                    true,
                                    TIME_AFTERIMAGE,
                                    TIME_AFTERIMAGE*0.5f,
                                    TIME_AFTERIMAGE
                                );

                        }

                        data.reset(TIME_AFTERIMAGE);
                    }
                } else if (effectLevel == 1) {
                    // USE
                    if (lastPoint != null) {
                        ship.getLocation().set(lastPoint.x, lastPoint.y);
                        ship.setFacing(lastPoint.z);
                    }
                    MagicTrailPlugin.cutTrailsOnEntity(ship);
                    data.use(TIME_AFTERIMAGE);
                    ship.setCollisionClass(CollisionClass.NONE);
                } else if (effectLevel > 0) {
                    if (!activated) {
                        MagicRender.battlespace(
                            Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                            new Vector2f(lastPoint.x, lastPoint.y),
                            Misc.ZERO,
                            new Vector2f(ship.getSpriteAPI().getHeight(), ship.getSpriteAPI().getWidth()),
                            Misc.ZERO,
                            lastPoint.z - 90,
                                0, Color.WHITE,
                            true,
                            0.4f,
                            0f,
                            0.1f
                        );
                    }
                    activated = true;
                    ship.setCollisionClass(CollisionClass.NONE);

                }

                if (effectLevel > 0) {
                    ship.getVelocity().set(0f, 0f);
                    Color color = JITTER_COLOR;
                    ship.setJitterUnder(KEY_SHIP, JITTER_UNDER_COLOR, effectLevel, 15, 0f, 3f + 50f);
                    ship.setJitter(KEY_SHIP, color, effectLevel, 4, 0f, 0 + 50 * 1f);
                }


            }
        } else if (OriginalClass != null && ship.getCollisionClass() != OriginalClass) ship.setCollisionClass(OriginalClass);
    }

    public static Object KEY_SHIP = new Object();

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {

        String shipID = id + "_" + ((ShipAPI)(stats.getEntity())).getId();

        Global.getCombatEngine().getTimeMult().unmodify(shipID);

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        String key = ID + "_data_" + ship.getId();
        if ((NA_ReversalDriveData) ship.getCustomData().get(key) != null) {
            ship.getCustomData().remove(key);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("WARNING temporal anomaly", false);
        }
        return null;
    }
}
