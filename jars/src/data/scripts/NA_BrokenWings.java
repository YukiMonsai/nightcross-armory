package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.stardust.NA_StargazerStardust;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NA_BrokenWings extends BaseShipSystemScript {


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


    public static float SPEED_BONUS = 25f;
    public static float TURN_BONUS = 40f;
    public static float TURN_RATE_BONUS = 25f;

    public static final String DMG_ID = "NA_BrokenWingsMod";

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


    SoundAPI sound;

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
            //Global.getSoundPlayer().playSound("na_chargeup", 1f, 1f, ship.getLocation(), ship.getVelocity());
            if (sound != null) sound.stop();
            playedCooledDown = true;
        } else if (state == State.COOLDOWN) {
            if (!playedEnd) {
                //Global.getSoundPlayer().playSound("na_chargeup", 1f, 1f, ship.getLocation(), ship.getVelocity());
                playedEnd = true;
            }
        }

        //if (sound != null) sound.setLocation(ship.getLocation().x, ship.getLocation().y);


        init(ship);

        if (effectLevel == 0) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
            stats.getFluxDissipation().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);
            stats.getMissileAmmoRegenMult().unmodify(id);
            stats.getEnergyAmmoRegenMult().unmodify(id);
            stats.getFluxDissipation().unmodify(id);

            /*List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
            if (maneuveringThrusters != null) {
                for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                    if (e.getEngineSlot().getContrailWidth() == 128f) {
                        ship.getEngineController().setFlameLevel(e.getEngineSlot(), 0f);
                    }
                }
            }*/

        } else if (effectLevel > 0) {
            float mult = effectLevel;
            stats.getMaxSpeed().modifyFlat(id, mult * SPEED_BONUS);
            stats.getAcceleration().modifyPercent(id, mult * 5f*SPEED_BONUS);
            stats.getDeceleration().modifyPercent(id, mult * 5f*SPEED_BONUS);
            stats.getTurnAcceleration().modifyFlat(id, mult * TURN_BONUS);
            stats.getMaxTurnRate().modifyPercent(id, mult * TURN_RATE_BONUS);
            stats.getMissileAmmoRegenMult().modifyPercent(id, 100f);
            stats.getEnergyAmmoRegenMult().modifyPercent(id, 100f);
            stats.getFluxDissipation().modifyPercent(id, 50f);
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
            //sound = Global.getSoundPlayer().playSound("na_brokenwings", 1f, 0.25f, ship.getLocation(), ship.getVelocity());
            playedWindup = true;
            playedEnd = false;
        }


    }


    protected void modify(String id, MutableShipStatsAPI stats, float effectLevel) {
        //stats.getEnergyWeaponFluxCostMod().modifyMult(DMG_ID, 0.5f);


        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        Color color = new Color(255, 200, 80, 255);

        ship.getEngineController().getExtendLengthFraction().advance(Global.getCombatEngine().getElapsedInLastFrame());

        if (ship.getChildModulesCopy() != null && ship.getChildModulesCopy().size() > 0) {
            for (ShipAPI child: ship.getChildModulesCopy()) {
                child.getEngineController().fadeToOtherColor(child, color, new Color(255, 5, 5, 50), effectLevel, 0.4f);
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
                    ship.getEngineController().fadeToOtherColor(e.getEngineSlot(), color, new Color(255, 0, 0, 50), effectLevel, 0.4f);
                    ship.getEngineController().extendFlame(e.getEngineSlot(), 0.5f * effectLevel * effectLevel, 0.5f * effectLevel, 1.4f * effectLevel);


                    /*if (beamTimer.intervalElapsed()) {
                        Global.getCombatEngine().addSwirlyNebulaParticle(
                                e.getLocation(), new Vector2f(ship.getVelocity().x*0.5f, ship.getVelocity().y*0.5f),
                                30f, 3f, 0.5f, 0.5f,
                                1.7f,
                                new Color(255, 19, 75, 140), true
                        );
                    }*/

                }
            }
        }


        //ship.setJitter(this, new Color(255, 0, 75, 150), (0.5f + effectLevel * 0.5f), 1, 25);

        if (beamTimer.intervalElapsed()) {
            beamTimer.randomize();
            ship.addAfterimage(new Color(255, 0, 75, (int) (25 + 50 * effectLevel)),
                0,
                0,
                -ship.getVelocity().x,
                -ship.getVelocity().y,
                2f, 0.25f, 0.5f, 0.5f,
                true,
                true,
                true
                    );

            NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
            if (swarm != null) {
                boolean realshock = false;
                if (MathUtils.getRandomNumberInRange(effectLevel, 1f) < 0.5f) {
                    // fake shock
                } else {
                    // real shock

                    WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker2 = swarm.getPicker(true, true);
                    NA_StargazerStardust.SwarmMember fragment = picker2.pick();
                    if (fragment != null) {
                        List<CombatEntityAPI> nearbyTargets = NAUtils.getEntitiesWithinRange(ship.getLocation(), 700f);
                        nearbyTargets.removeIf(entry -> (entry.getOwner() == ship.getOwner()));
                        if (!nearbyTargets.isEmpty()) {
                            WeightedRandomPicker<CombatEntityAPI> picker = new WeightedRandomPicker<>();
                            for (CombatEntityAPI entity : nearbyTargets) {
                                float size = 0;
                                if (entity instanceof MissileAPI || entity instanceof CombatAsteroidAPI) size = 0.25f;
                                if (entity instanceof ShipAPI) {
                                    switch (((ShipAPI) entity).getHullSize()) {
                                        case FRIGATE: size = 1; break;
                                        case CRUISER: size = 3.5f; break;
                                        case CAPITAL_SHIP: size = 6f; break;
                                        case DESTROYER: size = 2f; break;
                                        case FIGHTER: size = 0.4f; break;
                                    }
                                }
                                picker.add(entity, size);
                            }

                            CombatEntityAPI target = picker.pick();
                            if (target != null) {
                                realshock = true;
                                Global.getCombatEngine().spawnEmpArc(
                                        ship, fragment.loc, null, target, DamageType.HIGH_EXPLOSIVE, 200f + 75f*  ship.getFluxLevel(), 200f + 75f * ship.getFluxLevel(), 1200f,
                                        "na_rift_beam_explosion2", 22f + 11f *  ship.getFluxLevel(),

                                        new Color(255, 8, 187, 50),
                                        new Color(253, 162, 162, 250)
                                        );
                            }
                        }
                    }

                }
                if (!realshock && MathUtils.getRandomNumberInRange(effectLevel, 1f) > 0.5f) {


                    WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker2 = swarm.getPicker(true, true);
                    NA_StargazerStardust.SwarmMember fragment = picker2.pick();
                    NA_StargazerStardust.SwarmMember fragment2 = picker2.pick();
                    if (fragment2 != null && fragment != null && fragment2 != fragment) {
                        Global.getCombatEngine().spawnEmpArcVisual(
                                fragment.loc, ship, fragment2.loc, ship, 15f,
                                new Color(255, 0, 221, 50),
                                new Color(255, 222, 222, 150)
                        );
                    }
                }
            }

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
            return "broken wings - ready";
        }
        if (state == State.COOLDOWN) {
            return "broken wings - COOLDOWN";
        }
        if (state == State.IN) {
            return "broken wings - ENGAGE";
        }
        if (state == State.OUT) {
            return "broken wings - OVERLOAD!!!";
        }
        if (state == State.ACTIVE) {
            return "broken wings - ACTIVE";
        }
        return null;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }




}
















