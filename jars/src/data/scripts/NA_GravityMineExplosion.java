package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.plugins.NAUtils;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class NA_GravityMineExplosion  implements ProximityExplosionEffect {

    public static final float RADIUS = 450f;
    public static final float FORCE = 450f;
    public static float DISRUPTION_DUR = 2.4f;

    public static final Color OVERLOAD_COLOR = new Color(64, 227, 239,255);

    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {

        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f point = explosion.getLocation();

        engine.addNegativeSwirlyNebulaParticle(point,
                Misc.ZERO,
                RADIUS, //60-75
                1.25f,
                0.5f,
                0.5f,
               2.5f,
                new Color(191, 227, 50, 150));


        RippleDistortion ripple2 = new RippleDistortion(point, Misc.ZERO);
        ripple2.setSize(RADIUS);
        ripple2.setIntensity(25.0F);
        ripple2.setFrameRate(-40);
        ripple2.setCurrentFrame(59);
        ripple2.fadeOutIntensity(1.5f);
        DistortionShader.addDistortion(ripple2);

        List<CombatEntityAPI> entities = NAUtils.getEntitiesWithinRange(point, RADIUS);
        if (!entities.isEmpty()) {
            for (CombatEntityAPI entity : entities) {
                float dist = Math.max(50f, MathUtils.getDistance(point, entity.getLocation()) - entity.getCollisionRadius());
                if (entity instanceof ShipAPI) {
                    if (!((ShipAPI) entity).getFluxTracker().isOverloadedOrVenting()) {

                        // longer timeout if close to the center
                        float dur = DISRUPTION_DUR - (dist-50f)/RADIUS;
                        // less punishing to frigs and dest
                        if (((ShipAPI) entity).getHullSize() == ShipAPI.HullSize.FRIGATE) dur *= 0.8f;
                        else if (((ShipAPI) entity).getHullSize() == ShipAPI.HullSize.DESTROYER) dur *= 0.9f;
                        ((ShipAPI) entity).setOverloadColor(OVERLOAD_COLOR);
                        ((ShipAPI) entity).getFluxTracker().beginOverloadWithTotalBaseDuration(dur);
                    }
                }
                float angle = VectorUtils.getAngle(entity.getLocation(), point);
                Vector2f closest = MathUtils.getPointOnCircumference(
                        Misc.ZERO, 1f,
                        angle
                );
                float amt = FORCE * (dist/RADIUS);
                entity.getVelocity().set(
                        entity.getVelocity().x + closest.x*amt,
                        entity.getVelocity().y + closest.y*amt
                );
                // 'gravitational drag'
                if (entity instanceof ShipAPI) {
                    float len = entity.getVelocity().length();
                    float maxlen = 1.5f*((ShipAPI) entity).getMaxSpeed();
                    if (len > maxlen && maxlen > 1f) {
                        entity.getVelocity().set(
                                entity.getVelocity().x * maxlen/len,
                                entity.getVelocity().y * maxlen/len
                        );
                    }
                }


            }
        }


        // explosion_missile
        Global.getSoundPlayer().playSound("na_superblaster_impact", 1.2f, 1.3f, point, Misc.ZERO);

    }
}