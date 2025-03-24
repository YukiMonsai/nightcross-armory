package data.scripts.weapons;

import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.impl.combat.BreachOnHitEffect;
import com.fs.starfarer.api.impl.combat.CryofluxTransducerEffect;

public class NA_ArmorAblation implements OnHitEffectPlugin {

    //private static final String DISCORD_ID = "na_metahelium_shot";
    private static final float DISCORD_DMG = 20f;


    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!shieldHit && target instanceof ShipAPI) {
            BreachOnHitEffect.dealArmorDamage(projectile, (ShipAPI) target, point, DISCORD_DMG);
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
