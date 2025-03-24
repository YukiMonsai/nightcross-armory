package data.scripts.weapons;


import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class NA_IonShieldPierceArcEffect implements OnHitEffectPlugin {
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI) {
            float chancemult = ((ShipAPI) target).getHardFluxLevel();
            if ((float) Math.random() > ((shieldHit ? 0.3f * chancemult : 0.25f + 0.5f * chancemult))) {

                float emp = projectile.getEmpAmount();
                float dam = projectile.getDamageAmount();

                engine.spawnEmpArc(projectile.getSource(), point, target, target,
                        DamageType.ENERGY,
                        dam,
                        emp, // emp
                        100000f, // max range
                        "tachyon_lance_emp_impact",
                        20f, // thickness
                        new Color(25, 100, 155, 255),
                        new Color(255, 255, 255, 255)
                );

                //engine.spawnProjectile(null, null, "plasma", point, 0, new Vector2f(0, 0));
            }
        }
    }
}
