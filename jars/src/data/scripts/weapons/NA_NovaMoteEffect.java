package data.scripts.weapons;

import java.util.ArrayList;
import java.util.List;

import java.awt.Color;
import java.util.Vector;

import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.impl.combat.RiftTrailEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.magiclib.util.MagicLensFlare;

import static com.fs.starfarer.title.String.picker;

/**
 * IMPORTANT: will be multiple instances of this, one for the the OnFire (per weapon) and one for the OnHit (per missile) effects.
 *
 * (Well, no data members, so not *that* important.)
 */
public class NA_NovaMoteEffect implements OnFireEffectPlugin, OnHitEffectPlugin {

    public static String NA_NOVA_RIFT = "na_novamote";



    public static class NA_NovaMoteEffectCount {
        int count = 0;
        float totalElapsed = 0;

        public void update() {
            float elapsed = Global.getCombatEngine().getTotalElapsedTime(false);
            if (totalElapsed >= elapsed) return;

            totalElapsed = elapsed;

            count = 0;
            for (MissileAPI m : Global.getCombatEngine().getMissiles()) {
                if (m.hasTag(NA_NOVA_RIFT)) {
                    count++;
                }
            }
        }
    }

    public static float OVERLOAD_TIME = 0.5f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        doHit(projectile, target, point, shieldHit, damageResult, engine);
    }

    public static void doHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Color color = RiftCascadeEffect.STANDARD_RIFT_COLOR;
        Object o = projectile.getWeapon().getSpec().getProjectileSpec();
        if (o instanceof MissileSpecAPI) {
            MissileSpecAPI spec = (MissileSpecAPI) o;
            color = spec.getExplosionColor();
            // Do explosion here
            MagicLensFlare.createSharpFlare(engine, projectile.getSource(), point, 4, 400, 45, new Color(30, 255, 175), color);
            MagicLensFlare.createSharpFlare(engine, projectile.getSource(), point, 4, 400, 135, new Color(30, 255, 175), color);


        }

        engine.addSmoothParticle(point,
                Misc.ZERO,
                shieldHit ? 300 : 520, //60-75
                0.1f,
                0.2f,
                0.27f,
                new Color(175, 255, 215, 255));

        engine.addSmoothParticle(point,
                Misc.ZERO,
                shieldHit ? 250 : 420, //60-75
                0.1f,
                0.4f,
                0.87f,
                new Color(175, 255, 215, 255));

        if (target instanceof ShipAPI) {
            ShipAPI ship = ((ShipAPI) target);
            float time = shieldHit ? OVERLOAD_TIME * 2 : OVERLOAD_TIME;
            if (!ship.getFluxTracker().isOverloaded() || ship.getFluxTracker().getOverloadTimeRemaining() < time) {
                if (ship.getFluxTracker().isOverloaded()) ship.getFluxTracker().stopOverload();
                ship.getFluxTracker().beginOverloadWithTotalBaseDuration(time);
            }
        }



        Vector2f vel = new Vector2f();
        if (target != null) vel.set(target.getVelocity());
        Global.getSoundPlayer().playSound("na_bassdestroyer_charge", 1f, 0.25f, point, vel);
    }


    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Global.getCombatEngine().removeEntity(projectile); // pick a mote


    }


    public static void onLaunch(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        MissileAIPlugin proxAI = Global.getCombatEngine().createProximityFuseAI((MissileAPI)projectile);
        NA_NovaTrailEffect trail = new NA_NovaTrailEffect((MissileAPI) projectile, null) {
            boolean exploded = false;
            float elapsed = 0f;
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                super.advance(amount, events);
                proxAI.advance(amount);
                if (!exploded && !missile.didDamage() && missile.wasRemoved()) {// !engine.isMissileAlive(missile)) {
                    doHit(missile, null, missile.getLocation(), false, null, engine);

                    exploded = true;
                }

            }
            protected Color getUndercolor() {
                //return new Color(100, 0, 20, 255);
                return new Color(30, 255, 175);
            }
            protected Color getDarkeningColor() {
                return RiftLanceEffect.getColorForDarkening(getUndercolor());
            }
            @Override
            protected float getBaseParticleDuration() {
                return 1.5f;
            }


        };

        MissileAPI missile = ((MissileAPI) projectile);

        missile.setEmpResistance(1000);
        missile.setEccmChanceOverride(1f);
        missile.addTag(NA_NOVA_RIFT);

//		if (weapon.getShip().getHullSpec().hasTag(Tags.DWELLER)) {
//			missile.setHitpoints(missile.getHitpoints() * HITPOINTS_MULT_WHEN_BY_DWELLER_SHIP);
//		}

        Global.getCombatEngine().addPlugin(trail);
    }
}














