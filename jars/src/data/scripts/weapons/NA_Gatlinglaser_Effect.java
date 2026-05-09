package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.combat.BreachOnHitEffect;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class NA_Gatlinglaser_Effect implements OnHitEffectPlugin, OnFireEffectPlugin, EveryFrameWeaponEffectPlugin,
        DamageDealtModifier {

    private static final float ARMOR_DMG = 8f;
    protected String weaponId;
    public static float SHIELD_DMG_BOOST = 12f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        ShipAPI ship = weapon.getShip();
        if (!ship.hasListenerOfClass(com.fs.starfarer.api.impl.combat.threat.VoidblasterEffect.class)) {
            ship.addListener(this);
            weaponId = weapon.getId();
        }
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Vector2f vel = target.getVelocity();
        engine.addSmoothParticle(point, vel, 50f, 0f, 0.5f, Color.white);
        engine.addSmoothParticle(point, vel, 30f, 0f, 0.5f, Color.white);
        engine.addNebulaSmoothParticle(point, vel, 30f, 2f, 0f, 0f, 0.5f, Color.CYAN);
        //engine.addNegativeParticle(point, vel, 15f, 0f, 0.5f, Color.white);
        if (!shieldHit) {
            float dir = 0f;
            float arc = 360f;
            dir = Misc.getAngleInDegrees(target.getLocation(), point);
            arc = 270f;
            engine.spawnDebrisSmall(point, vel, 4, dir, arc, 20f, 20f, 720f);
            engine.spawnDebrisMedium(point, vel, 1, dir, arc, 10f, 20f, 360f);
        }

        if (!shieldHit && target instanceof ShipAPI) {

            float dmg = ARMOR_DMG;
            BreachOnHitEffect.dealArmorDamage(projectile, (ShipAPI) target, point,
                    dmg);

            Global.getCombatEngine().addSwirlyNebulaParticle(
                    point, Misc.ZERO, dmg, 1.5f, .4f, 0.7f, 1.2f,
                    new Color(238, 19, 107), true
            );
        }
    }


    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI p = (DamagingProjectileAPI) param;
            if (p.getWeapon() != null && p.getWeapon().getId().equals(weaponId)) {
                if (target instanceof ShipAPI) {
                    ((ShipAPI)target).setSkipNextDamagedExplosion(true);
                    if (shieldHit) {
                        damage.getModifier().modifyPercent("na_gatlinglaser", SHIELD_DMG_BOOST);
                    }
                }
                return "na_gatlinglaser";
            }
        }
        return null;
    }
}







