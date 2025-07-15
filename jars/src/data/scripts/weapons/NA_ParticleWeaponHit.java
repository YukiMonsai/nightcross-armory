package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NAUtils;
import data.scripts.everyframe.Nightcross_Trails;
import java.awt.Color;
import java.util.List;

import data.scripts.util.NAUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class NA_ParticleWeaponHit implements OnHitEffectPlugin {
    private static final Color COLOR1 = new Color(75, 225, 255);
    private static final Color COLOR2 = new Color(0, 0, 225);
    private static final Vector2f ZERO = new Vector2f();

    private static final float DMG_PER_POINT = 0.1f;
    private static final float EMP_PER_POINT = 0.8f;
    private static final float NUM_POINTS = 10f;
    private static final float DIST_PER_POINT = 50f;
    private static final float AOE_BASE = 30f;
    private static final float AOE_PER_POINT = 15f;
    private static final float PARTICLE_VEL = 3f;

    /*private static final class NA_ParticleListener implements DamageListener {

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if ((source instanceof DamagingProjectileAPI) && (target != null)) {
                DamagingProjectileAPI proj = (DamagingProjectileAPI) source;
                if ((proj.getProjectileSpecId() != null) && proj.getProjectileSpecId().contentEquals("na_heavyparticlecannon_shot")) {
                    if (result.getDamageToShields() > 0) {

                    }
                }
            }
        }
    }*/

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI resultAPI, CombatEngineAPI engineAPI) {
        if (point == null || shieldHit) {
            return;
        }

        if (target instanceof ShipAPI || target instanceof AsteroidAPI) {
            float emp = proj.getEmpAmount() * EMP_PER_POINT;
            float dmg = proj.getBaseDamageAmount() * DMG_PER_POINT;


            for (int i = 1; i <= NUM_POINTS; i++) {
                float dist = i * DIST_PER_POINT;
                float size = AOE_BASE + i*AOE_PER_POINT;
                Vector2f point_dmg = MathUtils.getPointOnCircumference(point, dist, (float) Math.toDegrees(Math.atan2(proj.getVelocity().y, proj.getVelocity().x)));

                boolean end = false;
                List<CombatEntityAPI> targets = NAUtils.getEntitiesWithinRange(point_dmg, size);
                ShipAPI src = proj.getSource();
                for (CombatEntityAPI tt : targets) {
                    if (tt != src && tt.getCollisionClass() != CollisionClass.NONE) {
                        if (tt instanceof ShipAPI) {
                            if (CollisionUtils.isPointWithinBounds(point_dmg, tt)) {
                                if (tt.getOwner() == src.getOwner()) {
                                    engineAPI.applyDamage(tt, point_dmg, dmg*0.1f, DamageType.FRAGMENTATION,
                                            emp*0.1f, false, true, src, false);
                                } else {
                                    engineAPI.applyDamage(tt, point_dmg, dmg, DamageType.FRAGMENTATION,
                                            emp, false, true, src, false);
                                }
                                if (!end && tt.getShield() != null && tt.getShield().isWithinArc(point_dmg)) {
                                    end = true;
                                }
                            }


                        }
                    }
                }
                for (int ii = 1; ii <= i; ii++) {
                    if (Math.random() < 0.4 + i * 0.1f)
                        engineAPI.addNebulaSmoothParticle(MathUtils.getRandomPointInCircle(point_dmg, size),
                                MathUtils.getRandomPointInCircle(ZERO, 10f*PARTICLE_VEL),
                                size*0.05f, 1.5f, 0.1f,
                                0.8f, 0.7f + 0.35f * (i/NUM_POINTS),
                                COLOR1);
                }
                if (Math.random() < 0.5)
                    engineAPI.addSmoothParticle(point_dmg, ZERO, size*0.85f, 1f,
                            0.5f,
                            0.5f + 2.0f * (i/NUM_POINTS), COLOR2);

                if (end) {
                    break;
                }
            }

        }
    }
}
