package data.scripts.stardust;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

public class NA_DarkSunMissileEffect extends NA_BaseStardustMissile {

    @Override
    public boolean getMissileFrom() {
        return true;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        super.advance(amount, engine, weapon);

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

                        float thickness = 25f;

                        EmpArcEntityAPI arc = engine.spawnEmpArcVisual(p.loc, weapon.getShip(),
                                from,
                                weapon.getShip(),
                                thickness, // thickness
                                new Color(81, 0, 0),
                                new Color(135, 0, 92),
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


    public static float CHARGE_TICK = 0.2f;
    public IntervalUtil chargeTimer = new IntervalUtil(CHARGE_TICK, CHARGE_TICK);

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        super.onFire(projectile, weapon, engine);

        RippleDistortion ripple = new RippleDistortion(projectile.getLocation(), Misc.ZERO);
        ripple.setSize(150f);
        ripple.setIntensity(40.0F);
        ripple.fadeOutIntensity(.5F);
        DistortionShader.addDistortion(ripple);
    }

    protected void configureMissileSwarmParams(NA_StargazerStardust.StardustParams params) {
//		params.flashFringeColor = new Color(255,50,50,255);
//		params.flashFringeColor = new Color(255,165,30,255);
        params.flashFringeColor = new Color(193, 0, 66,255);
        params.flashCoreColor = Color.white;
        params.flashRadius = 140f;
        params.flashCoreRadiusMult = 0.25f;
    }

    protected void swarmCreated(MissileAPI missile, NA_StargazerStardust missileSwarm, NA_StargazerStardust sourceSwarm) {
        if (!missileSwarm.members.isEmpty()) {
            NA_StargazerStardust.SwarmMember p = missileSwarm.members.get(0);
            p.scaler.setBrightness(p.scale);
            p.scaler.setBounceDown(false);
            p.scaler.fadeIn();
        }
    }

    protected int getNumOtherMembersToTransfer() {
        return 14;
        //return 0;
        //return 12;
    }

    protected int getEMPResistance() {
        return 6;
    }

    protected boolean explodeOnFizzling() {
        return false;
    }



//	protected String getExplosionSoundId() {
//		return "devastator_explosion";
//	}


}








