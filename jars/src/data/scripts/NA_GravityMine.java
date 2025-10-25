package data.scripts;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.MineStrikeStatsAIInfoProvider;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class NA_GravityMine extends BaseShipSystemScript implements MineStrikeStatsAIInfoProvider {

    protected static float MINE_RANGE = 1200f;

    public static final float MIN_SPAWN_DIST = 15f;
    public static final float MIN_SPAWN_DIST_FRIGATE = 110f;

    public static final float LIVE_TIME = 0.3f;

    public static final Color JITTER_COLOR = new Color(8, 38, 253,75);
    public static final Color JITTER_UNDER_COLOR = new Color(75, 51, 255,155);


    public static float getRange(ShipAPI ship) {
        if (ship == null) return MINE_RANGE;
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(MINE_RANGE);
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        //boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }


        float jitterLevel = effectLevel;
        if (state == State.OUT) {
            jitterLevel *= jitterLevel;
        }
        float maxRangeBonus = 25f;
        float jitterRangeBonus = jitterLevel * maxRangeBonus;
        if (state == State.OUT) {
        }

        ship.setJitterShields(false);
        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
        //ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);

        if (state == State.IN) {
        } else if (effectLevel >= 1) {
            Vector2f target = ship.getMouseTarget();
            if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.SYSTEM_TARGET_COORDS)){
                target = (Vector2f) ship.getAIFlags().getCustom(AIFlags.SYSTEM_TARGET_COORDS);
            }
            if (target != null) {
                float dist = Misc.getDistance(ship.getLocation(), target);
                float max = getMaxRange(ship) + ship.getCollisionRadius();
                if (dist > max) {
                    float dir = Misc.getAngleInDegrees(ship.getLocation(), target);
                    target = Misc.getUnitVectorAtDegreeAngle(dir);
                    target.scale(max);
                    Vector2f.add(target, ship.getLocation(), target);
                }

                target = findClearLocation(ship, target);

                if (target != null) {
                    spawnMine(ship, target);
                }
            }

        } else if (state == State.OUT ) {
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    public void spawnMine(ShipAPI source, Vector2f mineLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f currLoc = Misc.getPointAtRadius(mineLoc, 5);
        //Vector2f currLoc = null;
        float start = (float) Math.random() * 360f;
        for (float angle = start; angle < start + 390; angle += 30f) {
            if (angle != start) {
                Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
                loc.scale(20f + (float) Math.random() * 15f);
                currLoc = Vector2f.add(mineLoc, loc, new Vector2f());
            }
            for (MissileAPI other : Global.getCombatEngine().getMissiles()) {
                if (!other.isMine()) continue;

                float dist = Misc.getDistance(currLoc, other.getLocation());
                if (dist < other.getCollisionRadius() + 40f) {
                    currLoc = null;
                    break;
                }
            }
            if (currLoc != null) {
                break;
            }
        }
        if (currLoc == null) {
            currLoc = Misc.getPointAtRadius(mineLoc, 35);
        }

        for (float i = 0; i < 0.5f; i += 0.1f) {
            EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
            params.maxZigZagMult = 0.35f;
            params.flickerRateMult = 0.25f + i;
            params.glowSizeMult *= 0.8f;

            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
                    source.getLocation(), source,
                    MathUtils.getRandomPointInCircle(currLoc, 30),
                    null,
                    15f,
                    new Color(15, 155, 255),
                    new Color(255, 255, 255), params
            );
            arc.setSingleFlickerMode(true);
            arc.setRenderGlowAtStart(false);

        }

        RippleDistortion ripple = new RippleDistortion(currLoc, Misc.ZERO);
        ripple.setSize(50f);
        ripple.setIntensity(65.0F);
        ripple.setFrameRate(-45);
        ripple.setCurrentFrame(59);
        ripple.fadeOutIntensity(0.75f);
        DistortionShader.addDistortion(ripple);

        RippleDistortion ripple2 = new RippleDistortion(currLoc, Misc.ZERO);
        ripple2.setSize(NA_GravityMineExplosion.RADIUS + 25f);
        ripple2.setIntensity(65.0F);
        ripple2.setFrameRate(-10);
        ripple2.setCurrentFrame(59);
        ripple2.fadeOutIntensity(5.0f);
        DistortionShader.addDistortion(ripple2);

        //Vector2f currLoc = mineLoc;
        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
                "na_minelayer",
                currLoc,
                (float) Math.random() * 360f, null);
        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                    source, WeaponType.MISSILE, false, mine.getDamage());
