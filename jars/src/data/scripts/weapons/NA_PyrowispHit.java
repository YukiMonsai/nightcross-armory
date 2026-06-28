package data.scripts.weapons;

import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class NA_PyrowispHit implements OnHitEffectPlugin {

    private static final String PYROWISP_PROJ_ID = "na_pyrowisp_shot";
    private static final String PYROWISP_MEDIUM_PROJ_ID = "na_pyrowisp_medium_shot";
    private static final String RKKV_ID = "na_rkkv_he_dummy_shot";
    private static final String PYROWISP_LARGE_PROJ_ID = "na_pyrowisp_large_shot";
    private static final float PYROWISP_DMG = 90f;
    private static final float PYROWISP_MEDIUM_DMG = 200f;
    private static final float RKKV_DMG = 400f;
    private static final float PYROWISP_LARGE_DMG = 300f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI resultAPI, CombatEngineAPI engineAPI) {
        if (point == null || shieldHit) {
            return;
        }

        if (target instanceof ShipAPI || target instanceof AsteroidAPI) {
            float dmg = 0f;

            switch (proj.getProjectileSpecId()) {
                case PYROWISP_PROJ_ID:
                    dmg = PYROWISP_DMG;
                    break;
                case PYROWISP_MEDIUM_PROJ_ID:
                    dmg = PYROWISP_MEDIUM_DMG;
                    break;
                case PYROWISP_LARGE_PROJ_ID:
                    dmg = PYROWISP_LARGE_DMG;
                    break;
                case RKKV_ID:
                    dmg = RKKV_DMG;
                    break;
                default:
                    break;
            }

            if (dmg > 0) {
                engineAPI.applyDamage(
                        target,
                        point,
                        dmg,
                        DamageType.FRAGMENTATION,
                        0f,
                        false,
                        false,
                        proj.getSource()
                );
                engineAPI.addNegativeSwirlyNebulaParticle(point, target.getVelocity(), 150f, 1.5f,
                        MathUtils.getRandomNumberInRange(.3f, .5f), 1f, 1.5f,
                        new Color(21, 200, 255));
                engineAPI.addSwirlyNebulaParticle(point, target.getVelocity(), 80, 3.5f,
                        MathUtils.getRandomNumberInRange(.3f, .5f), 1.5f, 3f,
                        new Color(255, 83, 21), true);
            }

        }
    }
}
