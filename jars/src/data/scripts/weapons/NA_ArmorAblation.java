package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.impl.combat.BreachOnHitEffect;
import com.fs.starfarer.api.impl.combat.CryofluxTransducerEffect;

import java.awt.*;

public class NA_ArmorAblation implements OnHitEffectPlugin {

    //private static final String DISCORD_ID = "na_metahelium_shot";
    private static final float DISCORD_DMG = 25f;
    private static final float DISCORD_DMG_ENERGY = 35f;


    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!shieldHit && target instanceof ShipAPI) {

            float dmg = (projectile.getWeapon().getSlot().getWeaponType() != WeaponAPI.WeaponType.BALLISTIC) ? DISCORD_DMG_ENERGY : DISCORD_DMG;
            BreachOnHitEffect.dealArmorDamage(projectile, (ShipAPI) target, point,
                    dmg);

            Global.getCombatEngine().addSwirlyNebulaParticle(
                    point, Misc.ZERO, dmg, 1.5f, .4f, 0.7f, 1.2f,
                    new Color(238, 19, 107), true
            );
        }

        Misc.playSound(damageResult, point, projectile.getVelocity(),
                "cryoflamer_hit_shield_light",
                "cryoflamer_hit_shield_solid",
                "cryoflamer_hit_shield_heavy",
                "na_metahelium_impact",
                "na_metahelium_impact",
                "na_metahelium_impact");
    }

}