//			float extraDamageMult = source.getMutableStats().getMissileWeaponDamageMult().getModifiedValue();
//			mine.getDamage().setMultiplier(mine.getDamage().getMultiplier() * extraDamageMult);
        }


        float fadeInTime = 0.5f;
        mine.getVelocity().scale(0);
        mine.fadeOutThenIn(fadeInTime);

        Global.getCombatEngine().addPlugin(createMissileJitterPlugin(mine, fadeInTime));
        mine.setCollisionClass(CollisionClass.NONE);

        //mine.setFlightTime((float) Math.random());
        float liveTime = LIVE_TIME;
        //liveTime = 0.01f;
        mine.setFlightTime(mine.getMaxFlightTime() - liveTime);

        Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, mine.getLocation(), mine.getVelocity());
    }

    protected EveryFrameCombatPlugin createMissileJitterPlugin(final MissileAPI mine, final float fadeInTime) {
        return new BaseEveryFrameCombatPlugin() {
            float elapsed = 0f;
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) return;

                elapsed += amount;

                if (elapsed > 0.3f) mine.setCollisionClass(CollisionClass.MISSILE_NO_FF);

                float jitterLevel = mine.getCurrentBaseAlpha();
                if (jitterLevel < 0.5f) {
                    jitterLevel *= 2f;
                } else {
                    jitterLevel = (1f - jitterLevel) * 2f;
                }

                float jitterRange = 1f - mine.getCurrentBaseAlpha();
                //jitterRange = (float) Math.sqrt(jitterRange);
                float maxRangeBonus = 50f;
                float jitterRangeBonus = jitterRange * maxRangeBonus;
                Color c = JITTER_UNDER_COLOR;
                c = Misc.setAlpha(c, 70);
                //mine.setJitter(this, c, jitterLevel, 15, jitterRangeBonus * 0.1f, jitterRangeBonus);
                mine.setJitter(this, c, jitterLevel, 15, jitterRangeBonus * 0, jitterRangeBonus);

                if (jitterLevel >= 1 || elapsed > fadeInTime) {
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        };
    }


    protected float getMaxRange(ShipAPI ship) {
        return getMineRange(ship);
    }


    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != SystemState.IDLE) return null;

        Vector2f target = ship.getMouseTarget();
        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target);
            float max = getMaxRange(ship) + ship.getCollisionRadius();
            if (dist > max) {
                return "OUT OF RANGE";
            } else {
                return "READY";
            }
        }
        return null;
    }


    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return ship.getMouseTarget() != null;
    }


    private Vector2f findClearLocation(ShipAPI ship, Vector2f dest) {
        if (isLocationClear(dest)) return dest;

        float incr = 25f;

        WeightedRandomPicker<Vector2f> tested = new WeightedRandomPicker<Vector2f>();
        for (float distIndex = 1; distIndex <= 32f; distIndex *= 2f) {
            float start = (float) Math.random() * 360f;
            for (float angle = start; angle < start + 360; angle += 30f) {
                Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
                loc.scale(incr * distIndex);
                Vector2f.add(dest, loc, loc);
                tested.add(loc);
                if (isLocationClear(loc)) {
                    return loc;
                }
            }
        }

        if (tested.isEmpty()) return dest; // shouldn't happen

        return tested.pick();
    }

    private boolean isLocationClear(Vector2f loc) {
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (other.isShuttlePod()) continue;
            if (other.isFighter()) {
                if (MathUtils.getDistance(loc, other.getLocation()) < other.getCollisionRadius() + 35f) return false;
            } else {
                float dist = MathUtils.getDistance(loc, other.getLocation()) - 35f;
                // check the center
                if (other.isPointInBounds(loc) || (other.getShield() != null && other.getShield().isOn() && dist < other.getShieldRadiusEvenIfNoShield() && other.getShield().isWithinArc(loc))) {
                    return false;
                }
                // check a polygon
                for (float ang = 0; ang < 360f; ang += 60f) {
                    Vector2f loc2 = MathUtils.getPointOnCircumference(loc, 35f, ang);
                    float dist2 = MathUtils.getDistance(loc2, other.getLocation()) - 35f;
                    if (other.isPointInBounds(loc2) || (other.getShield() != null && other.getShield().isOn() && dist2 < other.getShieldRadiusEvenIfNoShield() && other.getShield().isWithinArc(loc2))) {
                        return false;
                    }
                }
            }

//			Vector2f otherLoc = other.getLocation();
//			float otherR = other.getCollisionRadius();

//			if (other.isPiece()) {
//				System.out.println("ewfewfewfwe");
//			}
        }
        for (CombatEntityAPI other : Global.getCombatEngine().getAsteroids()) {
            float dist = Misc.getDistance(loc, other.getLocation());
            if (dist < other.getCollisionRadius() + MIN_SPAWN_DIST) {
                return false;
            }
        }

        return true;
    }


    public float getFuseTime() {
        return 3f;
    }


    public float getMineRange(ShipAPI ship) {
        return getRange(ship);
        //return MINE_RANGE;
    }


}








