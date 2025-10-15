package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class NA_CorrosionHit implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Object targetDataObj = Global.getCombatEngine().getCustomData().get(projectile);
        if (targetDataObj != null && targetDataObj instanceof  NA_CorrosionListener) {
            ((NA_CorrosionListener) targetDataObj).doEndOnce = true;
            if (target instanceof ShipAPI) {
                // auto arm if you hit a ship
                ((NA_CorrosionListener) targetDataObj).armed = true;
            }

        }
    }
}