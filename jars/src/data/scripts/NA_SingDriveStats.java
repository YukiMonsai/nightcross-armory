package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.plugins.NAUtils;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class NA_SingDriveStats extends BaseShipSystemScript {

    public static final float SINGULARITY_PERIOD = 0.15f;
    IntervalUtil dmgTimer = new IntervalUtil(SINGULARITY_PERIOD, 1.25f*SINGULARITY_PERIOD);
    public static final float SINGULARITY_PULL_RADIUS = 1100f;
    public static final float SINGULARITY_PULL_MIN_RADIUS = 200f;
    public static final float SINGULARITY_PULL_STR = 1500000f;
    public static final float SPEED_BOOST = 105;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
        } else {

            stats.getMaxSpeed().modifyFlat(id, SPEED_BOOST * effectLevel);
            stats.getAcceleration().modifyFlat(id, SPEED_BOOST * effectLevel);
            stats.getZeroFluxSpeedBoost().modifyMult(id, 0);

            ShipAPI ship = (ShipAPI) stats.getEntity();
            if (ship == null) return;
            float amount = Global.getCombatEngine().getElapsedInLastFrame();
            this.dmgTimer.advance(amount);

            if (effectLevel > 0) {
                ship.setJitterShields(false);
                ship.setJitter(ship, new Color(40, 0, 120), effectLevel, 3, 20 + 15 * effectLevel);
                if (dmgTimer.intervalElapsed()) {
                    dmgTimer.randomize();

                    RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
                    ripple.setSize(SINGULARITY_PULL_RADIUS);
                    ripple.setIntensity(65.0F);
                    ripple.setFrameRate(-30);
                    ripple.setCurrentFrame(59);
                    ripple.fadeOutIntensity(0.75f);
                    DistortionShader.addDistortion(ripple);

                    doForce(amount, ship, SINGULARITY_PULL_RADIUS, SINGULARITY_PULL_MIN_RADIUS, SINGULARITY_PULL_STR);
                }

            }
            //stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
        }
    }


    public void doForce(float amount, ShipAPI ship, float radius, float minradius, float force) {
        Vector2f point = ship.getLocation();
        List<CombatEntityAPI> entities = NAUtils.getEntitiesWithinRange(point, radius);

        for (CombatEntityAPI e:entities) {
            if (e.getOwner() == ship.getOwner() && (e instanceof DamagingProjectileAPI || e instanceof MissileAPI)) continue;
            if (e instanceof ShipAPI && (((ShipAPI) e).isStation() || ((ShipAPI) e).isStationModule())) continue;
            if (e == ship) continue;
            float angle = VectorUtils.getAngle(e.getLocation(), point);
            if (Math.abs(MathUtils.getShortestRotation(
                    ship.getFacing(), angle)) < 90) {
                // e is behind. we shove them along
                if (e.getOwner() != ship.getOwner() || (e instanceof ShipAPI && ((ShipAPI)e).getEngineController().isAccelerating())) {
                    Vector2f closest = MathUtils.getPointOnCircumference(
                            Misc.ZERO, ship.getVelocity().length(),
                            ship.getFacing()
                    );
                    if (Math.abs(MathUtils.getShortestRotation(
                            ship.getFacing(), VectorUtils.getFacing(e.getVelocity()))) > 10
                            || e.getVelocity().length() < ship.getVelocity().length()) {
                        float dist = Math.max(minradius, MathUtils.getDistance(e.getLocation(), point));
                        float amt = amount/(dist*dist/(minradius*minradius)) * (2000f/(2000f + e.getMass())); // less effect on big ships
                        amt *= 0.03f;
                        e.getVelocity().set(
                                e.getVelocity().x + amount*closest.x*amt,
                                e.getVelocity().y + amount*closest.y*amt
                        );
                    }
                    // 'gravitational drag'
                    if (e instanceof ShipAPI) {
                        float len = e.getVelocity().length();
                        float maxlen = 1.5f*((ShipAPI) e).getMaxSpeed();
                        if (len > maxlen && maxlen > 1f) {
                            e.getVelocity().set(
                                    e.getVelocity().x * maxlen/len,
                                    e.getVelocity().y * maxlen/len
                            );
                        }
                    }
                }


            } else {
                // e is in front. succ
                Vector2f closest = MathUtils.getPointOnCircumference(
                        Misc.ZERO, force,
                        angle
                );
                float dist = Math.max(minradius, MathUtils.getDistance(e, ship.getLocation()));
                float amt = amount/(dist*dist/(minradius*minradius));
                if (dist > minradius && (!(e instanceof ShipAPI))) {
                    e.getVelocity().set(
                            e.getVelocity().x + amount*closest.x*amt,
                            e.getVelocity().y + amount*closest.y*amt
                    );
                }
                // 'gravitational drag'
                if (e instanceof ShipAPI) {
                    float len = e.getVelocity().length();
                    float maxlen = 1.5f*((ShipAPI) e).getMaxSpeed();
                    if (len > maxlen && maxlen > 1f) {
                        e.getVelocity().set(
                                e.getVelocity().x * maxlen/len,
                                e.getVelocity().y * maxlen/len
                        );
                    }
                }

            }
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getZeroFluxSpeedBoost().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("WARNING: EXTREME GRAVITATIONAL ANOMALY", false);
        }
        return null;
    }

}








