package data.scripts;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.loading.V;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NA_ReversalDrive extends BaseShipSystemScript {


    private static float DMG_AREA = 300f;
    private static float DMG_AMT = 800f;
    private static DamageType DMG_TYPE = DamageType.ENERGY;

    private String ID = "NA_ReversalDrive";

    private static Color COLOR_AFTERIMAGE = new Color(125, 75, 255, 255);
    private static Color color = new Color(125,75,255,255);

    private static float TIME_AFTERIMAGE = 0.1f;
    private static float TIME_STEPS_MAX = 30; // 3 seconds

    private boolean activated = false;


    public static Color JITTER_COLOR = new Color(50,50,255,75);
    public static Color JITTER_UNDER_COLOR = new Color(100,100,255,155);


    private static class NA_ReversalDriveData {
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
                return true;
            }
        }
        return false;
    }


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        String shipID = id + "_" + ship.getId();


        if (stats.getEntity() instanceof ShipAPI) {
            if (ship != null) {
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

                if (state == State.COOLDOWN || state == State.IDLE) {
                    if (activated) {
                        activated = false;
                        ship.setPhased(false);
                    }
                    // TRACK
                    data.interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
                    if (data.interval.intervalElapsed()) {
                        data.add(new Vector3f(
                                ship.getLocation().x,
                                ship.getLocation().y,
                                ship.getFacing()
                        ));
                        if (lastPoint != null) {
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
                    data.use(TIME_AFTERIMAGE);
                } else if (state == State.IN) {
                    activated = true;
                    ship.setPhased(true);
                }

                if (effectLevel > 0) {
                    ship.getVelocity().set(0f, 0f);
                    Color color = JITTER_COLOR;
                    ship.setJitterUnder(KEY_SHIP, JITTER_UNDER_COLOR, effectLevel, 15, 0f, 3f + 50f);
                    ship.setJitter(KEY_SHIP, color, effectLevel, 4, 0f, 0 + 50 * 1f);
                }


            }
        }
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
