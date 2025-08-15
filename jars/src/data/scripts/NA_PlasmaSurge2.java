package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NA_PlasmaSurge2 extends BaseShipSystemScript {


    public static class UnfurlDecoData {
        public float turnDir;
        public float turnRate;
        public float angle;
        public float count;
        public ShipEngineControllerAPI.ShipEngineAPI engineSlot = null;
        public float engineAngle;
        public float engineAngleOff;
        public float engineDist;
        public WeaponAPI w;
    }

    private IntervalUtil beamTimer = new IntervalUtil(0.15f, 0.3f);


    public static float SPEED_BONUS = 200f;
    public static float SPEED_BONUS_FORWARD = 300f;
    public static float TURN_BONUS = 150f;
    public static float TURN_RATE_BONUS = 150f;
    public static float FLUX_SCALE_MIN = 0.6f;
    public static float FLUX_SCALE_MAX = 1.35f;

    public static final String DMG_ID = "NA_PlasmaSurge2Mod";

    protected List<UnfurlDecoData> dishData = new ArrayList<UnfurlDecoData>();
    protected boolean needsUnapply = false;
    protected boolean playedWindup = false;
    protected boolean playedCooledDown = true;
    protected boolean playedEnd = false;


    protected boolean reloadWeps = false;


    public static Map<String, Map> engineMap = new HashMap();
    static {
        Map<String, float[]> tessera = new HashMap();
        float[] e1 = {-15f, -91f}; tessera.put("WS0026", e1);
        float[] e2 = {-23.5f, -63.5f}; tessera.put("WS0023", e2);
        float[] e3 = {-23.5f, 63.5f}; tessera.put("WS0010", e3);
        float[] e4 = {-15f, 91}; tessera.put("WS0027", e4);
        engineMap.put("na_tessera", tessera);
        engineMap.put("naai_tessera", tessera);
        engineMap.put("na_tessera_drill", tessera);
    }


    protected boolean inited = false;

    // plagarized ruthlessly from alex
    public void init(ShipAPI ship) {
        if (inited) return;
        if (ship == null) return;
        inited = true;

        needsUnapply = true;

        float count = 0f;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && (w.getSpec().hasTag("system_turn_left") || w.getSpec().hasTag("system_turn_right"))) {
                count++;
            }
        }
        List<WeaponAPI> lidar = new ArrayList<WeaponAPI>();
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && (w.getSpec().hasTag("system_turn_left") || w.getSpec().hasTag("system_turn_right"))) {
                lidar.add(w);
            }
        }
        Collections.sort(lidar, new Comparator<WeaponAPI>() {
            public int compare(WeaponAPI o1, WeaponAPI o2) {
                return (int) Math.signum(o1.getSlot().getLocation().x - o2.getSlot().getLocation().x);
            }
        });
        ShipHullSpecAPI spec = ship.getHullSpec().getBaseHull();
        if (spec == null) spec = ship.getHullSpec();
        for (WeaponAPI w : lidar) {
            if (w.isDecorative()) {
                UnfurlDecoData data = null;
                if (w.getSpec().hasTag("system_turn_left")) {
                    w.setSuspendAutomaticTurning(true);
                    data = new UnfurlDecoData();
                    data.turnDir = -1;
                    data.turnRate = 0.5f;
                    data.turnRate = 0.25f;
                    data.w = w;
                    data.angle = 0f;
                    data.count = count;
                    dishData.add(data);
                } else if (w.getSpec().hasTag("system_turn_right")) {
                    w.setSuspendAutomaticTurning(true);
                    data = new UnfurlDecoData();
                    data.turnDir = 1;
                    data.turnRate = 0.5f;
                    data.turnRate = 0.25f;
                    data.w = w;
                    data.angle = 0f;
                    data.count = count;
                    dishData.add(data);
                }
                if (data != null) {

                    if (engineMap.containsKey(spec.getHullId())) {
                        if (engineMap.get(ship.getHullSpec().getBaseHullId()).containsKey(w.getSlot().getId())) {
                            float[] slot = (float[]) engineMap.get(spec.getHullId()).get(w.getSlot().getId());
                            for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                                if (MathUtils.getDistance(
                                        new Vector2f(slot[0], slot[1]),
                                        MathUtils.getPointOnCircumference(Misc.ZERO,
                                                MathUtils.getDistance(engine.getLocation(), ship.getLocation()),
                                                VectorUtils.getAngle(ship.getLocation(), engine.getLocation()) - ship.getFacing())
                                ) < 1) {
                                    data.engineSlot = engine;
                                    Vector2f vec = new Vector2f(engine.getLocation().x - w.getSlot().getLocation().x,
                                            engine.getLocation().x - w.getSlot().getLocation().y);
                                    data.engineDist = vec.length();
                                    data.engineAngle = VectorUtils.getFacing(vec);
                                    data.engineAngleOff = engine.getEngineSlot().getAngle();
                                    break;
                                }
                            }
                        }

                    }
                }

            }
        }
    }

    public void rotateLidarDishes(boolean active, float effectLevel) {
        float amount = Global.getCombatEngine().getElapsedInLastFrame();

        float turnRateMult = 10f;
        if (active) {
            turnRateMult = 20f;
        }
        //turnRateMult = 0.1f;
        //boolean first = true;
        for (UnfurlDecoData data : dishData) {
            float arc = data.w.getArc();
            boolean vector = Math.signum(data.w.getShip().getAngularVelocity()) == Math.signum(data.turnDir)
                    && Math.abs(data.w.getShip().getAngularVelocity()) > 10f;
            if (data.w.getShip().getEngineController().isDecelerating()) vector = true;
            else if (data.w.getShip().getEngineController().isStrafingLeft()) vector = data.turnDir > 0;
            else if (data.w.getShip().getEngineController().isStrafingRight()) vector = data.turnDir < 0;
            float desired = (active || vector) ? arc * data.turnDir : 0f;
            float useTurnDir = Misc.getClosestTurnDirection(data.angle,
                    desired);
            float delta = useTurnDir * amount * data.turnRate * turnRateMult * arc;
            if (((effectLevel < 0.01f) && !vector)
                    && Math.abs(data.angle) < Math.abs(delta * 1.5f)) {
                data.angle = 0f;
            } else {
                if (Math.abs(data.angle - desired) > Math.abs(delta + 0.001))
                    data.angle += delta;
            }


            float facing = data.angle + data.w.getArcFacing() + data.w.getShip().getFacing();
            float angle = MathUtils.clamp(facing,
                    data.w.getArc()*-0.5f + data.w.getArcFacing() + data.w.getShip().getFacing(),
                    data.w.getArc()*0.5f + data.w.getArcFacing() + data.w.getShip().getFacing());
            data.w.setFacing(angle);
            data.w.updateBeamFromPoints();
            if (data.engineSlot != null)
                data.engineSlot.getEngineSlot().setAngle(MathUtils.clamp(data.angle,
                        data.w.getArc()*-0.5f + data.w.getArcFacing(),
                        data.w.getArc()*0.5f + data.w.getArcFacing())
                        + data.engineAngleOff);
        }
    }


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        if (ship == null || ship.isHulk()) {
            if (needsUnapply) {
                unmodify(id, stats);
                if (ship != null) {
                    for (WeaponAPI w : ship.getAllWeapons()) {
                        if (!w.isDecorative() && w.getSlot().isHardpoint() && !w.isBeam() &&
                                (w.getType() == WeaponAPI.WeaponType.BALLISTIC || w.getType() == WeaponAPI.WeaponType.ENERGY)) {
                            w.setGlowAmount(0, null);
                        }
                    }
                }
                needsUnapply = false;
                reloadWeps = true;
            }
            return;
        }

        if (state == State.IDLE && !playedCooledDown && ship.getSystem().getAmmo() > 0) {
            Global.getSoundPlayer().playSound("na_chargeup", 1f, 1f, ship.getLocation(), ship.getVelocity());
            playedCooledDown = true;
        } else if (state == State.COOLDOWN) {
            if (!playedEnd) {
                //Global.getSoundPlayer().playSound("na_chargeup", 1f, 1f, ship.getLocation(), ship.getVelocity());
                playedEnd = true;
            }
        }

        init(ship);

        if (effectLevel == 0) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
            stats.getFluxDissipation().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);

            /*List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
            if (maneuveringThrusters != null) {
                for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                    if (e.getEngineSlot().getContrailWidth() == 128f) {
                        ship.getEngineController().setFlameLevel(e.getEngineSlot(), 0f);
                    }
                }
            }*/

        } else if (effectLevel > 0) {
            boolean isThrustingForward = false;
            boolean isThrustingBackward = false;
            if (stats.getEntity() instanceof ShipAPI) {
                ShipEngineControllerAPI controller = ship.getEngineController();
                if (controller.isAcceleratingBackwards()) {
                    //sisThrustingForward = false;
                    isThrustingBackward = true;
                } else if (controller.isAccelerating()) {
                    isThrustingForward = true;
                }

            }
            float mult = FLUX_SCALE_MIN + ship.getFluxTracker().getHardFlux() * (FLUX_SCALE_MAX - FLUX_SCALE_MIN);
            stats.getMaxSpeed().modifyFlat(id, mult * (!isThrustingBackward ? SPEED_BONUS_FORWARD : SPEED_BONUS));
            stats.getAcceleration().modifyPercent(id, mult * (isThrustingForward ? SPEED_BONUS_FORWARD * (0.5f + 0.5f*effectLevel) * 2.0f : SPEED_BONUS * (0.5f + 0.5f*effectLevel) * 2.0f));
            stats.getDeceleration().modifyPercent(id, mult * SPEED_BONUS * (0.5f + 0.5f*effectLevel) * 2.5f);
            if (isThrustingForward) {
                stats.getMaxTurnRate().unmodify(id);
                stats.getTurnAcceleration().unmodify(id);
            } else {
                stats.getTurnAcceleration().modifyFlat(id, mult * TURN_BONUS * effectLevel);
                stats.getMaxTurnRate().modifyPercent(id, mult * TURN_RATE_BONUS);
            }
            //stats.getFluxDissipation().modifyFlat(id, FLUX_GEN * effectLevel);
        }



        boolean active = state == State.IN || state == State.ACTIVE || state == State.OUT;

        rotateLidarDishes(active, effectLevel);
        if (active) {
            modify(id, stats, effectLevel);
            needsUnapply = true;
        } else {
            if (needsUnapply) {
                unmodify(id, stats);
                needsUnapply = false;
                reloadWeps = true;
            }
        }

        if (!active) return;

        float time = Global.getCombatEngine().getElapsedInLastFrame();

        if (((state == State.IN && effectLevel > 0.1f) || state == State.ACTIVE) && !playedWindup) {
            Global.getSoundPlayer().playSound("na_plasmasurge2", 1f, 0.25f, ship.getLocation(), ship.getVelocity());
            playedWindup = true;
            playedEnd = false;
        }


    }


    protected void modify(String id, MutableShipStatsAPI stats, float effectLevel) {
        //stats.getEnergyWeaponFluxCostMod().modifyMult(DMG_ID, 0.5f);


        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        Color color = new Color(248, 230, 186, 255);

        ship.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

        if (ship.getChildModulesCopy() != null && ship.getChildModulesCopy().size() > 0) {
            for (ShipAPI child: ship.getChildModulesCopy()) {
                child.getEngineController().fadeToOtherColor(child, color, new Color(239, 5, 5, 50), effectLevel, 0.4f);
                child.getEngineController().extendFlame(child, 0.5f * effectLevel * effectLevel, 0.8f * effectLevel * effectLevel, 0.65f * effectLevel);
                child.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

            }
        }

        List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
        if (maneuveringThrusters != null) {
            for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                if (Math.abs(Math.sin(Math.toRadians(e.getEngineSlot().getAngle()))) > 0.1 && e.getEngineSlot().getLength() < 44) {
                    // Nothing!!
                } else {
                    ship.getEngineController().fadeToOtherColor(e.getEngineSlot(), color, new Color(239, 5, 5, 50), effectLevel, 0.4f);
                    ship.getEngineController().extendFlame(e.getEngineSlot(), 0.25f * effectLevel * effectLevel, 0.5f * effectLevel * effectLevel, 0.4f * effectLevel);


                    if (beamTimer.intervalElapsed()) {
                        Global.getCombatEngine().addSwirlyNebulaParticle(
                                e.getLocation(), new Vector2f(ship.getVelocity().x*0.5f, ship.getVelocity().y*0.5f),
                                30f, 3f, 0.5f, 0.5f,
                                1.7f,
                                new Color(238, 19, 110, 115), true
                        );
                    }

                }
            }
        }


        if (beamTimer.intervalElapsed()) {
            beamTimer.randomize();
        }
        beamTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());



    }
    protected void unmodify(String id, MutableShipStatsAPI stats) {
        playedWindup = false;
        playedCooledDown = false;

        //stats.getEnergyWeaponFluxCostMod().unmodify(DMG_ID);
        stats.getEnergyRoFMult().unmodify(DMG_ID);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        // not called
    }


    public String getDisplayNameOverride(State state, float effectLevel) {
        if (state == State.IDLE) {
            return "catalyst drive - ready";
        }
        if (state == State.COOLDOWN) {
            return "catalyst drive - OVERHEAT";
        }
        if (state == State.IN) {
            return "catalyst drive - ENGAGE";
        }
        if (state == State.OUT) {
            return "catalyst drive - OVERHEAT";
        }
        if (state == State.ACTIVE) {
            return "catalyst drive - ACTIVE";
        }
        return null;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }




}
















