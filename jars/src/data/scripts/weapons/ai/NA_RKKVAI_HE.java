package data.scripts.weapons.ai;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.plugins.NAUtils;
import data.scripts.weapons.NA_RKKVRenderer_HE;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicTargeting;

import java.awt.*;
import java.util.List;

public class NA_RKKVAI_HE implements MissileAIPlugin, GuidedMissileAI {

    private static CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private Vector2f lead = new Vector2f();
    private final IntervalUtil launchTimer = new IntervalUtil(0.2f, 0.4f);
    private final IntervalUtil deathTimer = new IntervalUtil(0.05f, 0.4f);

    private final float BEAM_TIME = 0.5f;
    private final float TARGETTIME = 0.5f;
    private final IntervalUtil beamTimer = new IntervalUtil(BEAM_TIME, BEAM_TIME);
    private final IntervalUtil targetTimer = new IntervalUtil(TARGETTIME, TARGETTIME);
    // data
    private final float MAX_SPEED;
    private final float SLOW_SPEED = 500f;


    private final float TRIGGER_DIST = 1000f;
    private final float TRIGGER_SUBS = 15f;
    private final float TRIGGER_ANGLE = 30f;
    private final String TRIGGER_WPN = "na_rkkv_he_dummy";

    private float target_angle = 0f;
    // 0 - standoff
    // 1 - full send
    private int stage = 0;

    public final float MIN_RANGE = 5000f;

    public NA_RKKVAI_HE(MissileAPI missile, ShipAPI ship) {
        if (layerRenderer == null || engine != Global.getCombatEngine()) {
            layerRenderer = new NA_RKKVRenderer_HE();
            Global.getCombatEngine().addLayeredRenderingPlugin(layerRenderer);
        }

        engine = Global.getCombatEngine();

        if (layerRenderer != null) {
            if (!layerRenderer.missiles.containsKey(this)) {
                layerRenderer.missiles.put(missile, null);
            }
        }

        this.missile = missile;
        MAX_SPEED = missile.getMaxSpeed();
        launchTimer.randomize();


        // left or right
        deathTimer.randomize();

        target_angle = missile.getFacing();
    }


    static NA_RKKVRenderer_HE layerRenderer = null;


    private int getCone() {
        // TODO shrink cone depending on speed
        return stage < 2 ? 360 : 30;
    }

    @Override
    public void advance(float amount) {

        CombatEngineAPI engine = Global.getCombatEngine();


        // skip AI if the missile is engineless or the game paused
        if (Global.getCombatEngine().isPaused() || missile.isFading() || missile.isFizzling()) {
            if (missile.isFading() || missile.isFizzling())
                this.setTarget(null);
            return;
        }

        targetTimer.advance(amount);
        updateTarget();

        // if the missile has no target, pick the nearest one
        if (!isValidTarget(target)) {
            // Clear stale references before inheriting a target or running the fallback search.
            setTarget(null);
            missile.giveCommand(stage < 2 ? ShipCommand.DECELERATE : ShipCommand.ACCELERATE);
            if (stage < 2 && missile.getSource() != null) {
                if (stage < 1 || missile.getSource() == engine.getPlayerShip()) {
                    if (missile.getSource().getShipTarget() != null)
                        setTarget(missile.getSource().getShipTarget());
                    else if (missile.getSource().getAIFlags() != null
                            && missile.getSource().getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) != null
                            && missile.getSource().getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) instanceof ShipAPI)
                        setTarget((ShipAPI) missile.getSource().getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET));

                }

