package data.scripts;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;

public class NA_TidalDisruptionStats extends BaseShipSystemScript {
    public static Object KEY_SHIP = new Object();
    public static Object KEY_TARGET = new Object();

    private static Map<Integer, Float> mag = new HashMap();
    static {
        mag.put(0, 0.25f);
        mag.put(1, 0.4f);
        mag.put(2, 0.5f);
        mag.put(3, 0.6f);
        mag.put(4, 0.7f);
    }

    protected static float RANGE = 1500f;
    protected static float RANGE_MAXEFFECT = 1000f;
    protected static float SELF_EFFECT = 0.5f; // self receives half the effect
    protected static float RANGE_SCALE = 0.9f;

    public static Color TEXT_COLOR = new Color(55,175,255,255);

    public static Color JITTER_COLOR = new Color(50,50,255,75);
    public static Color JITTER_UNDER_COLOR = new Color(100,100,255,155);


    public static class TargetData {
        public ShipAPI ship;
        public ShipAPI target;
        public EveryFrameCombatPlugin targetEffectPlugin;
        public float currMobilityMult;
        public float selfMobilityMult;
        public float elaspedAfterInState;
        public IntervalUtil beamTime = new IntervalUtil(0.2f, 0.2f);
        public TargetData(ShipAPI ship, ShipAPI target) {
            this.ship = ship;
            this.target = target;
        }
    }

    public float getMagnitude(ShipAPI ship, ShipAPI target) {
        if (target == null
            || ship == null
            || !target.isAlive()
            || !ship.isAlive()) return 1f;
        int size_this = 1;
        int size_that = 1;
        switch (ship.getHullSize()) {
            case CAPITAL_SHIP: size_this = 4; break;
            case CRUISER: size_this = 3; break;
            case DESTROYER: size_this = 2; break;
            case FIGHTER: size_this = 0; break;
        }
        switch (target.getHullSize()) {
            case CAPITAL_SHIP: size_that = 4; break;
            case CRUISER: size_that = 3; break;
            case DESTROYER: size_that = 2; break;
            case FIGHTER: size_that = 0; break;
        }

        int magnitude = Math.max(size_that - size_this, 0);
        magnitude += 1;
        // eg. this is a fighter, that is a cap: 4 - 0 + 1 = 5 => no effect
        // eg. this is a frigate, that is a cap: 4 - 1 + 1 = 4 => weakest effect
        float amt = mag.containsKey(magnitude) ? mag.get(magnitude) : 1f;
        float dist = MathUtils.getDistance(ship.getLocation(), target.getLocation());
        if (dist > RANGE_MAXEFFECT) {
            amt = amt + RANGE_SCALE * (1f - amt) * (dist - RANGE_MAXEFFECT)/(RANGE - RANGE_MAXEFFECT);
        }
        return amt;
    }

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        final String targetDataKey = ship.getId() + "_tidal_target_data";

        Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
        if (state == State.IN && targetDataObj == null) {
            ShipAPI target = findTarget(ship);
            Global.getCombatEngine().getCustomData().put(targetDataKey, new TargetData(ship, target));
            if (target != null) {
                if (target.getFluxTracker().showFloaty() ||
                        ship == Global.getCombatEngine().getPlayerShip() ||
                        target == Global.getCombatEngine().getPlayerShip()) {
                    target.getFluxTracker().showOverloadFloatyIfNeeded("Mobility Reduced!", TEXT_COLOR, 4f, true);
                }
            }
        } else if (state == State.IDLE && targetDataObj != null) {
            Global.getCombatEngine().getCustomData().remove(targetDataKey);
            ((TargetData)targetDataObj).currMobilityMult = 1f;
            ((TargetData)targetDataObj).selfMobilityMult = 1f;
            targetDataObj = null;
        }
        if (targetDataObj == null || ((TargetData) targetDataObj).target == null) return;

