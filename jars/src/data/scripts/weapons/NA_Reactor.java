package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NAUtils;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class NA_Reactor implements EveryFrameWeaponEffectPlugin {

    public IntervalUtil particletimer = new IntervalUtil(0.9f, 0.95f);
    public IntervalUtil alarmtimer = new IntervalUtil(0.2f, 0.2f);
    public final float INTENSITY_RAMP = 0.4f;
    public final float COLOR_RAMP = 50f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship != null && !ship.isPhased()) {
            float fluxperc = ship.getFluxTracker().getFluxLevel();
            float hardflux = ship.getFluxTracker().getHardFlux()/Math.max(1, ship.getFluxTracker().getMaxFlux());

            if (alarmtimer.intervalElapsed()) {
                alarmtimer = new IntervalUtil(0.12f, 0.17f);

                if (fluxperc > 0.6) {
                    Vector2f pos = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.75f);
                    Vector2f zero = Misc.ZERO;
                    float ang = MathUtils.clampAngle(MathUtils.getRandomNumberInRange(0, 360));

                    StandardLight light = new StandardLight(pos, zero, zero, null);
                    light.setIntensity(1.3f);
                    light.setSpecularMult(25f);
                    light.setColor(new Color(
                            255,45,45));
                    light.fadeOut(0.35f);
                    light.setSize(ship.getCollisionRadius() * 0.12f);
                    light.setLifetime(0.5f);

                    //light.setSize(ship.getCollisionRadius() * 0.35f);
                    LightShader.addLight(light);
                }


            } else {
                alarmtimer.advance(amount);
            }

            if (particletimer.intervalElapsed()) {
                particletimer = new IntervalUtil(0.75f, 0.75f);
                Vector2f pos = weapon.getLocation();
                Vector2f zero = Misc.ZERO;

                StandardLight light = new StandardLight(zero, zero, weapon.getSlot().getLocation(), ship);
                light.setIntensity(0.25f + 0.8f*INTENSITY_RAMP * fluxperc + 0.5f*INTENSITY_RAMP * hardflux);
                light.setColor(new Color(
                        (int) Math.max(75f, Math.min(225f, 75 + 2*COLOR_RAMP * hardflux)),
                        (int) Math.max(75f, Math.min(225f, 75 + COLOR_RAMP * (fluxperc - hardflux))),
                        225));
                light.fadeIn(0.35f);
                light.setLifetime(0.65f);
                light.setSize(ship.getCollisionRadius() * 0.1f);
                light.setSpecularMult(25f);
                LightShader.addLight(light);

                if (hardflux > 0.5 && MathUtils.getRandomNumberInRange(0f, 1.5f) < hardflux) {
                    engine.spawnEmpArc(ship,
                            pos,
                            ship,
                            ship,
                            DamageType.ENERGY,
                            0,
                            0, // emp
                            ship.getCollisionRadius(), // max range
                            null, //"tachyon_lance_emp_impact",
                            20f, // thickness
                            NAUtils.isStargazerRed(ship) ? new Color(
                                    170,
                                    86,
                                    86, 147) : new Color(
                                    175,
                                    75,
                                    225),
                            NAUtils.isStargazerRed(ship) ? new Color(
                                    159,
                                    110,
                                    110, 98) : new Color(200, 175, 225, 255)
                    );
                }
            } else {
                particletimer.advance(amount);
            }

        }
    }
}