                if (stage != 1) clampTarget();
            }
            if (target == null) {
                if (targetTimer.intervalElapsed()) {
                    targetTimer.setElapsed(0f);
                    setTarget(
                            MagicTargeting.pickTarget(
                                    missile, MagicTargeting.targetSeeking.LOCAL_RANDOM,
                                    (int) missile.getMaxRange()*2, getCone(),
                                    0, 1, 4, 10, 20, true));
                }

            }


            if (target == null) {
                target_angle = (float) (180f / Math.PI * Math.atan2(
                        missile.getVelocity().y,
                        missile.getVelocity().x
                ));

                float angle = MathUtils.getShortestRotation(
                        missile.getFacing(), target_angle);

                if (angle < -10f) {
                    missile.giveCommand(ShipCommand.TURN_RIGHT);
                } else if (angle > 10f) {
                    missile.giveCommand(ShipCommand.TURN_LEFT);
                }
            }


            return;
        }

        if (stage == 0)
        {
            List<CombatEntityAPI> asteroids = NAUtils.getEntitiesWithinRange(missile.getLocation(), 350f);

            for (CombatEntityAPI e : asteroids) {
                if (e instanceof CombatAsteroidAPI) {
                    float ang = MathUtils.getShortestRotation(
                            VectorUtils.getAngle(Misc.ZERO, missile.getVelocity()), VectorUtils.getAngle(missile.getLocation(), e.getLocation()));
                    if (Math.abs(ang) < 25) {
                        e.setHitpoints(0); // blow up the asteroid
                    }
                }

            }


            target_angle = (float) (180f / Math.PI * Math.atan2(
                    missile.getLocation().y - target.getLocation().y,
                    missile.getLocation().x - target.getLocation().x
            ));

            float angle = MathUtils.getShortestRotation(
                    missile.getFacing(), target_angle);


            if (clampTarget() || target == null) {
                return;
            }

            boolean allowAccel = true;
            if (!launchTimer.intervalElapsed()) {
                launchTimer.advance(amount);
                allowAccel = false;
            }

            if (Math.abs(angle) > 45 && missile.getVelocity().length() > SLOW_SPEED) allowAccel = false;

            // Damp angular velocity if the missile aim is getting close to the targeted angle

            if (steer(angle, 0.1f, amount)) {
                if (allowAccel)
                    missile.giveCommand(ShipCommand.ACCELERATE);
            }


            float distance = MathUtils.getDistance(missile, target.getLocation());
            if (distance > MIN_RANGE / missile.getEngineStats().getMissileMaxSpeedBonus().computeEffective(1)) {
                stage = 1;
                // gogogo
            }

        } else {
            // tracking



            float vmult = 0.4f;
            float pvmult = 0.55f;
            if (MathUtils.getDistance(target.getLocation(), missile.getLocation()) > (1.4f * missile.getVelocity().length())) {
                vmult = 0.75f;
                pvmult = 0.7f;
            }

            // leadPoint returns a world position: use target world velocity and the
            // missile's available speed, not relative velocity or launch speed.
            lead = leadPoint(
                    new Vector2f(target.getLocation()),
                    new Vector2f(vmult * target.getVelocity().x, vmult * target.getVelocity().y),
                    new Vector2f(missile.getLocation()), Math.max(1f, missile.getMaxSpeed() * pvmult));
            target_angle = (float) (180f / Math.PI * Math.atan2(
                    lead.y - missile.getLocation().y,
                    lead.x - missile.getLocation().x
            ));

            if (stage == 2 && (clampTarget() || target == null)) return;

            float angle = MathUtils.getShortestRotation(
                    missile.getFacing(), target_angle);

            float DAMPING = stage == 1 ? 0.03f : 0.2f;
            if (steer(angle, DAMPING, amount)) {
                missile.giveCommand(ShipCommand.ACCELERATE);
            } else if (stage == 1) {
                // decelerate
                missile.giveCommand(ShipCommand.DECELERATE);
            }

            if (stage == 1) {
                float velAngle = MathUtils.getShortestRotation(
                        VectorUtils.getFacing(missile.getVelocity()), target_angle);
                if (Math.abs(angle) < 20 && Math.abs(velAngle) < 20)
                    stage = 2;
                else {
                    float amt = 50f;
                    // decelerate
                    missile.getVelocity().set(
                            missile.getVelocity().x - amt * amount * (Math.signum(missile.getVelocity().x)),
                            missile.getVelocity().y - amt * amount * (Math.signum(missile.getVelocity().y)));
                }

                List<CombatEntityAPI> asteroids = NAUtils.getEntitiesWithinRange(missile.getLocation(), 350f);

                for (CombatEntityAPI e : asteroids) {
                    if (e instanceof CombatAsteroidAPI) {
                        float ang = MathUtils.getShortestRotation(
                                VectorUtils.getAngle(Misc.ZERO, missile.getVelocity()), VectorUtils.getAngle(missile.getLocation(), e.getLocation()));
                        if (Math.abs(ang) < 25) {
                            e.setHitpoints(0); // blow up the asteroid
                        }
                    }

                }
            }


            if (stage == 2 && beamTimer.intervalElapsed()) {
                float dist = MathUtils.getDistance(missile.getLocation(),target.getLocation());

                // we also blow up asteroids in front if they are further than our target

                List<CombatEntityAPI> asteroids = NAUtils.getEntitiesWithinRange(missile.getLocation(), Math.min(350f, dist));

                for (CombatEntityAPI e : asteroids) {
                    if (e instanceof CombatAsteroidAPI) {
                        float ang = MathUtils.getShortestRotation(
                                VectorUtils.getAngle(Misc.ZERO, missile.getVelocity()), VectorUtils.getAngle(missile.getLocation(), e.getLocation()));
                        if (Math.abs(ang) < 25) {
                            e.setHitpoints(0); // blow up the asteroid
                        }
                    }

                }


                // render a beam to help the player dodge
                MagicFakeBeam.spawnFakeBeam(
                        Global.getCombatEngine(),
                        missile.getLocation(),
                        Math.min(dist + 1000f, 5000f),
                        VectorUtils.getFacing(missile.getVelocity()),
                        6f,
                        0f,
                        0.05f,
                        0f,
                        new Color(201, 0, 0, 175),
                        new Color(255, 0, 0, 200),
                        0f,
                        DamageType.ENERGY,
                        0f,
                        missile.getSource()
                );

                if (target != null && MathUtils.getDistance(missile, target) <= TRIGGER_DIST) {
                    missile.setHitpoints(0); // boom

                    for (float ang = -TRIGGER_ANGLE; ang <= TRIGGER_ANGLE; ang += (2 * TRIGGER_ANGLE / TRIGGER_SUBS)) {
                        var scale = MathUtils.getRandomNumberInRange(0.3f- 0.07f * (Math.abs(ang) / TRIGGER_ANGLE), 0.6f);
                        CombatEntityAPI proj = Global.getCombatEngine().spawnProjectile(missile.getSource(), null,
                                TRIGGER_WPN,
                                missile.getLocation(),
                                Misc.getAngleInDegrees(Misc.ZERO, missile.getVelocity()) + ang,
                                new Vector2f(missile.getVelocity().x * scale, missile.getVelocity().y * scale));
                        if (proj instanceof MissileAPI) ((MissileAPI) proj).setEmpResistance(4);
                        Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(missile.getSource(),
                                WeaponAPI.WeaponType.MISSILE, false, ((DamagingProjectileAPI) proj).getDamage());
                        proj.setMass(250f);
                        engine.addSmoothParticle(missile.getLocation(),
                                Misc.ZERO,
                                420, //60-75
                                0.1f,
                                0.2f,
                                0.27f,
                                new Color(255, 255, 255, 55));
                        engine.addSwirlyNebulaParticle(missile.getLocation(),
                                MathUtils.getPointOnCircumference(Misc.ZERO, 75, VectorUtils.getFacing(missile.getVelocity())),
                                220, //60-75
                                3.5f,
                                0.5f,
                                0.7f,
                                4.5f,
                                new Color(40, 54, 64, 55), true);

                        MagicLensFlare.createSharpFlare(engine, missile.getSource(), missile.getLocation(), 1, 250, 0, new Color(255, 255, 255, 200), new Color(255, 55, 55, 255));
                        RippleDistortion ripple2 = new RippleDistortion(missile.getLocation(), Misc.ZERO);
                        ripple2.setSize(300);
                        ripple2.setIntensity(25.0F);
                        ripple2.setFrameRate(15);
                        ripple2.setCurrentFrame(0);
                        ripple2.fadeOutIntensity(1.5f);
                        DistortionShader.addDistortion(ripple2);

                        Global.getSoundPlayer().playSound("dragonfire_payload_fire", 1.0f, 0.7f, missile.getLocation(), Misc.ZERO);
                    }
                }

                beamTimer.setElapsed(0);
            } else {
                beamTimer.advance(amount);
            }
        }
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        // RKKVs are not affected by flares
        if (!(target instanceof MissileAPI)) {
            this.target = isValidTarget(target) ? target : null;
            updateTarget();

        }
    }

    private boolean isValidTarget(CombatEntityAPI candidate) {
        return candidate != null
                && !(candidate instanceof MissileAPI)
                && (!(candidate instanceof ShipAPI) || ((ShipAPI) candidate).isAlive())
                && Global.getCombatEngine().isEntityInPlay(candidate)
                && candidate.getCollisionClass() != CollisionClass.NONE;
    }

    /** Apply a turn without stepping past the heading during a slow update. */
    private boolean steer(float angle, float damping, float amount) {
        float step = Math.max(0f, amount);
        float horizon = Math.max(damping, step);
        // The engine applies turn acceleration after the AI's commands, so include it
        // when deciding whether a turn would overshoot, even when angular speed is zero.
        float possibleRate = Math.min(missile.getMaxTurnRate(),
                Math.abs(missile.getAngularVelocity()) + missile.getTurnAcceleration() * step);
        if (Math.abs(angle) <= possibleRate * horizon) {
            float desiredRate = angle / horizon;
            // With no turn command the engine brakes automatically. Compensate for
            // that braking so its final rate is desiredRate, without queuing a turn.
            float braking = missile.getEngineController().getTurnDeceleration() * step;
            missile.setAngularVelocity(desiredRate + Math.signum(desiredRate) * braking);
            return true;
        }
        missile.giveCommand(angle < 0f ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT);
        return false;
    }

    private boolean clampTarget() {
        if (target == null) return false;
        if (missile.getVelocity().length() < SLOW_SPEED + 50f
            || (stage == 0 && missile.getVelocity().length() < SLOW_SPEED * 1.5f)) return false;
        float velAngle = MathUtils.getShortestRotation(
                VectorUtils.getFacing(missile.getVelocity()), target_angle);
        if (Math.abs(velAngle) > getCone()) {
            setTarget(null);
            return true;
        }
        return false;
    }

    private void updateTarget() {
        if (layerRenderer != null) {
            if (!layerRenderer.missiles.containsKey(this)
                    || layerRenderer.missiles.get(this) != this.target) {
                layerRenderer.missiles.put(missile, this.target);
            }
        }
    }
    private Vector2f leadPoint(
            Vector2f targetPoint, Vector2f targetVel, Vector2f projPoint, float projSpeed) {
        float time =
                (targetPoint.x - projPoint.x) * (targetPoint.x - projPoint.x)
                        + (targetPoint.y - projPoint.y) * (targetPoint.y - projPoint.y); // distance squared
        time = (float) Math.sqrt(time); // distance
        time /= projSpeed; // divided by proj speed

        Vector2f leadPoint = targetVel;
        leadPoint.scale(time);
        Vector2f.add(leadPoint, targetPoint, leadPoint);
        return leadPoint;
    }

}
