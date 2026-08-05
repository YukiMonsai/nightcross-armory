package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.weapons.ai.NA_NovaMoteAI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class NA_NovaMote implements EveryFrameWeaponEffectPlugin {

    public static float ATTRACTOR_DURATION_LOCK = 20f;
    public static float ATTRACTOR_DURATION = 10f;

    public WeaponAPI weapon;
    public int lastAmmoCount = 0;


    public static class MoteData {
        public Color jitterColor;
        public Color empColor;

        public String impactSound;
        public String loopSound;
    }

    public static Map<String, MoteData> MOTE_DATA = new HashMap<String, MoteData>();

    public static String MOTELAUNCHER = "na_novamote";



    static {
        MoteData normal = new MoteData();
        normal.jitterColor = new Color(100,165,255,175);
        normal.empColor = new Color(100,165,255,255);
        normal.impactSound = "mote_attractor_impact_normal";
        normal.loopSound = "mote_attractor_loop";

        MOTE_DATA.put(MOTELAUNCHER, normal);
    }

    public static boolean isHighFrequency(ShipAPI ship) {
        //if (true) return true;
        return ship != null && ship.getVariant().hasHullMod(HullMods.HIGH_FREQUENCY_ATTRACTOR);
    }

    public static String getWeaponId(ShipAPI ship) {
        return MOTELAUNCHER;
    }

    public static String getImpactSoundId(ShipAPI ship) {
        return MOTE_DATA.get(getWeaponId(ship)).impactSound;
    }
    public static Color getJitterColor(ShipAPI ship) {
        return MOTE_DATA.get(getWeaponId(ship)).jitterColor;
    }
    public static Color getEMPColor(ShipAPI ship) {
        return MOTE_DATA.get(getWeaponId(ship)).empColor;
    }

    public int getMaxMotes(ShipAPI ship) {
        if (weapon == null) return 0;
        return weapon.getMaxAmmo();
    }

    public static String getLoopSound(ShipAPI ship) {
        return MOTE_DATA.get(getWeaponId(ship)).loopSound;
    }


    public static class SharedMoteAIData {
        public float elapsed = 0f;
        public java.util.List<MissileAPI> motes = new ArrayList<MissileAPI>();
        public java.util.List<MissileAPI> availableMotes = new ArrayList<MissileAPI>();
        public HashMap<MissileAPI, CombatEntityAPI> targets = new HashMap<>();

        public float attractorRemaining = 0f;
        public Vector2f attractorTarget = null;
        public ShipAPI attractorLock = null;
    }
    public static SharedMoteAIData getSharedData(ShipAPI source) {
        String key = source + "_mote_AI_shared";
        SharedMoteAIData data = (SharedMoteAIData) Global.getCombatEngine().getCustomData().get(key);
        if (data == null) {
            data = new SharedMoteAIData();
            Global.getCombatEngine().getCustomData().put(key, data);
        }
        return data;
    }



    protected IntervalUtil launchInterval = new IntervalUtil(1f, 1f);
    protected IntervalUtil attractorParticleInterval = new IntervalUtil(0.05f, 0.1f);

    //protected int empCount = 0;
    protected boolean findNewTargetOnUse = true;



    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        this.weapon = weapon;

        float effectLevel = weapon.getChargeLevel();

        //Global.getCombatEngine().setPaused(true);


        boolean inRange = isMouseInRange(ship);
        if (!inRange) weapon.setForceNoFireOneFrame(true);

        SharedMoteAIData data = getSharedData(ship);
        data.elapsed += amount;

        if (data.attractorRemaining > 0) {
            data.attractorRemaining -= amount;
            if (data.attractorRemaining <= 0 ||
                    (data.attractorLock != null && !data.attractorLock.isAlive()) ||
                    data.motes.isEmpty()) {
                data.attractorTarget = null;
                data.attractorLock = null;
                data.attractorRemaining = 0;
            }
        }
        if (effectLevel <= 0) {
            findNewTargetOnUse = true;
        }

        attractorParticleInterval.advance(amount);
        if (attractorParticleInterval.intervalElapsed()) {
            spawnAttractorParticles(ship);
        }


        /*if (ship == Global.getCombatEngine().getPlayerShip()) {
            Global.getCombatEngine().maintainStatusForPlayerShip(this,
                    Global.getSettings().getSpriteName("ui", "icon_tactical_fragment_swarm"),
                    weapon.getDisplayName(),
                    (int) (100 * launchInterval.getElapsed()) + "%",
                    false);
        }*/
        if (weapon.getAmmo() < weapon.getMaxAmmo() && !launchInterval.intervalElapsed())
            launchInterval.advance(amount * weapon.getAmmoPerSecond());
        if (launchInterval.intervalElapsed() || launchInterval.getElapsed() >= 1f) {
            weapon.getAmmoTracker().setReloadProgress(0);
            launchInterval = new IntervalUtil(1f, 1f);
            Iterator<MissileAPI> iter = data.motes.iterator();
            while (iter.hasNext()) {
                if (!engine.isMissileAlive(iter.next())) {
                    iter.remove();
                }
            }

            if (ship.isHulk()) {
                for (MissileAPI mote : data.motes) {
                    mote.flameOut();
                }
                data.motes.clear();
                return;
            }

             int maxMotes = getMaxMotes(ship);
            if (data.motes.size() < maxMotes &&// false &&
                    !ship.getFluxTracker().isOverloadedOrVenting()) {

                WeaponSlotAPI slot = weapon.getSlot();

                Vector2f loc = slot.computePosition(ship);
                float dir = slot.computeMidArcAngle(ship);
                float arc = slot.getArc();
                dir += arc * (float) Math.random() - arc /2f;

                String weaponId = getWeaponId(ship);
                MissileAPI mote = (MissileAPI) engine.spawnProjectile(ship, weapon,
                        weaponId,
                        loc, dir, null);
                mote.setWeaponSpec(weaponId);
                mote.setMissileAI(new NA_NovaMoteAI(mote, ship));
                mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
                // if they could flame out/be affected by emp, that'd be bad since they don't expire for a
                // very long time so they'd be stuck disabled permanently, for practical purposes
                // thus: total emp resistance (which can't target them anyway, but if it did.)
                mote.setEmpResistance(10000);
                data.motes.add(mote);

                engine.spawnMuzzleFlashOrSmoke(ship, slot, mote.getWeaponSpec(), 0, dir);

                Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 0.25f, loc, new Vector2f());
            }
        }

        float maxMotes = getMaxMotes(ship);
        float fraction = data.motes.size() / (Math.max(1f, maxMotes));
        float volume = fraction * 0.75f;
        if (volume > 1f) volume = 1f;
        if (data.motes.size() > 3) {
            Vector2f com = new Vector2f();
            for (MissileAPI mote : data.motes) {
                Vector2f.add(com, mote.getLocation(), com);
            }
            com.scale(1f / data.motes.size());
            //Global.getSoundPlayer().playLoop("mote_attractor_loop", ship, 1f, volume, com, new Vector2f());
            Global.getSoundPlayer().playLoop(getLoopSound(ship), ship, 1f, volume, com, new Vector2f());
        }


        if (effectLevel > 0 && findNewTargetOnUse) {
            calculateTargetData(ship);
            findNewTargetOnUse = false;
        }

        if (effectLevel == 1) {
            // possible if system is reused immediately w/ no time to cool down, I think
            if (data.attractorTarget == null) {
                calculateTargetData(ship);
            }



            Vector2f slotLoc = weapon.getSlot().computePosition(ship);

            CombatEntityAPI asteroid = engine.spawnAsteroid(0, data.attractorTarget.x, data.attractorTarget.y, 0, 0);
            asteroid.setCollisionClass(CollisionClass.NONE);
            CombatEntityAPI target = asteroid;
            if (data.attractorLock != null) {
                target = data.attractorLock;
            }

            WeightedRandomPicker<MissileAPI> picker = new WeightedRandomPicker<>();
            updateMotes(ship);
            for (MissileAPI mote : data.availableMotes) {
                picker.add(mote, 360 - Math.min(350, Misc.getAngleDiff(weapon.getCurrAngle(), VectorUtils.getAngle(weapon.getLocation(), mote.getLocation()))));
            }

            if (!picker.isEmpty()) {
                MissileAPI missile = picker.pick();

                data.targets.put(missile, target);
                if (missile.getUnwrappedMissileAI() instanceof NA_NovaMoteAI) {
                    ((NA_NovaMoteAI)missile.getUnwrappedMissileAI()).setTarget(target);
                    if (target == data.attractorLock)
                        NA_NovaMoteEffect.onLaunch(missile, weapon, engine);
                }
            }


            if (data.attractorLock == null) {
                //Global.getSoundPlayer().playSound("mote_attractor_targeted_empty_space", 1f, 1f, data.attractorTarget, new Vector2f());
            }


            engine.removeEntity(asteroid);
        }

        updateMotes(ship);
        weapon.getAmmoTracker().setAmmo(data.availableMotes.size());

        lastAmmoCount = weapon.getAmmo();
    }

    protected void spawnAttractorParticles(ShipAPI ship) {
        SharedMoteAIData data = getSharedData(ship);

        if (data.attractorTarget == null) return;

        CombatEngineAPI engine = Global.getCombatEngine();

        Vector2f targetLoc = data.attractorTarget;

        int glows = 2;
        float maxRadius = 300f;
        float minRadius = 200f;

        if (data.attractorLock != null) {
            maxRadius += data.attractorLock.getCollisionRadius();
            minRadius += data.attractorLock.getCollisionRadius();
            targetLoc = data.attractorLock.getShieldCenterEvenIfNoShield();
        }

        float minDur = 0.5f;
        float maxDur = 0.75f;
        float minSize = 5f;
        float maxSize = 15f;
        Color color = getEMPColor(ship);
        for (int i = 0; i < glows; i++) {
            float radius = minRadius + (float) Math.random() * (maxRadius - minRadius);
            Vector2f loc = Misc.getPointAtRadius(targetLoc, radius);
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(loc, targetLoc));
            float dist = Misc.getDistance(loc, targetLoc);

            float dur = minDur + (float) Math.random() * (maxDur - minDur);
            float speed = dist / dur;
            dir.scale(speed);

            float size = minSize + (float) Math.random() * (maxSize - minSize);

            engine.addHitParticle(loc, dir, size, 0.5f, 0.3f, dur, color);
            engine.addHitParticle(loc, dir, size * 0.5f, 0.5f, 0.3f, dur, Color.white);
        }
    }



    public void calculateTargetData(ShipAPI ship) {
        SharedMoteAIData data = getSharedData(ship);
        Vector2f targetLoc = getTargetLoc(ship);
        //System.out.println(getTargetedLocation(ship));
        data.attractorLock = getLockTarget(ship, targetLoc);

        data.attractorRemaining = ATTRACTOR_DURATION;
        if (data.attractorLock != null) {
            targetLoc = new Vector2f(data.attractorLock.getLocation());
            data.attractorRemaining = ATTRACTOR_DURATION_LOCK;
        }
        data.attractorTarget = targetLoc;

    }


    public Vector2f getTargetedLocation(ShipAPI from) {
        Vector2f loc = from.getShipTarget() != null ? from.getShipTarget().getLocation() : null;
        if (loc == null) {
            loc = new Vector2f(from.getMouseTarget());
        }
        return loc;
    }

    public Vector2f getTargetLoc(ShipAPI from) {

        Vector2f targetLoc = new Vector2f(getTargetedLocation(from));
        if (weapon == null) return targetLoc;
        Vector2f slotLoc = weapon.getSlot().computePosition(from);
        float dist = Misc.getDistance(slotLoc, targetLoc);
        if (dist > weapon.getRange()) {
            targetLoc = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(slotLoc, targetLoc));
            targetLoc.scale(weapon.getRange());
            Vector2f.add(targetLoc, slotLoc, targetLoc);
        }
        return targetLoc;
    }

    public boolean isMouseInRange(ShipAPI from) {
        Vector2f targetLoc = new Vector2f(from.getMouseTarget());
        return isLocationInRange(from, targetLoc);
    }

    public boolean isLocationInRange(ShipAPI from, Vector2f loc) {

        if (weapon == null) return false;
        Vector2f slotLoc = weapon.getSlot().computePosition(from);
        float dist = Misc.getDistance(slotLoc, loc);
        if (dist > weapon.getRange()) {
            return false;
        }
        return true;
    }


    public ShipAPI getLockTarget(ShipAPI from, Vector2f loc) {
        if (weapon == null) return null;
        Vector2f slotLoc = weapon.getSlot().computePosition(from);
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (other.isFighter()) continue;
            if (other.getOwner() == from.getOwner()) continue;
            if (other.isHulk()) continue;
            if (!other.isTargetable()) continue;

            float dist = Misc.getDistance(slotLoc, other.getLocation());
            if (dist > weapon.getRange() + other.getCollisionRadius()) continue;

            dist = Misc.getDistance(loc, other.getLocation());
            if (dist < other.getCollisionRadius() + 50f) {
                return other;
            }
        }
        return null;
    }

    public void updateMotes(ShipAPI ship) {
        SharedMoteAIData data = getSharedData(ship);

        data.availableMotes = new ArrayList<>();

        for (MissileAPI mote : data.motes) {
            if (Global.getCombatEngine().isEntityInPlay(mote) && (!data.targets.containsKey(mote) || data.targets.get(mote) == null || !Global.getCombatEngine().isEntityInPlay(data.targets.get(mote)))) {
                data.availableMotes.add(mote);
            }
        }
    }
}





