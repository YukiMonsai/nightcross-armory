package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.plugins.NAUtils;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class NA_DarkSunListener extends BaseEveryFrameCombatPlugin {

    public boolean destroyed = false;
    private String key;
    private Object source;
    DamagingProjectileAPI proj;
    public static final float EXPL_RADIUS = 300f;
    public static final float SHOOT_TIMER = 0.8f;
    public static final float SHOOT_TIMER_CLOSE = 0.45f;
    public static final float END_TIME = 1.5f;
    public static final float END_SUBTIME = 0.15f;
    public static final float SHOOT_RANGE = 1700f;
    IntervalUtil shootTimer = new IntervalUtil(SHOOT_TIMER_CLOSE, SHOOT_TIMER_CLOSE);
    IntervalUtil dmgTimer = new IntervalUtil(END_SUBTIME, END_SUBTIME);
    IntervalUtil endTimer = new IntervalUtil(END_TIME, END_TIME);

    public static final String LOOP_SOUND = "na_blackhole_loop";
    private SoundAPI sound;
    public Vector2f lastLocation;

    public NA_DarkSunListener(DamagingProjectileAPI proj, String id, Object source) {
        super();
        this.proj = proj;
        this.key = id;
        this.source = source;
        this.lastLocation = proj.getLocation();

        Global.getCombatEngine().addPlugin(this);

    }

    @Override
    public void init(CombatEngineAPI engine) {

    }



    public void createExplosion() {
        //NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
        //        new Color(75, 100, 255), 32f);
        //RiftCascadeMineExplosion.spawnStandardRift(proj, p);


        RippleDistortion ripple = new RippleDistortion(proj.getLocation(), Misc.ZERO);
        ripple.setSize(EXPL_RADIUS);
        ripple.setIntensity(40.0F);
        ripple.fadeOutIntensity(.5F);
        DistortionShader.addDistortion(ripple);
    }
    public void doEndState(float amount) {

        this.endTimer.advance(amount);

        if (sound != null && sound.isPlaying()) {
            float progress = endTimer.getElapsed()/ NA_DarkSunListener.END_TIME;
            progress = Math.min(1f, 5*progress) - Math.max(0, 2f*5f*(progress - 0.8f));
            sound.setVolume(Math.max(0.1f, progress));
        }

        this.dmgTimer.advance(amount);

        if (endTimer.intervalElapsed()) {
            destroy();

        } else if (armed) {



            if (dmgTimer.intervalElapsed() && endTimer.getElapsed() < END_TIME) {
                dmgTimer.setElapsed(0f);

                EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                params.segmentLengthMult = 4f;

                params.glowSizeMult = 0.5f;
                params.brightSpotFadeFraction = 0.33f;
                params.brightSpotFullFraction = 0.5f;
                params.movementDurMax = 0.2f;
                params.flickerRateMult = 0.5f;

                EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(lastLocation, proj,
                        MathUtils.getPointOnCircumference(lastLocation, EXPL_RADIUS + MathUtils.getRandomNumberInRange(0f, EXPL_RADIUS * 0.5f), MathUtils.getRandomNumberInRange(0f, 360f)),
                        proj,
                        5, // thickness
                        new Color(85, 0, 0),
                        new Color(255, 25, 52),
                        params
                );
                //arc.setCoreWidthOverride(thickness * coreWidthMult);
                arc.setSingleFlickerMode(true);
                arc.setUpdateFromOffsetEveryFrame(true);

                Global.getCombatEngine().addSwirlyNebulaParticle(proj.getLocation(),
                        Misc.ZERO,
                        2f*EXPL_RADIUS,
                        1.25f,
                        0.24f,
                        0.25f,
                        8f, new Color(95, 10, 67, 25),
                        true);
                //if (endTimer.getElapsed() < 2.5f) {
                //}

            }
        }
    }



    public void destroy() {
        // removes the plugin
        Global.getCombatEngine().removePlugin(this);
        Global.getCombatEngine().getCustomData().remove(key);


        if (sound != null && sound.isPlaying()) {
            sound.stop();
        }
    }

    private SpriteAPI sprite = null;
    static boolean doOnce = true;
    public boolean doEndOnce = false;

    public boolean armed = false;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine().isPaused()) return;

        // check if the projectile is alive or not
        if (destroyed || proj == null || proj.didDamage() || proj.isFading() || !Global.getCombatEngine().isEntityInPlay(proj)) {
            if (!destroyed || doEndOnce) {
                destroyed = true;
                doEndOnce = false;



                if (armed) {

                    sound = Global.getSoundPlayer().playSound(
                            LOOP_SOUND, 1f, 1, this.lastLocation != null ? this.lastLocation : proj.getLocation(), Misc.ZERO);


                    WaveDistortion ripple = new WaveDistortion(proj.getLocation(), Misc.ZERO);
                    ripple.setSize(256.0F);
                    ripple.setIntensity(40.0F);
                    ripple.fadeInSize(0.5F);
                    ripple.fadeOutIntensity(3.5F);
                    DistortionShader.addDistortion(ripple);

                    NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
                            new Color(75, 100, 255), 64);
                    RiftCascadeMineExplosion.spawnStandardRift(proj, p);
                }
                createExplosion();

            } else {
                doEndState(amount);
            }
        } else {
            this.lastLocation = proj.getLocation();

            doSelfLoop(amount);

            armed = true;
        }
    }

    public void doShootLoop(float amount) {

        this.shootTimer.advance(amount);
        if (shootTimer.intervalElapsed()) {
            shootTimer.setElapsed(MathUtils.getRandomNumberInRange(0.0f, 0.2f));

            // look for a target and shoot
            ShipAPI target = null;
            if (proj.getSource() != null && proj.getSource().getShipTarget() != null) {
                target = proj.getSource().getShipTarget();
            }

            if (target != null) {
                float d2 = MathUtils.getDistanceSquared(proj.getLocation(), target.getLocation());
                if (d2 > SHOOT_RANGE*SHOOT_RANGE) {
                    target = null;
                }
            }
            // target seeking if none found

            if (target == null) {
                List<ShipAPI> targets = NAUtils.getEnemyShipsWithinRange(proj, proj.getLocation(), SHOOT_RANGE, false);

                if (!targets.isEmpty()) {
                    float nearestdist = SHOOT_RANGE * SHOOT_RANGE * 4;
                    ShipAPI nearest = null;

                    for (ShipAPI tt: targets) {
                        float dist = MathUtils.getDistanceSquared(proj.getLocation(), tt.getLocation())
                                // prioritize closer to mothership
                                + (proj.getSource() != null ? MathUtils.getDistanceSquared(proj.getSource().getLocation(), tt.getLocation()) : 0);

                        if (dist < nearestdist) {
                            nearestdist = dist;
                            nearest = tt;
                        }
                    }

                    target = nearest;
                }
            }



            // actual shoot
            if (target != null) {
                float d2 = MathUtils.getDistanceSquared(proj.getLocation(), target.getLocation());
                if (d2 < SHOOT_RANGE*SHOOT_RANGE) {



                    NA_CorrosionMoteEffect.releaseMissile(NA_StargazerStardust.getSwarmFor(proj), proj, target.getLocation(), SHOOT_RANGE);


                    // faster shooting if close
                    if (d2 < 0.25 * SHOOT_RANGE * SHOOT_RANGE) {
                        shootTimer.setElapsed(SHOOT_TIMER-SHOOT_TIMER_CLOSE + MathUtils.getRandomNumberInRange(0.0f, 0.2f));
                    }
                }
            }

        }
    }



    public void doSelfLoop(float amount) {
        doShootLoop(amount);

        this.dmgTimer.advance(amount);
        if (dmgTimer.intervalElapsed() && endTimer.getElapsed() < END_TIME) {
            dmgTimer.setElapsed(0f);

            Global.getCombatEngine().addSwirlyNebulaParticle(proj.getLocation(),
                    new Vector2f(proj.getVelocity().x * 0.5f, proj.getVelocity().y * 0.5f),
                    96f,
                    1.25f,
                    0.24f,
                    0.25f,
                    2f, new Color(95, 10, 67),
                    true);
            //if (endTimer.getElapsed() < 2.5f) {
            //}

        }
    }

}


