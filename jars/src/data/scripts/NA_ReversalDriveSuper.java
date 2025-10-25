package data.scripts;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.plugins.NAUtils;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NA_ReversalDriveSuper extends NA_ReversalDrive {

    public static float DMG_AREA = 300f;
    public static float DMG_AMT = 800f;
    public static float DMG_AMT_SCALE = 1200f;
    public static float DMG_AMT_SCALETIME = 4f;
    public static DamageType DMG_TYPE = DamageType.ENERGY;
    public static IntervalUtil dmgInterval = new IntervalUtil(DMG_AMT_SCALETIME, DMG_AMT_SCALETIME);


    private String ID = "NA_ReversalDriveW";
    private String expl_sound = "na_rift_explosion";

    private static float TIME_AFTERIMAGE = 0.1f;
    private static float REVERT_TIME = 4.0f;
    private static float TIME_STEPS_MAX = 40; // 3 seconds
    public CollisionClass OriginalClass = null;

    protected static class NA_ReversalDriveSuperData extends NA_ReversalDriveData {
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
    public Vector3f getLastPoint() {
        if (ship == null) return null;
        String key = ID + "_data_" + ship.getId();
        NA_ReversalDriveSuperData data = (NA_ReversalDriveSuperData) ship.getCustomData().get(key);
        if (data == null) {
            data = new NA_ReversalDriveSuperData();
            ship.setCustomData(key, data);
        }

        Vector3f lastPoint = null;
        if (data.positions.size() > 1) {
            lastPoint = data.positions.get(data.positions.size()-1);
        }
        return lastPoint;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship != null) {
            String key = ID + "_data_" + ship.getId();
            NA_ReversalDriveSuperData data = (NA_ReversalDriveSuperData) ship.getCustomData().get(key);
            if (data == null) {
                data = new NA_ReversalDriveSuperData();
                ship.setCustomData(key, data);
            }

            if (data.positions.size() > 1) {
                Vector3f lp = getLastPoint();
                return lp == null || NAUtils.getShipsWithinRange(new Vector2f(lp.x, lp.y), 5f).size() == 0;
            }
        }
        return false;
    }

    public float getDmg() {
        return DMG_AMT + DMG_AMT_SCALE * Math.min(1f, Math.max(0f, dmgInterval.getElapsed()/DMG_AMT_SCALETIME));
    }

    public void doDmg(Vector2f point) {

        Global.getSoundPlayer().playSound(expl_sound, 1f, 1.2f, point, Misc.ZERO);

        WaveDistortion ripple = new WaveDistortion(point, Misc.ZERO);
        ripple.setSize(DMG_AREA);
        ripple.setIntensity(40.0F);
        ripple.fadeInSize(0.5F);
        ripple.fadeOutIntensity(3.5F);
        DistortionShader.addDistortion(ripple);

        CombatEngineAPI engine = Global.getCombatEngine();
        engine.addNegativeSwirlyNebulaParticle(
                point,
                Misc.ZERO,
                DMG_AREA + 10f,
                1.5f,
                0.6f,
                0.2f,
                2.5f,
                new Color(216, 246, 44)
        );
        engine.addSmoothParticle(
                point,
                Misc.ZERO,
                DMG_AREA + 100f,
                1.1f,
                0.1f,
                1.25f,
                new Color(252, 249, 253)
        );

        engine.addNegativeParticle(point, Misc.ZERO, 70f, 0f, 0.2f, Color.white);
        engine.addNegativeParticle(point, Misc.ZERO, 50f, 0.1f, 0.15f, Color.white);
        engine.addNegativeNebulaParticle(point, Misc.ZERO, 40f, 2f, 0.2f, 0f, 0.4f, Color.white);

        List<ShipAPI> enemiesNearby = NAUtils.getEnemyShipsWithinRange(ship, point, DMG_AREA, true);
        List<MissileAPI> missilesNearby = NAUtils.getMissilesWithinRange(point, DMG_AREA);

        float dmg = getDmg();
        dmgInterval = new IntervalUtil(DMG_AMT_SCALETIME, DMG_AMT_SCALETIME);

        for (MissileAPI missile : missilesNearby) {
            // just do the damage
            Global.getCombatEngine().applyDamage(
                    missile, missile.getLocation(), dmg, DMG_TYPE, 0f, false, false, ship
            );
        }
        for (ShipAPI trg : enemiesNearby) {
            Vector2f tpoint = CollisionUtils.getNearestPointOnBounds(
                    MathUtils.getPointOnCircumference(trg.getLocation(), trg.getCollisionRadius(),
                            VectorUtils.getAngle(trg.getLocation(), point)), trg);
            Global.getCombatEngine().applyDamage(
                    trg, tpoint, dmg, DMG_TYPE, 0f, false, false, ship, true
            );
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        if (this.ship == null) this.ship = ship;
        if (OriginalClass == null) OriginalClass = ship.getCollisionClass();

        TIME_AFTERIMAGE = 0.1f;
        REVERT_TIME = 4.0f;
        TIME_STEPS_MAX = 40; // 3 seconds

        String shipID = id + "_" + ship.getId();


        if (systemID == 0f)
            systemID = MagicTrailPlugin.getUniqueID();

        if (stats.getEntity() instanceof ShipAPI) {
            if (ship != null) {
                String key = ID + "_data_" + ship.getId();
                NA_ReversalDriveSuperData data = (NA_ReversalDriveSuperData) ship.getCustomData().get(key);
                if (data == null) {
                    data = new NA_ReversalDriveSuperData();
                    ship.setCustomData(key, data);
                }

                Vector3f lastPoint = null;
                if (data.positions.size() > 1) {
                    lastPoint = data.positions.get(data.positions.size()-1);
                }

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
                            1.0f, /* inDuration */
                            0.25f, /* mainDuration */
                            1.75f, /* outDuration */
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
                            dmgInterval.advance(Global.getCombatEngine().getElapsedInLastFrame() * ship.getMutableStats().getTimeMult().getModifiedValue());
                            if (Global.getCombatEngine().getPlayerShip() != null
                                    && ship.getId().equals(Global.getCombatEngine().getPlayerShip().getId()))
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
                        doDmg(new Vector2f(lastPoint.x, lastPoint.y));


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
                    ship.setJitterShields(false);
                    ship.setJitterUnder(KEY_SHIP, JITTER_UNDER_COLOR, effectLevel, 15, 0f, 3f + 50f);
                    ship.setJitter(KEY_SHIP, color, effectLevel, 4, 0f, 0 + 50 * 1f);
                }


            }
        } else if (OriginalClass != null && ship.getCollisionClass() != OriginalClass) ship.setCollisionClass(OriginalClass);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("stored system dmg: " + (Math.round(getDmg()/100)*100), false);
        }
        return null;
    }
}
