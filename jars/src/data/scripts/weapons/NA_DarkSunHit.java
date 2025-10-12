package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NAUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class NA_DarkSunHit implements OnHitEffectPlugin {
    private static final Color COLOR1 = new Color(255, 161, 192);
    private static final Color COLOR2 = new Color(69, 108, 2);
    private static final Vector2f ZERO = new Vector2f();

    private static final float DMG_PER_POINT = 0.0555f;
    private static final float EMP_PER_POINT = 0.8f;
    private static final float NUM_POINTS = 18f;
    private static final float DIST_PER_POINT = 9f;
    private static final float AOE_BASE = 30f;
    private static final float AOE_PER_POINT = 2f;
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

        if (target instanceof ShipAPI || target instanceof AsteroidAPI) {
            float emp = proj.getEmpAmount() * EMP_PER_POINT;
            float dmg = proj.getBaseDamageAmount() * DMG_PER_POINT;


            for (int i = 1; i <= NUM_POINTS; i++) {
                float dist = i * DIST_PER_POINT;
                float size = AOE_BASE + i*AOE_PER_POINT;
                Vector2f point_dmg = MathUtils.getPointOnCircumference(point, dist, i * MathUtils.getRandomNumberInRange(90f, 150f) + (float) Math.toDegrees(Math.atan2(proj.getVelocity().y, proj.getVelocity().x)));

                boolean end = false;
                List<CombatEntityAPI> targets = NAUtils.getEntitiesWithinRange(point_dmg, size);
                ShipAPI src = proj.getSource();
                for (CombatEntityAPI tt : targets) {
                    if (tt != src && tt.getCollisionClass() != CollisionClass.NONE) {
                        if (tt instanceof ShipAPI) {
                            if (CollisionUtils.isPointWithinBounds(point_dmg, tt)) {
                                if (tt.getShield() != null && tt.getShield().isWithinArc(point_dmg)) {
                                   // nothing
                                } else {
                                    if (tt.getOwner() == src.getOwner()) {
                                        engineAPI.applyDamage(tt, point_dmg, dmg*0.1f, DamageType.ENERGY,
                                                emp*0.1f, false, true, src, false);
                                    } else {
                                        engineAPI.applyDamage(tt, point_dmg, dmg, DamageType.ENERGY,
                                                emp, false, true, src, false);
                                    }

                                    if (tt instanceof ShipAPI) {
                                        Vector2f offset = Vector2f.sub(point_dmg, target.getLocation(), new Vector2f());
                                        offset = Misc.rotateAroundOrigin(offset, -target.getFacing());
                                        DisintegratorEffect effect = new DisintegratorEffect(proj, (ShipAPI) tt, offset) {
                                            protected float getTotalDamage() {
                                                return 10;
                                            }
                                            protected int getNumTicks() {
                                                return NUM_TICKS;
                                            }
                                            protected boolean canDamageHull() {
                                                return false;
                                            }
                                            protected int getNumParticlesPerTick() {
                                                return 5;
                                            }
                                            protected String getSoundLoopId() {
                                                return "na_blackhole_loop";
                                            }
                                            protected void addParticle() {
                                                ParticleData p = new ParticleData(25f, 3f + (float) Math.random() * 2f, 1f);
                                                p.color = RiftLanceEffect.getColorForDarkening(new Color(4, 1, 92));
                                                p.color = Misc.setAlpha(p.color, 35);
                                                particles.add(p);
                                                p.offset = Misc.getPointWithinRadius(p.offset, 10f);
                                            }
                                        };
                                        CombatEntityAPI e = Global.getCombatEngine().addLayeredRenderingPlugin(effect);
                                        e.getLocation().set(proj.getLocation());
                                    }

                                }

                            }


                        }
                    }
                }
                for (int ii = 1; ii <= i; ii++) {
                    if (Math.random() < 0.4 + i * 0.1f)
                        engineAPI.addNebulaSmoothParticle(MathUtils.getRandomPointInCircle(point_dmg, size),
                                MathUtils.getRandomPointInCircle(ZERO, 10f*PARTICLE_VEL),
                                size*0.05f, 1.5f, MathUtils.getRandomNumberInRange(.05f, .4f),
                                0.8f, 1.7f + 0.35f * (i/NUM_POINTS),
                                COLOR1);
                }
                if (Math.random() < 0.5)
                    engineAPI.addNegativeParticle(point_dmg, ZERO, size*1.85f,
                            MathUtils.getRandomNumberInRange(.3f, .5f), 0.5f + 2.0f * (i/NUM_POINTS),
                            COLOR2);

                if (end) {
                    break;
                }
            }

        }
    }
}
