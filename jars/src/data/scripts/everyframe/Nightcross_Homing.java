package data.scripts.everyframe;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseSensorGhost;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.NAModPlugin;
import data.scripts.NAUtils;
import data.scripts.util.NAUtil;
import data.scripts.weapons.NA_CorrosionListener;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Nightcross_Homing extends BaseEveryFrameCombatPlugin {

    private static final String STUNPULSE_PROJ_ID = "na_stunpulse_shot";
    private static final float STUNPULSE_HOMING_AMT = 18f;
    private static final float STUNPULSE_HOMING_DIST = 700f;
    private static final float STUNPULSE_HOMING_SPEED = 650f;
    private static final float STUNPULSE_SPEED_FORCE_REDUCTION = 300f;

    private static final String PYROWISP_PROJ_ID = "na_pyrowisp_shot";
    private static final String PYROWISP_MEDIUM_PROJ_ID = "na_pyrowisp_medium_shot";
    private static final String PYROWISP_LARGE_PROJ_ID = "na_pyrowisp_large_shot";
    private static final float PYROWISP_HOMING_AMT = 4f;
    private static final float PYROWISP_LARGE_HOMING_AMT = 2f;
    private static final float PYROWISP_HOMING_DIST = 1000f;
    private static final float PYROWISP_HOMING_SPEED = 475f;
    private static final float PYROWISP_HOMING_VFACTOR = 0.75f;
    private static final float PYROWISP_SPEED_FORCE_REDUCTION = 175f;


    private static final String INTERCEPTOR_ID = "na_interceptor_shot";


    private static final float SIXTY_FPS = 1f / 60f;
    private static final Vector2f ZERO = new Vector2f();

    private static final String DATA_KEY = "Nightcross_Homing";

    private CombatEngineAPI engine;

    private final float TARGETTIME = 0.5f; // clear target matrix
    private final IntervalUtil targetTimer = new IntervalUtil(TARGETTIME, TARGETTIME);


    public HashMap<DamagingProjectileAPI, CombatEntityAPI> projtargets = new HashMap<>();

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) {
            return;
        }
        if (engine.isPaused()) {
            return;
        }

        targetTimer.advance(amount);
        if (targetTimer.intervalElapsed()) {
            targetTimer.setElapsed(0);
            projtargets = new HashMap<>();
        }


        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        int size = projectiles.size();

        float trailFPSRatio = 3f;

        for (int i = 0; i < size; i++) {
            DamagingProjectileAPI proj = projectiles.get(i);
            String spec = proj.getProjectileSpecId();
            if (spec == null) {
                continue;
            }

            float home_amount = 0f;
            float home_dist = 0f;
            float home_speed = 1000f;
            float speed_reduction = 0f;
            float vfactor = 0f;
            boolean requireShield = false;
            boolean homeMissiles = false;
            boolean requireTarget = false;
            switch (spec) {
                case STUNPULSE_PROJ_ID:
                    home_amount = STUNPULSE_HOMING_AMT;
                    speed_reduction = STUNPULSE_SPEED_FORCE_REDUCTION;
                    home_dist = STUNPULSE_HOMING_DIST;
                    home_speed = STUNPULSE_HOMING_SPEED;
                    requireShield = true;
                    requireTarget = true;
                    break;
                //case PYROWISP_PROJ_ID:
                //case PYROWISP_MEDIUM_PROJ_ID:
                case PYROWISP_LARGE_PROJ_ID:
                    home_amount = spec == PYROWISP_LARGE_PROJ_ID ? PYROWISP_LARGE_HOMING_AMT : PYROWISP_HOMING_AMT;
                    home_dist = Math.max(PYROWISP_HOMING_DIST, (proj.getWeapon() != null) ? proj.getWeapon().getRange() : PYROWISP_HOMING_DIST);
                    home_speed = PYROWISP_HOMING_SPEED;
                    vfactor = PYROWISP_HOMING_VFACTOR;
                    speed_reduction = spec == PYROWISP_LARGE_PROJ_ID ? 0f : PYROWISP_SPEED_FORCE_REDUCTION;
                    requireTarget = true;
                    break;
                case INTERCEPTOR_ID:
                    home_amount = 40f;
                    home_dist = 1200;
                    home_speed = 375f;
                    vfactor = 0.25f;
                    homeMissiles = true;
                    break;
                default:
                    continue;
            }


            if (home_amount > 0) {
                Vector2f prloc = proj.getLocation();
                Vector2f prvel = proj.getVelocity();
                Vector2f testLoc = prloc;
                if (vfactor != 0)
                    testLoc = new Vector2f(
                            testLoc.x + (proj.getVelocity().x) * vfactor,
                            testLoc.y + (proj.getVelocity().y) * vfactor);

                if (homeMissiles) {
                    List<DamagingProjectileAPI> targets = NAUtils.getProjectilesWithinRange(testLoc, home_dist);
                    DamagingProjectileAPI selectedTarget = null;
                    float targetDist = 100000;
                    if (projtargets.containsKey(proj) && projtargets.get(proj) instanceof DamagingProjectileAPI) {
                        float dd = MathUtils.getDistance(proj.getLocation(), projtargets.get(proj).getLocation());
                        if (dd <= home_dist) {
                            selectedTarget = (DamagingProjectileAPI) projtargets.get(proj);
                            targetDist = 0;
                        }
                    }
                    if (targetDist > 0) {
                        for (int ii = 0; ii < targets.size(); ii++) {
                            DamagingProjectileAPI tt = targets.get(ii);
                            if ((tt instanceof MissileAPI) && (proj.getSource() == null ||
                                    (
                                            // FF prevention
                                            tt.getOwner() != proj.getSource().getOwner()
                                    ))) {
                                float dist = MathUtils.getDistance(tt.getLocation(), testLoc);
                                if (dist < targetDist) {
                                    selectedTarget = tt;
                                    targetDist = dist;
                                }
                            }
                        }
                    }

                    if (selectedTarget == null) {
                        // allow projectiles too

                        for (int ii = 0; ii < targets.size(); ii++) {
                            DamagingProjectileAPI tt = targets.get(ii);
                            if (!(tt instanceof MissileAPI) && (proj.getSource() == null ||
                                    (
                                            // FF prevention
                                            tt.getOwner() != proj.getSource().getOwner()
                                    ))) {
                                float dist = MathUtils.getDistance(tt.getLocation(), testLoc);
                                if (dist < targetDist) {

                                    Vector2f relativeLoc = new Vector2f(prloc.x - tt.getLocation().x, prloc.y - tt.getLocation().y);
                                    if (Vector2f.dot(prvel, relativeLoc) > 0) {
                                        selectedTarget = tt;
                                        targetDist = dist;
                                    }
                                }
                            }
                        }
                    }
                    if (selectedTarget != null) {
                        Vector2f ttloc = selectedTarget.getLocation();
                        Vector2f relativeLoc = new Vector2f(prloc.x - ttloc.x, prloc.y - ttloc.y);
                        if (vfactor != 0 && relativeLoc.length() > home_speed) {
                            ttloc = new Vector2f(
                                    ttloc.x + (selectedTarget.getVelocity().x - 0.5f*proj.getVelocity().x) * vfactor,
                                    ttloc.y + (selectedTarget.getVelocity().y - 0.5f*proj.getVelocity().y) * vfactor);
                        }
                        float speed = Math.max(proj.getMoveSpeed(), home_speed);

                        if (speed_reduction > 0 && speed > 0) {
                            home_amount *= speed / (speed_reduction + speed);
                        }

                        projtargets.put(proj, selectedTarget);

                        Vector2f vecPush = MathUtils.getPointOnCircumference(
                                ZERO,
                                home_amount*amount,
                                (float) Math.toDegrees(Math.atan2(ttloc.y - prloc.y, ttloc.x - prloc.x)));
                        proj.getVelocity().x += vecPush.x;
                        proj.getVelocity().y += vecPush.y;

                        if (proj.getVelocity().length() > 5f)
                            proj.setFacing((float) Math.toDegrees(Math.atan2(proj.getVelocity().y, proj.getVelocity().x)));

                        float len = proj.getVelocity().length();
                        if (len > speed && len > 0) {
                            proj.getVelocity().x *= speed/len;
                            proj.getVelocity().y *= speed/len;
                        }
                    }
                } else {
                    List<ShipAPI> targets = NAUtils.getShipsWithinRange(testLoc, home_dist);
                    ShipAPI selectedTarget = null;
                    float targetDist = 100000;
                    if (projtargets.containsKey(proj) && projtargets.get(proj) instanceof ShipAPI) {
                        float dd = MathUtils.getDistance(proj.getLocation(), projtargets.get(proj).getLocation());
                        if (dd <= home_dist) {
                            selectedTarget = (ShipAPI) projtargets.get(proj);
                            targetDist = 0;
                        }
                    }
                    if (targetDist > 0) {
                        for (int ii = 0; ii < targets.size(); ii++) {
                            ShipAPI tt = targets.get(ii);
                            if (!tt.isFighter() && !tt.isHulk() && tt.isAlive() && (proj.getSource() == null ||
                                    (
                                            // FF prevention
                                            tt.getOwner() != proj.getSource().getOwner()
                                    )) && (!requireShield || (tt.getShield() != null && tt.getShield().isOn()))) {
                                float dist = MathUtils.getDistance(tt.getLocation(), testLoc);
                                if (dist < targetDist) {

                                    Vector2f relativeLoc = new Vector2f(prloc.x - tt.getLocation().x, prloc.y - tt.getLocation().y);
                                    if (Vector2f.dot(prvel, relativeLoc) > 0) {
                                        selectedTarget = tt;
                                        targetDist = dist;
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTarget == null && proj.getSource() != null && proj.getSource().getShipTarget() != null) {
                        selectedTarget = proj.getSource().getShipTarget();
                        if (selectedTarget.isFighter() || selectedTarget.isHulk() || !selectedTarget.isAlive())
                            selectedTarget = null;
                        else if (requireShield && (selectedTarget.getShield() == null || !selectedTarget.getShield().isOn()))
                            selectedTarget = null;
                        else if (proj.getSource() != null && selectedTarget.getOwner() != proj.getSource().getOwner())
                            selectedTarget = null;
                        else {
                            Vector2f relativeLoc = new Vector2f(prloc.x - selectedTarget.getLocation().x, prloc.y - selectedTarget.getLocation().y);
                            if (Vector2f.dot(prvel, relativeLoc) <= 0) {
                                selectedTarget = null;
                            }
                        }
                    }

                    if (requireTarget && selectedTarget != null) {
                        // only home on ship target
                        if (proj.getSource() != null && proj.getSource().getShipTarget() != selectedTarget) {
                            selectedTarget = null;
                        }
                    }
                    if (selectedTarget != null) {
                        Vector2f ttloc = selectedTarget.getLocation();
                        if (vfactor != 0) {
                            ttloc = new Vector2f(
                                    ttloc.x + (selectedTarget.getVelocity().x - 0.5f*proj.getVelocity().x) * vfactor,
                                    ttloc.y + (selectedTarget.getVelocity().y - 0.5f*proj.getVelocity().y) * vfactor);
                        }
                        ShipAPI src = proj.getSource();
                        float speed = Math.max(proj.getMoveSpeed(), home_speed);

                        if (speed_reduction > 0 && speed > 0) {
                            home_amount *= speed / (speed_reduction + speed);
                        }


                        projtargets.put(proj, selectedTarget);

                        Vector2f vecPush = MathUtils.getPointOnCircumference(
                                ZERO,
                                home_amount*amount,
                                (float) Math.toDegrees(Math.atan2(ttloc.y - prloc.y, ttloc.x - prloc.x)));
                        proj.getVelocity().x += vecPush.x;
                        proj.getVelocity().y += vecPush.y;

                        if (proj.getVelocity().length() > 25f)
                            proj.setFacing((float) Math.toDegrees(Math.atan2(proj.getVelocity().y, proj.getVelocity().x)));

                        float len = proj.getVelocity().length();
                        if (len > speed && len > 0) {
                            proj.getVelocity().x *= speed/len;
                            proj.getVelocity().y *= speed/len;
                        }
                    }
                }

            }
        }

    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public static void createIfNeeded() {
        if (!NAModPlugin.hasMagicLib) {
            return;
        }

        if (Global.getCombatEngine() != null) {
            if (!Global.getCombatEngine().getCustomData().containsKey(DATA_KEY)) {
                Global.getCombatEngine().addPlugin(new Nightcross_Homing());
            }
        }
    }

}
