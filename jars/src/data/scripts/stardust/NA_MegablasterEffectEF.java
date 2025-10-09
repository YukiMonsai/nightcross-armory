package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class NA_MegablasterEffectEF implements NA_StardustWeapon, OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {
    public static float CHARGE_TICK = 0.2f;
    public IntervalUtil chargeTimer = new IntervalUtil(CHARGE_TICK, CHARGE_TICK);

    public static float BASE_RANGE = 1200f;
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(weapon.getShip());
        int active = swarm == null ? 0 : swarm.getNumActiveMembers();
        int required = getNumFragmentsToFire();
        if (active >= required) {
            for (int i = 0; i < required; i++) {
                NA_StargazerStardust.SwarmMember fragment = pickOuterFragmentWithinRangeClosestTo(swarm, 1000, weapon.getLocation());

                swarm.removeMember(fragment);

                Vector2f from = weapon.getFirePoint(0);

                EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                params.segmentLengthMult = 4f;

                params.glowSizeMult = 0.5f;
                params.brightSpotFadeFraction = 0.33f;
                params.brightSpotFullFraction = 0.5f;
                params.movementDurMax = 0.2f;
                params.flickerRateMult = 0.5f;

                float dist = Misc.getDistance(from, from);
                float minBright = 100f;
                if (dist * params.brightSpotFullFraction < minBright) {
                    params.brightSpotFullFraction = minBright / Math.max(minBright, dist);
                }

                float thickness = 50f;

                EmpArcEntityAPI arc = engine.spawnEmpArcVisual(fragment.loc, weapon.getShip(),
                        from,
                        weapon.getShip(),
                        thickness, // thickness
                        new Color(112, 0, 0),
                        new Color(255, 241, 244),
                        params
                );
                //arc.setCoreWidthOverride(thickness * coreWidthMult);
                arc.setSingleFlickerMode(true);
                arc.setUpdateFromOffsetEveryFrame(true);
                //arc.setRenderGlowAtStart(false);
                //arc.setFadedOutAtStart(true);
            }
        } else
            // remove the original
            Global.getCombatEngine().removeEntity(projectile);
    }



    protected static NA_StargazerStardust.SwarmMember pickOuterFragmentWithinRangeClosestTo(NA_StargazerStardust sourceSwarm, float range, Vector2f otherLoc) {
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


    public int getNumFragmentsToFire() {
        return 8;
    }


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() == 0) {
            if (chargeTimer.intervalElapsed()) {
                chargeTimer = new IntervalUtil(0.5f * CHARGE_TICK, CHARGE_TICK);

                RippleDistortion ripple = new RippleDistortion(weapon.getFirePoint(0), Misc.ZERO);
                ripple.setSize(15f + 75 * weapon.getChargeLevel());
                ripple.setIntensity(2f + 5f * weapon.getChargeLevel());
                ripple.fadeOutIntensity(.3F);
                DistortionShader.addDistortion(ripple);

                if (weapon.getShip() != null && NA_StargazerStardust.getSwarmFor(weapon.getShip()) != null) {
                    NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(weapon.getShip());

                    WeightedRandomPicker<NA_StargazerStardust.SwarmMember> picker = swarm.getPicker(true, true);
                    if (!picker.isEmpty()) {
                        NA_StargazerStardust.SwarmMember p = picker.pickAndRemove();

                        Vector2f from = weapon.getFirePoint(0);

                        EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                        params.segmentLengthMult = 4f;

                        params.glowSizeMult = 0.5f;
                        params.brightSpotFadeFraction = 0.33f;
                        params.brightSpotFullFraction = 0.5f;
                        params.movementDurMax = 0.2f;
                        params.flickerRateMult = 0.5f;

                        float dist = Misc.getDistance(from, from);
                        float minBright = 100f;
                        if (dist * params.brightSpotFullFraction < minBright) {
                            params.brightSpotFullFraction = minBright / Math.max(minBright, dist);
                        }

                        float thickness = 20f;

                        EmpArcEntityAPI arc = engine.spawnEmpArcVisual(p.loc, weapon.getShip(),
                                from,
                                weapon.getShip(),
                                thickness, // thickness
                                new Color(5, 5, 5),
                                new Color(255, 25, 52),
                                params
                        );
                        //arc.setCoreWidthOverride(thickness * coreWidthMult);
                        arc.setSingleFlickerMode(true);
                        arc.setRenderGlowAtEnd(false);
                        arc.setUpdateFromOffsetEveryFrame(true);
                    }


                }






                Global.getCombatEngine().addSwirlyNebulaParticle(weapon.getLocation(),
                        Misc.ZERO,
                        15f + 35 * weapon.getChargeLevel(),
                        0.8f,
                        0.1f,
                        0.25f,
                        0.5f, new Color(95, 10, 67),
                        true);
            }
        }

        if (!chargeTimer.intervalElapsed()) {
            chargeTimer.advance(amount);
        }
    }
}








