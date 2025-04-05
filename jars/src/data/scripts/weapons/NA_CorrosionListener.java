package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.PlaySound;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.NAUtils;
import data.scripts.util.NAUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class NA_CorrosionListener extends BaseEveryFrameCombatPlugin {

    public boolean destroyed = false;
    private String key;
    private Object source;
    DamagingProjectileAPI proj;
    public static final float SINGULARITY_DMG = 200;
    public static final float SINGULARITY_EMP = 500;
    public static final float SINGULARITY_DURATION = 4f;
    public static final float SINGULARITY_DMG_RADIUS = 200f;
    public static final DamageType SINGULARITY_DMG_TYPE = DamageType.HIGH_EXPLOSIVE;
    public static final float SINGULARITY_VISUAL_DUR = 0.2f;

    public static final String SINGULARITY_SOUND = "na_blackhole_loop";
    private SoundAPI sound;

    IntervalUtil dmgTimer = new IntervalUtil(SINGULARITY_PERIOD, SINGULARITY_PERIOD);
    public static final float SINGULARITY_PERIOD = 0.2f;
    public static final float SINGULARITY_PULL_RADIUS = 700f;
    public static final float SINGULARITY_PULL_MIN_RADIUS = 25f;
    public static final float SINGULARITY_PULL_STR = 1200000f;
    IntervalUtil endTimer = new IntervalUtil(SINGULARITY_DURATION, SINGULARITY_DURATION);
    IntervalUtil visTimer = new IntervalUtil(SINGULARITY_VISUAL_DUR, SINGULARITY_VISUAL_DUR*2f);
    IntervalUtil flareTimer = new IntervalUtil(NA_BlackholeRenderer.FLARE_TIME, NA_BlackholeRenderer.FLARE_TIME);

    private IntervalUtil minimumArmTimer = new IntervalUtil(0.55f, 0.55f);


    public Vector2f lastLocation;

    public NA_CorrosionListener(DamagingProjectileAPI proj, String id, Object source) {
        super();
        this.proj = proj;
        this.key = id;
        this.lastLocation = proj.getLocation();
        this.source = source;

        Global.getCombatEngine().addPlugin(this);

        if (layerRenderer != null) {
            if (!layerRenderer.missiles.containsKey(this)) {
                layerRenderer.missiles.put(this, this);
            }
        }
    }

    static NA_BlackholeRenderer layerRenderer = null;

    @Override
    public void init(CombatEngineAPI engine) {
        layerRenderer = new NA_BlackholeRenderer();
        engine.addLayeredRenderingPlugin(layerRenderer);
    }



    public void createExplosion() {
        //NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
        //        new Color(75, 100, 255), 32f);
        //RiftCascadeMineExplosion.spawnStandardRift(proj, p);

        Global.getCombatEngine().addSwirlyNebulaParticle(proj.getLocation(),
                Misc.ZERO,
                2f*SINGULARITY_DMG_RADIUS,
                1.25f,
                0.24f,
                0.25f,
                SINGULARITY_DURATION, new Color(95, 10, 67),
                true);
    }
    public void doEndState(float amount) {

        this.endTimer.advance(amount);

        if (sound != null && sound.isPlaying()) {
            float progress = endTimer.getElapsed()/NA_CorrosionListener.SINGULARITY_DURATION;
            progress = Math.min(1f, 5*progress) - Math.max(0, 2f*5f*(progress - 0.8f));
            sound.setVolume(Math.max(0.1f, progress));
        }

        this.dmgTimer.advance(amount);
        this.visTimer.advance(amount);

        if (endTimer.intervalElapsed()) {
            destroy();

        } else if (armed) {



            if (dmgTimer.intervalElapsed()) {
                dmgTimer.setElapsed(0f);
                NAUtils.doDamage(lastLocation, SINGULARITY_DMG_RADIUS, SINGULARITY_DMG,
                        SINGULARITY_EMP, SINGULARITY_DMG_TYPE, false, true, source, false);

                //if (endTimer.getElapsed() < 2.5f) {
                RippleDistortion ripple = new RippleDistortion(proj.getLocation(), Misc.ZERO);
                ripple.setSize(SINGULARITY_DMG_RADIUS);
                ripple.setIntensity(80.0F);
                ripple.setFrameRate(-30);
                ripple.setCurrentFrame(59);
                ripple.fadeOutIntensity(.5F);
                DistortionShader.addDistortion(ripple);
                //}


                float str = SINGULARITY_PULL_STR;
                if (layerRenderer != null && layerRenderer.blackholes.size() > 1) {
                    str *= 2f / (2f + Math.max(1f, Math.min(5f, (float) layerRenderer.blackholes.size())));
                } // weaken the pull if we spam them
                doForce(amount, lastLocation, SINGULARITY_PULL_RADIUS, SINGULARITY_PULL_MIN_RADIUS, str);
            }
            if (visTimer.intervalElapsed() && (endTimer.getElapsed() - SINGULARITY_DURATION < -1f)) {
                visTimer.randomize();
                createExplosion();
            }
        }
    }

    public void doForce(float amount, Vector2f point, float radius, float minradius, float force) {
        List<CombatEntityAPI> entities = NAUtils.getEntitiesWithinRange(point, radius);

        for (CombatEntityAPI e:entities) {
            float angle = (float) (Math.atan2(point.y - e.getLocation().y, point.x - e.getLocation().x)* 180f / Math.PI);
            Vector2f closest = MathUtils.getPointOnCircumference(
                    Misc.ZERO, force,
                    angle
            );
            float dist = Math.max(minradius, MathUtils.getDistance(e, point));
            float amt = amount/(dist*dist/(minradius*minradius));
            if (dist > minradius) {
                e.getVelocity().set(
                        e.getVelocity().x + amount*closest.x*amt,
                        e.getVelocity().y + amount*closest.y*amt
                );
            }
            // 'gravitational drag'
            float len = e.getVelocity().length();
            float maxlen = 1.5f*((ShipAPI) e).getMaxSpeed();
            if (len > maxlen && maxlen > 1f) {
                e.getVelocity().set(
                        e.getVelocity().x * len/maxlen,
                        e.getVelocity().y + len/maxlen
                );
            }
        }
    }


    public void destroy() {
        // removes the plugin
        Global.getCombatEngine().removePlugin(this);
        Global.getCombatEngine().getCustomData().remove(key);

        if (layerRenderer != null) {
            if (layerRenderer.blackholes.containsKey(this)) {
                layerRenderer.blackholes.remove(this);
            }
        }

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
                            SINGULARITY_SOUND, 1f, 1, this.lastLocation != null ? this.lastLocation : proj.getLocation(), Misc.ZERO);

                    if (layerRenderer != null) {
                        if (!layerRenderer.blackholes.containsKey(this)) {
                            layerRenderer.blackholes.put(this, this);
                        }
                    }

                    WaveDistortion ripple = new WaveDistortion(proj.getLocation(), Misc.ZERO);
                    ripple.setSize(256.0F);
                    ripple.setIntensity(40.0F);
                    ripple.fadeInSize(0.5F);
                    ripple.fadeOutIntensity(3.5F);
                    DistortionShader.addDistortion(ripple);

                    NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
                            new Color(75, 100, 255), 128f);
                    RiftCascadeMineExplosion.spawnStandardRift(proj, p);
                }
                createExplosion();

            } else {
                doEndState(amount);
            }
        } else {
            if (armed && !flareTimer.intervalElapsed())
                this.flareTimer.advance(amount);
            this.lastLocation = proj.getLocation();

            if (minimumArmTimer.intervalElapsed()) armed = true;
            else minimumArmTimer.advance(amount);
            if (layerRenderer != null) {
                layerRenderer.missiles.put(this, this);
            }
        }
    }

}
