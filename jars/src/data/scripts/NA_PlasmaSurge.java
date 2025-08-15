package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NA_PlasmaSurge extends BaseShipSystemScript {


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

    public static Color WEAPON_GLOW = new Color(20, 245, 96,155);

    public static final float SPEED_MULT = 0.5f;
    public static final float SPEED_BOOST = 120f;
    public static final float AMMO_REGEN_ON_CAST = 10f; // 10 seconds of reload time


    public static final String DMG_ID = "NA_PlasmaSurgeMod";

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
        engineMap.put("na_tessera_drill", tessera);
        engineMap.put("naai_tessera", tessera);
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

        if (effectLevel >= 1 && reloadWeps) {
            reloadWeps = false;
            // Do the reload
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (weaponEligible(w)) {
                    float ammoToRegen = (float) Math.ceil(AMMO_REGEN_ON_CAST * w.getAmmoPerSecond());
                    float maxAmmo = w.getMaxAmmo();
                    float current = w.getAmmo();

                    w.setAmmo((int) Math.min(maxAmmo, current + ammoToRegen));
                }
            }
        }

        boolean active = state == State.IN || state == State.ACTIVE || state == State.OUT;

        rotateLidarDishes(active, effectLevel);
        if (active) {
            modify(id, stats, effectLevel);
            needsUnapply = true;
        } else {
            if (needsUnapply) {
                unmodify(id, stats);
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (weaponEligible(w)) {
                        w.setGlowAmount(0, null);
                    }
                }
                needsUnapply = false;
                reloadWeps = true;
            }
        }

        if (!active) return;

        Color glowColor = WEAPON_GLOW;
        float time = Global.getCombatEngine().getElapsedInLastFrame();

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (weaponEligible(w)) {
                w.setGlowAmount(effectLevel, glowColor);

            }
        }

        if (((state == State.IN && effectLevel > 0.1f) || state == State.ACTIVE) && !playedWindup) {
            Global.getSoundPlayer().playSound("na_plasmasurge", 1f, 0.25f, ship.getLocation(), ship.getVelocity());
            playedWindup = true;
            playedEnd = false;
        }


    }


    protected void modify(String id, MutableShipStatsAPI stats, float effectLevel) {
        stats.getMaxSpeed().modifyFlat(DMG_ID, effectLevel*SPEED_BOOST);
        stats.getAcceleration().modifyFlat(DMG_ID, effectLevel*SPEED_BOOST);
        stats.getAcceleration().modifyMult(DMG_ID, 1f + effectLevel*SPEED_MULT);
        stats.getTurnAcceleration().modifyFlat(DMG_ID, effectLevel*SPEED_BOOST);
        stats.getTurnAcceleration().modifyMult(DMG_ID, 1f + effectLevel*SPEED_MULT);
        stats.getDeceleration().modifyFlat(DMG_ID, effectLevel*SPEED_BOOST);
        stats.getDeceleration().modifyMult(DMG_ID, 1f + effectLevel*SPEED_MULT);
        stats.getMaxTurnRate().modifyMult(DMG_ID, 1f + effectLevel*SPEED_MULT);
        stats.getEnergyWeaponFluxCostMod().modifyMult(DMG_ID, 0.7f);


        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        Color color = new Color(172, 47, 250, 75);

        ship.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

        if (ship.getChildModulesCopy() != null && ship.getChildModulesCopy().size() > 0) {
            for (ShipAPI child: ship.getChildModulesCopy()) {
                child.getEngineController().fadeToOtherColor(child, color, new Color(0, 0, 0, 0), effectLevel, 0.8f);
                child.getEngineController().extendFlame(child, 1.0f * effectLevel, 0.8f * effectLevel, 0.8f * effectLevel);
                child.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

            }
        }

        List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
        if (maneuveringThrusters != null) {
            for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                if (Math.abs(Math.sin(Math.toRadians(e.getEngineSlot().getAngle()))) > 0.1 && e.getEngineSlot().getLength() < 44) {
                    // Nothing!!
                } else {
                    ship.getEngineController().fadeToOtherColor(e.getEngineSlot(), color, new Color(0, 0, 0, 0), effectLevel, 0.8f);
                    ship.getEngineController().extendFlame(e.getEngineSlot(), 0.5f * effectLevel, 0.45f * effectLevel, 0.5f * effectLevel);


                    if (beamTimer.intervalElapsed()) {
                        Global.getCombatEngine().addSwirlyNebulaParticle(
                                e.getLocation(), new Vector2f(ship.getVelocity().x*0.5f, ship.getVelocity().y*0.5f),
                                30f, 3f, 0.5f, 0.5f,
                                1.7f,
                                NAUtils.isStargazerRed(ship) ? new Color(158, 0, 29, 150) : new Color(0, 61, 94, 150), true
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

        stats.getMaxSpeed().unmodify(DMG_ID);
        stats.getAcceleration().unmodify(DMG_ID);
        stats.getTurnAcceleration().unmodify(DMG_ID);
        stats.getDeceleration().unmodify(DMG_ID);
        stats.getMaxTurnRate().unmodify(DMG_ID);
        stats.getEnergyWeaponFluxCostMod().unmodify(DMG_ID);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        // not called
    }


    public String getDisplayNameOverride(State state, float effectLevel) {
        if (state == State.IDLE) {
            return "plasma surge - ready";
        }
        if (state == State.COOLDOWN) {
            return "RECHARGE";
        }
        if (state == State.IN) {
            return "plasma surge - active";
        }
        if (state == State.OUT) {
            return "plasma surge - active";
        }
        if (state == State.ACTIVE) {
            return "plasma surge - active";
        }
        return null;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }

    public static List<WeaponAPI> getWeapons(ShipAPI carrier) {
        List<WeaponAPI> result = new ArrayList<WeaponAPI>();

        for (WeaponAPI weapon : carrier.getAllWeapons()) {
            if (
                    weaponEligible(weapon)
            ) {
                result.add(weapon);
            }
        }

        return result;
    }

    public static boolean weaponEligible(WeaponAPI weapon) {
        return weapon != null && weapon.getSpec() != null
                && (
                weapon.getSpec().getType() == WeaponAPI.WeaponType.ENERGY
                        || weapon.getSpec().getType() == WeaponAPI.WeaponType.SYNERGY
                        || weapon.getSpec().getMountType() == WeaponAPI.WeaponType.ENERGY
                        || weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
                );
    }



}
















