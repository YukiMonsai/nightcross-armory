package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class NA_StarkillerEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, NA_StardustWeapon {


    public static float BASE_RANGE = 1500f;
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        // fire a corrosion mote
        ShipAPI target = null;
        if (weapon.getShip() != null) {
            target = weapon.getShip().getShipTarget();
        }
        releaseMissile(NA_StargazerStardust.getSwarmFor(weapon.getShip()), projectile, target != null ? target.getLocation() : MathUtils.getPointOnCircumference(weapon.getFirePoint(0), weapon.getRange(), weapon.getCurrAngle()), BASE_RANGE);

        // remove the original
        Global.getCombatEngine().removeEntity(projectile);
    }


    public static void releaseMissile(NA_StargazerStardust swarm, DamagingProjectileAPI projectile, Vector2f targetloc, float range) {
        if (projectile.getSource() != null) {
            // stargazer hullmod
            int active = swarm == null ? 0 : swarm.getNumActiveMembers();
            int required = 2;
            if (active >= required) {
                CombatEngineAPI engine = Global.getCombatEngine();
                NA_StargazerStardust.SwarmMember fragment = pickPrimaryFragment(swarm, projectile, range);
                swarm.removeMember(fragment);
                NA_StargazerStardust.SwarmMember fragment2 = pickPrimaryFragment(swarm, projectile, range);
                swarm.removeMember(fragment2);
                //NA_StargazerStardust.SwarmMember fragment3 = pickPrimaryFragment(swarm, projectile, range);
                //swarm.removeMember(fragment3);
                if (fragment == null) {
                    return;
                }

                Global.getSoundPlayer().playSound(
                        NA_CorrosionBeamEffect.mote_sfx, 1.0f, 1.0f, fragment.loc, Misc.ZERO);

                float ang = VectorUtils.getAngle(projectile.getLocation(), targetloc);
                CombatEntityAPI projfire = Global.getCombatEngine().spawnProjectile(projectile.getSource(), null,
                        wpn_id,
                        fragment.loc,
                        ang + Math.signum(MathUtils.getRandomNumberInRange(-1, 1)) * 15f,
                        projectile.getVelocity());
                if (projfire instanceof MissileAPI) ((MissileAPI) projfire).setEmpResistance(4);
                Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(projectile.getSource(),
                        WeaponAPI.WeaponType.MISSILE, false, ((DamagingProjectileAPI) projfire).getDamage());

                makeDistortion(fragment.loc);


                if (projfire instanceof MissileAPI) {
                    MissileAPI missile = (MissileAPI) projfire;
                    if (missile.getWeapon() == null || !missile.getWeapon().hasAIHint(WeaponAPI.AIHints.RANGE_FROM_SHIP_RADIUS)) {
                        missile.setStart(new Vector2f(missile.getLocation()));
                    }
                    missile.getLocation().set(fragment.loc);




                    Vector2f from = projectile.getLocation();

                    EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                    params.segmentLengthMult = 4f;

                    params.glowSizeMult = 0.5f;
                    params.brightSpotFadeFraction = 0.33f;
                    params.brightSpotFullFraction = 0.5f;
                    params.movementDurMax = 0.2f;
                    params.flickerRateMult = 0.35f;

                    float dist = Misc.getDistance(from, missile.getLocation());
                    float minBright = 100f;
                    if (dist * params.brightSpotFullFraction < minBright) {
                        params.brightSpotFullFraction = minBright / Math.max(minBright, dist);
                    }

                    float thickness = 20f;

                    EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, projectile,
                            missile.getLocation(),
                            missile,
                            thickness, // thickness
                            new Color(87, 0, 0),
                            new Color(255, 25, 52),
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
    }




    public static final String proj_id = "naai_starkiller_missile";
    public static final String wpn_id = "naai_starkiller";




    protected static NA_StargazerStardust.SwarmMember pickPrimaryFragment(NA_StargazerStardust sourceSwarm, DamagingProjectileAPI projectile, float range) {
        return pickOuterFragmentWithinRangeClosestTo(sourceSwarm, range, projectile.getLocation());
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

    public static void makeDistortion(Vector2f pp) {
        RippleDistortion ripple = new RippleDistortion(pp, Misc.ZERO);
        ripple.setSize(16f);
        ripple.setIntensity(10.0F +  MathUtils.getRandomNumberInRange(0, 20f));
        ripple.setFrameRate(30 + MathUtils.getRandomNumberInRange(0, 15));
        ripple.setCurrentFrame(MathUtils.getRandomNumberInRange(0, 10));
        ripple.fadeInIntensity(.15F + MathUtils.getRandomNumberInRange(0, 0.25f));
        DistortionShader.addDistortion(ripple);
    }


    public int getNumFragmentsToFire() {
        return 2;
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
}








