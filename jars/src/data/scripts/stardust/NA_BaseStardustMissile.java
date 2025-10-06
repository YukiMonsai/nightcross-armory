package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;


public class NA_BaseStardustMissile implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, NA_StardustWeapon {

    public static enum FragmentBehaviorOnImpact {
        STOP_AND_FADE,
        STOP_AND_FLASH,
        KEEP_GOING,
    }


    public boolean getMissileFrom() {
        return false;
    }

    protected DamagingProjectileAPI projectile;
    protected WeaponAPI weapon;
    protected CombatEngineAPI engine;
    protected NA_StargazerStardust sourceSwarm;
    protected MissileAPI missile;
    protected ShipAPI ship;

    public NA_BaseStardustMissile() {
    }


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
        int active = swarm == null ? 0 : swarm.getNumActiveMembers();
        int required = getNumFragmentsToFire();
        boolean disable = active < required;
        weapon.setForceDisabled(disable);

        showNoFragmentSwarmWarning(weapon, ship);
    }


    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        this.projectile = projectile;
        this.weapon = weapon;
        this.engine = engine;

        if (!(projectile instanceof MissileAPI)) {
            engine.removeEntity(projectile);
            return;
        }
        missile = (MissileAPI) projectile;
        if (missile.getSource() == null) {
            engine.removeEntity(projectile);
            return;
        }

        ship = missile.getSource();
        sourceSwarm = NA_StargazerStardust.getSwarmFor(missile.getSource());
        if (sourceSwarm == null) {
            engine.removeEntity(projectile);
            return;
        }

        missile.setEmpResistance(getEMPResistance());

        NA_StargazerStardust.SwarmMember fragment = pickPrimaryFragment();
        if (fragment == null) {
            engine.removeEntity(projectile);
            return;
        }

        if (missile.getWeapon() == null || !missile.getWeapon().hasAIHint(WeaponAPI.AIHints.RANGE_FROM_SHIP_RADIUS)) {
            missile.setStart(new Vector2f(missile.getLocation()));
        }
        if (!getMissileFrom())
            missile.getLocation().set(fragment.loc);

        // picked fragment with velocity closest to that of missile, leave the missile's velocity as is
        if (!shouldPickVelocityMatchingPrimaryFragment()) {
            missile.getVelocity().set(fragment.vel);
            boolean setFacing = false;
            if (shouldMakeMissileFaceTargetOnSpawnIfAny()) {
                if (missile.getAI() instanceof GuidedMissileAI) {
                    GuidedMissileAI ai = (GuidedMissileAI) missile.getAI();
                    if (ai.getTarget() != null) {
                        missile.setFacing(Misc.getAngleInDegrees(fragment.loc, ai.getTarget().getLocation()));
                        setFacing = true;
                    }
                }
            }
            if (!setFacing && fragment.vel.length() > 0.1f) {
                missile.setFacing(Misc.getAngleInDegrees(fragment.vel));
            }
        }

        NA_StargazerStardust.StardustParams params = new NA_StargazerStardust.StardustParams();
        params.despawnSound = null;
        params.maxSpeed = missile.getMaxSpeed() + 100f;
        params.baseMembersToMaintain = 0;
        params.removeMembersAboveMaintainLevel = false;
        params.keepProxBasedScaleForAllMembers = true;
        params.initialMembers = 0;
        params.maxOffset = missile.getCollisionRadius() * 1.5f;

        configureMissileSwarmParams(params);

        // can't use data members inside the anon class since they'll change when it fires again
        MissileAPI missile2 = missile;
        com.fs.starfarer.api.impl.combat.threat.BaseFragmentMissileEffect.FragmentBehaviorOnImpact behavior = getOtherFragmentBehaviorOnImpact();
        boolean explodeOnFizzling = explodeOnFizzling();
        String explosionSoundId = getExplosionSoundId();
        NA_StargazerStardust missileSwarm = new NA_StargazerStardust(missile2, params) {
            boolean exploded = false;
            Set<SwarmMember> stopped = new LinkedHashSet<>();
            int origMembers = 0;
            boolean inited = false;
            @Override
            public void advance(float amount) {
                super.advance(amount);
                //if (true) return;

                if (removeFragmentsWhenMissileLosesHitpoints() && !missile2.didDamage()) {
                    if (!inited) {
                        origMembers = members.size();
                        inited = true;
                    }
                    if (origMembers > 0 && members.size() > 1 && missile2.getMaxHitpoints() > 0) {
                        float max = missile2.getMaxHitpoints();
                        float hpPerMember = max / origMembers;
                        float hpLost = max - missile2.getHitpoints();
                        int loseMembers = (int) (hpLost / hpPerMember);
                        int num = members.size();
                        int alreadyLost = origMembers - num;
                        for (SwarmMember p : members) {
                            if (p.fader.isFadingOut()) {
                                alreadyLost++;
                            }
                        }
                        int lose = loseMembers - alreadyLost;
                        if (lose > 0) {
                            despawnMembers(lose, false);
                        }
                    }
                }

                fragment.loc.set(missile2.getLocation());
                fragment.vel.set(missile2.getVelocity());
                if (missile2.isFizzling() && engine.isMissileAlive(missile2)) {
                    fragment.fader.setBrightness(missile2.getCurrentBaseAlpha());
                }
                if (missile2.didDamage()) {
                    if (behavior != com.fs.starfarer.api.impl.combat.threat.BaseFragmentMissileEffect.FragmentBehaviorOnImpact.KEEP_GOING) {
                        CombatEntityAPI target = null;
                        if (missile2.getDamageTarget() instanceof CombatEntityAPI) {
                            target = (CombatEntityAPI) missile2.getDamageTarget();
                        }
                        for (SwarmMember p : members) {
                            if (p == fragment || stopped.contains(p)) {
                                if (p == fragment) {
                                    //p.fader.setDurationOut(0.5f);
                                }
                                continue;
                            }
                            boolean hit = false;
                            if (target != null && target.getExactBounds() != null) {
                                if (target instanceof ShipAPI) {
                                    ShipAPI ship = (ShipAPI) target;
                                    if (ship.getShield() != null) {
                                        boolean inArc = ship.getShield().isWithinArc(p.loc);
                                        if (inArc) {
                                            hit = Misc.getDistance(p.loc, ship.getShieldCenterEvenIfNoShield()) <
                                                    ship.getShieldRadiusEvenIfNoShield();
                                        }
                                    }
                                }
                                if (!hit) {
                                    hit = target.isPointInBounds(p.loc);
                                }
                            } else {
                                Vector2f toP = Vector2f.sub(p.loc, fragment.loc, new Vector2f());
                                hit = Vector2f.dot(toP, fragment.vel) > 0;
                            }
                            if (hit) {
                                p.vel.set(new Vector2f());
                                if (behavior == com.fs.starfarer.api.impl.combat.threat.BaseFragmentMissileEffect.FragmentBehaviorOnImpact.STOP_AND_FLASH) {
                                    p.flash();
                                }
                                reportFragmentHit(missile2, p, this, target);
                                stopped.add(p);
                            }
                        }
                    }
                }
                if (explodeOnFizzling && explosionSoundId != null) {
                    if ((missile2.isFizzling() || (missile2.getHitpoints() <= 0 && !missile2.didDamage())) && !exploded) {
                        exploded = true;
                        Global.getSoundPlayer().playSound(explosionSoundId, 1f, 1f, missile2.getLocation(), missile2.getVelocity());
                        missile2.interruptContrail();
                        engine.removeEntity(missile2);
                        missile2.explode();
                    }
                }

                if ((missile2.isFizzling() || missile2.getHitpoints() <= 0) && !missile2.didDamage() && !exploded) {
                    params.minDespawnTime = 0.5f;
                    params.maxDespawnTime = 1f;
                    params.minFadeoutTime = 0.5f;
                    params.maxFadeoutTime = 1f;
                    setForceDespawn(true);
                }

                swarmAdvance(amount, missile2, this);
            }

//			@Override
//			public int getNumMembersToMaintain() {
//				int base = params.baseMembersToMaintain;
//				float level = missile2.getHullLevel();
//				int maintain = (int) Math.round(level * base);
//				if (maintain < 1) maintain = 1;
//				return maintain;
//			}
        };

        sourceSwarm.removeMember(fragment);
        missileSwarm.addMember(fragment);
        fragment.rollOffset(missileSwarm.params, missile);

        if (makePrimaryFragmentGlow()) {
            if (fragment.flash != null) {
                fragment.flash = null;
            }
            fragment.flashNext = null;

            fragment.flash();
            fragment.flash.setBounceDown(false);
        }


        int transfer = getNumOtherMembersToTransfer();
        if (transfer > 0) {
            sourceSwarm.transferMembersTo(missileSwarm, transfer, fragment.loc, getRangeForNearbyFragments());
        }

        int add = getNumOtherMembersToAdd();
        if (addNewMembersIfNotEnoughToTransfer() && missileSwarm.members.size() - 1 < transfer) {
            add += transfer - (missileSwarm.members.size() - 1);
        }
        if (add > 0) {
            missileSwarm.addMembers(add);
        }

        swarmCreated(missile, missileSwarm, sourceSwarm);

        float hpLoss = getHPLossPerTransferredMember();
        hpLoss *= 1 + transfer;
        if (hpLoss > 0) {
            ship.setHitpoints(ship.getHitpoints() - hpLoss);
            // cause the swarm (or what's left of it) to despawn
            if (ship.getHitpoints() <= 0) {
                ship.setSpawnDebris(false);
                engine.applyDamage(ship, ship.getLocation(), 100f, DamageType.ENERGY, 0f, true, false, missile, false);
            }
        }

        if (withEMPArc()) {
            spawnEMPArc();
        }
    }

    protected void swarmCreated(MissileAPI missile, NA_StargazerStardust missileSwarm, NA_StargazerStardust sourceSwarm) {

    }
    protected void reportFragmentHit(MissileAPI missile, NA_StargazerStardust.SwarmMember p, NA_StargazerStardust swarm, CombatEntityAPI target) {

    }

    protected float getHPLossPerTransferredMember() {
        if (!ship.isFighter()) return 0f;
        float hpLoss = ship.getMaxHitpoints() / (sourceSwarm.params.baseMembersToMaintain * 0.8f);
        return hpLoss;
    }

    protected void configureMissileSwarmParams(NA_StargazerStardust.StardustParams params) {
        params.flashFringeColor = new Color(255,50,50,255);
        params.flashCoreColor = Color.white;
        params.flashRadius = 60f;
        params.flashCoreRadiusMult = 0.75f;
    }

    protected boolean shouldPickVelocityMatchingPrimaryFragment() {
        if (missile.getAI() instanceof GuidedMissileAI) {
            GuidedMissileAI ai = (GuidedMissileAI) missile.getAI();
            if (ai.getTarget() == null) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    protected boolean shouldMakeMissileFaceTargetOnSpawnIfAny() {
        return false;
    }

    protected NA_StargazerStardust.SwarmMember pickPrimaryFragment() {
        if (shouldPickVelocityMatchingPrimaryFragment()) {
            return pickVelocityMatchingFragmentWithinRange(getRangeFromSourceToPickFragments());
        }
        return pickOuterFragmentWithinRange(getRangeFromSourceToPickFragments());
    }

    protected NA_StargazerStardust.SwarmMember pickOuterFragmentWithinRange(float range) {
        NA_StargazerStardust.SwarmMember best = null;
        float maxDist = -Float.MAX_VALUE;
        WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker = sourceSwarm.getPicker(true, true);
        while (!picker.isEmpty()) {
            NA_StargazerStardust.SwarmMember p = picker.pickAndRemove();
            float dist = Misc.getDistance(p.loc, sourceSwarm.getAttachedTo().getLocation());
            if (sourceSwarm.params.generateOffsetAroundAttachedEntityOval) {
                //dist -= sourceSwarm.attachedTo.getCollisionRadius() * 0.75f;
                dist -= Misc.getTargetingRadius(p.loc, sourceSwarm.attachedTo, false) + sourceSwarm.params.maxOffset - range * 0.5f;
            }
            if (dist > maxDist && dist < range) {
                best = p;
                maxDist = dist;
            }
        }
        return best;
    }

    protected NA_StargazerStardust.SwarmMember pickVelocityMatchingFragmentWithinRange(float range) {
        Vector2f vel = missile.getVelocity();
        NA_StargazerStardust.SwarmMember best = null;
        float maxVelDiff = 0f;
        WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker = sourceSwarm.getPicker(true, true);
        while (!picker.isEmpty()) {
            NA_StargazerStardust.SwarmMember p = picker.pickAndRemove();
            float dist = Misc.getDistance(p.loc, sourceSwarm.getAttachedTo().getLocation());
            if (sourceSwarm.params.generateOffsetAroundAttachedEntityOval) {
                dist -= Misc.getTargetingRadius(p.loc, sourceSwarm.attachedTo, false) + sourceSwarm.params.maxOffset - range * 0.5f;
            }
            float velDiff = Misc.getDistance(p.vel, vel);
            if (velDiff > maxVelDiff && dist < range) {
                best = p;
                maxVelDiff = dist;
            }
        }
        return best;
    }

    protected NA_StargazerStardust.SwarmMember pickOuterFragmentWithinRangeClosestTo(float range, Vector2f otherLoc) {
        NA_StargazerStardust.SwarmMember best = null;
        float minDist = Float.MAX_VALUE;
        WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker = sourceSwarm.getPicker(true, true);
        while (!picker.isEmpty()) {
            NA_StargazerStardust.SwarmMember p = picker.pickAndRemove();
            float dist = Misc.getDistance(p.loc, sourceSwarm.getAttachedTo().getLocation());
            if (sourceSwarm.params.generateOffsetAroundAttachedEntityOval) {
                dist -= Misc.getTargetingRadius(p.loc, sourceSwarm.attachedTo, false) + sourceSwarm.params.maxOffset - range * 0.5f;
            }
            if (dist > range) continue;
            dist = Misc.getDistance(p.loc, otherLoc);
            if (dist < minDist) {
                best = p;
                minDist = dist;
            }
        }
        return best;
    }

    protected boolean removeFragmentsWhenMissileLosesHitpoints() {
        return true;
    }

    protected boolean makePrimaryFragmentGlow() {
        return true;
    }

    protected float getRangeForNearbyFragments() {
        return 75f;
    }
    protected float getRangeFromSourceToPickFragments() {
        return 150f;
    }

    protected int getNumOtherMembersToTransfer() {
        return 0;
    }
    protected boolean addNewMembersIfNotEnoughToTransfer() {
        return true;
    }
    protected int getNumOtherMembersToAdd() {
        return 0;
    }

    protected int getEMPResistance() {
        return 0;
    }

    protected com.fs.starfarer.api.impl.combat.threat.BaseFragmentMissileEffect.FragmentBehaviorOnImpact getOtherFragmentBehaviorOnImpact() {
        return com.fs.starfarer.api.impl.combat.threat.BaseFragmentMissileEffect.FragmentBehaviorOnImpact.STOP_AND_FLASH;
    }


    @Override
    public int getNumFragmentsToFire() {
        return 1 + getNumOtherMembersToTransfer();
    }

    protected boolean explodeOnFizzling() {
        return false;
    }

    protected String getExplosionSoundId() {
        return null;
    }

    protected void swarmAdvance(float amount, MissileAPI missile, NA_StargazerStardust swarm) {

    }


    protected boolean withEMPArc() {
        return !ship.isFighter();
    }

    protected Color getEMPFringeColor() {
        Color c = weapon.getSpec().getGlowColor();
        //c = Misc.setAlpha(c, 127);
        //c = Misc.scaleColorOnly(c, 0.75f);
        return c;
    }

    protected Color getEMPCoreColor() {
        return Color.white;
    }

    protected void spawnEMPArc() {

        Vector2f from = weapon.getFirePoint(0);

        EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
        params.segmentLengthMult = 4f;

        params.glowSizeMult = 0.5f;
        params.brightSpotFadeFraction = 0.33f;
        params.brightSpotFullFraction = 0.5f;
        params.movementDurMax = 0.2f;
        params.flickerRateMult = 0.5f;

        float dist = Misc.getDistance(from, missile.getLocation());
        float minBright = 100f;
        if (dist * params.brightSpotFullFraction < minBright) {
            params.brightSpotFullFraction = minBright / Math.max(minBright, dist);
        }

        float thickness = 20f;

        if (getMissileFrom()) {
            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
                    missile.getLocation(),
                    missile,from, weapon.getShip(),
                    thickness+2f, // thickness
                    getEMPFringeColor(),
                    getEMPCoreColor(),
                    params
            );
            //arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode(true);
            arc.setUpdateFromOffsetEveryFrame(true);
            //arc.setRenderGlowAtStart(false);
            //arc.setFadedOutAtStart(true);
        } else {
            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, weapon.getShip(),
                    missile.getLocation(),
                    missile,
                    thickness, // thickness
                    getEMPFringeColor(),
                    getEMPCoreColor(),
                    params
            );
            //arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode(true);
            arc.setUpdateFromOffsetEveryFrame(true);
            //arc.setRenderGlowAtStart(false);
            //arc.setFadedOutAtStart(true);
        }

    }

}




