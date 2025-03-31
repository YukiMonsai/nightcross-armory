package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.LidarArrayStats;
import com.fs.starfarer.api.impl.hullmods.BallisticRangefinder;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyframe.Nightcross_Trails;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NA_FastCaps extends BaseShipSystemScript {


    public static class UnfurlDecoData {
        public float turnDir;
        public float turnRate;
        public float angle;
        public float count;
        public WeaponAPI w;
    }

    private IntervalUtil beamTimer = new IntervalUtil(0.15f, 0.3f);

    public static Color WEAPON_GLOW = new Color(20, 245, 96,155);

    private NA_FastCapsRangeModifier listener;
    private NA_FastCapDmgBoost dmglistener;

    public static final float DMG_BONUS = 0.5f;
    public static final float RANGE_BOOST = 0.5f;
    public static final float AMMO_MULT = 0.5f;
    public static final float SPEED_MULT = -0.5f;
    public static final float ROF_BOOST = 1.5f;


    public static final String DMG_ID = "NA_FastCapDmgMod";

    protected List<UnfurlDecoData> dishData = new ArrayList<UnfurlDecoData>();
    protected boolean needsUnapply = false;
    protected boolean playedWindup = false;
    protected boolean playedCooledDown = false;
    protected boolean playedEnd = false;


    protected boolean inited = false;

    // plagarized ruthlessly from alex
    public void init(ShipAPI ship) {
        if (inited) return;
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
        for (WeaponAPI w : lidar) {
            if (w.isDecorative()) {
                if (w.getSpec().hasTag("system_turn_left")) {
                    w.setSuspendAutomaticTurning(true);
                    UnfurlDecoData data = new UnfurlDecoData();
                    data.turnDir = 1;
                    data.turnRate = 0.5f;
                    data.turnRate = 0.1f;
                    data.w = w;
                    data.angle = 0f;
                    data.count = count;
                    dishData.add(data);
                } else if (w.getSpec().hasTag("system_turn_right")) {
                    w.setSuspendAutomaticTurning(true);
                    UnfurlDecoData data = new UnfurlDecoData();
                    data.turnDir = -1;
                    data.turnRate = 0.5f;
                    data.turnRate = 0.1f;
                    data.w = w;
                    data.angle = 0f;
                    data.count = count;
                    dishData.add(data);
                }

            }
        }
    }

    public void rotateLidarDishes(boolean active, float effectLevel) {
        float amount = Global.getCombatEngine().getElapsedInLastFrame();

        float turnRateMult = 8f;
        if (active) {
            turnRateMult = 20f;
        }
        //turnRateMult = 0.1f;
        //boolean first = true;
        for (UnfurlDecoData data : dishData) {
            float arc = data.w.getArc();
            float desired = active ? arc * data.turnDir : 0f;
            float useTurnDir = Misc.getClosestTurnDirection(data.angle,
                    desired);
            float delta = useTurnDir * amount * data.turnRate * turnRateMult * arc;
            if (active && effectLevel < 0.01f && Math.abs(data.angle) < Math.abs(delta * 1.5f)) {
                data.angle = 0f;
            } else {
                if (Math.abs(data.angle - desired) > Math.abs(delta + 0.001))
                    data.angle += delta;
            }


            float facing = data.angle + data.w.getArcFacing() + data.w.getShip().getFacing();
            data.w.setFacing(facing);
            data.w.updateBeamFromPoints();
        }
    }


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        if (ship == null || ship.isHulk()) {
            if (needsUnapply) {
                unmodify(id, stats);
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (!w.isDecorative() && w.getSlot().isHardpoint() && !w.isBeam() &&
                            (w.getType() == WeaponAPI.WeaponType.BALLISTIC || w.getType() == WeaponAPI.WeaponType.ENERGY)) {
                        w.setGlowAmount(0, null);
                    }
                }
                needsUnapply = false;
            }
            return;
        }

        if (state == State.IDLE && !playedCooledDown) {
            Global.getSoundPlayer().playSound("na_chargeup", 1f, 1f, ship.getLocation(), ship.getVelocity());
            playedCooledDown = true;
        } else if (state == State.OUT || state == State.COOLDOWN) {
            if (!playedEnd) {
                Global.getSoundPlayer().playSound("na_steamoff", 1f, 1f, ship.getLocation(), ship.getVelocity());
                playedEnd = true;
            }
        }

        init(ship);

        if (effectLevel > 0) {
            if (listener == null) {
                List<WeaponAPI> weapons = getWeapons(ship);

                listener = new NA_FastCapsRangeModifier(weapons, RANGE_BOOST);
                ship.addListener(listener);
            }
            if (dmglistener == null) {
                List<WeaponAPI> weapons = getWeapons(ship);

                dmglistener = new NA_FastCapDmgBoost(weapons, DMG_BONUS);
                ship.addListener(dmglistener);
            }
        } else {
            if (listener != null) {
                ship.removeListener(listener);
                listener = null;
            }
            if (dmglistener != null) {
                ship.removeListener(dmglistener);
                dmglistener = null;
            }
        }

        if (listener != null) {
            listener.effectLevel = effectLevel;
        }
        if (dmglistener != null) {
            dmglistener.effectLevel = effectLevel;
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
            }
        }

        if (!active) return;

        Color glowColor = WEAPON_GLOW;
        float time = Global.getCombatEngine().getElapsedInLastFrame();

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (weaponEligible(w)) {
                w.setGlowAmount(effectLevel, glowColor);
                float projChance = 0.5f;

                if (!w.isBeam() || w.isBurstBeam()) {
                    if (w.getCooldownRemaining() > 0)
                        w.setRemainingCooldownTo(Math.max(0, w.getCooldownRemaining() - ROF_BOOST * time));
                }

                if (beamTimer.intervalElapsed() && Math.random() < 0.5) {
                    Global.getCombatEngine().spawnEmpArcVisual(
                            w.getFirePoint(0), ship,
                            MathUtils.getRandomPointInCircle(
                                    w.getLocation(), w.getSize() == WeaponAPI.WeaponSize.LARGE ? 84f : 50f
                            ), ship, 2f,
                            new Color(47, 250, 114, 75),
                            new Color(210, 238, 238, 75)
                    );
                }

                if (beamTimer.intervalElapsed() && Math.random() < 0.01) {

                    /*MagicFakeBeam.spawnFakeBeam(
                            Global.getCombatEngine(),
                            w.getFirePoint(0),
                            w.getRange(),
                            w.getCurrAngle(),
                            10, beamTimer.getMaxInterval(), 0.02f, 100f,
                            new Color(147, 1, 1, 150),
                            new Color(248, 239, 239, 220),
                            0f,
                            DamageType.ENERGY,
                            0f,
                            w.getShip()
                    );*/
                    for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
                        if (proj.getWeapon() != null && (!proj.isExpired() && !proj.isFading())
                                && proj.getWeapon().getId() == w.getId() && Math.random() < projChance) {
                            projChance *= 0.7f;
                            /*Global.getCombatEngine().addSwirlyNebulaParticle(
                                    proj.getLocation(), new Vector2f(proj.getVelocity().x*0.5f, proj.getVelocity().y*0.5f),
                                    proj.getWeapon().getSize() == WeaponAPI.WeaponSize.LARGE ? 84f : 50f, 0.5f, 0.05f, 0.5f,
                                    1.25f,
                                    new Color(47, 250, 114, 150), false
                            );*/
                            /*Global.getCombatEngine().spawnEmpArcVisual(
                                    proj.getLocation(), proj,
                                    MathUtils.getRandomPointInCircle(
                                            proj.getLocation(), w.getSize() == WeaponAPI.WeaponSize.LARGE ? 84f : 50f
                                    ), proj, 2f,
                                    new Color(47, 250, 114, 150),
                                    new Color(210, 238, 238, 150)
                            );*/
                            boolean trail = Math.random() < 0.5;
                            Global.getCombatEngine().addSmoothParticle(
                                    MathUtils.getPointOnCircumference(proj.getLocation(), proj.getCollisionRadius(), proj.getFacing()),
                                    trail ? MathUtils.getPointOnCircumference(Misc.ZERO, 10f, 180f + proj.getFacing()) : proj.getVelocity(),
                                    trail ? 32f : (proj.getWeapon().getSize() == WeaponAPI.WeaponSize.LARGE ? 84f : 50f),
                                    0.8f, 0.5f, 1.5f,
                                    new Color(47, 250, 114, 150)
                            );
                        }
                    }
                    // TBD
                }

            }
        }

        if (beamTimer.intervalElapsed()) {
            beamTimer.randomize();
        }
        beamTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());

        if (((state == State.IN && effectLevel > 0.1f) || state == State.ACTIVE) && !playedWindup) {
            Global.getSoundPlayer().playSound("gigacannon_charge", 1f, 0.8f, ship.getLocation(), ship.getVelocity());
            playedWindup = true;
            playedEnd = false;
        }


    }


    protected void modify(String id, MutableShipStatsAPI stats, float effectLevel) {
        stats.getEnergyAmmoRegenMult().modifyMult(DMG_ID, 1f + effectLevel*AMMO_MULT);
        stats.getMaxSpeed().modifyMult(DMG_ID, 1f + effectLevel*SPEED_MULT);
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        Color color = new Color(47, 250, 114, 75);

        if (ship.getChildModulesCopy() != null && ship.getChildModulesCopy().size() > 0) {
            for (ShipAPI child: ship.getChildModulesCopy()) {
                child.getEngineController().fadeToOtherColor(child, color, new Color(0, 0, 0, 0), effectLevel, 1.0f);
            }
        }

        List<ShipEngineControllerAPI.ShipEngineAPI> maneuveringThrusters = ship.getEngineController() != null ? ship.getEngineController().getShipEngines() : null;
        if (maneuveringThrusters != null) {
            for (ShipEngineControllerAPI.ShipEngineAPI e : maneuveringThrusters) {
                if (Math.abs(Math.sin(Math.toRadians(e.getEngineSlot().getAngle()))) > 0.1) {
                    // Nothing!!
                } else {
                    ship.getEngineController().fadeToOtherColor(e.getEngineSlot(), color, new Color(0, 0, 0, 0), effectLevel, 1.0f);
                }
            }
        }


    }
    protected void unmodify(String id, MutableShipStatsAPI stats) {
        playedWindup = false;
        playedCooledDown = false;

        stats.getEnergyAmmoRegenMult().unmodify(DMG_ID);
        stats.getMaxSpeed().unmodify(DMG_ID);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        // not called
    }


    public String getDisplayNameOverride(State state, float effectLevel) {
        if (state == State.IDLE) {
            return "chroma reactor - ready";
        }
        if (state == State.COOLDOWN) {
            return "reactor overheated";
        }
        if (state == State.IN) {
            return "rerouting power...";
        }
        if (state == State.OUT) {
            return "chroma reactor - danger";
        }
        if (state == State.ACTIVE) {
            return "chroma reactor - active";
        }
        return null;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        float mult = 1f + DMG_BONUS * effectLevel;
        float bonusPercent = (int) ((mult - 1f) * 100f);
        if (index == 0 && effectLevel > 0) {
            return new StatusData("main weapon damage +" + (int) Math.round(bonusPercent) + "%", false);
        }
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
        return weapon != null
                && weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.ENERGY
                && weapon.getSlot().getSlotSize() == WeaponAPI.WeaponSize.MEDIUM
                && weapon.getSlot().isHardpoint();
    }




    public static class NA_FastCapsRangeModifier implements WeaponBaseRangeModifier {
        public List<WeaponAPI> weapons;
        public float mult;
        public float effectLevel;
        public NA_FastCapsRangeModifier(List<WeaponAPI> weapons, float mult) {
            this.weapons = weapons;
            this.mult = mult;
        }

        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }
        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            if (weaponEligible(weapon)) return 1f + mult * effectLevel;
            return 1f;
        }
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }
    }


    public class NA_FastCapDmgBoost implements DamageDealtModifier {
        public List<WeaponAPI> weapons;
        public float mult;
        public float effectLevel;

        public NA_FastCapDmgBoost(List<WeaponAPI> weapons, float mult) {
            this.weapons = weapons;
            this.mult = mult;
        }


        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (param != null) {
                WeaponAPI weapon = null;
                if (param instanceof BeamAPI) {
                    weapon = ((BeamAPI) param).getWeapon();
                } else if (param instanceof DamagingProjectileAPI) {
                    weapon = ((DamagingProjectileAPI) param).getWeapon();
                }
                if (weaponEligible(weapon)) {
                    damage.getModifier().modifyMult(DMG_ID, 1f + mult * effectLevel);
                }
            }
            return null;
        }
    }
}
