        final TargetData targetData = (TargetData) targetDataObj;
        float eff_mult = getMagnitude(targetData.ship, targetData.target);
        float eff_mult2 = getMagnitude(targetData.target, targetData.ship);
        targetData.currMobilityMult = 1f + (eff_mult - 1f) * effectLevel;
        targetData.selfMobilityMult = 1f + (eff_mult2 - 1f) * effectLevel * SELF_EFFECT;
        //System.out.println("targetData.currDamMult: " + targetData.currDamMult);
        if (targetData.targetEffectPlugin == null) {
            targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (Global.getCombatEngine().isPaused()) return;
                    if (targetData.target == Global.getCombatEngine().getPlayerShip()) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(KEY_TARGET,
                                targetData.ship.getSystem().getSpecAPI().getIconSpriteName(),
                                targetData.ship.getSystem().getDisplayName(),
                                "-" + (int)(100f - (targetData.currMobilityMult - 1f) * 100f) + "% mobility", true);
                    }
                    if (targetData.ship == Global.getCombatEngine().getPlayerShip()) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(KEY_SHIP,
                                targetData.ship.getSystem().getSpecAPI().getIconSpriteName(),
                                targetData.ship.getSystem().getDisplayName(),
                                "-" + (int)(100f - (targetData.selfMobilityMult - 1f) * 100f) + "% mobility", true);
                    }

                    if (targetData.currMobilityMult >= 1f || !targetData.ship.isAlive()) {
                        targetData.target.getMutableStats().getAcceleration().unmodify(id);
                        targetData.target.getMutableStats().getMaxSpeed().unmodify(id);
                        targetData.target.getMutableStats().getMaxTurnRate().unmodify(id);
                        targetData.target.getMutableStats().getTurnAcceleration().unmodify(id);
                        targetData.target.getMutableStats().getShieldTurnRateMult().unmodify(id);
                        targetData.target.getMutableStats().getShieldUnfoldRateMult().unmodify(id);
                        Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
                    } else {
                        targetData.target.getMutableStats().getAcceleration().modifyMult(id, targetData.currMobilityMult);
                        targetData.target.getMutableStats().getMaxSpeed().modifyMult(id, targetData.currMobilityMult);
                        targetData.target.getMutableStats().getMaxTurnRate().modifyMult(id, targetData.currMobilityMult);
                        targetData.target.getMutableStats().getTurnAcceleration().modifyMult(id, targetData.currMobilityMult);
                        targetData.target.getMutableStats().getShieldTurnRateMult().modifyMult(id, targetData.currMobilityMult);
                        targetData.target.getMutableStats().getShieldUnfoldRateMult().modifyMult(id, targetData.currMobilityMult);
                    }


                    if (targetData.selfMobilityMult >= 1f || !targetData.target.isAlive()) {
                        targetData.ship.getMutableStats().getAcceleration().unmodify(id);
                        targetData.ship.getMutableStats().getMaxSpeed().unmodify(id);
                        targetData.ship.getMutableStats().getMaxTurnRate().unmodify(id);
                        targetData.ship.getMutableStats().getTurnAcceleration().unmodify(id);
                        targetData.ship.getMutableStats().getShieldTurnRateMult().unmodify(id);
                        targetData.ship.getMutableStats().getShieldUnfoldRateMult().unmodify(id);
                        Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
                    } else {
                        targetData.ship.getMutableStats().getAcceleration().modifyMult(id, targetData.selfMobilityMult);
                        targetData.ship.getMutableStats().getMaxSpeed().modifyMult(id, targetData.selfMobilityMult);
                        targetData.ship.getMutableStats().getMaxTurnRate().modifyMult(id, targetData.selfMobilityMult);
                        targetData.ship.getMutableStats().getTurnAcceleration().modifyMult(id, targetData.selfMobilityMult);
                        targetData.ship.getMutableStats().getShieldTurnRateMult().modifyMult(id, targetData.currMobilityMult);
                        targetData.ship.getMutableStats().getShieldUnfoldRateMult().modifyMult(id, targetData.currMobilityMult);
                    }
                }
            };
            Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
        }


        if (effectLevel > 0) {
            if (state != State.IN) {
                targetData.elaspedAfterInState += Global.getCombatEngine().getElapsedInLastFrame();
            }
            float shipJitterLevel = 0;
            if (state == State.IN) {
                shipJitterLevel = effectLevel;
            } else {
                float durOut = 0.5f;
                shipJitterLevel = Math.max(0, durOut - targetData.elaspedAfterInState) / durOut;
            }
            float targetJitterLevel = effectLevel;

            float maxRangeBonus = 50f;
            float jitterRangeBonus = shipJitterLevel * maxRangeBonus;

            Color color = JITTER_COLOR;
            if (shipJitterLevel > 0) {
                ship.setJitterUnder(KEY_SHIP, JITTER_UNDER_COLOR, shipJitterLevel, 21, 0f, 3f + jitterRangeBonus);
                ship.setJitter(KEY_SHIP, color, shipJitterLevel, 4, 0f, 0 + jitterRangeBonus * 1f);
            } else {
                targetData.ship.setJitterUnder(KEY_TARGET, JITTER_UNDER_COLOR, targetJitterLevel, 5, 0f, 15f);
                targetData.ship.setJitter(KEY_TARGET, color, targetJitterLevel, 3, 0f, 5f);
                if (targetData.target.isAlive()) {
                    if (targetData.beamTime.intervalElapsed()) {
                        targetData.beamTime.setElapsed(0);
                        float beam_size = 40f;
                        switch (ship.getHullSize()) {
                            case CAPITAL_SHIP: beam_size = 100f; break;
                            case CRUISER: beam_size = 80f; break;
                            case DESTROYER: beam_size = 60f; break;
                            case FIGHTER: beam_size = 10f; break;
                        }
                        Global.getCombatEngine().addNebulaSmoothParticle(
                                MathUtils.getRandomPointOnLine(
                                        targetData.target.getLocation(),
                                        targetData.ship.getLocation()
                                ),
                                MathUtils.getPointOnCircumference(Misc.ZERO, 100f, VectorUtils.getAngle(
                                        targetData.ship.getLocation(),
                                        targetData.target.getLocation()
                                )),
                                beam_size,
                                0.5f,
                                0.6f,
                                0.8f,
                                targetData.beamTime.getIntervalDuration(),
                                new Color(25, 0, 255, 100));
                        /*MagicFakeBeam.spawnFakeBeam(
                                Global.getCombatEngine(),
                                targetData.ship.getLocation(),
                                MathUtils.getDistance(targetData.ship, targetData.target.getLocation()),
                                VectorUtils.getAngle(targetData.ship.getLocation(), targetData.target.getLocation()),
                                200f,
                                0.0f,
                                0.02f,
                                0f,
                                new Color(25, 0, 255, 100),
                                new Color(75, 100, 255, 25),
                                0f,
                                DamageType.ENERGY,
                                0f,
                                targetData.ship
                        );*/
                    } else {
                        targetData.beamTime.advance(Global.getCombatEngine().getElapsedInLastFrame());
                    }
                }


            }

            if (targetJitterLevel > 0) {
                targetData.target.setJitterUnder(KEY_TARGET, JITTER_UNDER_COLOR, targetJitterLevel, 5, 0f, 15f);
                targetData.target.setJitter(KEY_TARGET, color, targetJitterLevel, 3, 0f, 5f);
            }
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {

    }

    protected ShipAPI findTarget(ShipAPI ship) {
        float range = getMaxRange(ship);
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        ShipAPI target = ship.getShipTarget();

        if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM)){
            target = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM);
        }

        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
            if (dist > range + radSum) target = null;
        } else {
            if (target == null || target.getOwner() == ship.getOwner()) {
                if (player) {
                    target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), ShipAPI.HullSize.FIGHTER, range, true);
                } else {
                    Object test = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
                    if (test instanceof ShipAPI) {
                        target = (ShipAPI) test;
                        float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
                        float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
                        if (dist > range + radSum) target = null;
                    }
                }
            }
            if (target == null) {
                target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), ShipAPI.HullSize.FIGHTER, range, true);
            }
        }

        return target;
    }


    public static float getMaxRange(ShipAPI ship) {
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (effectLevel > 0) {
            if (index == 0) {
                ShipAPI ship = Global.getCombatEngine().getPlayerShip();
                if (ship != null) {
                    return new StatusData("Tidal interference detected", false);
                }
            }
        }
        return null;
    }


    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;

        ShipAPI target = findTarget(ship);
        if (target != null && target != ship) {
            return "READY";
        }
        if ((target == null) && ship.getShipTarget() != null) {
            return "OUT OF RANGE";
        }
        return "NO TARGET";
    }


    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //if (true) return true;
        ShipAPI target = findTarget(ship);
        return target != null && target != ship;
    }

}








