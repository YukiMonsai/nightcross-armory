package data.scripts.weapons.ai;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTargeting;

public class NA_HomingLaserAI implements MissileAIPlugin, GuidedMissileAI {

    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private Vector2f lead = new Vector2f();
    /**
     * -1 = left
     * 0 = straight
     * 1 = right
     * **/
    private int stage = 0;
    private final IntervalUtil launchTimer = new IntervalUtil(0.8f, 1.4f);
    private IntervalUtil waveTimer = new IntervalUtil(0.4f, 0.6f);
    private final IntervalUtil deathTimer = new IntervalUtil(0.05f, 0.4f);
    // data
    private final float MAX_SPEED;


    private float target_angle = 0f;
    private final float STAGE_ONE_ANGLE = 30f;
    private final float ANGLE_MIN_ZIGZAG = 25f;
    private final float ANGLE_MAX_ZIGZAG = 40f;

    public NA_HomingLaserAI(MissileAPI missile, ShipAPI ship) {
        this.missile = missile;
        MAX_SPEED = missile.getMaxSpeed();
        launchTimer.randomize();


        // left or right
        if (missile.getWeapon() != null && missile.getWeapon().getSlot() != null) {
            WeaponSlotAPI slot = missile.getWeapon().getSlot();

            stage = slot.getLocation().x > 0 ? -1 : 1;
        } else {
            stage = MathUtils.getRandomNumberInRange(0, 1f) > 0.5 ? -1 : 1;
        }

        deathTimer.randomize();

        target_angle = missile.getFacing() + (float) (stage * (STAGE_ONE_ANGLE));
    }

    @Override
    public void advance(float amount) {

        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }

        // skip AI if the missile is engineless or the game paused
        if (Global.getCombatEngine().isPaused() || missile.isFading() || missile.isFizzling()) {
            if (missile.isFizzling()) {
                if (deathTimer.intervalElapsed()) {
                    // boom
                    missile.explode();
                    missile.setHitpoints(0f);
                } else {
                    deathTimer.advance(amount);
                }

            }
            return;
        }


        if (!launchTimer.intervalElapsed())
        {
            launchTimer.advance(amount);
            // gentle curve

            missile.giveCommand(ShipCommand.ACCELERATE);

            float angle = MathUtils.getShortestRotation(
                            missile.getFacing(), target_angle);

            if (angle < 0) {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            } else {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            }

            // Damp angular velocity if the missile aim is getting close to the targeted angle
            float DAMPING = 0.2f;
            if (Math.abs(angle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
                missile.setAngularVelocity(angle / DAMPING);
            }
        } else {
            // tracking and zigzag

            waveTimer.advance(amount);

            // if the missile has no target, pick the nearest one
            if (target == null
                    || (target instanceof ShipAPI && !((ShipAPI) target).isAlive())
                    || !engine.isEntityInPlay(target)
                    || target.getCollisionClass() == CollisionClass.NONE) {
                missile.giveCommand(ShipCommand.ACCELERATE);
                setTarget(
                        MagicTargeting.pickTarget(
                                missile, MagicTargeting.targetSeeking.LOCAL_RANDOM,
                                1250, 360,
                                1, 2, 2, 2, 2, true));
                if (waveTimer.intervalElapsed()) {
                    waveTimer = new IntervalUtil(0.5f, 0.8f);
                }
                return;
            }


            if (waveTimer.intervalElapsed())
            {
                float Tmult = 1;

                // recalculate lead point
                float maxDist = 300f;
                float midDist = 600f;
                float longDist = 1200f;
                float dd = MathUtils.getDistance(target, missile.getLocation());
                if (dd > maxDist) {
                    lead =
                            leadPoint(
                                    target.getLocation(),
                                    new Vector2f(target.getVelocity()),
                                    missile.getLocation(),
                                    MAX_SPEED);

                    target_angle = VectorUtils.getAngle(missile.getLocation(), lead);
                    float dist = MathUtils.getDistance(missile.getLocation(), target.getLocation());
                    if (dist > 200f + target.getCollisionRadius() && (
                        waveTimer.getMaxInterval() < 1f    ||
                        Math.abs(MathUtils.getShortestRotation(
                            VectorUtils.getFacing(missile.getVelocity()),
                            target_angle
                    )) < 120f)) {
                        float mult = MathUtils.getRandomNumberInRange(0, 1) > 0.5 ? -1 : 1;
                        if (waveTimer.getMinInterval() < missile.getSpec().getMaxFlightTime() * 0.5f) {
                            if (dd > longDist || dd < midDist) {
                                // factor weapon range
                                Tmult += 0.8f
                                        * (missile.getSource() == null ? 1f :
                                        missile.getWeaponSpec().getMaxRange() / Math.max(100, missile.getSource().getMutableStats().getEnergyWeaponRangeBonus().computeEffective(
                                                missile.getWeaponSpec().getMaxRange()
                                        )));

                                target_angle += mult * (MathUtils.getRandomNumberInRange(0, 0.25f*ANGLE_MAX_ZIGZAG));
                            } else {
                                Tmult += 0.5f
                                        * (missile.getSource() == null ? 1f :
                                        missile.getWeaponSpec().getMaxRange() / Math.max(100, missile.getSource().getMutableStats().getEnergyWeaponRangeBonus().computeEffective(
                                                missile.getWeaponSpec().getMaxRange()
                                        )));
                                target_angle += mult * (MathUtils.getRandomNumberInRange(ANGLE_MIN_ZIGZAG, ANGLE_MAX_ZIGZAG));
                            }
                        }

                    } else target_angle = VectorUtils.getFacing(missile.getVelocity());
                }

                if (waveTimer.intervalElapsed()) {
                    waveTimer = new IntervalUtil(waveTimer.getMinInterval() * Tmult, waveTimer.getMaxInterval() * Tmult);
                }


            }
            float angle = MathUtils.getShortestRotation(
                    missile.getFacing(), target_angle);
            // actually go
            missile.giveCommand(ShipCommand.ACCELERATE);
            /*if (angle < 0) {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            } else {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            }*/

            // harshmotion
            missile.setFacing(target_angle);
            missile.setAngularVelocity(0f);
            float speed = missile.getVelocity().length();
            missile.getVelocity().set(
                    (float) (speed*Math.cos(Math.toRadians(target_angle))),
                    (float) (speed*Math.sin(Math.toRadians(target_angle)))
            );
        }
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
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
