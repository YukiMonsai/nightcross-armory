package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

public class NA_CorrosionOnFire implements OnFireEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Object targetDataObj = Global.getCombatEngine().getCustomData().get(projectile);
        if (targetDataObj == null) {
            String key = "" + projectile.hashCode();
            Global.getCombatEngine().getCustomData().put(key, new NA_CorrosionListener(projectile, key, projectile.getSource()));
        }
    }
}