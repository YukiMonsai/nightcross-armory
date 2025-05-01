package data.scripts.weapons.ai;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.CombatEngine;
import com.fs.starfarer.combat.entities.Ship;
import com.fs.starfarer.combat.entities.terrain.Asteroid;
import data.scripts.NAUtils;
import data.scripts.util.NAUtil;
import data.scripts.weapons.NA_BlackholeRenderer;
import data.scripts.weapons.NA_RKKVRenderer;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;
import org.magiclib.util.MagicTargeting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NA_RKKVAI implements MissileAIPlugin, GuidedMissileAI {

    private static CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private Vector2f lead = new Vector2f();
    private final IntervalUtil launchTimer = new IntervalUtil(0.2f, 0.4f);
    private final IntervalUtil deathTimer = new IntervalUtil(0.05f, 0.4f);

    private final float BEAM_TIME = 0.5f;
    private final IntervalUtil beamTimer = new IntervalUtil(BEAM_TIME, BEAM_TIME);
    // data
    private final float MAX_SPEED;
    private final float SLOW_SPEED = 500f;

    private float target_angle = 0f;
    // 0 - standoff
    // 1 - full send
    private int stage = 0;

    public final float MIN_RANGE = 2500f;

    public NA_RKKVAI(MissileAPI missile, ShipAPI ship) {
        if (layerRenderer == null || engine != Global.getCombatEngine()) {
            layerRenderer = new NA_RKKVRenderer();
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


    static NA_RKKVRenderer layerRenderer = null;


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

        updateTarget();

        // if the missile has no target, pick the nearest one
        if (target == null
                || (target instanceof ShipAPI && !((ShipAPI) target).isAlive())
                || !engine.isEntityInPlay(target)
                || target.getCollisionClass() == CollisionClass.NONE) {
            missile.giveCommand(stage < 2 ? ShipCommand.DECELERATE : ShipCommand.ACCELERATE);
            if (stage < 2 && missile.getSource() != null) {
                if (missile.getSource().getShipTarget() != null)
                    setTarget(missile.getSource().getShipTarget());
                else if (missile.getSource().getAIFlags() != null
                    && missile.getSource().getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) != null
                    && missile.getSource().getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) instanceof ShipAPI)
                    setTarget((ShipAPI) missile.getSource().getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET));

                if (stage != 1) clampTarget();
            }
            if (target == null) {
                setTarget(
                        MagicTargeting.pickTarget(
                                missile, MagicTargeting.targetSeeking.LOCAL_RANDOM,
                                (int) missile.getMaxRange(), getCone(),
                                0, 1, 4, 10, 20, true));
            }


            if (target == null) {
                target_angle = (float) (180f / Math.PI * Math.atan2(
                        missile.getVelocity().y,
                        missile.getVelocity().x
                ));

                float angle = MathUtils.getShortestRotation(
                        missile.getFacing(), target_angle);

                if (angle < 0) {
                    missile.giveCommand(ShipCommand.TURN_RIGHT);
                } else {
                    missile.giveCommand(ShipCommand.TURN_LEFT);
                }
            }


            return;
        }

        if (stage == 0)
        {

            target_angle = (float) (180f / Math.PI * Math.atan2(
                    missile.getLocation().y - target.getLocation().y,
                    missile.getLocation().x - target.getLocation().x
            ));

            float angle = MathUtils.getShortestRotation(
                    missile.getFacing(), target_angle);


            if (clampTarget() || target == null) {
                return;
            }

            if (angle < 0) {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            } else {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            }

            boolean allowAccel = true;
            if (!launchTimer.intervalElapsed()) {
                launchTimer.advance(amount);
                allowAccel = false;
            }

            if (Math.abs(angle) < 45 && missile.getVelocity().length() > SLOW_SPEED) allowAccel = false;

            // Damp angular velocity if the missile aim is getting close to the targeted angle
            float DAMPING = 0.1f;
            if (Math.abs(angle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
                missile.setAngularVelocity(angle / DAMPING);
                if (allowAccel)
                    missile.giveCommand(ShipCommand.ACCELERATE);
            }


            float distance = MathUtils.getDistance(missile, target.getLocation());
            if (distance > MIN_RANGE) {
                stage = 1;
                // gogogo
            }

        } else {
            // tracking



            float vmult = 0.1f;
            float pvmult = 0.5f;
            if (MathUtils.getDistance(target.getLocation(), missile.getLocation()) > (1.4f * missile.getVelocity().length())) {
                vmult = 0.2f;
                pvmult = 0.75f;
            }

            lead = leadPoint(
                    new Vector2f(target.getLocation()),
                    new Vector2f(target.getVelocity().x - vmult * missile.getVelocity().x, target.getVelocity().y - vmult * missile.getVelocity().y),
                    new Vector2f(missile.getLocation()), Math.max(250, missile.getVelocity().length()*pvmult));
            target_angle = (float) (180f / Math.PI * Math.atan2(
                    lead.y - missile.getLocation().y,
                    lead.x - missile.getLocation().x
            ));

            if (stage == 2 && (clampTarget() || target == null)) return;

            float angle = MathUtils.getShortestRotation(
                    missile.getFacing(), target_angle);

            if (angle < 0) {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            } else {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            }

            float DAMPING = stage == 1 ? 0.05f : 0.25f;
            if (Math.abs(angle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
                missile.setAngularVelocity(angle / DAMPING);
                missile.giveCommand(ShipCommand.ACCELERATE);
            } else if (stage == 1) {
                // decelerate
                missile.giveCommand(ShipCommand.DECELERATE);
            }

            if (stage == 1) {
                float velAngle = MathUtils.getShortestRotation(
                        VectorUtils.getFacing(missile.getVelocity()), target_angle);
                if (Math.abs(angle) < 20 && Math.abs(velAngle) < 10)
                    stage = 2;
                else {
                    float amt = 50f;
                    // decelerate
                    missile.getVelocity().set(
                            missile.getVelocity().x - amt * amount * (Math.signum(missile.getVelocity().x)),
                            missile.getVelocity().y - amt * amount * (Math.signum(missile.getVelocity().y)));
                }
            }


            if (stage == 2 && beamTimer.intervalElapsed()) {
                float dist = MathUtils.getDistance(missile.getLocation(),target.getLocation());

                // we also blow up asteroids in front if they are further than our target

                List<CombatEntityAPI> asteroids = NAUtils.getEntitiesWithinRange(missile.getLocation(), Math.min(350f, dist));

                for (CombatEntityAPI e : asteroids) {
                    if (e instanceof Asteroid) {
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
                        dist + 1000f,
                        VectorUtils.getFacing(missile.getVelocity()),
                        8f,
                        0f,
                        0.01f,
                        0f,
                        new Color(201, 0, 0, 175),
                        new Color(255, 0, 0, 200),
                        0f,
                        DamageType.ENERGY,
                        0f,
                        missile.getSource()
                );
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
            this.target = target;
            updateTarget();

        }
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
                        + (targetPoint.y - projPoint.y)
                        + (targetPoint.y - projPoint.y); // distance squared
        time = (float) Math.sqrt(time); // distance
        time /= projSpeed; // divided by proj speed

        Vector2f leadPoint = targetVel;
        leadPoint.scale(time);
        Vector2f.add(leadPoint, targetPoint, leadPoint);
        return leadPoint;
    }

}
