package data.scripts.weapons.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import data.scripts.weapons.NA_NovaMote;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class NA_NovaMoteAI implements MissileAIPlugin {

    public static float MAX_FLOCK_RANGE = 500;
    public static float MAX_HARD_AVOID_RANGE = 200;
    public static float AVOID_RANGE = 50;
    public static float COHESION_RANGE = 100;

    public static float ATTRACTOR_LOCK_STOP_FLOCKING_ADD = 300f;

    protected MissileAPI missile;

    protected IntervalUtil tracker = new IntervalUtil(0.05f, 0.1f);

    protected IntervalUtil updateListTracker = new IntervalUtil(0.05f, 0.1f);
    protected List<MissileAPI> missileList = new ArrayList<MissileAPI>();
    protected List<CombatEntityAPI> hardAvoidList = new ArrayList<CombatEntityAPI>();

    protected float r;

    protected CombatEntityAPI target;
    protected NA_NovaMote.SharedMoteAIData data;



    public NA_NovaMoteAI(MissileAPI missile, ShipAPI ship) {
        this.missile = missile;
        r = (float) Math.random();
        elapsed = -(float) Math.random() * 0.5f;

        data = NA_NovaMote.getSharedData(missile.getSource());

        updateHardAvoidList();
    }

    public void updateHardAvoidList() {
        hardAvoidList.clear();

        CollisionGridAPI grid = Global.getCombatEngine().getAiGridShips();
        Iterator<Object> iter = grid.getCheckIterator(missile.getLocation(), MAX_HARD_AVOID_RANGE * 2f, MAX_HARD_AVOID_RANGE * 2f);
        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof ShipAPI)) continue;

            ShipAPI ship = (ShipAPI) o;

            if (ship.isFighter()) continue;
            hardAvoidList.add(ship);
        }

        grid = Global.getCombatEngine().getAiGridAsteroids();
        iter = grid.getCheckIterator(missile.getLocation(), MAX_HARD_AVOID_RANGE * 2f, MAX_HARD_AVOID_RANGE * 2f);
        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof CombatEntityAPI)) continue;

            CombatEntityAPI asteroid = (CombatEntityAPI) o;
            hardAvoidList.add(asteroid);
        }
    }

    public void doFlocking() {
        if (missile.getSource() == null) return;

        ShipAPI source = missile.getSource();
        CombatEngineAPI engine = Global.getCombatEngine();

        float avoidRange = AVOID_RANGE;
        float cohesionRange = COHESION_RANGE;

        float sourceRejoin = source.getCollisionRadius() * 0.5f + 100f;

        float sourceRepel = source.getCollisionRadius() * 0.5f + 50f;
        float sourceCohesion = source.getCollisionRadius() * 0.5f + 200f;

        float sin = (float) Math.sin(data.elapsed * 1f);
        float mult = 1f + sin * 0.25f;
        avoidRange *= mult;

        Vector2f total = new Vector2f();
        Vector2f attractor = getAttractorLoc();

        if (attractor != null && spent) {
            float dist = Misc.getDistance(missile.getLocation(), attractor);
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), attractor));
            float f = dist / 200f;
            if (f > 1f) f = 1f;
            dir.scale(f * 3f);
            Vector2f.add(total, dir, total);

            avoidRange *= 3f;
        }

        boolean hardAvoiding = false;
        for (CombatEntityAPI other : hardAvoidList) {
            float dist = Misc.getDistance(missile.getLocation(), other.getLocation());
            float hardAvoidRange = other.getCollisionRadius() + avoidRange + 50f;
            if (dist < hardAvoidRange) {
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(other.getLocation(), missile.getLocation()));
                float f = 1f - dist / (hardAvoidRange);
                dir.scale(f * 5f);
                Vector2f.add(total, dir, total);
                hardAvoiding = f > 0.5f;
            }
        }


        //for (MissileAPI otherMissile : missileList) {
        for (MissileAPI otherMissile : data.motes) {
            if (otherMissile == missile) continue;

            float dist = Misc.getDistance(missile.getLocation(), otherMissile.getLocation());


            float w = otherMissile.getMaxHitpoints();
            w = 1f;

            float currCohesionRange = cohesionRange;

            if (dist < avoidRange && otherMissile != missile && !hardAvoiding) {
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(otherMissile.getLocation(), missile.getLocation()));
                float f = 1f - dist / avoidRange;
                dir.scale(f * w);
                Vector2f.add(total, dir, total);
            }

            if (dist < currCohesionRange) {
                Vector2f dir = new Vector2f(otherMissile.getVelocity());
                Misc.normalise(dir);
                float f = 1f - dist / currCohesionRange;
                dir.scale(f * w);
                Vector2f.add(total, dir, total);
            }

        }

        if (missile.getSource() != null) {
            float dist = Misc.getDistance(missile.getLocation(), source.getLocation());
            if (dist > sourceRejoin) {
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), source.getLocation()));
                float f = dist / (sourceRejoin  + 100f) - 1f;
                dir.scale(f * 1.5f);

                Vector2f.add(total, dir, total);
            }

            /*if (dist < sourceRepel) {
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.getLocation(), missile.getLocation()));
                float f = 1f - dist / sourceRepel;
                dir.scale(f * 2f);
                Vector2f.add(total, dir, total);
            }*/

            if (dist < sourceCohesion && source.getVelocity().length() > 20f) {
                Vector2f dir = new Vector2f(source.getVelocity());
                Misc.normalise(dir);
                float f = 1f - dist / sourceCohesion;
                dir.scale(f * 1f);
                Vector2f.add(total, dir, total);
            }

            // if not strongly going anywhere, circle the source ship; only kicks in for lone motes
            if (total.length() <= 0.15f) {
                float offset = r > 0.5f ? 45f : -45f;
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(
                        Misc.getAngleInDegrees(missile.getLocation(), source.getLocation()) + offset);
                float f = 1f;
                dir.scale(f * 1f);
                Vector2f.add(total, dir, total);
            }
        }

        if (total.length() > 0) {
            float dir = Misc.getAngleInDegrees(total);
            engine.headInDirectionWithoutTurning(missile, dir, 10000);

            if (r > 0.5f) {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            } else {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            }
            missile.getEngineController().forceShowAccelerating();
        }
    }

    //public void accumulate(FlockingData data, Vector2f )


    protected  boolean spent = false;
    protected IntervalUtil flutterCheck = new IntervalUtil(2f, 4f);
    protected FaderUtil currFlutter = null;
    protected float flutterRemaining = 0f;

    protected float elapsed = 0f;
    public void advance(float amount) {
        if (missile.isFizzling()) return;
        if (missile.getSource() ==  null) return;

        elapsed += amount;

        updateListTracker.advance(amount);
        if (updateListTracker.intervalElapsed()) {
            updateHardAvoidList();
        }

        //missile.getEngineController().getShipEngines().get(0).

        if (flutterRemaining <= 0) {
            flutterCheck.advance(amount);
            if (flutterCheck.intervalElapsed() &&
                    ((float) Math.random() > 0.9f ||
                            (data.attractorLock != null && (float) Math.random() > 0.5f))) {
                flutterRemaining = 2f + (float) Math.random() * 2f;
            }
        }




        if (elapsed >= 0.5f) {

            boolean wantToFlock = !isTargetValid();
            if (data.attractorLock != null) {
                float dist = Misc.getDistance(missile.getLocation(), data.attractorLock.getLocation());
                if (dist > data.attractorLock.getCollisionRadius() + ATTRACTOR_LOCK_STOP_FLOCKING_ADD) {
                    wantToFlock = true;
                }
            }

            if (wantToFlock) {
                doFlocking();
            } else {
                CombatEngineAPI engine = Global.getCombatEngine();
                Vector2f targetLoc = engine.getAimPointWithLeadForAutofire(missile, 1.5f, target, 50);
                engine.headInDirectionWithoutTurning(missile,
                        Misc.getAngleInDegrees(missile.getLocation(), targetLoc),
                        10000);
                //AIUtils.turnTowardsPointV2(missile, targetLoc);
                if (r > 0.5f) {
                    missile.giveCommand(ShipCommand.TURN_LEFT);
                } else {
                    missile.giveCommand(ShipCommand.TURN_RIGHT);
                }
                missile.getEngineController().forceShowAccelerating();
            }
        }

        if (target != null) spent = true;
        tracker.advance(amount);
        if (tracker.intervalElapsed()) {
            if (elapsed >= 0.5f) {
                if (spent)
                    acquireNewTargetIfNeeded();
            }
            //causeEnemyMissilesToTargetThis();
        }
    }


    @SuppressWarnings("unchecked")
    protected boolean isTargetValid() {
        if (target == null || (target instanceof ShipAPI && ((ShipAPI)target).isPhased())) {
            return false;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        if (target != null && target instanceof ShipAPI && ((ShipAPI)target).isHulk()) return false;

        List list = null;
        if (target instanceof ShipAPI) {
            list = engine.getShips();
        } else {
            list = engine.getMissiles();
        }
        return target != null && list.contains(target) && target.getOwner() != missile.getOwner();
    }

    protected void acquireNewTargetIfNeeded() {
        if (target != null) return;
        if (data.attractorLock != null) {
            target = data.attractorLock;
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();

        // want to: target nearest missile that is not targeted by another two motes already
        int owner = missile.getOwner();

        int maxMotesPerMissile = 2;

        float minDist = Float.MAX_VALUE;
        CombatEntityAPI closest = null;

        for (ShipAPI other : engine.getShips()) {
            if (other.getOwner() == owner) continue;
            if (other.getOwner() == 100) continue;
            if (other.isFighter()) continue;
            float distToTarget = Misc.getDistance(missile.getLocation(), other.getLocation());
            if (distToTarget > minDist) continue;
            if (distToTarget > 3000 && !engine.isAwareOf(owner, other)) continue;

            float distFromAttractor = Float.MAX_VALUE;
            float bonus = 0;
            if (data.attractorTarget != null) {
                if (data.attractorTarget instanceof ShipAPI)
                    bonus += ((ShipAPI) data.attractorTarget).getCollisionRadius();
                distFromAttractor = Misc.getDistance(other.getLocation(), data.attractorTarget);
            }
            float distFromSource = Misc.getDistance(other.getLocation(), missile.getSource().getLocation());
            if (distFromSource > missile.getWeapon().getRange() + bonus &&
                    distFromAttractor > missile.getWeapon().getRange() + bonus) continue;

            if (getNumMotesTargeting(other) >= maxMotesPerMissile) continue;
            if (distToTarget < minDist) {
                closest = other;
                minDist = distToTarget;
            }
        }

        target = closest;
    }

    protected int getNumMotesTargeting(CombatEntityAPI other) {
        int count = 0;
        for (MissileAPI mote : data.motes) {
            if (mote == missile) continue;
            if (mote.getUnwrappedMissileAI() instanceof NA_NovaMoteAI) {
                NA_NovaMoteAI ai = (NA_NovaMoteAI) mote.getUnwrappedMissileAI();
                if (ai.getTarget() == other) {
                    count++;
                }
            }
        }
        return count;
    }

    public Vector2f getAttractorLoc() {
        Vector2f attractor = null;
        if (data.attractorTarget != null) {
            attractor = data.attractorTarget;
            if (data.attractorLock != null) {
                attractor = data.attractorLock.getLocation();
            }
        }
        return attractor;
    }

    public CombatEntityAPI getTarget() {
        return target;
    }

    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }
    public void render() {

    }
}
